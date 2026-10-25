package dev.feldsher.deconstructedcore.upgrades.battery;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.common.gui.SlotSuppliedHandler;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

public class BatteryUpgradeContainer extends UpgradeContainerBase<BatteryUpgradeWrapper, BatteryUpgradeContainer> {
	public static final String EMPTY_BATTERY_INPUT_SLOT_BACKGROUND = DeconstructedCore.MOD_ID + ":item/empty_battery_input_slot";
	public static final String EMPTY_BATTERY_OUTPUT_SLOT_BACKGROUND = DeconstructedCore.MOD_ID + ":item/empty_battery_output_slot";

	public BatteryUpgradeContainer(EntityPlayer player, int upgradeContainerId, BatteryUpgradeWrapper upgradeWrapper, UpgradeContainerType<BatteryUpgradeWrapper, BatteryUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
		slots.add(new BatteryIOSlot(BatteryUpgradeWrapper.INPUT_SLOT, EMPTY_BATTERY_INPUT_SLOT_BACKGROUND));
		slots.add(new BatteryIOSlot(BatteryUpgradeWrapper.OUTPUT_SLOT, EMPTY_BATTERY_OUTPUT_SLOT_BACKGROUND));
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		//noop
	}

	public int getEnergyStored() {
		return upgradeWrapper.getEnergyStored();
	}

	public int getMaxEnergyStored() {
		return upgradeWrapper.getMaxEnergyStored();
	}

	private class BatteryIOSlot extends SlotSuppliedHandler {
		private BatteryIOSlot(int slot, String background) {
			super(() -> BatteryUpgradeContainer.this.upgradeWrapper.getInventory(), slot, -100, -100);
			setBackgroundName(background);
		}

		@Override
		public int getItemStackLimit(@Nonnull ItemStack stack) {
			return 1;
		}
	}
}
