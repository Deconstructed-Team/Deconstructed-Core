package dev.feldsher.deconstructedcore.compat.hei;

import dev.feldsher.deconstructedcore.client.gui.SettingsScreen;
import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsContainer;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsTab;
import mezz.jei.api.gui.IGhostIngredientHandler;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/** With the memory tab open, an item dragged from HEI onto an empty slot makes it remember the item. */
public class SettingsGhostIngredientHandler<S extends SettingsScreen> implements IGhostIngredientHandler<S> {
	@Override
	public <I> List<Target<I>> getTargets(S gui, I ingredient, boolean doStart) {
		List<Target<I>> targets = new ArrayList<>();
		if (!(ingredient instanceof ItemStack)) {
			return targets;
		}
		ItemStack ghostStack = (ItemStack) ingredient;
		gui.getSettingsTabControl().flatMap(tabControl -> tabControl.getOpenTab()).ifPresent(tab -> {
			if (!(tab instanceof MemorySettingsTab)) {
				return;
			}
			MemorySettingsContainer memoryContainer = ((MemorySettingsTab) tab).getMemorySettingsContainer();
			for (Slot slot : gui.getMenu().getStorageInventorySlots()) {
				if (!slot.getHasStack() && slot.xPos != StorageScreenBase.DISABLED_SLOT_X_POS) {
					targets.add(new Target<I>() {
						@Override
						public Rectangle getArea() {
							return new Rectangle(gui.getGuiLeft() + slot.xPos, gui.getGuiTop() + slot.yPos, 17, 17);
						}

						@Override
						public void accept(I i) {
							memoryContainer.setFilter(slot.slotNumber, ghostStack);
						}
					});
				}
			}
		});
		return targets;
	}

	@Override
	public void onComplete() {
		//noop
	}
}
