package dev.feldsher.deconstructedcore.client.gui;

import dev.feldsher.deconstructedcore.client.gui.controls.ButtonBase;
import dev.feldsher.deconstructedcore.client.gui.controls.Label;
import dev.feldsher.deconstructedcore.client.gui.controls.WidgetBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;

/** A tab that opens into its controls, which are only its children while it's open. */
public abstract class SettingsTabBase<T extends GuiContainer> extends Tab {
	private static final int RIGHT_BORDER_WIDTH = 6;
	private static final int BOTTOM_BORDER_HEIGHT = 6;

	protected final T screen;
	protected Dimension openTabDimension = new Dimension(0, 0);
	protected boolean isOpen = false;
	private Runnable onOpen = () -> {
	};
	private Runnable onClose = () -> {
	};
	private final List<WidgetBase> hideableChildren = new ArrayList<>();

	protected SettingsTabBase(Position position, T screen, ITextComponent tabLabel, ITextComponent tooltip, Function<IntConsumer, ButtonBase> getTabButton) {
		super(position, tooltip, getTabButton);
		this.screen = screen;
		addHideableChild(new Label(new Position(x + 20, y + 8), tabLabel));
	}

	protected <U extends WidgetBase> U addHideableChild(U widget) {
		hideableChildren.add(widget);
		updateOpenTabDimension(widget);
		return widget;
	}

	private <U extends WidgetBase> void updateOpenTabDimension(U widget) {
		int widgetMaxWidthExtension = widget.getX() + widget.getWidth() + RIGHT_BORDER_WIDTH - x;
		int widgetMaxHeightExtension = widget.getY() + widget.getHeight() + BOTTOM_BORDER_HEIGHT - y;
		openTabDimension = new Dimension(Math.max(openTabDimension.width(), widgetMaxWidthExtension), Math.max(openTabDimension.height(), widgetMaxHeightExtension));
	}

	public void setHandlers(Runnable onOpen, Runnable onClose, BooleanSupplier shouldRender, BooleanSupplier shouldShowTooltip) {
		this.onOpen = onOpen;
		this.onClose = onClose;
		setHandlers(shouldShowTooltip, shouldRender);
	}

	@Override
	protected boolean isClosedTooltipVisible(int mouseX, int mouseY) {
		return !isOpen && super.isClosedTooltipVisible(mouseX, mouseY);
	}

	public boolean isOpen() {
		return isOpen;
	}

	public void close() {
		setOpen(false);
	}

	protected void onTabOpen() {
		setWidth(openTabDimension.width());
		setHeight(openTabDimension.height());

		hideableChildren.forEach(this::addChild);
		onOpen.run();
	}

	protected void onTabClose() {
		setWidth(DEFAULT_WIDTH);
		setHeight(DEFAULT_HEIGHT);

		children.removeAll(hideableChildren);
		onClose.run();
	}

	protected void setOpen(boolean isOpen) {
		this.isOpen = isOpen;
		if (isOpen) {
			onTabOpen();
		} else {
			onTabClose();
		}
	}

	@Override
	protected void onTabIconClicked(int button) {
		if (button != 0) {
			return;
		}
		setOpen(!isOpen);
	}
}
