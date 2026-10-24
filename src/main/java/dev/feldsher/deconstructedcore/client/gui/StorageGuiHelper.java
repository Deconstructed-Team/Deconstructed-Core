package dev.feldsher.deconstructedcore.client.gui;

import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import net.minecraft.util.ResourceLocation;

public class StorageGuiHelper {

	private StorageGuiHelper() {}

	/**
	 * The storage background: the top with half the slots' height, the middle repeated while the slots are taller than
	 * the texture, and the bottom with the other half and the player's inventory.
	 */
	public static void renderStorageBackground(int x, int y, ResourceLocation textureName, int xSize, int slotsHeight) {
		int slotsTopBottomHeight = Math.min(slotsHeight / 2, 150);
		int yOffset = 0;

		GuiHelper.blit(textureName, x, y, 0, 0, xSize, StorageScreenBase.SLOTS_Y_OFFSET + slotsTopBottomHeight);

		if (slotsHeight / 2 > 150) {
			int middleHeight = (slotsHeight / 2 - 150) * 2;
			GuiHelper.blit(textureName, x, y + StorageScreenBase.SLOTS_Y_OFFSET + slotsTopBottomHeight, 0, StorageScreenBase.SLOTS_Y_OFFSET, xSize, middleHeight);
			yOffset = middleHeight;
		}

		int playerInventoryHeight = 97;
		GuiHelper.blit(textureName, x, y + yOffset + StorageScreenBase.SLOTS_Y_OFFSET + slotsTopBottomHeight, 0, 256 - (playerInventoryHeight + slotsTopBottomHeight), xSize, playerInventoryHeight + slotsTopBottomHeight);
	}
}
