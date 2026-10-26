package dev.feldsher.deconstructedcore.client;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;

import java.io.IOException;

/**
 * Typing in a storage screen's text box, as the anvil upgrade's name: the keys go to the box before anything else sees
 * them, so that a recipe viewer's shortcuts don't fire meanwhile. 1.12 HEI looks only for vanilla text fields. A click
 * anywhere, its overlay's too, takes the focus from the box first, for its own search field to get the keys.
 */
@Mod.EventBusSubscriber(modid = DeconstructedCore.MOD_ID, value = Side.CLIENT)
public final class TextInputHandler {
	private TextInputHandler() {}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
		GuiScreen gui = event.getGui();
		if (!(gui instanceof StorageScreenBase) || !Mouse.getEventButtonState() || Mouse.getEventButton() < 0) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		int mouseX = Mouse.getEventX() * gui.width / mc.displayWidth;
		int mouseY = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;
		((StorageScreenBase<?>) gui).onScreenClicked(mouseX, mouseY, Mouse.getEventButton());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
		GuiScreen gui = event.getGui();
		if (!(gui instanceof StorageScreenBase) || !((StorageScreenBase<?>) gui).isTyping()) {
			return;
		}
		try {
			gui.handleKeyboardInput();
		} catch (IOException e) {
			DeconstructedCore.LOGGER.error("Failed to handle a key typed in a storage screen", e);
		}
		event.setCanceled(true);
	}
}
