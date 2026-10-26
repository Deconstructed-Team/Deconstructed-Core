package dev.feldsher.deconstructedcore.util;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.List;

/** Mixes dyes into a color as leather armor does, with the storage's own color in the mix unless it's the default. */
public final class ColorHelper {
	/** The ore dictionary's dye names, by the dye's damage. */
	private static final String[] DYE_ORE_NAMES = {"dyeBlack", "dyeRed", "dyeGreen", "dyeBrown", "dyeBlue", "dyePurple", "dyeCyan", "dyeLightGray",
			"dyeGray", "dyePink", "dyeLime", "dyeYellow", "dyeLightBlue", "dyeMagenta", "dyeOrange", "dyeWhite"};

	private ColorHelper() {}

	/** The dye's color by the ore dictionary, as 1.21 by the dye tags: modded dyes too. */
	@Nullable
	public static EnumDyeColor getDyeColor(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		for (int oreId : OreDictionary.getOreIDs(stack)) {
			String oreName = OreDictionary.getOreName(oreId);
			for (int damage = 0; damage < DYE_ORE_NAMES.length; damage++) {
				if (DYE_ORE_NAMES[damage].equals(oreName)) {
					return EnumDyeColor.byDyeDamage(damage);
				}
			}
		}
		return null;
	}

	/** The ore dictionary's name of the dye of this color, as "dyeRed". */
	public static String getDyeOreName(EnumDyeColor color) {
		return DYE_ORE_NAMES[color.getDyeDamage()];
	}

	public static int calculateColor(int baseColor, int defaultColor, List<EnumDyeColor> dyes) {
		if (dyes.isEmpty()) {
			return baseColor;
		}

		int[] rgb = new int[3];
		int sumMaxComponent = 0;
		int numberOfColors = 0;
		if (baseColor != defaultColor) {
			float baseRed = (baseColor >> 16 & 255);
			float baseGreen = (baseColor >> 8 & 255);
			float baseBlue = (baseColor & 255);
			sumMaxComponent = (int) (sumMaxComponent + Math.max(baseRed, Math.max(baseGreen, baseBlue)));
			rgb[0] = (int) (rgb[0] + baseRed);
			rgb[1] = (int) (rgb[1] + baseGreen);
			rgb[2] = (int) (rgb[2] + baseBlue);
			++numberOfColors;
		}

		for (EnumDyeColor dye : dyes) {
			int dyeRgb = dye.getColorValue();
			int dyeRed = (dyeRgb >> 16) & 255;
			int dyeGreen = (dyeRgb >> 8) & 255;
			int dyeBlue = dyeRgb & 255;
			sumMaxComponent += Math.max(dyeRed, Math.max(dyeGreen, dyeBlue));
			rgb[0] += dyeRed;
			rgb[1] += dyeGreen;
			rgb[2] += dyeBlue;
			++numberOfColors;
		}

		int avgRed = rgb[0] / numberOfColors;
		int avgGreen = rgb[1] / numberOfColors;
		int avgBlue = rgb[2] / numberOfColors;
		float avgMaxComponent = (float) sumMaxComponent / (float) numberOfColors;
		float maxAvgComponent = Math.max(avgRed, Math.max(avgGreen, avgBlue));
		avgRed = (int) (avgRed * avgMaxComponent / maxAvgComponent);
		avgGreen = (int) (avgGreen * avgMaxComponent / maxAvgComponent);
		avgBlue = (int) (avgBlue * avgMaxComponent / maxAvgComponent);
		int finalColor = (avgRed << 8) + avgGreen;
		finalColor = (finalColor << 8) + avgBlue;

		return 0xFF_000000 | finalColor;
	}
}
