package dev.feldsher.deconstructedcore.api;

import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.upgrades.UpgradeHandler;
import net.minecraft.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * A storage: its inventory, where its contents live, and how it looks.
 * Settings, sorting and fluids/energy come with the stages that port them.
 */
public interface IStorageWrapper {

	void setContentsChangeHandler(Runnable contentsChangeHandler);

	default void setInventorySlotChangeHandler(Runnable slotChangeHandler) {
		//noop
	}

	InventoryHandler getInventoryHandler();

	/** The inventory as upgrades see it; one that changes what they see comes with the upgrade that does. */
	default InventoryHandler getInventoryForUpgradeProcessing() {
		return getInventoryHandler();
	}

	UpgradeHandler getUpgradeHandler();

	default void setUpgradeCachesInvalidatedHandler(Runnable handler) {
		//noop
	}

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
