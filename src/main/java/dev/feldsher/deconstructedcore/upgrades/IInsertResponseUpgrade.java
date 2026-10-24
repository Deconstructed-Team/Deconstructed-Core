package dev.feldsher.deconstructedcore.upgrades;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public interface IInsertResponseUpgrade {
	ItemStack onBeforeInsert(IItemHandler inventoryHandler, int slot, ItemStack stack, boolean simulate);

	void onAfterInsert(IItemHandler inventoryHandler, int slot);
}
