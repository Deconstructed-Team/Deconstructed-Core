package dev.feldsher.deconstructedcore.network;

import dev.feldsher.deconstructedcore.common.gui.ISyncedContainer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

/** What a storage screen tells its container on the server: a button pressed, a tab opened. */
public class SyncContainerClientDataMessage implements IMessage {
	@Nullable
	private NBTTagCompound data;

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public SyncContainerClientDataMessage() {
		this(null);
	}

	public SyncContainerClientDataMessage(@Nullable NBTTagCompound data) {
		this.data = data;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		data = ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeTag(buf, data);
	}

	public static class Handler implements IMessageHandler<SyncContainerClientDataMessage, IMessage> {
		@Override
		public IMessage onMessage(SyncContainerClientDataMessage message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			NBTTagCompound data = message.data;
			if (data == null) {
				return null;
			}
			player.getServerWorld().addScheduledTask(() -> {
				if (player.openContainer instanceof ISyncedContainer) {
					((ISyncedContainer) player.openContainer).handlePacket(data);
				}
			});
			return null;
		}
	}
}
