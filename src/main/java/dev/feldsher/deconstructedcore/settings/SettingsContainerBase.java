package dev.feldsher.deconstructedcore.settings;

import dev.feldsher.deconstructedcore.common.gui.SettingsContainerMenu;
import dev.feldsher.deconstructedcore.network.PacketHandler;
import dev.feldsher.deconstructedcore.network.SyncContainerClientDataMessage;
import net.minecraft.nbt.NBTTagCompound;

import java.util.function.Supplier;

/** A settings category in the settings container: what its tab changes, sent from the client to the server. */
public abstract class SettingsContainerBase<C extends ISettingsCategory<?>> {
	private final SettingsContainerMenu<?> settingsContainer;
	private final String categoryName;
	private final C category;

	protected SettingsContainerBase(SettingsContainerMenu<?> settingsContainer, String categoryName, C category) {
		this.settingsContainer = settingsContainer;
		this.categoryName = categoryName;
		this.category = category;
	}

	protected C getCategory() {
		return category;
	}

	public SettingsContainerMenu<?> getSettingsContainer() {
		return settingsContainer;
	}

	public void sendIntToServer(String key, int value) {
		sendDataToServer(() -> {
			NBTTagCompound data = new NBTTagCompound();
			data.setInteger(key, value);
			return data;
		});
	}

	public void sendStringToServer(String key, String value) {
		sendDataToServer(() -> {
			NBTTagCompound data = new NBTTagCompound();
			data.setString(key, value);
			return data;
		});
	}

	public void sendBooleanToServer(String key, boolean value) {
		sendDataToServer(() -> {
			NBTTagCompound data = new NBTTagCompound();
			data.setBoolean(key, value);
			return data;
		});
	}

	public void sendDataToServer(Supplier<NBTTagCompound> supplyData) {
		if (isServer()) {
			return;
		}
		NBTTagCompound data = supplyData.get();
		data.setString("categoryName", categoryName);
		PacketHandler.INSTANCE.sendToServer(new SyncContainerClientDataMessage(data));
	}

	protected boolean isServer() {
		return !settingsContainer.getPlayer().world.isRemote;
	}

	public abstract void handlePacket(NBTTagCompound data);
}
