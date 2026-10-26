package dev.feldsher.deconstructedcore.client;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.api.IStashStorageItem;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.network.PacketHandler;
import dev.feldsher.deconstructedcore.network.StashMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;
import java.util.Collections;

/**
 * Stashing in any inventory screen, modded ones too, as long as it has real slots: a right click with a storage item
 * held onto an item, or with an item held onto a storage item, is caught before the screen sees it and done by the
 * server. Over the slots it can be done with, a sign: + on storage items, - on items, green where the storage has the
 * item or remembers it.
 */
@Mod.EventBusSubscriber(modid = DeconstructedCore.MOD_ID, value = Side.CLIENT)
public final class StashHandler {
	private static final int RIGHT_BUTTON = 1;
	private static final int MATCH_COLOR = 0x55FF55;
	private static final int SPACE_COLOR = 0xFFFF00;
	/** The release of a caught click is caught too, or the screen would take it for a click of its own. */
	private static boolean catchRelease = false;

	private StashHandler() {}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
		if (Mouse.getEventButton() != RIGHT_BUTTON) {
			return;
		}
		if (!Mouse.getEventButtonState()) {
			if (catchRelease) {
				catchRelease = false;
				event.setCanceled(true);
			}
			return;
		}
		if (!(event.getGui() instanceof GuiContainer) || event.getGui() instanceof GuiContainerCreative) {
			return;
		}
		GuiContainer screen = (GuiContainer) event.getGui();
		Slot slot = screen.getSlotUnderMouse();
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if (slot == null || player == null || getStashResult(slot, player.inventory.getItemStack()) == null) {
			return;
		}
		PacketHandler.INSTANCE.sendToServer(new StashMessage(screen.inventorySlots.windowId, slot.slotNumber));
		catchRelease = true;
		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
		if (!(event.getGui() instanceof GuiContainer) || event.getGui() instanceof GuiContainerCreative) {
			return;
		}
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		ItemStack held = player == null ? ItemStack.EMPTY : player.inventory.getItemStack();
		if (held.isEmpty()) {
			return;
		}
		GuiContainer screen = (GuiContainer) event.getGui();
		Slot under = screen.getSlotUnderMouse();
		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 0, 300);
		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		for (Slot slot : screen.inventorySlots.inventorySlots) {
			if (slot == under || !slot.isEnabled()) {
				continue;
			}
			IStashStorageItem.StashResult result = getStashResult(slot, held);
			if (result != null) {
				renderSign(screen, slot, result);
			}
		}
		GlStateManager.enableDepth();
		GlStateManager.popMatrix();
		if (under != null && getStashResult(under, held) != null) {
			screen.drawHoveringText(Collections.singletonList(I18n.format(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".right_click_to_add_to_storage")),
					event.getMouseX(), event.getMouseY());
		}
	}

	private static void renderSign(GuiContainer screen, Slot slot, IStashStorageItem.StashResult result) {
		int x = screen.getGuiLeft() + slot.xPos;
		int y = screen.getGuiTop() + slot.yPos;
		int color = result == IStashStorageItem.StashResult.MATCH_AND_SPACE ? MATCH_COLOR : SPACE_COLOR;
		if (slot.getStack().getItem() instanceof IStashStorageItem) {
			screen.mc.fontRenderer.drawStringWithShadow("+", x + 10, y + 8, color);
		} else {
			screen.mc.fontRenderer.drawStringWithShadow("-", x + 1, y, color);
		}
	}

	/**
	 * How the slot's stack and the held one can be stashed, the one into the other; null when they can't: as on the
	 * server, a slot's items are taken only from a slot that gives them, a storage item only from one that would take it back.
	 */
	@Nullable
	private static IStashStorageItem.StashResult getStashResult(Slot slot, ItemStack held) {
		ItemStack inSlot = slot.getStack();
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if (held.isEmpty() || inSlot.isEmpty() || !slot.canTakeStack(player)) {
			return null;
		}
		IStashStorageItem.StashResult result = null;
		if (held.getCount() == 1 && held.getItem() instanceof IStashStorageItem) {
			result = ((IStashStorageItem) held.getItem()).getItemStashable(held, inSlot);
		} else if (inSlot.getCount() == 1 && inSlot.getItem() instanceof IStashStorageItem && slot.isItemValid(inSlot)) {
			result = ((IStashStorageItem) inSlot.getItem()).getItemStashable(inSlot, held);
		}
		return result == IStashStorageItem.StashResult.NO_SPACE ? null : result;
	}
}
