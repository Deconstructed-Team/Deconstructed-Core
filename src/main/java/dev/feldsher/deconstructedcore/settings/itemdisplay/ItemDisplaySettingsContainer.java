package dev.feldsher.deconstructedcore.settings.itemdisplay;

import dev.feldsher.deconstructedcore.common.gui.SettingsContainerMenu;
import dev.feldsher.deconstructedcore.renderdata.DisplaySide;
import dev.feldsher.deconstructedcore.settings.SettingsContainerBase;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;

import java.util.List;

/**
 * The client asks, the server changes what's shown, and the settings come back to the client changed. Selecting and
 * unselecting change the client's copy at once too, so that the tab's selection mark knows where to go.
 */
public class ItemDisplaySettingsContainer extends SettingsContainerBase<ItemDisplaySettingsCategory> {
	private static final String COLOR_TAG = "color";
	private static final String DISPLAY_SIDE_TAG = "displaySide";
	private static final String SELECT_SLOT_TAG = "selectSlot";
	private static final String UNSELECT_SLOT_TAG = "unselectSlot";
	private static final String ROTATE_CLOCKWISE_TAG = "rotateClockwise";
	private static final String ROTATE_COUNTER_CLOCKWISE_TAG = "rotateCounterClockwise";

	public ItemDisplaySettingsContainer(SettingsContainerMenu<?> settingsContainer, String categoryName, ItemDisplaySettingsCategory category) {
		super(settingsContainer, categoryName, category);
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(SELECT_SLOT_TAG)) {
			selectSlot(data.getInteger(SELECT_SLOT_TAG));
		} else if (data.hasKey(UNSELECT_SLOT_TAG)) {
			unselectSlot(data.getInteger(UNSELECT_SLOT_TAG));
		} else if (data.hasKey(ROTATE_CLOCKWISE_TAG)) {
			rotateClockwise(data.getInteger(ROTATE_CLOCKWISE_TAG));
		} else if (data.hasKey(ROTATE_COUNTER_CLOCKWISE_TAG)) {
			rotateCounterClockwise(data.getInteger(ROTATE_COUNTER_CLOCKWISE_TAG));
		} else if (data.hasKey(COLOR_TAG)) {
			setColor(EnumDyeColor.byMetadata(data.getInteger(COLOR_TAG)));
		} else if (data.hasKey(DISPLAY_SIDE_TAG)) {
			setDisplaySide(DisplaySide.fromName(data.getString(DISPLAY_SIDE_TAG)));
		}
	}

	public void unselectSlot(int slotIndex) {
		if (!isSlotSelected(slotIndex)) {
			return;
		}
		getCategory().unselectSlot(slotIndex);
		if (!isServer()) {
			sendIntToServer(UNSELECT_SLOT_TAG, slotIndex);
		}
	}

	public void selectSlot(int slotIndex) {
		if (isSlotSelected(slotIndex)) {
			return;
		}
		getCategory().selectSlot(slotIndex);
		if (!isServer()) {
			sendIntToServer(SELECT_SLOT_TAG, slotIndex);
		}
	}

	public void rotateClockwise(int slotIndex) {
		if (isServer()) {
			getCategory().rotate(slotIndex, true);
		} else {
			sendIntToServer(ROTATE_CLOCKWISE_TAG, slotIndex);
		}
	}

	public void rotateCounterClockwise(int slotIndex) {
		if (isServer()) {
			getCategory().rotate(slotIndex, false);
		} else {
			sendIntToServer(ROTATE_COUNTER_CLOCKWISE_TAG, slotIndex);
		}
	}

	public void setColor(EnumDyeColor color) {
		if (isServer()) {
			getCategory().setColor(color);
		} else {
			sendIntToServer(COLOR_TAG, color.getMetadata());
		}
	}

	public void setDisplaySide(DisplaySide displaySide) {
		if (isServer()) {
			getCategory().setDisplaySide(displaySide);
		} else {
			sendStringToServer(DISPLAY_SIDE_TAG, displaySide.getSerializedName());
		}
	}

	public boolean isSlotSelected(int slotIndex) {
		return getCategory().getSlots().contains(slotIndex);
	}

	public EnumDyeColor getColor() {
		return getCategory().getColor();
	}

	public int getRotation(int slotIndex) {
		return getCategory().getRotation(slotIndex);
	}

	public int getFirstSelectedSlot() {
		List<Integer> slots = getCategory().getSlots();
		return slots.isEmpty() ? -1 : slots.get(0);
	}

	public DisplaySide getDisplaySide() {
		return getCategory().getDisplaySide();
	}
}
