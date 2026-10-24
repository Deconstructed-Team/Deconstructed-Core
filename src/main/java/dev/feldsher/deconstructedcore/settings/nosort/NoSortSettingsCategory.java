package dev.feldsher.deconstructedcore.settings.nosort;

import dev.feldsher.deconstructedcore.settings.ISettingsCategory;
import dev.feldsher.deconstructedcore.settings.ISlotColorCategory;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** The slots sorting leaves as they are. */
public class NoSortSettingsCategory implements ISettingsCategory<NoSortSettingsCategory>, ISlotColorCategory {
	public static final String NAME = "no_sort";
	private static final String COLOR_TAG = "color";
	private static final String SELECTED_SLOTS_TAG = "selectedSlots";
	private NBTTagCompound categoryNbt;
	private final Consumer<NBTTagCompound> saveNbt;
	private final Set<Integer> selectedSlots = new HashSet<>();
	private EnumDyeColor color = EnumDyeColor.LIME;

	public NoSortSettingsCategory(NBTTagCompound categoryNbt, Consumer<NBTTagCompound> saveNbt) {
		this.categoryNbt = categoryNbt;
		this.saveNbt = saveNbt;
		deserialize();
	}

	private void deserialize() {
		for (int slotNumber : categoryNbt.getIntArray(SELECTED_SLOTS_TAG)) {
			selectedSlots.add(slotNumber);
		}
		if (categoryNbt.hasKey(COLOR_TAG)) {
			color = EnumDyeColor.byMetadata(categoryNbt.getInteger(COLOR_TAG));
		}
	}

	public boolean isSlotSelected(int slotNumber) {
		return selectedSlots.contains(slotNumber);
	}

	public void unselectAllSlots() {
		selectedSlots.clear();
		serializeSelectedSlots();
	}

	/**
	 * @param minSlot inclusive
	 * @param maxSlot exclusive
	 */
	public void selectSlots(int minSlot, int maxSlot) {
		for (int slot = minSlot; slot < maxSlot; slot++) {
			selectedSlots.add(slot);
		}
		serializeSelectedSlots();
	}

	public void selectSlot(int slotNumber) {
		selectSlots(slotNumber, slotNumber + 1);
	}

	public void unselectSlot(int slotNumber) {
		selectedSlots.remove(slotNumber);
		serializeSelectedSlots();
	}

	private void serializeSelectedSlots() {
		categoryNbt.setIntArray(SELECTED_SLOTS_TAG, selectedSlots.stream().mapToInt(Integer::intValue).toArray());
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

	@Override
	public Optional<Integer> getSlotColor(int slotNumber) {
		return selectedSlots.contains(slotNumber) ? Optional.of(color.getColorValue()) : Optional.empty();
	}

	public Set<Integer> getNoSortSlots() {
		return selectedSlots;
	}

	@Override
	public void reloadFrom(NBTTagCompound categoryNbt) {
		this.categoryNbt = categoryNbt;
		selectedSlots.clear();
		color = EnumDyeColor.LIME;
		deserialize();
	}

	@Override
	public void overwriteWith(NoSortSettingsCategory otherCategory) {
		selectedSlots.clear();
		selectedSlots.addAll(otherCategory.getNoSortSlots());
		serializeSelectedSlots();
		setColor(otherCategory.getColor());
	}

	@Override
	public boolean isLargerThanNumberOfSlots(int slots) {
		return selectedSlots.stream().anyMatch(slotIndex -> slotIndex >= slots);
	}

	@Override
	public void copyTo(NoSortSettingsCategory otherCategory, int startFromSlot, int slotOffset) {
		selectedSlots.forEach(slotIndex -> {
			if (slotIndex >= startFromSlot) {
				otherCategory.selectedSlots.add(slotIndex + slotOffset);
			}
		});
		otherCategory.serializeSelectedSlots();
	}

	@Override
	public void deleteSlotSettingsFrom(int slotIndex) {
		selectedSlots.removeIf(slot -> slot >= slotIndex);
		serializeSelectedSlots();
	}
}
