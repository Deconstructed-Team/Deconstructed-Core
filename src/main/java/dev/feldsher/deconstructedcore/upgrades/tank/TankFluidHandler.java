package dev.feldsher.deconstructedcore.upgrades.tank;

import dev.feldsher.deconstructedcore.api.IStorageFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

/** One tank upgrade as a fluid handler, for what is moved to or from it alone. */
public class TankFluidHandler implements IStorageFluidHandler {
	private final TankUpgradeWrapper tank;

	public TankFluidHandler(TankUpgradeWrapper tank) {
		this.tank = tank;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		FluidStack contents = tank.getContents();
		return new IFluidTankProperties[] {new FluidTankProperties(contents == null ? null : contents.copy(), tank.getTankCapacity())};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill, boolean ignoreInOutLimit) {
		return tank.fill(resource, doFill, ignoreInOutLimit);
	}

	@Nullable
	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain, boolean ignoreInOutLimit) {
		return resource != null && resource.isFluidEqual(tank.getContents()) ? tank.drain(resource.amount, doDrain, ignoreInOutLimit) : null;
	}

	@Nullable
	@Override
	public FluidStack drain(int maxDrain, boolean doDrain, boolean ignoreInOutLimit) {
		return tank.drain(maxDrain, doDrain, ignoreInOutLimit);
	}
}
