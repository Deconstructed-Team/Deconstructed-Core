package dev.feldsher.deconstructedcore.settings;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Each player's settings templates, in numbered slots and under names, kept with the world. The client has a copy of
 * its own player's, which the server sends when a settings screen opens.
 */
public class SettingsTemplateStorage extends WorldSavedData {
	private static final String SAVED_DATA_NAME = DeconstructedCore.MOD_ID + "_settings_templates";
	private static final String PLAYER_TEMPLATES_TAG = "playerTemplates";
	private static final String PLAYER_NAMED_TEMPLATES_TAG = "playerNamedTemplates";
	private static final SettingsTemplateStorage CLIENT_STORAGE_COPY = new SettingsTemplateStorage(SAVED_DATA_NAME);
	private final Map<UUID, Map<Integer, NBTTagCompound>> playerTemplates = new HashMap<>();
	private final Map<UUID, Map<String, NBTTagCompound>> playerNamedTemplates = new HashMap<>();

	@SuppressWarnings("WeakerAccess") // instantiated by MapStorage
	public SettingsTemplateStorage(String name) {
		super(name);
	}

	public static SettingsTemplateStorage get(World world) {
		if (world.isRemote) {
			return CLIENT_STORAGE_COPY;
		}
		MapStorage storage = world.getMapStorage();
		//noinspection ConstantConditions - a server world has its map storage
		SettingsTemplateStorage templateStorage = (SettingsTemplateStorage) storage.getOrLoadData(SettingsTemplateStorage.class, SAVED_DATA_NAME);
		if (templateStorage == null) {
			templateStorage = new SettingsTemplateStorage(SAVED_DATA_NAME);
			storage.setData(SAVED_DATA_NAME, templateStorage);
		}
		return templateStorage;
	}

	public void putPlayerTemplate(EntityPlayer player, int slot, NBTTagCompound settingsTag) {
		playerTemplates.computeIfAbsent(player.getUniqueID(), u -> new HashMap<>()).put(slot, settingsTag);
		markDirty();
	}

	public void putPlayerNamedTemplate(EntityPlayer player, String name, NBTTagCompound settingsTag) {
		playerNamedTemplates.computeIfAbsent(player.getUniqueID(), u -> new TreeMap<>()).put(name, settingsTag);
		markDirty();
	}

	public Map<Integer, NBTTagCompound> getPlayerTemplates(EntityPlayer player) {
		return playerTemplates.getOrDefault(player.getUniqueID(), new HashMap<>());
	}

	public Map<String, NBTTagCompound> getPlayerNamedTemplates(EntityPlayer player) {
		return playerNamedTemplates.getOrDefault(player.getUniqueID(), new TreeMap<>());
	}

	public void clearPlayerTemplates(EntityPlayer player) {
		playerTemplates.remove(player.getUniqueID());
		playerNamedTemplates.remove(player.getUniqueID());
		markDirty();
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		playerTemplates.clear();
		playerNamedTemplates.clear();
		NBTTagCompound playerTemplatesTag = tag.getCompoundTag(PLAYER_TEMPLATES_TAG);
		for (String uuid : playerTemplatesTag.getKeySet()) {
			Map<Integer, NBTTagCompound> templates = new HashMap<>();
			NBTTagCompound slotTemplates = playerTemplatesTag.getCompoundTag(uuid);
			for (String slot : slotTemplates.getKeySet()) {
				templates.put(Integer.valueOf(slot), slotTemplates.getCompoundTag(slot));
			}
			playerTemplates.put(UUID.fromString(uuid), templates);
		}
		NBTTagCompound playerNamedTemplatesTag = tag.getCompoundTag(PLAYER_NAMED_TEMPLATES_TAG);
		for (String uuid : playerNamedTemplatesTag.getKeySet()) {
			Map<String, NBTTagCompound> templates = new TreeMap<>();
			NBTTagCompound namedTemplates = playerNamedTemplatesTag.getCompoundTag(uuid);
			for (String name : namedTemplates.getKeySet()) {
				templates.put(name, namedTemplates.getCompoundTag(name));
			}
			playerNamedTemplates.put(UUID.fromString(uuid), templates);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		NBTTagCompound playerTemplatesTag = new NBTTagCompound();
		playerTemplates.forEach((uuid, templates) -> {
			NBTTagCompound slotTemplates = new NBTTagCompound();
			templates.forEach((slot, settingsTag) -> slotTemplates.setTag(String.valueOf(slot), settingsTag));
			playerTemplatesTag.setTag(uuid.toString(), slotTemplates);
		});
		tag.setTag(PLAYER_TEMPLATES_TAG, playerTemplatesTag);
		NBTTagCompound playerNamedTemplatesTag = new NBTTagCompound();
		playerNamedTemplates.forEach((uuid, templates) -> {
			NBTTagCompound namedTemplates = new NBTTagCompound();
			templates.forEach(namedTemplates::setTag);
			playerNamedTemplatesTag.setTag(uuid.toString(), namedTemplates);
		});
		tag.setTag(PLAYER_NAMED_TEMPLATES_TAG, playerNamedTemplatesTag);
		return tag;
	}
}
