package dev.feldsher.deconstructedcore.client.gui;

import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.common.gui.StorageBackgroundProperties;
import dev.feldsher.deconstructedcore.common.gui.StorageContainerMenuBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;

/**
 * A storage's screen: as wide as 9 or 12 slots a line, as tall as its rows.
 * <p>
 * Upgrade slots and tabs, settings, sort buttons, the search box and the scroll panel for storages taller than the
 * screen come with the stages that port them.
 */
public abstract class StorageScreenBase<S extends StorageContainerMenuBase<?>> extends GuiContainer {
	public static final int DISABLED_SLOT_X_POS = -2000;
	static final int SLOTS_Y_OFFSET = 17;
	static final int SLOTS_X_OFFSET = 7;
	public static final int HEIGHT_WITHOUT_STORAGE_SLOTS = 114;

	protected final S menu;
	private final String title;
	private StorageBackgroundProperties storageBackgroundProperties = StorageBackgroundProperties.REGULAR_9_SLOT;
	private int visibleSlotsCount;
	protected int inventoryLabelX = 8;
	protected int inventoryLabelY;

	protected StorageScreenBase(S menu, String title) {
		super(menu);
		this.menu = menu;
		this.title = title;
		visibleSlotsCount = menu.getNumberOfStorageInventorySlots();
		updateDimensionsAndSlotPositions();
	}

	public S getMenu() {
		return menu;
	}

	private void updateDimensionsAndSlotPositions() {
		int displayableNumberOfRows = getMenu().getNumberOfRows();
		storageBackgroundProperties = (getMenu().getNumberOfStorageInventorySlots() + getMenu().getColumnsTaken() * getMenu().getNumberOfRows()) <= 81 ? StorageBackgroundProperties.REGULAR_9_SLOT : StorageBackgroundProperties.REGULAR_12_SLOT;

		xSize = storageBackgroundProperties.getSlotsOnLine() * 18 + 14;
		updateStorageSlotsPositions();
		ySize = HEIGHT_WITHOUT_STORAGE_SLOTS + getStorageInventoryHeight(displayableNumberOfRows);
		inventoryLabelY = ySize - 94;
		inventoryLabelX = 8 + storageBackgroundProperties.getPlayerInventoryXOffset();
		updatePlayerSlotsPositions();
	}

	protected int getStorageInventoryHeight(int displayableNumberOfRows) {
		return displayableNumberOfRows * 18;
	}

	protected int getSlotsOnLine() {
		return storageBackgroundProperties.getSlotsOnLine() - getMenu().getColumnsTaken();
	}

	protected void updateStorageSlotsPositions() {
		int yPosition = 18;

		visibleSlotsCount = 0;
		for (int slotIndex = 0; slotIndex < getMenu().getNumberOfStorageInventorySlots(); slotIndex++) {
			Slot slot = getMenu().getSlot(slotIndex);
			int lineIndex = visibleSlotsCount % getSlotsOnLine();
			slot.xPos = 8 + lineIndex * 18;
			slot.yPos = yPosition;
			visibleSlotsCount++;
			if (visibleSlotsCount % getSlotsOnLine() == 0) {
				yPosition += 18;
			}
		}
	}

	protected void updatePlayerSlotsPositions() {
		int playerInventoryXOffset = storageBackgroundProperties.getPlayerInventoryXOffset();
		int firstPlayerSlot = getMenu().getNumberOfStorageInventorySlots();

		int yPosition = inventoryLabelY + 12;
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				Slot slot = getMenu().getSlot(firstPlayerSlot + j + i * 9);
				slot.xPos = playerInventoryXOffset + 8 + j * 18;
				slot.yPos = yPosition;
			}
			yPosition += 18;
		}

		yPosition += 4;
		for (int slotIndex = 0; slotIndex < 9; ++slotIndex) {
			Slot slot = getMenu().getSlot(firstPlayerSlot + 27 + slotIndex);
			slot.xPos = playerInventoryXOffset + 8 + slotIndex * 18;
			slot.yPos = yPosition;
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		int x = (width - xSize) / 2;
		int y = (height - ySize) / 2;
		StorageGuiHelper.renderStorageBackground(x, y, storageBackgroundProperties.getTextureName(), xSize, ySize - HEIGHT_WITHOUT_STORAGE_SLOTS);
		drawSlotBg(x, y, visibleSlotsCount);
	}

	protected void drawSlotBg(int x, int y, int visibleSlotsCount) {
		int slotsOnLine = getSlotsOnLine();
		int slotRows = visibleSlotsCount / slotsOnLine;
		int remainingSlots = visibleSlotsCount % slotsOnLine;
		GuiHelper.renderSlotsBackground(x + SLOTS_X_OFFSET, y + SLOTS_Y_OFFSET, slotsOnLine, slotRows, remainingSlots);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		fontRenderer.drawString(title, 8, 6, 0x404040);
		fontRenderer.drawString(I18n.format("container.inventory"), inventoryLabelX, inventoryLabelY, 0x404040);
	}

	@Override
	public void setWorldAndResolution(Minecraft mc, int width, int height) {
		updateDimensionsAndSlotPositions();
		super.setWorldAndResolution(mc, width, height);
	}
}
