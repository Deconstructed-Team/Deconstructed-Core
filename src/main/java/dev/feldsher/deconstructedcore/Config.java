package dev.feldsher.deconstructedcore;

import dev.feldsher.deconstructedcore.upgrades.jukebox.DiscLengths;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@net.minecraftforge.common.config.Config(modid = DeconstructedCore.MOD_ID)
@Mod.EventBusSubscriber(modid = DeconstructedCore.MOD_ID)
public class Config {
	@net.minecraftforge.common.config.Config.Comment({"How long music discs play, for the advanced jukebox upgrade's progress, as 1.12's discs don't say,",
			"in format of \"ItemRegistryName|Seconds\". Vanilla's are known already; these are used before what mods register."})
	public static String[] discLengths = {};

	private Config() {}

	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
		if (event.getModID().equals(DeconstructedCore.MOD_ID)) {
			ConfigManager.sync(DeconstructedCore.MOD_ID, net.minecraftforge.common.config.Config.Type.INSTANCE);
			DiscLengths.clearConfigCache();
		}
	}
}
