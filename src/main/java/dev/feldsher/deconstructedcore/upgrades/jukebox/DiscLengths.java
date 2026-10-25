package dev.feldsher.deconstructedcore.upgrades.jukebox;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * How long the discs play, which 1.12's records don't say: vanilla's, as 1.21 has them. For others it's unknown, and
 * the tab shows no progress of them.
 */
public final class DiscLengths {
	private static final Map<ResourceLocation, Integer> LENGTHS_IN_SECONDS = new HashMap<>();

	static {
		put("record_13", 178);
		put("record_cat", 185);
		put("record_blocks", 345);
		put("record_chirp", 185);
		put("record_far", 174);
		put("record_mall", 197);
		put("record_mellohi", 96);
		put("record_stal", 150);
		put("record_strad", 188);
		put("record_ward", 251);
		put("record_11", 71);
		put("record_wait", 238);
	}

	private DiscLengths() {}

	private static void put(String record, int seconds) {
		LENGTHS_IN_SECONDS.put(new ResourceLocation("minecraft", record), seconds);
	}

	/** 0 when it isn't known. */
	public static int getLengthInTicks(ItemStack disc) {
		ResourceLocation name = disc.getItem().getRegistryName();
		return name == null ? 0 : LENGTHS_IN_SECONDS.getOrDefault(name, 0) * 20;
	}
}
