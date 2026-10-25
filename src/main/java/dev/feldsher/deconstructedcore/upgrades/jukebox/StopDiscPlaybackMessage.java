package dev.feldsher.deconstructedcore.upgrades.jukebox;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

public class StopDiscPlaybackMessage implements IMessage {
	private UUID storageUuid;

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public StopDiscPlaybackMessage() {
	}

	public StopDiscPlaybackMessage(UUID storageUuid) {
		this.storageUuid = storageUuid;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		storageUuid = new UUID(buf.readLong(), buf.readLong());
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(storageUuid.getMostSignificantBits());
		buf.writeLong(storageUuid.getLeastSignificantBits());
	}

	public static class Handler implements IMessageHandler<StopDiscPlaybackMessage, IMessage> {
		@Override
		public IMessage onMessage(StopDiscPlaybackMessage message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> StorageSoundHandler.stopStorageSound(message.storageUuid));
			return null;
		}
	}
}
