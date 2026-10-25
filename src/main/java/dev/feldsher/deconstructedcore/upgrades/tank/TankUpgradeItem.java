package dev.feldsher.deconstructedcore.upgrades.tank;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.common.gui.UpgradeSlotChangeResult;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;
import dev.feldsher.deconstructedcore.upgrades.stack.StackUpgradeItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

/** Takes two columns of the storage's inventory for a tank, as big as the storage and its stack upgrades; two at most. */
public class TankUpgradeItem extends UpgradeItemBase<TankUpgradeWrapper> {
	public static final UpgradeType<TankUpgradeWrapper> TYPE = new UpgradeType<>(TankUpgradeWrapper::new);
	private static final List<UpgradeConflictDefinition> UPGRADE_CONFLICT_DEFINITIONS = Collections.singletonList(new UpgradeConflictDefinition(TankUpgradeItem.class::isInstance, 1,
			TranslationHelper.INSTANCE.translError("add.two_tank_upgrades_present")));

	private final ITankUpgradeConfig tankUpgradeConfig;

	public TankUpgradeItem(ITankUpgradeConfig tankUpgradeConfig, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.tankUpgradeConfig = tankUpgradeConfig;
	}

	public int getBaseCapacity(IStorageWrapper storageWrapper) {
		return tankUpgradeConfig.getCapacityPerSlotRow() * storageWrapper.getNumberOfSlotRows();
	}

	public double getAdjustedStackMultiplier(IStorageWrapper storageWrapper) {
		double stackMultiplier = StackUpgradeItem.getInventorySlotLimit(storageWrapper) / 64D;
		return 1 + tankUpgradeConfig.getStackMultiplierRatio() * (stackMultiplier - 1);
	}

	public int getTankCapacity(IStorageWrapper storageWrapper) {
		double stackMultiplier = getAdjustedStackMultiplier(storageWrapper);
		int baseCapacity = getBaseCapacity(storageWrapper);
		return Integer.MAX_VALUE / stackMultiplier < baseCapacity ? Integer.MAX_VALUE : (int) (baseCapacity * stackMultiplier);
	}

	public ITankUpgradeConfig getTankUpgradeConfig() {
		return tankUpgradeConfig;
	}

	@Override
	public UpgradeType<TankUpgradeWrapper> getType() {
		return TYPE;
	}

	/** A tank with more in it than this storage holds doesn't go in. */
	@Override
	public UpgradeSlotChangeResult checkExtraInsertConditions(ItemStack upgradeStack, IStorageWrapper storageWrapper, boolean isClientSide, @Nullable IUpgradeItem<?> upgradeInSlot) {
		int capacityAfter = (int) (getTankCapacity(storageWrapper) / (upgradeInSlot instanceof StackUpgradeItem ? ((StackUpgradeItem) upgradeInSlot).getStackSizeMultiplier() : 1));
		FluidStack contents = TankUpgradeWrapper.getContents(upgradeStack);
		double multiplierRequired = contents == null ? 0 : (double) contents.amount / capacityAfter;
		if (multiplierRequired > 1) {
			DecimalFormat multiplierFormat = new DecimalFormat("0.#");
			String formattedMultiplierRequired = multiplierFormat.format(Math.ceil(10 * multiplierRequired) / 10);
			return UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("add.tank_capacity_high", formattedMultiplierRequired), Collections.emptySet(), Collections.emptySet());
		}

		return UpgradeSlotChangeResult.success();
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return UPGRADE_CONFLICT_DEFINITIONS;
	}

	@Override
	public int getInventoryColumnsTaken() {
		return 2;
	}
}
