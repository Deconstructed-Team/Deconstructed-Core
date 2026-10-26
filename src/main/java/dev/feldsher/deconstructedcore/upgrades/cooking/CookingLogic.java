package dev.feldsher.deconstructedcore.upgrades.cooking;

import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A furnace kept in the upgrade: its input, fuel and output slots, and when the fuel stops burning and the item is
 * cooked, as world times, so that it needn't be saved each tick. Paused, it keeps the times left instead.
 */
public class CookingLogic {
	/** The render data a burning cooking upgrade shows its flames by. */
	public static final String RENDER_DATA_TYPE = "cooking";
	public static final int COOK_INPUT_SLOT = 0;
	public static final int FUEL_SLOT = 1;
	public static final int COOK_OUTPUT_SLOT = 2;
	private static final String COOKING_INVENTORY_TAG = "cookingInventory";
	private static final String BURN_TIME_FINISH_TAG = "burnTimeFinish";
	private static final String BURN_TIME_TOTAL_TAG = "burnTimeTotal";
	private static final String COOK_TIME_FINISH_TAG = "cookTimeFinish";
	private static final String COOK_TIME_TOTAL_TAG = "cookTimeTotal";
	private static final String IS_COOKING_TAG = "isCooking";

	private final ItemStack upgrade;
	private final Consumer<ItemStack> saveHandler;
	private final CookingType cookingType;
	private final Predicate<ItemStack> isFuel;
	private final Predicate<ItemStack> isInput;
	private final double cookingSpeedMultiplier;
	private final double fuelEfficiencyMultiplier;
	private CookingItemHandler cookingInventory = null;
	private ItemStack cookingResult = ItemStack.EMPTY;
	private boolean cookingResultInitialized = false;

	private boolean paused = false;
	private long remainingCookTime = 0;
	private long remainingBurnTime = 0;

	public CookingLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, ICookingUpgradeConfig cookingUpgradeConfig, CookingType cookingType) {
		this(upgrade, saveHandler, s -> cookingType.getBurnTime(s) > 0, cookingType::isInput, cookingUpgradeConfig, cookingType);
	}

	public CookingLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, Predicate<ItemStack> isFuel, Predicate<ItemStack> isInput, ICookingUpgradeConfig cookingUpgradeConfig, CookingType cookingType) {
		this.upgrade = upgrade;
		this.saveHandler = saveHandler;
		this.isFuel = isFuel;
		this.isInput = isInput;
		cookingSpeedMultiplier = cookingUpgradeConfig.getCookingSpeedMultiplier();
		fuelEfficiencyMultiplier = cookingUpgradeConfig.getFuelEfficiencyMultiplier();
		this.cookingType = cookingType;
	}

	private void save() {
		saveHandler.accept(upgrade);
	}

	/** Whether it did something, so that the upgrade needn't wait before the next tick. */
	public boolean tick(World world) {
		updateTimes(world);

		boolean didSomething = true;
		if (isBurning(world) || readyToStartCooking()) {
			ItemStack result = getCookingResult();
			if (result.isEmpty() && isCooking()) {
				setIsCooking(false);
			}
			if (!result.isEmpty()) {
				updateFuel(world, result);

				if (isBurning(world) && canSmelt(result)) {
					updateCookingProgress(world, result);
				} else if (!isBurning(world)) {
					didSomething = false;
				}
			}
		}

		if (!isBurning(world) && isCooking()) {
			updateCookingCooldown(world);
		} else {
			didSomething = false;
		}
		return didSomething;
	}

	private void updateTimes(World world) {
		if (paused) {
			unpause(world);
			return;
		}

		if (isBurning(world)) {
			remainingBurnTime = getBurnTimeFinish() - world.getTotalWorldTime();
		} else {
			remainingBurnTime = 0;
		}
		if (isCooking()) {
			remainingCookTime = getCookTimeFinish() - world.getTotalWorldTime();
		} else {
			remainingCookTime = 0;
		}
	}

	private void unpause(World world) {
		paused = false;

		if (remainingBurnTime > 0) {
			setBurnTimeFinish(world.getTotalWorldTime() + remainingBurnTime);
		}
		if (remainingCookTime > 0) {
			setCookTimeFinish(world.getTotalWorldTime() + remainingCookTime);
			setIsCooking(true);
		}
	}

	public boolean isBurning(World world) {
		return getBurnTimeFinish() >= world.getTotalWorldTime();
	}

	private ItemStack getCookingResult() {
		if (!cookingResultInitialized) {
			cookingResult = cookingType.getResult(getCookInput());
			cookingResultInitialized = true;
		}
		return cookingResult;
	}

	/** Without fuel the progress goes back, twice as fast as it went. */
	private void updateCookingCooldown(World world) {
		if (getRemainingCookTime(world) + 2 > getCookTimeTotal()) {
			setIsCooking(false);
		} else {
			setCookTimeFinish(world.getTotalWorldTime() + Math.min(getRemainingCookTime(world) + 2, getCookTimeTotal()));
		}
	}

	private void updateCookingProgress(World world, ItemStack result) {
		if (isCooking() && finishedCooking(world)) {
			smelt(result);
			if (canSmelt(result)) {
				setCookTime(world, (int) (cookingType.getCookingTime() * (1 / cookingSpeedMultiplier)));
			} else {
				setIsCooking(false);
			}
		} else if (!isCooking()) {
			setIsCooking(true);
			setCookTime(world, (int) (cookingType.getCookingTime() * (1 / cookingSpeedMultiplier)));
		}
	}

	private boolean finishedCooking(World world) {
		return getCookTimeFinish() <= world.getTotalWorldTime();
	}

	private boolean readyToStartCooking() {
		return !getFuel().isEmpty() && !getCookInput().isEmpty();
	}

	private void smelt(ItemStack result) {
		if (!canSmelt(result)) {
			return;
		}

		ItemStack input = getCookInput().copy();
		ItemStack output = getCookOutput();
		if (output.isEmpty()) {
			setCookOutput(result.copy());
		} else if (ItemStack.areItemsEqual(output, result)) {
			ItemStack grown = output.copy();
			grown.grow(result.getCount());
			setCookOutput(grown);
		}

		if (input.getItem() == Item.getItemFromBlock(Blocks.SPONGE) && input.getMetadata() == 1 && getFuel().getItem() == Items.BUCKET) {
			setFuel(new ItemStack(Items.WATER_BUCKET));
		}

		input.shrink(1);
		setCookInput(input);
	}

	public void setCookInput(ItemStack input) {
		getCookingInventory().setStackInSlot(COOK_INPUT_SLOT, input);
	}

	private void setCookOutput(ItemStack stack) {
		getCookingInventory().setStackInSlot(COOK_OUTPUT_SLOT, stack);
	}

	private int getRemainingCookTime(World world) {
		return (int) (getCookTimeFinish() - world.getTotalWorldTime());
	}

	private void setCookTime(World world, int cookTime) {
		setCookTimeFinish(world.getTotalWorldTime() + cookTime);
		setCookTimeTotal(cookTime);
	}

	/** Stops it where it is, to go on from there once it ticks again. */
	public void pause() {
		paused = true;
		setCookTimeFinish(0);
		setIsCooking(false);
		setBurnTimeFinish(0);
	}

	private void updateFuel(World world, ItemStack result) {
		ItemStack fuel = getFuel();
		if (!isBurning(world) && canSmelt(result)) {
			int burnTime = cookingType.getBurnTime(fuel);
			if (burnTime <= 0) {
				return;
			}
			setBurnTime(world, (int) (burnTime * fuelEfficiencyMultiplier / cookingSpeedMultiplier));
			if (isBurning(world)) {
				ItemStack remaining = fuel.getItem().getContainerItem(fuel);
				ItemStack shrunk = fuel.copy();
				shrunk.shrink(1);
				setFuel(shrunk.isEmpty() ? remaining : shrunk);
			}
		}
	}

	private void setBurnTime(World world, int burnTime) {
		setBurnTimeFinish(world.getTotalWorldTime() + burnTime);
		setBurnTimeTotal(burnTime);
	}

	private boolean canSmelt(ItemStack result) {
		if (getCookInput().isEmpty() || result.isEmpty()) {
			return false;
		}
		ItemStack output = getCookOutput();
		if (output.isEmpty()) {
			return true;
		}
		if (!ItemStack.areItemsEqual(output, result) || !ItemStack.areItemStackTagsEqual(output, result)) {
			return false;
		}
		return output.getCount() + result.getCount() <= output.getMaxStackSize();
	}

	public ItemStack getCookOutput() {
		return getCookingInventory().getStackInSlot(COOK_OUTPUT_SLOT);
	}

	public ItemStack getCookInput() {
		return getCookingInventory().getStackInSlot(COOK_INPUT_SLOT);
	}

	public ItemStack getFuel() {
		return getCookingInventory().getStackInSlot(FUEL_SLOT);
	}

	public void setFuel(ItemStack fuel) {
		getCookingInventory().setStackInSlot(FUEL_SLOT, fuel);
	}

	public boolean isFuel(ItemStack stack) {
		return isFuel.test(stack);
	}

	public CookingItemHandler getCookingInventory() {
		if (cookingInventory == null) {
			cookingInventory = new CookingItemHandler();
			NBTTagCompound tag = upgrade.getTagCompound();
			if (tag != null && tag.hasKey(COOKING_INVENTORY_TAG)) {
				cookingInventory.deserializeNBT(tag.getCompoundTag(COOKING_INVENTORY_TAG));
			}
		}
		return cookingInventory;
	}

	private NBTTagCompound getTag() {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag == null ? new NBTTagCompound() : tag;
	}

	public long getBurnTimeFinish() {
		return getTag().getLong(BURN_TIME_FINISH_TAG);
	}

	private void setBurnTimeFinish(long burnTimeFinish) {
		StorageNbt.getOrCreateTag(upgrade).setLong(BURN_TIME_FINISH_TAG, burnTimeFinish);
		save();
	}

	public int getBurnTimeTotal() {
		return getTag().getInteger(BURN_TIME_TOTAL_TAG);
	}

	private void setBurnTimeTotal(int burnTimeTotal) {
		StorageNbt.getOrCreateTag(upgrade).setInteger(BURN_TIME_TOTAL_TAG, burnTimeTotal);
		save();
	}

	public long getCookTimeFinish() {
		NBTTagCompound tag = getTag();
		return tag.hasKey(COOK_TIME_FINISH_TAG) ? tag.getLong(COOK_TIME_FINISH_TAG) : -1L;
	}

	private void setCookTimeFinish(long cookTimeFinish) {
		StorageNbt.getOrCreateTag(upgrade).setLong(COOK_TIME_FINISH_TAG, cookTimeFinish);
		save();
	}

	public int getCookTimeTotal() {
		return getTag().getInteger(COOK_TIME_TOTAL_TAG);
	}

	private void setCookTimeTotal(int cookTimeTotal) {
		StorageNbt.getOrCreateTag(upgrade).setInteger(COOK_TIME_TOTAL_TAG, cookTimeTotal);
		save();
	}

	public boolean isCooking() {
		return getTag().getBoolean(IS_COOKING_TAG);
	}

	private void setIsCooking(boolean isCooking) {
		StorageNbt.getOrCreateTag(upgrade).setBoolean(IS_COOKING_TAG, isCooking);
		save();
	}

	/** The three slots, saved into the upgrade; the input and fuel slots take only what cooks and what burns. */
	public class CookingItemHandler extends ItemStackHandler {
		private CookingItemHandler() {
			super(3);
		}

		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			StorageNbt.getOrCreateTag(upgrade).setTag(COOKING_INVENTORY_TAG, serializeNBT());
			save();
			if (slot == COOK_INPUT_SLOT) {
				cookingResultInitialized = false;
			}
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			if (stack.isEmpty() || ItemStack.areItemsEqual(getStackInSlot(slot), stack) && ItemStack.areItemStackTagsEqual(getStackInSlot(slot), stack)) {
				return true;
			}
			switch (slot) {
				case COOK_INPUT_SLOT:
					return isInput.test(stack);
				case FUEL_SLOT:
					return isFuel.test(stack);
				default:
					return true;
			}
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
		}
	}
}
