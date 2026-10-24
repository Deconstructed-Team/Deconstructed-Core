package dev.feldsher.deconstructedcore.upgrades.magnet;

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
import net.minecraft.util.text.TextFormatting;

public class MagnetUpgradeTab extends UpgradeSettingsTab<MagnetUpgradeContainer> {
	private static final ButtonDefinition.Toggle<Boolean> PICKUP_ITEMS = ButtonDefinitions.createToggleButtonDefinition(
			ButtonDefinitions.getBooleanStateData(
					GuiHelper.getButtonStateData(new UV(128, 48), TranslationHelper.INSTANCE.translUpgradeButton("pickup_items"), Dimension.SQUARE_16, new Position(1, 1)),
					GuiHelper.getButtonStateData(new UV(144, 48), TranslationHelper.INSTANCE.translUpgradeButton("do_not_pickup_items"), Dimension.SQUARE_16, new Position(1, 1))
			));

	private static final ButtonDefinition.Toggle<Boolean> PICKUP_XP = ButtonDefinitions.createToggleButtonDefinition(
			ButtonDefinitions.getBooleanStateData(
					GuiHelper.getButtonStateData(new UV(96, 48), Dimension.SQUARE_16, new Position(1, 1),
							new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("pickup_xp")), detail(TranslationHelper.INSTANCE.translUpgradeButton("pickup_xp.detail"))),
					GuiHelper.getButtonStateData(new UV(112, 48), TranslationHelper.INSTANCE.translUpgradeButton("do_not_pickup_xp"), Dimension.SQUARE_16, new Position(1, 1))
			));

	protected ContentsFilterControl filterLogicControl;

	protected MagnetUpgradeTab(MagnetUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, ITextComponent tabLabel, ITextComponent closedTooltip) {
		super(upgradeContainer, position, screen, tabLabel, closedTooltip);
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), PICKUP_ITEMS,
				button -> getContainer().setPickupItems(!getContainer().shouldPickupItems()),
				() -> getContainer().shouldPickupItems()));
		addHideableChild(new ToggleButton<>(new Position(x + 21, y + 24), PICKUP_XP,
				button -> getContainer().setPickupXp(!getContainer().shouldPickupXp()),
				() -> getContainer().shouldPickupXp()));
	}

	private static ITextComponent detail(String translationKey) {
		ITextComponent component = new TextComponentTranslation(translationKey);
		component.getStyle().setColor(TextFormatting.DARK_GRAY).setItalic(true);
		return component;
	}

	@Override
	protected void moveSlotsToTab() {
		filterLogicControl.moveSlotsToView();
	}

	private static ITextComponent tooltip(String upgradeName) {
		return new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey(upgradeName) + TranslationHelper.TOOLTIP_SUFFIX);
	}

	public static class Basic extends MagnetUpgradeTab {
		public Basic(MagnetUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow, ButtonDefinition.Toggle<ContentsFilterType> contentsFilterButton) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("magnet"), tooltip("magnet"));
			filterLogicControl = addHideableChild(new ContentsFilterControl.Basic(screen, new Position(x + 3, y + 44), getContainer().getFilterLogicContainer(),
					slotsPerRow, contentsFilterButton));
		}
	}

	public static class Advanced extends MagnetUpgradeTab {
		public Advanced(MagnetUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow, ButtonDefinition.Toggle<ContentsFilterType> contentsFilterButton) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("advanced_magnet"), tooltip("advanced_magnet"));
			filterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(screen, new Position(x + 3, y + 44), getContainer().getFilterLogicContainer(),
					slotsPerRow, contentsFilterButton));
		}
	}
}
