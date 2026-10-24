package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class ContentsFilteredUpgradeContainer<W extends IUpgradeWrapper & IContentsFilteredUpgrade>
		extends UpgradeContainerBase<W, ContentsFilteredUpgradeContainer<W>> {
	private final ContentsFilterLogicContainer filterLogicContainer;

	public ContentsFilteredUpgradeContainer(EntityPlayer player, int containerId, W wrapper, UpgradeContainerType<W, ContentsFilteredUpgradeContainer<W>> type) {
		super(player, containerId, wrapper, type);
		filterLogicContainer = new ContentsFilterLogicContainer(() -> upgradeWrapper.getFilterLogic(), this, slots::add);
	}

	public ContentsFilterLogicContainer getFilterLogicContainer() {
		return filterLogicContainer;
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		filterLogicContainer.handlePacket(data);
	}
}
