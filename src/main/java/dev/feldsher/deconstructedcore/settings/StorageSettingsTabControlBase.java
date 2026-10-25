package dev.feldsher.deconstructedcore.settings;

import dev.feldsher.deconstructedcore.client.gui.SettingsScreen;
import dev.feldsher.deconstructedcore.client.gui.SettingsTabControl;
import dev.feldsher.deconstructedcore.client.gui.Tab;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import net.minecraft.client.gui.Gui;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The settings screen's tabs: back to the storage, then one per settings category, which mark the slots they set. */
public abstract class StorageSettingsTabControlBase extends SettingsTabControl<SettingsScreen, SettingsTab<?>> {
	private final List<SettingsTab<?>> settingsTabs = new ArrayList<>();
	protected final SettingsScreen screen;

	protected StorageSettingsTabControlBase(SettingsScreen screen, Position position) {
		super(position);
		this.screen = screen;
		addChild(instantiateReturnBackTab());
		screen.getMenu().forEachSettingsContainer((categoryName, settingsContainer) -> {
			if (isSettingsCategoryDisabled(categoryName)) {
				return;
			}
			ISettingsTabFactory<?, ?> factory = getSettingsTabFactories().get(categoryName);
			if (factory != null) {
				settingsTabs.add(addSettingsTab(() -> {}, () -> {}, instantiateTab(factory, settingsContainer, new Position(x, getTopY()), screen)));
			}
		});
	}

	@SuppressWarnings("unused") //categoryName used in the overrides
	protected boolean isSettingsCategoryDisabled(String categoryName) {
		return false;
	}

	protected abstract Tab instantiateReturnBackTab();

	protected abstract Map<String, ISettingsTabFactory<?, ?>> getSettingsTabFactories();

	@SuppressWarnings("unchecked")
	private static <C extends SettingsContainerBase<?>> SettingsTab<?> instantiateTab(ISettingsTabFactory<?, ?> factory, SettingsContainerBase<?> container, Position position, SettingsScreen screen) {
		return ((ISettingsTabFactory<C, ?>) factory).create((C) container, position, screen);
	}

	/** In stripes, one per tab that marks the slot, translucent over the slot. Coordinates are the screen's, less its corner. */
	public void renderSlotOverlays(Slot slot, boolean templateLoadHovered) {
		List<Integer> colors = new ArrayList<>();
		settingsTabs.forEach(tab -> tab.getSlotOverlayColor(slot.slotNumber, templateLoadHovered).ifPresent(colors::add));
		if (colors.isEmpty()) {
			return;
		}
		int stripeHeight = 16 / colors.size();
		int i = 0;
		for (int color : colors) {
			int yOffset = i * stripeHeight;
			int height = i == colors.size() - 1 ? 16 - yOffset : stripeHeight;
			Gui.drawRect(slot.xPos, slot.yPos + yOffset, slot.xPos + 16, slot.yPos + yOffset + height, (color & 0x00_FFFFFF) | 0x50_000000);
			i++;
		}
	}

	public ItemStack getSlotStackDisplayOverride(int slotNumber, boolean templateLoadHovered) {
		for (SettingsTab<?> settingsTab : settingsTabs) {
			ItemStack stack = settingsTab.getItemDisplayOverride(slotNumber, templateLoadHovered);
			if (!stack.isEmpty()) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	public int getSlotItemRotation(int slotNumber, boolean templateLoadHovered) {
		for (SettingsTab<?> settingsTab : settingsTabs) {
			int rotation = settingsTab.getItemRotation(slotNumber, templateLoadHovered);
			if (rotation != 0) {
				return rotation;
			}
		}
		return 0;
	}

	public void renderSlotExtra(Slot slot, boolean templateLoadHovered) {
		settingsTabs.forEach(tab -> tab.renderExtra(slot, templateLoadHovered));
	}

	public void handleSlotClick(Slot slot, int mouseButton) {
		getOpenTab().ifPresent(tab -> tab.handleSlotClick(slot, mouseButton));
	}

	public interface ISettingsTabFactory<C extends SettingsContainerBase<?>, T extends SettingsTab<C>> {
		T create(C container, Position position, SettingsScreen screen);
	}
}
