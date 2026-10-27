package dev.feldsher.deconstructedcore.upgrades.stonecutter;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import java.util.function.Consumer;

/**
 * The input slot, saved into the upgrade, and the result last chosen, chosen again for the same input. Future MC's
 * recipes have no ids, so the result itself is kept.
 */
public class StonecutterUpgradeWrapper extends UpgradeWrapperBase<StonecutterUpgradeWrapper, StonecutterUpgradeItem> {
	private static final String INPUT_ITEM_TAG = "inputItem";
	private static final String RECIPE_RESULT_TAG = "recipeResult";
	private static final String SHIFT_CLICK_INTO_STORAGE_TAG = "shiftClickIntoStorage";
	private final ItemStackHandler inputInventory;

	public StonecutterUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		inputInventory = new ItemStackHandler(1) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				StorageNbt.getOrCreateTag(upgrade).setTag(INPUT_ITEM_TAG, getStackInSlot(0).writeToNBT(new NBTTagCompound()));
				save();
			}
		};
		NBTTagCompound tag = upgrade.getTagCompound();
		if (tag != null && tag.hasKey(INPUT_ITEM_TAG)) {
			inputInventory.setStackInSlot(0, new ItemStack(tag.getCompoundTag(INPUT_ITEM_TAG)));
		}
	}

	public ICuttingResults getCuttingResults() {
		return upgradeItem.getCuttingResults();
	}

	public String getTabName() {
		return upgradeItem.getTabName();
	}

	public IItemHandlerModifiable getInputInventory() {
		return inputInventory;
	}

	public ItemStack getRecipeResult() {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag != null && tag.hasKey(RECIPE_RESULT_TAG) ? new ItemStack(tag.getCompoundTag(RECIPE_RESULT_TAG)) : ItemStack.EMPTY;
	}

	public void setRecipeResult(ItemStack result) {
		StorageNbt.getOrCreateTag(upgrade).setTag(RECIPE_RESULT_TAG, result.writeToNBT(new NBTTagCompound()));
		save();
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	public boolean shouldShiftClickIntoStorage() {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag == null || !tag.hasKey(SHIFT_CLICK_INTO_STORAGE_TAG) || tag.getBoolean(SHIFT_CLICK_INTO_STORAGE_TAG);
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		StorageNbt.getOrCreateTag(upgrade).setBoolean(SHIFT_CLICK_INTO_STORAGE_TAG, shiftClickIntoStorage);
		save();
	}
}
