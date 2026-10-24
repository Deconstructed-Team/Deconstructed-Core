package dev.feldsher.deconstructedcore.upgrades.voiding;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class VoidUpgradeItem extends UpgradeItemBase<VoidUpgradeWrapper> {
	public static final UpgradeType<VoidUpgradeWrapper> TYPE = new UpgradeType<>(VoidUpgradeWrapper::new);
	private final IntSupplier filterSlotCount;
	private final BooleanSupplier voidAnythingEnabled;

	public VoidUpgradeItem(IntSupplier filterSlotCount, BooleanSupplier voidAnythingEnabled, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.filterSlotCount = filterSlotCount;
		this.voidAnythingEnabled = voidAnythingEnabled;
	}

	@Override
	public UpgradeType<VoidUpgradeWrapper> getType() {
		return TYPE;
	}

	public int getFilterSlotCount() {
		return filterSlotCount.getAsInt();
	}

	public boolean isVoidAnythingEnabled() {
		return voidAnythingEnabled.getAsBoolean();
	}
}
