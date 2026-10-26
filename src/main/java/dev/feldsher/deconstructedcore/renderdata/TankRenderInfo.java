package dev.feldsher.deconstructedcore.renderdata;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Optional;

/** A shown tank: its fluid and how full it is, in tenths, so that a tank that fills slowly isn't sent every tick. */
public final class TankRenderInfo {
	private static final String FLUID_TAG = "fluid";
	private static final String FILL_RATIO_TAG = "fillRatio";
	public static final TankRenderInfo EMPTY = new TankRenderInfo(null, 0);

	@Nullable
	private final FluidStack fluid;
	private final float fillRatio;

	public TankRenderInfo(@Nullable FluidStack fluid, float fillRatio) {
		this.fluid = fluid == null ? null : fluid.copy();
		this.fillRatio = fillRatio;
	}

	public static TankRenderInfo of(@Nullable FluidStack contents, int capacity) {
		if (contents == null || contents.amount <= 0 || capacity <= 0) {
			return EMPTY;
		}
		return new TankRenderInfo(new FluidStack(contents, 1), (float) Math.round((float) contents.amount / capacity * 10) / 10);
	}

	public Optional<FluidStack> getFluid() {
		return Optional.ofNullable(fluid);
	}

	public float getFillRatio() {
		return fillRatio;
	}

	NBTTagCompound serialize() {
		NBTTagCompound tag = new NBTTagCompound();
		if (fluid != null) {
			tag.setTag(FLUID_TAG, fluid.writeToNBT(new NBTTagCompound()));
			tag.setFloat(FILL_RATIO_TAG, fillRatio);
		}
		return tag;
	}

	static TankRenderInfo deserialize(NBTTagCompound tag) {
		FluidStack fluid = tag.hasKey(FLUID_TAG) ? FluidStack.loadFluidStackFromNBT(tag.getCompoundTag(FLUID_TAG)) : null;
		return fluid == null ? EMPTY : new TankRenderInfo(fluid, tag.getFloat(FILL_RATIO_TAG));
	}
}
