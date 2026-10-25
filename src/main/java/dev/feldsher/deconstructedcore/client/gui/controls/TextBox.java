package dev.feldsher.deconstructedcore.client.gui.controls;

import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

/** A line of text to type in while it's focused; enter hands it on. */
public class TextBox extends WidgetBase {
	private static final int MAX_LENGTH = 50;
	private final GuiTextField textField;

	public TextBox(Position position, Dimension dimension) {
		super(position, dimension);
		textField = new GuiTextField(0, font, x, y, dimension.width(), dimension.height());
		textField.setMaxStringLength(MAX_LENGTH);
	}

	public String getValue() {
		return textField.getText();
	}

	public void setValue(String value) {
		textField.setText(value);
	}

	public boolean isFocused() {
		return visible && textField.isFocused();
	}

	public void setFocused(boolean focused) {
		textField.setFocused(focused);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		textField.setVisible(visible);
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		textField.drawTextBox();
	}

	/** Whether it took the key, which it does for all but escape while focused. */
	public boolean keyTyped(char typedChar, int keyCode) {
		if (!isFocused() || keyCode == Keyboard.KEY_ESCAPE) {
			return false;
		}
		if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
			onEnterPressed();
			return true;
		}
		textField.textboxKeyTyped(typedChar, keyCode);
		return true;
	}

	protected void onEnterPressed() {
		//noop by default
	}
}
