package dev.feldsher.deconstructedcore.client.gui.controls;

import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class ToggleButton<T extends Comparable<T>> extends Button {
	private final Map<T, StateData> stateData;
	private final Supplier<T> getState;

	public ToggleButton(Position position, ButtonDefinition.Toggle<T> buttonDefinition, IntConsumer onClick, Supplier<T> getState) {
		super(position, buttonDefinition, onClick);
		stateData = buttonDefinition.getStateData();
		this.getState = getState;
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		StateData data = stateData.get(getState.get());
		if (data != null) {
			GuiHelper.blit(x, y, data.getTexture());
		}
	}

	@Override
	public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
		if (visible && isMouseOver(mouseX, mouseY)) {
			StateData data = stateData.get(getState.get());
			if (data != null) {
				GuiHelper.renderTooltip(screen, GuiHelper.toLines(getTooltip(data)), mouseX, mouseY);
			}
		}
	}

	protected List<ITextComponent> getTooltip(StateData data) {
		return data.getTooltip();
	}

	public static class StateData {
		private final TextureBlitData texture;
		private final List<ITextComponent> tooltip;

		public StateData(TextureBlitData texture, List<ITextComponent> tooltip) {
			this.texture = texture;
			this.tooltip = tooltip;
		}

		public StateData(TextureBlitData texture, ITextComponent... tooltip) {
			this(texture, Arrays.asList(tooltip));
		}

		public TextureBlitData getTexture() {
			return texture;
		}

		public List<ITextComponent> getTooltip() {
			return tooltip;
		}
	}
}
