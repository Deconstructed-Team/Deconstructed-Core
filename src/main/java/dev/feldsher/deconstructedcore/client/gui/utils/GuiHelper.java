package dev.feldsher.deconstructedcore.client.gui.utils;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiHelper {
	public static final ResourceLocation GUI_CONTROLS = DeconstructedCore.getRL("textures/gui/gui_controls.png");
	public static final ResourceLocation SLOTS_BACKGROUND = DeconstructedCore.getRL("textures/gui/slots_background.png");
	public static final ResourceLocation ICONS = DeconstructedCore.getRL("textures/gui/icons.png");
	public static final TextureBlitData DEFAULT_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(47, 0), Dimension.SQUARE_18);
	public static final TextureBlitData DEFAULT_BUTTON_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(29, 0), Dimension.SQUARE_18);
	public static final TextureBlitData SMALL_BUTTON_BACKGROUND = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 18), Dimension.SQUARE_12);
	public static final TextureBlitData SMALL_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(41, 18), Dimension.SQUARE_12);

	private GuiHelper() {}

	/** Draws a part of a 256x256 texture. */
	public static void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		GlStateManager.color(1, 1, 1, 1);
		Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, 256, 256);
	}

	public static void blit(int x, int y, TextureBlitData texData) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(texData.getTextureName());
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		Gui.drawModalRectWithCustomSizedTexture(x + texData.getXOffset(), y + texData.getYOffset(), texData.getU(), texData.getV(),
				texData.getWidth(), texData.getHeight(), texData.getTextureWidth(), texData.getTextureHeight());
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

	public static void renderControlBackground(int x, int y, int renderWidth, int renderHeight) {
		renderControlBackground(x, y, renderWidth, renderHeight, 29, 146, 66, 56);
	}

	public static void renderControlBackground(int x, int y, int renderWidth, int renderHeight, int u, int v, int textureBgWidth, int textureBgHeight) {
		int halfWidth = renderWidth / 2;
		int halfHeight = renderHeight / 2;
		blit(GUI_CONTROLS, x, y, u, v, halfWidth, halfHeight);
		blit(GUI_CONTROLS, x, y + halfHeight, u, v + textureBgHeight - halfHeight, halfWidth, halfHeight);
		blit(GUI_CONTROLS, x + halfWidth, y, u + textureBgWidth - halfWidth, v, halfWidth, halfHeight);
		blit(GUI_CONTROLS, x + halfWidth, y + halfHeight, u + textureBgWidth - halfWidth, v + textureBgHeight - halfHeight, halfWidth, halfHeight);
	}

	public static void renderItemInGUI(ItemStack stack, int x, int y) {
		RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();
		RenderHelper.enableGUIStandardItemLighting();
		GlStateManager.enableDepth();
		itemRender.renderItemAndEffectIntoGUI(stack, x, y);
		RenderHelper.disableStandardItemLighting();
	}

	public static void renderTooltip(GuiScreen screen, List<String> lines, int x, int y) {
		if (!lines.isEmpty()) {
			screen.drawHoveringText(lines, x, y);
			GlStateManager.disableLighting();
		}
	}

	public static List<String> toLines(List<ITextComponent> components) {
		List<String> lines = new ArrayList<>();
		components.forEach(component -> lines.add(component.getFormattedText()));
		return lines;
	}

	public static ToggleButton.StateData getButtonStateData(UV uv, Dimension dimension, Position offset, ITextComponent... tooltip) {
		return getButtonStateData(uv, dimension, offset, Arrays.asList(tooltip));
	}

	public static ToggleButton.StateData getButtonStateData(UV uv, String tooltip, Dimension dimension) {
		return getButtonStateData(uv, tooltip, dimension, new Position(0, 0));
	}

	public static ToggleButton.StateData getButtonStateData(UV uv, String tooltip, Dimension dimension, Position offset) {
		return new ToggleButton.StateData(new TextureBlitData(ICONS, offset, Dimension.SQUARE_256, uv, dimension), new TextComponentTranslation(tooltip));
	}

	public static ToggleButton.StateData getButtonStateData(UV uv, Dimension dimension, Position offset, List<ITextComponent> tooltip) {
		return new ToggleButton.StateData(new TextureBlitData(ICONS, offset, Dimension.SQUARE_256, uv, dimension), tooltip);
	}
}
