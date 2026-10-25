package dev.feldsher.deconstructedcore.upgrades.cooking;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeGroup;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

/** A furnace, smoker or blast furnace in the storage's upgrade tab. */
public class CookingUpgradeItem extends UpgradeItemBase<CookingUpgradeWrapper> {
	private final UpgradeType<CookingUpgradeWrapper> type = new UpgradeType<>(CookingUpgradeWrapper::new);
	private final CookingType cookingType;
	private final ICookingUpgradeConfig cookingUpgradeConfig;

	public CookingUpgradeItem(CookingType cookingType, ICookingUpgradeConfig cookingUpgradeConfig, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.cookingType = cookingType;
		this.cookingUpgradeConfig = cookingUpgradeConfig;
	}

	@Override
	public UpgradeType<CookingUpgradeWrapper> getType() {
		return type;
	}

	public CookingType getCookingType() {
		return cookingType;
	}

	public ICookingUpgradeConfig getCookingUpgradeConfig() {
		return cookingUpgradeConfig;
	}

	@Override
	public UpgradeGroup getUpgradeGroup() {
		return ICookingUpgrade.UPGRADE_GROUP;
	}
}
