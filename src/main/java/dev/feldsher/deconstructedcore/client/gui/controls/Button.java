package dev.feldsher.deconstructedcore.client.gui.controls;

import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.IntConsumer;

public class Button extends ButtonBase {
	@Nullable
	private final TextureBlitData backgroundTexture;
	@Nullable
	private final TextureBlitData hoveredBackgroundTexture;
	@Nullable
	private final TextureBlitData foregroundTexture;
	private List<ITextComponent> tooltip;
	private boolean hovered = false;

	public Button(Position position, ButtonDefinition buttonDefinition, IntConsumer onClick) {
		super(position, buttonDefinition.getDimension(), onClick);
		backgroundTexture = buttonDefinition.getBackgroundTexture();
		foregroundTexture = buttonDefinition.getForegroundTexture();
		hoveredBackgroundTexture = buttonDefinition.getHoveredBackgroundTexture();
		tooltip = buttonDefinition.getTooltip();
	}

	public boolean isHovered() {
		return hovered;
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		if (isMouseOver(mouseX, mouseY)) {
			hovered = true;
			if (hoveredBackgroundTexture != null) {
				GuiHelper.blit(x, y, hoveredBackgroundTexture);
			}
		} else {
			hovered = false;
			if (backgroundTexture != null) {
				GuiHelper.blit(x, y, backgroundTexture);
			}
		}
	}

	protected List<ITextComponent> getTooltip() {
		return tooltip;
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		if (foregroundTexture != null) {
			GuiHelper.blit(x, y, foregroundTexture);
		}
	}

	public void setTooltip(List<ITextComponent> tooltip) {
		this.tooltip = tooltip;
	}

	@Override
	public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
		super.renderTooltip(screen, mouseX, mouseY);
		if (visible && isMouseOver(mouseX, mouseY)) {
			GuiHelper.renderTooltip(screen, GuiHelper.toLines(getTooltip()), mouseX, mouseY);
		}
	}
}
