package dev.feldsher.deconstructedcore.inventory;

import net.minecraft.item.ItemStack;

import java.util.Objects;

/** A stack as a map key: its item, metadata and NBT, not its count. */
public final class ItemStackKey {
	private final ItemStack stack;
	private final int hash;

	private ItemStackKey(ItemStack stack) {
		this.stack = stack.copy();
		this.stack.setCount(1);
		hash = Objects.hash(stack.getItem(), stack.getMetadata(), stack.getTagCompound());
	}

	public static ItemStackKey of(ItemStack stack) {
		return new ItemStackKey(stack);
	}

	public ItemStack getStack() {
		return stack;
	}

	public boolean matches(ItemStack otherStack) {
		return ItemStack.areItemsEqual(stack, otherStack) && ItemStack.areItemStackTagsEqual(stack, otherStack);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		return matches(((ItemStackKey) o).stack);
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public String toString() {
		return "ItemStackKey[stack=" + stack + ']';
	}
}
