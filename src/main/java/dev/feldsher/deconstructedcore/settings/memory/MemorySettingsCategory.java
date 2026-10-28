package dev.feldsher.deconstructedcore.settings.memory;

import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.inventory.ItemStackKey;
import dev.feldsher.deconstructedcore.settings.ISettingsCategory;
import dev.feldsher.deconstructedcore.util.GhostStacks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The slots that remember what they held and take only that. Ignoring NBT, a slot remembers the item, with its
 * metadata where the item has subtypes (1.12's wool colors, dyes); matching NBT, it remembers the whole stack.
 */
public class MemorySettingsCategory implements ISettingsCategory<MemorySettingsCategory> {
	public static final String NAME = "memory";
	private static final String SLOT_FILTER_ITEMS_TAG = "slotFilterItems";
	private static final String SLOT_FILTER_STACKS_TAG = "slotFilterStacks";
	private static final String IGNORE_NBT_TAG = "ignoreNbt";
	private final Supplier<InventoryHandler> inventoryHandlerSupplier;
	private NBTTagCompound categoryNbt;
	private final Consumer<NBTTagCompound> saveNbt;
	private final Map<Integer, ItemStackKey> slotFilterItems = new LinkedHashMap<>();
	private final Map<Integer, ItemStackKey> slotFilterStacks = new LinkedHashMap<>();
	private final Map<ItemStackKey, Set<Integer>> filterItemSlots = new HashMap<>();
	private final Map<ItemStackKey, Set<Integer>> filterStackSlots = new HashMap<>();
	private boolean ignoreNbt = true;

	public MemorySettingsCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, NBTTagCompound categoryNbt, Consumer<NBTTagCompound> saveNbt) {
		this.inventoryHandlerSupplier = inventoryHandlerSupplier;
		this.categoryNbt = categoryNbt;
		this.saveNbt = saveNbt;
		deserialize();
	}

	/** What a slot remembers while ignoring NBT: the item, and its metadata only where it tells subtypes apart. */
	private static ItemStackKey itemKey(ItemStack stack) {
		return ItemStackKey.of(new ItemStack(stack.getItem(), 1, stack.getHasSubtypes() ? stack.getMetadata() : 0));
	}

	private void deserialize() {
		readSlotStacks(SLOT_FILTER_ITEMS_TAG, this::addSlotItem);
		readSlotStacks(SLOT_FILTER_STACKS_TAG, this::addSlotStack);
		ignoreNbt = !categoryNbt.hasKey(IGNORE_NBT_TAG) || categoryNbt.getBoolean(IGNORE_NBT_TAG);
	}

	private void readSlotStacks(String tagName, BiConsumer<Integer, ItemStack> addSlot) {
		NBTTagCompound slotsNbt = categoryNbt.getCompoundTag(tagName);
		for (String key : slotsNbt.getKeySet()) {
			ItemStack stack = new ItemStack(slotsNbt.getCompoundTag(key));
			if (!stack.isEmpty()) {
				try {
					addSlot.accept(Integer.valueOf(key), stack);
				} catch (NumberFormatException ignored) {
					// not a slot number: left out
				}
			}
		}
	}

	public boolean matchesFilter(int slotNumber, ItemStack stack) {
		if (slotFilterItems.containsKey(slotNumber)) {
			return !stack.isEmpty() && slotFilterItems.get(slotNumber).equals(itemKey(stack));
		}
		if (slotFilterStacks.containsKey(slotNumber)) {
			return !stack.isEmpty() && slotFilterStacks.get(slotNumber).matches(stack);
		}
		return true;
	}

	/** Whether any slot remembers this stack. */
	public boolean matchesFilter(ItemStack stack) {
		return filterItemSlots.containsKey(itemKey(stack)) || !filterStackSlots.isEmpty() && filterStackSlots.containsKey(ItemStackKey.of(stack));
	}

	public Optional<ItemStack> getSlotFilterStack(int slotNumber, boolean copy) {
		ItemStackKey key = slotFilterItems.containsKey(slotNumber) ? slotFilterItems.get(slotNumber) : slotFilterStacks.get(slotNumber);
		if (key == null) {
			return Optional.empty();
		}
		return Optional.of(copy ? key.getStack().copy() : key.getStack());
	}

	public boolean isSlotSelected(int slotNumber) {
		return slotFilterItems.containsKey(slotNumber) || slotFilterStacks.containsKey(slotNumber);
	}

	public void unselectAllSlots() {
		clearSlots();
		serializeFilterItems();
	}

	private void clearSlots() {
		slotFilterItems.clear();
		filterItemSlots.clear();
		slotFilterStacks.clear();
		filterStackSlots.clear();
	}

	/**
	 * Slots remember what they hold; empty ones what a part of the inventory has set them for, as compression, if
	 * anything.
	 *
	 * @param minSlot inclusive
	 * @param maxSlot exclusive
	 */
	public void selectSlots(int minSlot, int maxSlot) {
		InventoryHandler inventoryHandler = inventoryHandlerSupplier.get();
		for (int slot = minSlot; slot < maxSlot && slot < inventoryHandler.getSlots(); slot++) {
			ItemStack stackInSlot = inventoryHandler.getStackInSlot(slot);
			if (!stackInSlot.isEmpty()) {
				addSlot(slot, stackInSlot);
			} else if (inventoryHandler.getFilterItem(slot) != Items.AIR) {
				addSlot(slot, new ItemStack(inventoryHandler.getFilterItem(slot)));
			}
		}
		serializeFilterItems();
	}

	public void selectSlot(int slotNumber) {
		selectSlots(slotNumber, slotNumber + 1);
	}

	/**
	 * A stack whose tag is too big to keep, as a shulker box full of books, is remembered by its item alone: kept
	 * whole, the settings would grow past what a packet holds, and be sent with every template and every opening.
	 */
	private void addSlot(int slot, ItemStack stack) {
		if (ignoreNbt || !GhostStacks.hasFilterSizedTag(stack)) {
			addSlotItem(slot, stack);
		} else {
			addSlotStack(slot, stack);
		}
	}

	private void addSlotItem(int slot, ItemStack stack) {
		ItemStackKey key = itemKey(stack);
		slotFilterItems.put(slot, key);
		filterItemSlots.computeIfAbsent(key, k -> new TreeSet<>()).add(slot);
	}

	private void addSlotStack(int slot, ItemStack stack) {
		ItemStackKey key = ItemStackKey.of(stack);
		slotFilterStacks.put(slot, key);
		filterStackSlots.computeIfAbsent(key, k -> new TreeSet<>()).add(slot);
	}

	public void unselectSlot(int slotNumber) {
		removeSlot(slotNumber, slotFilterItems, filterItemSlots);
		removeSlot(slotNumber, slotFilterStacks, filterStackSlots);
		serializeFilterItems();
	}

	private static void removeSlot(int slotNumber, Map<Integer, ItemStackKey> slotFilters, Map<ItemStackKey, Set<Integer>> filterSlots) {
		ItemStackKey key = slotFilters.remove(slotNumber);
		if (key == null) {
			return;
		}
		Set<Integer> slots = filterSlots.get(key);
		slots.remove(slotNumber);
		if (slots.isEmpty()) {
			filterSlots.remove(key);
		}
	}

	public boolean ignoresNbt() {
		return ignoreNbt;
	}

	/** The memorized slots switch to remembering the other way: from what they hold, or else what they remembered. */
	public void setIgnoreNbt(boolean ignoreNbt) {
		if (this.ignoreNbt == ignoreNbt) {
			return;
		}
		Map<Integer, ItemStack> remembered = new LinkedHashMap<>();
		InventoryHandler inventoryHandler = inventoryHandlerSupplier.get();
		for (int slot : getSlotIndexes()) {
			ItemStack stackInSlot = slot < inventoryHandler.getSlots() ? inventoryHandler.getStackInSlot(slot) : ItemStack.EMPTY;
			remembered.put(slot, stackInSlot.isEmpty() ? getSlotFilterStack(slot, true).orElse(ItemStack.EMPTY) : stackInSlot);
		}
		clearSlots();
		this.ignoreNbt = ignoreNbt;
		remembered.forEach(this::addSlot);
		serializeIgnoreNbt();
		serializeFilterItems();
	}

	/** Remembers this stack in an empty slot, as when it's dragged in from an item list. */
	public void setFilter(int slot, ItemStack filter) {
		InventoryHandler inventoryHandler = inventoryHandlerSupplier.get();
		if (slot < inventoryHandler.getSlots() && inventoryHandler.getStackInSlot(slot).isEmpty() && !filter.isEmpty()) {
			addSlot(slot, filter);
			serializeFilterItems();
		}
	}

	private void serializeIgnoreNbt() {
		categoryNbt.setBoolean(IGNORE_NBT_TAG, ignoreNbt);
		saveNbt.accept(categoryNbt);
	}

	private void serializeFilterItems() {
		categoryNbt.setTag(SLOT_FILTER_ITEMS_TAG, writeSlotStacks(slotFilterItems));
		categoryNbt.setTag(SLOT_FILTER_STACKS_TAG, writeSlotStacks(slotFilterStacks));
		saveNbt.accept(categoryNbt);
	}

	private static NBTTagCompound writeSlotStacks(Map<Integer, ItemStackKey> slotFilters) {
		NBTTagCompound slotsNbt = new NBTTagCompound();
		slotFilters.forEach((slot, key) -> slotsNbt.setTag(String.valueOf(slot), key.getStack().writeToNBT(new NBTTagCompound())));
		return slotsNbt;
	}

	@Override
	public void reloadFrom(NBTTagCompound categoryNbt) {
		this.categoryNbt = categoryNbt;
		clearSlots();
		deserialize();
	}

	/** The other's memory, for the slots that are empty or hold what it remembers. */
	@Override
	public void overwriteWith(MemorySettingsCategory otherCategory) {
		clearSlots();
		ignoreNbt = otherCategory.ignoreNbt;
		InventoryHandler inventoryHandler = inventoryHandlerSupplier.get();
		Map<Integer, ItemStackKey> otherSlots = ignoreNbt ? otherCategory.slotFilterItems : otherCategory.slotFilterStacks;
		otherSlots.forEach((slot, key) -> {
			if (slot >= inventoryHandler.getSlots()) {
				return;
			}
			ItemStack stackInSlot = inventoryHandler.getStackInSlot(slot);
			if (stackInSlot.isEmpty() || otherCategory.matchesFilter(slot, stackInSlot)) {
				addSlot(slot, key.getStack());
			}
		});
		serializeIgnoreNbt();
		serializeFilterItems();
	}

	public Set<Integer> getSlotIndexes() {
		Set<Integer> slots = new HashSet<>(slotFilterItems.keySet());
		slots.addAll(slotFilterStacks.keySet());
		return slots;
	}

	@Override
	public boolean isLargerThanNumberOfSlots(int slots) {
		return getSlotIndexes().stream().anyMatch(slotIndex -> slotIndex >= slots);
	}

	@Override
	public void copyTo(MemorySettingsCategory otherCategory, int startFromSlot, int slotOffset) {
		slotFilterItems.forEach((slotIndex, key) -> {
			if (slotIndex >= startFromSlot) {
				otherCategory.addSlotItem(slotIndex + slotOffset, key.getStack());
			}
		});
		slotFilterStacks.forEach((slotIndex, key) -> {
			if (slotIndex >= startFromSlot) {
				otherCategory.addSlotStack(slotIndex + slotOffset, key.getStack());
			}
		});
		otherCategory.serializeFilterItems();
	}

	@Override
	public void deleteSlotSettingsFrom(int slotIndex) {
		for (int slot : getSlotIndexes()) {
			if (slot >= slotIndex) {
				removeSlot(slot, slotFilterItems, filterItemSlots);
				removeSlot(slot, slotFilterStacks, filterStackSlots);
			}
		}
		serializeFilterItems();
	}
}
