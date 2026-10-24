package dev.feldsher.deconstructedcore.upgrades.filter;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

import java.util.function.IntSupplier;

public class FilterUpgradeItem extends UpgradeItemBase<FilterUpgradeWrapper> {
	public static final UpgradeType<FilterUpgradeWrapper> TYPE = new UpgradeType<>(FilterUpgradeWrapper::new);
	private final IntSupplier filterSlotCount;

	public FilterUpgradeItem(IntSupplier filterSlotCount, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.filterSlotCount = filterSlotCount;
	}

	@Override
	public UpgradeType<FilterUpgradeWrapper> getType() {
		return TYPE;
	}

	public int getFilterSlotCount() {
		return filterSlotCount.getAsInt();
	}
}
