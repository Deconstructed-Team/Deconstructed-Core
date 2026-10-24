package dev.feldsher.deconstructedcore.settings.main;

import dev.feldsher.deconstructedcore.client.gui.SettingsScreen;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonBase;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinitions;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import dev.feldsher.deconstructedcore.settings.SettingsTab;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntConsumer;

import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.getButtonStateData;

/** The main settings: whose they are, and the toggles. Keeping the search phrase comes with the search box. */
public class MainSettingsTab<T extends MainSettingsContainer> extends SettingsTab<T> {
	private static final ButtonDefinition.Toggle<Boolean> SHIFT_CLICK_INTO_OPEN_TAB = ButtonDefinitions.createToggleButtonDefinition(
			ButtonDefinitions.getBooleanStateData(
					getButtonStateData(new UV(80, 32), Dimension.SQUARE_16, new Position(1, 1), settingTooltip("shift_click_open_tab.on")),
					getButtonStateData(new UV(64, 96), Dimension.SQUARE_16, new Position(1, 1), settingTooltip("shift_click_open_tab.off"))
			));
	private static final ButtonDefinition.Toggle<Boolean> KEEP_TAB_OPEN = ButtonDefinitions.createToggleButtonDefinition(
			ButtonDefinitions.getBooleanStateData(
					getButtonStateData(new UV(80, 80), Dimension.SQUARE_16, new Position(1, 1), settingTooltip("keep_tab_open.on")),
					getButtonStateData(new UV(80, 96), Dimension.SQUARE_16, new Position(1, 1), settingTooltip("keep_tab_open.off"))
			));
	private static final List<ITextComponent> PLAYER_CONTEXT_TOOLTIP = Arrays.asList(
			new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("context_player.tooltip")),
			gray(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("context_player.tooltip_detail")))
	);

	public MainSettingsTab(T container, Position position, SettingsScreen screen, List<ITextComponent> storageContextTooltip, ITextComponent storageContextTitle,
			String tabLabelTranslKey, String tabTooltipTranslKey, Function<IntConsumer, ButtonBase> getTabButton) {
		super(container, position, screen, new TextComponentTranslation(tabLabelTranslKey), new TextComponentTranslation(tabTooltipTranslKey), getTabButton);
		addHideableChild(new ContextButton(new Position(x + 3, y + 24), button -> container.toggleContext(),
				() -> container.getContext() == Context.PLAYER ? new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("context_player")) : storageContextTitle,
				() -> container.getContext() == Context.PLAYER ? PLAYER_CONTEXT_TOOLTIP : storageContextTooltip));
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + 46), SHIFT_CLICK_INTO_OPEN_TAB, button -> container.toggleShiftClickIntoOpenTab(), container::shouldShiftClickIntoOpenTab));
		addHideableChild(new ToggleButton<>(new Position(x + 21, y + 46), KEEP_TAB_OPEN, button -> container.toggleKeepTabOpen(), container::shouldKeepTabOpen));
	}

	private static List<ITextComponent> settingTooltip(String buttonName) {
		return Arrays.asList(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton(buttonName)),
				gray(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton(buttonName + ".tooltip"))));
	}

	protected static ITextComponent gray(ITextComponent component) {
		component.getStyle().setColor(TextFormatting.GRAY);
		return component;
	}

	@Override
	public Optional<Integer> getSlotOverlayColor(int slotNumber) {
		return Optional.empty();
	}

	@Override
	public void handleSlotClick(Slot slot, int mouseButton) {
		//noop
	}
}
