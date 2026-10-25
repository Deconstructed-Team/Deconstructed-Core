package dev.feldsher.deconstructedcore.client.gui.utils;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.opengl.GL11;

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
	public static final TextureBlitData BAR_BACKGROUND_TOP = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 30), Dimension.SQUARE_18);
	public static final TextureBlitData BAR_BACKGROUND_MIDDLE = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 48), Dimension.SQUARE_18);
	public static final TextureBlitData BAR_BACKGROUND_BOTTOM = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 66), Dimension.SQUARE_18);

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

	/** Draws the texture's part at another size, its four corners kept and its middle cut out or repeated at the edges. */
	public static void blit(int x, int y, TextureBlitData texData, int width, int height) {
		int halfWidth = width / 2;
		int secondHalfWidth = width - halfWidth;
		int halfHeight = height / 2;
		int secondHalfHeight = height - halfHeight;
		int left = x + texData.getXOffset();
		int top = y + texData.getYOffset();
		int secondU = texData.getU() + texData.getWidth() - secondHalfWidth;
		int secondV = texData.getV() + texData.getHeight() - secondHalfHeight;
		int textureWidth = texData.getTextureWidth();
		int textureHeight = texData.getTextureHeight();
		Minecraft.getMinecraft().getTextureManager().bindTexture(texData.getTextureName());
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.enableBlend();
		Gui.drawModalRectWithCustomSizedTexture(left, top, texData.getU(), texData.getV(), halfWidth, halfHeight, textureWidth, textureHeight);
		Gui.drawModalRectWithCustomSizedTexture(left + halfWidth, top, secondU, texData.getV(), secondHalfWidth, halfHeight, textureWidth, textureHeight);
		Gui.drawModalRectWithCustomSizedTexture(left, top + halfHeight, texData.getU(), secondV, halfWidth, secondHalfHeight, textureWidth, textureHeight);
		Gui.drawModalRectWithCustomSizedTexture(left + halfWidth, top + halfHeight, secondU, secondV, secondHalfWidth, secondHalfHeight, textureWidth, textureHeight);
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

	/** A line per component, split where the translation has a line break, each part keeping the formatting before it. */
	public static List<String> toLines(List<ITextComponent> components) {
		List<String> lines = new ArrayList<>();
		for (ITextComponent component : components) {
			String formatting = "";
			for (String part : component.getFormattedText().split("\\\\n|\n")) {
				lines.add(formatting + part);
				formatting = FontRenderer.getFormatFromString(formatting + part);
			}
		}
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

	/** A column 16 wide of the block atlas sprite, tinted, tiled up from its bottom: a fluid as high as it fills. */
	public static void renderTiledFluidTextureAtlas(TextureAtlasSprite sprite, int color, int x, int y, int height) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		float red = (color >> 16 & 255) / 255.0F;
		float green = (color >> 8 & 255) / 255.0F;
		float blue = (color & 255) / 255.0F;
		float alpha = (color >> 24 & 255) == 0 ? 1 : (color >> 24 & 255) / 255.0F;
		GlStateManager.enableBlend();
		GlStateManager.color(1, 1, 1, 1);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder builder = tessellator.getBuffer();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		float u0 = sprite.getMinU();
		float u1 = sprite.getMaxU();
		float v0 = sprite.getMinV();
		int bottom = y + height;
		while (height > 0) {
			int renderHeight = Math.min(16, height);
			height -= renderHeight;
			float v1 = sprite.getInterpolatedV(renderHeight);
			int top = bottom - renderHeight;
			builder.pos(x, bottom, 0).tex(u0, v1).color(red, green, blue, alpha).endVertex();
			builder.pos(x + 16D, bottom, 0).tex(u1, v1).color(red, green, blue, alpha).endVertex();
			builder.pos(x + 16D, top, 0).tex(u1, v0).color(red, green, blue, alpha).endVertex();
			builder.pos(x, top, 0).tex(u0, v0).color(red, green, blue, alpha).endVertex();
			bottom = top;
		}
		tessellator.draw();
	}
}
