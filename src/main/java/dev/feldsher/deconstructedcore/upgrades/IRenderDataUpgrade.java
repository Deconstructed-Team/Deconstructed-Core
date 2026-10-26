package dev.feldsher.deconstructedcore.upgrades;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Map;

/**
 * An upgrade with something to show on its storage while it works, as a furnace's flames: put by its type into the
 * storage's render info, where the client's upgrade renderers find it.
 */
public interface IRenderDataUpgrade {
	void addRenderData(Map<String, NBTTagCompound> renderData);
}
