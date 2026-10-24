package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * A storage's container: its inventory slots, then the player's 27 + 9.
 * Positions are set by the screen, which knows how wide the storage is drawn.
 * <p>
 * Upgrade slots and their tabs, settings, no-sort and memory slots, sorting, the search box and
 * oversized stack sync come with the stages that port them.
 */
public abstract class StorageContainerMenuBase<S extends IStorageWrapper> extends Container {
	public static final int NUMBER_OF_PLAYER_SLOTS = 36;

	protected final EntityPlayer player;
	protected final S storageWrapper;
	private final int storageItemSlotIndex;
	private final boolean shouldLockStorageItemSlot;
	private int storageItemSlotNumber = -1;

	protected StorageContainerMenuBase(EntityPlayer player, S storageWrapper, int storageItemSlotIndex, boolean shouldLockStorageItemSlot) {
		this.player = player;
		this.storageWrapper = storageWrapper;
		this.storageItemSlotIndex = storageItemSlotIndex;
		this.shouldLockStorageItemSlot = shouldLockStorageItemSlot;

		storageWrapper.onInit();
		addStorageInventorySlots();
		addPlayerInventorySlots(player.inventory);
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

	public int getStorageItemSlotNumber() {
		return storageItemSlotNumber;
	}

	public int getInventorySlotsSize() {
		return inventorySlots.size();
	}

	public int getNumberOfStorageInventorySlots() {
		return storageWrapper.getInventoryHandler().getSlots();
	}

	public int getNumberOfUpgradeSlots() {
		return 0;
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
		return index >= getNumberOfStorageInventorySlots() && index < getNumberOfStorageInventorySlots() + NUMBER_OF_PLAYER_SLOTS;
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
		if (isStorageInventorySlot(index)) {
			return mergeItemStack(slotStack, storageSlots, storageSlots + NUMBER_OF_PLAYER_SLOTS, true);
		}
		return mergeItemStack(slotStack, 0, storageSlots, false);
	}

	@Override
	public void onContainerClosed(EntityPlayer playerIn) {
		super.onContainerClosed(playerIn);
		storageWrapper.getInventoryHandler().saveInventory();
	}
}
