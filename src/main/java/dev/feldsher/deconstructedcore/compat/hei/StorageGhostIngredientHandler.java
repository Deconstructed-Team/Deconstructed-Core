package dev.feldsher.deconstructedcore.compat.hei;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.common.gui.IFilterSlot;
import dev.feldsher.deconstructedcore.common.gui.StorageContainerMenuBase;
import mezz.jei.api.gui.IGhostIngredientHandler;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/** An item dragged from HEI onto a filter slot of the open upgrade tab sets it, as a click with the item would. */
public class StorageGhostIngredientHandler<S extends StorageScreenBase<?>> implements IGhostIngredientHandler<S> {
	@Override
	public <I> List<Target<I>> getTargets(S gui, I ingredient, boolean doStart) {
		List<Target<I>> targets = new ArrayList<>();
		if (!(ingredient instanceof ItemStack)) {
			return targets;
		}
		ItemStack ghostStack = (ItemStack) ingredient;
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

	@Override
	public void onComplete() {
		//noop
	}
}
