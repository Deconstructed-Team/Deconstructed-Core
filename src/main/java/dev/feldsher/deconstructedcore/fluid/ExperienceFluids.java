package dev.feldsher.deconstructedcore.fluid;

import dev.feldsher.deconstructedcore.Config;
import dev.feldsher.deconstructedcore.DeconstructedCore;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Liquid experience. 1.12 has no fluid tags: mods share one by its name instead, {@value #XP_JUICE} at 20 mB a point,
 * and Forge makes the fluids of a name one. This mod registers that one too, so that there is one without them.
 * <p>
 * Which fluids are experience, and how many mB a point, comes from the config; the first of them that the game has
 * is what experience is stored as when nothing of it is stored yet.
 */
public final class ExperienceFluids {
	public static final String XP_JUICE = "xpjuice";
	public static final ResourceLocation XP_STILL_TEXTURE = DeconstructedCore.getRL("block/xp_still");
	public static final ResourceLocation XP_FLOWING_TEXTURE = DeconstructedCore.getRL("block/xp_flowing");
	@Nullable
	private static Map<String, Integer> mbPerExperience = null;

	private ExperienceFluids() {}

	/** In pre-init, with the universal bucket enabled before, which then holds it too. */
	public static void register() {
		Fluid fluid = new Fluid(XP_JUICE, XP_STILL_TEXTURE, XP_FLOWING_TEXTURE)
				.setUnlocalizedName(DeconstructedCore.MOD_ID + ".experience").setLuminosity(10).setDensity(800).setViscosity(1500);
		FluidRegistry.registerFluid(fluid);
		FluidRegistry.addBucketForFluid(FluidRegistry.getFluid(XP_JUICE));
	}

	public static void clearConfigCache() {
		mbPerExperience = null;
	}

	/** How many mB of the fluid are a point of experience; 0 when it isn't experience. */
	public static int getMbPerExperience(@Nullable FluidStack fluid) {
		return fluid == null || fluid.getFluid() == null ? 0 : getMbPerExperience().getOrDefault(fluid.getFluid().getName(), 0);
	}

	/** What experience is stored as, when nothing of it is yet; null when the config names no fluid the game has. */
	@Nullable
	public static Fluid getDefaultFluid() {
		for (String name : getMbPerExperience().keySet()) {
			Fluid fluid = FluidRegistry.getFluid(name);
			if (fluid != null) {
				return fluid;
			}
		}
		return null;
	}

	/** Entries of "fluidName|mBPerPoint". */
	private static Map<String, Integer> getMbPerExperience() {
		if (mbPerExperience == null) {
			Map<String, Integer> ratios = new LinkedHashMap<>();
			for (String entry : Config.experienceFluids) {
				String[] parts = entry.split("\\|");
				int ratio = 0;
				try {
					if (parts.length == 2) {
						ratio = Integer.parseInt(parts[1].trim());
					}
				} catch (NumberFormatException e) {
					ratio = 0;
				}
				if (ratio <= 0 || parts[0].trim().isEmpty()) {
					DeconstructedCore.LOGGER.warn("Experience fluid \"{}\" in the config isn't \"FluidName|MbPerPoint\" with positive mB", entry);
				} else if (!FluidRegistry.isFluidRegistered(parts[0].trim())) {
					DeconstructedCore.LOGGER.debug("Experience fluid \"{}\" in the config is of a fluid the game doesn't have, skipped", entry);
				} else {
					ratios.put(parts[0].trim(), ratio);
				}
			}
			mbPerExperience = ratios;
		}
		return mbPerExperience;
	}
}
