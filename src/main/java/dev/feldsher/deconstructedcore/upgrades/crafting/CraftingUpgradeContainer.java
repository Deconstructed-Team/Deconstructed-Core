package dev.feldsher.deconstructedcore.upgrades.crafting;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.common.gui.SlotSuppliedHandler;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import dev.feldsher.deconstructedcore.util.RecipeHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The crafting grid's nine slots and its result slot. The server finds the result, by the recipe used last while it
 * still matches; when several recipes match the grid, the player can choose among their results. The client finds the
 * matching recipes itself, to show the choice.
 */
public class CraftingUpgradeContainer extends UpgradeContainerBase<CraftingUpgradeWrapper, CraftingUpgradeContainer> {
	private static final String DATA_SHIFT_CLICK_INTO_STORAGE = "shiftClickIntoStorage";
	private static final String DATA_SELECT_RESULT = "selectResult";
	private static final int GRID_SLOTS = 9;
	private final InventoryCraftResult craftResult = new InventoryCraftResult();
	private final CraftingItemHandler craftMatrix;
	private final SlotCrafting craftingResultSlot;
	@Nullable
	private IRecipe lastRecipe = null;
	private List<IRecipe> matchedCraftingRecipes = new ArrayList<>();
	private final List<ItemStack> matchedCraftingResults = new ArrayList<>();
	private int selectedCraftingResultIndex = 0;

	public CraftingUpgradeContainer(EntityPlayer player, int containerId, CraftingUpgradeWrapper wrapper, UpgradeContainerType<CraftingUpgradeWrapper, CraftingUpgradeContainer> type) {
		super(player, containerId, wrapper, type);

		for (int slot = 0; slot < GRID_SLOTS; slot++) {
			slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.getInventory(), slot, -100, -100) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					updateCraftingResult();
				}
			});
		}
		craftMatrix = new CraftingItemHandler(() -> upgradeWrapper.getInventory(), this::updateCraftingResult);
		craftingResultSlot = new SlotCrafting(player, craftMatrix, craftResult, 0, -100, -100) {
			/** The grid is used up by the recipe whose result is shown, not by the first that matches as vanilla's. */
			@Nonnull
			@Override
			public ItemStack onTake(EntityPlayer thePlayer, @Nonnull ItemStack stack) {
				if (thePlayer.world.isRemote) {
					return stack;
				}
				ItemStack remainingStack = getStack();
				onCrafting(stack);
				ForgeHooks.setCraftingPlayer(thePlayer);
				NonNullList<ItemStack> remainingItems = lastRecipe != null && lastRecipe.matches(craftMatrix, thePlayer.world)
						? lastRecipe.getRemainingItems(craftMatrix) : NonNullList.withSize(GRID_SLOTS, ItemStack.EMPTY);
				ForgeHooks.setCraftingPlayer(null);
				if (remainingItems.size() > GRID_SLOTS) {
					DeconstructedCore.LOGGER.error("Recipe {} returned more than 9 remaining items, ignoring the rest!", lastRecipe == null ? "[unknown]" : lastRecipe.getRegistryName());
				}

				for (int i = 0; i < Math.min(GRID_SLOTS, remainingItems.size()); i++) {
					ItemStack recipeInputStack = craftMatrix.getStackInSlot(i);
					ItemStack remainingItemStack = remainingItems.get(i);
					if (!recipeInputStack.isEmpty()) {
						craftMatrix.decrStackSize(i, 1);
						recipeInputStack = craftMatrix.getStackInSlot(i);
					}
					if (!remainingItemStack.isEmpty()) {
						if (recipeInputStack.isEmpty()) {
							craftMatrix.setInventorySlotContents(i, remainingItemStack);
						} else if (ItemHandlerHelper.canItemStacksStack(recipeInputStack, remainingItemStack)) {
							remainingItemStack.grow(recipeInputStack.getCount());
							craftMatrix.setInventorySlotContents(i, remainingItemStack);
						} else if (!thePlayer.inventory.addItemStackToInventory(remainingItemStack)) {
							thePlayer.dropItem(remainingItemStack, false);
						}
					}
				}

				if (!remainingStack.isEmpty()) {
					thePlayer.dropItem(remainingStack, false);
				}
				return stack;
			}

			/**
			 * Only the result of what's in the grid now: another player's container on the same upgrade may have
			 * changed the grid since this one's result was set.
			 */
			@Override
			public boolean canTakeStack(EntityPlayer playerIn) {
				if (playerIn.world.isRemote || isResultUpToDate(getStack())) {
					return super.canTakeStack(playerIn);
				}
				updateCraftingResult();
				return false;
			}

			@Override
			public void onSlotChanged() {
				super.onSlotChanged();
				if (player.world.isRemote) {
					updateClientMatchedResults(getStack());
				}
			}
		};
		slots.add(craftingResultSlot);
	}

	@Override
	public void onInit() {
		super.onInit();
		updateCraftingResult();
	}

	private void updateClientMatchedResults(ItemStack shownResult) {
		matchedCraftingRecipes = new ArrayList<>();
		matchedCraftingResults.clear();
		if (shownResult.isEmpty()) {
			return;
		}
		matchedCraftingRecipes = RecipeHelper.getCraftingRecipesFor(craftMatrix, player.world);
		for (int i = 0; i < matchedCraftingRecipes.size(); i++) {
			ItemStack result = matchedCraftingRecipes.get(i).getCraftingResult(craftMatrix);
			matchedCraftingResults.add(result);
			if (ItemStack.areItemStacksEqual(shownResult, result)) {
				selectedCraftingResultIndex = i;
			}
		}
	}

	private void updateCraftingResult() {
		World world = player.world;
		if (world.isRemote) {
			return;
		}
		ItemStack result = ItemStack.EMPTY;
		if (lastRecipe != null && lastRecipe.matches(craftMatrix, world) && matchedCraftingRecipes.size() <= 1) {
			result = lastRecipe.getCraftingResult(craftMatrix);
		} else {
			// with more than one recipe to pick from, their list is kept that of the grid, and the picked one stays picked
			IRecipe previousRecipe = lastRecipe != null && lastRecipe.matches(craftMatrix, world) ? lastRecipe : null;
			matchedCraftingRecipes = RecipeHelper.getCraftingRecipesFor(craftMatrix, world);
			matchedCraftingResults.clear();
			selectedCraftingResultIndex = 0;
			lastRecipe = null;
			if (!matchedCraftingRecipes.isEmpty()) {
				int index = Math.max(0, matchedCraftingRecipes.indexOf(previousRecipe));
				IRecipe recipe = matchedCraftingRecipes.get(index);
				if (canCraft(recipe)) {
					craftResult.setRecipeUsed(recipe);
					lastRecipe = recipe;
					selectedCraftingResultIndex = index;
					result = recipe.getCraftingResult(craftMatrix);
				}
				for (IRecipe matchedRecipe : matchedCraftingRecipes) {
					matchedCraftingResults.add(matchedRecipe.getCraftingResult(craftMatrix));
				}
			}
		}
		craftResult.setInventorySlotContents(0, result);
	}

	/** Vanilla's workbench check: with limited crafting only unlocked recipes craft. */
	private boolean canCraft(IRecipe recipe) {
		World world = player.world;
		return recipe.isDynamic() || !world.getGameRules().getBoolean("doLimitedCrafting") || ((EntityPlayerMP) player).getRecipeBook().isUnlocked(recipe);
	}

	private boolean isResultUpToDate(ItemStack shownResult) {
		if (shownResult.isEmpty()) {
			return true;
		}
		return lastRecipe != null && lastRecipe.matches(craftMatrix, player.world) && ItemStack.areItemStacksEqual(shownResult, lastRecipe.getCraftingResult(craftMatrix));
	}

	public List<ItemStack> getMatchedCraftingResults() {
		return matchedCraftingResults;
	}

	public void selectNextCraftingResult() {
		if (matchedCraftingResults.size() > 1) {
			selectCraftingResult((selectedCraftingResultIndex + 1) % matchedCraftingResults.size());
		}
	}

	public void selectPreviousCraftingResult() {
		if (matchedCraftingResults.size() > 1) {
			selectCraftingResult((selectedCraftingResultIndex + matchedCraftingResults.size() - 1) % matchedCraftingResults.size());
		}
	}

	public void selectCraftingResult(int resultIndex) {
		if (resultIndex < 0 || resultIndex >= matchedCraftingResults.size()) {
			return;
		}
		if (player.world.isRemote) {
			sendDataToServer(() -> {
				NBTTagCompound data = new NBTTagCompound();
				data.setInteger(DATA_SELECT_RESULT, resultIndex);
				return data;
			});
			return;
		}
		// the list the client picked from may be older than the grid: the recipe must match what's there now
		IRecipe recipe = matchedCraftingRecipes.get(resultIndex);
		if (!recipe.matches(craftMatrix, player.world) || !canCraft(recipe)) {
			lastRecipe = null;
			updateCraftingResult();
			return;
		}
		selectedCraftingResultIndex = resultIndex;
		lastRecipe = recipe;
		craftResult.setRecipeUsed(recipe);
		craftResult.setInventorySlotContents(0, recipe.getCraftingResult(craftMatrix));
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(DATA_SHIFT_CLICK_INTO_STORAGE)) {
			setShiftClickIntoStorage(data.getBoolean(DATA_SHIFT_CLICK_INTO_STORAGE));
		} else if (data.hasKey(DATA_SELECT_RESULT)) {
			selectCraftingResult(data.getInteger(DATA_SELECT_RESULT));
		}
	}

	/** A shift clicked result counts as crafted, as vanilla's workbench does it. */
	@Override
	public ItemStack getSlotStackToTransfer(Slot slot) {
		if (slot == craftingResultSlot) {
			ItemStack slotStack = slot.getStack();
			slotStack.getItem().onCreated(slotStack, player.world, player);
			return slotStack;
		}
		return super.getSlotStackToTransfer(slot);
	}

	public boolean shouldShiftClickIntoStorage() {
		return upgradeWrapper.shouldShiftClickIntoStorage();
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		upgradeWrapper.setShiftClickIntoStorage(shiftClickIntoStorage);
		sendBooleanToServer(DATA_SHIFT_CLICK_INTO_STORAGE, shiftClickIntoStorage);
	}

	@Override
	public boolean mergeIntoStorageFirst(Slot slot) {
		return slot != craftingResultSlot || shouldShiftClickIntoStorage();
	}

	@Override
	public boolean allowsPickupAll(Slot slot) {
		return slot != craftingResultSlot;
	}
}
