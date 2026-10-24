package dev.feldsher.deconstructedcore.settings;

import dev.feldsher.deconstructedcore.settings.main.MainSettingsCategory;
import dev.feldsher.deconstructedcore.network.PacketHandler;
import dev.feldsher.deconstructedcore.network.SyncPlayerSettingsMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The main settings, and where their values come from: a storage's own value where it set one, otherwise the player's,
 * otherwise the default. The player's are kept in the player's persisted data, which outlives death as 1.21's copied tag does.
 */
public class SettingsManager {
	private SettingsManager() {}

	private static final Map<String, MainSetting<?>> settings = new HashMap<>();

	public static final MainSetting<Boolean> SHIFT_CLICK_INTO_OPEN_TAB_FIRST = MainSetting.ofBoolean("shiftClickOpenTab", true);
	public static final MainSetting<Boolean> KEEP_TAB_OPEN = MainSetting.ofBoolean("keepTabOpen", true);
	public static final MainSetting<Boolean> KEEP_SEARCH_PHRASE = MainSetting.ofBoolean("keepSearchPhrase", true);
	public static final MainSetting<String> SEARCH_PHRASE = MainSetting.ofString("searchPhrase", "");

	static {
		addSetting(SHIFT_CLICK_INTO_OPEN_TAB_FIRST);
		addSetting(KEEP_TAB_OPEN);
		addSetting(KEEP_SEARCH_PHRASE);
		addSetting(SEARCH_PHRASE);
	}

	public static void addSetting(MainSetting<?> setting) {
		settings.put(setting.getName(), setting);
	}

	public static Optional<MainSetting<?>> getSetting(String settingName) {
		return Optional.ofNullable(settings.get(settingName));
	}

	public static <T> T getPlayerSettingOrDefault(EntityPlayer player, String playerSettingsTagName, MainSetting<T> setting) {
		return getPlayerSetting(player, playerSettingsTagName, setting).orElse(setting.getDefaultValue());
	}

	public static <T> Optional<T> getPlayerSetting(EntityPlayer player, String playerSettingsTagName, MainSetting<T> setting) {
		return setting.getValue(getPlayerSettingsTag(player, playerSettingsTagName));
	}

	private static NBTTagCompound getPersistedData(EntityPlayer player) {
		NBTTagCompound entityData = player.getEntityData();
		if (!entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
			entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
		}
		return entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
	}

	public static NBTTagCompound getPlayerSettingsTag(EntityPlayer player, String playerSettingsTagName) {
		NBTTagCompound persistedData = getPersistedData(player);
		if (!persistedData.hasKey(playerSettingsTagName)) {
			persistedData.setTag(playerSettingsTagName, new NBTTagCompound());
		}
		return persistedData.getCompoundTag(playerSettingsTagName);
	}

	public static void setPlayerSettingsTag(EntityPlayer player, String playerSettingsTagName, NBTTagCompound settingsNbt) {
		getPersistedData(player).setTag(playerSettingsTagName, settingsNbt);
	}

	/** The client's player has none of the player's settings but those the server sends it: on joining, respawning and changing dimension. */
	public static void syncPlayerSettings(EntityPlayerMP player, String playerSettingsTagName) {
		PacketHandler.INSTANCE.sendTo(new SyncPlayerSettingsMessage(playerSettingsTagName, getPlayerSettingsTag(player, playerSettingsTagName)), player);
	}

	public static <T> void setPlayerSetting(EntityPlayer player, String playerSettingsTagName, MainSetting<T> setting, T value) {
		if (!Objects.equals(value, setting.getDefaultValue())) {
			setting.setValue(getPlayerSettingsTag(player, playerSettingsTagName), value);
		} else {
			setting.removeFrom(getPlayerSettingsTag(player, playerSettingsTagName));
		}
	}

	/** The storage keeps its own value only where it differs from the player's. */
	public static <T> void setSetting(EntityPlayer player, String playerSettingsTagName, MainSettingsCategory<?> category, MainSetting<T> setting, T value) {
		T playerSettingValue = getPlayerSettingOrDefault(player, playerSettingsTagName, setting);
		if (!Objects.equals(playerSettingValue, value)) {
			category.setSettingValue(setting, value);
		} else {
			category.removeSetting(setting);
		}
	}

	public static <T> T getSettingValue(EntityPlayer player, String playerSettingsTagName, MainSettingsCategory<?> category, MainSetting<T> setting) {
		return category.getSettingValue(setting).orElseGet(() -> getPlayerSettingOrDefault(player, playerSettingsTagName, setting));
	}
}
