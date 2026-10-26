package dev.feldsher.deconstructedcore.client.gui;

import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import net.minecraft.inventory.Slot;

import java.util.Map;

/**
 * The tabs on the right of a storage's screen: the storage's settings, then one per upgrade with settings.
 */
public class UpgradeSettingsTabControl extends SettingsTabControl<StorageScreenBase<?>, UpgradeSettingsTab<?>> {
	public UpgradeSettingsTabControl(Position position, StorageScreenBase<?> screen, String storageSettingsTabTooltip) {
		super(position);
		addChild(new StorageSettingsTab(new Position(x, getTopY()), screen, storageSettingsTabTooltip));
		for (Map.Entry<Integer, UpgradeContainerBase<?, ?>> entry : screen.getMenu().getUpgradeContainers().entrySet()) {
			addSettingsTab(() -> screen.getMenu().setOpenTabId(entry.getKey()), () -> screen.getMenu().removeOpenTabId(),
					UpgradeGuiManager.getTab(entry.getValue(), new Position(x, getTopY()), screen))
					.onAfterInit();
		}
	}

	public boolean slotIsNotCoveredAt(Slot slot, double mouseX, double mouseY) {
		for (Tab tab : children) {
			if (tab instanceof UpgradeSettingsTab<?> && !((UpgradeSettingsTab<?>) tab).slotIsNotCoveredAt(slot, mouseX, mouseY)) {
				return false;
			}
		}
		return true;
	}

	/** Opens the upgrade's tab, as a click on it would, unless it's open. */
	public void openUpgradeTab(UpgradeContainerBase<?, ?> upgradeContainer) {
		for (Tab tab : children) {
			if (tab instanceof UpgradeSettingsTab<?> && ((UpgradeSettingsTab<?>) tab).getContainer() == upgradeContainer && !((UpgradeSettingsTab<?>) tab).isOpen()) {
				((UpgradeSettingsTab<?>) tab).setOpen(true);
			}
		}
	}

	public void tick() {
		children.forEach(Tab::tick);
	}
}
