package dev.feldsher.deconstructedcore.common.gui;

import net.minecraft.nbt.NBTTagCompound;

/** A container that takes data from its screen. */
public interface ISyncedContainer {
	void handlePacket(NBTTagCompound data);
}
