package dev.feldsher.deconstructedcore.util;

import dev.feldsher.deconstructedcore.inventory.ItemStackKey;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

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
}
