package dev.feldsher.deconstructedcore.upgrades.stonecutter;

import dev.feldsher.deconstructedcore.common.gui.IServerUpdater;
import dev.feldsher.deconstructedcore.common.gui.SlotSuppliedHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The input slot, the results the stonecutter makes of it, the chosen one, and its result slot. Both sides find the
 * results themselves; the client's choice is sent to the server. The choice is kept for the next of the same input.
 */
public class StonecutterRecipeContainer {
	private static final String DATA_SELECTED_RECIPE_INDEX = "selectedRecipeIndex";
	private final StonecutterUpgradeContainer upgradeContainer;
	private final Slot inputSlot;
	private final Slot outputSlot;
	private final IServerUpdater serverUpdater;
	private final EntityPlayer player;
	private final InventoryCraftResult resultInventory = new InventoryCraftResult();
	private List<ItemStack> recipes = new ArrayList<>();
	private int selectedRecipe = -1;
	private ItemStack inputItem = ItemStack.EMPTY;
	private Runnable inventoryUpdateListener = () -> {};
	private long lastOnTake = -1;

	public StonecutterRecipeContainer(StonecutterUpgradeContainer upgradeContainer, Consumer<Slot> addSlot, IServerUpdater serverUpdater, EntityPlayer player) {
		this.upgradeContainer = upgradeContainer;
		this.serverUpdater = serverUpdater;
		this.player = player;
		inputSlot = new SlotSuppliedHandler(() -> upgradeContainer.getUpgradeWrapper().getInputInventory(), 0, -1, -1) {
			@Override
			public void onSlotChanged() {
				super.onSlotChanged();
				onInputChanged();
			}

			@Nonnull
			@Override
			public ItemStack decrStackSize(int amount) {
				ItemStack ret = super.decrStackSize(amount);
				onSlotChanged();
				return ret;
			}
		};
		addSlot.accept(inputSlot);
		outputSlot = new ResultSlot();
		addSlot.accept(outputSlot);
		onInputChanged();
	}

	private void onInputChanged() {
		ItemStack stack = inputSlot.getStack();
		if (stack.getItem() != inputItem.getItem() || stack.getMetadata() != inputItem.getMetadata()) {
			inputItem = stack.copy();
			updateAvailableRecipes(stack);
		}
		inventoryUpdateListener.run();
	}

	private void updateAvailableRecipes(ItemStack stack) {
		recipes = upgradeContainer.getUpgradeWrapper().getCuttingResults().getResults(stack);
		selectedRecipe = -1;
		resultInventory.setInventorySlotContents(0, ItemStack.EMPTY);
		ItemStack lastResult = upgradeContainer.getUpgradeWrapper().getRecipeResult();
		if (!lastResult.isEmpty()) {
			for (int i = 0; i < recipes.size(); i++) {
				if (ItemStack.areItemStacksEqual(recipes.get(i), lastResult)) {
					selectedRecipe = i;
					updateRecipeResultSlot();
					break;
				}
			}
		}
	}

	public Slot getInputSlot() {
		return inputSlot;
	}

	public Slot getOutputSlot() {
		return outputSlot;
	}

	public void setInventoryUpdateListener(Runnable listener) {
		inventoryUpdateListener = listener;
	}

	public List<ItemStack> getRecipeList() {
		return recipes;
	}

	public int getSelectedRecipe() {
		return selectedRecipe;
	}

	public boolean hasItemsInInputSlot() {
		return inputSlot.getHasStack() && !recipes.isEmpty();
	}

	public boolean selectRecipe(int recipeIndex) {
		if (!isIndexInRecipeBounds(recipeIndex)) {
			return false;
		}
		selectedRecipe = recipeIndex;
		upgradeContainer.getUpgradeWrapper().setRecipeResult(recipes.get(recipeIndex));
		updateRecipeResultSlot();
		serverUpdater.sendDataToServer(() -> {
			NBTTagCompound data = new NBTTagCompound();
			data.setInteger(DATA_SELECTED_RECIPE_INDEX, recipeIndex);
			return data;
		});
		return true;
	}

	private boolean isIndexInRecipeBounds(int index) {
		return index >= 0 && index < recipes.size();
	}

	private void updateRecipeResultSlot() {
		resultInventory.setInventorySlotContents(0, isIndexInRecipeBounds(selectedRecipe) ? recipes.get(selectedRecipe).copy() : ItemStack.EMPTY);
	}

	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(DATA_SELECTED_RECIPE_INDEX)) {
			selectRecipe(data.getInteger(DATA_SELECTED_RECIPE_INDEX));
		}
	}

	public boolean isNotResultSlot(Slot slot) {
		return slot != outputSlot;
	}

	private class ResultSlot extends Slot {
		private ResultSlot() {
			super(resultInventory, 0, -1, -1);
		}

		@Override
		public boolean isItemValid(@Nonnull ItemStack stack) {
			return false;
		}

		/** Uses up one of the input, as the stonecutter or chisel does, carving with its sound once a tick. */
		@Nonnull
		@Override
		public ItemStack onTake(EntityPlayer thePlayer, @Nonnull ItemStack stack) {
			stack.onCrafting(thePlayer.world, thePlayer, stack.getCount());
			ItemStack input = inputSlot.getStack().copy();
			ItemStack taken = inputSlot.decrStackSize(1);
			if (!taken.isEmpty()) {
				updateRecipeResultSlot();
			}
			if (!thePlayer.world.isRemote) {
				long time = thePlayer.world.getTotalWorldTime();
				SoundEvent sound = upgradeContainer.getUpgradeWrapper().getCuttingResults().getSound(input);
				if (lastOnTake != time && sound != null) {
					thePlayer.world.playSound(null, player.posX, player.posY, player.posZ, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
					lastOnTake = time;
				}
			}
			return super.onTake(thePlayer, stack);
		}
	}
}
