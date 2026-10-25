package dev.feldsher.deconstructedcore.inventory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

/** An inventory that also takes a stack into itself as a whole, putting it where it belongs. */
public interface IItemHandlerSimpleInserter extends IItemHandlerModifiable {
	ItemStack insertItem(ItemStack stack, boolean simulate);
}
