package dev.feldsher.deconstructedcore.settings.main;

import dev.feldsher.deconstructedcore.common.gui.SettingsContainerMenu;
import dev.feldsher.deconstructedcore.settings.MainSetting;
import dev.feldsher.deconstructedcore.settings.SettingsContainerBase;
import dev.feldsher.deconstructedcore.settings.SettingsManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * The main settings, of the player or of the storage. A change applies on the client at once and is sent to the
 * server, which applies it too; the player's settings reach the client again from the server when the player joins.
 */
public class MainSettingsContainer extends SettingsContainerBase<MainSettingsCategory<?>> {
	private static final String CONTEXT_TAG = "context";
	private Context context = Context.PLAYER;

	public MainSettingsContainer(SettingsContainerMenu<?> settingsContainer, String categoryName, MainSettingsCategory<?> category) {
		super(settingsContainer, categoryName, category);
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(CONTEXT_TAG)) {
			context = Context.fromId(data.getInteger(CONTEXT_TAG));
		} else {
			for (String tagName : data.getKeySet()) {
				SettingsManager.getSetting(tagName).ifPresent(setting -> setSettingValue(getPlayer(), setting, data));
			}
		}
	}

	public void toggleContext() {
		context = context == Context.PLAYER ? Context.STORAGE : Context.PLAYER;
		sendIntToServer(CONTEXT_TAG, context.getId());
	}

	public Context getContext() {
		return context;
	}

	protected EntityPlayer getPlayer() {
		return getSettingsContainer().getPlayer();
	}

	public void toggleShiftClickIntoOpenTab() {
		toggleBooleanSetting(SettingsManager.SHIFT_CLICK_INTO_OPEN_TAB_FIRST);
	}

	public boolean shouldShiftClickIntoOpenTab() {
		return getSettingValue(SettingsManager.SHIFT_CLICK_INTO_OPEN_TAB_FIRST);
	}

	public void toggleKeepTabOpen() {
		toggleBooleanSetting(SettingsManager.KEEP_TAB_OPEN);
	}

	public boolean shouldKeepTabOpen() {
		return getSettingValue(SettingsManager.KEEP_TAB_OPEN);
	}

	protected <S> S getSettingValue(MainSetting<S> setting) {
		String playerSettingsTagName = getCategory().getPlayerSettingsTagName();
		if (context == Context.PLAYER) {
			return SettingsManager.getPlayerSettingOrDefault(getPlayer(), playerSettingsTagName, setting);
		}
		return SettingsManager.getSettingValue(getPlayer(), playerSettingsTagName, getCategory(), setting);
	}

	private <S> void setSettingValue(EntityPlayer player, MainSetting<S> setting, NBTTagCompound data) {
		setting.getValue(data).ifPresent(value -> setSettingValue(player, setting, value));
	}

	private <S> void setSettingValue(EntityPlayer player, MainSetting<S> setting, S value) {
		String playerSettingsTagName = getCategory().getPlayerSettingsTagName();
		if (context == Context.PLAYER) {
			SettingsManager.setPlayerSetting(player, playerSettingsTagName, setting, value);
		} else {
			SettingsManager.setSetting(player, playerSettingsTagName, getCategory(), setting, value);
		}
	}

	protected void toggleBooleanSetting(MainSetting<Boolean> setting) {
		boolean value = !getSettingValue(setting);
		setSettingValue(getPlayer(), setting, value);
		NBTTagCompound data = new NBTTagCompound();
		setting.setValue(data, value);
		sendDataToServer(() -> data);
	}
}
