package dev.feldsher.deconstructedcore.client.render;

import dev.feldsher.deconstructedcore.api.IUpgradeRenderer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;
import java.util.function.UnaryOperator;

/** A playing jukebox upgrade: notes rising off the back, the vanilla ones, colored by the first speed as a note block's are. */
public class JukeboxUpgradeRenderer implements IUpgradeRenderer {
	@Override
	public void render(World world, Random rand, UnaryOperator<Vec3d> getPositionFromOffset, NBTTagCompound renderData) {
		if (rand.nextInt(2) != 0) {
			return;
		}
		double xOffset = rand.nextFloat() * 0.6f - 0.3f;
		double yOffset = 0.5f + rand.nextFloat() * 6.0f / 16.0f;
		double zOffset = rand.nextFloat() * 0.6f - 0.1f;
		Vec3d randomAtTheBack = getPositionFromOffset.apply(new Vec3d(xOffset, yOffset, zOffset));
		world.spawnParticle(EnumParticleTypes.NOTE, randomAtTheBack.x, randomAtTheBack.y, randomAtTheBack.z, rand.nextFloat(), 0, 0);
	}
}
