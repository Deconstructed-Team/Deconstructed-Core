package dev.feldsher.deconstructedcore.client.gui;

import net.minecraft.init.Items;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.Rect2i;
import dev.feldsher.deconstructedcore.common.gui.SettingsContainerMenu;
import dev.feldsher.deconstructedcore.common.gui.StorageBackgroundProperties;
import dev.feldsher.deconstructedcore.settings.StorageSettingsTabControlBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A storage's settings screen: its slots, to pick for the open settings tab, the tabs on the right and the settings
 * template buttons on the left. Rows that don't fit scroll with the mouse wheel.
 */
public abstract class SettingsScreen extends GuiContainer {
	public static final int HEIGHT_WITHOUT_STORAGE_SLOTS = 114;
	private static final int SLOTS_X_OFFSET = 7;
	private static final int SLOTS_Y_OFFSET = 17;

	private final SettingsContainerMenu<?> menu;
	private final String title;
	@Nullable
	private StorageSettingsTabControlBase settingsTabControl = null;
	@Nullable
	private TemplatePersistanceControl templatePersistanceControl = null;
	private StorageBackgroundProperties storageBackgroundProperties = StorageBackgroundProperties.REGULAR_9_SLOT;
	private int numberOfVisibleRows;
	private int firstVisibleRow = 0;
	private int visibleSlotsCount;
	private int inventoryLabelX = 8;
	private int inventoryLabelY;

	protected SettingsScreen(SettingsContainerMenu<?> menu, String title) {
		super(menu);
		this.menu = menu;
		this.title = title;
	}

	public SettingsContainerMenu<?> getMenu() {
		return menu;
	}

	public Optional<StorageSettingsTabControlBase> getSettingsTabControl() {
		return Optional.ofNullable(settingsTabControl);
	}

	public int getGuiLeft() {
		return guiLeft;
	}

	public int getGuiTop() {
		return guiTop;
	}

	@Override
	public void setWorldAndResolution(Minecraft mc, int width, int height) {
		updateDimensionsAndSlotPositions(height);
		super.setWorldAndResolution(mc, width, height);
	}

	@Override
	public void initGui() {
		super.initGui();
		settingsTabControl = initializeTabControl();
		templatePersistanceControl = new TemplatePersistanceControl(new Position(guiLeft + inventoryLabelX - 29, guiTop + inventoryLabelY + 29),
				menu.getTemplatePersistanceContainer());
	}

	private boolean isTemplateLoadHovered() {
		return templatePersistanceControl != null && templatePersistanceControl.isTemplateLoadHovered();
	}

	protected abstract StorageSettingsTabControlBase initializeTabControl();

	/** Back to the storage's own screen, as escape and the inventory key do. */
	protected abstract void sendStorageInventoryScreenOpenMessage();

	private void updateDimensionsAndSlotPositions(int height) {
		numberOfVisibleRows = Math.max(1, Math.min((height - HEIGHT_WITHOUT_STORAGE_SLOTS) / 18, menu.getNumberOfRows()));
		storageBackgroundProperties = (menu.getNumberOfStorageInventorySlots() + menu.getColumnsTaken() * menu.getNumberOfRows()) <= 81
				? StorageBackgroundProperties.REGULAR_9_SLOT : StorageBackgroundProperties.REGULAR_12_SLOT;
		xSize = storageBackgroundProperties.getSlotsOnLine() * 18 + 14;
		firstVisibleRow = Math.min(firstVisibleRow, getMaxFirstVisibleRow());
		updateStorageSlotsPositions();
		ySize = HEIGHT_WITHOUT_STORAGE_SLOTS + numberOfVisibleRows * 18;
		inventoryLabelY = ySize - 94;
		inventoryLabelX = 8 + storageBackgroundProperties.getPlayerInventoryXOffset();
	}

	private int getMaxFirstVisibleRow() {
		return Math.max(0, menu.getNumberOfRows() - numberOfVisibleRows);
	}

	private int getSlotsOnLine() {
		return storageBackgroundProperties.getSlotsOnLine() - menu.getColumnsTaken();
	}

	private void updateStorageSlotsPositions() {
		visibleSlotsCount = 0;
		for (int slotIndex = 0; slotIndex < menu.getNumberOfStorageInventorySlots(); slotIndex++) {
			Slot slot = menu.getSlot(slotIndex);
			int row = slotIndex / getSlotsOnLine() - firstVisibleRow;
			if (row < 0 || row >= numberOfVisibleRows) {
				slot.xPos = StorageScreenBase.DISABLED_SLOT_X_POS;
				continue;
			}
			slot.xPos = 8 + (slotIndex % getSlotsOnLine()) * 18;
			slot.yPos = 18 + row * 18;
			visibleSlotsCount++;
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		StorageSettingsTabControlBase tabControl = settingsTabControl;
		boolean templateLoadHovered = isTemplateLoadHovered();
		SettingsContainerMenu.setStacksHiddenFromDrawing(tabControl == null ? null : slotNumber -> tabControl.getSlotItemRotation(slotNumber, templateLoadHovered) != 0);
		try {
			super.drawScreen(mouseX, mouseY, partialTicks);
		} finally {
			SettingsContainerMenu.setStacksHiddenFromDrawing(null);
		}
		if (settingsTabControl != null) {
			settingsTabControl.renderTooltip(this, mouseX, mouseY);
		}
		if (templatePersistanceControl != null) {
			templatePersistanceControl.renderTooltip(this, mouseX, mouseY);
		}
		renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		int x = (width - xSize) / 2;
		int y = (height - ySize) / 2;
		StorageGuiHelper.renderStorageBackground(x, y, storageBackgroundProperties.getTextureName(), xSize, ySize - HEIGHT_WITHOUT_STORAGE_SLOTS);
		int slotsOnLine = getSlotsOnLine();
		GuiHelper.renderSlotsBackground(x + SLOTS_X_OFFSET, y + SLOTS_Y_OFFSET, slotsOnLine, visibleSlotsCount / slotsOnLine, visibleSlotsCount % slotsOnLine);
		if (settingsTabControl != null) {
			settingsTabControl.render(mouseX, mouseY, partialTicks);
		}
		if (templatePersistanceControl != null) {
			templatePersistanceControl.render(mouseX, mouseY, partialTicks);
		}
	}

	/** Over the slots vanilla drew: what empty slots show for their settings, and the tabs' marks. */
	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		fontRenderer.drawString(title, 8, 6, 0x404040);
		fontRenderer.drawString(I18n.format("container.inventory"), inventoryLabelX, inventoryLabelY, 0x404040);
		if (settingsTabControl == null) {
			return;
		}
		boolean templateLoadHovered = isTemplateLoadHovered();
		for (Slot slot : menu.getStorageInventorySlots()) {
			if (slot.xPos == StorageScreenBase.DISABLED_SLOT_X_POS) {
				continue;
			}
			int rotation = settingsTabControl.getSlotItemRotation(slot.slotNumber, templateLoadHovered);
			if (rotation != 0) {
				renderRotatedItem(slot.slotNumber, slot.xPos, slot.yPos, rotation, templateLoadHovered);
			} else if (!slot.getHasStack()) {
				ItemStack displayOverride = settingsTabControl.getSlotStackDisplayOverride(slot.slotNumber, templateLoadHovered);
				if (displayOverride.isEmpty() && menu.getStorageWrapper().getInventoryHandler().getFilterItem(slot.slotNumber) != Items.AIR) {
					displayOverride = new ItemStack(menu.getStorageWrapper().getInventoryHandler().getFilterItem(slot.slotNumber));
				}
				if (!displayOverride.isEmpty()) {
					RenderHelper.enableGUIStandardItemLighting();
					itemRender.renderItemAndEffectIntoGUI(mc.player, displayOverride, slot.xPos, slot.yPos);
					RenderHelper.disableStandardItemLighting();
				}
			}
			GlStateManager.disableLighting();
			GlStateManager.disableDepth();
			settingsTabControl.renderSlotOverlays(slot, templateLoadHovered);
			settingsTabControl.renderSlotExtra(slot, templateLoadHovered);
			GlStateManager.enableDepth();
		}
	}

	/** The item turned around the slot's middle, with its count unturned. */
	private void renderRotatedItem(int slotNumber, int x, int y, int rotation, boolean templateLoadHovered) {
		ItemStack stack = menu.getStorageWrapper().getInventoryHandler().getStackInSlot(slotNumber);
		if (stack.isEmpty() && settingsTabControl != null) {
			stack = settingsTabControl.getSlotStackDisplayOverride(slotNumber, templateLoadHovered);
		}
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 8, y + 8, 0);
		GlStateManager.rotate(rotation, 0, 0, 1);
		GlStateManager.translate(-x - 8, -y - 8, 0);
		RenderHelper.enableGUIStandardItemLighting();
		itemRender.renderItemAndEffectIntoGUI(mc.player, stack, x, y);
		RenderHelper.disableStandardItemLighting();
		GlStateManager.popMatrix();
		itemRender.renderItemOverlayIntoGUI(fontRenderer, stack, x, y, null);
		GlStateManager.disableLighting();
	}

	/** A slot click goes to the open tab, not to the server. */
	@Override
	protected void handleMouseClick(@Nullable Slot slot, int slotId, int mouseButton, ClickType type) {
		if (slot != null && settingsTabControl != null && slot.xPos != StorageScreenBase.DISABLED_SLOT_X_POS) {
			settingsTabControl.handleSlotClick(slot, mouseButton);
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (templatePersistanceControl != null && templatePersistanceControl.mouseClicked(mouseX, mouseY, mouseButton)) {
			return;
		}
		if (settingsTabControl != null && settingsTabControl.mouseClicked(mouseX, mouseY, mouseButton)) {
			return;
		}
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	/** Dragging over slots sets them as clicking each would. */
	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		Slot slot = getSlotUnderMouse();
		if (slot != null && settingsTabControl != null && slot.xPos != StorageScreenBase.DISABLED_SLOT_X_POS) {
			settingsTabControl.handleSlotClick(slot, clickedMouseButton);
		}
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int wheel = Mouse.getEventDWheel();
		if (wheel == 0) {
			return;
		}
		int mouseX = Mouse.getEventX() * width / mc.displayWidth;
		int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
		if (templatePersistanceControl != null && templatePersistanceControl.mouseScrolled(mouseX, mouseY, Integer.signum(wheel))) {
			return;
		}
		if (getMaxFirstVisibleRow() > 0) {
			firstVisibleRow = Math.max(0, Math.min(getMaxFirstVisibleRow(), firstVisibleRow - Integer.signum(wheel)));
			updateStorageSlotsPositions();
		}
	}

	/** The tabs and the template buttons, which item list mods mustn't cover. */
	public List<Rect2i> getExtraAreas() {
		List<Rect2i> areas = new ArrayList<>();
		if (settingsTabControl != null) {
			areas.addAll(settingsTabControl.getTabRectangles());
		}
		if (templatePersistanceControl != null) {
			areas.add(templatePersistanceControl.getRectangle());
		}
		return areas;
	}

	@Override
	protected boolean hasClickedOutside(int mouseX, int mouseY, int guiLeft, int guiTop) {
		return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop) && (settingsTabControl == null || !settingsTabControl.isMouseOver(mouseX, mouseY))
				&& (templatePersistanceControl == null || !templatePersistanceControl.getRectangle().contains(mouseX, mouseY));
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (templatePersistanceControl != null && templatePersistanceControl.keyTyped(typedChar, keyCode)) {
			return;
		}
		if (keyCode == 1 || mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
			sendStorageInventoryScreenOpenMessage();
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}
}
