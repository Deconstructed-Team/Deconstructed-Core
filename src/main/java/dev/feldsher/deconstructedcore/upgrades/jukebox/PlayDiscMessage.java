package dev.feldsher.deconstructedcore.upgrades.jukebox;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

/** A storage's disc to play: where a placed storage is, or following the entity that carries it. */
public class PlayDiscMessage implements IMessage {
	private boolean blockStorage;
	private UUID storageUuid;
	private ItemStack disc = ItemStack.EMPTY;
	private int entityId;
	private BlockPos pos = BlockPos.ORIGIN;

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public PlayDiscMessage() {
	}

	public PlayDiscMessage(UUID storageUuid, ItemStack disc, BlockPos pos) {
		blockStorage = true;
		this.storageUuid = storageUuid;
		this.disc = disc;
		this.pos = pos;
	}

	public PlayDiscMessage(UUID storageUuid, ItemStack disc, int entityId) {
		blockStorage = false;
		this.storageUuid = storageUuid;
		this.disc = disc;
		this.entityId = entityId;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		blockStorage = buf.readBoolean();
		storageUuid = new UUID(buf.readLong(), buf.readLong());
		disc = ByteBufUtils.readItemStack(buf);
		entityId = buf.readInt();
		pos = BlockPos.fromLong(buf.readLong());
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(blockStorage);
		buf.writeLong(storageUuid.getMostSignificantBits());
		buf.writeLong(storageUuid.getLeastSignificantBits());
		ByteBufUtils.writeItemStack(buf, disc);
		buf.writeInt(entityId);
		buf.writeLong(pos.toLong());
	}

	public static class Handler implements IMessageHandler<PlayDiscMessage, IMessage> {
		@Override
		public IMessage onMessage(PlayDiscMessage message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
				if (message.blockStorage) {
					StorageSoundHandler.playStorageSound(message.disc, message.storageUuid, message.pos);
				} else {
					StorageSoundHandler.playStorageSound(message.disc, message.storageUuid, message.entityId);
				}
			});
			return null;
		}
	}
}
