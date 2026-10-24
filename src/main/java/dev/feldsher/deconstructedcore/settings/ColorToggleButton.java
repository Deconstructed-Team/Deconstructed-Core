package dev.feldsher.deconstructedcore.settings;

import dev.feldsher.deconstructedcore.client.gui.controls.ButtonBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_BACKGROUND;
import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND;

/** Cycles a settings category's color: next on left click, previous on right click. */
public class ColorToggleButton extends ButtonBase {
	private static final EnumDyeColor[] DYE_VALUES = EnumDyeColor.values();

	private final Supplier<EnumDyeColor> getColor;
	private final Consumer<EnumDyeColor> setColor;

	public ColorToggleButton(Position position, Supplier<EnumDyeColor> getColor, Consumer<EnumDyeColor> setColor) {
		super(position, Dimension.SQUARE_18, b -> {});
		this.getColor = getColor;
		this.setColor = setColor;
		setOnClick(this::toggleColor);
	}

	private void toggleColor(int button) {
		int ordinal = getColor.get().ordinal();
		if (button == 0) {
			setColor.accept(DYE_VALUES[(ordinal + 1) % DYE_VALUES.length]);
		} else if (button == 1) {
			setColor.accept(DYE_VALUES[(ordinal - 1 + DYE_VALUES.length) % DYE_VALUES.length]);
		}
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		GuiHelper.blit(x, y, isMouseOver(mouseX, mouseY) ? DEFAULT_BUTTON_HOVERED_BACKGROUND : DEFAULT_BUTTON_BACKGROUND);
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		Gui.drawRect(x + 3, y + 3, x + 15, y + 15, getColor.get().getColorValue() | (200 << 24));
	}

	@Override
	public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
		super.renderTooltip(screen, mouseX, mouseY);
		if (isMouseOver(mouseX, mouseY)) {
			List<String> lines = new ArrayList<>();
			lines.add(TranslationHelper.INSTANCE.translate(TranslationHelper.INSTANCE.translSettingsButton("toggle_color")));
			lines.addAll(TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsButton("toggle_color_detail"), TextFormatting.GRAY));
			GuiHelper.renderTooltip(screen, lines, mouseX, mouseY);
		}
	}
}
