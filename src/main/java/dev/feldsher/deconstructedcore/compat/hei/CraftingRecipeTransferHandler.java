package dev.feldsher.deconstructedcore.compat.hei;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.common.gui.StorageContainerMenuBase;
import dev.feldsher.deconstructedcore.network.PacketHandler;
import dev.feldsher.deconstructedcore.network.TransferRecipeMessage;
import dev.feldsher.deconstructedcore.upgrades.crafting.CraftingRecipeTransfer;
import dev.feldsher.deconstructedcore.upgrades.crafting.CraftingUpgradeContainer;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HEI's "+" on a crafting recipe, for a storage with a crafting upgrade: the recipe goes into its grid, the tab opening.
 * Only with all the items there, in the storage, the player's inventory or the grid; the missing ones are marked.
 */
public class CraftingRecipeTransferHandler<C extends StorageContainerMenuBase<?>> implements IRecipeTransferHandler<C> {
	// HEI's crafting layout: the result, then the 3x3 grid
	private static final int OUTPUT_SLOT = 0;
	private static final int FIRST_INPUT_SLOT = 1;
	private static final int GRID_SLOTS = 9;

	private final Class<C> containerClass;
	private final IRecipeTransferHandlerHelper handlerHelper;

	public CraftingRecipeTransferHandler(Class<C> containerClass, IRecipeTransferHandlerHelper handlerHelper) {
		this.containerClass = containerClass;
		this.handlerHelper = handlerHelper;
	}

	@Override
	public Class<C> getContainerClass() {
		return containerClass;
	}

	@Nullable
	@Override
	public IRecipeTransferError transferRecipe(C container, IRecipeLayout recipeLayout, EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
		Optional<CraftingUpgradeContainer> crafting = CraftingRecipeTransfer.getCraftingContainer(container);
		if (!crafting.isPresent()) {
			return handlerHelper.createInternalError();
		}

		List<List<ItemStack>> grid = new ArrayList<>();
		for (int i = 0; i < GRID_SLOTS; i++) {
			grid.add(new ArrayList<>());
		}
		ItemStack output = ItemStack.EMPTY;
		for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : recipeLayout.getItemStacks().getGuiIngredients().entrySet()) {
			int slot = entry.getKey();
			IGuiIngredient<ItemStack> ingredient = entry.getValue();
			if (slot == OUTPUT_SLOT && !ingredient.isInput()) {
				ItemStack displayed = ingredient.getDisplayedIngredient();
				output = displayed == null ? ItemStack.EMPTY : displayed;
			} else if (ingredient.isInput() && slot >= FIRST_INPUT_SLOT && slot < FIRST_INPUT_SLOT + GRID_SLOTS) {
				for (ItemStack accepted : ingredient.getAllIngredients()) {
					if (accepted != null && !accepted.isEmpty()) {
						grid.get(slot - FIRST_INPUT_SLOT).add(accepted);
					}
				}
			}
		}

		List<Integer> missing = getMissingSlots(container, crafting.get(), player, grid);
		if (!missing.isEmpty()) {
			return handlerHelper.createUserErrorForSlots(I18n.format("jei.tooltip.error.recipe.transfer.missing"), missing);
		}

		if (doTransfer) {
			if (Minecraft.getMinecraft().currentScreen instanceof StorageScreenBase) {
				((StorageScreenBase<?>) Minecraft.getMinecraft().currentScreen).getUpgradeSettingsControl().openUpgradeTab(crafting.get());
			}
			PacketHandler.INSTANCE.sendToServer(new TransferRecipeMessage(container.windowId, grid, output, maxTransfer));
		}
		return null;
	}

	/** The recipe's slots, as HEI numbers them, that none of what's there now fills, each item counted once. */
	private List<Integer> getMissingSlots(C container, CraftingUpgradeContainer crafting, EntityPlayer player, List<List<ItemStack>> grid) {
		List<ItemStack> available = new ArrayList<>();
		for (Slot slot : CraftingRecipeTransfer.getSourceSlots(container, player)) {
			available.add(slot.getStack().copy());
		}
		for (Slot slot : crafting.getSlots().subList(0, GRID_SLOTS)) {
			available.add(slot.getStack().copy());
		}

		List<Integer> missing = new ArrayList<>();
		for (int i = 0; i < GRID_SLOTS; i++) {
			List<ItemStack> accepted = grid.get(i);
			if (!accepted.isEmpty() && !takeOne(available, accepted)) {
				missing.add(FIRST_INPUT_SLOT + i);
			}
		}
		return missing;
	}

	private static boolean takeOne(List<ItemStack> available, List<ItemStack> accepted) {
		for (ItemStack stack : available) {
			for (ItemStack acceptedStack : accepted) {
				if (CraftingRecipeTransfer.matches(acceptedStack, stack)) {
					stack.shrink(1);
					return true;
				}
			}
		}
		return false;
	}
}
