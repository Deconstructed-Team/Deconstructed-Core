package dev.feldsher.deconstructedcore.upgrades.pump;

import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.Consumer;

/** The fluids an upgrade works with, in the upgrade's stack; with none set, any. Empty filters are null. */
public class FluidFilterLogic {
	private static final String FLUID_FILTERS_TAG = "fluidFilters";
	private final FluidStack[] fluidFilters;
	private final ItemStack upgrade;
	private final Consumer<ItemStack> saveHandler;
	private boolean noFilter = true;

	public FluidFilterLogic(int filterSlots, ItemStack upgrade, Consumer<ItemStack> saveHandler) {
		fluidFilters = new FluidStack[filterSlots];
		this.upgrade = upgrade;
		this.saveHandler = saveHandler;
		deserializeFluidFilters();
		updateNoFilter();
	}

	private void deserializeFluidFilters() {
		NBTTagCompound tag = upgrade.getTagCompound();
		if (tag == null) {
			return;
		}
		NBTTagList filters = tag.getTagList(FLUID_FILTERS_TAG, Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < filters.tagCount() && i < fluidFilters.length; i++) {
			fluidFilters[i] = FluidStack.loadFluidStackFromNBT(filters.getCompoundTagAt(i));
		}
	}

	private void updateNoFilter() {
		noFilter = Arrays.stream(fluidFilters).allMatch(f -> f == null);
	}

	public boolean fluidMatches(@Nullable FluidStack fluid) {
		if (fluid == null) {
			return false;
		}
		if (noFilter) {
			return true;
		}
		for (FluidStack fluidFilter : fluidFilters) {
			if (fluidFilter != null && fluidFilter.isFluidEqual(fluid)) {
				return true;
			}
		}
		return false;
	}

	public void setFluid(int index, @Nullable FluidStack fluid) {
		if (index < 0 || index >= fluidFilters.length) {
			return;
		}
		fluidFilters[index] = fluid == null ? null : new FluidStack(fluid, 1);
		serializeFluidFilters();
		updateNoFilter();
		saveHandler.accept(upgrade);
	}

	@Nullable
	public FluidStack getFluid(int index) {
		return fluidFilters[index];
	}

	public int getNumberOfFluidFilters() {
		return fluidFilters.length;
	}

	private void serializeFluidFilters() {
		NBTTagList filters = new NBTTagList();
		for (FluidStack fluidFilter : fluidFilters) {
			filters.appendTag(fluidFilter == null ? new NBTTagCompound() : fluidFilter.writeToNBT(new NBTTagCompound()));
		}
		StorageNbt.getOrCreateTag(upgrade).setTag(FLUID_FILTERS_TAG, filters);
	}
}
