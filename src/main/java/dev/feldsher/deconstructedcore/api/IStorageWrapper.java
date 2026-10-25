package dev.feldsher.deconstructedcore.api;

import dev.feldsher.deconstructedcore.inventory.IItemHandlerSimpleInserter;
import dev.feldsher.deconstructedcore.common.gui.SortBy;
import dev.feldsher.deconstructedcore.inventory.FilteredItemHandler;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.renderdata.RenderInfo;
import dev.feldsher.deconstructedcore.settings.SettingsHandler;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.UpgradeHandler;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A storage: its inventory, where its contents live, and how it looks.
 */
public interface IStorageWrapper {

	void setContentsChangeHandler(Runnable contentsChangeHandler);

	default void setInventorySlotChangeHandler(Runnable slotChangeHandler) {
		//noop
	}

	InventoryHandler getInventoryHandler();

	/** The inventory as upgrades see it: with inception, also the inventories of the storages inside. */
	default IItemHandlerSimpleInserter getInventoryForUpgradeProcessing() {
		return getInventoryHandler();
	}

	/** Builds the inventory upgrades see anew, when what changes it changed. */
	default void refreshInventoryForUpgradeProcessing() {
		//noop
	}

	UpgradeHandler getUpgradeHandler();

	/** Its battery, when it has one. */
	default Optional<IEnergyStorage> getEnergyStorage() {
		return Optional.empty();
	}

	/** Its tanks, when it has any. */
	default Optional<IStorageFluidHandler> getFluidHandler() {
		return Optional.empty();
	}

	SettingsHandler getSettingsHandler();

	/** What clients draw the storage with, as its stack carries it. */
	default RenderInfo getRenderInfo() {
		return RenderInfo.EMPTY;
	}

	/**
	 * On the server: brings what the stack carries for clients to draw up to date with the contents and settings,
	 * when they changed it. Each stack with these contents is refreshed by its own wrapper.
	 */
	default void refreshRenderInfo() {
		//noop by default
	}

	/** The inventory as machines see it: through the filter upgrades that filter input and output. */
	default IItemHandlerSimpleInserter getInventoryForInputOutput() {
		List<FilterLogic> inputFilters = new ArrayList<>();
		List<FilterLogic> outputFilters = new ArrayList<>();
		for (IIOFilterUpgrade filterUpgrade : getUpgradeHandler().getWrappersThatImplement(IIOFilterUpgrade.class)) {
			filterUpgrade.getInputFilter().ifPresent(inputFilters::add);
			filterUpgrade.getOutputFilter().ifPresent(outputFilters::add);
		}
		IItemHandlerSimpleInserter inventory = getInventoryForUpgradeProcessing();
		return inputFilters.isEmpty() && outputFilters.isEmpty() ? inventory : new FilteredItemHandler.Modifiable(inventory, inputFilters, outputFilters);
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

	/** What stack upgrades multiply; above 1 for storages that hold more than a stack per slot without upgrades. */
	default int getBaseStackSizeMultiplier() {
		return 1;
	}

	SortBy getSortBy();

	void setSortBy(SortBy sortBy);

	/** Sorts the inventory by its sort order, around the slots that settings leave out of sorting. */
	void sort();

	String getStorageType();

	String getDisplayName();
}
