package dev.feldsher.deconstructedcore.settings.memory;

import dev.feldsher.deconstructedcore.client.gui.SettingsScreen;
import dev.feldsher.deconstructedcore.client.gui.controls.Button;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinitions;
import dev.feldsher.deconstructedcore.client.gui.controls.ImageButton;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import dev.feldsher.deconstructedcore.settings.SettingsTab;
import dev.feldsher.deconstructedcore.settings.nosort.NoSortSettingsTab;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.Optional;

/** Left click makes a slot remember what it holds, right click makes it forget; empty slots show what they remember. */
public class MemorySettingsTab extends SettingsTab<MemorySettingsContainer> {
	private static final TextureBlitData ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(128, 32), Dimension.SQUARE_16);

	public MemorySettingsTab(MemorySettingsContainer container, Position position, SettingsScreen screen) {
		super(container, position, screen, new TextComponentTranslation(TranslationHelper.INSTANCE.translSettings(MemorySettingsCategory.NAME)),
				new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsTooltip(MemorySettingsCategory.NAME)),
				onTabIconClicked -> new ImageButton(new Position(position.x() + 1, position.y() + 4), Dimension.SQUARE_16, ICON, onTabIconClicked));
		addHideableChild(new Button(new Position(x + 3, y + 24), NoSortSettingsTab.SELECT_ALL_SLOTS, button -> container.selectAllSlots()));
		addHideableChild(new Button(new Position(x + 21, y + 24), NoSortSettingsTab.UNSELECT_ALL_SLOTS, button -> container.unselectAllSlots()));
		addHideableChild(new ToggleButton<>(new Position(x + 39, y + 24), ButtonDefinitions.MATCH_NBT,
				button -> container.setIgnoreNbt(!container.ignoresNbt()), () -> !container.ignoresNbt()));
	}

	public MemorySettingsContainer getMemorySettingsContainer() {
		return getSettingsContainer();
	}

	@Override
	public Optional<Integer> getSlotOverlayColor(int slotNumber, boolean templateLoadHovered) {
		return Optional.empty();
	}

	@Override
	public void handleSlotClick(Slot slot, int mouseButton) {
		if (mouseButton == 0) {
			getSettingsContainer().selectSlot(slot.slotNumber);
		} else if (mouseButton == 1) {
			getSettingsContainer().unselectSlot(slot.slotNumber);
		}
	}

	@Override
	public ItemStack getItemDisplayOverride(int slotNumber, boolean templateLoadHovered) {
		if (templateLoadHovered) {
			return getSettingsContainer().getSelectedTemplatesMemorizedStack(slotNumber);
		}
		return getSettingsContainer().getMemorizedStack(slotNumber);
	}

	@Override
	public void renderExtra(Slot slot, boolean templateLoadHovered) {
		if (templateLoadHovered ? !getSettingsContainer().getSelectedTemplatesMemorizedStack(slot.slotNumber).isEmpty()
				: getSettingsContainer().isSlotSelected(slot.slotNumber)) {
			drawMemorizedStackOverlay(slot.xPos, slot.yPos);
		}
	}

	/** The memorized slot's mark: a translucent cover over its item. */
	public static void drawMemorizedStackOverlay(int x, int y) {
		GlStateManager.enableBlend();
		GlStateManager.disableDepth();
		GuiHelper.blit(GuiHelper.GUI_CONTROLS, x, y, 77, 0, 16, 16);
		GlStateManager.enableDepth();
		GlStateManager.disableBlend();
	}
}
