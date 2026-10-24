package dev.feldsher.deconstructedcore.upgrades.filter;

import dev.feldsher.deconstructedcore.api.IIOFilterUpgrade;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterLogic;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.IContentsFilteredUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Optional;
import java.util.function.Consumer;

/** Filters what machines put into the storage and take out of it. */
public class FilterUpgradeWrapper extends UpgradeWrapperBase<FilterUpgradeWrapper, FilterUpgradeItem> implements IContentsFilteredUpgrade, IIOFilterUpgrade {
	private static final String DIRECTION_TAG = "direction";
	private final ContentsFilterLogic filterLogic;

	public FilterUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new ContentsFilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(), storageWrapper::getInventoryHandler, "filter_attributes");
	}

	public void setDirection(Direction direction) {
		StorageNbt.getOrCreateTag(upgrade).setString(DIRECTION_TAG, direction.getSerializedName());
		save();
	}

	public Direction getDirection() {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag != null && tag.hasKey(DIRECTION_TAG) ? Direction.fromName(tag.getString(DIRECTION_TAG)) : Direction.BOTH;
	}

	@Override
	public ContentsFilterLogic getFilterLogic() {
		return filterLogic;
	}

	@Override
	public Optional<FilterLogic> getInputFilter() {
		Direction direction = getDirection();
		return direction == Direction.INPUT || direction == Direction.BOTH ? Optional.of(getFilterLogic()) : Optional.empty();
	}

	@Override
	public Optional<FilterLogic> getOutputFilter() {
		Direction direction = getDirection();
		return direction == Direction.OUTPUT || direction == Direction.BOTH ? Optional.of(getFilterLogic()) : Optional.empty();
	}
}
