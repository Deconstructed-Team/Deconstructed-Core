package dev.feldsher.deconstructedcore.client.gui.controls;

import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * Picks a color: by saturation and brightness on a square of its hue, by hue on a rainbow beside it, by its hex code,
 * or of the sixteen dye colors. Confirmed, it gives the color; cancelled, the one it started with; or no color, -1.
 */
public class ColorPicker extends CompositeWidgetBase<WidgetBase> {
	public static final int COLOR_GRADIENT_WIDTH = 50;
	public static final int RAINBOW_SLIDER_WIDTH = 10;
	public static final int COLOR_ENTRY_WIDTH = COLOR_GRADIENT_WIDTH + 2 + RAINBOW_SLIDER_WIDTH;
	public static final int COLOR_GRADIENT_HEIGHT = 50;
	public static final Dimension DIMENSIONS = new Dimension(114, 84);

	private final TextBox textColorEntry;
	private final ColorPreview colorPreview;
	private final ColorGradientArea colorGradientArea;
	private final RainbowSlider rainbowSlider;
	private final Button confirmButton;
	private final Button cancelButton;
	private final Button transparentColorButton;
	private final List<ColorButton> defaultColorButtons = new ArrayList<>();

	public ColorPicker(Position position, int color, IntConsumer colorSetter) {
		super(position, DIMENSIONS);
		int startColor = color == -1 ? 0xFFFFFF : color & 0xFFFFFF;
		colorPreview = addChild(new ColorPreview(new Position(0, 0), new Dimension(50, 12), startColor));
		textColorEntry = addChild(new TextBox(new Position(0, 0), new Dimension(COLOR_ENTRY_WIDTH, 12)));
		colorGradientArea = new ColorGradientArea(new Position(0, 0), new Dimension(COLOR_GRADIENT_WIDTH, COLOR_GRADIENT_HEIGHT), gradientColor -> {
			textColorEntry.setValue(getHexColor(gradientColor));
			colorPreview.setColor(gradientColor);
		});
		colorGradientArea.setColor(startColor);
		rainbowSlider = new RainbowSlider(new Position(0, 0), new Dimension(RAINBOW_SLIDER_WIDTH, 50), colorGradientArea::setHue);
		rainbowSlider.setColor(startColor);
		addChild(rainbowSlider);
		addChild(colorGradientArea);

		textColorEntry.setMaxLength(7);
		textColorEntry.setValue(getHexColor(startColor));
		textColorEntry.setResponder(s -> {
			if (s.length() != 7 || s.charAt(0) != '#') {
				return;
			}
			try {
				int c = Integer.parseInt(s.substring(1), 16) & 0xFFFFFF;
				colorPreview.setColor(c);
				colorGradientArea.setColor(c);
				rainbowSlider.setColor(c);
			} catch (NumberFormatException e) {
				//noop: not a color yet
			}
		});

		confirmButton = addChild(new Button(new Position(0, 0), ButtonDefinitions.CONFIRM, button -> colorSetter.accept(colorPreview.getColor())));
		transparentColorButton = addChild(new Button(new Position(0, 0), ButtonDefinitions.TRANSPARENT, button -> colorSetter.accept(-1)));
		// the old color back
		cancelButton = addChild(new Button(new Position(0, 0), ButtonDefinitions.CANCEL, button -> colorSetter.accept(color)));
		addDefaultColorButtons();
		setPosition(position);
	}

	public static String getHexColor(int color) {
		return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
	}

	private void addDefaultColorButtons() {
		for (EnumDyeColor value : EnumDyeColor.values()) {
			int dyeColor = value.getColorValue();
			ColorButton colorButton = new ColorButton(new Position(0, 0), new Dimension(11, 11), () -> dyeColor, button -> {
				colorGradientArea.setColor(dyeColor);
				rainbowSlider.setColor(dyeColor);
				colorPreview.setColor(dyeColor);
				textColorEntry.setValue(getHexColor(dyeColor));
			}, null);
			defaultColorButtons.add(colorButton);
			addChild(colorButton);
		}
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		GuiHelper.renderControlBackground(x - 5, y - 5, getWidth() + 5 + 5, getHeight() + 5 + 5, 128, 0, 128, 256);
	}

	@Override
	public void setPosition(Position position) {
		super.setPosition(position);
		textColorEntry.setPosition(new Position(x, y + colorGradientArea.getHeight() + 2));
		colorPreview.setPosition(new Position(x + textColorEntry.getWidth() + 2, y + colorGradientArea.getHeight() + 2));
		colorGradientArea.setPosition(new Position(x, y));
		rainbowSlider.setPosition(new Position(x + colorGradientArea.getWidth() + 2, y));
		confirmButton.setPosition(new Position(x + getWidth() / 2 - 32, y + colorGradientArea.getHeight() + 2 + textColorEntry.getHeight() + 2));
		transparentColorButton.setPosition(new Position(x + getWidth() / 2 - 8, confirmButton.getY()));
		cancelButton.setPosition(new Position(x + getWidth() / 2 + 16, confirmButton.getY()));

		int row = 0;
		int column = 0;
		for (ColorButton defaultColorButton : defaultColorButtons) {
			defaultColorButton.setPosition(new Position(rainbowSlider.getX() + rainbowSlider.getWidth() + 2 + column * 13, y + row * 13));
			column++;
			if (column > 3) {
				column = 0;
				row++;
			}
		}
	}

	private static float[] toHsb(int color) {
		return Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
	}

	private static void fill(double x1, double y1, double x2, double y2, int color) {
		// drawRect takes whole pixels; the markers are thinner than one, drawn scaled up
		net.minecraft.client.renderer.GlStateManager.pushMatrix();
		net.minecraft.client.renderer.GlStateManager.scale(0.1, 0.1, 1);
		Gui.drawRect((int) Math.round(x1 * 10), (int) Math.round(y1 * 10), (int) Math.round(x2 * 10), (int) Math.round(y2 * 10), color);
		net.minecraft.client.renderer.GlStateManager.popMatrix();
	}

	private static class ColorGradientArea extends WidgetBase {
		private int color;
		private float hue;
		private final IntConsumer colorSetter;

		protected ColorGradientArea(Position position, Dimension dimension, IntConsumer colorSetter) {
			super(position, dimension);
			this.colorSetter = colorSetter;
		}

		public void setColor(int color) {
			this.color = color;
			hue = toHsb(color)[0];
		}

		public void setHue(double hue) {
			this.hue = (float) hue;
			float[] hsb = toHsb(color);
			color = MathHelper.hsvToRGB(this.hue, hsb[1], hsb[2]) & 0xFFFFFF;
			colorSetter.accept(color);
		}

		@Override
		protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
			int topRightCornerColor = MathHelper.hsvToRGB(hue, 1, 1);
			int red = topRightCornerColor >> 16 & 255;
			int green = topRightCornerColor >> 8 & 255;
			int blue = topRightCornerColor & 255;
			for (int i = 0; i < getWidth(); i++) {
				for (int j = 0; j < getHeight(); j++) {
					float horizontalFactor = (float) i / getWidth();
					float verticalFactor = (float) j / getHeight();
					int r = (int) ((1 - verticalFactor) * ((1 - horizontalFactor) * 255 + red * horizontalFactor));
					int g = (int) ((1 - verticalFactor) * ((1 - horizontalFactor) * 255 + green * horizontalFactor));
					int b = (int) ((1 - verticalFactor) * ((1 - horizontalFactor) * 255 + blue * horizontalFactor));
					Gui.drawRect(x + i, y + j, x + i + 1, y + j + 1, 0xFF_000000 | r << 16 | g << 8 | b);
				}
			}
		}

		@Override
		protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
			float[] hsb = toHsb(color);
			int markerX = (int) (hsb[1] * getWidth());
			int markerY = (int) ((1 - hsb[2]) * getHeight());
			fill(x, y + Math.max(markerY - 0.2f, 0), x + getWidth(), y + Math.min(markerY + 1.2f, getHeight()), 0xFF_FFFFFF);
			fill(x + Math.max(markerX - 0.2f, 0), y, x + Math.min(markerX + 1.2f, getWidth()), y + getHeight(), 0xFF_FFFFFF);
			Gui.drawRect(x, y + markerY, x + getWidth(), y + markerY + 1, 0xFF_000000);
			Gui.drawRect(x + markerX, y, x + markerX + 1, y + getHeight(), 0xFF_000000);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			setColorBasedOnMouseCoords(mouseX, mouseY);
			return true;
		}

		private void setColorBasedOnMouseCoords(double mouseX, double mouseY) {
			float saturation = MathHelper.clamp((float) (mouseX - x) / getWidth(), 0, 1);
			float value = MathHelper.clamp(1 - (float) (mouseY - y) / getHeight(), 0, 1);
			color = MathHelper.hsvToRGB(hue, saturation, value) & 0xFFFFFF;
			colorSetter.accept(color);
		}

		@Override
		public boolean mouseDragged(double mouseX, double mouseY, int button) {
			if (!isMouseOver(mouseX, mouseY)) {
				return false;
			}
			setColorBasedOnMouseCoords(mouseX, mouseY);
			return true;
		}
	}

	private static class RainbowSlider extends WidgetBase {
		private final DoubleConsumer hueSetter;
		private float hue;

		protected RainbowSlider(Position position, Dimension dimension, DoubleConsumer hueSetter) {
			super(position, dimension);
			this.hueSetter = hueSetter;
		}

		public void setColor(int color) {
			hue = toHsb(color)[0];
		}

		@Override
		protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
			for (int i = 0; i < getHeight(); i++) {
				float renderedHue = (float) i / getHeight();
				Gui.drawRect(x, y + getHeight() - i - 1, x + getWidth(), y + getHeight() - i, 0xFF_000000 | MathHelper.hsvToRGB(renderedHue, 1, 1));
			}
		}

		@Override
		protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
			int hueMarker = (int) (hue * getHeight());
			fill(x, y + getHeight() - hueMarker - 1.2f, x + getWidth(), y + getHeight() - hueMarker - 1, 0xFF_FFFFFF);
			Gui.drawRect(x, y + getHeight() - hueMarker - 1, x + getWidth(), y + getHeight() - hueMarker, 0xFF_000000);
			fill(x, y + getHeight() - hueMarker, x + getWidth(), y + getHeight() - hueMarker + 0.2f, 0xFF_FFFFFF);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			setHueBasedOnMouseY(mouseY);
			return true;
		}

		private void setHueBasedOnMouseY(double mouseY) {
			hue = MathHelper.clamp(1 - (float) (mouseY - y) / getHeight(), 0, 1);
			hueSetter.accept(hue);
		}

		@Override
		public boolean mouseDragged(double mouseX, double mouseY, int button) {
			if (!isMouseOver(mouseX, mouseY)) {
				return false;
			}
			setHueBasedOnMouseY(mouseY);
			return true;
		}
	}

	private static class ColorPreview extends WidgetBase {
		private int color;

		protected ColorPreview(Position position, Dimension dimension, int color) {
			super(position, dimension);
			this.color = color;
		}

		public void setColor(int color) {
			this.color = color;
		}

		@Override
		protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
			//noop
		}

		@Override
		protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
			Gui.drawRect(x, y, x + getWidth(), y + getHeight(), 0xFF_000000 | color);
		}

		public int getColor() {
			return color;
		}
	}
}
