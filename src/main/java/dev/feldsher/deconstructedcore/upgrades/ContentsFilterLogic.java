package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A filter that can also go by what the storage holds. The memory settings' slots count as held once they come.
 */
public class ContentsFilterLogic extends FilterLogic {
	private static final String FILTER_BY_STORAGE = "filter_by_storage";
	private final Supplier<InventoryHandler> getInventoryHandler;

	public ContentsFilterLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, int filterSlotCount, Supplier<InventoryHandler> getInventoryHandler, String attributesKey) {
		super(upgrade, saveHandler, filterSlotCount, attributesKey);
		this.getInventoryHandler = getInventoryHandler;
	}

	public ContentsFilterType getFilterType() {
		if (shouldFilterByStorage()) {
			return ContentsFilterType.STORAGE;
		}
		return isAllowList() ? ContentsFilterType.ALLOW : ContentsFilterType.BLOCK;
	}

	public void setDepositFilterType(ContentsFilterType contentsFilterType) {
		switch (contentsFilterType) {
			case ALLOW:
				getOrCreateAttributes().setBoolean(FILTER_BY_STORAGE, false);
				setAllowList(true);
				break;
			case BLOCK:
				getOrCreateAttributes().setBoolean(FILTER_BY_STORAGE, false);
				setAllowList(false);
				break;
			case STORAGE:
				setBoolean(FILTER_BY_STORAGE, true);
				break;
		}
	}

	@Override
	public boolean matchesFilter(ItemStack stack) {
		if (!shouldFilterByStorage()) {
			return super.matchesFilter(stack);
		}

		InventoryHandler inventoryHandler = getInventoryHandler.get();
		for (int slot = 0; slot < inventoryHandler.getSlots(); slot++) {
			ItemStack slotStack = inventoryHandler.getStackInSlot(slot);
			if (!slotStack.isEmpty() && stackMatchesFilter(stack, slotStack)) {
				return true;
			}
		}
		return false;
	}

	protected boolean shouldFilterByStorage() {
		return getBoolean(FILTER_BY_STORAGE, false);
	}
}
