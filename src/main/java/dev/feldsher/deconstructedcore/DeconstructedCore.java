package dev.feldsher.deconstructedcore;

import dev.feldsher.deconstructedcore.fluid.ExperienceFluids;
import dev.feldsher.deconstructedcore.network.PacketHandler;
import dev.feldsher.deconstructedcore.settings.FileSettingsTemplates;
import dev.feldsher.deconstructedcore.util.RecipeHelper;
import net.minecraft.util.ResourceLocation;
import dev.feldsher.deconstructedcore.upgrades.jukebox.DiscLengths;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = DeconstructedCore.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class DeconstructedCore {
	public static final String MOD_ID = Tags.MOD_ID;
	public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

	static {
		// Before pre-init, as Forge asks: it holds the experience fluid, which has no bucket of its own
		FluidRegistry.enableUniversalBucket();
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		LOGGER.info("{} {} loading", Tags.MOD_NAME, Tags.VERSION);
		PacketHandler.init();
		ExperienceFluids.register();
	}

	@Mod.EventHandler
	public void onImc(FMLInterModComms.IMCEvent event) {
		for (FMLInterModComms.IMCMessage message : event.getMessages()) {
			if (message.key.equals(DiscLengths.IMC_KEY) && message.isNBTMessage()) {
				DiscLengths.registerFromImc(message.getSender(), message.getNBTValue());
			}
		}
	}

	@Mod.EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		FileSettingsTemplates.loadServerTemplates();
		RecipeHelper.clearCache();
	}

	public static ResourceLocation getRL(String regName) {
		return new ResourceLocation(MOD_ID, regName);
	}
}
