package dev.feldsher.deconstructedcore.upgrades.crafting;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.UpgradeSettingsTab;
import dev.feldsher.deconstructedcore.client.gui.controls.Button;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.GUI_CONTROLS;
import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.ICONS;

/**
 * The crafting grid, with the button for where shift clicked results go. When several recipes match the grid, buttons
 * under the result go through their results, and the one over it shows all of them to pick from, drawn over the slots.
 */
public class CraftingUpgradeTab extends UpgradeSettingsTab<CraftingUpgradeContainer> {
	private static final int RESULT_SELECTION_BORDER_WIDTH = 3;
	private static final TextureBlitData ARROW = new TextureBlitData(GUI_CONTROLS, new UV(97, 216), new Dimension(15, 8));
	private static final TextureBlitData CRAFTING_RESULT_SLOT = new TextureBlitData(GUI_CONTROLS, new UV(71, 216), new Dimension(26, 26));
	private static final Dimension DIMENSION_8_12 = new Dimension(8, 12);
	private static final Dimension DIMENSION_16_12 = new Dimension(16, 12);
	private static final TextureBlitData SMALL_BUTTON_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(53, 18), DIMENSION_8_12);
	private static final TextureBlitData SMALL_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(61, 18), DIMENSION_8_12);
	private static final ButtonDefinition PREVIOUS_RESULT = new ButtonDefinition(DIMENSION_8_12, SMALL_BUTTON_BACKGROUND, SMALL_BUTTON_HOVERED_BACKGROUND,
			new TextureBlitData(ICONS, new Position(0, 0), Dimension.SQUARE_256, new UV(48, 144), DIMENSION_8_12),
			new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("previous_result")));
	private static final ButtonDefinition NEXT_RESULT = new ButtonDefinition(DIMENSION_8_12, SMALL_BUTTON_BACKGROUND, SMALL_BUTTON_HOVERED_BACKGROUND,
			new TextureBlitData(ICONS, new Position(0, 0), Dimension.SQUARE_256, new UV(56, 144), DIMENSION_8_12),
			new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("next_result")));
	private static final ButtonDefinition SELECT_RESULT = new ButtonDefinition(DIMENSION_16_12, new TextureBlitData(GUI_CONTROLS, new UV(69, 18), DIMENSION_16_12),
			new TextureBlitData(GUI_CONTROLS, new UV(63, 30), DIMENSION_16_12), null,
			new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("select_result")));
	private static final int HOVER_HIGHLIGHT_COLOR = 0x80FFFFFF;

	private final Button previousResultButton;
	private final Button nextResultButton;
	private final Button selectResultButton;
	private boolean resultSelectionShown = false;
	@Nullable
	private Position resultListPosition = null;
	private Dimension resultListDimension = new Dimension(0, 0);

	public CraftingUpgradeTab(CraftingUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, ButtonDefinition.Toggle<Boolean> shiftClickTargetButton) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("crafting"),
				new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey("crafting") + TranslationHelper.TOOLTIP_SUFFIX));
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), shiftClickTargetButton,
				button -> getContainer().setShiftClickIntoStorage(!getContainer().shouldShiftClickIntoStorage()), () -> getContainer().shouldShiftClickIntoStorage()));
		openTabDimension = new Dimension(63, 142);
		previousResultButton = addHideableChild(new Button(new Position(x + 9, y + 118), PREVIOUS_RESULT, button -> {
			if (button == 0) {
				getContainer().selectPreviousCraftingResult();
			}
		}));
		nextResultButton = addHideableChild(new Button(new Position(x + 43, y + 118), NEXT_RESULT, button -> {
			if (button == 0) {
				getContainer().selectNextCraftingResult();
			}
		}));
		selectResultButton = addHideableChild(new Button(new Position(x + 22, y + 99), SELECT_RESULT, button -> {
			if (button == 0) {
				resultSelectionShown = !resultSelectionShown;
			}
		}) {
			@Override
			protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
				super.renderWidget(mouseX, mouseY, partialTicks);
				font.drawStringWithShadow(String.valueOf(getContainer().getMatchedCraftingResults().size()), x + 5, y + 2, 0xFFFFFF);
			}
		});
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		super.renderBg(minecraft, mouseX, mouseY);
		if (getContainer().isOpen()) {
			GuiHelper.renderSlotsBackground(x + 3, y + 44, 3, 3);
			GuiHelper.blit(x + 3 + 19, y + 101, ARROW);
			GuiHelper.blit(x + 3 + 14, y + 111, CRAFTING_RESULT_SLOT);
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		boolean showSelection = shouldShowResultSelection();
		previousResultButton.setVisible(isOpen && showSelection);
		nextResultButton.setVisible(isOpen && showSelection);
		selectResultButton.setVisible(isOpen && showSelection);
		if (!showSelection) {
			resultSelectionShown = false;
		}
		super.render(mouseX, mouseY, partialTicks);
	}

	private boolean shouldShowResultSelection() {
		return getContainer().getMatchedCraftingResults().size() > 1;
	}

	private boolean isResultSelectionVisible() {
		return isOpen && resultSelectionShown && shouldShowResultSelection();
	}

	/** Above the select button, centered on it: three results in a row. */
	private Position getResultListPosition() {
		int resultCount = getContainer().getMatchedCraftingResults().size();
		int height = RESULT_SELECTION_BORDER_WIDTH * 2 + ((resultCount - 1) / 3 + 1) * 18;
		int width = RESULT_SELECTION_BORDER_WIDTH * 2 + Math.min(resultCount, 3) * 18;
		resultListDimension = new Dimension(width, height);
		resultListPosition = new Position(selectResultButton.getX() + 8 - RESULT_SELECTION_BORDER_WIDTH - (int) (Math.min(resultCount, 3) / 2f * 18),
				selectResultButton.getY() - height);
		return resultListPosition;
	}

	private Position getChoicePosition(Position listPosition, int index) {
		return new Position(listPosition.x() + RESULT_SELECTION_BORDER_WIDTH + (index % 3) * 18, listPosition.y() + RESULT_SELECTION_BORDER_WIDTH + (index / 3) * 18);
	}

	private Optional<Integer> getResultChoiceHovered(int mouseX, int mouseY) {
		if (!isResultSelectionVisible()) {
			return Optional.empty();
		}
		Position listPosition = getResultListPosition();
		List<ItemStack> results = getContainer().getMatchedCraftingResults();
		for (int i = 0; i < results.size(); i++) {
			Position position = getChoicePosition(listPosition, i);
			if (mouseX >= position.x() && mouseX < position.x() + 18 && mouseY >= position.y() && mouseY < position.y() + 18) {
				return Optional.of(i);
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		Optional<Integer> choice = getResultChoiceHovered((int) mouseX, (int) mouseY);
		if (choice.isPresent()) {
			getContainer().selectCraftingResult(choice.get());
			resultSelectionShown = false;
			return true;
		}
		if (isResultSelectionVisible() && !slotIsNotCoveredAt(null, mouseX, mouseY)) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean slotIsNotCoveredAt(@Nullable Slot slot, double mouseX, double mouseY) {
		if (!isResultSelectionVisible()) {
			return true;
		}
		Position pos = getResultListPosition();
		return mouseX < pos.x() || mouseX > pos.x() + resultListDimension.width() || mouseY < pos.y() || mouseY > pos.y() + resultListDimension.height();
	}

	/** The choice of results is drawn with the tooltips, after the slots, so that it covers their items. */
	@Override
	public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
		if (!isResultSelectionVisible()) {
			super.renderTooltip(screen, mouseX, mouseY);
			return;
		}
		List<ItemStack> results = getContainer().getMatchedCraftingResults();
		Position pos = getResultListPosition();
		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 0, 300);
		GlStateManager.color(1, 1, 1, 1);
		renderResultSelectionBackground(pos, results.size());
		RenderHelper.enableGUIStandardItemLighting();
		for (int i = 0; i < results.size(); i++) {
			Position position = getChoicePosition(pos, i);
			GuiHelper.renderItemInGUI(results.get(i), position.x() + 1, position.y() + 1);
		}
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableDepth();
		getResultChoiceHovered(mouseX, mouseY).ifPresent(i -> {
			Position position = getChoicePosition(pos, i);
			Gui.drawRect(position.x() + 1, position.y() + 1, position.x() + 17, position.y() + 17, HOVER_HIGHLIGHT_COLOR);
		});
		GlStateManager.enableDepth();
		GlStateManager.popMatrix();

		Optional<Integer> hovered = getResultChoiceHovered(mouseX, mouseY);
		if (hovered.isPresent()) {
			screen.drawHoveringText(screen.getItemToolTip(results.get(hovered.get())), mouseX, mouseY);
		} else {
			super.renderTooltip(screen, mouseX, mouseY);
		}
	}

	private static void renderResultSelectionBackground(Position pos, int resultCount) {
		int width = RESULT_SELECTION_BORDER_WIDTH * 2 + Math.min(resultCount, 3) * 18;
		int height = RESULT_SELECTION_BORDER_WIDTH * 2 + ((resultCount - 1) / 3 + 1) * 18;
		int halfWidth = width / 2;
		int halfHeight = height / 2;
		GuiHelper.blit(GUI_CONTROLS, pos.x(), pos.y(), 85, 24, halfWidth, halfHeight);
		GuiHelper.blit(GUI_CONTROLS, pos.x() + halfWidth, pos.y(), 117 - halfWidth, 24, halfWidth, halfHeight);
		GuiHelper.blit(GUI_CONTROLS, pos.x(), pos.y() + halfHeight, 85, 56 - halfHeight, halfWidth, halfHeight);
		GuiHelper.blit(GUI_CONTROLS, pos.x() + halfWidth, pos.y() + halfHeight, 117 - halfWidth, 56 - halfHeight, halfWidth, halfHeight);
		GuiHelper.renderSlotsBackground(pos.x() + RESULT_SELECTION_BORDER_WIDTH, pos.y() + RESULT_SELECTION_BORDER_WIDTH, 3, resultCount / 3, resultCount % 3);
	}

	@Override
	protected void moveSlotsToTab() {
		List<Slot> slots = getContainer().getSlots();
		for (int slotNumber = 0; slotNumber < 9; slotNumber++) {
			Slot slot = slots.get(slotNumber);
			slot.xPos = x + 3 - screen.getGuiLeft() + 1 + (slotNumber % 3) * 18;
			slot.yPos = y + 44 - screen.getGuiTop() + 1 + (slotNumber / 3) * 18;
		}
		Slot craftingSlot = slots.get(9);
		craftingSlot.xPos = x + 3 - screen.getGuiLeft() + 19;
		craftingSlot.yPos = y + 44 - screen.getGuiTop() + 72;
	}
}
