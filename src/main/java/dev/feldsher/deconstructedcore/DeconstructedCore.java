package dev.feldsher.deconstructedcore;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = DeconstructedCore.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class DeconstructedCore {
	public static final String MOD_ID = Tags.MOD_ID;
	public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		LOGGER.info("{} {} loading", Tags.MOD_NAME, Tags.VERSION);
	}

	public static ResourceLocation getRL(String regName) {
		return new ResourceLocation(MOD_ID, regName);
	}
}
