package dev.feldsher.deconstructedcore.network;

import dev.feldsher.deconstructedcore.client.ClientContainerSync;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** A storage container's slot, as vanilla's set slot packet but with counts above what it can carry. */
public class SyncSlotStackMessage implements IMessage {
	private int windowId;
	private int slotNumber;
	private ItemStack stack = ItemStack.EMPTY;

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public SyncSlotStackMessage() {
	}

	public SyncSlotStackMessage(int windowId, int slotNumber, ItemStack stack) {
		this.windowId = windowId;
		this.slotNumber = slotNumber;
		this.stack = stack;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		windowId = buf.readInt();
		slotNumber = buf.readInt();
		stack = StackSyncHelper.readStack(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(windowId);
		buf.writeInt(slotNumber);
		StackSyncHelper.writeStack(buf, stack);
	}

	public static class Handler implements IMessageHandler<SyncSlotStackMessage, IMessage> {
		@Override
		public IMessage onMessage(SyncSlotStackMessage message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> ClientContainerSync.setSlot(message.windowId, message.slotNumber, message.stack));
			return null;
		}
	}
}
