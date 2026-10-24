package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.common.gui.FilterSlotItemHandler;
import dev.feldsher.deconstructedcore.common.gui.IServerUpdater;
import net.minecraft.inventory.Slot;
import net.minecraftforge.items.IItemHandler;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FilterLogicContainer<T extends FilterLogic> extends FilterLogicContainerBase<T, FilterLogicContainer.FilterLogicSlot> {
	public FilterLogicContainer(Supplier<T> filterLogic, IServerUpdater serverUpdater, Consumer<Slot> addSlot) {
		super(serverUpdater, filterLogic, addSlot);
		int slots = filterLogic.get().getFilterHandler().getSlots();
		for (int slot = 0; slot < slots; slot++) {
			FilterLogicSlot filterSlot = new FilterLogicSlot(() -> filterLogic.get().getFilterHandler(), slot);
			addSlot.accept(filterSlot);
			filterSlots.add(filterSlot);
		}
	}

	public static class FilterLogicSlot extends FilterSlotItemHandler {
		private boolean enabled = true;

		public FilterLogicSlot(Supplier<IItemHandler> filterHandler, int slot) {
			super(filterHandler, slot, -100, -100);
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public boolean isEnabled() {
			return enabled;
		}
	}
}
