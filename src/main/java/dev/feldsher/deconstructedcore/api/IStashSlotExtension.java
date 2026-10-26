package dev.feldsher.deconstructedcore.api;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Stashing for a mod's own kind of slot, one the server doesn't have as a slot, as a network terminal's: whether it
 * can stash between the slot and the held stack, and how it asks the server to. Client side.
 */
public interface IStashSlotExtension {
	boolean handles(Slot slot);

	/** Null when nothing can be stashed. */
	@Nullable
	IStashStorageItem.StashResult getStashResult(Slot slot, ItemStack held);

	void stash(GuiContainer screen, Slot slot);
}
