package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.UpgradeHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * A storage's container: its inventory slots, then the player's 27 + 9, then the upgrade slots.
 * Positions are set by the screen, which knows how wide the storage is drawn.
 * <p>
 * Upgrade tabs, settings, no-sort and memory slots, sorting, the search box and oversized stack sync come with the
 * stages that port them.
 */
public abstract class StorageContainerMenuBase<S extends IStorageWrapper> extends Container {
	public static final int NUMBER_OF_PLAYER_SLOTS = 36;
	private static final int ERROR_RESULT_TICKS = 60;

	protected final EntityPlayer player;
	protected final S storageWrapper;
	private final int storageItemSlotIndex;
	private final boolean shouldLockStorageItemSlot;
	private int storageItemSlotNumber = -1;
	private int numberOfUpgradeSlots = 0;

	@Nullable
	private UpgradeSlotChangeResult errorUpgradeSlotChangeResult = null;
	private long errorResultExpirationTime = 0;

	protected StorageContainerMenuBase(EntityPlayer player, S storageWrapper, int storageItemSlotIndex, boolean shouldLockStorageItemSlot) {
		this.player = player;
		this.storageWrapper = storageWrapper;
		this.storageItemSlotIndex = storageItemSlotIndex;
		this.shouldLockStorageItemSlot = shouldLockStorageItemSlot;

		storageWrapper.onInit();
		addStorageInventorySlots();
		addPlayerInventorySlots(player.inventory);
		addUpgradeSlots();
	}

	public S getStorageWrapper() {
		return storageWrapper;
	}

	protected void addStorageInventorySlots() {
		for (int slotIndex = 0; slotIndex < storageWrapper.getInventoryHandler().getSlots(); slotIndex++) {
			addSlotToContainer(new StorageInventorySlot(storageWrapper, slotIndex));
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
