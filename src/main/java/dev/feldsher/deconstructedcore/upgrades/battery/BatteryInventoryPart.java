package dev.feldsher.deconstructedcore.upgrades.battery;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.UpgradeInventoryPartBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import net.minecraft.client.resources.I18n;

import java.util.Collections;

/** The battery, as tall as the visible rows, in the two columns it takes: its charge in segments, yellow to red. */
public class BatteryInventoryPart extends UpgradeInventoryPartBase<BatteryUpgradeContainer> {
	private static final TextureBlitData OVERLAY = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(47, 56), new Dimension(16, 18));
	private static final TextureBlitData CHARGE_SEGMENT = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(47, 74), new Dimension(16, 6));
	private static final TextureBlitData CONNECTION_TOP = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(47, 48), new Dimension(16, 4));
	private static final TextureBlitData CONNECTION_BOTTOM = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(47, 52), new Dimension(16, 4));
	private static final int TOP_BAR_COLOR = 0xff1a1a;
	private static final int BOTTOM_BAR_COLOR = 0xffff40;
	private final Position pos;
	private final int height;
	private final StorageScreenBase<?> screen;

	public BatteryInventoryPart(int upgradeSlot, BatteryUpgradeContainer container, Position pos, int height, StorageScreenBase<?> screen) {
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

		yOffset = 0;
		for (int i = 0; i < height / 18; i++) {
			GuiHelper.blit(left + 1, top + yOffset, OVERLAY);
			yOffset += 18;
		}

		renderCharge();

		GuiHelper.blit(left + 1, top, CONNECTION_TOP);
		GuiHelper.blit(left + 1, top + height - 4, CONNECTION_BOTTOM);
	}

	@Override
	public boolean handleMouseReleased(int mouseX, int mouseY, int button) {
		return false;
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
			GuiHelper.renderTooltip(screen, Collections.singletonList(I18n.format(TranslationHelper.INSTANCE.translUpgradeKey("battery.contents_tooltip"),
					String.format("%,d", container.getEnergyStored()), String.format("%,d", container.getMaxEnergyStored()))), mouseX, mouseY);
		}
	}

	private void renderCharge() {
		int maxEnergyStored = container.getMaxEnergyStored();
		if (maxEnergyStored <= 0) {
			return;
		}
		int segmentHeight = CHARGE_SEGMENT.getHeight();
		int numberOfSegments = height / segmentHeight;
		int displayLevel = (int) (numberOfSegments * Math.min(1F, (float) container.getEnergyStored() / maxEnergyStored));

		for (int i = 0; i < displayLevel; i++) {
			float percentage = numberOfSegments > 1 ? (float) i / (numberOfSegments - 1) : 0;
			int red = (int) ((BOTTOM_BAR_COLOR >> 16 & 255) * (1 - percentage) + (TOP_BAR_COLOR >> 16 & 255) * percentage);
			int green = (int) ((BOTTOM_BAR_COLOR >> 8 & 255) * (1 - percentage) + (TOP_BAR_COLOR >> 8 & 255) * percentage);
			int blue = (int) ((BOTTOM_BAR_COLOR & 255) * (1 - percentage) + (TOP_BAR_COLOR & 255) * percentage);
			GuiHelper.coloredBlit(getTankLeft() + 1, getTankTop() + height - (i + 1) * segmentHeight, CHARGE_SEGMENT, red << 16 | green << 8 | blue);
		}
	}
}
