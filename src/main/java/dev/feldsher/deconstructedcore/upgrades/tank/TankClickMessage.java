package dev.feldsher.deconstructedcore.upgrades.tank;

import dev.feldsher.deconstructedcore.api.IStorageFluidHandler;
import dev.feldsher.deconstructedcore.common.gui.StorageContainerMenuBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.util.FluidContainerHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * The tank of the upgrade in the slot was clicked with a fluid container: it fills one container of the held stack,
 * or else empties it.
 */
public class TankClickMessage implements IMessage {
	private int upgradeSlot;

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public TankClickMessage() {
	}

	public TankClickMessage(int upgradeSlot) {
		this.upgradeSlot = upgradeSlot;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		upgradeSlot = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(upgradeSlot);
	}

	private static void handle(EntityPlayerMP player, int upgradeSlot) {
		if (!(player.openContainer instanceof StorageContainerMenuBase<?>)) {
			return;
		}
		UpgradeContainerBase<?, ?> upgradeContainer = ((StorageContainerMenuBase<?>) player.openContainer).getUpgradeContainers().get(upgradeSlot);
		if (!(upgradeContainer instanceof TankUpgradeContainer)) {
			return;
		}
		ItemStack cursorStack = player.inventory.getItemStack();
		TankUpgradeWrapper tankWrapper = ((TankUpgradeContainer) upgradeContainer).getUpgradeWrapper();
		IStorageFluidHandler tank = tankWrapper.getFluidHandler();
		// By hand, as much as the container holds or has room for, not a bucket's worth an operation as automation
		FluidActionResult result = tankWrapper.getContents() == null ? FluidActionResult.FAILURE : FluidContainerHelper.fillContainer(cursorStack, tank, Integer.MAX_VALUE, true);
		if (!result.isSuccess()) {
			result = FluidContainerHelper.emptyContainer(cursorStack, tank, Integer.MAX_VALUE, true);
		}
		if (result.isSuccess()) {
			FluidContainerHelper.settleResult(player, cursorStack, result.getResult(), stack -> {
				player.inventory.setItemStack(stack);
				player.updateHeldItem();
			});
		}
	}

	public static class Handler implements IMessageHandler<TankClickMessage, IMessage> {
		@Override
		public IMessage onMessage(TankClickMessage message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServerWorld().addScheduledTask(() -> handle(player, message.upgradeSlot));
			return null;
		}
	}
}
