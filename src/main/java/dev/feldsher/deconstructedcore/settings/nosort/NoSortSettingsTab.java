package dev.feldsher.deconstructedcore.settings.nosort;

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
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.Optional;

import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_BACKGROUND;
import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND;

/** Left click marks a slot to be left out of sorting, right click unmarks it. */
public class NoSortSettingsTab extends SettingsTab<NoSortSettingsContainer> {
	private static final TextureBlitData ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(32, 80), Dimension.SQUARE_16);
	private static final TextureBlitData SELECT_ALL_SLOTS_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(16, 80), Dimension.SQUARE_16);
	public static final ButtonDefinition SELECT_ALL_SLOTS = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, SELECT_ALL_SLOTS_FOREGROUND,
			new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("select_all_slots")));
	private static final TextureBlitData UNSELECT_ALL_SLOTS_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(48, 80), Dimension.SQUARE_16);
	public static final ButtonDefinition UNSELECT_ALL_SLOTS = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, UNSELECT_ALL_SLOTS_FOREGROUND,
			new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("unselect_all_slots")));

	public NoSortSettingsTab(NoSortSettingsContainer container, Position position, SettingsScreen screen) {
		super(container, position, screen, new TextComponentTranslation(TranslationHelper.INSTANCE.translSettings(NoSortSettingsCategory.NAME)),
				new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsTooltip(NoSortSettingsCategory.NAME)),
				onTabIconClicked -> new ImageButton(new Position(position.x() + 1, position.y() + 4), Dimension.SQUARE_16, ICON, onTabIconClicked));
		addHideableChild(new Button(new Position(x + 3, y + 24), SELECT_ALL_SLOTS, button -> container.selectAllSlots()));
		addHideableChild(new Button(new Position(x + 21, y + 24), UNSELECT_ALL_SLOTS, button -> container.unselectAllSlots()));
		addHideableChild(new ColorToggleButton(new Position(x + 39, y + 24), container::getColor, container::setColor));
	}

	@Override
	public Optional<Integer> getSlotOverlayColor(int slotNumber) {
		return getSettingsContainer().isSlotSelected(slotNumber) ? Optional.of(getSettingsContainer().getColor().getColorValue()) : Optional.empty();
	}

	@Override
	public void handleSlotClick(Slot slot, int mouseButton) {
		if (mouseButton == 0) {
			getSettingsContainer().selectSlot(slot.slotNumber);
		} else if (mouseButton == 1) {
			getSettingsContainer().unselectSlot(slot.slotNumber);
		}
	}
}
