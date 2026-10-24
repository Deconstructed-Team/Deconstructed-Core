package dev.feldsher.deconstructedcore.api;

import dev.feldsher.deconstructedcore.inventory.FilteredItemHandler;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.UpgradeHandler;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;
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

	/** The inventory as machines see it: through the filter upgrades that filter input and output. */
	default IItemHandlerModifiable getInventoryForInputOutput() {
		List<FilterLogic> inputFilters = new ArrayList<>();
		List<FilterLogic> outputFilters = new ArrayList<>();
		for (IIOFilterUpgrade filterUpgrade : getUpgradeHandler().getWrappersThatImplement(IIOFilterUpgrade.class)) {
			filterUpgrade.getInputFilter().ifPresent(inputFilters::add);
			filterUpgrade.getOutputFilter().ifPresent(outputFilters::add);
		}
		InventoryHandler inventory = getInventoryForUpgradeProcessing();
		return inputFilters.isEmpty() && outputFilters.isEmpty() ? inventory : new FilteredItemHandler(inventory, inputFilters, outputFilters);
	}

	default void setUpgradeCachesInvalidatedHandler(Runnable handler) {
		//noop
	}

	Optional<UUID> getContentsUuid();

	/** The upgrade whose settings tab was open, so that it opens again. */
	Optional<Integer> getOpenTabId();

	void setOpenTabId(int openTabId);

	void removeOpenTabId();

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
