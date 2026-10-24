package dev.feldsher.deconstructedcore.client.gui.controls;

import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import java.util.function.IntConsumer;

public class ItemButton extends ButtonBase {
	private final ItemStack stack;

	public ItemButton(Position position, IntConsumer onClick, ItemStack stack) {
		super(position, Dimension.SQUARE_16, onClick);
		this.stack = stack;
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		GuiHelper.renderItemInGUI(stack, x, y);
	}
}
