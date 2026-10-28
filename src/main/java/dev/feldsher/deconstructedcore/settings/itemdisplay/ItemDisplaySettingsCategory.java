package dev.feldsher.deconstructedcore.settings.itemdisplay;

import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.renderdata.DisplaySide;
import dev.feldsher.deconstructedcore.renderdata.RenderInfo;
import dev.feldsher.deconstructedcore.settings.ISettingsCategory;
import dev.feldsher.deconstructedcore.settings.ISlotColorCategory;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The slots whose items the storage shows on itself, each turned by its own rotation. The storage's stack carries what
 * this builds ({@link #buildItemDisplayRenderInfo}), for clients that don't have the contents.
 */
public class ItemDisplaySettingsCategory implements ISettingsCategory<ItemDisplaySettingsCategory>, ISlotColorCategory {
	public static final String NAME = "item_display";
	private static final String SLOTS_TAG = "slots";
	private static final String ROTATIONS_TAG = "rotations";
	private static final String COLOR_TAG = "color";
	private static final String DISPLAY_SIDE_TAG = "displaySide";
	private final Supplier<InventoryHandler> inventoryHandlerSupplier;
	private NBTTagCompound categoryNbt;
	private final Consumer<NBTTagCompound> saveNbt;
	private final int itemNumberLimit;
	private final Supplier<MemorySettingsCategory> getMemorySettings;
	private EnumDyeColor color = EnumDyeColor.RED;
	private final List<Integer> slotIndexes = new LinkedList<>();
	private final Map<Integer, Integer> slotRotations = new HashMap<>();
	private DisplaySide displaySide = DisplaySide.FRONT;

	public ItemDisplaySettingsCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, NBTTagCompound categoryNbt, Consumer<NBTTagCompound> saveNbt,
			int itemNumberLimit, Supplier<MemorySettingsCategory> getMemorySettings) {
		this.inventoryHandlerSupplier = inventoryHandlerSupplier;
		this.categoryNbt = categoryNbt;
		this.saveNbt = saveNbt;
		this.itemNumberLimit = itemNumberLimit;
		this.getMemorySettings = getMemorySettings;
		deserialize();
	}

	private void deserialize() {
		slotIndexes.clear();
		for (int slot : categoryNbt.getIntArray(SLOTS_TAG)) {
			slotIndexes.add(slot);
		}
		slotRotations.clear();
		NBTTagCompound rotations = categoryNbt.getCompoundTag(ROTATIONS_TAG);
		for (String key : rotations.getKeySet()) {
			try {
				slotRotations.put(Integer.valueOf(key), rotations.getInteger(key));
			} catch (NumberFormatException ignored) {
				// not a slot number: left out
			}
		}
		color = categoryNbt.hasKey(COLOR_TAG) ? EnumDyeColor.byMetadata(categoryNbt.getInteger(COLOR_TAG)) : EnumDyeColor.RED;
		displaySide = categoryNbt.hasKey(DISPLAY_SIDE_TAG) ? DisplaySide.fromName(categoryNbt.getString(DISPLAY_SIDE_TAG)) : DisplaySide.FRONT;
	}

	public int getItemNumberLimit() {
		return itemNumberLimit;
	}

	/** What the selected slots show: their item, or for an empty slot what it remembers, one of it. */
	public RenderInfo.ItemDisplayRenderInfo buildItemDisplayRenderInfo() {
		return buildItemDisplayRenderInfo(false);
	}

	/** With every slot's count and fill ratio, for a storage that shows them, as a limited barrel does. */
	public RenderInfo.ItemDisplayRenderInfo buildItemDisplayRenderInfo(boolean withCountsAndFillRatios) {
		List<RenderInfo.DisplayItem> displayItems = new ArrayList<>();
		List<Integer> inaccessibleSlots = new ArrayList<>();
		InventoryHandler inventoryHandler = inventoryHandlerSupplier.get();
		for (int slotIndex : slotIndexes) {
			getSlotItemCopy(slotIndex).ifPresent(item -> displayItems.add(new RenderInfo.DisplayItem(item, getRotation(slotIndex), slotIndex, displaySide)));
			if (slotIndex < inventoryHandler.getSlots() && !inventoryHandler.isSlotAccessible(slotIndex)) {
				inaccessibleSlots.add(slotIndex);
			}
		}
		List<Integer> infiniteSlots = new ArrayList<>();
		List<Integer> slotCounts = new ArrayList<>();
		List<Float> slotFillRatios = new ArrayList<>();
		if (withCountsAndFillRatios) {
			for (int slotIndex = 0; slotIndex < inventoryHandler.getSlots(); slotIndex++) {
				ItemStack stack = inventoryHandler.getStackInSlot(slotIndex);
				slotCounts.add(stack.getCount());
				slotFillRatios.add(stack.isEmpty() ? 0 : (float) stack.getCount() / inventoryHandler.getStackLimit(slotIndex, stack));
				if (inventoryHandler.isInfinite(slotIndex)) {
					infiniteSlots.add(slotIndex);
				}
			}
		}
		return new RenderInfo.ItemDisplayRenderInfo(displayItems, inaccessibleSlots, infiniteSlots, slotCounts, slotFillRatios);
	}

	private Optional<ItemStack> getSlotItemCopy(int slotIndex) {
		InventoryHandler inventoryHandler = inventoryHandlerSupplier.get();
		if (slotIndex >= inventoryHandler.getSlots()) {
			return Optional.empty();
		}
		ItemStack slotStack = inventoryHandler.getStackInSlot(slotIndex);
		if (slotStack.isEmpty()) {
			if (inventoryHandler.getFilterItem(slotIndex) != Items.AIR) {
				return Optional.of(new ItemStack(inventoryHandler.getFilterItem(slotIndex)));
			}
			return getMemorySettings.get().getSlotFilterStack(slotIndex, true);
		}
		ItemStack stackCopy = slotStack.copy();
		stackCopy.setCount(1);
		return Optional.of(stackCopy);
	}

	public void selectSlot(int slotIndex) {
		if (slotIndexes.contains(slotIndex) || slotIndexes.size() + 1 > itemNumberLimit) {
			return;
		}
		slotIndexes.add(slotIndex);
		serializeSlotIndexes();
	}

	public void unselectSlot(int slotIndex) {
		slotIndexes.remove((Integer) slotIndex);
		slotRotations.remove(slotIndex);
		serializeSlotIndexes();
		serializeRotations();
	}

	private void serializeSlotIndexes() {
		categoryNbt.setIntArray(SLOTS_TAG, slotIndexes.stream().mapToInt(Integer::intValue).toArray());
		saveNbt.accept(categoryNbt);
	}

	public List<Integer> getSlots() {
		return slotIndexes;
	}

	public int getRotation(int slotIndex) {
		return slotRotations.getOrDefault(slotIndex, 0);
	}

	/** By 45 degrees. */
	public void rotate(int slotIndex, boolean clockwise) {
		if (!slotIndexes.contains(slotIndex)) {
			return;
		}
		int rotation = (getRotation(slotIndex) + (clockwise ? 1 : -1) * 45 + 360) % 360;
		slotRotations.put(slotIndex, rotation);
		serializeRotations();
	}

	private void serializeRotations() {
		NBTTagCompound rotations = new NBTTagCompound();
		slotRotations.forEach((slot, rotation) -> rotations.setInteger(String.valueOf(slot), rotation));
		categoryNbt.setTag(ROTATIONS_TAG, rotations);
		saveNbt.accept(categoryNbt);
	}

	public void setColor(EnumDyeColor color) {
		this.color = color;
		categoryNbt.setInteger(COLOR_TAG, color.getMetadata());
		saveNbt.accept(categoryNbt);
	}

	public EnumDyeColor getColor() {
		return color;
	}

	public DisplaySide getDisplaySide() {
		return displaySide;
	}

	public void setDisplaySide(DisplaySide displaySide) {
		this.displaySide = displaySide;
		categoryNbt.setString(DISPLAY_SIDE_TAG, displaySide.getSerializedName());
		saveNbt.accept(categoryNbt);
	}

	@Override
	public Optional<Integer> getSlotColor(int slotNumber) {
		return slotIndexes.contains(slotNumber) ? Optional.of(color.getColorValue()) : Optional.empty();
	}

	@Override
	public void reloadFrom(NBTTagCompound categoryNbt) {
		this.categoryNbt = categoryNbt;
		deserialize();
	}

	@Override
	public void overwriteWith(ItemDisplaySettingsCategory otherCategory) {
		slotIndexes.clear();
		slotIndexes.addAll(otherCategory.getSlots());
		serializeSlotIndexes();
		slotRotations.clear();
		slotRotations.putAll(otherCategory.slotRotations);
		serializeRotations();
		setColor(otherCategory.getColor());
	}

	@Override
	public boolean isLargerThanNumberOfSlots(int slots) {
		return slotIndexes.stream().anyMatch(slotIndex -> slotIndex >= slots);
	}

	/** The other storage keeps the items it shows. */
	@Override
	public void copyTo(ItemDisplaySettingsCategory otherCategory, int startFromSlot, int slotOffset) {
		//noop
	}

	@Override
	public void deleteSlotSettingsFrom(int slotIndex) {
		slotIndexes.removeIf(slot -> slot >= slotIndex);
		slotRotations.keySet().removeIf(slot -> slot >= slotIndex);
		serializeSlotIndexes();
		serializeRotations();
	}
}
