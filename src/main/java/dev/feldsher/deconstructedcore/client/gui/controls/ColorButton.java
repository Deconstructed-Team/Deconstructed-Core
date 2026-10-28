package dev.feldsher.deconstructedcore.client.gui.controls;

import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** A square of a color, framed lighter while hovered; no color, -1, shows as a checkerboard. */
public class ColorButton extends ButtonBase {
	private final IntSupplier colorGetter;
	@Nullable
	private final ITextComponent tooltip;

	public ColorButton(Position position, Dimension dimension, IntSupplier colorGetter, IntConsumer onClick, @Nullable ITextComponent tooltip) {
		super(position, dimension, onClick);
		this.colorGetter = colorGetter;
		this.tooltip = tooltip;
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		Gui.drawRect(x, y, x + getWidth(), y + getHeight(), isMouseOver(mouseX, mouseY) ? 0xFF_FFFFFF : 0xFF_CCCCCC);
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		int color = colorGetter.getAsInt();
		if (color == -1) {
			for (int row = 0; row < getHeight() - 2; row++) {
				for (int column = 0; column < getWidth() - 2; column++) {
					Gui.drawRect(x + column + 1, y + row + 1, x + column + 2, y + row + 2, (row + column) % 2 == 0 ? 0xFF_CCCCCC : 0xFF_888888);
				}
			}
		} else {
			Gui.drawRect(x + 1, y + 1, x + getWidth() - 1, y + getHeight() - 1, 0xFF_000000 | color);
		}
	}

	@Override
	public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
		super.renderTooltip(screen, mouseX, mouseY);
		if (visible && tooltip != null && isMouseOver(mouseX, mouseY)) {
			screen.drawHoveringText(Collections.singletonList(tooltip.getFormattedText()), mouseX, mouseY);
		}
	}
}
