package dev.feldsher.deconstructedcore.util;

import dev.feldsher.deconstructedcore.inventory.FilteredItemHandler;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.inventory.ItemStackKey;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class InventoryHelper {
	private InventoryHelper() {}

	/** The handler's stacks put together, each item with its whole count, but for the slots left out. */
	public static Map<ItemStackKey, Integer> getCompactedStacks(IItemHandler handler, Set<Integer> ignoreSlots) {
		Map<ItemStackKey, Integer> ret = new HashMap<>();
		iterate(handler, (slot, stack) -> {
			if (stack.isEmpty() || ignoreSlots.contains(slot)) {
				return;
			}
			ret.merge(ItemStackKey.of(stack), stack.getCount(), Integer::sum);
		});
		return ret;
	}

	public static void iterate(IItemHandler handler, BiConsumer<Integer, ItemStack> actOn) {
		iterate(handler, actOn, () -> false);
	}

	public static void iterate(IItemHandler handler, BiConsumer<Integer, ItemStack> actOn, BooleanSupplier shouldExit) {
		int slots = handler.getSlots();
		for (int slot = 0; slot < slots; slot++) {
			actOn.accept(slot, handler.getStackInSlot(slot));
			if (shouldExit.getAsBoolean()) {
				break;
			}
		}
	}

	public static boolean hasItem(IItemHandler handler, Predicate<ItemStack> matches) {
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			if (matches.test(handler.getStackInSlot(slot))) {
				return true;
			}
		}
		return false;
	}

	public static void copyTo(IItemHandler handler, net.minecraftforge.items.IItemHandlerModifiable otherHandler) {
		for (int slot = 0; slot < handler.getSlots() && slot < otherHandler.getSlots(); slot++) {
			otherHandler.setStackInSlot(slot, handler.getStackInSlot(slot).copy());
		}
	}

	/** Puts the stack into the handler, and what doesn't fit into the player's inventory or at their feet. */
	public static void insertOrDropItem(EntityPlayer player, ItemStack stack, IItemHandler handler) {
		ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, stack, false);
		if (!remaining.isEmpty()) {
			ItemHandlerHelper.giveItemToPlayer(player, remaining);
		}
	}

	/**
	 * Into the whole handler: a storage inventory's own way, else onto the stacks of the same item first and only then
	 * into empty slots, wherever in the handler they are.
	 */
	public static ItemStack insertIntoInventory(ItemStack stack, IItemHandler inventory, boolean simulate) {
		if (inventory instanceof InventoryHandler) {
			return ((InventoryHandler) inventory).insertItem(stack, simulate);
		}
		if (inventory instanceof FilteredItemHandler.Modifiable) {
			return ((FilteredItemHandler.Modifiable) inventory).insertItem(stack, simulate);
		}
		return ItemHandlerHelper.insertItemStacked(inventory, stack.copy(), simulate);
	}

	/** All that handler B takes from handler A, a stack at most at a time; each moved stack is reported. */
	public static void transfer(IItemHandler handlerA, IItemHandler handlerB, Consumer<Supplier<ItemStack>> onInserted) {
		int slotsA = handlerA.getSlots();
		for (int slot = 0; slot < slotsA; slot++) {
			ItemStack slotStack = handlerA.getStackInSlot(slot);
			if (slotStack.isEmpty()) {
				continue;
			}

			int countToTransfer = slotStack.getCount();
			while (countToTransfer > 0) {
				ItemStack toInsert = slotStack.copy();
				toInsert.setCount(Math.min(slotStack.getMaxStackSize(), countToTransfer));
				ItemStack remainingAfterInsert = insertIntoInventory(toInsert, handlerB, true);
				if (remainingAfterInsert.getCount() == toInsert.getCount()) {
					break;
				}
				int toExtract = toInsert.getCount() - remainingAfterInsert.getCount();

				ItemStack extractedStack = handlerA.extractItem(slot, toExtract, true);
				if (extractedStack.isEmpty()) {
					break;
				}

				insertIntoInventory(handlerA.extractItem(slot, extractedStack.getCount(), false), handlerB, false);

				onInserted.accept(() -> {
					ItemStack copiedStack = slotStack.copy();
					copiedStack.setCount(extractedStack.getCount());
					return copiedStack;
				});
				countToTransfer -= extractedStack.getCount();
			}
		}
	}

	public static Set<ItemStackKey> getUniqueStacks(IItemHandler handler) {
		Set<ItemStackKey> uniqueStacks = new HashSet<>();
		iterate(handler, (slot, stack) -> {
			if (!stack.isEmpty()) {
				uniqueStacks.add(ItemStackKey.of(stack));
			}
		});
		return uniqueStacks;
	}

	/** All of the stacks into the handler, what doesn't fit returned; simulated, into a copy of the handler. */
	public static List<ItemStack> insertIntoInventory(List<ItemStack> stacks, IItemHandler inventory, boolean simulate) {
		if (stacks.isEmpty()) {
			return stacks;
		}
		IItemHandler targetInventory = simulate ? cloneInventory(inventory) : inventory;
		List<ItemStack> remaining = new ArrayList<>();
		for (ItemStack stack : stacks) {
			ItemStack result = insertIntoInventory(stack, targetInventory, false);
			if (!result.isEmpty()) {
				remaining.add(result);
			}
		}
		return remaining;
	}

	public static IItemHandler cloneInventory(IItemHandler inventory) {
		ItemStackHandler cloned = new ItemStackHandler(inventory.getSlots());
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			cloned.setStackInSlot(slot, inventory.getStackInSlot(slot).copy());
		}
		return cloned;
	}

	/** How many of the filter's item the handler lacks to hold the expected count of it. */
	public static int getCountMissingInHandler(IItemHandler itemHandler, ItemStack filter, int expectedCount) {
		int missingCount = expectedCount;
		for (int slot = 0; slot < itemHandler.getSlots() && missingCount > 0; slot++) {
			ItemStack stack = itemHandler.getStackInSlot(slot);
			if (ItemHandlerHelper.canItemStacksStack(stack, filter)) {
				missingCount -= Math.min(stack.getCount(), missingCount);
			}
		}
		return missingCount;
	}

	/** As many of the stack, by its count, as the handler has, from whichever slots hold it. */
	public static ItemStack extractFromInventory(ItemStack stack, IItemHandler inventory, boolean simulate) {
		int extractedCount = 0;
		int slots = inventory.getSlots();
		for (int slot = 0; slot < slots && extractedCount < stack.getCount(); slot++) {
			ItemStack slotStack = inventory.getStackInSlot(slot);
			if (ItemHandlerHelper.canItemStacksStack(stack, slotStack)) {
				int toExtract = Math.min(slotStack.getCount(), stack.getCount() - extractedCount);
				extractedCount += inventory.extractItem(slot, toExtract, simulate).getCount();
			}
		}

		if (extractedCount == 0) {
			return ItemStack.EMPTY;
		}
		return ItemHandlerHelper.copyStackWithSize(stack, extractedCount);
	}
}
