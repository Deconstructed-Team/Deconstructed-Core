package dev.feldsher.deconstructedcore.network;

import dev.feldsher.deconstructedcore.DeconstructedCore;
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
	}
}
