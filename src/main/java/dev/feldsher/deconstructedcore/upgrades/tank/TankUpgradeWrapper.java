package dev.feldsher.deconstructedcore.upgrades.tank;

import dev.feldsher.deconstructedcore.renderdata.TankRenderInfo;
import dev.feldsher.deconstructedcore.upgrades.IRenderedTankUpgrade;
import dev.feldsher.deconstructedcore.api.IStorageFluidHandler;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.upgrades.IStackableContentsUpgrade;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.util.FluidContainerHelper;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Holds one fluid, in the upgrade's stack. Of the containers in its two slots it empties the input one into the
 * tank and fills the output one from it, a bucket's worth after each cooldown.
 */
public class TankUpgradeWrapper extends UpgradeWrapperBase<TankUpgradeWrapper, TankUpgradeItem> implements ITickableUpgrade, IStackableContentsUpgrade, IRenderedTankUpgrade {
	public static final int INPUT_SLOT = 0;
	public static final int OUTPUT_SLOT = 1;
	private static final String CONTENTS_TAG = "contents";
	private static final String INVENTORY_TAG = "inventory";

	private final ItemStackHandler inventory;
	@Nullable
	private FluidStack contents;
	private long cooldownTime = 0;

	public TankUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		inventory = new TankItemHandler();
		NBTTagCompound tag = upgrade.getTagCompound();
		if (tag != null && tag.hasKey(INVENTORY_TAG)) {
			NBTTagCompound inventoryTag = tag.getCompoundTag(INVENTORY_TAG).copy();
			inventoryTag.removeTag("Size");
			inventory.deserializeNBT(inventoryTag);
		}
		contents = getContents(upgrade);
	}

	/** What the upgrade stack holds; null when empty. */
	@Nullable
	public static FluidStack getContents(ItemStack upgrade) {
		NBTTagCompound tag = upgrade.getTagCompound();
		if (tag == null || !tag.hasKey(CONTENTS_TAG)) {
			return null;
		}
		FluidStack fluidStack = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag(CONTENTS_TAG));
		return fluidStack == null || fluidStack.amount <= 0 ? null : fluidStack;
	}

	/** An input container gives a fluid the tank takes; an output one takes the tank's fluid, or any while it's empty. */
	private boolean isValidFluidItem(ItemStack stack, boolean isOutput) {
		if (isOutput) {
			return FluidContainerHelper.canFill(stack, contents);
		}
		FluidStack drainable = FluidContainerHelper.getDrainable(stack);
		return drainable != null && (contents == null || contents.isFluidEqual(drainable));
	}

	/** This tank alone, as a fluid handler. */
	public IStorageFluidHandler getFluidHandler() {
		return new TankFluidHandler(this);
	}

	@Nullable
	@Override
	public TankRenderInfo getTankRenderInfo() {
		return TankRenderInfo.of(getContents(), getTankCapacity());
	}

	public FluidStack getContents() {
		return contents;
	}

	public int getTankCapacity() {
		return upgradeItem.getTankCapacity(storageWrapper);
	}

	public IItemHandler getInventory() {
		return inventory;
	}

	private int getMaxInOut() {
		return (int) Math.max(Fluid.BUCKET_VOLUME, upgradeItem.getTankUpgradeConfig().getMaxInputOutput() * storageWrapper.getNumberOfSlotRows()
				* upgradeItem.getAdjustedStackMultiplier(storageWrapper));
	}

	public int fill(@Nullable FluidStack resource, boolean doFill, boolean ignoreInOutLimit) {
		if (resource == null || resource.amount <= 0) {
			return 0;
		}
		int capacity = getTankCapacity();
		int amount = contents == null ? 0 : contents.amount;
		if (amount >= capacity || (contents != null && !contents.isFluidEqual(resource))) {
			return 0;
		}

		int toFill = Math.min(capacity - amount, resource.amount);
		if (!ignoreInOutLimit) {
			toFill = Math.min(getMaxInOut(), toFill);
		}

		if (doFill) {
			if (contents == null) {
				contents = new FluidStack(resource, toFill);
			} else {
				contents.amount += toFill;
			}
			serializeContents();
		}

		return toFill;
	}

	private void serializeContents() {
		NBTTagCompound tag = StorageNbt.getOrCreateTag(upgrade);
		if (contents == null) {
			tag.removeTag(CONTENTS_TAG);
		} else {
			tag.setTag(CONTENTS_TAG, contents.writeToNBT(new NBTTagCompound()));
		}
		save();
	}

	@Nullable
	public FluidStack drain(int maxDrain, boolean doDrain, boolean ignoreInOutLimit) {
		if (contents == null || maxDrain <= 0) {
			return null;
		}

		int toDrain = Math.min(maxDrain, contents.amount);
		if (!ignoreInOutLimit) {
			toDrain = Math.min(getMaxInOut(), toDrain);
		}

		FluidStack ret = new FluidStack(contents, toDrain);
		if (doDrain) {
			if (toDrain == contents.amount) {
				contents = null;
			} else {
				contents.amount -= toDrain;
			}
			serializeContents();
		}

		return ret;
	}

	@Override
	public void tick(@Nullable Entity entity, World world, BlockPos pos) {
		if (world.getTotalWorldTime() < cooldownTime) {
			return;
		}

		boolean didSomething = false;
		FluidActionResult emptied = FluidContainerHelper.emptyContainer(inventory.getStackInSlot(INPUT_SLOT), getFluidHandler(), Fluid.BUCKET_VOLUME, false);
		if (emptied.isSuccess()) {
			inventory.setStackInSlot(INPUT_SLOT, emptied.getResult());
			didSomething = true;
		}
		FluidActionResult filled = FluidContainerHelper.fillContainer(inventory.getStackInSlot(OUTPUT_SLOT), getFluidHandler(), Fluid.BUCKET_VOLUME, false);
		if (filled.isSuccess()) {
			inventory.setStackInSlot(OUTPUT_SLOT, filled.getResult());
			didSomething = true;
		}

		if (didSomething) {
			cooldownTime = world.getTotalWorldTime() + upgradeItem.getTankUpgradeConfig().getAutoFillDrainContainerCooldown();
		}
	}

	@Override
	public int getMinimumMultiplierRequired() {
		return contents == null ? 0 : (int) Math.ceil((float) contents.amount / upgradeItem.getBaseCapacity(storageWrapper));
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	/** One container in each slot, of those the slot's way works with; 1.12's insert doesn't ask whether it's valid. */
	private class TankItemHandler extends ItemStackHandler {
		private TankItemHandler() {
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
				return stack.isEmpty() || isValidFluidItem(stack, false);
			} else if (slot == OUTPUT_SLOT) {
				return stack.isEmpty() || isValidFluidItem(stack, true);
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
