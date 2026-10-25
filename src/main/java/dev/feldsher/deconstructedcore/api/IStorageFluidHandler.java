package dev.feldsher.deconstructedcore.api;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/**
 * A storage's tanks as one fluid handler; what its upgrades move may go past the limit others are given per
 * operation. 1.21 also fills and drains by fluid tag, which 1.12 doesn't have.
 */
public interface IStorageFluidHandler extends IFluidHandler {
	int fill(FluidStack resource, boolean doFill, boolean ignoreInOutLimit);

	@Nullable
	FluidStack drain(FluidStack resource, boolean doDrain, boolean ignoreInOutLimit);

	@Nullable
	FluidStack drain(int maxDrain, boolean doDrain, boolean ignoreInOutLimit);

	@Override
	default int fill(FluidStack resource, boolean doFill) {
		return fill(resource, doFill, false);
	}

	@Nullable
	@Override
	default FluidStack drain(FluidStack resource, boolean doDrain) {
		return drain(resource, doDrain, false);
	}

	@Nullable
	@Override
	default FluidStack drain(int maxDrain, boolean doDrain) {
		return drain(maxDrain, doDrain, false);
	}
}
