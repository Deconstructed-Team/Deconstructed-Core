package dev.feldsher.deconstructedcore.client.gui;

import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Rect2i;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.common.gui.StorageBackgroundProperties;
import dev.feldsher.deconstructedcore.common.gui.StorageContainerMenuBase;
import dev.feldsher.deconstructedcore.client.gui.controls.Button;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinitions;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.common.gui.SortBy;
import dev.feldsher.deconstructedcore.settings.ISlotColorCategory;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsTab;
import dev.feldsher.deconstructedcore.common.gui.StorageInventorySlot;
import dev.feldsher.deconstructedcore.util.CountAbbreviator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A storage's screen: as wide as 9 or 12 slots a line, as tall as its rows, or as the screen lets it be:
 * then the rows scroll, as in 1.21's InventoryScrollPanel.
 * <p>
 * The upgrade slots sit in a column on the left, the upgrades' settings tabs on the right.
 * Upgrade switches, settings, sort buttons and the search box come with the stages that port them.
 */
public abstract class StorageScreenBase<S extends StorageContainerMenuBase<?>> extends GuiContainer {
	public static final int DISABLED_SLOT_X_POS = -2000;
	static final int SLOTS_Y_OFFSET = 17;
	static final int SLOTS_X_OFFSET = 7;
	public static final int HEIGHT_WITHOUT_STORAGE_SLOTS = 114;
	private static final int SCROLL_BAR_WIDTH = 6;
	private static final int UPGRADE_TOP_HEIGHT = 7;
	private static final int UPGRADE_SLOT_HEIGHT = 16;
	private static final int UPGRADE_BOTTOM_HEIGHT = 6;
	public static final int UPGRADE_INVENTORY_OFFSET = 21;

	protected final S menu;
	private final String title;
	private StorageBackgroundProperties storageBackgroundProperties = StorageBackgroundProperties.REGULAR_9_SLOT;
	private int visibleSlotsCount;
	protected int inventoryLabelX = 8;
	protected int inventoryLabelY;

	private final int numberOfUpgradeSlots;
	private int numberOfVisibleRows;
	private int firstVisibleRow = 0;
	private boolean draggingScrollBar = false;
	@Nullable
	private UpgradeSettingsTabControl settingsTabControl = null;
	@Nullable
	private Button sortButton = null;
	@Nullable
	private ToggleButton<SortBy> sortByButton = null;

	protected StorageScreenBase(S menu, String title) {
		super(menu);
		this.menu = menu;
		this.title = title;
		visibleSlotsCount = menu.getNumberOfStorageInventorySlots();
		numberOfUpgradeSlots = menu.getNumberOfUpgradeSlots();
		updateDimensionsAndSlotPositions(new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight());
	}

	public S getMenu() {
		return menu;
	}

	@Override
	public void initGui() {
		super.initGui();
		settingsTabControl = new UpgradeSettingsTabControl(new Position(guiLeft + xSize, guiTop + 4), this, getStorageSettingsTabTooltip());
		if (shouldShowSortButtons()) {
			addSortButtons();
		}
	}

	@Nullable
	protected abstract String getStorageSettingsTabTooltip();

	protected boolean shouldShowSortButtons() {
		return true;
	}

	/** On the title line, at its right, as 1.21's default place for them; its other places come with the client config. */
	private void addSortButtons() {
		Position pos = new Position(guiLeft + xSize - 31, guiTop + 4);
		sortButton = new Button(pos, ButtonDefinitions.SORT, button -> {
			if (button == 0) {
				getMenu().sort();
			}
		});
		sortByButton = new ToggleButton<>(new Position(pos.x() + 12, pos.y()), ButtonDefinitions.SORT_BY, button -> {
			if (button == 0) {
				getMenu().setSortBy(getMenu().getSortBy().next());
			}
		}, () -> getMenu().getSortBy());
	}

	public UpgradeSettingsTabControl getUpgradeSettingsControl() {
		return settingsTabControl;
	}

	public int getGuiLeft() {
		return guiLeft;
	}

	public int getGuiTop() {
		return guiTop;
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		if (settingsTabControl != null) {
			settingsTabControl.tick();
		}
	}

	private void updateDimensionsAndSlotPositions(int height) {
		numberOfVisibleRows = Math.max(1, Math.min((height - HEIGHT_WITHOUT_STORAGE_SLOTS) / 18, getMenu().getNumberOfRows()));
		storageBackgroundProperties = (getMenu().getNumberOfStorageInventorySlots() + getMenu().getColumnsTaken() * getMenu().getNumberOfRows()) <= 81 ? StorageBackgroundProperties.REGULAR_9_SLOT : StorageBackgroundProperties.REGULAR_12_SLOT;

		xSize = storageBackgroundProperties.getSlotsOnLine() * 18 + 14;
		if (isScrolling()) {
			storageBackgroundProperties = storageBackgroundProperties == StorageBackgroundProperties.REGULAR_9_SLOT ? StorageBackgroundProperties.WIDER_9_SLOT : StorageBackgroundProperties.WIDER_12_SLOT;
			xSize += SCROLL_BAR_WIDTH;
		}
		firstVisibleRow = Math.min(firstVisibleRow, getMaxFirstVisibleRow());
		updateStorageSlotsPositions();
		ySize = HEIGHT_WITHOUT_STORAGE_SLOTS + getStorageInventoryHeight(numberOfVisibleRows);
		inventoryLabelY = ySize - 94;
		inventoryLabelX = 8 + storageBackgroundProperties.getPlayerInventoryXOffset();
		updatePlayerSlotsPositions();
		updateUpgradeSlotsPositions();
	}

	protected void updateUpgradeSlotsPositions() {
		int yPosition = 6;
		for (int slotIndex = 0; slotIndex < numberOfUpgradeSlots; slotIndex++) {
			Slot slot = getMenu().getSlot(getMenu().getFirstUpgradeSlot() + slotIndex);
			slot.yPos = yPosition;
			yPosition += UPGRADE_SLOT_HEIGHT;
		}
	}

	protected int getUpgradeHeightWithoutBottom() {
		return UPGRADE_BOTTOM_HEIGHT + numberOfUpgradeSlots * UPGRADE_SLOT_HEIGHT;
	}

	protected int getUpgradeHeight() {
		return getUpgradeHeightWithoutBottom() + UPGRADE_TOP_HEIGHT;
	}

	private boolean isScrolling() {
		return numberOfVisibleRows < getMenu().getNumberOfRows();
	}

	private int getMaxFirstVisibleRow() {
		return Math.max(0, getMenu().getNumberOfRows() - numberOfVisibleRows);
	}

	protected int getStorageInventoryHeight(int displayableNumberOfRows) {
		return displayableNumberOfRows * 18;
	}

	protected int getSlotsOnLine() {
		return storageBackgroundProperties.getSlotsOnLine() - getMenu().getColumnsTaken();
	}

	/** Slots in the visible rows go where they're drawn; those scrolled away go off screen, where they can't be clicked. */
	protected void updateStorageSlotsPositions() {
		visibleSlotsCount = 0;
		for (int slotIndex = 0; slotIndex < getMenu().getNumberOfStorageInventorySlots(); slotIndex++) {
			Slot slot = getMenu().getSlot(slotIndex);
			int row = slotIndex / getSlotsOnLine() - firstVisibleRow;
			if (row < 0 || row >= numberOfVisibleRows) {
				slot.xPos = DISABLED_SLOT_X_POS;
				continue;
			}
			slot.xPos = 8 + (slotIndex % getSlotsOnLine()) * 18;
			slot.yPos = 18 + row * 18;
			visibleSlotsCount++;
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
		StorageInventorySlot.setHideLargeCounts(true);
		try {
			super.drawScreen(mouseX, mouseY, partialTicks);
		} finally {
			StorageInventorySlot.setHideLargeCounts(false);
		}
		renderHoveredToolTip(mouseX, mouseY);
		if (settingsTabControl != null) {
			settingsTabControl.renderTooltip(this, mouseX, mouseY);
		}
		if (sortButton != null && sortByButton != null) {
			sortButton.renderTooltip(this, mouseX, mouseY);
			sortByButton.renderTooltip(this, mouseX, mouseY);
		}
		renderUpgradeSlotChangeError(mouseX, mouseY);
	}

	private void renderUpgradeSlotChangeError(int mouseX, int mouseY) {
		getMenu().getErrorUpgradeSlotChangeResult().flatMap(result -> result.getErrorMessage()).ifPresent(message ->
				drawHoveringText(java.util.Collections.singletonList(net.minecraft.util.text.TextFormatting.RED + message.getFormattedText()), mouseX, mouseY));
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		int x = (width - xSize) / 2;
		int y = (height - ySize) / 2;
		StorageGuiHelper.renderStorageBackground(x, y, storageBackgroundProperties.getTextureName(), xSize, ySize - HEIGHT_WITHOUT_STORAGE_SLOTS);
		drawSlotBg(x, y, visibleSlotsCount);
		if (isScrolling()) {
			drawScrollBar();
		}
		drawUpgradeBackground();
		if (sortButton != null && sortByButton != null) {
			sortButton.render(mouseX, mouseY, partialTicks);
			sortByButton.render(mouseX, mouseY, partialTicks);
		}
		if (settingsTabControl != null) {
			settingsTabControl.render(mouseX, mouseY, partialTicks);
		}
		renderMemorizedStacks(x, y);
	}

	/** Empty memorized slots show what they remember, under a translucent cover, where vanilla then draws nothing. */
	private void renderMemorizedStacks(int x, int y) {
		for (int slotIndex = 0; slotIndex < getMenu().getNumberOfStorageInventorySlots(); slotIndex++) {
			Slot slot = getMenu().getSlot(slotIndex);
			if (slot.xPos == DISABLED_SLOT_X_POS || slot.getHasStack()) {
				continue;
			}
			getMenu().getMemorizedStackInSlot(slotIndex).ifPresent(memorizedStack -> {
				RenderHelper.enableGUIStandardItemLighting();
				itemRender.renderItemAndEffectIntoGUI(mc.player, memorizedStack, x + slot.xPos, y + slot.yPos);
				RenderHelper.disableStandardItemLighting();
				MemorySettingsTab.drawMemorizedStackOverlay(x + slot.xPos, y + slot.yPos);
			});
		}
	}

	private void drawUpgradeBackground() {
		if (numberOfUpgradeSlots == 0) {
			return;
		}

		int heightWithoutBottom = getUpgradeHeightWithoutBottom();
		GuiHelper.blit(GuiHelper.GUI_CONTROLS, guiLeft - UPGRADE_INVENTORY_OFFSET, guiTop, 0, 0, 26, 4);
		GuiHelper.blit(GuiHelper.GUI_CONTROLS, guiLeft - UPGRADE_INVENTORY_OFFSET, guiTop + 4, 0, 4, 25, heightWithoutBottom - 4);
		GuiHelper.blit(GuiHelper.GUI_CONTROLS, guiLeft - UPGRADE_INVENTORY_OFFSET, guiTop + heightWithoutBottom, 0, 198, 25, UPGRADE_BOTTOM_HEIGHT);
	}

	/** What the screen draws outside of its main area, which item list mods mustn't cover: upgrades, tabs, sort buttons. */
	public List<Rect2i> getExtraAreas() {
		List<Rect2i> areas = new ArrayList<>();
		if (numberOfUpgradeSlots > 0) {
			areas.add(new Rect2i(guiLeft - UPGRADE_INVENTORY_OFFSET, guiTop, UPGRADE_INVENTORY_OFFSET, getUpgradeHeight()));
		}
		if (settingsTabControl != null) {
			areas.addAll(settingsTabControl.getTabRectangles());
		}
		if (sortButton != null && sortByButton != null) {
			areas.add(new Rect2i(sortButton.getX(), sortButton.getY(), sortByButton.getX() + sortByButton.getWidth() - sortButton.getX(), sortButton.getHeight()));
		}
		return areas;
	}

	private boolean isOverUpgradeColumn(int mouseX, int mouseY) {
		return numberOfUpgradeSlots > 0 && mouseX >= guiLeft - UPGRADE_INVENTORY_OFFSET && mouseX < guiLeft
				&& mouseY >= guiTop && mouseY < guiTop + getUpgradeHeight();
	}

	protected void drawSlotBg(int x, int y, int visibleSlotsCount) {
		int slotsOnLine = getSlotsOnLine();
		int slotRows = visibleSlotsCount / slotsOnLine;
		int remainingSlots = visibleSlotsCount % slotsOnLine;
		GuiHelper.renderSlotsBackground(x + SLOTS_X_OFFSET, y + SLOTS_Y_OFFSET, slotsOnLine, slotRows, remainingSlots);
	}

	private int getScrollBarLeft() {
		return guiLeft + SLOTS_X_OFFSET + getSlotsOnLine() * 18;
	}

	private int getScrollAreaTop() {
		return guiTop + SLOTS_Y_OFFSET;
	}

	private int getScrollAreaHeight() {
		return numberOfVisibleRows * 18;
	}

	private int getScrollBarHeight() {
		int areaHeight = getScrollAreaHeight();
		int barHeight = areaHeight * numberOfVisibleRows / getMenu().getNumberOfRows();
		return Math.max(Math.min(barHeight, areaHeight - 8), Math.min(32, areaHeight));
	}

	/** The bar as Forge's ScrollPanel draws it: gray on black, lighter at the top and left. */
	private void drawScrollBar() {
		int left = getScrollBarLeft();
		int right = left + SCROLL_BAR_WIDTH;
		int top = getScrollAreaTop();
		int areaHeight = getScrollAreaHeight();
		int barHeight = getScrollBarHeight();
		int barTop = top + (getMaxFirstVisibleRow() == 0 ? 0 : firstVisibleRow * (areaHeight - barHeight) / getMaxFirstVisibleRow());

		drawRect(left, top, right, top + areaHeight, 0xFF000000);
		drawRect(left, barTop, right, barTop + barHeight, 0xFF808080);
		drawRect(left, barTop, right - 1, barTop + barHeight - 1, 0xFFC0C0C0);
	}

	private boolean isOverScrollBar(int mouseX, int mouseY) {
		int left = getScrollBarLeft();
		int top = getScrollAreaTop();
		return mouseX >= left && mouseX < left + SCROLL_BAR_WIDTH && mouseY >= top && mouseY < top + getScrollAreaHeight();
	}

	private void scrollTo(int row) {
		int newRow = Math.max(0, Math.min(row, getMaxFirstVisibleRow()));
		if (newRow != firstVisibleRow) {
			firstVisibleRow = newRow;
			updateStorageSlotsPositions();
		}
	}

	private void scrollToMouse(int mouseY) {
		int travel = getScrollAreaHeight() - getScrollBarHeight();
		if (travel <= 0) {
			return;
		}
		int barTop = mouseY - getScrollAreaTop() - getScrollBarHeight() / 2;
		scrollTo(Math.round((float) barTop * getMaxFirstVisibleRow() / travel));
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
		if (settingsTabControl != null && settingsTabControl.mouseScrolled(mouseX, mouseY, wheel)) {
			return;
		}
		if (isScrolling()) {
			scrollTo(firstVisibleRow + (wheel > 0 ? -1 : 1));
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (sortButton != null && sortByButton != null && (sortButton.mouseClicked(mouseX, mouseY, mouseButton) || sortByButton.mouseClicked(mouseX, mouseY, mouseButton))) {
			return;
		}
		if (settingsTabControl != null && !isOverTabSlot(mouseX, mouseY) && settingsTabControl.mouseClicked(mouseX, mouseY, mouseButton)) {
			return;
		}
		if (isScrolling() && mouseButton == 0 && isOverScrollBar(mouseX, mouseY)) {
			draggingScrollBar = true;
			scrollToMouse(mouseY);
			return;
		}
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		if (draggingScrollBar) {
			scrollToMouse(mouseY);
			return;
		}
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		if (draggingScrollBar) {
			draggingScrollBar = false;
			return;
		}
		super.mouseReleased(mouseX, mouseY, state);
	}

	@Override
	protected boolean hasClickedOutside(int mouseX, int mouseY, int guiLeft, int guiTop) {
		return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop) && !isOverScrollBar(mouseX, mouseY) && !isOverUpgradeColumn(mouseX, mouseY)
				&& (settingsTabControl == null || !settingsTabControl.isMouseOver(mouseX, mouseY));
	}

	/** Slots of an open tab are the container's, and take their clicks as any slot does. */
	private boolean isOverTabSlot(int mouseX, int mouseY) {
		for (int index = getMenu().getFirstUpgradeSlot() + getMenu().getNumberOfUpgradeSlots(); index < getMenu().inventorySlots.size(); index++) {
			Slot slot = getMenu().getSlot(index);
			if (slot.xPos != DISABLED_SLOT_X_POS && isPointInRegion(slot.xPos, slot.yPos, 16, 16, mouseX, mouseY)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		fontRenderer.drawString(title, 8, 6, 0x404040);
		fontRenderer.drawString(I18n.format("container.inventory"), inventoryLabelX, inventoryLabelY, 0x404040);
		renderLargeStackCounts();
		renderSlotColors();
	}

	/** The colors settings categories mark slots with, such as the no-sort slots', translucent over the slots. */
	private void renderSlotColors() {
		java.util.List<ISlotColorCategory> categories = getMenu().getStorageWrapper().getSettingsHandler().getCategoriesThatImplement(ISlotColorCategory.class);
		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		for (int slotIndex = 0; slotIndex < getMenu().getNumberOfStorageInventorySlots(); slotIndex++) {
			Slot slot = getMenu().getSlot(slotIndex);
			if (slot.xPos == DISABLED_SLOT_X_POS) {
				continue;
			}
			for (ISlotColorCategory category : categories) {
				int index = slotIndex;
				category.getSlotColor(index).ifPresent(color -> drawRect(slot.xPos, slot.yPos, slot.xPos + 16, slot.yPos + 16, (color & 0x00_FFFFFF) | 0x50_000000));
			}
		}
		GlStateManager.enableDepth();
	}

	/** The counts the slots hid while vanilla drew them: abbreviated, and scaled down to fit the slot. */
	private void renderLargeStackCounts() {
		StorageInventorySlot.setHideLargeCounts(false);
		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		GlStateManager.disableBlend();
		for (int slotIndex = 0; slotIndex < getMenu().getNumberOfStorageInventorySlots(); slotIndex++) {
			Slot slot = getMenu().getSlot(slotIndex);
			ItemStack stack = slot.getStack();
			if (slot.xPos != DISABLED_SLOT_X_POS && stack.getCount() > StorageInventorySlot.SPECIAL_COUNT_RENDER_THRESHOLD) {
				renderStackCount(CountAbbreviator.abbreviate(stack.getCount()), slot.xPos, slot.yPos);
			}
		}
		GlStateManager.enableLighting();
		GlStateManager.enableDepth();
		StorageInventorySlot.setHideLargeCounts(true);
	}

	private void renderStackCount(String count, int x, int y) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 0, 200);
		int width = fontRenderer.getStringWidth(count);
		float scale = Math.min(1f, 16f / width);
		if (scale < 1f) {
			GlStateManager.scale(scale, scale, 1);
		}
		fontRenderer.drawStringWithShadow(count, (x + 19 - 2 - width * scale) / scale, (y + 6 + 3 + (1 / (scale * scale) - 1)) / scale, 0xFFFFFF);
		GlStateManager.popMatrix();
	}

	@Override
	public void setWorldAndResolution(Minecraft mc, int width, int height) {
		updateDimensionsAndSlotPositions(height);
		super.setWorldAndResolution(mc, width, height);
	}
}
