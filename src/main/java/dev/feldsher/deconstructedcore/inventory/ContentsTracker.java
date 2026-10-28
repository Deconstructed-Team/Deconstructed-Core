package dev.feldsher.deconstructedcore.inventory;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * What an inventory holds, by stack and by item, and how many of its slots are empty, kept as its slots change: a
 * controller asks each of its storages these as it puts items away, and they mustn't each look through every slot.
 */
class ContentsTracker {
	private final ItemStackKey[] slotKeys;
	private final Map<ItemStackKey, Integer> stackSlots = new HashMap<>();
	private final Map<Item, Integer> itemSlots = new HashMap<>();
	private int emptySlots = 0;

	ContentsTracker(int slots, IntFunction<ItemStack> getSlotStack) {
		slotKeys = new ItemStackKey[slots];
		for (int slot = 0; slot < slots; slot++) {
			ItemStack stack = getSlotStack.apply(slot);
			if (stack.isEmpty()) {
				emptySlots++;
			} else {
				add(slot, ItemStackKey.of(stack));
			}
		}
	}

	void onSlotChanged(int slot, ItemStack stack) {
		if (slot < 0 || slot >= slotKeys.length) {
			return;
		}
		ItemStackKey previous = slotKeys[slot];
		if (previous == null ? stack.isEmpty() : previous.matches(stack)) {
			return;
		}
		if (previous == null) {
			emptySlots--;
		} else {
			remove(slot, previous);
		}
		if (stack.isEmpty()) {
			emptySlots++;
		} else {
			add(slot, ItemStackKey.of(stack));
		}
	}

	private void add(int slot, ItemStackKey key) {
		slotKeys[slot] = key;
		stackSlots.merge(key, 1, Integer::sum);
		itemSlots.merge(key.getStack().getItem(), 1, Integer::sum);
	}

	private void remove(int slot, ItemStackKey key) {
		slotKeys[slot] = null;
		decrement(stackSlots, key);
		decrement(itemSlots, key.getStack().getItem());
	}

	private static <K> void decrement(Map<K, Integer> counts, K key) {
		@Nullable
		Integer count = counts.get(key);
		if (count == null || count <= 1) {
			counts.remove(key);
		} else {
			counts.put(key, count - 1);
		}
	}

	boolean hasStack(ItemStack stack) {
		return stackSlots.containsKey(ItemStackKey.of(stack));
	}

	boolean hasItem(Item item) {
		return itemSlots.containsKey(item);
	}

	boolean hasEmptySlots() {
		return emptySlots > 0;
	}
}
