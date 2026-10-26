package dev.feldsher.deconstructedcore.client.gui.controls;

import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.List;

public abstract class CompositeWidgetBase<T extends WidgetBase> extends WidgetBase {
	protected final List<T> children = new ArrayList<>();

	protected CompositeWidgetBase(Position position, Dimension dimension) {
		super(position, dimension);
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		new ArrayList<>(children).forEach(child -> child.render(mouseX, mouseY, partialTicks));
	}

	protected <U extends T> U addChild(U child) {
		children.add(child);
		return child;
	}

	public List<T> getChildren() {
		return children;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (T child : new ArrayList<>(children)) {
			if (child.visible && child.isMouseOver(mouseX, mouseY) && child.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button) {
		for (T child : new ArrayList<>(children)) {
			if (child.visible && child.mouseDragged(mouseX, mouseY, button)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		for (T child : new ArrayList<>(children)) {
			if (child.visible && child.isMouseOver(mouseX, mouseY) && child.mouseScrolled(mouseX, mouseY, delta)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isTyping() {
		for (WidgetBase child : children) {
			if (child.visible && child.isTyping()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onScreenClicked(double mouseX, double mouseY, int button) {
		new ArrayList<>(children).forEach(child -> child.onScreenClicked(mouseX, mouseY, button));
	}

	@Override
	public boolean keyTyped(char typedChar, int keyCode) {
		for (T child : new ArrayList<>(children)) {
			if (child.visible && child.keyTyped(typedChar, keyCode)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
		super.renderTooltip(screen, mouseX, mouseY);
		new ArrayList<>(children).forEach(c -> c.renderTooltip(screen, mouseX, mouseY));
	}
}
