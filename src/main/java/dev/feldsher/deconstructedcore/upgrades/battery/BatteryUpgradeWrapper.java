package dev.feldsher.deconstructedcore.upgrades.battery;

import dev.feldsher.deconstructedcore.renderdata.BatteryRenderInfo;
import dev.feldsher.deconstructedcore.upgrades.IRenderedBatteryUpgrade;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.upgrades.IStackableContentsUpgrade;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Holds Forge Energy, in the upgrade's stack. Of the items in its two slots it drains the input one into itself and
 * charges the output one from itself, every tick.
 */
public class BatteryUpgradeWrapper extends UpgradeWrapperBase<BatteryUpgradeWrapper, BatteryUpgradeItem>
		implements IEnergyStorage, ITickableUpgrade, IStackableContentsUpgrade, IRenderedBatteryUpgrade {
	public static final int INPUT_SLOT = 0;
	public static final int OUTPUT_SLOT = 1;
	private static final String ENERGY_STORED_TAG = "energyStored";
	private static final String INVENTORY_TAG = "inventory";

	private final ItemStackHandler inventory;
	private int energyStored;

	public BatteryUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		inventory = new BatteryItemHandler();
		NBTTagCompound tag = upgrade.getTagCompound();
		if (tag != null && tag.hasKey(INVENTORY_TAG)) {
			NBTTagCompound inventoryTag = tag.getCompoundTag(INVENTORY_TAG).copy();
			inventoryTag.removeTag("Size");
			inventory.deserializeNBT(inventoryTag);
		}
		energyStored = getEnergyStored(upgrade);
	}

	public static int getEnergyStored(ItemStack upgrade) {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag == null ? 0 : tag.getInteger(ENERGY_STORED_TAG);
	}

	@Nullable
	private static IEnergyStorage getEnergyStorage(ItemStack stack) {
		return !stack.isEmpty() && stack.hasCapability(CapabilityEnergy.ENERGY, null) ? stack.getCapability(CapabilityEnergy.ENERGY, null) : null;
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		if (!isValid()) {
			return 0;
		}
		int ret = Math.max(0, Math.min(getMaxEnergyStored() - energyStored, Math.min(getMaxInOut(), maxReceive)));
		if (!simulate && ret > 0) {
			energyStored += ret;
			serializeEnergyStored();
		}
		return ret;
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		if (!isValid()) {
			return 0;
		}
		int ret = Math.max(0, Math.min(energyStored, Math.min(getMaxInOut(), maxExtract)));
		if (!simulate && ret > 0) {
			energyStored -= ret;
			serializeEnergyStored();
		}
		return ret;
	}

	private void serializeEnergyStored() {
		StorageNbt.getOrCreateTag(upgrade).setInteger(ENERGY_STORED_TAG, energyStored);
		save();
	}

	@Override
	public BatteryRenderInfo getBatteryRenderInfo() {
		return BatteryRenderInfo.of(energyStored, getMaxEnergyStored());
	}

	@Override
	public int getEnergyStored() {
		return energyStored;
	}

	@Override
	public int getMaxEnergyStored() {
		return upgradeItem.getMaxEnergyStored(storageWrapper);
	}

	@Override
	public boolean canExtract() {
		return true;
	}

	@Override
	public boolean canReceive() {
		return true;
	}

	private int getMaxInOut() {
		double stackMultiplier = upgradeItem.getAdjustedStackMultiplier(storageWrapper);
		int baseInOut = Math.max(1, upgradeItem.getBatteryUpgradeConfig().getMaxInputOutput() * storageWrapper.getNumberOfSlotRows());
		return stackMultiplier > (double) Integer.MAX_VALUE / baseInOut ? Integer.MAX_VALUE : (int) (baseInOut * stackMultiplier);
	}

	private static boolean isValidEnergyItem(ItemStack stack, boolean isOutput) {
		IEnergyStorage energyStorage = getEnergyStorage(stack);
		if (energyStorage == null) {
			return false;
		}
		return isOutput ? energyStorage.canReceive() : energyStorage.canExtract() && energyStorage.getEnergyStored() > 0;
	}

	@Override
	public void tick(@Nullable Entity entity, World world, BlockPos pos) {
		if (energyStored < getMaxEnergyStored()) {
			IEnergyStorage energyStorage = getEnergyStorage(inventory.getStackInSlot(INPUT_SLOT));
			if (energyStorage != null) {
				receiveFromStorage(energyStorage);
			}
		}

		if (energyStored > 0) {
			IEnergyStorage energyStorage = getEnergyStorage(inventory.getStackInSlot(OUTPUT_SLOT));
			if (energyStorage != null) {
				extractToStorage(energyStorage);
			}
		}
	}

	/** The item's stack is changed in place by its energy storage; set again, the handler saves it. */
	private void extractToStorage(IEnergyStorage energyStorage) {
		int toExtract = extractEnergy(getMaxInOut(), true);
		if (toExtract > 0) {
			toExtract = energyStorage.receiveEnergy(toExtract, true);
			if (toExtract > 0) {
				extractEnergy(energyStorage.receiveEnergy(toExtract, false), false);
				inventory.setStackInSlot(OUTPUT_SLOT, inventory.getStackInSlot(OUTPUT_SLOT));
			}
		}
	}

	private void receiveFromStorage(IEnergyStorage energyStorage) {
		int toReceive = receiveEnergy(getMaxInOut(), true);
		if (toReceive > 0) {
			toReceive = energyStorage.extractEnergy(toReceive, true);
			if (toReceive > 0) {
				receiveEnergy(energyStorage.extractEnergy(toReceive, false), false);
				inventory.setStackInSlot(INPUT_SLOT, inventory.getStackInSlot(INPUT_SLOT));
			}
		}
	}

	public IItemHandler getInventory() {
		return inventory;
	}

	@Override
	public int getMinimumMultiplierRequired() {
		return (int) Math.ceil((float) energyStored / upgradeItem.getMaxEnergyBase(storageWrapper));
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	/** One energy item in each slot, of those the slot's way works with; 1.12's insert doesn't ask whether it's valid. */
	private class BatteryItemHandler extends ItemStackHandler {
		private BatteryItemHandler() {
			super(2);
		}

		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			StorageNbt.getOrCreateTag(upgrade).setTag(INVENTORY_TAG, serializeNBT());
			save();
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			if (slot == INPUT_SLOT) {
				return stack.isEmpty() || isValidEnergyItem(stack, false);
			} else if (slot == OUTPUT_SLOT) {
				return stack.isEmpty() || isValidEnergyItem(stack, true);
			}
			return false;
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}
	}
}
