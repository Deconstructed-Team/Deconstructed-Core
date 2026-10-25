package dev.feldsher.deconstructedcore.upgrades.stonecutter;

import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;

public class StonecutterUpgradeContainer extends UpgradeContainerBase<StonecutterUpgradeWrapper, StonecutterUpgradeContainer> {
	private static final String DATA_SHIFT_CLICK_INTO_STORAGE = "shiftClickIntoStorage";
	private final StonecutterRecipeContainer recipeContainer;

	public StonecutterUpgradeContainer(EntityPlayer player, int containerId, StonecutterUpgradeWrapper wrapper, UpgradeContainerType<StonecutterUpgradeWrapper, StonecutterUpgradeContainer> type) {
		super(player, containerId, wrapper, type);
		recipeContainer = new StonecutterRecipeContainer(this, slots::add, this, player);
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(DATA_SHIFT_CLICK_INTO_STORAGE)) {
			setShiftClickIntoStorage(data.getBoolean(DATA_SHIFT_CLICK_INTO_STORAGE));
		} else {
			recipeContainer.handlePacket(data);
		}
	}

	public boolean shouldShiftClickIntoStorage() {
		return upgradeWrapper.shouldShiftClickIntoStorage();
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		upgradeWrapper.setShiftClickIntoStorage(shiftClickIntoStorage);
		sendBooleanToServer(DATA_SHIFT_CLICK_INTO_STORAGE, shiftClickIntoStorage);
	}

	public StonecutterRecipeContainer getRecipeContainer() {
		return recipeContainer;
	}

	@Override
	public boolean mergeIntoStorageFirst(Slot slot) {
		return recipeContainer.isNotResultSlot(slot) || shouldShiftClickIntoStorage();
	}

	@Override
	public boolean allowsPickupAll(Slot slot) {
		return recipeContainer.isNotResultSlot(slot);
	}
}
