package dev.feldsher.deconstructedcore.api;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;
import java.util.function.UnaryOperator;

/**
 * Shows what an upgrade does on its storage, as a furnace's flames, from the upgrade's render data. The offsets it
 * picks are from the middle of the storage's back face, outwards; the storage turns them into world positions. Client side.
 */
public interface IUpgradeRenderer {
	void render(World world, Random rand, UnaryOperator<Vec3d> getPositionFromOffset, NBTTagCompound renderData);
}
