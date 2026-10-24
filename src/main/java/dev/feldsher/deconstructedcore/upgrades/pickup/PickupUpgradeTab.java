package dev.feldsher.deconstructedcore.upgrades.pickup;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.UpgradeSettingsTab;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterControl;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterType;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilteredUpgradeContainer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class PickupUpgradeTab extends UpgradeSettingsTab<ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> {
	protected ContentsFilterControl filterLogicControl;

	protected PickupUpgradeTab(ContentsFilteredUpgradeContainer<PickupUpgradeWrapper> upgradeContainer, Position position, StorageScreenBase<?> screen, ITextComponent tabLabel, ITextComponent closedTooltip) {
		super(upgradeContainer, position, screen, tabLabel, closedTooltip);
	}

	@Override
	protected void moveSlotsToTab() {
		filterLogicControl.moveSlotsToView();
	}

	public static class Basic extends PickupUpgradeTab {
		public Basic(ContentsFilteredUpgradeContainer<PickupUpgradeWrapper> upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow, ButtonDefinition.Toggle<ContentsFilterType> contentsFilterButton) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("pickup"), new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey("pickup") + TranslationHelper.TOOLTIP_SUFFIX));
			filterLogicControl = addHideableChild(new ContentsFilterControl.Basic(screen, new Position(x + 3, y + 24), getContainer().getFilterLogicContainer(),
					slotsPerRow, contentsFilterButton));
		}
	}

	public static class Advanced extends PickupUpgradeTab {
		public Advanced(ContentsFilteredUpgradeContainer<PickupUpgradeWrapper> upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow, ButtonDefinition.Toggle<ContentsFilterType> contentsFilterButton) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("advanced_pickup"), new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey("advanced_pickup") + TranslationHelper.TOOLTIP_SUFFIX));
			filterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(screen, new Position(x + 3, y + 24), getContainer().getFilterLogicContainer(),
					slotsPerRow, contentsFilterButton));
		}
	}
}
