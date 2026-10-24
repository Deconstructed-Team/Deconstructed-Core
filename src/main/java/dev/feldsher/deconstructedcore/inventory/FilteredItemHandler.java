package dev.feldsher.deconstructedcore.inventory;

import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.List;

/** A storage as machines see it: what goes in has to match an input filter, what comes out an output filter. */
public class FilteredItemHandler implements IItemHandlerModifiable {
	protected final InventoryHandler inventoryHandler;
	protected final List<FilterLogic> inputFilters;
	private final List<FilterLogic> outputFilters;

	public FilteredItemHandler(InventoryHandler inventoryHandler, List<FilterLogic> inputFilters, List<FilterLogic> outputFilters) {
		this.inventoryHandler = inventoryHandler;
		this.inputFilters = inputFilters;
		this.outputFilters = outputFilters;
	}

	@Override
	public int getSlots() {
		return inventoryHandler.getSlots();
	}

	@Nonnull
	@Override
	public ItemStack getStackInSlot(int slot) {
		return inventoryHandler.getStackInSlot(slot);
	}

	private boolean matchesAny(List<FilterLogic> filters, ItemStack stack) {
		if (filters.isEmpty()) {
			return true;
		}
		for (FilterLogic filter : filters) {
			if (filter.matchesFilter(stack)) {
				return true;
			}
		}
		return false;
	}

	@Nonnull
	@Override
	public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
		return matchesAny(inputFilters, stack) ? inventoryHandler.insertItem(slot, stack, simulate) : stack;
	}

	public ItemStack insertItem(ItemStack stack, boolean simulate) {
		return matchesAny(inputFilters, stack) ? inventoryHandler.insertItem(stack, simulate) : stack;
	}

	@Nonnull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return matchesAny(outputFilters, getStackInSlot(slot)) ? inventoryHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
	}

	@Override
	public int getSlotLimit(int slot) {
		return inventoryHandler.getSlotLimit(slot);
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		return inventoryHandler.isItemValid(slot, stack);
	}

	@Override
	public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
		inventoryHandler.setStackInSlot(slot, stack);
	}
}
