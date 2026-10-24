package dev.feldsher.deconstructedcore.client.gui.utils;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiHelper {
	public static final ResourceLocation GUI_CONTROLS = DeconstructedCore.getRL("textures/gui/gui_controls.png");
	public static final ResourceLocation SLOTS_BACKGROUND = DeconstructedCore.getRL("textures/gui/slots_background.png");

	private GuiHelper() {}

	/** Draws a part of a 256x256 texture. */
	public static void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		GlStateManager.color(1, 1, 1, 1);
		Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, 256, 256);
	}

	/** The slots' background, 12 rows at most at a time since that's how tall the texture is. */
	public static void renderSlotsBackground(int x, int y, int slotWidth, int slotHeight) {
		for (int currentY = y, remainingSlotHeight = slotHeight; remainingSlotHeight > 0; currentY += 12 * 18, remainingSlotHeight -= Math.min(slotHeight, 12)) {
			blit(SLOTS_BACKGROUND, x, currentY, 0, 0, slotWidth * 18, Math.min(remainingSlotHeight, 12) * 18);
		}
	}

	public static void renderSlotsBackground(int x, int y, int slotsInRow, int fullSlotRows, int extraRowSlots) {
		renderSlotsBackground(x, y, slotsInRow, fullSlotRows);
		if (extraRowSlots > 0) {
			renderSlotsBackground(x, y + fullSlotRows * 18, extraRowSlots, 1);
		}
	}
}
