package dev.feldsher.deconstructedcore.client.gui.controls;

import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import java.util.function.Supplier;

/** The part of a texture that the progress shows: from the left, or what's left of it from the bottom up. */
public class ProgressBar extends WidgetBase {
	private final TextureBlitData progressTexture;
	private final Supplier<Float> getProgress;
	private final ProgressDirection dir;

	public ProgressBar(Position position, TextureBlitData progressTexture, Supplier<Float> getProgress, ProgressDirection dir) {
		super(position, new Dimension(progressTexture.getWidth(), progressTexture.getHeight()));
		this.progressTexture = progressTexture;
		this.getProgress = getProgress;
		this.dir = dir;
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		int height = progressTexture.getHeight();
		int width = progressTexture.getWidth();
		float progress = getProgress.get();
		if (progress <= 0) {
			return;
		}
		int yOffset = 0;
		if (dir == ProgressDirection.BOTTOM_UP) {
			yOffset = (int) (height * progress);
			height -= yOffset;
		} else if (dir == ProgressDirection.LEFT_RIGHT) {
			width = (int) (width * progress);
		}
		minecraft.getTextureManager().bindTexture(progressTexture.getTextureName());
		GlStateManager.color(1, 1, 1, 1);
		Gui.drawModalRectWithCustomSizedTexture(x, y + yOffset, progressTexture.getU(), (float) progressTexture.getV() + yOffset, width, height,
				progressTexture.getTextureWidth(), progressTexture.getTextureHeight());
	}

	public enum ProgressDirection {
		LEFT_RIGHT,
		BOTTOM_UP
	}
}
