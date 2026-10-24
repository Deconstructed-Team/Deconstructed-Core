package dev.feldsher.deconstructedcore.settings.memory;

import dev.feldsher.deconstructedcore.common.gui.SettingsContainerMenu;
import dev.feldsher.deconstructedcore.settings.SettingsContainerBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** The client asks, the server changes what the slots remember, and the settings come back to the client changed. */
public class MemorySettingsContainer extends SettingsContainerBase<MemorySettingsCategory> {
	private static final String ACTION_TAG = "action";
	private static final String SELECT_ALL_ACTION = "selectAll";
	private static final String UNSELECT_ALL_ACTION = "unselectAll";
	private static final String UNSELECT_SLOT_TAG = "unselectSlot";
	private static final String SELECT_SLOT_TAG = "selectSlot";
	private static final String IGNORE_NBT_TAG = "ignoreNbt";

	public MemorySettingsContainer(SettingsContainerMenu<?> settingsContainer, String categoryName, MemorySettingsCategory category) {
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
		} else if (data.hasKey(IGNORE_NBT_TAG)) {
			setIgnoreNbt(data.getBoolean(IGNORE_NBT_TAG));
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
			getCategory().selectSlot(slotNumber);
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

	public boolean isSlotSelected(int slotNumber) {
		return getCategory().isSlotSelected(slotNumber);
	}

	public boolean ignoresNbt() {
		return getCategory().ignoresNbt();
	}

	public void setIgnoreNbt(boolean ignoreNbt) {
		if (isServer()) {
			getCategory().setIgnoreNbt(ignoreNbt);
		} else {
			sendBooleanToServer(IGNORE_NBT_TAG, ignoreNbt);
		}
	}

	public ItemStack getMemorizedStack(int slotNumber) {
		return getCategory().getSlotFilterStack(slotNumber, false).orElse(ItemStack.EMPTY);
	}
}
