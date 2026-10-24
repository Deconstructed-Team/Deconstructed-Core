package dev.feldsher.deconstructedcore.upgrades.pickup;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

import java.util.function.IntSupplier;

public class PickupUpgradeItem extends UpgradeItemBase<PickupUpgradeWrapper> {
	public static final UpgradeType<PickupUpgradeWrapper> TYPE = new UpgradeType<>(PickupUpgradeWrapper::new);

	private final IntSupplier filterSlotCount;

	public PickupUpgradeItem(IntSupplier filterSlotCount, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.filterSlotCount = filterSlotCount;
	}

	public int getFilterSlotCount() {
		return filterSlotCount.getAsInt();
	}

	@Override
	public UpgradeType<PickupUpgradeWrapper> getType() {
		return TYPE;
	}
}
