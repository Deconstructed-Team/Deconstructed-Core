package dev.feldsher.deconstructedcore.network;

import dev.feldsher.deconstructedcore.api.IStashStorageItem;
import dev.feldsher.deconstructedcore.common.gui.StorageContainerMenuBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.SlotItemHandler;

/**
 * A right click in an inventory screen that stashes: with a storage item held, the clicked slot's items go into it;
 * with an item held, it goes into the storage item in the slot. Done on the server as 1.21's stacked-on hooks do it.
 */
public class StashMessage implements IMessage {
	private int windowId;
	private int slotNumber;

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public StashMessage() {
	}

	public StashMessage(int windowId, int slotNumber) {
		this.windowId = windowId;
		this.slotNumber = slotNumber;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		windowId = buf.readInt();
		slotNumber = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(windowId);
		buf.writeInt(slotNumber);
	}

	/**
	 * Only slots that are what they look like: the player's own, a plain slot or item handler slot, and those of this
	 * mod's screens. Stashing works on the slot itself, past the container's click handling, and a mod's own kind of
	 * slot, as a ghost or a shop's one, often counts on that handling to keep its items where they are.
	 */
	public static boolean canStashIn(Container container, Slot slot) {
		return slot.inventory instanceof InventoryPlayer || container instanceof StorageContainerMenuBase
				|| slot.getClass() == Slot.class || slot.getClass() == SlotItemHandler.class;
	}

	public static class Handler implements IMessageHandler<StashMessage, IMessage> {
		@Override
		public IMessage onMessage(StashMessage message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServerWorld().addScheduledTask(() -> handle(player, message));
			return null;
		}

		private static void handle(EntityPlayerMP player, StashMessage message) {
			Container container = player.openContainer;
			if (container.windowId != message.windowId || message.slotNumber < 0 || message.slotNumber >= container.inventorySlots.size()
					|| container instanceof StorageContainerMenuBase && ((StorageContainerMenuBase<?>) container).refuseIfOutdated()) {
				return;
			}
			Slot slot = container.getSlot(message.slotNumber);
			if (!canStashIn(container, slot)) {
				return;
			}
			ItemStack held = player.inventory.getItemStack();
			if (!held.isEmpty() && held.getCount() == 1 && held.getItem() instanceof IStashStorageItem) {
				stashSlotIntoHeld(player, slot, held, (IStashStorageItem) held.getItem());
			} else if (!held.isEmpty() && slot.getStack().getCount() == 1 && slot.getStack().getItem() instanceof IStashStorageItem) {
				stashHeldIntoSlot(player, slot, held, (IStashStorageItem) slot.getStack().getItem());
			}
			container.detectAndSendChanges();
			player.updateHeldItem();
		}

		/** Taken in turns, for a slot that gives no more than a stack at a time, or one with more than a stack. */
		private static void stashSlotIntoHeld(EntityPlayerMP player, Slot slot, ItemStack storageStack, IStashStorageItem storageItem) {
			ItemStack stackToStash = slot.getStack();
			if (stackToStash.isEmpty() || !slot.canTakeStack(player)) {
				return;
			}
			ItemStack remainder = storageItem.stash(storageStack, stackToStash.copy(), true);
			int countToTake = stackToStash.getCount() - remainder.getCount();
			boolean stashed = false;
			while (countToTake > 0 && slot.getHasStack() && slot.canTakeStack(player)) {
				ItemStack taken = slot.decrStackSize(countToTake);
				if (taken.isEmpty()) {
					break;
				}
				taken = slot.onTake(player, taken);
				countToTake -= taken.getCount();
				ItemStack notStashed = storageItem.stash(storageStack, taken, false);
				stashed = true;
				if (!notStashed.isEmpty()) {
					// It said it fits; what didn't goes to the player
					if (!player.inventory.addItemStackToInventory(notStashed)) {
						player.dropItem(notStashed, false);
					}
					break;
				}
			}
			if (stashed) {
				storageItem.onStashed(player, storageStack);
			}
		}

		private static void stashHeldIntoSlot(EntityPlayerMP player, Slot slot, ItemStack held, IStashStorageItem storageItem) {
			ItemStack storageStack = slot.getStack();
			if (!slot.canTakeStack(player) || !slot.isItemValid(storageStack)) {
				return;
			}
			ItemStack remainder = storageItem.stash(storageStack, held, false);
			if (remainder.getCount() != held.getCount()) {
				player.inventory.setItemStack(remainder);
				slot.putStack(storageStack);
				storageItem.onStashed(player, storageStack);
			}
		}
	}
}
