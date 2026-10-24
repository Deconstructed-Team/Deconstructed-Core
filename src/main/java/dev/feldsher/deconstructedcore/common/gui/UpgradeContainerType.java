package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeWrapper;
import net.minecraft.entity.player.EntityPlayer;

public class UpgradeContainerType<W extends IUpgradeWrapper, C extends UpgradeContainerBase<W, C>> {
	private final IFactory<W, C> factory;

	public UpgradeContainerType(IFactory<W, C> factory) {
		this.factory = factory;
	}

	public C create(EntityPlayer player, int containerId, W wrapper) {
		return factory.create(player, containerId, wrapper, this);
	}

	public interface IFactory<W extends IUpgradeWrapper, C extends UpgradeContainerBase<W, C>> {
		C create(EntityPlayer player, int containerId, W upgrade, UpgradeContainerType<W, C> type);
	}
}
