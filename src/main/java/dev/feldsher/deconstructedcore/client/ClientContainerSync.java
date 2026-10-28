package dev.feldsher.deconstructedcore.client;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.common.gui.SettingsContainerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.List;
import java.util.function.Consumer;

/** Puts what the storage containers' own slot packets carry into the client's open container. */
public final class ClientContainerSync {
	private ClientContainerSync() {}

	public static void setSlot(int windowId, int slotNumber, ItemStack stack) {
		EntityPlayer player = Minecraft.getMinecraft().player;
		if (player != null && player.openContainer.windowId == windowId && slotNumber >= 0 && slotNumber < player.openContainer.inventorySlots.size()) {
			player.openContainer.putStackInSlot(slotNumber, stack);
		}
	}

	public static void setSettings(int windowId, NBTTagCompound settings) {
		EntityPlayer player = Minecraft.getMinecraft().player;
		if (player != null && player.openContainer.windowId == windowId && player.openContainer instanceof SettingsContainerMenu) {
			IStorageWrapper storageWrapper = ((SettingsContainerMenu<?>) player.openContainer).getStorageWrapper();
			storageWrapper.getSettingsHandler().reloadSettings(settings);
			// the parts that go by what slots remember, as compression, work them out again as the server did
			storageWrapper.getInventoryHandler().onSlotFiltersChanged();
		}
	}

	/** Into the client's copies, and the open settings screen lists them anew. */
	public static void setTemplateSettings(Consumer<EntityPlayer> applyTemplates) {
		EntityPlayer player = Minecraft.getMinecraft().player;
		if (player == null) {
			return;
		}
		applyTemplates.accept(player);
		if (player.openContainer instanceof SettingsContainerMenu) {
			((SettingsContainerMenu<?>) player.openContainer).refreshTemplateSlots();
		}
	}

	public static void setAll(int windowId, List<ItemStack> stacks) {
		EntityPlayer player = Minecraft.getMinecraft().player;
		if (player != null && player.openContainer.windowId == windowId && stacks.size() == player.openContainer.inventorySlots.size()) {
			player.openContainer.setAll(stacks);
		}
	}
}
