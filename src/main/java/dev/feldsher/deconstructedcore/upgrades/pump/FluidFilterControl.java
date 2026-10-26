package dev.feldsher.deconstructedcore.upgrades.pump;

import dev.feldsher.deconstructedcore.client.gui.controls.WidgetBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.Rect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.fluids.FluidStack;

import java.util.Collections;
import java.util.Optional;

/** A row of the fluid filter's slots, each showing its fluid. */
public class FluidFilterControl extends WidgetBase {
	private final FluidFilterContainer container;

	protected FluidFilterControl(Position position, FluidFilterContainer container) {
		super(position, new Dimension(container.getNumberOfFluidFilters() * 18, 18));
		this.container = container;
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		GuiHelper.renderSlotsBackground(x, y, container.getNumberOfFluidFilters(), 1);
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		for (int i = 0; i < container.getNumberOfFluidFilters(); i++) {
			FluidStack fluid = container.getFluid(i);
			if (fluid != null) {
				TextureAtlasSprite still = minecraft.getTextureMapBlocks().getAtlasSprite(fluid.getFluid().getStill(fluid).toString());
				GuiHelper.renderTiledFluidTextureAtlas(still, fluid.getFluid().getColor(fluid), x + i * 18 + 1, y + 1, 16);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!visible || !isMouseOver(mouseX, mouseY)) {
			return false;
		}
		getSlotClicked(mouseX, mouseY).ifPresent(container::slotClick);
		return true;
	}

	@Override
	public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
		if (!visible) {
			return;
		}
		getSlotClicked(mouseX, mouseY).ifPresent(slot -> {
			FluidStack fluid = container.getFluid(slot);
			if (fluid != null) {
				GuiHelper.renderTooltip(screen, Collections.singletonList(fluid.getLocalizedName()), mouseX, mouseY);
			}
		});
	}

	public int getNumberOfSlots() {
		return container.getNumberOfFluidFilters();
	}

	/** The slot's area on screen, for a fluid dragged onto it. */
	public Rect2i getSlotArea(int slot) {
		return new Rect2i(x + slot * 18, y, 17, 17);
	}

	public void setFluid(int slot, FluidStack fluid) {
		container.setFluid(slot, fluid);
	}

	private Optional<Integer> getSlotClicked(double mouseX, double mouseY) {
		if (mouseY < y + 1 || mouseY >= y + 17 || mouseX < x) {
			return Optional.empty();
		}
		int index = (int) ((mouseX - x) / 18);
		if (index >= container.getNumberOfFluidFilters()) {
			return Optional.empty();
		}
		return Optional.of(index);
	}
}
