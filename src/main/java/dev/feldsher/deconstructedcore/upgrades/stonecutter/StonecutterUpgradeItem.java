package dev.feldsher.deconstructedcore.upgrades.stonecutter;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

/** A stonecutter in the storage's upgrade tab, with the recipes Future MC's stonecutter has. */
public class StonecutterUpgradeItem extends UpgradeItemBase<StonecutterUpgradeWrapper> {
	private static final UpgradeType<StonecutterUpgradeWrapper> TYPE = new UpgradeType<>(StonecutterUpgradeWrapper::new);

	public StonecutterUpgradeItem(IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
	}

	@Override
	public UpgradeType<StonecutterUpgradeWrapper> getType() {
		return TYPE;
	}
}
