package dev.feldsher.deconstructedcore.client.gui.controls;

import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.util.function.Consumer;

/** A line of text to type in while it's focused; enter hands it on. */
public class TextBox extends WidgetBase {
	private static final int MAX_LENGTH = 50;
	private final GuiTextField textField;
	private boolean editable = true;

	public TextBox(Position position, Dimension dimension) {
		super(position, dimension);
		textField = new GuiTextField(0, font, x, y, dimension.width(), dimension.height());
		textField.setMaxStringLength(MAX_LENGTH);
	}

	public String getValue() {
		return textField.getText();
	}

	/** Sets the text without telling the responder. */
	public void setValue(String value) {
		textField.setText(value);
	}

	public boolean isFocused() {
		return visible && textField.isFocused();
	}

	public void setFocused(boolean focused) {
		textField.setFocused(focused);
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
		textField.setEnabled(editable);
		if (!editable) {
			setFocused(false);
		}
	}

	public void setTextColor(int color) {
		textField.setTextColor(color);
	}

	public void setTextColorUneditable(int color) {
		textField.setDisabledTextColour(color);
	}

	public void setBordered(boolean bordered) {
		textField.setEnableBackgroundDrawing(bordered);
	}

	public void setMaxLength(int maxLength) {
		textField.setMaxStringLength(maxLength);
	}

	/** Told the text each time the player types or deletes in it. */
	public void setResponder(Consumer<String> responder) {
		textField.setGuiResponder(new GuiPageButtonList.GuiResponder() {
			@Override
			public void setEntryValue(int id, boolean value) {
				//noop
			}

			@Override
			public void setEntryValue(int id, float value) {
				//noop
			}

			@Override
			public void setEntryValue(int id, String value) {
				responder.accept(value);
			}
		});
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

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return editable && textField.mouseClicked((int) mouseX, (int) mouseY, button);
	}

	@Override
	public void onScreenClicked(double mouseX, double mouseY, int button) {
		if (!isMouseOver(mouseX, mouseY)) {
			setFocused(false);
		}
	}

	/** Whether it took the key, which it does for all but escape while focused. */
	@Override
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
