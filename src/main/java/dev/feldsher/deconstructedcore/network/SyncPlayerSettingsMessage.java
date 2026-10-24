package dev.feldsher.deconstructedcore.network;

import dev.feldsher.deconstructedcore.client.ClientPlayerSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** A player's settings of one storage mod, for the client's player, which the server keeps them for. */
public class SyncPlayerSettingsMessage implements IMessage {
	private String playerSettingsTagName = "";
	private NBTTagCompound settings = new NBTTagCompound();

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public SyncPlayerSettingsMessage() {
	}

	public SyncPlayerSettingsMessage(String playerSettingsTagName, NBTTagCompound settings) {
		this.playerSettingsTagName = playerSettingsTagName;
		this.settings = settings;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		playerSettingsTagName = ByteBufUtils.readUTF8String(buf);
		NBTTagCompound tag = ByteBufUtils.readTag(buf);
		settings = tag == null ? new NBTTagCompound() : tag;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, playerSettingsTagName);
		ByteBufUtils.writeTag(buf, settings);
	}

	public static class Handler implements IMessageHandler<SyncPlayerSettingsMessage, IMessage> {
		@Override
		public IMessage onMessage(SyncPlayerSettingsMessage message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> ClientPlayerSettings.set(message.playerSettingsTagName, message.settings));
			return null;
		}
	}
}
