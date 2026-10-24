package dev.feldsher.deconstructedcore.client.gui.controls;

import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;

public class Label extends WidgetBase {
	private static final int DEFAULT_GUI_TEXT_COLOR = 4210752;
	private final ITextComponent labelText;
	private final int color;

	public Label(Position position, ITextComponent labelText) {
		this(position, labelText, DEFAULT_GUI_TEXT_COLOR);
	}

	public Label(Position position, ITextComponent labelText, int color) {
		super(position, new Dimension(Minecraft.getMinecraft().fontRenderer.getStringWidth(labelText.getFormattedText()), 8));
		this.labelText = labelText;
		this.color = color;
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		font.drawString(labelText.getFormattedText(), x, y, color);
	}
}
