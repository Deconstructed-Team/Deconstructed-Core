package dev.feldsher.deconstructedcore.client.gui.controls;

import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import net.minecraft.client.Minecraft;

import java.util.function.IntConsumer;

public class ImageButton extends ButtonBase {
	private final TextureBlitData texture;

	public ImageButton(Position position, Dimension dimension, TextureBlitData texture, IntConsumer onClick) {
		super(position, dimension, onClick);
		this.texture = texture;
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		GuiHelper.blit(x, y, texture);
	}
}
