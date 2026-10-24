package dev.feldsher.deconstructedcore.upgrades;

import net.minecraft.item.ItemStack;

public interface IOverflowResponseUpgrade {
	default boolean stackMatchesFilterStack(ItemStack stack, ItemStack filterStack) {
		return getFilterLogic().stackMatchesFilter(stack, filterStack);
	}

	FilterLogic getFilterLogic();

	boolean worksInGui();

	ItemStack onOverflow(ItemStack stack);

	boolean stackMatchesFilter(ItemStack stack);
}
