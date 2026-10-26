package dev.feldsher.deconstructedcore.client.render;

import dev.feldsher.deconstructedcore.api.IUpgradeRenderer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;
import java.util.function.UnaryOperator;

/** A burning cooking upgrade: flames and smoke at the back, and now and then a furnace's crackle. */
public class CookingUpgradeRenderer implements IUpgradeRenderer {
	@Override
	public void render(World world, Random rand, UnaryOperator<Vec3d> getPositionFromOffset, NBTTagCompound renderData) {
		if (rand.nextDouble() < 0.1D) {
			Vec3d center = getPositionFromOffset.apply(Vec3d.ZERO);
			world.playSound(center.x, center.y, center.z, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
		}

		double xOffset = rand.nextFloat() * 0.6f - 0.3f;
		double yOffset = rand.nextFloat() * 6.0f / 16.0f;
		Vec3d randomAtTheBack = getPositionFromOffset.apply(new Vec3d(xOffset, yOffset, 0.02f));
		world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, randomAtTheBack.x, randomAtTheBack.y, randomAtTheBack.z, 0, 0, 0);
		world.spawnParticle(EnumParticleTypes.FLAME, randomAtTheBack.x, randomAtTheBack.y, randomAtTheBack.z, 0, 0, 0);
	}
}
