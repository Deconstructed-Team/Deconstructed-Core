package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.network.PacketHandler;
import dev.feldsher.deconstructedcore.network.SyncContainerClientDataMessage;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.UpgradeHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A storage's container: its inventory slots, then the player's 27 + 9, then the upgrade slots, then the slots of
 * the upgrades' settings tabs. Positions are set by the screen, which knows how wide the storage is drawn.
 * <p>
 * Settings, no-sort and memory slots, sorting, the search box and oversized stack sync come with the stages that
 * port them.
 */
public abstract class StorageContainerMenuBase<S extends IStorageWrapper> extends Container implements ISyncedContainer {
	private static final String OPEN_TAB_ID_TAG = "openTabId";
	private static final String REMOVE_OPEN_TAB_ID_TAG = "removeOpenTabId";

	public static final int NUMBER_OF_PLAYER_SLOTS = 36;
	private static final int ERROR_RESULT_TICKS = 60;

	protected final EntityPlayer player;
	protected final S storageWrapper;
	private final int storageItemSlotIndex;
	private final boolean shouldLockStorageItemSlot;
	private int storageItemSlotNumber = -1;
	private int numberOfUpgradeSlots = 0;
	private final Map<Integer, UpgradeContainerBase<?, ?>> upgradeContainers = new LinkedHashMap<>();

	@Nullable
	private UpgradeSlotChangeResult errorUpgradeSlotChangeResult = null;
	private long errorResultExpirationTime = 0;

	protected StorageContainerMenuBase(EntityPlayer player, S storageWrapper, int storageItemSlotIndex, boolean shouldLockStorageItemSlot) {
		this.player = player;
		this.storageWrapper = storageWrapper;
		this.storageItemSlotIndex = storageItemSlotIndex;
		this.shouldLockStorageItemSlot = shouldLockStorageItemSlot;

		if (player.world.isRemote) {
			// The server sent the contents just before opening: the client builds from them, not from what it had
			storageWrapper.onContentsNbtUpdated();
		}
		storageWrapper.onInit();
		addStorageInventorySlots();
		addPlayerInventorySlots(player.inventory);
		addUpgradeSlots();
		addUpgradeSettingsContainers();
	}

	public S getStorageWrapper() {
		return storageWrapper;
	}

	protected void addStorageInventorySlots() {
		for (int slotIndex = 0; slotIndex < storageWrapper.getInventoryHandler().getSlots(); slotIndex++) {
			addSlotToContainer(new StorageInventorySlot(player.world.isRemote, storageWrapper, slotIndex));
		}
	}

	protected void addPlayerInventorySlots(InventoryPlayer playerInventory) {
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				addStorageItemSafeSlot(playerInventory, j + i * 9 + 9);
			}
		}

		for (int slotIndex = 0; slotIndex < 9; ++slotIndex) {
			addStorageItemSafeSlot(playerInventory, slotIndex);
		}
	}

	private void addStorageItemSafeSlot(InventoryPlayer playerInventory, int slotIndex) {
		Slot slot;
		if (shouldLockStorageItemSlot && slotIndex == storageItemSlotIndex) {
			slot = new Slot(playerInventory, slotIndex, 0, 0) {
				@Override
				public boolean canTakeStack(EntityPlayer playerIn) {
					return false;
				}
			};
		} else {
			slot = new Slot(playerInventory, slotIndex, 0, 0);
		}
		addSlotToContainer(slot);
		if (shouldLockStorageItemSlot && slotIndex == storageItemSlotIndex) {
			storageItemSlotNumber = slot.slotNumber;
		}
	}

	/** After the inventory: by then the storage has its contents, and with them its real upgrade handler on both sides. */
	private void addUpgradeSlots() {
		UpgradeHandler upgradeHandler = storageWrapper.getUpgradeHandler();
		numberOfUpgradeSlots = upgradeHandler.getSlots();
		for (int slotIndex = 0; slotIndex < numberOfUpgradeSlots; slotIndex++) {
			addSlotToContainer(new StorageUpgradeSlot(upgradeHandler, slotIndex));
		}
	}

	private void addUpgradeSettingsContainers() {
		storageWrapper.getUpgradeHandler().getSlotWrappers().forEach((slot, wrapper) -> UpgradeContainerRegistry.instantiateContainer(player, slot, wrapper)
				.ifPresent(container -> upgradeContainers.put(slot, container)));

		for (UpgradeContainerBase<?, ?> container : upgradeContainers.values()) {
			container.getSlots().forEach(this::addSlotToContainer);
			container.onInit();
		}

		storageWrapper.getOpenTabId().ifPresent(id -> {
			if (upgradeContainers.containsKey(id)) {
				upgradeContainers.get(id).setIsOpen(true);
			}
		});
	}

	public Map<Integer, UpgradeContainerBase<?, ?>> getUpgradeContainers() {
		return upgradeContainers;
	}

	public boolean isUpgradeSettingsSlot(int index) {
		return index >= getFirstUpgradeSlot() + numberOfUpgradeSlots && index < inventorySlots.size();
	}

	public void setOpenTabId(int tabId) {
		if (player.world.isRemote) {
			sendToServer(OPEN_TAB_ID_TAG, tag -> tag.setInteger(OPEN_TAB_ID_TAG, tabId));
		}
		storageWrapper.setOpenTabId(tabId);
	}

	public void removeOpenTabId() {
		if (player.world.isRemote) {
			sendToServer(REMOVE_OPEN_TAB_ID_TAG, tag -> tag.setBoolean(REMOVE_OPEN_TAB_ID_TAG, true));
		}
		storageWrapper.removeOpenTabId();
	}

	private void sendToServer(String key, java.util.function.Consumer<NBTTagCompound> fill) {
		NBTTagCompound data = new NBTTagCompound();
		fill.accept(data);
		PacketHandler.INSTANCE.sendToServer(new SyncContainerClientDataMessage(data));
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey("containerId")) {
			int containerId = data.getInteger("containerId");
			if (upgradeContainers.containsKey(containerId)) {
				upgradeContainers.get(containerId).handlePacket(data);
			}
		} else if (data.hasKey(OPEN_TAB_ID_TAG)) {
			setOpenTabId(data.getInteger(OPEN_TAB_ID_TAG));
		} else if (data.hasKey(REMOVE_OPEN_TAB_ID_TAG)) {
			removeOpenTabId();
		}
	}

	/** A click into a filter slot leaves a ghost copy of what's held there, and the held stack where it was. */
	@Override
	public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
		if (slotId >= 0 && isUpgradeSettingsSlot(slotId) && inventorySlots.get(slotId) instanceof IFilterSlot) {
			Slot slot = inventorySlots.get(slotId);
			ItemStack cursorStack = player.inventory.getItemStack();
			if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) {
				if (slot.isItemValid(cursorStack) || cursorStack.isEmpty()) {
					ItemStack ghost = cursorStack.copy();
					if (ghost.getCount() > 1) {
						ghost.setCount(1);
					}
					slot.putStack(ghost);
				}
			}
			return ItemStack.EMPTY;
		}
		return super.slotClick(slotId, dragType, clickType, player);
	}

	@Override
	public boolean canMergeSlot(ItemStack stack, Slot slot) {
		return !(slot instanceof IFilterSlot) && super.canMergeSlot(stack, slot);
	}

	@Override
	public boolean canDragIntoSlot(Slot slot) {
		return !(slot instanceof IFilterSlot) && super.canDragIntoSlot(slot);
	}

	public int getStorageItemSlotNumber() {
		return storageItemSlotNumber;
	}

	public int getInventorySlotsSize() {
		return getNumberOfStorageInventorySlots() + NUMBER_OF_PLAYER_SLOTS;
	}

	public int getNumberOfStorageInventorySlots() {
		return storageWrapper.getInventoryHandler().getSlots();
	}

	public int getNumberOfUpgradeSlots() {
		return numberOfUpgradeSlots;
	}

	public int getFirstUpgradeSlot() {
		return getInventorySlotsSize();
	}

	public int getNumberOfRows() {
		return storageWrapper.getNumberOfSlotRows();
	}

	public int getColumnsTaken() {
		return storageWrapper.getColumnsTaken();
	}

	public boolean isStorageInventorySlot(int index) {
		return index >= 0 && index < getNumberOfStorageInventorySlots();
	}

	protected boolean isPlayerInventorySlot(int index) {
		return index >= getNumberOfStorageInventorySlots() && index < getInventorySlotsSize();
	}

	public boolean isUpgradeSlot(int index) {
		return index >= getFirstUpgradeSlot() && index < getFirstUpgradeSlot() + numberOfUpgradeSlots;
	}

	public Optional<UpgradeSlotChangeResult> getErrorUpgradeSlotChangeResult() {
		if (errorUpgradeSlotChangeResult != null && player.world.getTotalWorldTime() >= errorResultExpirationTime) {
			errorUpgradeSlotChangeResult = null;
		}
		return Optional.ofNullable(errorUpgradeSlotChangeResult);
	}

	private void updateSlotChangeError(UpgradeSlotChangeResult result) {
		if (player.world.isRemote && !result.successful()) {
			errorUpgradeSlotChangeResult = result;
			errorResultExpirationTime = player.world.getTotalWorldTime() + ERROR_RESULT_TICKS;
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = inventorySlots.get(index);
		if (slot != null && slot.getHasStack()) {
			ItemStack slotStack = slot.getStack();
			itemstack = slotStack.copy();

			if (!mergeSlotStack(index, slotStack)) {
				return ItemStack.EMPTY;
			}

			if (slotStack.isEmpty()) {
				slot.putStack(ItemStack.EMPTY);
			} else {
				slot.onSlotChanged();
			}

			if (slotStack.getCount() == itemstack.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTake(playerIn, slotStack);
		}
		return itemstack;
	}

	private boolean mergeSlotStack(int index, ItemStack slotStack) {
		int storageSlots = getNumberOfStorageInventorySlots();
		int playerSlotsEnd = getInventorySlotsSize();
		if (isUpgradeSlot(index)) {
			return mergeItemStack(slotStack, storageSlots, playerSlotsEnd, true) || mergeItemStack(slotStack, 0, storageSlots, false);
		} else if (isStorageInventorySlot(index)) {
			return mergeItemStack(slotStack, storageSlots, playerSlotsEnd, true);
		} else if (isUpgradeSettingsSlot(index)) {
			return false;
		}
		return mergeToUpgradeSlots(slotStack) || mergeItemStack(slotStack, 0, storageSlots, false);
	}

	/** One upgrade into the first empty upgrade slot that takes it: vanilla's merge would stack upgrades in a slot. */
	private boolean mergeToUpgradeSlots(ItemStack slotStack) {
		if (!(slotStack.getItem() instanceof IUpgradeItem<?>)) {
			return false;
		}
		for (int index = getFirstUpgradeSlot(); index < getFirstUpgradeSlot() + numberOfUpgradeSlots; index++) {
			Slot upgradeSlot = inventorySlots.get(index);
			if (!upgradeSlot.getHasStack() && upgradeSlot.isItemValid(slotStack)) {
				upgradeSlot.putStack(slotStack.splitStack(1));
				return true;
			}
		}
		return false;
	}

	@Override
	public void onContainerClosed(EntityPlayer playerIn) {
		super.onContainerClosed(playerIn);
		storageWrapper.getInventoryHandler().saveInventory();
	}

	/** Called on both sides after an upgrade is put in or taken out. */
	protected void onUpgradeChanged() {
		//noop by default
	}

	/** An upgrade slot: it takes one upgrade, and only what the upgrade allows to be added to or removed from this storage. */
	public class StorageUpgradeSlot extends SlotItemHandler {
		private final int slotIndex;
		private boolean wasEmpty;

		public StorageUpgradeSlot(UpgradeHandler upgradeHandler, int slotIndex) {
			super(upgradeHandler, slotIndex, -15, 0);
			this.slotIndex = slotIndex;
			wasEmpty = upgradeHandler.getStackInSlot(slotIndex).isEmpty();
		}

		@Override
		public void onSlotChanged() {
			super.onSlotChanged();
			boolean isEmpty = getStack().isEmpty();
			if (wasEmpty != isEmpty) {
				onUpgradeChanged();
			}
			wasEmpty = isEmpty;
		}

		@Override
		public void putStack(@Nonnull ItemStack stack) {
			boolean changed = getStack().getItem() != stack.getItem();
			super.putStack(stack);
			wasEmpty = getStack().isEmpty();
			if (changed) {
				onUpgradeChanged();
			}
		}

		@Override
		public boolean isItemValid(@Nonnull ItemStack stack) {
			if (stack.isEmpty() || !getItemHandler().isItemValid(slotIndex, stack)) {
				return false;
			}
			UpgradeSlotChangeResult result;
			if (getStack().isEmpty()) {
				result = ((IUpgradeItem<?>) stack.getItem()).canAddUpgradeTo(storageWrapper, stack, true, player.world.isRemote);
			} else if (stack.getCount() > 1) {
				return false;
			} else {
				result = ((IUpgradeItem<?>) getStack().getItem()).canSwapUpgradeFor(stack, slotIndex, storageWrapper, player.world.isRemote);
			}

			updateSlotChangeError(result);
			return result.successful();
		}

		@Override
		public boolean canTakeStack(EntityPlayer playerIn) {
			if (!super.canTakeStack(playerIn)) {
				return false;
			}
			if (!(getStack().getItem() instanceof IUpgradeItem<?>)) {
				return true;
			}
			UpgradeSlotChangeResult result = ((IUpgradeItem<?>) getStack().getItem()).canRemoveUpgradeFrom(storageWrapper, playerIn.world.isRemote, playerIn);
			updateSlotChangeError(result);
			return result.successful();
		}
	}
}
