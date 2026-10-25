package dev.feldsher.deconstructedcore.settings.itemdisplay;

import dev.feldsher.deconstructedcore.client.gui.SettingsScreen;
import dev.feldsher.deconstructedcore.client.gui.controls.Button;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.ImageButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import dev.feldsher.deconstructedcore.settings.ColorToggleButton;
import dev.feldsher.deconstructedcore.settings.SettingsTab;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_BACKGROUND;
import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND;

/**
 * Left click shows a slot's item, right click stops showing it; the rotate button turns the marked slot's item. The
 * side the items are shown on comes with the storages that can change it.
 */
public class ItemDisplaySettingsTab extends SettingsTab<ItemDisplaySettingsContainer> {
	private static final TextureBlitData ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(112, 64), Dimension.SQUARE_16);
	private static final TextureBlitData SLOT_SELECTION = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(93, 0), Dimension.SQUARE_24);
	private static final TextureBlitData ROTATE_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(128, 64), Dimension.SQUARE_16);
	public static final ButtonDefinition ROTATE = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, ROTATE_FOREGROUND);
	private int currentSelectedSlot;

	public ItemDisplaySettingsTab(ItemDisplaySettingsContainer container, Position position, SettingsScreen screen) {
		super(container, position, screen, new TextComponentTranslation(TranslationHelper.INSTANCE.translSettings(ItemDisplaySettingsCategory.NAME)),
				new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsTooltip(ItemDisplaySettingsCategory.NAME)),
				onTabIconClicked -> new ImageButton(new Position(position.x() + 1, position.y() + 4), Dimension.SQUARE_16, ICON, onTabIconClicked));
		addHideableChild(new Button(new Position(x + 3, y + 24), ROTATE, button -> {
			if (button == 0) {
				container.rotateClockwise(currentSelectedSlot);
			} else if (button == 1) {
				container.rotateCounterClockwise(currentSelectedSlot);
			}
		}) {
			@Override
			protected List<ITextComponent> getTooltip() {
				List<ITextComponent> lines = new ArrayList<>();
				lines.add(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("rotate")));
				TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsButton("rotate_detail"), TextFormatting.GRAY)
						.forEach(line -> lines.add(new TextComponentString(line)));
				return lines;
			}
		});
		addHideableChild(new ColorToggleButton(new Position(x + 21, y + 24), container::getColor, container::setColor));
		currentSelectedSlot = getSettingsContainer().getFirstSelectedSlot();
	}

	@Override
	public Optional<Integer> getSlotOverlayColor(int slotNumber, boolean templateLoadHovered) {
		if (templateLoadHovered) {
			return getSettingsContainer().getSettingsContainer().getSelectedTemplatesCategory(ItemDisplaySettingsCategory.class)
					.filter(category -> category.getSlots().contains(slotNumber))
					.map(category -> category.getColor().getColorValue());
		}
		return getSettingsContainer().isSlotSelected(slotNumber) ? Optional.of(getSettingsContainer().getColor().getColorValue()) : Optional.empty();
	}

	@Override
	public void handleSlotClick(Slot slot, int mouseButton) {
		if (mouseButton == 0) {
			getSettingsContainer().selectSlot(slot.slotNumber);
			if (getSettingsContainer().isSlotSelected(slot.slotNumber)) {
				currentSelectedSlot = slot.slotNumber;
			}
		} else if (mouseButton == 1) {
			getSettingsContainer().unselectSlot(slot.slotNumber);
			if (currentSelectedSlot == slot.slotNumber) {
				currentSelectedSlot = getSettingsContainer().getFirstSelectedSlot();
			}
		}
	}

	/** The slot the rotate button turns, marked while the tab is open. */
	@Override
	public void renderExtra(Slot slot, boolean templateLoadHovered) {
		if (isOpen && !templateLoadHovered && slot.slotNumber == currentSelectedSlot) {
			GlStateManager.disableDepth();
			GlStateManager.colorMask(true, true, true, false);
			GuiHelper.blit(slot.xPos - 4, slot.yPos - 4, SLOT_SELECTION);
			GlStateManager.colorMask(true, true, true, true);
			GlStateManager.enableDepth();
		}
	}

	@Override
	public int getItemRotation(int slotNumber, boolean templateLoadHovered) {
		if (templateLoadHovered) {
			return getSettingsContainer().getSettingsContainer().getSelectedTemplatesCategory(ItemDisplaySettingsCategory.class)
					.filter(category -> category.getSlots().contains(slotNumber))
					.map(category -> category.getRotation(slotNumber))
					.orElse(0);
		}
		return getSettingsContainer().getRotation(slotNumber);
	}
}
