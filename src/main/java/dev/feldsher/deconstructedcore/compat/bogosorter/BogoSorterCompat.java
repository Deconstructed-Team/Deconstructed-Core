package dev.feldsher.deconstructedcore.compat.bogosorter;

import com.cleanroommc.bogosorter.api.IBogoSortAPI;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.Loader;

/**
 * Inventory Bogosorter sorts and moves items in slot groups. Storage slots hold more than a stack and have slots kept
 * out of sorting, which its sorting doesn't know of, so a storage container gets no groups of its own: Bogosorter
 * sorts the player's inventory there and moves items within it, and storage slots are sorted by the storage's own sort.
 * Without this, it would take every other slot of the container, upgrade and filter slots too, for its shortcuts.
 */
public final class BogoSorterCompat {
	public static final String MOD_ID = "bogosorter";

	private BogoSorterCompat() {}

	/** Containers are looked up by their exact class, so each concrete one is registered. */
	@SafeVarargs
	public static void registerStorageContainers(Class<? extends Container>... containerClasses) {
		if (Loader.isModLoaded(MOD_ID)) {
			Registration.register(containerClasses);
		}
	}

	/** Apart, so that nothing loads Bogosorter's classes without it. */
	private static final class Registration {
		private Registration() {}

		private static void register(Class<? extends Container>[] containerClasses) {
			for (Class<? extends Container> containerClass : containerClasses) {
				IBogoSortAPI.getInstance().addCompat(containerClass, (container, builder) -> {});
			}
		}
	}
}
