package dev.feldsher.deconstructedcore.upgrades.compacting;

import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.FilterLogicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class CompactingUpgradeContainer extends UpgradeContainerBase<CompactingUpgradeWrapper, CompactingUpgradeContainer> {
	private static final String DATA_SHOULD_WORK_IN_GUI = "shouldWorkInGUI";
	private static final String DATA_SHOULD_COMPACT_NON_UNCRAFTABLE = "shouldCompactNonUncraftable";
	private final FilterLogicContainer<FilterLogic> filterLogicContainer;

	public CompactingUpgradeContainer(EntityPlayer player, int containerId, CompactingUpgradeWrapper wrapper, UpgradeContainerType<CompactingUpgradeWrapper, CompactingUpgradeContainer> type) {
		super(player, containerId, wrapper, type);
		filterLogicContainer = new FilterLogicContainer<>(upgradeWrapper::getFilterLogic, this, slots::add);
	}

	public FilterLogicContainer<FilterLogic> getFilterLogicContainer() {
		return filterLogicContainer;
	}

	public void setCompactNonUncraftable(boolean shouldCompactNonUncraftable) {
		upgradeWrapper.setCompactNonUncraftable(shouldCompactNonUncraftable);
		sendBooleanToServer(DATA_SHOULD_COMPACT_NON_UNCRAFTABLE, shouldCompactNonUncraftable);
	}

	public boolean shouldCompactNonUncraftable() {
		return upgradeWrapper.shouldCompactNonUncraftable();
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(DATA_SHOULD_COMPACT_NON_UNCRAFTABLE)) {
			setCompactNonUncraftable(data.getBoolean(DATA_SHOULD_COMPACT_NON_UNCRAFTABLE));
		} else if (data.hasKey(DATA_SHOULD_WORK_IN_GUI)) {
			setShouldWorkInGUI(data.getBoolean(DATA_SHOULD_WORK_IN_GUI));
		} else {
			filterLogicContainer.handlePacket(data);
		}
	}

	public void setShouldWorkInGUI(boolean shouldWorkInGUI) {
		upgradeWrapper.setShouldWorkInGUI(shouldWorkInGUI);
		sendBooleanToServer(DATA_SHOULD_WORK_IN_GUI, shouldWorkInGUI);
	}

	public boolean shouldWorkInGUI() {
		return upgradeWrapper.shouldWorkInGUI();
	}
}
