package dev.feldsher.deconstructedcore.client;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.common.gui.StorageContainerMenuBase;
import dev.feldsher.deconstructedcore.upgrades.tank.TankUpgradeContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = DeconstructedCore.MOD_ID, value = Side.CLIENT)
public final class ClientEventHandler {
	private ClientEventHandler() {}

	/** Slot backgrounds are sprites of the blocks atlas in 1.12, as in 1.21. */
	@SubscribeEvent
	public static void onTextureStitch(TextureStitchEvent.Pre event) {
		event.getMap().registerSprite(new ResourceLocation(StorageContainerMenuBase.EMPTY_UPGRADE_SLOT_BACKGROUND));
		event.getMap().registerSprite(new ResourceLocation(TankUpgradeContainer.EMPTY_TANK_INPUT_SLOT_BACKGROUND));
		event.getMap().registerSprite(new ResourceLocation(TankUpgradeContainer.EMPTY_TANK_OUTPUT_SLOT_BACKGROUND));
	}
}
