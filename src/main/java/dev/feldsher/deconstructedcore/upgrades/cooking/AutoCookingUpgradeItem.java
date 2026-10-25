package dev.feldsher.deconstructedcore.upgrades.cooking;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeGroup;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

/** A cooking upgrade that takes what it cooks and burns from the storage, and puts what it cooked back. */
public class AutoCookingUpgradeItem extends UpgradeItemBase<AutoCookingUpgradeWrapper> {
	private final UpgradeType<AutoCookingUpgradeWrapper> type = new UpgradeType<>(AutoCookingUpgradeWrapper::new);
	private final CookingType cookingType;
	private final IAutoCookingUpgradeConfig autoCookingUpgradeConfig;

	public AutoCookingUpgradeItem(CookingType cookingType, IAutoCookingUpgradeConfig autoCookingUpgradeConfig, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.cookingType = cookingType;
		this.autoCookingUpgradeConfig = autoCookingUpgradeConfig;
	}

	@Override
	public UpgradeType<AutoCookingUpgradeWrapper> getType() {
		return type;
	}

	public CookingType getCookingType() {
		return cookingType;
	}

	public IAutoCookingUpgradeConfig getAutoCookingUpgradeConfig() {
		return autoCookingUpgradeConfig;
	}

	@Override
	public UpgradeGroup getUpgradeGroup() {
		return ICookingUpgrade.UPGRADE_GROUP;
	}
}
