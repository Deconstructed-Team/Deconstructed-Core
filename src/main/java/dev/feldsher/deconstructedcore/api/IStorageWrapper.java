package dev.feldsher.deconstructedcore.api;

import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import net.minecraft.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * A storage: its inventory, where its contents live, and how it looks.
 * Upgrades, settings, sorting and fluids/energy come with the stages that port them.
 */
public interface IStorageWrapper {

	void setContentsChangeHandler(Runnable contentsChangeHandler);

	default void setInventorySlotChangeHandler(Runnable slotChangeHandler) {
		//noop
	}

	InventoryHandler getInventoryHandler();

	Optional<UUID> getContentsUuid();

	void onContentsNbtUpdated();

	void setPersistent(boolean persistent);

	int getMainColor();

	int getAccentColor();

	void setColors(int mainColor, int accentColor);

	void setColumnsTaken(int columnsTaken, boolean hasChanged);

	int getColumnsTaken();

	default int getNumberOfSlotRows() {
		return 0;
	}

	default ItemStack getWrappedStorageStack() {
		return ItemStack.EMPTY;
	}

	default void onInit() {
		//noop
	}

	String getStorageType();

	String getDisplayName();
}
