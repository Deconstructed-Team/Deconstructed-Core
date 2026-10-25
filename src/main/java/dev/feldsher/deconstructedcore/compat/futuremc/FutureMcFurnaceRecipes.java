package dev.feldsher.deconstructedcore.compat.futuremc;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * The recipes of Future MC's smoker or blast furnace, which 1.12 doesn't have otherwise. Through reflection, so that
 * Future MC is needed neither to build nor to run: without it there are none.
 */
public final class FutureMcFurnaceRecipes {
	private static final String MOD_ID = "futuremc";
	private static final String SIMPLE_RECIPE = "thedarkcolour.futuremc.recipe.SimpleRecipe";
	public static final FutureMcFurnaceRecipes SMOKER = new FutureMcFurnaceRecipes("thedarkcolour.futuremc.recipe.furnace.SmokerRecipes");
	public static final FutureMcFurnaceRecipes BLAST_FURNACE = new FutureMcFurnaceRecipes("thedarkcolour.futuremc.recipe.furnace.BlastFurnaceRecipes");

	private final String recipesClassName;
	private boolean initialized = false;
	@Nullable
	private Object recipesInstance = null;
	@Nullable
	private Method getRecipe = null;
	@Nullable
	private Method getOutput = null;

	private FutureMcFurnaceRecipes(String recipesClassName) {
		this.recipesClassName = recipesClassName;
	}

	private void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		if (!Loader.isModLoaded(MOD_ID)) {
			return;
		}
		try {
			Class<?> recipesClass = Class.forName(recipesClassName);
			recipesInstance = recipesClass.getField("INSTANCE").get(null);
			getRecipe = recipesClass.getMethod("getRecipe", ItemStack.class);
			getOutput = Class.forName(SIMPLE_RECIPE).getMethod("getOutput");
		}
		catch (ReflectiveOperationException e) {
			DeconstructedCore.LOGGER.error("Couldn't read Future MC's recipes of {}: the upgrade using them will have none", recipesClassName, e);
			recipesInstance = null;
		}
	}

	/** What the stack cooks into, not to be changed, or empty. */
	public ItemStack getResult(ItemStack input) {
		init();
		if (recipesInstance == null || getRecipe == null || getOutput == null || input.isEmpty()) {
			return ItemStack.EMPTY;
		}
		try {
			Object recipe = getRecipe.invoke(recipesInstance, input);
			return recipe == null ? ItemStack.EMPTY : (ItemStack) getOutput.invoke(recipe);
		}
		catch (ReflectiveOperationException | ClassCastException e) {
			DeconstructedCore.LOGGER.debug("Couldn't get Future MC's recipe of {} for {}", recipesClassName, input, e);
			return ItemStack.EMPTY;
		}
	}
}
