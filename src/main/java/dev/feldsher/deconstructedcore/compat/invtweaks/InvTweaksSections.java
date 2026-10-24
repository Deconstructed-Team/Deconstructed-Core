package dev.feldsher.deconstructedcore.compat.invtweaks;

import invtweaks.api.container.ContainerSection;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The sections Inventory Tweaks sorts and moves items in, for a storage container: only the player's inventory. It
 * sorts with clicks that know nothing of storage slots holding more than a stack or of slots kept out of sorting, so
 * storage slots are in no section and the storage's own sort handles them. Called only by Inventory Tweaks.
 */
public final class InvTweaksSections {
	private static final int HOTBAR_SIZE = 9;
	private static final int MAIN_INVENTORY_SIZE = 36;

	private InvTweaksSections() {}

	public static Map<ContainerSection, List<Slot>> playerInventoryOnly(Container container) {
		List<Slot> notHotbar = new ArrayList<>();
		List<Slot> hotbar = new ArrayList<>();
		for (Slot slot : container.inventorySlots) {
			if (slot.inventory instanceof InventoryPlayer && slot.getSlotIndex() < MAIN_INVENTORY_SIZE) {
				(slot.getSlotIndex() < HOTBAR_SIZE ? hotbar : notHotbar).add(slot);
			}
		}
		// In the order of vanilla containers, which its sorting expects: the main inventory row by row, then the hotbar
		notHotbar.sort(Comparator.comparingInt(Slot::getSlotIndex));
		hotbar.sort(Comparator.comparingInt(Slot::getSlotIndex));
		List<Slot> inventory = new ArrayList<>(notHotbar);
		inventory.addAll(hotbar);

		Map<ContainerSection, List<Slot>> sections = new EnumMap<>(ContainerSection.class);
		sections.put(ContainerSection.INVENTORY, inventory);
		sections.put(ContainerSection.INVENTORY_NOT_HOTBAR, notHotbar);
		sections.put(ContainerSection.INVENTORY_HOTBAR, hotbar);
		return sections;
	}
}
