package dev.feldsher.deconstructedcore.client;

import dev.feldsher.deconstructedcore.settings.SettingsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public final class ClientPlayerSettings {
	private ClientPlayerSettings() {}

	public static void set(String playerSettingsTagName, NBTTagCompound settings) {
		EntityPlayer player = Minecraft.getMinecraft().player;
		if (player != null) {
			SettingsManager.setPlayerSettingsTag(player, playerSettingsTagName, settings);
		}
	}
}
