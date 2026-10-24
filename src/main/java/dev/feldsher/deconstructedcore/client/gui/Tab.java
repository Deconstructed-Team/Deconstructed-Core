package dev.feldsher.deconstructedcore.client.gui;

import dev.feldsher.deconstructedcore.client.gui.controls.ButtonBase;
import dev.feldsher.deconstructedcore.client.gui.controls.CompositeWidgetBase;
import dev.feldsher.deconstructedcore.client.gui.controls.WidgetBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.Rect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;

public abstract class Tab extends CompositeWidgetBase<WidgetBase> {
	private static final int TEXTURE_WIDTH = 256;
	private static final int TEXTURE_HEIGHT = 256;
	public static final int DEFAULT_HEIGHT = 24;
	protected static final int DEFAULT_WIDTH = 21;

	private int width = DEFAULT_WIDTH;
	private int height = DEFAULT_HEIGHT;
	private final List<ITextComponent> tooltip;

	private BooleanSupplier shouldShowTooltip = () -> true;
	protected BooleanSupplier shouldRender = () -> true;

	protected Tab(Position position, List<ITextComponent> tooltip, Function<IntConsumer, ButtonBase> getTabButton) {
		super(position, new Dimension(0, 0));
		this.tooltip = tooltip;
		addChild(getTabButton.apply(this::onTabIconClicked));
	}

	protected Tab(Position position, ITextComponent tooltip, Function<IntConsumer, ButtonBase> getTabButton) {
		this(position, Collections.singletonList(tooltip), getTabButton);
	}

	public void setHandlers(BooleanSupplier shouldShowTooltip, BooleanSupplier shouldRender) {
		this.shouldShowTooltip = shouldShowTooltip;
		this.shouldRender = shouldRender;
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		if (!shouldRender.getAsBoolean()) {
			return;
		}
		super.render(mouseX, mouseY, partialTicks);
	}

	@Override
	public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
		super.renderTooltip(screen, mouseX, mouseY);
		if (shouldRender.getAsBoolean() && isClosedTooltipVisible(mouseX, mouseY)) {
			GuiHelper.renderTooltip(screen, GuiHelper.toLines(tooltip), mouseX, mouseY);
		}
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public Rect2i getTabRectangle() {
		return new Rect2i(x, y, width, height);
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		int halfHeight = height / 2;
		int oddHeightAddition = height % 2;
		int secondHalfHeight = halfHeight + oddHeightAddition;
		GuiHelper.blit(GuiHelper.GUI_CONTROLS, x, y, TEXTURE_WIDTH - width, 0, width, halfHeight);
		GuiHelper.blit(GuiHelper.GUI_CONTROLS, x, y + halfHeight, TEXTURE_WIDTH - width, TEXTURE_HEIGHT - secondHalfHeight, width, secondHalfHeight);
		GuiHelper.blit(GuiHelper.GUI_CONTROLS, x - 3, y, TEXTURE_WIDTH / 2, TEXTURE_HEIGHT - height, 3, height);
	}

	protected boolean isClosedTooltipVisible(int mouseX, int mouseY) {
		return shouldShowTooltip.getAsBoolean() && isMouseOver(mouseX, mouseY);
	}

	public int getTopY() {
		return y;
	}

	public int getBottomY() {
		return y + getHeight();
	}

	protected abstract void onTabIconClicked(int button);

	public void tick() {
		//noop
	}
}
