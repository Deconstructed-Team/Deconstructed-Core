package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.network.PacketHandler;
import dev.feldsher.deconstructedcore.network.SyncContainerClientDataMessage;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeWrapper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The container side of an upgrade's settings tab: its slots, and the data its controls send.
 * A change made on the client is applied there at once and sent to the server, which applies the same.
 */
public abstract class UpgradeContainerBase<W extends IUpgradeWrapper, C extends UpgradeContainerBase<W, C>> implements IServerUpdater {
	protected final ArrayList<Slot> slots = new ArrayList<>();

	private final int upgradeContainerId;

	protected W upgradeWrapper;
	protected final EntityPlayer player;
	private final UpgradeContainerType<W, C> type;
	private boolean isOpen = false;

	protected UpgradeContainerBase(EntityPlayer player, int upgradeContainerId, W upgradeWrapper, UpgradeContainerType<W, C> type) {
		this.upgradeContainerId = upgradeContainerId;
		this.upgradeWrapper = upgradeWrapper;
		this.player = player;
		this.type = type;
	}

	public List<Slot> getSlots() {
		return slots;
	}

	public UpgradeContainerType<W, C> getType() {
		return type;
	}

	public void setIsOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}

	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public void sendBooleanToServer(String key, boolean value) {
		if (!player.world.isRemote) {
			return;
		}
		sendDataToServer(() -> {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setBoolean(key, value);
			return tag;
		});
	}

	@Override
	public void sendDataToServer(Supplier<NBTTagCompound> supplyData) {
		if (!player.world.isRemote) {
			return;
		}
		NBTTagCompound data = supplyData.get();
		data.setInteger("containerId", upgradeContainerId);
		PacketHandler.INSTANCE.sendToServer(new SyncContainerClientDataMessage(data));
	}

	public void onInit() {
		//noop by default
	}

	public abstract void handlePacket(NBTTagCompound data);

	public ItemStack getUpgradeStack() {
		return upgradeWrapper.getUpgradeStack();
	}

	public W getUpgradeWrapper() {
		return upgradeWrapper;
	}

	public void setUpgradeWrapper(IUpgradeWrapper updatedUpgradeWrapper) {
		//noinspection unchecked - only used in logic that makes sure the item is the same and the same item will have a wrapper with the same (W) class
		upgradeWrapper = (W) updatedUpgradeWrapper;
	}

	/** What shift click moves out of the slot. */
	public ItemStack getSlotStackToTransfer(Slot slot) {
		return slot.getStack();
	}

	/** Whether shift click moves the slot's stack into the storage before the player's inventory. */
	public boolean mergeIntoStorageFirst(Slot slot) {
		return true;
	}

	/** Whether a double click gathering a stack may take from the slot. */
	public boolean allowsPickupAll(Slot slot) {
		return true;
	}

	public boolean containsSlot(Slot slot) {
		return slots.contains(slot);
	}

	public int getUpgradeContainerId() {
		return upgradeContainerId;
	}
}
