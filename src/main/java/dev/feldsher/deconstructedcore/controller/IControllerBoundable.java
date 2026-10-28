package dev.feldsher.deconstructedcore.controller;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A block a controller takes into its network: a storage, or a block that only joins, as an I/O block. Those that
 * connect storages pass the network on to their neighbours.
 */
public interface IControllerBoundable {
	String CONTROLLER_POS_TAG = "controllerPos";

	void setControllerPos(BlockPos controllerPos);

	Optional<BlockPos> getControllerPos();

	void removeControllerPos();

	BlockPos getStorageBlockPos();

	World getStorageBlockLevel();

	default boolean canBeConnected() {
		return !getControllerPos().isPresent();
	}

	void registerController(ControllerBlockEntityBase controllerBlockEntity);

	void unregisterController();

	boolean canConnectStorages();

	default void runOnController(World world, Consumer<ControllerBlockEntityBase> toRun) {
		getControllerPos().flatMap(pos -> ControllerBlockEntityBase.getLoadedTileEntity(world, pos, ControllerBlockEntityBase.class)).ifPresent(toRun);
	}

	default void saveControllerPos(NBTTagCompound tag) {
		getControllerPos().ifPresent(p -> tag.setLong(CONTROLLER_POS_TAG, p.toLong()));
	}

	default void loadControllerPos(NBTTagCompound tag) {
		if (tag.hasKey(CONTROLLER_POS_TAG)) {
			setControllerPos(BlockPos.fromLong(tag.getLong(CONTROLLER_POS_TAG)));
		} else {
			removeControllerPos();
		}
	}

	default void addToController(World world, BlockPos pos, BlockPos controllerPos) {
		//noop by default
	}

	/** Into the network of a neighbouring controller, or of a neighbour that connects storages and is in one. */
	default void addToAdjacentController() {
		World world = getStorageBlockLevel();
		if (world.isRemote) {
			return;
		}
		BlockPos pos = getStorageBlockPos();
		for (EnumFacing dir : EnumFacing.values()) {
			BlockPos offsetPos = pos.offset(dir);
			TileEntity te = world.getTileEntity(offsetPos);
			if (te instanceof IControllerBoundable) {
				IControllerBoundable neighbour = (IControllerBoundable) te;
				if (neighbour.canConnectStorages()) {
					neighbour.getControllerPos().ifPresent(controllerPos -> addToController(world, pos, controllerPos));
				}
			} else {
				addToController(world, pos, offsetPos);
			}
			if (getControllerPos().isPresent()) {
				break;
			}
		}
	}
}
