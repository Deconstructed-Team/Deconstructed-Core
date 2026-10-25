package dev.feldsher.deconstructedcore.upgrades.jukebox;

import dev.feldsher.deconstructedcore.Config;
import dev.feldsher.deconstructedcore.DeconstructedCore;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * How long the discs play, which 1.12's records don't say: vanilla's, as 1.21 has them, those other mods register,
 * and those the config gives, Future MC's by default, which win over the rest. For others it's unknown, and the tab
 * shows no progress of them.
 * <p>
 * Mods register theirs with {@link #register}, or, without depending on this mod, with an IMC message
 * {@value #IMC_KEY} to {@code deconstructedcore} whose NBT has the disc item's {@code item} name and its {@code seconds}.
 */
public final class DiscLengths {
	public static final String IMC_KEY = "disc_length";
	private static final Map<ResourceLocation, Integer> LENGTHS_IN_SECONDS = new HashMap<>();
	@Nullable
	private static Map<ResourceLocation, Integer> configLengthsInSeconds = null;

	static {
		register("record_13", 178);
		register("record_cat", 185);
		register("record_blocks", 345);
		register("record_chirp", 185);
		register("record_far", 174);
		register("record_mall", 197);
		register("record_mellohi", 96);
		register("record_stal", 150);
		register("record_strad", 188);
		register("record_ward", 251);
		register("record_11", 71);
		register("record_wait", 238);
	}

	private DiscLengths() {}

	private static void register(String vanillaRecord, int seconds) {
		register(new ResourceLocation("minecraft", vanillaRecord), seconds);
	}

	/** How long the disc item plays; a later registration of the same disc replaces the earlier. */
	public static void register(ResourceLocation discItem, int lengthInSeconds) {
		LENGTHS_IN_SECONDS.put(discItem, lengthInSeconds);
	}

	/** A message of {@link #IMC_KEY}: {@code item} and {@code seconds}. */
	public static void registerFromImc(String sender, NBTTagCompound message) {
		String item = message.getString("item");
		int seconds = message.getInteger("seconds");
		if (item.isEmpty() || seconds <= 0) {
			DeconstructedCore.LOGGER.warn("{} sent a disc length without an item name or with no positive seconds: {}", sender, message);
			return;
		}
		register(new ResourceLocation(item), seconds);
	}

	/** 0 when it isn't known. */
	public static int getLengthInTicks(ItemStack disc) {
		ResourceLocation name = disc.getItem().getRegistryName();
		if (name == null) {
			return 0;
		}
		Integer seconds = getConfigLengths().get(name);
		if (seconds == null) {
			seconds = LENGTHS_IN_SECONDS.getOrDefault(name, 0);
		}
		return seconds * 20;
	}

	public static void clearConfigCache() {
		configLengthsInSeconds = null;
	}

	private static Map<ResourceLocation, Integer> getConfigLengths() {
		if (configLengthsInSeconds == null) {
			Map<ResourceLocation, Integer> lengths = new HashMap<>();
			for (String entry : Config.discLengths) {
				String[] parts = entry.split("\\|");
				ResourceLocation item = null;
				int seconds = 0;
				try {
					if (parts.length == 2) {
						item = new ResourceLocation(parts[0].trim());
						seconds = Integer.parseInt(parts[1].trim());
					}
				} catch (RuntimeException e) {
					item = null;
				}
				if (item == null || seconds <= 0) {
					DeconstructedCore.LOGGER.warn("Disc length \"{}\" in the config isn't \"ItemRegistryName|Seconds\" with positive seconds", entry);
				} else if (!ForgeRegistries.ITEMS.containsKey(item)) {
					DeconstructedCore.LOGGER.debug("Disc length \"{}\" in the config is of an item the game doesn't have, skipped", entry);
				} else {
					lengths.put(item, seconds);
				}
			}
			configLengthsInSeconds = lengths;
		}
		return configLengthsInSeconds;
	}
}
