package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.api.ISlotChangeResponseUpgrade;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class StorageInventorySlot extends SlotItemHandler {
	private final boolean isClientSide;
	private final IStorageWrapper storageWrapper;
	private final int slotIndex;

	public StorageInventorySlot(boolean isClientSide, IStorageWrapper storageWrapper, int slotIndex) {
		super(storageWrapper.getInventoryHandler(), slotIndex, 0, 0);
		this.isClientSide = isClientSide;
		this.storageWrapper = storageWrapper;
		this.slotIndex = slotIndex;
	}

	@Override
	public boolean isItemValid(@Nonnull ItemStack stack) {
		return storageWrapper.getInventoryHandler().isItemValid(slotIndex, stack);
	}

	@Override
	public void onSlotChanged() {
		super.onSlotChanged();
		// saving here as well because there are many cases where vanilla modifies stack directly and inventory handler isn't aware of it
		// however it does notify the slot of change
		storageWrapper.getInventoryHandler().saveInventory();
		if (!isClientSide) {
			storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(ISlotChangeResponseUpgrade.class)
					.forEach(u -> u.onSlotChange(storageWrapper.getInventoryHandler(), slotIndex));
		}
	}

	@Override
	public int getItemStackLimit(@Nonnull ItemStack stack) {
		return storageWrapper.getInventoryHandler().getBaseStackLimit(stack);
	}
}
