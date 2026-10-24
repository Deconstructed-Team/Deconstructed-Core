package dev.feldsher.deconstructedcore.upgrades.pickup;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterLogic;
import dev.feldsher.deconstructedcore.upgrades.IContentsFilteredUpgrade;
import dev.feldsher.deconstructedcore.upgrades.IPickupResponseUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class PickupUpgradeWrapper extends UpgradeWrapperBase<PickupUpgradeWrapper, PickupUpgradeItem>
		implements IPickupResponseUpgrade, IContentsFilteredUpgrade {
	public static final String FILTER_ATTRIBUTES = "filter_attributes";
	private final ContentsFilterLogic filterLogic;

	public PickupUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new ContentsFilterLogic(upgrade, stack -> save(), upgradeItem.getFilterSlotCount(), storageWrapper::getInventoryHandler, FILTER_ATTRIBUTES);
	}

	@Override
	public ItemStack pickup(World world, ItemStack stack, boolean simulate) {
		if (!filterLogic.matchesFilter(stack)) {
			return stack;
		}

		return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
	}

	@Override
	public ContentsFilterLogic getFilterLogic() {
		return filterLogic;
	}
}
