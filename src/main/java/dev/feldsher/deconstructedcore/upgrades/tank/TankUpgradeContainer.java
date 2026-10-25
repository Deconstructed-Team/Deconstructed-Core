package dev.feldsher.deconstructedcore.upgrades.tank;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.common.gui.SlotSuppliedHandler;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TankUpgradeContainer extends UpgradeContainerBase<TankUpgradeWrapper, TankUpgradeContainer> {
	public static final String EMPTY_TANK_INPUT_SLOT_BACKGROUND = DeconstructedCore.MOD_ID + ":item/empty_tank_input_slot";
	public static final String EMPTY_TANK_OUTPUT_SLOT_BACKGROUND = DeconstructedCore.MOD_ID + ":item/empty_tank_output_slot";

	public TankUpgradeContainer(EntityPlayer player, int upgradeContainerId, TankUpgradeWrapper upgradeWrapper, UpgradeContainerType<TankUpgradeWrapper, TankUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
		slots.add(new TankIOSlot(TankUpgradeWrapper.INPUT_SLOT, EMPTY_TANK_INPUT_SLOT_BACKGROUND));
		slots.add(new TankIOSlot(TankUpgradeWrapper.OUTPUT_SLOT, EMPTY_TANK_OUTPUT_SLOT_BACKGROUND));
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		//noop
	}

	@Nullable
	public FluidStack getContents() {
		return upgradeWrapper.getContents();
	}

	public int getTankCapacity() {
		return upgradeWrapper.getTankCapacity();
	}

	private class TankIOSlot extends SlotSuppliedHandler {
		private TankIOSlot(int slot, String background) {
			super(() -> TankUpgradeContainer.this.upgradeWrapper.getInventory(), slot, -100, -100);
			setBackgroundName(background);
		}

		@Override
		public int getItemStackLimit(@Nonnull ItemStack stack) {
			return 1;
		}
	}
}
