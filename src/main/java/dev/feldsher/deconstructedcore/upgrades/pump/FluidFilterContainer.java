package dev.feldsher.deconstructedcore.upgrades.pump;

import dev.feldsher.deconstructedcore.common.gui.IServerUpdater;
import dev.feldsher.deconstructedcore.util.FluidContainerHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** The fluid filter's slots: a click with a fluid container sets the fluid it holds, an empty hand clears it. */
public class FluidFilterContainer {
	private static final String DATA_FLUID = "setFluid";
	private final EntityPlayer player;
	private final IServerUpdater serverUpdater;
	private final Supplier<FluidFilterLogic> fluidFilterLogic;

	public FluidFilterContainer(EntityPlayer player, IServerUpdater serverUpdater, Supplier<FluidFilterLogic> fluidFilterLogic) {
		this.player = player;
		this.serverUpdater = serverUpdater;
		this.fluidFilterLogic = fluidFilterLogic;
	}

	@Nullable
	public FluidStack getFluid(int index) {
		return fluidFilterLogic.get().getFluid(index);
	}

	public void setFluid(int index, @Nullable FluidStack fluid) {
		fluidFilterLogic.get().setFluid(index, fluid);
		serverUpdater.sendDataToServer(() -> {
			NBTTagCompound fluidNbt = new NBTTagCompound();
			fluidNbt.setInteger("index", index);
			if (fluid != null) {
				fluidNbt.setTag("fluid", fluid.writeToNBT(new NBTTagCompound()));
			}
			NBTTagCompound ret = new NBTTagCompound();
			ret.setTag(DATA_FLUID, fluidNbt);
			return ret;
		});
	}

	public boolean handlePacket(NBTTagCompound data) {
		if (data.hasKey(DATA_FLUID)) {
			NBTTagCompound fluidData = data.getCompoundTag(DATA_FLUID);
			setFluid(fluidData.getInteger("index"), FluidStack.loadFluidStackFromNBT(fluidData.getCompoundTag("fluid")));
			return true;
		}
		return false;
	}

	public int getNumberOfFluidFilters() {
		return fluidFilterLogic.get().getNumberOfFluidFilters();
	}

	public void slotClick(int index) {
		if (player.inventory.getItemStack().isEmpty()) {
			setFluid(index, null);
			return;
		}
		FluidStack containedFluid = FluidContainerHelper.getDrainable(player.inventory.getItemStack());
		if (containedFluid != null) {
			setFluid(index, containedFluid);
		}
	}
}
