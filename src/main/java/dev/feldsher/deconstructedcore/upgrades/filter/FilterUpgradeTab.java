package dev.feldsher.deconstructedcore.upgrades.filter;

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
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterControl;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.HashMap;
import java.util.Map;

public abstract class FilterUpgradeTab extends UpgradeSettingsTab<FilterUpgradeContainer> {
	private static final ButtonDefinition.Toggle<Direction> DIRECTION;

	static {
		Map<Direction, ToggleButton.StateData> states = new HashMap<>();
		states.put(Direction.BOTH, GuiHelper.getButtonStateData(new UV(0, 32), TranslationHelper.INSTANCE.translUpgradeButton("direction_both"), Dimension.SQUARE_16, new Position(1, 1)));
		states.put(Direction.INPUT, GuiHelper.getButtonStateData(new UV(16, 32), TranslationHelper.INSTANCE.translUpgradeButton("direction_input"), Dimension.SQUARE_16, new Position(1, 1)));
		states.put(Direction.OUTPUT, GuiHelper.getButtonStateData(new UV(32, 32), TranslationHelper.INSTANCE.translUpgradeButton("direction_output"), Dimension.SQUARE_16, new Position(1, 1)));
		DIRECTION = ButtonDefinitions.createToggleButtonDefinition(states);
	}

	protected ContentsFilterControl filterLogicControl;

	protected FilterUpgradeTab(FilterUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, ITextComponent tabLabel, ITextComponent closedTooltip) {
		super(upgradeContainer, position, screen, tabLabel, closedTooltip);
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), DIRECTION,
				button -> getContainer().setDirection(getContainer().getDirection().next()), () -> getContainer().getDirection()));
	}

	@Override
	protected void moveSlotsToTab() {
		filterLogicControl.moveSlotsToView();
	}

	static ITextComponent tooltip(String upgradeName) {
		return new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey(upgradeName) + TranslationHelper.TOOLTIP_SUFFIX);
	}

	public static class Basic extends FilterUpgradeTab {
		public Basic(FilterUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow, ButtonDefinition.Toggle<ContentsFilterType> contentsFilterButton) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("filter"), tooltip("filter"));
			filterLogicControl = addHideableChild(new ContentsFilterControl.Basic(screen, new Position(x + 3, y + 44), getContainer().getFilterLogicContainer(),
					slotsPerRow, contentsFilterButton));
		}
	}

	public static class Advanced extends FilterUpgradeTab {
		public Advanced(FilterUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow, ButtonDefinition.Toggle<ContentsFilterType> contentsFilterButton) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("advanced_filter"), tooltip("advanced_filter"));
			filterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(screen, new Position(x + 3, y + 44), getContainer().getFilterLogicContainer(),
					slotsPerRow, contentsFilterButton));
		}
	}
}
