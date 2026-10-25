package dev.feldsher.deconstructedcore.upgrades.tank;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.UpgradeInventoryPartBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import dev.feldsher.deconstructedcore.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import java.util.ArrayList;
import java.util.List;

/** The tank, as tall as the visible rows, in the two columns it takes; clicked with a fluid container, it fills or empties it. */
public class TankInventoryPart extends UpgradeInventoryPartBase<TankUpgradeContainer> {
	private static final TextureBlitData OVERLAY = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(47, 30), new Dimension(16, 18));
	private final Position pos;
	private final int height;
	private final StorageScreenBase<?> screen;

	public TankInventoryPart(int upgradeSlot, TankUpgradeContainer container, Position pos, int height, StorageScreenBase<?> screen) {
		super(upgradeSlot, container);
		this.pos = pos;
		this.height = height;
		this.screen = screen;
	}

	private int getTankLeft() {
		return screen.getGuiLeft() + pos.x() + 9;
	}

	private int getTankTop() {
		return screen.getGuiTop() + pos.y();
	}

	@Override
	public void render(int mouseX, int mouseY) {
		int left = getTankLeft();
		int top = getTankTop();
		GuiHelper.blit(left, top, GuiHelper.BAR_BACKGROUND_TOP);
		int yOffset = 18;
		for (int i = 0; i < (height - 36) / 18; i++) {
			GuiHelper.blit(left, top + yOffset, GuiHelper.BAR_BACKGROUND_MIDDLE);
			yOffset += 18;
		}
		GuiHelper.blit(left, top + yOffset, GuiHelper.BAR_BACKGROUND_BOTTOM);

		renderFluid();

		yOffset = 0;
		for (int i = 0; i < height / 18; i++) {
			GuiHelper.blit(left + 1, top + yOffset, OVERLAY);
			yOffset += 18;
		}
	}

	@Override
	public boolean handleMouseReleased(int mouseX, int mouseY, int button) {
		if (mouseX < getTankLeft() || mouseX >= getTankLeft() + 18 || mouseY < getTankTop() || mouseY >= getTankTop() + height) {
			return false;
		}

		ItemStack cursorStack = Minecraft.getMinecraft().player.inventory.getItemStack();
		if (cursorStack.getCount() > 1 || FluidUtil.getFluidHandler(cursorStack) == null) {
			return false;
		}

		PacketHandler.INSTANCE.sendToServer(new TankClickMessage(upgradeSlot));
		return true;
	}

	@Override
	public void renderErrorOverlay() {
		screen.renderOverlay(StorageScreenBase.ERROR_SLOT_COLOR, getTankLeft() + 1, getTankTop() + 1, 16, height - 2);
	}

	@Override
	public void renderTooltip(StorageScreenBase<?> screen, int mouseX, int mouseY) {
		int left = getTankLeft() + 1;
		int top = getTankTop() + 1;
		if (mouseX >= left && mouseX < left + 16 && mouseY >= top && mouseY < top + height - 2) {
			FluidStack contents = container.getContents();
			List<String> tooltip = new ArrayList<>();
			if (contents != null) {
				tooltip.add(contents.getLocalizedName());
			}
			tooltip.add(I18n.format(TranslationHelper.INSTANCE.translUpgradeKey("tank.contents_tooltip"),
					String.format("%,d", contents == null ? 0 : contents.amount), String.format("%,d", container.getTankCapacity())));
			GuiHelper.renderTooltip(screen, tooltip, mouseX, mouseY);
		}
	}

	private void renderFluid() {
		FluidStack contents = container.getContents();
		int capacity = container.getTankCapacity();
		if (contents == null || capacity <= 0) {
			return;
		}

		int displayLevel = (int) ((height - 2) * Math.min(1F, (float) contents.amount / capacity));
		TextureAtlasSprite still = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(contents.getFluid().getStill(contents).toString());
		GuiHelper.renderTiledFluidTextureAtlas(still, contents.getFluid().getColor(contents), getTankLeft() + 1, getTankTop() + 1 + height - 2 - displayLevel, displayLevel);
	}
}
