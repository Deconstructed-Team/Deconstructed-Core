package dev.feldsher.deconstructedcore.upgrades.compacting;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.UpgradeSettingsTab;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinitions;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.FilterLogicContainer;
import dev.feldsher.deconstructedcore.upgrades.FilterLogicControl;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public abstract class CompactingUpgradeTab extends UpgradeSettingsTab<CompactingUpgradeContainer> {
	public static final ButtonDefinition.Toggle<Boolean> COMPACT_UNCRAFTABLE = ButtonDefinitions.createToggleButtonDefinition(
			ButtonDefinitions.getBooleanStateData(
					GuiHelper.getButtonStateData(new UV(80, 32), TranslationHelper.INSTANCE.translUpgradeButton("compact_anything"), Dimension.SQUARE_16, new Position(1, 1)),
					GuiHelper.getButtonStateData(new UV(80, 48), TranslationHelper.INSTANCE.translUpgradeButton("compact_only_uncraftable"), Dimension.SQUARE_16, new Position(1, 1))
			));

	protected FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> filterLogicControl;

	protected CompactingUpgradeTab(CompactingUpgradeContainer container, Position position, StorageScreenBase<?> screen, ITextComponent tabLabel, ITextComponent closedTooltip) {
		super(container, position, screen, tabLabel, closedTooltip);
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), COMPACT_UNCRAFTABLE, button -> getContainer().setCompactNonUncraftable(!getContainer().shouldCompactNonUncraftable()),
				getContainer()::shouldCompactNonUncraftable));
		addHideableChild(new ToggleButton<>(new Position(x + 21, y + 24), ButtonDefinitions.WORK_IN_GUI, button -> getContainer().setShouldWorkInGUI(!getContainer().shouldWorkInGUI()),
				getContainer()::shouldWorkInGUI));
	}

	protected static ITextComponent tooltip(String upgradeName) {
		return new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey(upgradeName) + TranslationHelper.TOOLTIP_SUFFIX);
	}

	@Override
	protected void moveSlotsToTab() {
		filterLogicControl.moveSlotsToView();
	}

	public static class Basic extends CompactingUpgradeTab {
		public Basic(CompactingUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("compacting"), tooltip("compacting"));
			filterLogicControl = addHideableChild(new FilterLogicControl.Basic(screen, new Position(x + 3, y + 44), getContainer().getFilterLogicContainer(),
					slotsPerRow));
		}
	}

	public static class Advanced extends CompactingUpgradeTab {
		public Advanced(CompactingUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("advanced_compacting"), tooltip("advanced_compacting"));
			filterLogicControl = addHideableChild(new FilterLogicControl.Advanced(screen, new Position(x + 3, y + 44), getContainer().getFilterLogicContainer(),
					slotsPerRow));
		}
	}
}
