package dev.feldsher.deconstructedcore.common.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/** A filter slot: it holds a ghost copy of what is clicked into it, and can't be taken from. */
public class FilterSlotItemHandler extends SlotSuppliedHandler implements IFilterSlot {
	public FilterSlotItemHandler(Supplier<IItemHandler> itemHandlerSupplier, int slot, int xPosition, int yPosition) {
		super(itemHandlerSupplier, slot, xPosition, yPosition);
	}

	@Override
	public boolean canTakeStack(EntityPlayer playerIn) {
		return false;
	}

	@Override
	public int getItemStackLimit(@Nonnull ItemStack stack) {
		return 1;
	}
}
