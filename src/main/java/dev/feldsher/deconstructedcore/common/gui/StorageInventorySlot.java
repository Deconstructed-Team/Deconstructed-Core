package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.api.ISlotChangeResponseUpgrade;
import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import net.minecraft.init.Items;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StorageInventorySlot extends SlotItemHandler {
	/** Above this, the screen draws the count itself, abbreviated: vanilla's would run out of the slot. */
	public static final int SPECIAL_COUNT_RENDER_THRESHOLD = 99;
	public static final String INACCESSIBLE_SLOT_BACKGROUND = DeconstructedCore.MOD_ID + ":item/inaccessible_slot";

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

	/** A slot its part shuts shows so; one set for an item, or remembering one, shows the item instead, drawn by the screen. */
	@Nullable
	@SideOnly(Side.CLIENT)
	@Override
	public String getSlotTexture() {
		return getSlotTexture(storageWrapper, slotIndex);
	}

	@Nullable
	public static String getSlotTexture(IStorageWrapper storageWrapper, int slotIndex) {
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
		if (!inventoryHandler.isSlotAccessible(slotIndex)) {
			return INACCESSIBLE_SLOT_BACKGROUND;
		}
		if (inventoryHandler.getFilterItem(slotIndex) != Items.AIR
				|| storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).isSlotSelected(slotIndex)) {
			return null;
		}
		return inventoryHandler.getNoItemIcon(slotIndex);
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
