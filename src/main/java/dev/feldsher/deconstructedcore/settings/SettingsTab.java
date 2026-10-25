package dev.feldsher.deconstructedcore.settings;

import dev.feldsher.deconstructedcore.client.gui.SettingsScreen;
import dev.feldsher.deconstructedcore.client.gui.SettingsTabBase;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntConsumer;

/** A settings category's tab: the slots it marks, and what a click on a slot does while it's open. */
public abstract class SettingsTab<C extends SettingsContainerBase<?>> extends SettingsTabBase<SettingsScreen> {
	private final C settingsContainer;

	protected SettingsTab(C settingsContainer, Position position, SettingsScreen screen, ITextComponent tabLabel, ITextComponent tooltip,
			Function<IntConsumer, ButtonBase> getTabButton) {
		super(position, screen, tabLabel, tooltip, getTabButton);
		this.settingsContainer = settingsContainer;
	}

	protected C getSettingsContainer() {
		return settingsContainer;
	}

	/**
	 * The color the slot is marked with, for this category's setting on it. While the template load button is hovered,
	 * the tabs show what the selected template would set instead.
	 */
	public abstract Optional<Integer> getSlotOverlayColor(int slotNumber, boolean templateLoadHovered);

	public abstract void handleSlotClick(Slot slot, int mouseButton);

	@SuppressWarnings("unused") //parameters used in overrides
	public void renderExtra(Slot slot, boolean templateLoadHovered) {
		//noop by default
	}

	/** How far, in degrees, the slot's item is drawn turned, as the storage shows it. */
	@SuppressWarnings("unused") // parameter used in override
	public int getItemRotation(int slotNumber, boolean templateLoadHovered) {
		return 0;
	}

	/** What an empty slot shows instead of nothing, as a memorized item. */
	@SuppressWarnings("unused") // parameter used in override
	public ItemStack getItemDisplayOverride(int slotNumber, boolean templateLoadHovered) {
		return ItemStack.EMPTY;
	}
}
