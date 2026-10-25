package dev.feldsher.deconstructedcore.upgrades.compacting;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

import java.util.function.IntSupplier;

public class CompactingUpgradeItem extends UpgradeItemBase<CompactingUpgradeWrapper> {
	private static final UpgradeType<CompactingUpgradeWrapper> TYPE = new UpgradeType<>(CompactingUpgradeWrapper::new);
	private final boolean shouldCompactThreeByThree;
	private final IntSupplier filterSlotCount;

	public CompactingUpgradeItem(boolean shouldCompactThreeByThree, IntSupplier filterSlotCount, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.shouldCompactThreeByThree = shouldCompactThreeByThree;
		this.filterSlotCount = filterSlotCount;
	}

	@Override
	public UpgradeType<CompactingUpgradeWrapper> getType() {
		return TYPE;
	}

	public boolean shouldCompactThreeByThree() {
		return shouldCompactThreeByThree;
	}

	public int getFilterSlotCount() {
		return filterSlotCount.getAsInt();
	}
}
