package dev.feldsher.deconstructedcore.crafting;

import com.google.gson.JsonObject;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeItem;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapedOreRecipe;

import javax.annotation.Nullable;

/** A shaped recipe that turns an upgrade into its next tier, keeping the upgrade's settings. */
public class UpgradeNextTierRecipe extends ShapedOreRecipe {
	public UpgradeNextTierRecipe(@Nullable ResourceLocation group, ItemStack result, CraftingHelper.ShapedPrimer primer) {
		super(group, result, primer);
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {
		ItemStack nextTier = super.getCraftingResult(inv);
		for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
			ItemStack slotStack = inv.getStackInSlot(slot);
			if (slotStack.getItem() instanceof IUpgradeItem<?> && slotStack.getTagCompound() != null) {
				nextTier.setTagCompound(slotStack.getTagCompound().copy());
				break;
			}
		}
		return nextTier;
	}

	/** {@code "type": "deconstructedcore:upgrade_next_tier"}, the rest as {@code forge:ore_shaped}. */
	public static class Factory implements IRecipeFactory {
		@Override
		public IRecipe parse(JsonContext context, JsonObject json) {
			ShapedOreRecipe recipe = ShapedOreRecipe.factory(context, json);
			CraftingHelper.ShapedPrimer primer = new CraftingHelper.ShapedPrimer();
			primer.width = recipe.getRecipeWidth();
			primer.height = recipe.getRecipeHeight();
			primer.mirrored = JsonUtils.getBoolean(json, "mirrored", true);
			primer.input = recipe.getIngredients();
			String group = JsonUtils.getString(json, "group", "");
			return new UpgradeNextTierRecipe(group.isEmpty() ? null : new ResourceLocation(group), recipe.getRecipeOutput(), primer);
		}
	}
}
