package dev.feldsher.deconstructedcore.upgrades.cooking;

import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.upgrades.UpgradeGroup;

public interface ICookingUpgrade {
	UpgradeGroup UPGRADE_GROUP = new UpgradeGroup("furnace_upgrades", TranslationHelper.INSTANCE.translUpgradeGroup("cooking_upgrades"));

	CookingLogic getCookingLogic();
}
