package dev.feldsher.deconstructedcore.util;

import dev.feldsher.deconstructedcore.common.gui.SortBy;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.inventory.ItemStackKey;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Sorts an inventory: its stacks put together, ordered, and laid out again from the first slot, around the slots left out. */
public class InventorySorter {
	private InventorySorter() {}

	public static final Comparator<Map.Entry<ItemStackKey, Integer>> BY_NAME = Comparator.comparing(o -> o.getKey().getStack().getDisplayName().toLowerCase());
	public static final Comparator<Map.Entry<ItemStackKey, Integer>> BY_MOD =
			Comparator.<Map.Entry<ItemStackKey, Integer>, String>comparing(o -> getRegistryName(o.getKey()).getNamespace())
					.thenComparing(o -> o.getKey().getStack().getDisplayName());

	public static final Comparator<Map.Entry<ItemStackKey, Integer>> BY_COUNT = (first, second) -> {
		int ret = second.getValue().compareTo(first.getValue());
		return ret != 0 ? ret : getRegistryName(first.getKey()).toString().compareTo(getRegistryName(second.getKey()).toString());
	};

	/** 1.21 sorts by item tags; 1.12's counterpart is the ore dictionary: the stacks with more ore names first, then by the names. */
	public static final Comparator<Map.Entry<ItemStackKey, Integer>> BY_TAGS = (first, second) -> {
		ItemStack firstStack = first.getKey().getStack();
		ItemStack secondStack = second.getKey().getStack();
		if (firstStack.getItem() == secondStack.getItem() && firstStack.getMetadata() == secondStack.getMetadata()) {
			return 0;
		}
		int ret = compareOreNames(getSortedOreNames(firstStack), getSortedOreNames(secondStack));
		return ret != 0 ? ret : getRegistryName(first.getKey()).toString().compareTo(getRegistryName(second.getKey()).toString());
	};

	public static Comparator<Map.Entry<ItemStackKey, Integer>> getComparator(SortBy sortBy) {
		switch (sortBy) {
			case COUNT:
				return BY_COUNT;
			case TAGS:
				return BY_TAGS;
			case MOD:
				return BY_MOD;
			default:
				return BY_NAME;
		}
	}

	private static String[] getSortedOreNames(ItemStack stack) {
		if (stack.isEmpty()) {
			return new String[0];
		}
		String[] names = Arrays.stream(OreDictionary.getOreIDs(stack)).mapToObj(OreDictionary::getOreName).toArray(String[]::new);
		Arrays.sort(names);
		return names;
	}

	private static int compareOreNames(String[] firstNames, String[] secondNames) {
		int ret = Integer.compare(secondNames.length, firstNames.length);
		if (ret != 0) {
			return ret;
		}
		for (int i = 0; i < firstNames.length; i++) {
			ret = firstNames[i].compareTo(secondNames[i]);
			if (ret != 0) {
				return ret;
			}
		}
		return 0;
	}

	private static ResourceLocation getRegistryName(ItemStackKey itemStackKey) {
		return Objects.requireNonNull(itemStackKey.getStack().getItem().getRegistryName());
	}

	public static void sortHandler(IItemHandlerModifiable handler, Comparator<? super Map.Entry<ItemStackKey, Integer>> comparator, Set<Integer> noSortSlots) {
		Map<ItemStackKey, Integer> compactedStacks = InventoryHelper.getCompactedStacks(handler, noSortSlots);
		List<Map.Entry<ItemStackKey, Integer>> sortedList = new ArrayList<>(compactedStacks.entrySet());
		sortedList.sort(comparator);

		sortIntoNoSortSlots(handler, noSortSlots, sortedList);
		sortIntoOtherSlots(handler, noSortSlots, sortedList, handler.getSlots());
	}

	private static void sortIntoOtherSlots(IItemHandlerModifiable handler, Set<Integer> noSortSlots, List<Map.Entry<ItemStackKey, Integer>> sortedList, int slots) {
		Iterator<Map.Entry<ItemStackKey, Integer>> ite = sortedList.iterator();
		ItemStackKey current = null;
		int count = 0;

		for (int slot = 0; slot < slots; slot++) {
			if (noSortSlots.contains(slot)) {
				continue;
			}
			if ((current == null || count <= 0) && ite.hasNext()) {
				Map.Entry<ItemStackKey, Integer> entry = ite.next();
				current = entry.getKey();
				count = entry.getValue();
			}
			if (current != null && count > 0) {
				count -= placeStack(handler, current, count, slot, false);
			} else {
				emptySlot(handler, slot);
			}
		}
	}

	/** What the slots left out of sorting already hold more of goes there first, up to their limit. */
	private static void sortIntoNoSortSlots(IItemHandlerModifiable handler, Set<Integer> noSortSlots, List<Map.Entry<ItemStackKey, Integer>> sortedList) {
		if (noSortSlots.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<ItemStackKey, Integer>> it = sortedList.iterator();
		while (it.hasNext()) {
			Map.Entry<ItemStackKey, Integer> entry = it.next();
			ItemStackKey current = entry.getKey();
			int count = entry.getValue();

			for (int slot : noSortSlots) {
				if (current.matches(handler.getStackInSlot(slot))) {
					count -= placeStack(handler, current, count, slot, true);
					entry.setValue(count);
					if (count <= 0) {
						it.remove();
						break;
					}
				}
			}
		}
	}

	private static void emptySlot(IItemHandlerModifiable handler, int slot) {
		if (!handler.getStackInSlot(slot).isEmpty()) {
			if (handler instanceof InventoryHandler) {
				((InventoryHandler) handler).setSlotStack(slot, ItemStack.EMPTY);
			} else {
				handler.setStackInSlot(slot, ItemStack.EMPTY);
			}
		}
	}

	private static int placeStack(IItemHandlerModifiable handler, ItemStackKey current, int count, int slot, boolean countWithCurrentStack) {
		ItemStack copy = current.getStack().copy();
		int slotLimit = handler instanceof InventoryHandler ? ((InventoryHandler) handler).getStackLimit(slot, copy) : handler.getSlotLimit(slot);
		ItemStack slotStack = handler instanceof InventoryHandler ? ((InventoryHandler) handler).getSlotStack(slot) : handler.getStackInSlot(slot);
		int existingCount = slotStack.getCount();
		if (countWithCurrentStack) {
			count += existingCount;
		}
		int countPlaced = Math.min(count, slotLimit);
		copy.setCount(countPlaced);
		if (!ItemStack.areItemStacksEqual(slotStack, copy)) {
			if (handler instanceof InventoryHandler) {
				((InventoryHandler) handler).setSlotStack(slot, copy);
			} else {
				handler.setStackInSlot(slot, copy);
			}
		}
		return countWithCurrentStack ? countPlaced - existingCount : countPlaced;
	}
}
