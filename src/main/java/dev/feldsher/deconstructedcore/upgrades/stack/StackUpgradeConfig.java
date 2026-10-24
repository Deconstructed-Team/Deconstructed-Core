package dev.feldsher.deconstructedcore.upgrades.stack;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/** Which items stack upgrades leave at their own stack size, read from a storage mod's config list of registry names. */
public class StackUpgradeConfig {
	@Nullable
	private Set<Item> nonStackableItems = null;
	private final Supplier<String[]> nonStackableItemsList;

	public StackUpgradeConfig(Supplier<String[]> nonStackableItemsList) {
		this.nonStackableItemsList = nonStackableItemsList;
	}

	public static String[] getDefaultNonStackableList() {
		String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "silver", "cyan", "purple", "blue",
				"brown", "green", "red", "black"};
		String[] ret = new String[colors.length];
		for (int i = 0; i < colors.length; i++) {
			ret[i] = "minecraft:" + colors[i] + "_shulker_box";
		}
		return ret;
	}

	public boolean canStackItem(Item item) {
		if (nonStackableItems == null) {
			nonStackableItems = new HashSet<>();
			for (String name : nonStackableItemsList.get()) {
				ResourceLocation registryName = new ResourceLocation(name);
				Item nonStackable = ForgeRegistries.ITEMS.getValue(registryName);
				if (nonStackable != null && ForgeRegistries.ITEMS.containsKey(registryName)) {
					nonStackableItems.add(nonStackable);
				} else {
					DeconstructedCore.LOGGER.error("Item {} is set to not be affected by stack upgrade in config, but it does not exist in item registry", name);
				}
			}
		}
		return !nonStackableItems.contains(item);
	}

	public void clearNonStackableItems() {
		nonStackableItems = null;
	}
}
