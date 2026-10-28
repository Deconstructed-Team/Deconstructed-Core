package dev.feldsher.deconstructedcore.upgrades.crafting;

import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.function.Supplier;
import net.minecraft.inventory.IInventory;

/**
 * The 3x3 crafting grid as recipes see it, over the upgrade's own slots, which are read each time as the wrapper
 * may be replaced. Changes go to the given callback rather than to a container.
 */
public class CraftingItemHandler extends InventoryCrafting {
	private static final Container NO_CONTAINER = new Container() {
		@Override
		public boolean canInteractWith(@Nonnull EntityPlayer player) {
			return false;
		}

		@Override
		public void onCraftMatrixChanged(IInventory inventory) {
			//noop
		}
	};

	private final Supplier<IItemHandlerModifiable> supplyInventory;
	private final Runnable onCraftingMatrixChanged;

	public CraftingItemHandler(Supplier<IItemHandlerModifiable> supplyInventory, Runnable onCraftingMatrixChanged) {
		super(NO_CONTAINER, 3, 3);
		this.supplyInventory = supplyInventory;
		this.onCraftingMatrixChanged = onCraftingMatrixChanged;
	}

	@Override
	public int getSizeInventory() {
		return supplyInventory.get().getSlots();
	}

	@Override
	public boolean isEmpty() {
		IItemHandlerModifiable inventory = supplyInventory.get();
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			if (!inventory.getStackInSlot(slot).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Nonnull
	@Override
	public ItemStack getStackInSlot(int index) {
		IItemHandlerModifiable inventory = supplyInventory.get();
		return index < 0 || index >= inventory.getSlots() ? ItemStack.EMPTY : inventory.getStackInSlot(index);
	}

	@Nonnull
	@Override
	public ItemStack getStackInRowAndColumn(int row, int column) {
		return row >= 0 && row < getWidth() && column >= 0 && column < getHeight() ? getStackInSlot(row + column * getWidth()) : ItemStack.EMPTY;
	}

	@Nonnull
	@Override
	public ItemStack removeStackFromSlot(int index) {
		ItemStack stack = getStackInSlot(index);
		supplyInventory.get().setStackInSlot(index, ItemStack.EMPTY);
		return stack;
	}

	@Nonnull
	@Override
	public ItemStack decrStackSize(int index, int count) {
		ItemStack stack = supplyInventory.get().extractItem(index, count, false);
		if (!stack.isEmpty()) {
			onCraftingMatrixChanged.run();
		}
		return stack;
	}

	@Override
	public void setInventorySlotContents(int index, @Nonnull ItemStack stack) {
		supplyInventory.get().setStackInSlot(index, stack);
		onCraftingMatrixChanged.run();
	}

	@Override
	public void clear() {
		IItemHandlerModifiable inventory = supplyInventory.get();
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			inventory.setStackInSlot(slot, ItemStack.EMPTY);
		}
	}

	@Override
	public void fillStackedContents(@Nonnull RecipeItemHelper helper) {
		IItemHandlerModifiable inventory = supplyInventory.get();
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			helper.accountStack(inventory.getStackInSlot(slot));
		}
	}
}
