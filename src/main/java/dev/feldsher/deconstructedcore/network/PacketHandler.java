package dev.feldsher.deconstructedcore.network;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.upgrades.jukebox.PlayDiscMessage;
import dev.feldsher.deconstructedcore.upgrades.jukebox.SoundFinishedNotificationMessage;
import dev.feldsher.deconstructedcore.upgrades.jukebox.StopDiscPlaybackMessage;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
	public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(DeconstructedCore.MOD_ID);

	private PacketHandler() {}

	public static void init() {
		int id = 0;
		INSTANCE.registerMessage(SyncContainerClientDataMessage.Handler.class, SyncContainerClientDataMessage.class, id++, Side.SERVER);
		INSTANCE.registerMessage(SyncSlotStackMessage.Handler.class, SyncSlotStackMessage.class, id++, Side.CLIENT);
		INSTANCE.registerMessage(SyncContainerStacksMessage.Handler.class, SyncContainerStacksMessage.class, id++, Side.CLIENT);
		INSTANCE.registerMessage(SyncPlayerSettingsMessage.Handler.class, SyncPlayerSettingsMessage.class, id++, Side.CLIENT);
		INSTANCE.registerMessage(SyncSettingsMessage.Handler.class, SyncSettingsMessage.class, id++, Side.CLIENT);
		INSTANCE.registerMessage(SyncTemplateSettingsMessage.Handler.class, SyncTemplateSettingsMessage.class, id++, Side.CLIENT);
		INSTANCE.registerMessage(PlayDiscMessage.Handler.class, PlayDiscMessage.class, id++, Side.CLIENT);
		INSTANCE.registerMessage(StopDiscPlaybackMessage.Handler.class, StopDiscPlaybackMessage.class, id++, Side.CLIENT);
		INSTANCE.registerMessage(SoundFinishedNotificationMessage.Handler.class, SoundFinishedNotificationMessage.class, id++, Side.SERVER);
	}
}
