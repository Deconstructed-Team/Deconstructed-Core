package dev.feldsher.deconstructedcore.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Optional;
import java.util.UUID;

/**
 * The tags a storage stack carries: what 1.21 keeps in data components. Each key is the component's name.
 */
public class StorageNbt {
	public static final String NUMBER_OF_INVENTORY_SLOTS = "number_of_inventory_slots";
	public static final String NUMBER_OF_UPGRADE_SLOTS = "number_of_upgrade_slots";
	public static final String MAIN_COLOR = "main_color";
	public static final String ACCENT_COLOR = "accent_color";
	public static final String STORAGE_UUID = "storage_uuid";
	public static final String COLUMNS_TAKEN = "columns_taken";

	private StorageNbt() {}

	public static NBTTagCompound getOrCreateTag(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null) {
			tag = new NBTTagCompound();
			stack.setTagCompound(tag);
		}
		return tag;
	}

	public static int getInt(ItemStack stack, String key, int defaultValue) {
		NBTTagCompound tag = stack.getTagCompound();
		return tag != null && tag.hasKey(key) ? tag.getInteger(key) : defaultValue;
	}

	public static Optional<UUID> getStorageUuid(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return tag != null && tag.hasUniqueId(STORAGE_UUID) ? Optional.ofNullable(tag.getUniqueId(STORAGE_UUID)) : Optional.empty();
	}

	public static void setStorageUuid(ItemStack stack, UUID uuid) {
		getOrCreateTag(stack).setUniqueId(STORAGE_UUID, uuid);
	}

	public static void removeStorageUuid(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag != null) {
			tag.removeTag(STORAGE_UUID + "Most");
			tag.removeTag(STORAGE_UUID + "Least");
			if (tag.isEmpty()) {
				stack.setTagCompound(null);
			}
		}
	}
}
