package dev.feldsher.deconstructedcore.upgrades.tank;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.UpgradeSettingsTab;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;

/** The tank's input and output slots. */
public class TankUpgradeTab extends UpgradeSettingsTab<TankUpgradeContainer> {
	public TankUpgradeTab(TankUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("tank"),
				new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey("tank") + TranslationHelper.TOOLTIP_SUFFIX));
		openTabDimension = new Dimension(48, 48);
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		super.renderBg(minecraft, mouseX, mouseY);
		if (getContainer().isOpen()) {
			GuiHelper.renderSlotsBackground(x + 3, y + 24, 1, 1);
			GuiHelper.renderSlotsBackground(x + 24, y + 24, 1, 1);
		}
	}

	@Override
	protected void moveSlotsToTab() {
		List<Slot> slots = getContainer().getSlots();
		positionSlot(slots.get(TankUpgradeWrapper.INPUT_SLOT), 4);
		positionSlot(slots.get(TankUpgradeWrapper.OUTPUT_SLOT), 25);
	}

	private void positionSlot(Slot slot, int xOffset) {
		slot.xPos = x - screen.getGuiLeft() + xOffset;
		slot.yPos = y - screen.getGuiTop() + 25;
	}
}
