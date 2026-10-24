package dev.feldsher.deconstructedcore.upgrades.magnet;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

import java.util.function.IntSupplier;

public class MagnetUpgradeItem extends UpgradeItemBase<MagnetUpgradeWrapper> {
	public static final UpgradeType<MagnetUpgradeWrapper> TYPE = new UpgradeType<>(MagnetUpgradeWrapper::new);
	private final IntSupplier radius;
	private final IntSupplier filterSlotCount;

	public MagnetUpgradeItem(IntSupplier radius, IntSupplier filterSlotCount, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.radius = radius;
		this.filterSlotCount = filterSlotCount;
	}

	@Override
	public UpgradeType<MagnetUpgradeWrapper> getType() {
		return TYPE;
	}

	public int getFilterSlotCount() {
		return filterSlotCount.getAsInt();
	}

	public int getRadius() {
		return radius.getAsInt();
	}
}
