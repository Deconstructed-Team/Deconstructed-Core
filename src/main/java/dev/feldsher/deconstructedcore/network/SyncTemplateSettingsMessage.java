package dev.feldsher.deconstructedcore.network;

import dev.feldsher.deconstructedcore.client.ClientContainerSync;
import dev.feldsher.deconstructedcore.settings.FileSettingsTemplates;
import dev.feldsher.deconstructedcore.settings.SettingsTemplateStorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/** The player's settings templates and the ones from files, for the client's copies, as a settings screen opens. */
public class SyncTemplateSettingsMessage implements IMessage {
	private static final String PLAYER_TEMPLATES_TAG = "playerTemplates";
	private static final String PLAYER_NAMED_TEMPLATES_TAG = "playerNamedTemplates";
	private static final String FILE_TEMPLATES_TAG = "fileTemplates";
	private NBTTagCompound data = new NBTTagCompound();

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public SyncTemplateSettingsMessage() {
	}

	public SyncTemplateSettingsMessage(Map<Integer, NBTTagCompound> playerTemplates, Map<String, NBTTagCompound> playerNamedTemplates,
			Map<String, Map<String, NBTTagCompound>> fileTemplates) {
		NBTTagCompound playerTemplatesTag = new NBTTagCompound();
		playerTemplates.forEach((slot, settingsTag) -> playerTemplatesTag.setTag(String.valueOf(slot), settingsTag));
		data.setTag(PLAYER_TEMPLATES_TAG, playerTemplatesTag);
		NBTTagCompound playerNamedTemplatesTag = new NBTTagCompound();
		playerNamedTemplates.forEach(playerNamedTemplatesTag::setTag);
		data.setTag(PLAYER_NAMED_TEMPLATES_TAG, playerNamedTemplatesTag);
		NBTTagCompound fileTemplatesTag = new NBTTagCompound();
		fileTemplates.forEach((source, templates) -> {
			NBTTagCompound sourceTag = new NBTTagCompound();
			templates.forEach(sourceTag::setTag);
			fileTemplatesTag.setTag(source, sourceTag);
		});
		data.setTag(FILE_TEMPLATES_TAG, fileTemplatesTag);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		NBTTagCompound tag = ByteBufUtils.readTag(buf);
		data = tag == null ? new NBTTagCompound() : tag;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeTag(buf, data);
	}

	private void applyTo(EntityPlayer player) {
		SettingsTemplateStorage settingsTemplateStorage = SettingsTemplateStorage.get(player.world);
		settingsTemplateStorage.clearPlayerTemplates(player);
		NBTTagCompound playerTemplatesTag = data.getCompoundTag(PLAYER_TEMPLATES_TAG);
		for (String slot : playerTemplatesTag.getKeySet()) {
			settingsTemplateStorage.putPlayerTemplate(player, Integer.parseInt(slot), playerTemplatesTag.getCompoundTag(slot));
		}
		NBTTagCompound playerNamedTemplatesTag = data.getCompoundTag(PLAYER_NAMED_TEMPLATES_TAG);
		for (String name : playerNamedTemplatesTag.getKeySet()) {
			settingsTemplateStorage.putPlayerNamedTemplate(player, name, playerNamedTemplatesTag.getCompoundTag(name));
		}
		Map<String, Map<String, NBTTagCompound>> fileTemplates = new HashMap<>();
		NBTTagCompound fileTemplatesTag = data.getCompoundTag(FILE_TEMPLATES_TAG);
		for (String source : fileTemplatesTag.getKeySet()) {
			Map<String, NBTTagCompound> templates = new TreeMap<>();
			NBTTagCompound sourceTag = fileTemplatesTag.getCompoundTag(source);
			for (String name : sourceTag.getKeySet()) {
				templates.put(name, sourceTag.getCompoundTag(name));
			}
			fileTemplates.put(source, templates);
		}
		FileSettingsTemplates.get(true).setAll(fileTemplates);
	}

	public static class Handler implements IMessageHandler<SyncTemplateSettingsMessage, IMessage> {
		@Override
		public IMessage onMessage(SyncTemplateSettingsMessage message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> ClientContainerSync.setTemplateSettings(message::applyTo));
			return null;
		}
	}
}
