package dev.feldsher.deconstructedcore.client.gui;

import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Which tab shows which upgrade container. */
public class UpgradeGuiManager {
	private UpgradeGuiManager() {}

	private static final Map<UpgradeContainerType<?, ?>, TabFactory<?>> UPGRADE_TABS = new HashMap<>();
	private static final Map<UpgradeContainerType<?, ?>, InventoryPartFactory<?>> UPGRADE_INVENTORY_PARTS = new HashMap<>();

	public static <W extends IUpgradeWrapper, C extends UpgradeContainerBase<W, C>> void registerTab(UpgradeContainerType<W, C> containerType, TabFactory<C> tabFactory) {
		UPGRADE_TABS.put(containerType, tabFactory);
	}

	public static <C extends UpgradeContainerBase<?, ?>> UpgradeSettingsTab<C> getTab(C container, Position position, StorageScreenBase<?> screen) {
		//noinspection unchecked
		TabFactory<C> tabFactory = (TabFactory<C>) UPGRADE_TABS.get(container.getType());
		if (tabFactory == null) {
			throw new IllegalStateException("No tab registered for upgrade " + container.getUpgradeStack().getItem().getRegistryName());
		}
		return tabFactory.create(container, position, screen);
	}

	public static <W extends IUpgradeWrapper, C extends UpgradeContainerBase<W, C>> void registerInventoryPart(UpgradeContainerType<W, C> containerType, InventoryPartFactory<C> factory) {
		UPGRADE_INVENTORY_PARTS.put(containerType, factory);
	}

	public static <C extends UpgradeContainerBase<?, ?>> Optional<UpgradeInventoryPartBase<C>> getInventoryPart(int upgradeSlot, C container, Position position, int height, StorageScreenBase<?> screen) {
		//noinspection unchecked
		InventoryPartFactory<C> factory = (InventoryPartFactory<C>) UPGRADE_INVENTORY_PARTS.get(container.getType());
		return factory == null ? Optional.empty() : Optional.of(factory.create(upgradeSlot, container, position, height, screen));
	}

	public interface InventoryPartFactory<C extends UpgradeContainerBase<?, ?>> {
		UpgradeInventoryPartBase<C> create(int upgradeSlot, C container, Position position, int height, StorageScreenBase<?> screen);
	}

	public interface TabFactory<C extends UpgradeContainerBase<?, ?>> {
		UpgradeSettingsTab<C> create(C container, Position position, StorageScreenBase<?> screen);
	}
}
