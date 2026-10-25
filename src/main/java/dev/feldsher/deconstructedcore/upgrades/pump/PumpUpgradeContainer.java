package dev.feldsher.deconstructedcore.upgrades.pump;

import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class PumpUpgradeContainer extends UpgradeContainerBase<PumpUpgradeWrapper, PumpUpgradeContainer> {
	private static final String DATA_IS_INPUT = "isInput";
	private static final String DATA_INTERACT_WITH_HAND = "interactWithHand";
	private static final String DATA_INTERACT_WITH_WORLD = "interactWithWorld";
	private final FluidFilterContainer fluidFilterContainer;

	public PumpUpgradeContainer(EntityPlayer player, int upgradeContainerId, PumpUpgradeWrapper upgradeWrapper, UpgradeContainerType<PumpUpgradeWrapper, PumpUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
		fluidFilterContainer = new FluidFilterContainer(player, this, () -> this.upgradeWrapper.getFluidFilterLogic());
	}

	public void setIsInput(boolean input) {
		upgradeWrapper.setIsInput(input);
		sendBooleanToServer(DATA_IS_INPUT, input);
	}

	public boolean isInput() {
		return upgradeWrapper.isInput();
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(DATA_IS_INPUT)) {
			setIsInput(data.getBoolean(DATA_IS_INPUT));
		} else if (data.hasKey(DATA_INTERACT_WITH_HAND)) {
			setInteractWithHand(data.getBoolean(DATA_INTERACT_WITH_HAND));
		} else if (data.hasKey(DATA_INTERACT_WITH_WORLD)) {
			setInteractWithWorld(data.getBoolean(DATA_INTERACT_WITH_WORLD));
		}
		fluidFilterContainer.handlePacket(data);
	}

	public FluidFilterContainer getFluidFilterContainer() {
		return fluidFilterContainer;
	}

	public void setInteractWithHand(boolean interactWithHand) {
		upgradeWrapper.setInteractWithHand(interactWithHand);
		sendBooleanToServer(DATA_INTERACT_WITH_HAND, interactWithHand);
	}

	public boolean shouldInteractWithHand() {
		return upgradeWrapper.shouldInteractWithHand();
	}

	public void setInteractWithWorld(boolean interactWithWorld) {
		upgradeWrapper.setInteractWithWorld(interactWithWorld);
		sendBooleanToServer(DATA_INTERACT_WITH_WORLD, interactWithWorld);
	}

	public boolean shouldInteractWithWorld() {
		return upgradeWrapper.shouldInteractWithWorld();
	}
}
