package dev.feldsher.deconstructedcore.client.gui;

import dev.feldsher.deconstructedcore.client.gui.controls.ItemButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;

/** An upgrade's settings tab, its icon the upgrade itself. Its slots are off screen while it's closed. */
public abstract class UpgradeSettingsTab<C extends UpgradeContainerBase<?, ?>> extends SettingsTabBase<StorageScreenBase<?>> {
	private final C upgradeContainer;

	protected UpgradeSettingsTab(C upgradeContainer, Position position, StorageScreenBase<?> screen, ITextComponent tabLabel, ITextComponent closedTooltip) {
		super(position, screen, tabLabel, closedTooltip,
				onTabIconClicked -> new ItemButton(new Position(position.x() + 1, position.y() + 4), onTabIconClicked, upgradeContainer.getUpgradeStack()));
		this.upgradeContainer = upgradeContainer;
		moveSlotsOutOfView();
	}

	protected C getContainer() {
		return upgradeContainer;
	}

	protected abstract void moveSlotsToTab();

	protected void moveSlotsOutOfView() {
		getContainer().getSlots().forEach(slot -> slot.xPos = StorageScreenBase.DISABLED_SLOT_X_POS);
	}

	@Override
	protected void onTabOpen() {
		super.onTabOpen();
		moveSlotsToTab();
	}

	@Override
	protected void onTabClose() {
		super.onTabClose();
		moveSlotsOutOfView();
	}

	@Override
	protected void setOpen(boolean isOpen) {
		upgradeContainer.setIsOpen(isOpen);
		super.setOpen(isOpen);
	}

	public void onAfterInit() {
		if (upgradeContainer.isOpen()) {
			setOpen(true);
		}
	}

	@SuppressWarnings("unused") //parameters used in overrides
	public boolean slotIsNotCoveredAt(Slot slot, double mouseX, double mouseY) {
		return true;
	}
}
