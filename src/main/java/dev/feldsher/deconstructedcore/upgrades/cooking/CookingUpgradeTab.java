package dev.feldsher.deconstructedcore.upgrades.cooking;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.UpgradeSettingsTab;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import net.minecraft.util.text.TextComponentTranslation;

/** The furnace, named by what it cooks: "smelting", "smoking" or "blasting". */
public class CookingUpgradeTab extends UpgradeSettingsTab<CookingUpgradeContainer> {
	private final CookingLogicControl cookingLogicControl;

	public CookingUpgradeTab(CookingUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, String upgradeName) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade(upgradeName),
				new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey(upgradeName) + TranslationHelper.TOOLTIP_SUFFIX));
		cookingLogicControl = addHideableChild(new CookingLogicControl(new Position(x + 3, y + 24), getContainer().getCookingLogicContainer()));
	}

	@Override
	protected void moveSlotsToTab() {
		cookingLogicControl.moveSlotsToView(screen.getGuiLeft(), screen.getGuiTop());
	}
}
