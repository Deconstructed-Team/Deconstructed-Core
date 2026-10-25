package dev.feldsher.deconstructedcore.util;

import dev.feldsher.deconstructedcore.api.IStorageFluidHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Fills and empties fluid containers, anything with Forge's item fluid handler: buckets, mods' tanks and cells.
 * <p>
 * Always one container of a stack: a handler reads the stack as given, and one of several buckets would answer for
 * all of them while filling only itself. Both sides are simulated first, the container then gives or takes first,
 * and what the other side doesn't take after all goes back, so that no fluid is made or lost on the way.
 */
public final class FluidContainerHelper {
	private FluidContainerHelper() {}

	@Nullable
	private static IFluidHandlerItem getHandlerOfOne(ItemStack container) {
		if (container.isEmpty()) {
			return null;
		}
		ItemStack one = container.copy();
		one.setCount(1);
		return FluidUtil.getFluidHandler(one);
	}

	/** What one container of the stack can give, whichever fluid it is; null when nothing. */
	@Nullable
	public static FluidStack getDrainable(ItemStack container) {
		IFluidHandlerItem handler = getHandlerOfOne(container);
		FluidStack drainable = handler == null ? null : handler.drain(Integer.MAX_VALUE, false);
		return drainable == null || drainable.amount <= 0 ? null : drainable;
	}

	/** Whether one container of the stack takes some of the fluid, or, for none given, has room for any fluid. */
	public static boolean canFill(ItemStack container, @Nullable FluidStack fluid) {
		IFluidHandlerItem handler = getHandlerOfOne(container);
		if (handler == null) {
			return false;
		}
		if (fluid != null) {
			return handler.fill(new FluidStack(fluid, Integer.MAX_VALUE), false) > 0;
		}
		for (IFluidTankProperties tank : handler.getTankProperties()) {
			FluidStack contents = tank.getContents();
			if (tank.canFill() && (contents == null || contents.amount < tank.getCapacity())) {
				return true;
			}
		}
		return false;
	}

	/** Fills one container of the stack from the source, up to maxAmount; the result holds that filled container. */
	public static FluidActionResult fillContainer(ItemStack container, IStorageFluidHandler source, int maxAmount, boolean ignoreInOutLimit) {
		IFluidHandlerItem handler = getHandlerOfOne(container);
		if (handler == null) {
			return FluidActionResult.FAILURE;
		}
		FluidStack drainable = source.drain(maxAmount, false, ignoreInOutLimit);
		if (drainable == null || drainable.amount <= 0) {
			return FluidActionResult.FAILURE;
		}
		int room = handler.fill(drainable, false);
		if (room <= 0) {
			return FluidActionResult.FAILURE;
		}
		// By the fluid, not just an amount: a source of several fluids mustn't hand out another one
		FluidStack drained = source.drain(new FluidStack(drainable, room), true, ignoreInOutLimit);
		if (drained == null || drained.amount <= 0) {
			return FluidActionResult.FAILURE;
		}
		int filled = handler.fill(drained, true);
		if (filled < drained.amount) {
			source.fill(new FluidStack(drained, drained.amount - filled), true, true);
		}
		return filled > 0 ? new FluidActionResult(handler.getContainer()) : FluidActionResult.FAILURE;
	}

	/** Empties one container of the stack into the destination, up to maxAmount; the result holds that emptied container. */
	public static FluidActionResult emptyContainer(ItemStack container, IStorageFluidHandler destination, int maxAmount, boolean ignoreInOutLimit) {
		IFluidHandlerItem handler = getHandlerOfOne(container);
		if (handler == null) {
			return FluidActionResult.FAILURE;
		}
		FluidStack drainable = handler.drain(maxAmount, false);
		if (drainable == null || drainable.amount <= 0) {
			return FluidActionResult.FAILURE;
		}
		int room = destination.fill(drainable, false, ignoreInOutLimit);
		if (room <= 0) {
			return FluidActionResult.FAILURE;
		}
		// A container that gives only whole amounts, as a bucket, gives nothing when there's room for less
		FluidStack drained = handler.drain(new FluidStack(drainable, room), true);
		if (drained == null || drained.amount <= 0) {
			return FluidActionResult.FAILURE;
		}
		int inserted = destination.fill(drained, true, ignoreInOutLimit);
		if (inserted < drained.amount) {
			handler.fill(new FluidStack(drained, drained.amount - inserted), true);
		}
		return inserted > 0 ? new FluidActionResult(handler.getContainer()) : FluidActionResult.FAILURE;
	}

	/**
	 * Moves up to maxAmount from one fluid handler to another, the same way: simulated both ways, drained by the
	 * fluid, and what the destination doesn't take after all given back. True when something moved.
	 */
	public static boolean moveFluid(IFluidHandler source, IFluidHandler destination, int maxAmount) {
		FluidStack drainable = source.drain(maxAmount, false);
		if (drainable == null || drainable.amount <= 0) {
			return false;
		}
		int room = destination.fill(drainable, false);
		if (room <= 0) {
			return false;
		}
		FluidStack drained = source.drain(new FluidStack(drainable, room), true);
		if (drained == null || drained.amount <= 0) {
			return false;
		}
		int filled = destination.fill(drained, true);
		if (filled < drained.amount) {
			source.fill(new FluidStack(drained, drained.amount - filled), true);
		}
		return filled > 0;
	}

	/**
	 * Where the container that was filled or emptied goes: in place of the stack it was one of, when that was all
	 * of it, or else to the player's inventory, or at their feet, with the rest of the stack left in place.
	 */
	public static void settleResult(EntityPlayer player, ItemStack original, ItemStack result, Consumer<ItemStack> setInPlace) {
		if (original.getCount() <= 1) {
			setInPlace.accept(result);
			return;
		}
		ItemStack remainder = original.copy();
		remainder.shrink(1);
		setInPlace.accept(remainder);
		if (!result.isEmpty() && !player.inventory.addItemStackToInventory(result)) {
			player.dropItem(result, false);
		}
	}
}
