package dev.feldsher.deconstructedcore.client.gui.controls;

import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

import java.util.function.IntConsumer;

public abstract class ButtonBase extends WidgetBase {
	protected IntConsumer onClick;

	protected ButtonBase(Position position, Dimension dimension, IntConsumer onClick) {
		super(position, dimension);
		this.onClick = onClick;
	}

	protected void setOnClick(IntConsumer onClick) {
		this.onClick = onClick;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isMouseOver(mouseX, mouseY)) {
			return false;
		}
		onClick.accept(button);
		Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		return true;
	}
}
