package dev.feldsher.deconstructedcore.upgrades.compacting;

import dev.feldsher.deconstructedcore.api.ISlotChangeResponseUpgrade;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.IFilteredUpgrade;
import dev.feldsher.deconstructedcore.upgrades.IInsertResponseUpgrade;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.util.InventoryHelper;
import dev.feldsher.deconstructedcore.util.RecipeHelper;
import dev.feldsher.deconstructedcore.util.RecipeHelper.CompactingShape;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Crafts what's put in into its compacted form, 2x2 or, advanced, 3x3, as nuggets into ingots and ingots into blocks.
 * By default only what uncrafts back into the same items; set to, anything that compacts. Working in the GUI, it also
 * compacts what the player puts in by hand.
 */
public class CompactingUpgradeWrapper extends UpgradeWrapperBase<CompactingUpgradeWrapper, CompactingUpgradeItem>
		implements IInsertResponseUpgrade, IFilteredUpgrade, ISlotChangeResponseUpgrade, ITickableUpgrade {
	private static final String COMPACT_NON_UNCRAFTABLE_TAG = "compact_non_uncraftable";
	private static final String SHOULD_WORK_IN_GUI_TAG = "should_work_in_gui";
	private final FilterLogic filterLogic;
	private final Set<Integer> slotsToCompact = new HashSet<>();

	public CompactingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);

		filterLogic = new FilterLogic(upgrade, this.upgradeSaveHandler, upgradeItem.getFilterSlotCount(),
				stack -> RecipeHelper.getItemCompactingShapes(stack).stream().anyMatch(shape -> shape != CompactingShape.NONE),
				"filter_attributes");
	}

	@Override
	public ItemStack onBeforeInsert(IItemHandler inventoryHandler, int slot, ItemStack stack, boolean simulate) {
		return stack;
	}

	@Override
	public void onAfterInsert(IItemHandler inventoryHandler, int slot) {
		compactSlot(inventoryHandler, slot);
	}

	private void compactSlot(IItemHandler inventoryHandler, int slot) {
		ItemStack slotStack = inventoryHandler.getStackInSlot(slot);

		if (slotStack.isEmpty() || !filterLogic.matchesFilter(slotStack)) {
			return;
		}

		Set<CompactingShape> shapes = RecipeHelper.getItemCompactingShapes(slotStack);

		if (upgradeItem.shouldCompactThreeByThree() && (shapes.contains(CompactingShape.THREE_BY_THREE_UNCRAFTABLE) || (shouldCompactNonUncraftable() && shapes.contains(CompactingShape.THREE_BY_THREE)))) {
			tryCompacting(inventoryHandler, slotStack, 3, 3);
		} else if (shapes.contains(CompactingShape.TWO_BY_TWO_UNCRAFTABLE) || (shouldCompactNonUncraftable() && shapes.contains(CompactingShape.TWO_BY_TWO))) {
			tryCompacting(inventoryHandler, slotStack, 2, 2);
		}
	}

	private void tryCompacting(IItemHandler inventoryHandler, ItemStack stack, int width, int height) {
		int totalCount = width * height;
		RecipeHelper.CompactingResult compactingResult = RecipeHelper.getCompactingResult(stack, width, height);
		if (compactingResult.getResult().isEmpty()) {
			return;
		}
		ItemStack toCompact = ItemHandlerHelper.copyStackWithSize(stack, totalCount);
		ItemStack extractedStack = InventoryHelper.extractFromInventory(toCompact, inventoryHandler, true);
		while (extractedStack.getCount() == totalCount) {
			ItemStack resultCopy = compactingResult.getResult().copy();
			List<ItemStack> remainingItemsCopy = new ArrayList<>();
			compactingResult.getRemainingItems().forEach(remainingItem -> remainingItemsCopy.add(remainingItem.copy()));

			if (!fitsResultAndRemainingItems(inventoryHandler, remainingItemsCopy, resultCopy)) {
				break;
			}
			InventoryHelper.extractFromInventory(toCompact, inventoryHandler, false);
			InventoryHelper.insertIntoInventory(resultCopy, inventoryHandler, false);
			InventoryHelper.insertIntoInventory(remainingItemsCopy, inventoryHandler, false);
			extractedStack = InventoryHelper.extractFromInventory(toCompact, inventoryHandler, true);
		}
	}

	private boolean fitsResultAndRemainingItems(IItemHandler inventoryHandler, List<ItemStack> remainingItems, ItemStack result) {
		if (!remainingItems.isEmpty()) {
			IItemHandler clonedHandler = InventoryHelper.cloneInventory(inventoryHandler);
			return InventoryHelper.insertIntoInventory(result, clonedHandler, false).isEmpty()
					&& InventoryHelper.insertIntoInventory(remainingItems, clonedHandler, false).isEmpty();
		}
		return InventoryHelper.insertIntoInventory(result, inventoryHandler, true).isEmpty();
	}

	@Override
	public FilterLogic getFilterLogic() {
		return filterLogic;
	}

	public boolean shouldCompactNonUncraftable() {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag != null && tag.getBoolean(COMPACT_NON_UNCRAFTABLE_TAG);
	}

	public void setCompactNonUncraftable(boolean shouldCompactNonUncraftable) {
		StorageNbt.getOrCreateTag(upgrade).setBoolean(COMPACT_NON_UNCRAFTABLE_TAG, shouldCompactNonUncraftable);
		save();
	}

	@Override
	public void onSlotChange(IItemHandler inventoryHandler, int slot) {
		if (shouldWorkInGUI()) {
			slotsToCompact.add(slot);
		}
	}

	public void setShouldWorkInGUI(boolean shouldWorkInGUI) {
		StorageNbt.getOrCreateTag(upgrade).setBoolean(SHOULD_WORK_IN_GUI_TAG, shouldWorkInGUI);
		save();
	}

	public boolean shouldWorkInGUI() {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag != null && tag.getBoolean(SHOULD_WORK_IN_GUI_TAG);
	}

	@Override
	public void tick(@Nullable Entity entity, World world, BlockPos pos) {
		if (slotsToCompact.isEmpty()) {
			return;
		}

		for (int slot : slotsToCompact) {
			compactSlot(storageWrapper.getInventoryHandler(), slot);
		}

		slotsToCompact.clear();
	}
}
