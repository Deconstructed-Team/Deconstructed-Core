package dev.feldsher.deconstructedcore.upgrades.cooking;

import dev.feldsher.deconstructedcore.compat.futuremc.FutureMcFurnaceRecipes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;

import java.util.function.Function;

/**
 * What a cooking upgrade cooks, as 1.21's recipe types: the furnace's recipes, and the smoker's and blast furnace's,
 * which 1.12 has only from Future MC. Those two cook twice as fast, and fuel burns half as long in them.
 */
public enum CookingType {
	SMELTING(input -> FurnaceRecipes.instance().getSmeltingResult(input), 200, 1),
	SMOKING(FutureMcFurnaceRecipes.SMOKER::getResult, 100, 0.5F),
	BLASTING(FutureMcFurnaceRecipes.BLAST_FURNACE::getResult, 100, 0.5F);

	private final Function<ItemStack, ItemStack> getResult;
	private final int cookingTime;
	private final float burnTimeModifier;

	CookingType(Function<ItemStack, ItemStack> getResult, int cookingTime, float burnTimeModifier) {
		this.getResult = getResult;
		this.cookingTime = cookingTime;
		this.burnTimeModifier = burnTimeModifier;
	}

	/** What the stack cooks into, not to be changed, or empty. */
	public ItemStack getResult(ItemStack input) {
		return input.isEmpty() ? ItemStack.EMPTY : getResult.apply(input);
	}

	public boolean isInput(ItemStack stack) {
		return !getResult(stack).isEmpty();
	}

	public int getCookingTime() {
		return cookingTime;
	}

	public int getBurnTime(ItemStack fuel) {
		return (int) (TileEntityFurnace.getItemBurnTime(fuel) * burnTimeModifier);
	}
}
