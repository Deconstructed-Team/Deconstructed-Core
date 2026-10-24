package dev.feldsher.deconstructedcore.upgrades.magnet;

import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterLogicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class MagnetUpgradeContainer extends UpgradeContainerBase<MagnetUpgradeWrapper, MagnetUpgradeContainer> {
	private static final String DATA_PICKUP_ITEMS = "pickupItems";
	private static final String DATA_PICKUP_XP = "pickupXp";

	private final ContentsFilterLogicContainer filterLogicContainer;

	public MagnetUpgradeContainer(EntityPlayer player, int containerId, MagnetUpgradeWrapper wrapper, UpgradeContainerType<MagnetUpgradeWrapper, MagnetUpgradeContainer> type) {
		super(player, containerId, wrapper, type);
		filterLogicContainer = new ContentsFilterLogicContainer(() -> upgradeWrapper.getFilterLogic(), this, slots::add);
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(DATA_PICKUP_ITEMS)) {
			setPickupItems(data.getBoolean(DATA_PICKUP_ITEMS));
		} else if (data.hasKey(DATA_PICKUP_XP)) {
			setPickupXp(data.getBoolean(DATA_PICKUP_XP));
		}
		filterLogicContainer.handlePacket(data);
	}

	public ContentsFilterLogicContainer getFilterLogicContainer() {
		return filterLogicContainer;
	}

	public void setPickupItems(boolean pickupItems) {
		upgradeWrapper.setPickupItems(pickupItems);
		sendBooleanToServer(DATA_PICKUP_ITEMS, pickupItems);
	}

	public boolean shouldPickupItems() {
		return upgradeWrapper.shouldPickupItems();
	}

	public void setPickupXp(boolean pickupXp) {
		upgradeWrapper.setPickupXp(pickupXp);
		sendBooleanToServer(DATA_PICKUP_XP, pickupXp);
	}

	public boolean shouldPickupXp() {
		return upgradeWrapper.shouldPickupXp();
	}
}
