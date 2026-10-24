package dev.feldsher.deconstructedcore.upgrades.voiding;

import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.FilterLogicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class VoidUpgradeContainer extends UpgradeContainerBase<VoidUpgradeWrapper, VoidUpgradeContainer> {
	private static final String DATA_SHOULD_WORKD_IN_GUI = "shouldWorkdInGUI";
	private static final String DATA_SHOULD_VOID_OVERFLOW = "shouldVoidOverflow";
	private final FilterLogicContainer<FilterLogic> filterLogicContainer;

	public VoidUpgradeContainer(EntityPlayer player, int containerId, VoidUpgradeWrapper wrapper, UpgradeContainerType<VoidUpgradeWrapper, VoidUpgradeContainer> type) {
		super(player, containerId, wrapper, type);
		filterLogicContainer = new FilterLogicContainer<>(() -> upgradeWrapper.getFilterLogic(), this, slots::add);
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(DATA_SHOULD_WORKD_IN_GUI)) {
			setShouldWorkdInGUI(data.getBoolean(DATA_SHOULD_WORKD_IN_GUI));
		} else if (data.hasKey(DATA_SHOULD_VOID_OVERFLOW)) {
			setShouldVoidOverflow(data.getBoolean(DATA_SHOULD_VOID_OVERFLOW));
		}
		filterLogicContainer.handlePacket(data);
	}

	public FilterLogicContainer<FilterLogic> getFilterLogicContainer() {
		return filterLogicContainer;
	}

	public void setShouldWorkdInGUI(boolean shouldWorkdInGUI) {
		upgradeWrapper.setShouldWorkdInGUI(shouldWorkdInGUI);
		sendBooleanToServer(DATA_SHOULD_WORKD_IN_GUI, shouldWorkdInGUI);
	}

	public void setShouldVoidOverflow(boolean shouldVoidOverflow) {
		upgradeWrapper.setShouldVoidOverflow(shouldVoidOverflow);
		sendBooleanToServer(DATA_SHOULD_VOID_OVERFLOW, shouldVoidOverflow);
	}

	public boolean shouldWorkInGUI() {
		return upgradeWrapper.shouldWorkInGUI();
	}

	public boolean shouldVoidOverflow() {
		return upgradeWrapper.shouldVoidOverflow();
	}
}
