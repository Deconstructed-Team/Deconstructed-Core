package dev.feldsher.deconstructedcore.upgrades.jukebox;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

/** A client's disc of a storage finished playing. */
public class SoundFinishedNotificationMessage implements IMessage {
	private UUID storageUuid;

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public SoundFinishedNotificationMessage() {
	}

	public SoundFinishedNotificationMessage(UUID storageUuid) {
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

	public static class Handler implements IMessageHandler<SoundFinishedNotificationMessage, IMessage> {
		@Override
		public IMessage onMessage(SoundFinishedNotificationMessage message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServerWorld().addScheduledTask(() -> ServerStorageSoundHandler.onSoundFinished(player.world, message.storageUuid));
			return null;
		}
	}
}
