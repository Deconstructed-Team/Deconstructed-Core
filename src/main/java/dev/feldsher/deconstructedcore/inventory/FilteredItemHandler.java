package dev.feldsher.deconstructedcore.inventory;

import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.List;

/** A handler seen through filters: stacks go in only if an input filter matches, and out only if an output one does. */
public class FilteredItemHandler<T extends IItemHandler> implements IItemHandler {
	protected final T inventoryHandler;
	protected final List<FilterLogic> inputFilters;
	private final List<FilterLogic> outputFilters;

	public FilteredItemHandler(T inventoryHandler, List<FilterLogic> inputFilters, List<FilterLogic> outputFilters) {
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

	protected static boolean matchesAny(List<FilterLogic> filters, ItemStack stack) {
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

	/** A storage's own inventory, filtered, which also takes a stack into the whole inventory at once. */
	public static class Modifiable extends FilteredItemHandler<IItemHandlerSimpleInserter> implements IItemHandlerSimpleInserter {
		public Modifiable(IItemHandlerSimpleInserter inventoryHandler, List<FilterLogic> inputFilters, List<FilterLogic> outputFilters) {
			super(inventoryHandler, inputFilters, outputFilters);
		}

		@Override
		public ItemStack insertItem(ItemStack stack, boolean simulate) {
			return matchesAny(inputFilters, stack) ? inventoryHandler.insertItem(stack, simulate) : stack;
		}

		@Override
		public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
			inventoryHandler.setStackInSlot(slot, stack);
		}
	}
}
