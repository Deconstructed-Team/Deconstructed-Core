package dev.feldsher.deconstructedcore.upgrades.xppump;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.UpgradeSettingsTab;
import dev.feldsher.deconstructedcore.client.gui.controls.Button;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinitions;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.client.gui.controls.WidgetBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.IntSupplier;

import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_BACKGROUND;
import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND;

/** Which way the pump keeps the player's level and at which; buttons to store or take levels, a number or all. */
public class XpPumpUpgradeTab extends UpgradeSettingsTab<XpPumpUpgradeContainer> {
	private static final ButtonDefinition.Toggle<AutomationDirection> DIRECTION;
	private static final ButtonDefinition.Toggle<Boolean> MEND_ITEMS = ButtonDefinitions.createToggleButtonDefinition(
			ButtonDefinitions.getBooleanStateData(
					GuiHelper.getButtonStateData(new UV(144, 32), TranslationHelper.INSTANCE.translUpgradeButton("mend_items"), Dimension.SQUARE_16, new Position(1, 1)),
					GuiHelper.getButtonStateData(new UV(160, 32), TranslationHelper.INSTANCE.translUpgradeButton("do_not_mend_items"), Dimension.SQUARE_16, new Position(1, 1))
			));
	private static final ButtonDefinition STORE_ALL = button(new UV(192, 16), new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("store_all_experience")));
	private static final ButtonDefinition TAKE_ALL = button(new UV(144, 16), new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("take_all_experience")));
	private static final ButtonDefinition TAKE = button(new UV(160, 16));
	private static final ButtonDefinition STORE = button(new UV(176, 16));

	static {
		Map<AutomationDirection, ToggleButton.StateData> states = new EnumMap<>(AutomationDirection.class);
		states.put(AutomationDirection.INPUT, GuiHelper.getButtonStateData(new UV(144, 0), TranslationHelper.INSTANCE.translUpgradeButton("xp_pump_input"), Dimension.SQUARE_16, new Position(1, 1)));
		states.put(AutomationDirection.OUTPUT, GuiHelper.getButtonStateData(new UV(128, 16), TranslationHelper.INSTANCE.translUpgradeButton("xp_pump_output"), Dimension.SQUARE_16, new Position(1, 1)));
		states.put(AutomationDirection.KEEP, GuiHelper.getButtonStateData(new UV(96, 96), TranslationHelper.INSTANCE.translUpgradeButton("xp_pump_keep"), Dimension.SQUARE_16, new Position(1, 1)));
		states.put(AutomationDirection.OFF, GuiHelper.getButtonStateData(new UV(240, 0), TranslationHelper.INSTANCE.translUpgradeButton("xp_pump_off"), Dimension.SQUARE_16, new Position(1, 1)));
		DIRECTION = ButtonDefinitions.createToggleButtonDefinition(states);
	}

	private static ButtonDefinition button(UV uv, ITextComponent... tooltip) {
		return new ButtonDefinition(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND,
				new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, uv, Dimension.SQUARE_16), tooltip);
	}

	public XpPumpUpgradeTab(XpPumpUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, boolean isMendingTurnedOn) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("xp_pump"),
				new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey("xp_pump") + TranslationHelper.TOOLTIP_SUFFIX));
		int currentYOffset = 24;
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + currentYOffset), DIRECTION, button -> {
			if (button == 0) {
				getContainer().setDirection(getContainer().getDirection().next());
			}
		}, () -> getContainer().getDirection()));
		addHideableChild(new LevelSelector(new Position(x + 21, y + currentYOffset), upgradeContainer::getLevel, delta ->
				upgradeContainer.setLevel(upgradeContainer.getLevel() + (delta > 0 ? 1 : -1))));
		currentYOffset += 20;

		if (isMendingTurnedOn) {
			addHideableChild(new ToggleButton<>(new Position(x + 3, y + currentYOffset), MEND_ITEMS, button -> {
				if (button == 0) {
					upgradeContainer.setMendItems(!upgradeContainer.shouldMendItems());
				}
			}, upgradeContainer::shouldMendItems));
			currentYOffset += 20;
		}

		addHideableChild(new Button(new Position(x + 3, y + currentYOffset), TAKE_ALL, button -> {
			if (button == 0) {
				upgradeContainer.takeAllExperience();
			}
		}));
		addHideableChild(new LevelsButton(new Position(x + 21, y + currentYOffset), TAKE, upgradeContainer::takeLevels, "take_levels", TextFormatting.GREEN,
				upgradeContainer::getLevelsToTake, delta -> upgradeContainer.setLevelsToTake(upgradeContainer.getLevelsToTake() + (delta > 0 ? 1 : -1))));
		addHideableChild(new LevelsButton(new Position(x + 39, y + currentYOffset), STORE, upgradeContainer::storeLevels, "store_levels", TextFormatting.RED,
				upgradeContainer::getLevelsToStore, delta -> upgradeContainer.setLevelsToStore(upgradeContainer.getLevelsToStore() + (delta > 0 ? 1 : -1))));
		addHideableChild(new Button(new Position(x + 57, y + currentYOffset), STORE_ALL, button -> {
			if (button == 0) {
				upgradeContainer.storeAllExperience();
			}
		}));
	}

	@Override
	protected void moveSlotsToTab() {
		//noop
	}

	/** Takes or stores a number of levels at a click; scrolling over it changes the number, which its tooltip says. */
	private static class LevelsButton extends Button {
		private final String tooltipKey;
		private final TextFormatting numberFormatting;
		private final IntSupplier getLevels;
		private final DoubleConsumer onScroll;

		private LevelsButton(Position position, ButtonDefinition definition, Runnable onClick, String tooltipKey, TextFormatting numberFormatting,
				IntSupplier getLevels, DoubleConsumer onScroll) {
			super(position, definition, button -> {
				if (button == 0) {
					onClick.run();
				}
			});
			this.tooltipKey = tooltipKey;
			this.numberFormatting = numberFormatting;
			this.getLevels = getLevels;
			this.onScroll = onScroll;
		}

		@Override
		protected List<ITextComponent> getTooltip() {
			ITextComponent levels = new TextComponentString(String.valueOf(getLevels.getAsInt()));
			levels.getStyle().setColor(numberFormatting);
			ITextComponent controls = new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton(tooltipKey + ".controls"));
			controls.getStyle().setItalic(true).setColor(TextFormatting.DARK_GRAY);
			return Arrays.asList(new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton(tooltipKey), levels), controls);
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
			onScroll.accept(delta);
			return true;
		}
	}

	/** The level the pump keeps the player at, changed by scrolling over it. */
	private static class LevelSelector extends WidgetBase {
		private final IntSupplier getLevel;
		private final DoubleConsumer onScroll;

		private LevelSelector(Position position, IntSupplier getLevel, DoubleConsumer onScroll) {
			super(position, new Dimension(54, 18));
			this.getLevel = getLevel;
			this.onScroll = onScroll;
		}

		@Override
		protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
			GuiHelper.renderControlBackground(x, y, 54, 18);
		}

		@Override
		protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
			String text = TextFormatting.GRAY + I18n.format(TranslationHelper.INSTANCE.translUpgradeControl("xp_level_select"), TextFormatting.WHITE + String.valueOf(getLevel.getAsInt()) + TextFormatting.GRAY);
			int xOffset = (getWidth() - font.getStringWidth(text)) / 2;
			int yOffset = (int) Math.ceil((getHeight() - font.FONT_HEIGHT) / 2d);
			font.drawString(text, x + xOffset, y + yOffset, 0);
		}

		@Override
		public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
			super.renderTooltip(screen, mouseX, mouseY);
			if (visible && isMouseOver(mouseX, mouseY)) {
				GuiHelper.renderTooltip(screen, Arrays.asList(I18n.format(TranslationHelper.INSTANCE.translUpgradeControl("xp_level_select.tooltip")),
						TextFormatting.DARK_GRAY.toString() + TextFormatting.ITALIC + I18n.format(TranslationHelper.INSTANCE.translUpgradeControl("xp_level_select.tooltip.controls"))), mouseX, mouseY);
			}
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
			onScroll.accept(delta);
			return true;
		}
	}
}
