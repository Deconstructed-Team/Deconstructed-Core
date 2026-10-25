package dev.feldsher.deconstructedcore.client.gui;

import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;

/** What an upgrade draws in the columns of the storage's inventory it takes, as a tank; at gui positions. */
public abstract class UpgradeInventoryPartBase<C extends UpgradeContainerBase<?, ?>> {
	protected final C container;
	protected final int upgradeSlot;

	protected UpgradeInventoryPartBase(int upgradeSlot, C container) {
		this.container = container;
		this.upgradeSlot = upgradeSlot;
	}

	public abstract void render(int mouseX, int mouseY);

	/** At screen positions; true when it took the click. */
	public abstract boolean handleMouseReleased(int mouseX, int mouseY, int button);

	public abstract void renderErrorOverlay();

	/** At screen positions. */
	public abstract void renderTooltip(StorageScreenBase<?> screen, int mouseX, int mouseY);
}
