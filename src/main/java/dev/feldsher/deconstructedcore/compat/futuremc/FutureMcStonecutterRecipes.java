package dev.feldsher.deconstructedcore.compat.futuremc;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.upgrades.stonecutter.ICuttingResults;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The stonecutter's recipes, which 1.12 has only from Future MC, with whatever packs add to them there. Through
 * reflection, so that Future MC is needed neither to build nor to run: without it there are none.
 */
public final class FutureMcStonecutterRecipes {
	private static final String MOD_ID = "futuremc";
	private static final String STONECUTTER_RECIPES = "thedarkcolour.futuremc.recipe.stonecutter.StonecutterRecipes";
	private static final String SIMPLE_RECIPE = "thedarkcolour.futuremc.recipe.SimpleRecipe";
	private static final ResourceLocation CARVE_SOUND = new ResourceLocation(MOD_ID, "stonecutter_carve");

	private static boolean initialized = false;
	@Nullable
	private static Object recipesInstance = null;
	@Nullable
	private static Method getRecipes = null;
	@Nullable
	private static Method getOutput = null;

	/** As a cutting upgrade's results: the stonecutter's, with its carving sound. */
	public static final ICuttingResults RESULTS = new ICuttingResults() {
		@Override
		public List<ItemStack> getResults(ItemStack input) {
			return FutureMcStonecutterRecipes.getResults(input);
		}

		@Nullable
		@Override
		public SoundEvent getSound(ItemStack input) {
			return getCarveSound();
		}
	};

	private FutureMcStonecutterRecipes() {}

	private static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		if (!Loader.isModLoaded(MOD_ID)) {
			return;
		}
		try {
			Class<?> recipesClass = Class.forName(STONECUTTER_RECIPES);
			recipesInstance = recipesClass.getField("INSTANCE").get(null);
			getRecipes = recipesClass.getMethod("getRecipes", ItemStack.class);
			getOutput = Class.forName(SIMPLE_RECIPE).getMethod("getOutput");
		}
		catch (ReflectiveOperationException e) {
			DeconstructedCore.LOGGER.error("Couldn't read Future MC's stonecutter recipes: the stonecutter upgrade will have none", e);
			recipesInstance = null;
		}
	}

	public static boolean isAvailable() {
		init();
		return recipesInstance != null;
	}

	/** What the stonecutter makes of the stack, each a copy, in Future MC's order. */
	public static List<ItemStack> getResults(ItemStack input) {
		init();
		if (recipesInstance == null || getRecipes == null || getOutput == null || input.isEmpty()) {
			return Collections.emptyList();
		}
		try {
			List<?> recipes = (List<?>) getRecipes.invoke(recipesInstance, input);
			List<ItemStack> results = new ArrayList<>();
			for (Object recipe : recipes) {
				ItemStack output = (ItemStack) getOutput.invoke(recipe);
				if (!output.isEmpty()) {
					results.add(output.copy());
				}
			}
			return results;
		}
		catch (ReflectiveOperationException | ClassCastException e) {
			DeconstructedCore.LOGGER.debug("Couldn't get Future MC's stonecutter recipes for {}", input, e);
			return Collections.emptyList();
		}
	}

	@Nullable
	public static SoundEvent getCarveSound() {
		return ForgeRegistries.SOUND_EVENTS.getValue(CARVE_SOUND);
	}
}
