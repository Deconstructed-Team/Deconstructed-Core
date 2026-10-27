package dev.feldsher.deconstructedcore.upgrades.voiding;

import dev.feldsher.deconstructedcore.api.ISlotChangeResponseUpgrade;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.IFilteredUpgrade;
import dev.feldsher.deconstructedcore.upgrades.IInsertResponseUpgrade;
import dev.feldsher.deconstructedcore.upgrades.IOverflowResponseUpgrade;
import dev.feldsher.deconstructedcore.upgrades.ISlotLimitUpgrade;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.upgrades.PrimaryMatch;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Voids what its filter matches as it's put in, or only what overflows the slots that already hold it.
 * Working in the GUI, it also voids what the player puts into a slot by hand.
 */
public class VoidUpgradeWrapper extends UpgradeWrapperBase<VoidUpgradeWrapper, VoidUpgradeItem>
		implements IInsertResponseUpgrade, IFilteredUpgrade, ISlotChangeResponseUpgrade, ITickableUpgrade, IOverflowResponseUpgrade, ISlotLimitUpgrade {
	private static final String SHOULD_WORK_IN_GUI_TAG = "should_work_in_gui";
	private static final String SHOULD_VOID_OVERFLOW_TAG = "should_void_overflow";
	private final FilterLogic filterLogic;
	private final Set<Integer> slotsToVoid = new HashSet<>();
	private boolean shouldVoidOverflow;

	public VoidUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new FilterLogic(upgrade, this.upgradeSaveHandler, upgradeItem.getFilterSlotCount(), "filter_attributes");
		filterLogic.setAllowByDefault(true);
		setShouldVoidOverflowDefaultOrLoadFromNbt(false);
	}

	@Override
	public ItemStack onBeforeInsert(IItemHandler inventoryHandler, int slot, ItemStack stack, boolean simulate) {
		if (shouldVoidOverflow && inventoryHandler.getStackInSlot(slot).isEmpty() && (!filterLogic.shouldMatchComponents() || !filterLogic.shouldMatchDurability() || filterLogic.getPrimaryMatch() != PrimaryMatch.ITEM) && filterLogic.matchesFilter(stack)) {
			for (int s = 0; s < inventoryHandler.getSlots(); s++) {
				if (s == slot) {
					continue;
				}
				if (stackMatchesFilterStack(inventoryHandler.getStackInSlot(s), stack)) {
					return ItemStack.EMPTY;
				}
			}
			return stack;
		}

		return !shouldVoidOverflow && filterLogic.matchesFilter(stack) ? ItemStack.EMPTY : stack;
	}

	@Override
	public void onAfterInsert(IItemHandler inventoryHandler, int slot) {
		//noop
	}

	@Override
	public FilterLogic getFilterLogic() {
		return filterLogic;
	}

	private boolean getBoolean(String key, boolean defaultValue) {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag != null && tag.hasKey(key) ? tag.getBoolean(key) : defaultValue;
	}

	public void setShouldWorkdInGUI(boolean shouldWorkdInGUI) {
		StorageNbt.getOrCreateTag(upgrade).setBoolean(SHOULD_WORK_IN_GUI_TAG, shouldWorkdInGUI);
		save();
	}

	public boolean shouldWorkInGUI() {
		return getBoolean(SHOULD_WORK_IN_GUI_TAG, false);
	}

	public void setShouldVoidOverflow(boolean shouldVoidOverflow) {
		if (!shouldVoidOverflow && !upgradeItem.isVoidAnythingEnabled()) {
			return;
		}

		this.shouldVoidOverflow = shouldVoidOverflow;
		StorageNbt.getOrCreateTag(upgrade).setBoolean(SHOULD_VOID_OVERFLOW_TAG, shouldVoidOverflow);
		save();
	}

	public void setShouldVoidOverflowDefaultOrLoadFromNbt(boolean shouldVoidOverflowDefault) {
		shouldVoidOverflow = !upgradeItem.isVoidAnythingEnabled() || getBoolean(SHOULD_VOID_OVERFLOW_TAG, shouldVoidOverflowDefault);
	}

	public boolean shouldVoidOverflow() {
		return !upgradeItem.isVoidAnythingEnabled() || shouldVoidOverflow;
	}

	@Override
	public void onSlotChange(IItemHandler inventoryHandler, int slot) {
		if (!shouldWorkInGUI() || shouldVoidOverflow()) {
			return;
		}

		ItemStack slotStack = inventoryHandler.getStackInSlot(slot);
		if (filterLogic.matchesFilter(slotStack)) {
			slotsToVoid.add(slot);
		}
	}

	@Override
	public void tick(@Nullable Entity entity, World world, BlockPos pos) {
		if (slotsToVoid.isEmpty()) {
			return;
		}

		InventoryHandler storageInventory = storageWrapper.getInventoryHandler();
		for (int slot : slotsToVoid) {
			storageInventory.extractItem(slot, storageInventory.getStackInSlot(slot).getCount(), false);
		}

		slotsToVoid.clear();
	}

	@Override
	public boolean worksInGui() {
		return shouldWorkInGUI();
	}

	@Override
	public ItemStack onOverflow(ItemStack stack) {
		return filterLogic.matchesFilter(stack) ? ItemStack.EMPTY : stack;
	}

	@Override
	public boolean stackMatchesFilter(ItemStack stack) {
		return filterLogic.matchesFilter(stack);
	}

	public boolean isVoidAnythingEnabled() {
		return upgradeItem.isVoidAnythingEnabled();
	}

	@Override
	public int getSlotLimit() {
		return Integer.MAX_VALUE;
	}
}
