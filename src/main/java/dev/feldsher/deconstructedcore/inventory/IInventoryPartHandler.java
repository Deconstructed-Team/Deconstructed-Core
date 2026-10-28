package dev.feldsher.deconstructedcore.inventory;

import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * How a run of an inventory's slots behaves, as the {@link InventoryPartitioner} assigns it; the default part is a
 * plain inventory, others take over limits, insertion and extraction. The handler's own behavior is passed in as the
 * "super" functions, so that a part can fall back to it.
 */
public interface IInventoryPartHandler {
	IInventoryPartHandler EMPTY = () -> "EMPTY";

	default int getSlotLimit(int slot) {
		return 0;
	}

	default boolean isSlotAccessible(int slot) {
		return false;
	}

	default int getStackLimit(int slot, ItemStack stack) {
		return 0;
	}

	default ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemStack.EMPTY;
	}

	default ItemStack insertItem(int slot, ItemStack stack, boolean simulate, SlotInserter insertSuper) {
		return stack;
	}

	default void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotSuper) {
		//noop
	}

	default boolean isItemValid(int slot, ItemStack stack, @Nullable EntityPlayer player, BiPredicate<Integer, ItemStack> isItemValidSuper) {
		return false;
	}

	default ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
		return ItemStack.EMPTY;
	}

	default boolean canBeReplaced() {
		return false;
	}

	default int getSlots() {
		return 0;
	}

	String getName();

	/** The sprite, on the blocks atlas, an empty slot of this part shows; null for none. */
	@Nullable
	default String getNoItemIcon(int slot) {
		return null;
	}

	/** The item this part has set the slot for, which it then shows when empty and takes alone; air for none. */
	default Item getFilterItem(int slot) {
		return Items.AIR;
	}

	default void onSlotLimitChange() {
		//noop
	}

	default Set<Integer> getNoSortSlots() {
		return Collections.emptySet();
	}

	default void onSlotFilterChanged(int slot) {
		//noop
	}

	default boolean isFilterItem(Item item) {
		return false;
	}

	default Map<Item, Set<Integer>> getFilterItems() {
		return Collections.emptyMap();
	}

	/**
	 * Whether its slots hold one pool between them, as compression's: what goes into one is what the part takes, and
	 * a simulated insert into each of them would count the same room again.
	 */
	default boolean sharesCapacityAcrossSlots() {
		return false;
	}

	default void onInit() {
		//noop
	}

	default boolean isInfinite(int slot) {
		return false;
	}

	interface SlotInserter {
		ItemStack insert(int slot, ItemStack stack, boolean simulate);
	}

	class Default implements IInventoryPartHandler {
		public static final String NAME = "default";
		private final InventoryHandler parent;
		private final int slots;

		public Default(InventoryHandler parent, int slots) {
			this.parent = parent;
			this.slots = slots;
		}

		@Override
		public int getSlotLimit(int slot) {
			return parent.getBaseSlotLimit();
		}

		@Override
		public int getStackLimit(int slot, ItemStack stack) {
			return parent.getBaseStackLimit(stack);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return parent.extractItemInternal(slot, amount, simulate);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate, SlotInserter insertSuper) {
			return insertSuper.insert(slot, stack, simulate);
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotSuper) {
			setStackInSlotSuper.accept(slot, stack);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack, @Nullable EntityPlayer player, BiPredicate<Integer, ItemStack> isItemValidSuper) {
			return true;
		}

		@Override
		public ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
			return getStackInSlotSuper.apply(slot);
		}

		@Override
		public boolean canBeReplaced() {
			return true;
		}

		@Override
		public boolean isSlotAccessible(int slot) {
			return true;
		}

		@Override
		public int getSlots() {
			return slots;
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

	interface Factory {
		IInventoryPartHandler create(InventoryHandler parent, SlotRange slotRange, Supplier<MemorySettingsCategory> getMemorySettings);
	}
}
