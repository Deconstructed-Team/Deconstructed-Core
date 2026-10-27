package dev.feldsher.deconstructedcore.upgrades.cooking;

import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import dev.feldsher.deconstructedcore.upgrades.IRenderDataUpgrade;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.inventory.IItemHandlerSimpleInserter;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.util.InventoryHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Takes what it can cook and burn from the storage, through filters for each, and puts what it cooked, and what's
 * left of fuel that no longer burns, such as an empty bucket, back into it.
 */
public class AutoCookingUpgradeWrapper extends UpgradeWrapperBase<AutoCookingUpgradeWrapper, AutoCookingUpgradeItem> implements ITickableUpgrade, ICookingUpgrade, IRenderDataUpgrade {
	/** As of its last tick, for its flames on the storage. */
	private boolean burning = false;
	private static final int NOTHING_TO_DO_COOLDOWN = 10;
	private static final int NO_INVENTORY_SPACE_COOLDOWN = 60;

	private final FilterLogic inputFilterLogic;
	private final FilterLogic fuelFilterLogic;
	private final CookingLogic cookingLogic;
	private final Predicate<ItemStack> isValidInput;
	private final Predicate<ItemStack> isValidFuel;
	private final CookingType cookingType;
	private int outputCooldown = 0;
	private int fuelCooldown = 0;
	private int inputCooldown = 0;

	public AutoCookingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		cookingType = upgradeItem.getCookingType();
		IAutoCookingUpgradeConfig config = upgradeItem.getAutoCookingUpgradeConfig();
		inputFilterLogic = new FilterLogic(upgrade, this.upgradeSaveHandler, config.getInputFilterSlots(), cookingType::isInput, "input_filter_attributes");
		fuelFilterLogic = new FilterLogic(upgrade, this.upgradeSaveHandler, config.getFuelFilterSlots(), s -> cookingType.getBurnTime(s) > 0, "fuel_filter_attributes");
		fuelFilterLogic.setAllowByDefault(true);
		fuelFilterLogic.setEmptyAllowListMatchesEverything();

		isValidInput = s -> cookingType.isInput(s) && inputFilterLogic.matchesFilter(s);
		isValidFuel = s -> cookingType.getBurnTime(s) > 0 && fuelFilterLogic.matchesFilter(s);
		cookingLogic = new CookingLogic(upgrade, this.upgradeSaveHandler, isValidFuel, isValidInput, config, cookingType);
	}

	/** Its flames, while it burns. */
	@Override
	public void addRenderData(Map<String, NBTTagCompound> renderData) {
		if (burning) {
			renderData.put(CookingLogic.RENDER_DATA_TYPE, new NBTTagCompound());
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (!enabled) {
			burning = false;
			cookingLogic.pause();
		}
		super.setEnabled(enabled);
	}

	@Override
	public void onBeforeRemoved() {
		cookingLogic.pause();
	}

	private void tryPushingOutput() {
		if (outputCooldown > 0) {
			outputCooldown--;
			return;
		}

		ItemStack output = cookingLogic.getCookOutput();
		IItemHandlerSimpleInserter inventory = storageWrapper.getInventoryForUpgradeProcessing();
		if (!output.isEmpty() && inventory.insertItem(output, true).getCount() < output.getCount()) {
			ItemStack ret = inventory.insertItem(output, false);
			cookingLogic.getCookingInventory().extractItem(CookingLogic.COOK_OUTPUT_SLOT, output.getCount() - ret.getCount(), false);
		} else {
			outputCooldown = NO_INVENTORY_SPACE_COOLDOWN;
		}

		ItemStack fuel = cookingLogic.getFuel();
		if (!fuel.isEmpty() && cookingType.getBurnTime(fuel) <= 0 && inventory.insertItem(fuel, true).getCount() < fuel.getCount()) {
			ItemStack ret = inventory.insertItem(fuel, false);
			cookingLogic.getCookingInventory().extractItem(CookingLogic.FUEL_SLOT, fuel.getCount() - ret.getCount(), false);
		}
	}

	@Override
	public void tick(@Nullable Entity entity, World world, BlockPos pos) {
		burning = cookingLogic.isBurning(world);
		if (isInCooldown(world)) {
			return;
		}
		tryPushingOutput();
		tryPullingFuel();
		tryPullingInput();

		if (!cookingLogic.tick(world) && outputCooldown <= 0 && fuelCooldown <= 0 && inputCooldown <= 0) {
			setCooldown(world, NOTHING_TO_DO_COOLDOWN);
		}
	}

	private void tryPullingInput() {
		if (inputCooldown > 0) {
			inputCooldown--;
			return;
		}

		if (tryPullingGetUnsuccessful(cookingLogic.getCookInput(), cookingLogic::setCookInput, isValidInput)) {
			inputCooldown = NO_INVENTORY_SPACE_COOLDOWN;
		}
	}

	private void tryPullingFuel() {
		if (fuelCooldown > 0) {
			fuelCooldown--;
			return;
		}

		if (tryPullingGetUnsuccessful(cookingLogic.getFuel(), cookingLogic::setFuel, isValidFuel)) {
			fuelCooldown = NO_INVENTORY_SPACE_COOLDOWN;
		}
	}

	private boolean tryPullingGetUnsuccessful(ItemStack stack, Consumer<ItemStack> setSlot, Predicate<ItemStack> isItemValid) {
		ItemStack toExtract;
		IItemHandlerModifiable inventory = storageWrapper.getInventoryForUpgradeProcessing();
		if (stack.isEmpty()) {
			AtomicReference<ItemStack> ret = new AtomicReference<>(ItemStack.EMPTY);
			InventoryHelper.iterate(inventory, (slot, st) -> {
				if (isItemValid.test(st)) {
					ret.set(st.copy());
				}
			}, () -> !ret.get().isEmpty());
			if (!ret.get().isEmpty()) {
				toExtract = ret.get();
				toExtract.setCount(toExtract.getMaxStackSize());
			} else {
				return true;
			}
		} else if (stack.getCount() == stack.getMaxStackSize() || !isItemValid.test(stack)) {
			return true;
		} else {
			toExtract = stack.copy();
			toExtract.setCount(stack.getMaxStackSize() - stack.getCount());
		}

		if (InventoryHelper.extractFromInventory(toExtract, inventory, true).getCount() > 0) {
			ItemStack toSet = InventoryHelper.extractFromInventory(toExtract, inventory, false);
			toSet.grow(stack.getCount());
			setSlot.accept(toSet);
		} else {
			return true;
		}
		return false;
	}

	@Override
	public CookingLogic getCookingLogic() {
		return cookingLogic;
	}

	public FilterLogic getInputFilterLogic() {
		return inputFilterLogic;
	}

	public FilterLogic getFuelFilterLogic() {
		return fuelFilterLogic;
	}
}
