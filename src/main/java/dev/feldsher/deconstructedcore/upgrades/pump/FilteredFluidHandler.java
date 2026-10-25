package dev.feldsher.deconstructedcore.upgrades.pump;

import dev.feldsher.deconstructedcore.api.IStorageFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

/** The storage's tanks as the pump sees them: only the fluids its filter lets through go in or out. */
public class FilteredFluidHandler implements IStorageFluidHandler {
	private final IStorageFluidHandler storageFluidHandler;
	private final FluidFilterLogic fluidFilterLogic;

	public FilteredFluidHandler(IStorageFluidHandler storageFluidHandler, FluidFilterLogic fluidFilterLogic) {
		this.storageFluidHandler = storageFluidHandler;
		this.fluidFilterLogic = fluidFilterLogic;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return storageFluidHandler.getTankProperties();
	}

	@Override
	public int fill(FluidStack resource, boolean doFill, boolean ignoreInOutLimit) {
		return fluidFilterLogic.fluidMatches(resource) ? storageFluidHandler.fill(resource, doFill, ignoreInOutLimit) : 0;
	}

	@Nullable
	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain, boolean ignoreInOutLimit) {
		return fluidFilterLogic.fluidMatches(resource) ? storageFluidHandler.drain(resource, doDrain, ignoreInOutLimit) : null;
	}

	/** Of the first tank whose fluid the filter lets through. */
	@Nullable
	@Override
	public FluidStack drain(int maxDrain, boolean doDrain, boolean ignoreInOutLimit) {
		for (IFluidTankProperties tank : getTankProperties()) {
			FluidStack contents = tank.getContents();
			if (contents != null && contents.amount > 0 && fluidFilterLogic.fluidMatches(contents)) {
				return storageFluidHandler.drain(new FluidStack(contents, maxDrain), doDrain, ignoreInOutLimit);
			}
		}
		return null;
	}
}
