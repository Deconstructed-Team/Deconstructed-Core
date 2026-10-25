package dev.feldsher.deconstructedcore.client.gui.controls;

import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

/**
 * A widget of a storage's screen: drawn and clicked by the screen itself, as 1.12's GuiContainer has no widgets of
 * this kind. Coordinates are the screen's.
 */
public abstract class WidgetBase {
	protected int x;
	protected int y;
	protected final Minecraft minecraft;
	protected final FontRenderer font;
	private int height;
	private int width;
	protected boolean isHovered;
	protected boolean visible = true;

	protected WidgetBase(Position position, Dimension dimension) {
		x = position.x();
		y = position.y();
		width = dimension.width();
		height = dimension.height();
		minecraft = Minecraft.getMinecraft();
		font = minecraft.fontRenderer;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public void render(int mouseX, int mouseY, float partialTicks) {
		if (!visible) {
			return;
		}

		isHovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
		renderBg(minecraft, mouseX, mouseY);
		renderWidget(mouseX, mouseY, partialTicks);
	}

	protected abstract void renderBg(Minecraft minecraft, int mouseX, int mouseY);

	protected abstract void renderWidget(int mouseX, int mouseY, float partialTicks);

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	protected void updateDimensions(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + getWidth() && mouseY >= y && mouseY < y + getHeight();
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	/** The mouse moved with a button held, wherever it is now; true when this widget took it. */
	public boolean mouseDragged(double mouseX, double mouseY, int button) {
		return false;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		return false;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setPosition(Position position) {
		x = position.x();
		y = position.y();
	}

	protected int getCenteredX(int elementWidth) {
		return (getWidth() - elementWidth) / 2;
	}

	public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
		//noop
	}
}
