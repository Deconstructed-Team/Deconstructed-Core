package dev.feldsher.deconstructedcore.upgrades;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public interface ITickableUpgrade {
	void tick(@Nullable Entity entity, World world, BlockPos pos);
}
