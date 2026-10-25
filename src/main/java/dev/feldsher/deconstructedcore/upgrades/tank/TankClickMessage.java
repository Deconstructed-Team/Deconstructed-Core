package dev.feldsher.deconstructedcore.upgrades.tank;

import dev.feldsher.deconstructedcore.common.gui.StorageContainerMenuBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.function.Consumer;

/** The tank of the upgrade in the slot was clicked with a fluid container: it fills the container, or empties it. */
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
		if (cursorStack.getCount() > 1) {
			return;
		}
		IFluidHandlerItem fluidHandler = FluidUtil.getFluidHandler(cursorStack);
		if (fluidHandler == null) {
			return;
		}

		TankUpgradeWrapper tankWrapper = ((TankUpgradeContainer) upgradeContainer).getUpgradeWrapper();
		Consumer<ItemStack> setCursorStack = stack -> {
			player.inventory.setItemStack(stack);
			player.updateHeldItem();
		};
		if (tankWrapper.getContents() == null || !tankWrapper.fillHandler(fluidHandler, setCursorStack)) {
			tankWrapper.drainHandler(fluidHandler, setCursorStack);
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
