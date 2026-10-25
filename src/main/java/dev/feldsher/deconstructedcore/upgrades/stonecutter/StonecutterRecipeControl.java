package dev.feldsher.deconstructedcore.upgrades.stonecutter;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.controls.WidgetBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/** The stonecutter's input slot, its list of results in four columns and three rows with a slider, and the result slot. */
public class StonecutterRecipeControl extends WidgetBase {
	private static final Dimension RECTANGLE_12_15 = new Dimension(12, 15);
	private static final Dimension RECTANGLE_16_18 = new Dimension(16, 18);
	private static final TextureBlitData SLIDER = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 131), RECTANGLE_12_15);
	private static final TextureBlitData DISABLED_SLIDER = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(41, 131), RECTANGLE_12_15);
	private static final TextureBlitData RECIPE_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(110, 148), RECTANGLE_16_18);
	private static final TextureBlitData SELECTED_RECIPE_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(110, 166), RECTANGLE_16_18);
	private static final TextureBlitData RECIPE_BACKGROUND_HOVERED = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(110, 184), RECTANGLE_16_18);
	private static final TextureBlitData LIST_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 146), new Dimension(81, 56));
	private static final TextureBlitData CRAFTING_RESULT_SLOT = new TextureBlitData(GuiHelper.GUI_CONTROLS, new UV(71, 216), new Dimension(26, 26));

	private static final int LIST_Y_OFFSET = 22;
	private static final int INPUT_SLOT_HEIGHT = 18;
	private static final int SPACING = 4;
	private static final int VISIBLE_RECIPES = 12;
	private static final int COLUMNS = 4;

	private boolean clickedOnScroll;
	private final StorageScreenBase<?> screen;
	private final StonecutterRecipeContainer container;
	private boolean hasItemsInInputSlot;
	private int recipeIndexOffset;
	private float sliderProgress;

	protected StonecutterRecipeControl(StorageScreenBase<?> screen, StonecutterRecipeContainer container, Position position) {
		super(position, new Dimension(81, 108));
		this.screen = screen;
		this.container = container;
		container.setInventoryUpdateListener(this::onInventoryUpdate);
		onInventoryUpdate();
	}

	public void moveSlotsToView() {
		Slot inputSlot = container.getInputSlot();
		inputSlot.xPos = x + getCenteredX(16) - screen.getGuiLeft();
		inputSlot.yPos = y - screen.getGuiTop() + 1;
		Slot outputSlot = container.getOutputSlot();
		outputSlot.xPos = x + getCenteredX(16) - screen.getGuiLeft();
		outputSlot.yPos = inputSlot.yPos + INPUT_SLOT_HEIGHT + SPACING + LIST_BACKGROUND.getHeight() + SPACING + 4;
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		GuiHelper.renderSlotsBackground(x + getCenteredX(18), y, 1, 1);
		GuiHelper.blit(x, y + LIST_Y_OFFSET, LIST_BACKGROUND);
		GuiHelper.blit(x + getCenteredX(26), y + INPUT_SLOT_HEIGHT + SPACING + LIST_BACKGROUND.getHeight() + SPACING, CRAFTING_RESULT_SLOT);
		int sliderYOffset = (int) (39.0F * sliderProgress) + 1;
		GuiHelper.blit(x + 68, y + LIST_Y_OFFSET + sliderYOffset, canScroll() ? SLIDER : DISABLED_SLIDER);

		int listInnerLeftX = x + 1;
		int listTopY = y + LIST_Y_OFFSET;
		renderRecipeBackgrounds(mouseX, mouseY, listInnerLeftX, listTopY);
		drawRecipesItems(listInnerLeftX, listTopY);
	}

	private int getVisibleEnd() {
		return Math.min(recipeIndexOffset + VISIBLE_RECIPES, container.getRecipeList().size());
	}

	private void drawRecipesItems(int listInnerLeftX, int top) {
		List<ItemStack> list = container.getRecipeList();
		RenderHelper.enableGUIStandardItemLighting();
		for (int i = recipeIndexOffset; i < getVisibleEnd(); ++i) {
			int j = i - recipeIndexOffset;
			GuiHelper.renderItemInGUI(list.get(i), listInnerLeftX + j % COLUMNS * 16, top + j / COLUMNS * 18 + 2);
		}
		RenderHelper.disableStandardItemLighting();
	}

	private void renderRecipeBackgrounds(int mouseX, int mouseY, int listInnerLeftX, int listTopY) {
		for (int recipeIndex = recipeIndexOffset; recipeIndex < getVisibleEnd(); ++recipeIndex) {
			int j = recipeIndex - recipeIndexOffset;
			int recipeX = listInnerLeftX + j % COLUMNS * 16;
			int recipeY = listTopY + j / COLUMNS * 18 + 2;
			TextureBlitData background = RECIPE_BACKGROUND;
			if (recipeIndex == container.getSelectedRecipe()) {
				background = SELECTED_RECIPE_BACKGROUND;
			} else if (mouseX >= recipeX && mouseY >= recipeY && mouseX < recipeX + 16 && mouseY < recipeY + 18) {
				background = RECIPE_BACKGROUND_HOVERED;
			}
			GuiHelper.blit(recipeX, recipeY - 1, background);
		}
	}

	private boolean canScroll() {
		return hasItemsInInputSlot && container.getRecipeList().size() > VISIBLE_RECIPES;
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		//noop - everything is rendered in background or with the tooltips
	}

	@Override
	public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
		super.renderTooltip(screen, mouseX, mouseY);
		if (!visible || !hasItemsInInputSlot) {
			return;
		}
		int listTopY = y + LIST_Y_OFFSET;
		List<ItemStack> list = container.getRecipeList();
		for (int recipeIndex = recipeIndexOffset; recipeIndex < getVisibleEnd(); ++recipeIndex) {
			int inViewIndex = recipeIndex - recipeIndexOffset;
			int recipeLeftX = x + 1 + inViewIndex % COLUMNS * 16;
			int recipeTopY = listTopY + inViewIndex / COLUMNS * 18 + 2;
			if (mouseX >= recipeLeftX && mouseX < recipeLeftX + 16 && mouseY >= recipeTopY && mouseY < recipeTopY + 18) {
				screen.drawHoveringText(screen.getItemToolTip(list.get(recipeIndex)), mouseX, mouseY);
			}
		}
	}

	private void onInventoryUpdate() {
		hasItemsInInputSlot = container.hasItemsInInputSlot();
		if (!hasItemsInInputSlot) {
			sliderProgress = 0.0F;
			recipeIndexOffset = 0;
		} else if (container.getSelectedRecipe() - recipeIndexOffset >= VISIBLE_RECIPES) {
			int rowsToScroll = (container.getSelectedRecipe() - recipeIndexOffset - VISIBLE_RECIPES) / COLUMNS + 1;
			scrollRecipesByDelta(-rowsToScroll);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		clickedOnScroll = false;
		if (!hasItemsInInputSlot) {
			return false;
		}
		int listInnerLeftX = x + 1;
		int listInnerTopY = y + LIST_Y_OFFSET + 1;
		for (int recipeIndex = recipeIndexOffset; recipeIndex < recipeIndexOffset + VISIBLE_RECIPES; ++recipeIndex) {
			int visibleRecipeIndex = recipeIndex - recipeIndexOffset;
			double relativeX = mouseX - (listInnerLeftX + visibleRecipeIndex % COLUMNS * 16);
			double relativeY = mouseY - (listInnerTopY + Math.floorDiv(visibleRecipeIndex, COLUMNS) * 18);
			if (relativeX >= 0.0D && relativeY >= 0.0D && relativeX < 16.0D && relativeY < 18.0D && container.selectRecipe(recipeIndex)) {
				minecraft.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
		}
		int sliderLeftX = listInnerLeftX + 67;
		if (mouseX >= sliderLeftX && mouseX < sliderLeftX + 12 && mouseY >= listInnerTopY && mouseY < listInnerTopY + 54) {
			clickedOnScroll = true;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button) {
		if (clickedOnScroll && canScroll()) {
			int listTopY = y + LIST_Y_OFFSET;
			int listBottomY = listTopY + 54;
			sliderProgress = MathHelper.clamp(((float) mouseY - listTopY - 7.5F) / ((listBottomY - listTopY) - 15.0F), 0.0F, 1.0F);
			recipeIndexOffset = (int) ((sliderProgress * getHiddenRows()) + 0.5D) * COLUMNS;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (canScroll()) {
			scrollRecipesByDelta(delta);
		}
		return true;
	}

	private void scrollRecipesByDelta(double delta) {
		int hiddenRows = getHiddenRows();
		sliderProgress = MathHelper.clamp((float) (sliderProgress - delta / hiddenRows), 0.0F, 1.0F);
		recipeIndexOffset = (int) (sliderProgress * hiddenRows + 0.5D) * COLUMNS;
	}

	protected int getHiddenRows() {
		return (container.getRecipeList().size() + COLUMNS - 1) / COLUMNS - 3;
	}
}
