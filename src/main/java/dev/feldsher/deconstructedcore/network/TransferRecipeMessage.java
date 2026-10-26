package dev.feldsher.deconstructedcore.network;

import dev.feldsher.deconstructedcore.common.gui.StorageContainerMenuBase;
import dev.feldsher.deconstructedcore.upgrades.crafting.CraftingRecipeTransfer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A recipe from a recipe viewer for the open storage's crafting upgrade: for each of the 3x3 grid's slots, the items
 * it accepts (none for an empty one), and the result shown, which is picked if the grid matches more than one.
 */
public class TransferRecipeMessage implements IMessage {
	private static final int MAX_ACCEPTED_PER_SLOT = 256;

	private int windowId;
	private List<List<ItemStack>> grid = new ArrayList<>();
	private ItemStack output = ItemStack.EMPTY;
	private boolean maxTransfer;

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public TransferRecipeMessage() {
	}

	public TransferRecipeMessage(int windowId, List<List<ItemStack>> grid, ItemStack output, boolean maxTransfer) {
		this.windowId = windowId;
		this.grid = grid;
		this.output = output;
		this.maxTransfer = maxTransfer;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		windowId = buf.readInt();
		int slots = buf.readUnsignedByte();
		grid = new ArrayList<>(slots);
		for (int slot = 0; slot < slots; slot++) {
			int count = buf.readUnsignedShort();
			List<ItemStack> accepted = new ArrayList<>(Math.min(count, MAX_ACCEPTED_PER_SLOT));
			for (int i = 0; i < count; i++) {
				ItemStack stack = ByteBufUtils.readItemStack(buf);
				if (i < MAX_ACCEPTED_PER_SLOT) {
					accepted.add(stack);
				}
			}
			grid.add(accepted);
		}
		output = ByteBufUtils.readItemStack(buf);
		maxTransfer = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(windowId);
		buf.writeByte(grid.size());
		for (List<ItemStack> accepted : grid) {
			int count = Math.min(accepted.size(), MAX_ACCEPTED_PER_SLOT);
			buf.writeShort(count);
			for (int i = 0; i < count; i++) {
				ByteBufUtils.writeItemStack(buf, accepted.get(i));
			}
		}
		ByteBufUtils.writeItemStack(buf, output);
		buf.writeBoolean(maxTransfer);
	}

	public static class Handler implements IMessageHandler<TransferRecipeMessage, IMessage> {
		@Override
		public IMessage onMessage(TransferRecipeMessage message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServerWorld().addScheduledTask(() -> {
				if (player.openContainer instanceof StorageContainerMenuBase && player.openContainer.windowId == message.windowId
							&& !((StorageContainerMenuBase<?>) player.openContainer).refuseIfOutdated()) {
					CraftingRecipeTransfer.transfer(player, (StorageContainerMenuBase<?>) player.openContainer, message.grid, message.output, message.maxTransfer);
				}
			});
			return null;
		}
	}
}
