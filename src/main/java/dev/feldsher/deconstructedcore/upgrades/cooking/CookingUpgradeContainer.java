package dev.feldsher.deconstructedcore.upgrades.cooking;

import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class CookingUpgradeContainer extends UpgradeContainerBase<CookingUpgradeWrapper, CookingUpgradeContainer> {
	private final CookingLogicContainer cookingLogicContainer;

	public CookingUpgradeContainer(EntityPlayer player, int containerId, CookingUpgradeWrapper wrapper, UpgradeContainerType<CookingUpgradeWrapper, CookingUpgradeContainer> type) {
		super(player, containerId, wrapper, type);
		cookingLogicContainer = new CookingLogicContainer(() -> upgradeWrapper.getCookingLogic(), slots::add);
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		//noop
	}

	public CookingLogicContainer getCookingLogicContainer() {
		return cookingLogicContainer;
	}
}
