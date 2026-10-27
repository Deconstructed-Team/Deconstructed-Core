package dev.feldsher.deconstructedcore.compat.chisel;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.upgrades.stonecutter.ICuttingResults;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The chisel upgrade's results: the other blocks of the input's carving group in Chisel, as its chisel's GUI lists
 * them, and the group's sound. Through reflection, so that Chisel is needed neither to build nor to run: without it
 * there are none.
 */
public final class ChiselCarvingResults implements ICuttingResults {
	public static final ChiselCarvingResults INSTANCE = new ChiselCarvingResults();
	private static final String MOD_ID = "chisel";
	private static final String CARVING_UTILS = "team.chisel.api.carving.CarvingUtils";
	private static final String CARVING_REGISTRY = "team.chisel.api.carving.ICarvingRegistry";

	private boolean initialized = false;
	@Nullable
	private Method getChiselRegistry = null;
	@Nullable
	private Method getItemsForChiseling = null;
	@Nullable
	private Method getVariationSound = null;

	private ChiselCarvingResults() {}

	private void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		if (!Loader.isModLoaded(MOD_ID)) {
			return;
		}
		try {
			getChiselRegistry = Class.forName(CARVING_UTILS).getMethod("getChiselRegistry");
			Class<?> registryClass = Class.forName(CARVING_REGISTRY);
			getItemsForChiseling = registryClass.getMethod("getItemsForChiseling", ItemStack.class);
			getVariationSound = registryClass.getMethod("getVariationSound", ItemStack.class);
		} catch (ReflectiveOperationException e) {
			DeconstructedCore.LOGGER.error("Couldn't read Chisel's carving groups: the chisel upgrade will have none", e);
			getChiselRegistry = null;
		}
	}

	@Nullable
	private Object getRegistry() throws ReflectiveOperationException {
		init();
		return getChiselRegistry == null ? null : getChiselRegistry.invoke(null);
	}

	/** Not the input itself: carving it into what it is would only use it up. */
	@Override
	public List<ItemStack> getResults(ItemStack input) {
		if (input.isEmpty()) {
			return Collections.emptyList();
		}
		try {
			Object registry = getRegistry();
			if (registry == null || getItemsForChiseling == null) {
				return Collections.emptyList();
			}
			List<ItemStack> results = new ArrayList<>();
			for (Object item : (List<?>) getItemsForChiseling.invoke(registry, input)) {
				ItemStack stack = (ItemStack) item;
				if (!stack.isEmpty() && !(stack.getItem() == input.getItem() && stack.getMetadata() == input.getMetadata())) {
					ItemStack result = stack.copy();
					result.setCount(1);
					results.add(result);
				}
			}
			return results;
		} catch (ReflectiveOperationException | ClassCastException e) {
			DeconstructedCore.LOGGER.debug("Couldn't get Chisel's variations of {}", input, e);
			return Collections.emptyList();
		}
	}

	@Nullable
	@Override
	public SoundEvent getSound(ItemStack input) {
		try {
			Object registry = getRegistry();
			return registry == null || getVariationSound == null ? null : (SoundEvent) getVariationSound.invoke(registry, input);
		} catch (ReflectiveOperationException | ClassCastException e) {
			return null;
		}
	}
}
