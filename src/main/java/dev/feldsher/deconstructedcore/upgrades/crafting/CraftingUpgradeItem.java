package dev.feldsher.deconstructedcore.upgrades.crafting;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

/** A crafting table in the storage's upgrade tab, its grid kept in the upgrade. */
public class CraftingUpgradeItem extends UpgradeItemBase<CraftingUpgradeWrapper> {
	private static final UpgradeType<CraftingUpgradeWrapper> TYPE = new UpgradeType<>(CraftingUpgradeWrapper::new);

	public CraftingUpgradeItem(IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
	}

	@Override
	public UpgradeType<CraftingUpgradeWrapper> getType() {
		return TYPE;
	}
}
