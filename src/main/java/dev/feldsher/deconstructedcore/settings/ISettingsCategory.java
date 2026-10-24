package dev.feldsher.deconstructedcore.settings;

import net.minecraft.nbt.NBTTagCompound;

public interface ISettingsCategory<T extends ISettingsCategory<?>> {
	void reloadFrom(NBTTagCompound categoryNbt);

	void overwriteWith(T otherCategory);

	boolean isLargerThanNumberOfSlots(int slots);

	void copyTo(T otherCategory, int startFromSlot, int slotOffset);

	void deleteSlotSettingsFrom(int slotIndex);
}
