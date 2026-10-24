package dev.feldsher.deconstructedcore.network;

import dev.feldsher.deconstructedcore.client.ClientContainerSync;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/** All of a storage container's slots, as vanilla's window items packet but with counts above what it can carry. */
public class SyncContainerStacksMessage implements IMessage {
	private int windowId;
	private List<ItemStack> stacks = new ArrayList<>();

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public SyncContainerStacksMessage() {
	}

	public SyncContainerStacksMessage(int windowId, List<ItemStack> stacks) {
		this.windowId = windowId;
		this.stacks = stacks;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		windowId = buf.readInt();
		int size = buf.readInt();
		stacks = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			stacks.add(StackSyncHelper.readStack(buf));
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(windowId);
		buf.writeInt(stacks.size());
		for (ItemStack stack : stacks) {
			StackSyncHelper.writeStack(buf, stack);
		}
	}

	public static class Handler implements IMessageHandler<SyncContainerStacksMessage, IMessage> {
		@Override
		public IMessage onMessage(SyncContainerStacksMessage message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> ClientContainerSync.setAll(message.windowId, message.stacks));
			return null;
		}
	}
}
