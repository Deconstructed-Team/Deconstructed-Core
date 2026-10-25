package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.api.ISlotChangeResponseUpgrade;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class StorageInventorySlot extends SlotItemHandler {
	/** Above this, the screen draws the count itself, abbreviated: vanilla's would run out of the slot. */
	public static final int SPECIAL_COUNT_RENDER_THRESHOLD = 99;

	private static boolean hideLargeCounts = false;

	private final boolean isClientSide;
	private final IStorageWrapper storageWrapper;
	private final int slotIndex;
	private final EntityPlayer player;

	public StorageInventorySlot(EntityPlayer player, IStorageWrapper storageWrapper, int slotIndex) {
		super(storageWrapper.getInventoryHandler(), slotIndex, 0, 0);
		this.player = player;
		isClientSide = player.world.isRemote;
		this.storageWrapper = storageWrapper;
		this.slotIndex = slotIndex;
	}

	/**
	 * While set, client slots show stacks above {@link #SPECIAL_COUNT_RENDER_THRESHOLD} as single items, so that the
	 * screen's vanilla slot rendering draws no count over which the screen draws the abbreviated one.
	 */
	public static void setHideLargeCounts(boolean hide) {
		hideLargeCounts = hide;
	}

	@Override
	public ItemStack getStack() {
		ItemStack stack = super.getStack();
		if (hideLargeCounts && isClientSide && stack.getCount() > SPECIAL_COUNT_RENDER_THRESHOLD) {
			ItemStack single = stack.copy();
			single.setCount(1);
			return single;
		}
		return stack;
	}

	@Override
	public boolean isItemValid(@Nonnull ItemStack stack) {
		return storageWrapper.getInventoryHandler().isItemValid(slotIndex, stack, player);
	}

	@Override
	public void onSlotChanged() {
		super.onSlotChanged();
		// saving here as well because there are many cases where vanilla modifies stack directly and inventory handler isn't aware of it
		// however it does notify the slot of change
		storageWrapper.getInventoryHandler().onContentsChanged(slotIndex);
		if (!isClientSide) {
			storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(ISlotChangeResponseUpgrade.class)
					.forEach(u -> u.onSlotChange(storageWrapper.getInventoryHandler(), slotIndex));
		}
	}

	@Override
	public int getItemStackLimit(@Nonnull ItemStack stack) {
		return storageWrapper.getInventoryHandler().getStackLimit(slotIndex, stack);
	}
}
