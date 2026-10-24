package dev.feldsher.deconstructedcore.common.gui;

import net.minecraft.nbt.NBTTagCompound;

import java.util.function.Supplier;

public interface IServerUpdater {
	void sendBooleanToServer(String key, boolean value);

	void sendDataToServer(Supplier<NBTTagCompound> supplyData);
}
