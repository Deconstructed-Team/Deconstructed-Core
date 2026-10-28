package dev.feldsher.deconstructedcore.controller;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Set;

/** A block the storage tool links to a controller that isn't next to it, as a storage link, within the controller's range. */
public interface ILinkable extends IControllerBoundable {
	default void linkToController(BlockPos controllerPos) {
		if (!canBeLinked() || isLinked()) {
			return;
		}
		World world = getStorageBlockLevel();
		if (!world.isRemote && ControllerBlockEntityBase.getLoadedTileEntity(world, controllerPos, ControllerBlockEntityBase.class).isPresent()) {
			boolean hadControllerPos = getControllerPos().isPresent();
			if (!hadControllerPos) {
				setControllerPos(controllerPos);
			}
			runOnController(world, controller -> {
				if (!controller.addLinkedBlock(getStorageBlockPos()) && !hadControllerPos) {
					removeControllerPos();
				}
			});
		}
	}

	default void unlinkFromController() {
		World world = getStorageBlockLevel();
		if (!world.isRemote && isLinked()) {
			runOnController(world, controller -> controller.removeLinkedBlock(getStorageBlockPos()));
			removeControllerPos();
			setNotLinked();
		}
	}

	default void setNotLinked() {
		//noop by default
	}

	/** What it connects to the controller besides itself, as a link the storage it's on. */
	Set<BlockPos> getConnectablePositions();

	/** Whether it joins the network itself, as a linked storage, not only what it's on. */
	boolean connectLinkedSelf();

	boolean isLinked();

	default boolean canBeLinked() {
		return true;
	}
}
