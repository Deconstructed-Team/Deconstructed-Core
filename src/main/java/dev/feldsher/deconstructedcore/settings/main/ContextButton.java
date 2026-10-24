package dev.feldsher.deconstructedcore.settings.main;

import dev.feldsher.deconstructedcore.client.gui.controls.ButtonBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.GUI_CONTROLS;

/** The button that switches the main settings tab between the player's settings and the storage's. */
public class ContextButton extends ButtonBase {
	private static final TextureBlitData LEFT_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(47, 0), new Dimension(16, 18));
	private static final TextureBlitData LEFT_BUTTON_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(29, 0), new Dimension(16, 18));
	private static final TextureBlitData MIDDLE_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(49, 0), new Dimension(14, 18));
	private static final TextureBlitData MIDDLE_BUTTON_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(31, 0), new Dimension(14, 18));
	private static final TextureBlitData RIGHT_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(49, 0), new Dimension(16, 18));
	private static final TextureBlitData RIGHT_BUTTON_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(31, 0), new Dimension(16, 18));

	private final Supplier<ITextComponent> getTitle;
	private final Supplier<List<ITextComponent>> getTooltip;

	protected ContextButton(Position position, IntConsumer onClick, Supplier<ITextComponent> getTitle, Supplier<List<ITextComponent>> getTooltip) {
		super(position, new Dimension(62, 18), onClick);
		this.getTitle = getTitle;
		this.getTooltip = getTooltip;
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		if (isMouseOver(mouseX, mouseY)) {
			renderBackground(LEFT_BUTTON_HOVERED_BACKGROUND, MIDDLE_BUTTON_HOVERED_BACKGROUND, RIGHT_BUTTON_HOVERED_BACKGROUND);
		} else {
			renderBackground(LEFT_BUTTON_BACKGROUND, MIDDLE_BUTTON_BACKGROUND, RIGHT_BUTTON_BACKGROUND);
		}
	}

	private void renderBackground(TextureBlitData left, TextureBlitData middle, TextureBlitData right) {
		int leftX = x;
		GuiHelper.blit(leftX, y, left);
		leftX += left.getWidth();
		GuiHelper.blit(leftX, y, middle);
		leftX += middle.getWidth();
		GuiHelper.blit(leftX, y, middle);
		leftX += middle.getWidth();
		GuiHelper.blit(leftX, y, right);
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		String title = getTitle.get().getFormattedText();
		font.drawStringWithShadow(title, x + getWidth() / 2f - font.getStringWidth(title) / 2f, y - 4 + getHeight() / 2f, 0xFFFFFF);
	}

	@Override
	public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
		super.renderTooltip(screen, mouseX, mouseY);
		if (isMouseOver(mouseX, mouseY)) {
			GuiHelper.renderTooltip(screen, GuiHelper.toLines(getTooltip.get()), mouseX, mouseY);
		}
	}
}
