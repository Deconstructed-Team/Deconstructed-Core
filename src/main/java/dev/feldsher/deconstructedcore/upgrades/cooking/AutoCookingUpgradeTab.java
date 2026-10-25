package dev.feldsher.deconstructedcore.upgrades.cooking;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.UpgradeSettingsTab;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.FilterLogicContainer;
import dev.feldsher.deconstructedcore.upgrades.FilterLogicControl;
import net.minecraft.util.text.TextComponentTranslation;

/** The input filter, the furnace and the fuel filter, named by what it cooks: "auto_smelting" and so on. */
public class AutoCookingUpgradeTab extends UpgradeSettingsTab<AutoCookingUpgradeContainer> {
	private final FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> inputFilterLogicControl;
	private final FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> fuelFilterLogicControl;
	private final CookingLogicControl cookingLogicControl;

	public AutoCookingUpgradeTab(AutoCookingUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, String upgradeName,
			int inputFilterSlotsPerRow, int fuelFilterSlotsPerRow) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade(upgradeName),
				new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey(upgradeName) + TranslationHelper.TOOLTIP_SUFFIX));
		inputFilterLogicControl = addHideableChild(new FilterLogicControl.Advanced(screen, new Position(x + 3, y + 24), getContainer().getInputFilterLogicContainer(),
				inputFilterSlotsPerRow));
		cookingLogicControl = addHideableChild(new CookingLogicControl(new Position(x + 3, y + 84), getContainer().getCookingLogicContainer()));
		fuelFilterLogicControl = addHideableChild(new FilterLogicControl<>(screen, new Position(x + 3, y + 142), getContainer().getFuelFilterLogicContainer(),
				fuelFilterSlotsPerRow));
	}

	@Override
	protected void moveSlotsToTab() {
		inputFilterLogicControl.moveSlotsToView();
		cookingLogicControl.moveSlotsToView(screen.getGuiLeft(), screen.getGuiTop());
		fuelFilterLogicControl.moveSlotsToView();
	}
}
