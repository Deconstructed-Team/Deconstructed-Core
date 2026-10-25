package dev.feldsher.deconstructedcore.upgrades.feeding;

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

import java.util.EnumMap;
import java.util.Map;

public class FeedingUpgradeTab extends UpgradeSettingsTab<FeedingUpgradeContainer> {
	public static final ButtonDefinition.Toggle<HungerLevel> HUNGER_LEVEL;
	public static final ButtonDefinition.Toggle<Boolean> FEED_IMMEDIATELY_WHEN_HURT = ButtonDefinitions.createToggleButtonDefinition(
			ButtonDefinitions.getBooleanStateData(
					GuiHelper.getButtonStateData(new UV(96, 16), TranslationHelper.INSTANCE.translUpgradeButton("feed_immediately_when_hurt"), Dimension.SQUARE_16, new Position(1, 1)),
					GuiHelper.getButtonStateData(new UV(112, 16), TranslationHelper.INSTANCE.translUpgradeButton("do_not_consider_health"), Dimension.SQUARE_16, new Position(1, 1))
			));

	static {
		Map<HungerLevel, ToggleButton.StateData> states = new EnumMap<>(HungerLevel.class);
		states.put(HungerLevel.ANY, GuiHelper.getButtonStateData(new UV(128, 0), TranslationHelper.INSTANCE.translUpgradeButton("hunger_level_any"), Dimension.SQUARE_16, new Position(1, 1)));
		states.put(HungerLevel.HALF, GuiHelper.getButtonStateData(new UV(112, 0), TranslationHelper.INSTANCE.translUpgradeButton("hunger_level_half"), Dimension.SQUARE_16, new Position(1, 1)));
		states.put(HungerLevel.FULL, GuiHelper.getButtonStateData(new UV(96, 0), TranslationHelper.INSTANCE.translUpgradeButton("hunger_level_full"), Dimension.SQUARE_16, new Position(1, 1)));
		HUNGER_LEVEL = ButtonDefinitions.createToggleButtonDefinition(states);
	}

	protected FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> filterLogicControl;

	protected FeedingUpgradeTab(FeedingUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, ITextComponent tabLabel, ITextComponent closedTooltip) {
		super(upgradeContainer, position, screen, tabLabel, closedTooltip);
	}

	protected static ITextComponent tooltip(String upgradeName) {
		return new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey(upgradeName) + TranslationHelper.TOOLTIP_SUFFIX);
	}

	@Override
	protected void moveSlotsToTab() {
		filterLogicControl.moveSlotsToView();
	}

	public static class Basic extends FeedingUpgradeTab {
		public Basic(FeedingUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("feeding"), tooltip("feeding"));
			filterLogicControl = addHideableChild(new FilterLogicControl.Basic(screen, new Position(x + 3, y + 24), getContainer().getFilterLogicContainer(),
					slotsPerRow));
		}
	}

	public static class Advanced extends FeedingUpgradeTab {
		public Advanced(FeedingUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("advanced_feeding"), tooltip("advanced_feeding"));
			addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), HUNGER_LEVEL,
					button -> getContainer().setFeedAtHungerLevel(getContainer().getFeedAtHungerLevel().next()),
					() -> getContainer().getFeedAtHungerLevel()));
			addHideableChild(new ToggleButton<>(new Position(x + 21, y + 24), FEED_IMMEDIATELY_WHEN_HURT,
					button -> getContainer().setFeedImmediatelyWhenHurt(!getContainer().shouldFeedImmediatelyWhenHurt()),
					() -> getContainer().shouldFeedImmediatelyWhenHurt()));

			filterLogicControl = addHideableChild(new FilterLogicControl.Advanced(screen, new Position(x + 3, y + 44), getContainer().getFilterLogicContainer(),
					slotsPerRow));
		}
	}
}
