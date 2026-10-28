package dev.feldsher.deconstructedcore.settings.nosort;

import dev.feldsher.deconstructedcore.common.gui.SettingsContainerMenu;
import dev.feldsher.deconstructedcore.settings.SettingsContainerBase;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;

/** The client asks, the server changes the slots, and the settings come back to the client changed. */
public class NoSortSettingsContainer extends SettingsContainerBase<NoSortSettingsCategory> {
	private static final String ACTION_TAG = "action";
	private static final String SELECT_ALL_ACTION = "selectAll";
	private static final String UNSELECT_ALL_ACTION = "unselectAll";
	private static final String UNSELECT_SLOT_TAG = "unselectSlot";
	private static final String SELECT_SLOT_TAG = "selectSlot";
	private static final String COLOR_TAG = "color";

	public NoSortSettingsContainer(SettingsContainerMenu<?> settingsContainer, String categoryName, NoSortSettingsCategory category) {
		super(settingsContainer, categoryName, category);
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(ACTION_TAG)) {
			String action = data.getString(ACTION_TAG);
			if (SELECT_ALL_ACTION.equals(action)) {
				selectAllSlots();
			} else if (UNSELECT_ALL_ACTION.equals(action)) {
				unselectAllSlots();
			}
		} else if (data.hasKey(SELECT_SLOT_TAG)) {
			selectSlot(data.getInteger(SELECT_SLOT_TAG));
		} else if (data.hasKey(UNSELECT_SLOT_TAG)) {
			unselectSlot(data.getInteger(UNSELECT_SLOT_TAG));
		} else if (data.hasKey(COLOR_TAG)) {
			setColor(EnumDyeColor.byMetadata(data.getInteger(COLOR_TAG)));
		}
	}

	public void unselectSlot(int slotNumber) {
		if (!isSlotSelected(slotNumber)) {
			return;
		}
		if (isServer()) {
			getCategory().unselectSlot(slotNumber);
		} else {
			sendIntToServer(UNSELECT_SLOT_TAG, slotNumber);
		}
	}

	public void selectSlot(int slotNumber) {
		if (isSlotSelected(slotNumber)) {
			return;
		}
		if (isServer()) {
			// one of the storage's own slots, which the client is held to
			if (slotNumber >= 0 && slotNumber < getSettingsContainer().getStorageWrapper().getInventoryHandler().getSlots()) {
				getCategory().selectSlot(slotNumber);
			}
		} else {
			sendIntToServer(SELECT_SLOT_TAG, slotNumber);
		}
	}

	public void unselectAllSlots() {
		if (isServer()) {
			getCategory().unselectAllSlots();
		} else {
			sendStringToServer(ACTION_TAG, UNSELECT_ALL_ACTION);
		}
	}

	public void selectAllSlots() {
		if (isServer()) {
			getCategory().selectSlots(0, getSettingsContainer().getNumberOfStorageInventorySlots());
		} else {
			sendStringToServer(ACTION_TAG, SELECT_ALL_ACTION);
		}
	}

	public void setColor(EnumDyeColor color) {
		if (isServer()) {
			getCategory().setColor(color);
		} else {
			sendIntToServer(COLOR_TAG, color.getMetadata());
		}
	}

	public boolean isSlotSelected(int slotNumber) {
		return getCategory().isSlotSelected(slotNumber);
	}

	public EnumDyeColor getColor() {
		return getCategory().getColor();
	}
}
