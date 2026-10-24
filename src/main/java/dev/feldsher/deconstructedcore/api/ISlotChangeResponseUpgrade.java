package dev.feldsher.deconstructedcore.api;

import net.minecraftforge.items.IItemHandler;

public interface ISlotChangeResponseUpgrade {
	void onSlotChange(IItemHandler inventoryHandler, int slot);
}
