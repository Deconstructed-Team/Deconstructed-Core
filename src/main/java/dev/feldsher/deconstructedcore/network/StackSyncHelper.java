package dev.feldsher.deconstructedcore.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;

/** Vanilla writes a stack's count as a byte, and storage slots hold more than that: these write it as an int. */
final class StackSyncHelper {
	private StackSyncHelper() {}

	static void writeStack(ByteBuf buf, ItemStack stack) {
		ItemStack single = stack.copy();
		if (!stack.isEmpty()) {
			single.setCount(1);
		}
		ByteBufUtils.writeItemStack(buf, single);
		buf.writeInt(stack.getCount());
	}

	static ItemStack readStack(ByteBuf buf) {
		ItemStack stack = ByteBufUtils.readItemStack(buf);
		int count = buf.readInt();
		if (!stack.isEmpty()) {
			stack.setCount(count);
		}
		return stack;
	}
}
