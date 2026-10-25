package dev.feldsher.deconstructedcore.upgrades.cooking;

import dev.feldsher.deconstructedcore.client.gui.controls.CompositeWidgetBase;
import dev.feldsher.deconstructedcore.client.gui.controls.ProgressBar;
import dev.feldsher.deconstructedcore.client.gui.controls.WidgetBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.world.World;

import java.util.List;

import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.GUI_CONTROLS;

/** A furnace: input, fuel and output slots, with the flame and the arrow showing how far burning and cooking got. */
public class CookingLogicControl extends CompositeWidgetBase<WidgetBase> {
	private static final TextureBlitData FURNACE_BACKGROUND = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 202), new Dimension(68, 54));
	private static final TextureBlitData COOK_PROGRESS = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(100, 239), new Dimension(22, 16));
	private static final TextureBlitData BURN_PROGRESS = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(99, 225), new Dimension(14, 14));
	private final CookingLogicContainer cookingLogicContainer;

	public CookingLogicControl(Position position, CookingLogicContainer cookingLogicContainer) {
		super(position, new Dimension(68, 54));
		this.cookingLogicContainer = cookingLogicContainer;
		addChild(new ProgressBar(new Position(x + 19, y + 18), COOK_PROGRESS, this::getCookProgress, ProgressBar.ProgressDirection.LEFT_RIGHT));
		addChild(new ProgressBar(new Position(x + 1, y + 20), BURN_PROGRESS, this::getBurnProgress, ProgressBar.ProgressDirection.BOTTOM_UP));
	}

	private float getBurnProgress() {
		World world = Minecraft.getMinecraft().world;
		return world != null && cookingLogicContainer.isBurning(world) ? getProgress(cookingLogicContainer.getBurnTimeFinish(), cookingLogicContainer.getBurnTimeTotal()) : 0;
	}

	private float getCookProgress() {
		return cookingLogicContainer.isCooking() ? getProgress(cookingLogicContainer.getCookTimeFinish(), cookingLogicContainer.getCookTimeTotal()) : 0;
	}

	private float getProgress(long finishTime, int timeTotal) {
		World world = Minecraft.getMinecraft().world;
		if (world == null || timeTotal <= 0) {
			return 0;
		}
		return 1 - ((float) Math.max(finishTime - world.getTotalWorldTime(), 0) / timeTotal);
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		GuiHelper.blit(x, y, FURNACE_BACKGROUND);
	}

	public void moveSlotsToView(int screenGuiLeft, int screenGuiTop) {
		List<Slot> cookingSlots = cookingLogicContainer.getCookingSlots();
		positionSlot(cookingSlots.get(CookingLogic.COOK_INPUT_SLOT), screenGuiLeft, screenGuiTop, 1, 1);
		positionSlot(cookingSlots.get(CookingLogic.COOK_OUTPUT_SLOT), screenGuiLeft, screenGuiTop, 47, 19);
		positionSlot(cookingSlots.get(CookingLogic.FUEL_SLOT), screenGuiLeft, screenGuiTop, 1, 37);
	}

	private void positionSlot(Slot slot, int screenGuiLeft, int screenGuiTop, int xOffset, int yOffset) {
		slot.xPos = x - screenGuiLeft + xOffset;
		slot.yPos = y - screenGuiTop + yOffset;
	}
}
