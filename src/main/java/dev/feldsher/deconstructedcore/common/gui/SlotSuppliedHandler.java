package dev.feldsher.deconstructedcore.common.gui;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/** A slot of a handler that may be replaced while the container is open, such as an upgrade wrapper's. */
public class SlotSuppliedHandler extends SlotItemHandler {
	private final Supplier<IItemHandler> itemHandlerSupplier;
	private final int slot;

	public SlotSuppliedHandler(Supplier<IItemHandler> itemHandlerSupplier, int slot, int xPosition, int yPosition) {
		super(itemHandlerSupplier.get(), slot, xPosition, yPosition);
		this.itemHandlerSupplier = itemHandlerSupplier;
		this.slot = slot;
	}

	@Override
	public IItemHandler getItemHandler() {
		return itemHandlerSupplier.get();
	}

	@Override
	public boolean isItemValid(@Nonnull ItemStack stack) {
		return itemHandlerSupplier.get().isItemValid(slot, stack);
	}

	/** Clicks and merges grow and shrink the slot's stack in place; setting it again lets the handler save it. */
	@Override
	public void onSlotChanged() {
		super.onSlotChanged();
		IItemHandler handler = itemHandlerSupplier.get();
		if (handler instanceof IItemHandlerModifiable) {
			((IItemHandlerModifiable) handler).setStackInSlot(slot, handler.getStackInSlot(slot));
		}
	}

	@Override
	public int getSlotStackLimit() {
		return itemHandlerSupplier.get().getSlotLimit(slot);
	}
}
