package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import net.minecraft.util.ResourceLocation;

public final class StorageBackgroundProperties {
	public static final StorageBackgroundProperties REGULAR_9_SLOT = new StorageBackgroundProperties(9, 0,
			DeconstructedCore.getRL("textures/gui/storage_background_9.png"));
	public static final StorageBackgroundProperties WIDER_9_SLOT = new StorageBackgroundProperties(9, 3,
			DeconstructedCore.getRL("textures/gui/storage_background_9_wider.png"));
	public static final StorageBackgroundProperties REGULAR_12_SLOT = new StorageBackgroundProperties(12, 27,
			DeconstructedCore.getRL("textures/gui/storage_background_12.png"));
	public static final StorageBackgroundProperties WIDER_12_SLOT = new StorageBackgroundProperties(12, 30,
			DeconstructedCore.getRL("textures/gui/storage_background_12_wider.png"));

	private final int slotsOnLine;
	private final int playerInventoryXOffset;
	private final ResourceLocation textureName;

	public StorageBackgroundProperties(int slotsOnLine, int playerInventoryXOffset, ResourceLocation textureName) {
		this.slotsOnLine = slotsOnLine;
		this.playerInventoryXOffset = playerInventoryXOffset;
		this.textureName = textureName;
	}

	public int getSlotsOnLine() {
		return slotsOnLine;
	}

	public int getPlayerInventoryXOffset() {
		return playerInventoryXOffset;
	}

	public ResourceLocation getTextureName() {
		return textureName;
	}
}
