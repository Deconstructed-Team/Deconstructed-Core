package dev.feldsher.deconstructedcore.upgrades.pump;

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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/** Whether the pump pumps in or out; the advanced one also chooses the world and hands, and filters its fluids. */
public class PumpUpgradeTab extends UpgradeSettingsTab<PumpUpgradeContainer> {
	private static final ButtonDefinition.Toggle<Boolean> IS_INPUT = ButtonDefinitions.createToggleButtonDefinition(
			ButtonDefinitions.getBooleanStateData(
					GuiHelper.getButtonStateData(new UV(144, 0), TranslationHelper.INSTANCE.translUpgradeButton("pump_input"), Dimension.SQUARE_16, new Position(1, 1)),
					GuiHelper.getButtonStateData(new UV(160, 0), TranslationHelper.INSTANCE.translUpgradeButton("pump_output"), Dimension.SQUARE_16, new Position(1, 1))
			));
	private static final ButtonDefinition.Toggle<Boolean> INTERACT_WITH_WORLD = ButtonDefinitions.createToggleButtonDefinition(
			ButtonDefinitions.getBooleanStateData(
					GuiHelper.getButtonStateData(new UV(176, 0), TranslationHelper.INSTANCE.translUpgradeButton("interact_with_world"), Dimension.SQUARE_16, new Position(1, 1)),
					GuiHelper.getButtonStateData(new UV(192, 0), TranslationHelper.INSTANCE.translUpgradeButton("do_not_interact_with_world"), Dimension.SQUARE_16, new Position(1, 1))
			));
	private static final ButtonDefinition.Toggle<Boolean> INTERACT_WITH_HAND = ButtonDefinitions.createToggleButtonDefinition(
			ButtonDefinitions.getBooleanStateData(
					GuiHelper.getButtonStateData(new UV(208, 0), TranslationHelper.INSTANCE.translUpgradeButton("interact_with_hand"), Dimension.SQUARE_16, new Position(1, 1)),
					GuiHelper.getButtonStateData(new UV(224, 0), TranslationHelper.INSTANCE.translUpgradeButton("do_not_interact_with_hand"), Dimension.SQUARE_16, new Position(1, 1))
			));

	protected PumpUpgradeTab(PumpUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, String upgradeName) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade(upgradeName), tooltip(upgradeName));
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), IS_INPUT, button -> {
			if (button == 0) {
				getContainer().setIsInput(!getContainer().isInput());
			}
		}, () -> getContainer().isInput()));
	}

	private static ITextComponent tooltip(String upgradeName) {
		return new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey(upgradeName) + TranslationHelper.TOOLTIP_SUFFIX);
	}

	@Override
	protected void moveSlotsToTab() {
		//noop
	}

	public static class Basic extends PumpUpgradeTab {
		public Basic(PumpUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
			super(upgradeContainer, position, screen, "pump");
		}
	}

	public static class Advanced extends PumpUpgradeTab {
		public Advanced(PumpUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
			super(upgradeContainer, position, screen, "advanced_pump");
			addHideableChild(new ToggleButton<>(new Position(x + 21, y + 24), INTERACT_WITH_WORLD, button -> {
				if (button == 0) {
					getContainer().setInteractWithWorld(!getContainer().shouldInteractWithWorld());
				}
			}, () -> getContainer().shouldInteractWithWorld()));
			addHideableChild(new ToggleButton<>(new Position(x + 39, y + 24), INTERACT_WITH_HAND, button -> {
				if (button == 0) {
					getContainer().setInteractWithHand(!getContainer().shouldInteractWithHand());
				}
			}, () -> getContainer().shouldInteractWithHand()));
			addHideableChild(new FluidFilterControl(new Position(x + 3, y + 44), getContainer().getFluidFilterContainer()));
		}
	}
}
