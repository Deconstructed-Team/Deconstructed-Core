package dev.feldsher.deconstructedcore.compat.hei;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Rect2i;
import dev.feldsher.deconstructedcore.common.gui.IFilterSlot;
import dev.feldsher.deconstructedcore.common.gui.StorageContainerMenuBase;
import dev.feldsher.deconstructedcore.upgrades.pump.FluidFilterControl;
import dev.feldsher.deconstructedcore.upgrades.pump.PumpUpgradeTab;
import dev.feldsher.deconstructedcore.util.FluidContainerHelper;
import mezz.jei.api.gui.IGhostIngredientHandler;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * An item dragged from HEI onto a filter slot of the open upgrade tab sets it, as a click with the item would. A fluid,
 * or an item holding one, sets a fluid filter slot of the open advanced pump tab.
 */
public class StorageGhostIngredientHandler<S extends StorageScreenBase<?>> implements IGhostIngredientHandler<S> {
	@Override
	public <I> List<Target<I>> getTargets(S gui, I ingredient, boolean doStart) {
		List<Target<I>> targets = new ArrayList<>();
		if (ingredient instanceof FluidStack) {
			addFluidTargets(gui, (FluidStack) ingredient, targets);
			return targets;
		}
		if (!(ingredient instanceof ItemStack)) {
			return targets;
		}
		ItemStack ghostStack = (ItemStack) ingredient;
		FluidStack containedFluid = FluidContainerHelper.getDrainable(ghostStack);
		if (containedFluid != null && getOpenPumpTab(gui) != null) {
			addFluidTargets(gui, containedFluid, targets);
			return targets;
		}
		StorageContainerMenuBase<?> container = gui.getMenu();
		for (Slot slot : container.inventorySlots) {
			if (slot instanceof IFilterSlot && container.isUpgradeSettingsSlot(slot.slotNumber) && slot.xPos != StorageScreenBase.DISABLED_SLOT_X_POS
					&& slot.isItemValid(ghostStack)) {
				targets.add(new Target<I>() {
					@Override
					public Rectangle getArea() {
						return new Rectangle(gui.getGuiLeft() + slot.xPos, gui.getGuiTop() + slot.yPos, 17, 17);
					}

					@Override
					public void accept(I i) {
						container.setGhostSlot(slot.slotNumber, ghostStack);
					}
				});
			}
		}
		return targets;
	}

	private static PumpUpgradeTab.Advanced getOpenPumpTab(StorageScreenBase<?> gui) {
		return gui.getUpgradeSettingsControl().getOpenTab().filter(PumpUpgradeTab.Advanced.class::isInstance)
				.map(PumpUpgradeTab.Advanced.class::cast).orElse(null);
	}

	private static <I> void addFluidTargets(StorageScreenBase<?> gui, FluidStack ghostFluid, List<Target<I>> targets) {
		PumpUpgradeTab.Advanced pumpTab = getOpenPumpTab(gui);
		if (pumpTab == null) {
			return;
		}
		FluidFilterControl fluidFilterControl = pumpTab.getFluidFilterControl();
		for (int slot = 0; slot < fluidFilterControl.getNumberOfSlots(); slot++) {
			int slotIndex = slot;
			Rect2i area = fluidFilterControl.getSlotArea(slot);
			targets.add(new Target<I>() {
				@Override
				public Rectangle getArea() {
					return new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight());
				}

				@Override
				public void accept(I i) {
					fluidFilterControl.setFluid(slotIndex, ghostFluid.copy());
				}
			});
		}
	}

	@Override
	public void onComplete() {
		//noop
	}
}
