package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.network.PacketHandler;
import dev.feldsher.deconstructedcore.network.SyncContainerStacksMessage;
import dev.feldsher.deconstructedcore.network.SyncSlotStackMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;

import java.util.ArrayList;
import java.util.List;

/**
 * A container whose slots may hold more than vanilla's packets can carry, which carry counts in a byte: players get
 * its slots in packets of its own, with int counts.
 */
public abstract class LargeStackSyncContainer extends Container {
	private boolean needsFullSync = false;

	/** Sends all the slots again on the next change detection, as when the client may have done what the server didn't. */
	protected void requestFullSync() {
		needsFullSync = true;
	}

	@Override
	public void addListener(IContainerListener listener) {
		if (!(listener instanceof EntityPlayerMP)) {
			super.addListener(listener);
			return;
		}
		if (listeners.contains(listener)) {
			throw new IllegalArgumentException("Listener already listening");
		}
		listeners.add(listener);
		sendAllStacks((EntityPlayerMP) listener);
		detectAndSendChanges();
	}

	/** Vanilla's, but players get the slots in this container's own packet. */
	@Override
	public void detectAndSendChanges() {
		if (needsFullSync) {
			needsFullSync = false;
			for (IContainerListener listener : listeners) {
				if (listener instanceof EntityPlayerMP) {
					sendAllStacks((EntityPlayerMP) listener);
				}
			}
		}

		for (int i = 0; i < inventorySlots.size(); ++i) {
			ItemStack stack = inventorySlots.get(i).getStack();
			ItemStack lastStack = inventoryItemStacks.get(i);
			if (!ItemStack.areItemStacksEqual(lastStack, stack)) {
				boolean clientStackChanged = !ItemStack.areItemStacksEqualUsingNBTShareTag(lastStack, stack);
				lastStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
				inventoryItemStacks.set(i, lastStack);
				if (clientStackChanged) {
					sendSlotStack(i, lastStack);
				}
			}
		}
	}

	private void sendSlotStack(int slotNumber, ItemStack stack) {
		for (IContainerListener listener : listeners) {
			if (listener instanceof EntityPlayerMP) {
				EntityPlayerMP playerListener = (EntityPlayerMP) listener;
				// as vanilla: the client already did what the click it just sent did
				if (!playerListener.isChangingQuantityOnly) {
					PacketHandler.INSTANCE.sendTo(new SyncSlotStackMessage(windowId, slotNumber, stack), playerListener);
				}
			} else {
				listener.sendSlotContents(this, slotNumber, stack);
			}
		}
	}

	private void sendAllStacks(EntityPlayerMP playerListener) {
		List<ItemStack> stacks = new ArrayList<>(inventorySlots.size());
		for (Slot slot : inventorySlots) {
			stacks.add(slot.getStack().copy());
		}
		PacketHandler.INSTANCE.sendTo(new SyncContainerStacksMessage(windowId, stacks), playerListener);
		playerListener.connection.sendPacket(new SPacketSetSlot(-1, -1, playerListener.inventory.getItemStack()));
	}

	/**
	 * Set to false when a click the client sent didn't do the same on the server. The server then sends all the slots in
	 * vanilla's packet, which wraps oversized counts: they're sent again, right, on the next change detection.
	 */
	@Override
	public void setCanCraft(EntityPlayer player, boolean canCraft) {
		super.setCanCraft(player, canCraft);
		if (!canCraft && !player.world.isRemote) {
			requestFullSync();
		}
	}
}
