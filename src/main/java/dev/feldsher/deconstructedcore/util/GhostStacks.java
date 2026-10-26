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
		ItemStack sent = new ItemStack(stackNbt);
		if (sent.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack ghost = new ItemStack(sent.getItem(), 1, sent.getMetadata());
		NBTTagCompound tag = sent.getTagCompound();
		if (tag != null && getSize(tag) <= MAX_TAG_BYTES) {
			ghost.setTagCompound(tag);
		}
		return ghost;
	}

	private static int getSize(NBTTagCompound tag) {
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
