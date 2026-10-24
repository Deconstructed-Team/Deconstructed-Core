package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeWrapper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Which upgrades have a settings tab, and the container behind it. */
public class UpgradeContainerRegistry {
	private UpgradeContainerRegistry() {}

	private static final Map<ResourceLocation, UpgradeContainerType<? extends IUpgradeWrapper, ? extends UpgradeContainerBase<?, ?>>> UPGRADE_CONTAINERS = new HashMap<>();

	public static void register(ResourceLocation upgradeName, UpgradeContainerType<? extends IUpgradeWrapper, ? extends UpgradeContainerBase<?, ?>> containerFactory) {
		UPGRADE_CONTAINERS.put(upgradeName, containerFactory);
	}

	public static <W extends IUpgradeWrapper, C extends UpgradeContainerBase<W, C>> Optional<UpgradeContainerBase<W, C>> instantiateContainer(EntityPlayer player, int containerId, W wrapper) {
		ResourceLocation upgradeName = wrapper.getUpgradeStack().getItem().getRegistryName();
		if (!(wrapper.getUpgradeStack().getItem() instanceof IUpgradeItem<?>) || wrapper.hideSettingsTab() || !UPGRADE_CONTAINERS.containsKey(upgradeName)) {
			return Optional.empty();
		}
		//noinspection unchecked
		return Optional.of((UpgradeContainerBase<W, C>) getContainerType(upgradeName).create(player, containerId, wrapper));
	}

	private static <W extends IUpgradeWrapper, C extends UpgradeContainerBase<W, C>> UpgradeContainerType<W, C> getContainerType(ResourceLocation upgradeName) {
		//noinspection unchecked
		return (UpgradeContainerType<W, C>) UPGRADE_CONTAINERS.get(upgradeName);
	}
}
