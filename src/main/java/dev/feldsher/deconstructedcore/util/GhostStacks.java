package dev.feldsher.deconstructedcore.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A filter's ghost stack as a client sends it, from a recipe viewer or the memory settings. The server keeps it in an
 * upgrade's or the storage's NBT, so it takes no more of it than a filter needs: one item, and its tag only when that's
 * of a sane size. Else a client could fill items and the save with whatever it likes, as with books in vanilla.
 */
public final class GhostStacks {
	public static final int MAX_TAG_BYTES = 4096;

	private GhostStacks() {}

	public static ItemStack fromClient(NBTTagCompound stackNbt) {
		return asFilter(new ItemStack(stackNbt));
	}

	/** The ghost of a stack, as a filter slot keeps it: one item, with its tag only when that's of a sane size. */
	public static ItemStack asFilter(ItemStack stack) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack ghost = new ItemStack(stack.getItem(), 1, stack.getMetadata());
		if (hasFilterSizedTag(stack) && stack.getTagCompound() != null) {
			ghost.setTagCompound(stack.getTagCompound().copy());
		}
		return ghost;
	}

	/** Whether the stack's tag is of a size a filter keeps. */
	public static boolean hasFilterSizedTag(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return tag == null || getSize(tag) <= MAX_TAG_BYTES;
	}

	/** The tag's size as it's written: as it's sent, and saved. */
	public static int getSize(NBTTagCompound tag) {
		CountingStream counter = new CountingStream();
		try (DataOutputStream output = new DataOutputStream(counter)) {
			CompressedStreamTools.write(tag, output);
		} catch (IOException | RuntimeException e) {
			return Integer.MAX_VALUE;
		}
		return counter.count;
	}

	private static class CountingStream extends OutputStream {
		private int count;

		@Override
		public void write(int b) {
			count++;
		}

		@Override
		public void write(byte[] b, int off, int len) {
			count += len;
		}
	}
}
