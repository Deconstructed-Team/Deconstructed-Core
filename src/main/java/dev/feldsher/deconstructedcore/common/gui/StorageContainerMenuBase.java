package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.compat.invtweaks.InvTweaksSections;
import dev.feldsher.deconstructedcore.network.PacketHandler;
import dev.feldsher.deconstructedcore.network.SyncContainerClientDataMessage;
import dev.feldsher.deconstructedcore.settings.SettingsManager;
import dev.feldsher.deconstructedcore.settings.main.MainSettingsCategory;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.UpgradeHandler;
import invtweaks.api.container.ChestContainer;
import invtweaks.api.container.ContainerSectionCallback;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemHandlerHelper;
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
 * Storage slots may hold more than a stack, which vanilla neither syncs (its packets carry counts in a byte) nor
 * clicks right: the container sends its slots in its own packets, and handles clicks on storage slots itself.
 * <p>
 * The search box comes with the stage that ports it.
 * <p>
 * Inventory Tweaks treats it as a chest without its buttons, whose sections are only the player's inventory: it sorts
 * that and moves items in it, and leaves storage slots to the storage's own sort.
 */
@ChestContainer(showButtons = false)
public abstract class StorageContainerMenuBase<S extends IStorageWrapper> extends LargeStackSyncContainer implements ISyncedContainer {
	private static final String OPEN_TAB_ID_TAG = "openTabId";
	private static final String REMOVE_OPEN_TAB_ID_TAG = "removeOpenTabId";
	private static final String GHOST_SLOT_TAG = "ghostSlot";
	private static final String GHOST_STACK_TAG = "ghostStack";
	private static final String SORT_BY_TAG = "sortBy";
	private static final String ACTION_TAG = "action";

	public static final int NUMBER_OF_PLAYER_SLOTS = 36;
	public static final String EMPTY_UPGRADE_SLOT_BACKGROUND = DeconstructedCore.MOD_ID + ":item/empty_upgrade_slot";
	private static final int ERROR_RESULT_TICKS = 60;

	protected final EntityPlayer player;
	protected final S storageWrapper;
	private final int storageItemSlotIndex;
	private final boolean shouldLockStorageItemSlot;
	private int storageItemSlotNumber = -1;
	private int numberOfUpgradeSlots = 0;
	private boolean reopening = false;
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

	/** For Inventory Tweaks, which alone calls it: the raw map keeps its classes out of this one's signatures. */
	@ContainerSectionCallback
	@SuppressWarnings({"unused", "rawtypes"})
	public Map getInvTweaksSections() {
		return InvTweaksSections.playerInventoryOnly(this);
	}

	/** The open storage shows a change of the items it shows at once, not at its next refresh. */
	@Override
	public void detectAndSendChanges() {
		if (!player.world.isRemote) {
			storageWrapper.refreshRenderInfo();
		}
		super.detectAndSendChanges();
	}

	public S getStorageWrapper() {
		return storageWrapper;
	}

	protected void addStorageInventorySlots() {
		for (int slotIndex = 0; slotIndex < storageWrapper.getInventoryHandler().getSlots(); slotIndex++) {
			addSlotToContainer(new StorageInventorySlot(player, storageWrapper, slotIndex));
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

	public void sort() {
		if (player.world.isRemote) {
			sendToServer(ACTION_TAG, tag -> tag.setString(ACTION_TAG, "sort"));
			return;
		}
		storageWrapper.sort();
	}

	/** Opens the storage's settings screen in place of this one: the client asks, the server opens it. */
	public void openSettings() {
		if (player.world.isRemote) {
			sendToServer(ACTION_TAG, tag -> tag.setString(ACTION_TAG, "openSettings"));
			return;
		}
		openSettingsScreen();
	}

	protected abstract void openSettingsScreen();

	private MainSettingsCategory<?> getGlobalSettingsCategory() {
		return storageWrapper.getSettingsHandler().getGlobalSettingsCategory();
	}

	private boolean shouldShiftClickIntoOpenTabFirst() {
		MainSettingsCategory<?> category = getGlobalSettingsCategory();
		return SettingsManager.getSettingValue(player, category.getPlayerSettingsTagName(), category, SettingsManager.SHIFT_CLICK_INTO_OPEN_TAB_FIRST);
	}

	/**
	 * With keep tab open off, the open tab is forgotten when the screen closes; not when it's reopened to show changed
	 * upgrades, which is no close to the player.
	 */
	protected void removeOpenTabIfKeepOff() {
		MainSettingsCategory<?> category = getGlobalSettingsCategory();
		if (Boolean.FALSE.equals(SettingsManager.getSettingValue(player, category.getPlayerSettingsTagName(), category, SettingsManager.KEEP_TAB_OPEN))) {
			storageWrapper.removeOpenTabId();
		}
	}

	public void setReopening() {
		reopening = true;
	}

	public SortBy getSortBy() {
		return storageWrapper.getSortBy();
	}

	public void setSortBy(SortBy sortBy) {
		if (player.world.isRemote) {
			sendToServer(SORT_BY_TAG, tag -> tag.setString(SORT_BY_TAG, sortBy.getSerializedName()));
		}
		storageWrapper.setSortBy(sortBy);
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
		} else if (data.hasKey(SORT_BY_TAG)) {
			setSortBy(SortBy.fromName(data.getString(SORT_BY_TAG)));
		} else if ("sort".equals(data.getString(ACTION_TAG))) {
			sort();
		} else if ("openSettings".equals(data.getString(ACTION_TAG))) {
			openSettings();
		} else if (data.hasKey(GHOST_SLOT_TAG)) {
			setGhostSlot(data.getInteger(GHOST_SLOT_TAG), new ItemStack(data.getCompoundTag(GHOST_STACK_TAG)));
		}
	}

	/** A ghost copy into an upgrade's filter slot that takes it, as one dragged onto it from an item list. */
	public void setGhostSlot(int slotId, ItemStack stack) {
		if (player.world.isRemote) {
			sendToServer(GHOST_SLOT_TAG, tag -> {
				tag.setInteger(GHOST_SLOT_TAG, slotId);
				tag.setTag(GHOST_STACK_TAG, stack.writeToNBT(new NBTTagCompound()));
			});
			return;
		}
		if (slotId < 0 || slotId >= inventorySlots.size() || !isUpgradeSettingsSlot(slotId) || !(inventorySlots.get(slotId) instanceof IFilterSlot)) {
			return;
		}
		Slot slot = inventorySlots.get(slotId);
		if (stack.isEmpty() || slot.isItemValid(stack)) {
			ItemStack ghost = stack.copy();
			if (ghost.getCount() > 1) {
				ghost.setCount(1);
			}
			slot.putStack(ghost);
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
		if (clickType == ClickType.SWAP && dragType >= 0 && dragType < 9 && isHotbarSwapBlocked(slotId, dragType, player)) {
			return ItemStack.EMPTY;
		}
		ItemStack result;
		if (isStorageInventorySlot(slotId) && clickType == ClickType.PICKUP && (dragType == 0 || dragType == 1)) {
			resetDrag();
			result = pickupWithStorageSlot(inventorySlots.get(slotId), dragType, player);
		} else if (isStorageInventorySlot(slotId) && clickType == ClickType.SWAP && dragType >= 0 && dragType < 9) {
			resetDrag();
			swapWithStorageSlot(inventorySlots.get(slotId), dragType, player);
			result = ItemStack.EMPTY;
		} else {
			result = super.slotClick(slotId, dragType, clickType, player);
		}
		// The client sends what its click returned for the server to compare, in vanilla's packet with a byte count:
		// both sides cap it the same so that the comparison holds for oversized stacks
		if (result.getCount() > 64) {
			result = result.copy();
			result.setCount(64);
		}
		return result;
	}

	/**
	 * Vanilla's hotbar key swap doesn't ask the hotbar slot whether its stack can be taken or another put in, so the
	 * open storage's own slot, which is locked, would be swapped away by its number key.
	 */
	private boolean isHotbarSwapBlocked(int slotId, int hotbarIndex, EntityPlayer player) {
		Slot hotbarSlot = null;
		for (Slot slot : inventorySlots) {
			if (slot.inventory == player.inventory && slot.getSlotIndex() == hotbarIndex) {
				hotbarSlot = slot;
				break;
			}
		}
		if (hotbarSlot == null || slotId < 0 || slotId >= inventorySlots.size()) {
			return false;
		}
		ItemStack fromStack = inventorySlots.get(slotId).getStack();
		return !fromStack.isEmpty() && !hotbarSlot.isItemValid(fromStack) || hotbarSlot.getHasStack() && !hotbarSlot.canTakeStack(player);
	}

	/** Vanilla's left and right click, but taking at most a stack out of a slot and putting in up to the slot's limit. */
	private ItemStack pickupWithStorageSlot(Slot slot, int dragType, EntityPlayer player) {
		InventoryPlayer inventory = player.inventory;
		ItemStack slotStack = slot.getStack();
		ItemStack cursorStack = inventory.getItemStack();
		ItemStack ret = slotStack.isEmpty() ? ItemStack.EMPTY : slotStack.copy();

		if (slotStack.isEmpty()) {
			if (!cursorStack.isEmpty() && slot.isItemValid(cursorStack)) {
				int count = Math.min(dragType == 0 ? cursorStack.getCount() : 1, slot.getItemStackLimit(cursorStack));
				slot.putStack(cursorStack.splitStack(count));
			}
		} else if (slot.canTakeStack(player)) {
			if (cursorStack.isEmpty()) {
				int count = Math.min(slotStack.getCount(), slotStack.getMaxStackSize());
				if (dragType == 1) {
					count = count / 2 + count % 2;
				}
				inventory.setItemStack(slot.decrStackSize(count));
				slot.onTake(player, inventory.getItemStack());
			} else if (slot.isItemValid(cursorStack)) {
				if (ItemHandlerHelper.canItemStacksStack(slotStack, cursorStack)) {
					int count = Math.min(dragType == 0 ? cursorStack.getCount() : 1, slot.getItemStackLimit(cursorStack) - slotStack.getCount());
					if (count > 0) {
						cursorStack.shrink(count);
						slotStack.grow(count);
					}
				} else if (cursorStack.getCount() <= slot.getItemStackLimit(cursorStack) && slotStack.getCount() <= slotStack.getMaxStackSize()) {
					slot.putStack(cursorStack);
					inventory.setItemStack(slotStack);
				}
			} else if (ItemHandlerHelper.canItemStacksStack(slotStack, cursorStack)) {
				int count = Math.min(slotStack.getCount(), cursorStack.getMaxStackSize() - cursorStack.getCount());
				if (count > 0) {
					ItemStack taken = slot.decrStackSize(count);
					cursorStack.grow(taken.getCount());
					slot.onTake(player, taken);
				}
			}
		}
		slot.onSlotChanged();
		return ret;
	}

	/** Vanilla's hotbar key swap, but taking at most a stack out of a slot, and leaving a slot holding more than a stack. */
	private void swapWithStorageSlot(Slot slot, int hotbarSlot, EntityPlayer player) {
		InventoryPlayer inventory = player.inventory;
		ItemStack hotbarStack = inventory.getStackInSlot(hotbarSlot);
		ItemStack slotStack = slot.getStack();
		if (hotbarStack.isEmpty() && slotStack.isEmpty()) {
			return;
		}

		if (hotbarStack.isEmpty()) {
			if (slot.canTakeStack(player)) {
				if (slotStack.getCount() <= slotStack.getMaxStackSize()) {
					inventory.setInventorySlotContents(hotbarSlot, slotStack);
					slot.putStack(ItemStack.EMPTY);
					slot.onTake(player, slotStack);
				} else {
					inventory.setInventorySlotContents(hotbarSlot, slot.decrStackSize(slotStack.getMaxStackSize()));
					slot.onSlotChanged();
				}
			}
		} else if (slotStack.isEmpty()) {
			if (slot.isItemValid(hotbarStack)) {
				int limit = slot.getItemStackLimit(hotbarStack);
				if (hotbarStack.getCount() > limit) {
					slot.putStack(hotbarStack.splitStack(limit));
				} else {
					slot.putStack(hotbarStack);
					inventory.setInventorySlotContents(hotbarSlot, ItemStack.EMPTY);
				}
			}
		} else if (slotStack.getCount() <= slotStack.getMaxStackSize() && slot.canTakeStack(player) && slot.isItemValid(hotbarStack)) {
			int limit = slot.getItemStackLimit(hotbarStack);
			if (hotbarStack.getCount() > limit) {
				slot.putStack(hotbarStack.splitStack(limit));
				slot.onTake(player, slotStack);
				if (!inventory.addItemStackToInventory(slotStack)) {
					player.dropItem(slotStack, true);
				}
			} else {
				slot.putStack(hotbarStack);
				inventory.setInventorySlotContents(hotbarSlot, slotStack);
				slot.onTake(player, slotStack);
			}
		}
	}

	/**
	 * Vanilla's merge, but up to the storage slots' limits, and never more than an item's stack size into other slots,
	 * as a storage slot's stack may be bigger than that.
	 */
	@Override
	protected boolean mergeItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
		boolean merged = false;
		boolean intoStorage = isStorageInventorySlot(startIndex);
		int first = reverseDirection ? endIndex - 1 : startIndex;
		int step = reverseDirection ? -1 : 1;

		if (stack.isStackable() || intoStorage) {
			for (int i = first; i >= startIndex && i < endIndex && !stack.isEmpty(); i += step) {
				Slot slot = inventorySlots.get(i);
				ItemStack slotStack = slot.getStack();
				if (!slotStack.isEmpty() && ItemHandlerHelper.canItemStacksStack(slotStack, stack) && (!intoStorage || slot.isItemValid(stack))) {
					int maxSize = getMergeLimit(slot, stack);
					if (slotStack.getCount() < maxSize) {
						int count = Math.min(maxSize - slotStack.getCount(), stack.getCount());
						stack.shrink(count);
						slotStack.grow(count);
						slot.onSlotChanged();
						merged = true;
					}
				}
			}
		}

		if (intoStorage) {
			// Empty slots that remember the stack take it before those that remember nothing
			MemorySettingsCategory memory = storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
			for (int i = first; i >= startIndex && i < endIndex && !stack.isEmpty(); i += step) {
				Slot slot = inventorySlots.get(i);
				if (!slot.getHasStack() && memory.isSlotSelected(i) && slot.isItemValid(stack)) {
					slot.putStack(stack.splitStack(Math.min(getMergeLimit(slot, stack), stack.getCount())));
					slot.onSlotChanged();
					merged = true;
				}
			}
		}

		for (int i = first; i >= startIndex && i < endIndex && !stack.isEmpty(); i += step) {
			Slot slot = inventorySlots.get(i);
			if (!slot.getHasStack() && slot.isItemValid(stack)) {
				slot.putStack(stack.splitStack(Math.min(getMergeLimit(slot, stack), stack.getCount())));
				slot.onSlotChanged();
				merged = true;
			}
		}
		return merged;
	}

	private static int getMergeLimit(Slot slot, ItemStack stack) {
		if (slot instanceof StorageInventorySlot) {
			return slot.getItemStackLimit(stack);
		}
		return Math.min(slot.getItemStackLimit(stack), stack.getMaxStackSize());
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

	/** What the storage slot remembers, for its empty slot to show. */
	public Optional<ItemStack> getMemorizedStackInSlot(int slotIndex) {
		return storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getSlotFilterStack(slotIndex, false);
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
		if (!player.world.isRemote && !result.successful()) {
			// The client checks less and may have done the click: what it sent back looks the same, so vanilla won't resync
			requestFullSync();
		}
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
			if (shouldShiftClickIntoOpenTabFirst()) {
				return mergeToOpenUpgradeTab(slotStack) || mergeItemStack(slotStack, storageSlots, playerSlotsEnd, true);
			}
			return mergeItemStack(slotStack, storageSlots, playerSlotsEnd, true) || mergeToOpenUpgradeTab(slotStack);
		} else if (isUpgradeSettingsSlot(index)) {
			return mergeItemStack(slotStack, 0, storageSlots, false) || mergeItemStack(slotStack, storageSlots, playerSlotsEnd, true);
		}
		if (shouldShiftClickIntoOpenTabFirst()) {
			return mergeToOpenUpgradeTab(slotStack) || mergeToUpgradeSlots(slotStack) || mergeItemStack(slotStack, 0, storageSlots, false);
		}
		return mergeToUpgradeSlots(slotStack) || mergeItemStack(slotStack, 0, storageSlots, false) || mergeToOpenUpgradeTab(slotStack);
	}

	/** Into the open upgrade tab's slots that hold items; filter slots only take ghost copies, by clicks. */
	private boolean mergeToOpenUpgradeTab(ItemStack slotStack) {
		for (UpgradeContainerBase<?, ?> container : upgradeContainers.values()) {
			if (!container.isOpen()) {
				continue;
			}
			boolean merged = false;
			for (Slot slot : container.getSlots()) {
				if (!(slot instanceof IFilterSlot) && !slotStack.isEmpty()) {
					merged |= mergeItemStack(slotStack, slot.slotNumber, slot.slotNumber + 1, false);
				}
			}
			return merged;
		}
		return false;
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
		if (!reopening && !playerIn.world.isRemote) {
			removeOpenTabIfKeepOff();
		}
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
			setBackgroundName(EMPTY_UPGRADE_SLOT_BACKGROUND);
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
