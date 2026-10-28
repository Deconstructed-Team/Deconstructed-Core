package dev.feldsher.deconstructedcore.controller;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.inventory.IItemHandlerSimpleInserter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A storage in a controller's network: its inventory is part of the controller's. The controller asks it what it
 * holds as it puts items away, so a storage tells the controller only when its number of slots changes.
 */
public interface IControllableStorage extends IControllerBoundable {
	IStorageWrapper getStorageWrapper();

	/** What the controller puts items in and takes them from, as pipes would: through the storage's filters and lock. */
	default IItemHandlerSimpleInserter getControllerInventory() {
		return getStorageWrapper().getInventoryForInputOutput();
	}

	/** Whether the controller may put a new kind of item into its empty slots, as a locked limited barrel doesn't. */
	default boolean emptySlotsAcceptItems() {
		return true;
	}

	@Override
	default boolean canConnectStorages() {
		return true;
	}

	/** Whether its inventory is part of the controller's; the second half of a double chest only connects. */
	default boolean hasStorageData() {
		return true;
	}

	default void tryToAddToController() {
		addToAdjacentController();
	}

	default void removeFromController() {
		World world = getStorageBlockLevel();
		if (!world.isRemote) {
			getControllerPos().flatMap(p -> ControllerBlockEntityBase.getLoadedTileEntity(world, p, ControllerBlockEntityBase.class))
					.ifPresent(c -> c.removeStorage(getStorageBlockPos()));
			removeControllerPos();
		}
	}

	@Override
	default void addToController(World world, BlockPos pos, BlockPos controllerPos) {
		ControllerBlockEntityBase.getLoadedTileEntity(world, controllerPos, ControllerBlockEntityBase.class).ifPresent(c -> c.addStorage(pos));
	}

	@Override
	default void registerController(ControllerBlockEntityBase controllerBlockEntity) {
		setControllerPos(controllerBlockEntity.getPos());
	}

	@Override
	default void unregisterController() {
		removeControllerPos();
	}

	/** Loaded again, it's still in its controller's network, or it looks for one next to it. */
	default void registerWithControllerOnLoad() {
		getControllerPos().ifPresent(controllerPos -> {
			World world = getStorageBlockLevel();
			if (!world.isRemote) {
				ControllerBlockEntityBase.getLoadedTileEntity(world, controllerPos, ControllerBlockEntityBase.class).ifPresent(controller -> {
					if (!controller.isStorageConnected(getStorageBlockPos())) {
						removeControllerPos();
						tryToAddToController();
					}
				});
			}
		});
	}

	/** Its inventory grew or shrank, as a tier up: the controller's slots after it move. */
	default void changeSlots(int newSlots) {
		World world = getStorageBlockLevel();
		if (!world.isRemote) {
			runOnController(world, controller -> controller.changeSlots(getStorageBlockPos(), newSlots));
		}
	}
}
