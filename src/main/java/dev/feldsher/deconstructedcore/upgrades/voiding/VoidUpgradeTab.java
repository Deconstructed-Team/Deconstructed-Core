package dev.feldsher.deconstructedcore.upgrades.voiding;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.UpgradeSettingsTab;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinitions;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.FilterLogicContainer;
import dev.feldsher.deconstructedcore.upgrades.FilterLogicControl;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.getButtonStateData;

public class VoidUpgradeTab extends UpgradeSettingsTab<VoidUpgradeContainer> {
	private static final ButtonDefinition.Toggle<Boolean> VOID_OVERFLOW = ButtonDefinitions.createToggleButtonDefinition(
			ButtonDefinitions.getBooleanStateData(
					getButtonStateData(new UV(224, 16), Dimension.SQUARE_16, new Position(1, 1), voidOverflowTooltip(), voidOverflowTooltipDetail()),
					getButtonStateData(new UV(208, 16), TranslationHelper.INSTANCE.translUpgradeButton("void_any"), Dimension.SQUARE_16, new Position(1, 1))
			));
	private static final ButtonDefinition.Toggle<Boolean> VOID_OVERFLOW_ONLY = ButtonDefinitions.createToggleButtonDefinition(
			ButtonDefinitions.getBooleanStateData(
					getButtonStateData(new UV(224, 16), Dimension.SQUARE_16, new Position(1, 1), voidOverflowTooltip(), voidOverflowTooltipDetail(), voidAnythingDisabledTooltip()),
					getButtonStateData(new UV(208, 16), TranslationHelper.INSTANCE.translUpgradeButton("void_any"), Dimension.SQUARE_16, new Position(1, 1))
			));

	protected FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> filterLogicControl;

	protected VoidUpgradeTab(VoidUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, ITextComponent tabLabel, ITextComponent closedTooltip) {
		super(upgradeContainer, position, screen, tabLabel, closedTooltip);
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), ButtonDefinitions.WORK_IN_GUI, button -> getContainer().setShouldWorkdInGUI(!getContainer().shouldWorkInGUI()),
				getContainer()::shouldWorkInGUI));
		addHideableChild(new ToggleButton<>(new Position(x + 21, y + 24), getContainer().getUpgradeWrapper().isVoidAnythingEnabled() ? VOID_OVERFLOW : VOID_OVERFLOW_ONLY,
				button -> getContainer().setShouldVoidOverflow(!getContainer().shouldVoidOverflow()), getContainer()::shouldVoidOverflow));
	}

	private static ITextComponent voidOverflowTooltip() {
		return new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("void_overflow"));
	}

	private static ITextComponent voidOverflowTooltipDetail() {
		ITextComponent component = new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("void_overflow.detail"));
		component.getStyle().setColor(TextFormatting.GRAY);
		return component;
	}

	private static ITextComponent voidAnythingDisabledTooltip() {
		ITextComponent component = new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("void_anything_disabled"));
		component.getStyle().setColor(TextFormatting.RED);
		return component;
	}

	@Override
	protected void moveSlotsToTab() {
		filterLogicControl.moveSlotsToView();
	}

	private static ITextComponent tooltip(String upgradeName) {
		return new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey(upgradeName) + TranslationHelper.TOOLTIP_SUFFIX);
	}

	public static class Basic extends VoidUpgradeTab {
		public Basic(VoidUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("void"), tooltip("void"));
			filterLogicControl = addHideableChild(new FilterLogicControl.Basic(screen, new Position(x + 3, y + 44), getContainer().getFilterLogicContainer(),
					slotsPerRow));
		}
	}

	public static class Advanced extends VoidUpgradeTab {
		public Advanced(VoidUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("advanced_void"), tooltip("advanced_void"));
			filterLogicControl = addHideableChild(new FilterLogicControl.Advanced(screen, new Position(x + 3, y + 44), getContainer().getFilterLogicContainer(),
					slotsPerRow));
		}
	}
}
