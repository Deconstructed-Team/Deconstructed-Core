package dev.feldsher.deconstructedcore.util;

import net.minecraft.entity.player.EntityPlayer;

/** Experience points and levels, as vanilla counts them. */
public final class XpHelper {
	private XpHelper() {}

	public static int getExperienceForLevel(int level) {
		if (level <= 0) {
			return 0;
		}
		if (level < 16) {
			return level * (12 + level * 2) / 2;
		} else if (level < 31) {
			return (level - 15) * (69 + (level - 15) * 5) / 2 + 315;
		} else {
			return (level - 30) * (215 + (level - 30) * 9) / 2 + 1395;
		}
	}

	/** The points from the level to the next, as vanilla's xpBarCap. */
	public static int getExperienceLimitOnLevel(int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		} else if (level >= 15) {
			return 37 + (level - 15) * 5;
		}
		return 7 + level * 2;
	}

	public static int getLevelForExperience(int experience) {
		int level = 0;
		while (getExperienceForLevel(level + 1) <= experience) {
			level++;
		}
		return level;
	}

	public static double getLevelsForExperience(int experience) {
		int baseLevel = getLevelForExperience(experience);
		int remainingExperience = experience - getExperienceForLevel(baseLevel);
		return baseLevel + ((double) remainingExperience / getExperienceLimitOnLevel(baseLevel));
	}

	public static int getPlayerTotalExperience(EntityPlayer player) {
		// Rounded: the progress is a float, and a point of it is often stored a hair below
		return getExperienceForLevel(player.experienceLevel) + Math.round(player.experience * player.xpBarCap());
	}

	/**
	 * Takes the points from the player, down a level where it goes below one: vanilla's addExperience only adds. The
	 * total changes too, which is what makes the server send the player their experience.
	 */
	public static void takeExperience(EntityPlayer player, int points) {
		int total = Math.max(0, getPlayerTotalExperience(player) - points);
		player.experienceLevel = getLevelForExperience(total);
		player.experience = (float) (total - getExperienceForLevel(player.experienceLevel)) / player.xpBarCap();
		player.experienceTotal = Math.max(0, player.experienceTotal - points);
	}
}
