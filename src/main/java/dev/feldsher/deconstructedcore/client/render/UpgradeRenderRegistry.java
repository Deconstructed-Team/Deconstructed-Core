package dev.feldsher.deconstructedcore.client.render;

import dev.feldsher.deconstructedcore.api.IUpgradeRenderer;
import dev.feldsher.deconstructedcore.upgrades.cooking.CookingLogic;
import dev.feldsher.deconstructedcore.upgrades.jukebox.JukeboxUpgradeWrapper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.UnaryOperator;

/** The renderers of upgrades' render data, by its type. */
public final class UpgradeRenderRegistry {
	private static final Map<String, IUpgradeRenderer> RENDERERS = new HashMap<>();

	static {
		register(CookingLogic.RENDER_DATA_TYPE, new CookingUpgradeRenderer());
		register(JukeboxUpgradeWrapper.RENDER_DATA_TYPE, new JukeboxUpgradeRenderer());
	}

	private UpgradeRenderRegistry() {}

	public static void register(String type, IUpgradeRenderer renderer) {
		RENDERERS.put(type, renderer);
	}

	public static void render(Map<String, NBTTagCompound> upgradeRenderData, World world, Random rand, UnaryOperator<Vec3d> getPositionFromOffset) {
		upgradeRenderData.forEach((type, data) -> {
			IUpgradeRenderer renderer = RENDERERS.get(type);
			if (renderer != null) {
				renderer.render(world, rand, getPositionFromOffset, data);
			}
		});
	}
}
