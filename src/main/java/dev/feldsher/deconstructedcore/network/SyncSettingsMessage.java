package dev.feldsher.deconstructedcore.network;

import dev.feldsher.deconstructedcore.client.ClientContainerSync;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** A storage's settings, as the server has them, for the open settings screen. */
public class SyncSettingsMessage implements IMessage {
	private int windowId;
	private NBTTagCompound settings = new NBTTagCompound();

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public SyncSettingsMessage() {
	}

	public SyncSettingsMessage(int windowId, NBTTagCompound settings) {
		this.windowId = windowId;
		this.settings = settings;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		windowId = buf.readInt();
		NBTTagCompound tag = ByteBufUtils.readTag(buf);
		settings = tag == null ? new NBTTagCompound() : tag;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(windowId);
		ByteBufUtils.writeTag(buf, settings);
	}

	public static class Handler implements IMessageHandler<SyncSettingsMessage, IMessage> {
		@Override
		public IMessage onMessage(SyncSettingsMessage message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> ClientContainerSync.setSettings(message.windowId, message.settings));
			return null;
		}
	}
}
