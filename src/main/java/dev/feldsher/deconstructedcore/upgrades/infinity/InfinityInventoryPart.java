package dev.feldsher.deconstructedcore.upgrades.infinity;

import dev.feldsher.deconstructedcore.inventory.IInventoryPartHandler;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.inventory.SlotRange;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;

/**
 * Slots that, once given an item, hold endlessly many of it: taking leaves it as it was, and nothing more goes in.
 * Only a player with the part's permission level can give an empty slot its item.
 */
public abstract class InfinityInventoryPart implements IInventoryPartHandler {
	private final InventoryHandler parent;
	private final SlotRange slotRange;
	private final int permissionLevel;
	private final Map<Integer, ItemStack> cachedStacks = new HashMap<>();

	protected InfinityInventoryPart(InventoryHandler parent, SlotRange slotRange, int permissionLevel) {
		this.parent = parent;
		this.slotRange = slotRange;
		this.permissionLevel = permissionLevel;
	}

	@Override
	public boolean isInfinite(int slot) {
		return !parent.getSlotStack(slot).isEmpty();
	}

	@Override
	public int getSlotLimit(int slot) {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack, @Nullable EntityPlayer player, BiPredicate<Integer, ItemStack> isItemValidSuper) {
		return player != null && player.canUseCommand(permissionLevel, "") && parent.getSlotStack(slot).isEmpty() && isItemValidSuper.test(slot, stack);
	}

	@Override
	public boolean isSlotAccessible(int slot) {
		return true;
	}

	@Override
	public int getStackLimit(int slot, ItemStack stack) {
		return Integer.MAX_VALUE;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemHandlerHelper.copyStackWithSize(parent.getSlotStack(slot), amount);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate, SlotInserter insertSuper) {
		if (!parent.getSlotStack(slot).isEmpty()) {
			return stack;
		}
		cachedStacks.remove(slot);
		return insertSuper.insert(slot, stack, simulate);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotSuper) {
		if (parent.getSlotStack(slot).isEmpty()) {
			cachedStacks.remove(slot);
			parent.setSlotStack(slot, stack);
		}
	}

	@Override
	public ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
		ItemStack cachedStack = cachedStacks.get(slot);
		ItemStack slotStack = parent.getSlotStack(slot);
		if (cachedStack == null || cachedStack.isEmpty() != slotStack.isEmpty()) {
			cachedStack = slotStack.isEmpty() ? ItemStack.EMPTY : ItemHandlerHelper.copyStackWithSize(slotStack, Integer.MAX_VALUE);
			cachedStacks.put(slot, cachedStack);
		}
		return cachedStack;
	}

	@Override
	public int getSlots() {
		return slotRange.numberOfSlots();
	}

	/** Only admins, at permission level 2, give its slots their items. */
	public static class Admin extends InfinityInventoryPart {
		public static final String NAME = "infinity";

		protected Admin(InventoryHandler parent, SlotRange slotRange) {
			super(parent, slotRange, 2);
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

	public static class Survival extends InfinityInventoryPart {
		public static final String NAME = "survival_infinity";

		protected Survival(InventoryHandler parent, SlotRange slotRange) {
			super(parent, slotRange, 0);
		}

		@Override
		public String getName() {
			return NAME;
		}
	}
}
