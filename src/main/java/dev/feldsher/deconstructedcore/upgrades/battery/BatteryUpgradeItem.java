package dev.feldsher.deconstructedcore.upgrades.battery;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.common.gui.UpgradeSlotChangeResult;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;
import dev.feldsher.deconstructedcore.upgrades.stack.StackUpgradeItem;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

/** Takes two columns of the storage's inventory for a battery, as big as the storage and its stack upgrades; one at most. */
public class BatteryUpgradeItem extends UpgradeItemBase<BatteryUpgradeWrapper> {
	public static final UpgradeType<BatteryUpgradeWrapper> TYPE = new UpgradeType<>(BatteryUpgradeWrapper::new);
	private static final List<UpgradeConflictDefinition> UPGRADE_CONFLICT_DEFINITIONS = Collections.singletonList(new UpgradeConflictDefinition(BatteryUpgradeItem.class::isInstance, 0,
			TranslationHelper.INSTANCE.translError("add.battery_exists")));

	private final IBatteryUpgradeConfig batteryUpgradeConfig;

	public BatteryUpgradeItem(IBatteryUpgradeConfig batteryUpgradeConfig, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.batteryUpgradeConfig = batteryUpgradeConfig;
	}

	public IBatteryUpgradeConfig getBatteryUpgradeConfig() {
		return batteryUpgradeConfig;
	}

	@Override
	public UpgradeType<BatteryUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public int getInventoryColumnsTaken() {
		return 2;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return UPGRADE_CONFLICT_DEFINITIONS;
	}

	/** A battery with more energy than this storage holds doesn't go in. */
	@Override
	public UpgradeSlotChangeResult checkExtraInsertConditions(ItemStack upgradeStack, IStorageWrapper storageWrapper, boolean isClientSide, @Nullable IUpgradeItem<?> upgradeInSlot) {
		int maxEnergyAfter = (int) (getMaxEnergyStored(storageWrapper) / (upgradeInSlot instanceof StackUpgradeItem ? ((StackUpgradeItem) upgradeInSlot).getStackSizeMultiplier() : 1));
		double multiplierRequired = (double) BatteryUpgradeWrapper.getEnergyStored(upgradeStack) / maxEnergyAfter;
		if (multiplierRequired > 1) {
			DecimalFormat multiplierFormat = new DecimalFormat("0.#");
			String formattedMultiplierRequired = multiplierFormat.format(Math.ceil(10 * multiplierRequired) / 10);
			return UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("add.battery_energy_high", formattedMultiplierRequired), Collections.emptySet(), Collections.emptySet());
		}

		return UpgradeSlotChangeResult.success();
	}

	public int getMaxEnergyStored(IStorageWrapper storageWrapper) {
		double stackMultiplier = getAdjustedStackMultiplier(storageWrapper);
		int maxEnergyBase = getMaxEnergyBase(storageWrapper);
		return Integer.MAX_VALUE / stackMultiplier < maxEnergyBase ? Integer.MAX_VALUE : (int) (maxEnergyBase * stackMultiplier);
	}

	public double getAdjustedStackMultiplier(IStorageWrapper storageWrapper) {
		double stackMultiplier = StackUpgradeItem.getInventorySlotLimit(storageWrapper) / 64D;
		return 1 + batteryUpgradeConfig.getStackMultiplierRatio() * (stackMultiplier - 1);
	}

	public int getMaxEnergyBase(IStorageWrapper storageWrapper) {
		return batteryUpgradeConfig.getEnergyPerSlotRow() * storageWrapper.getNumberOfSlotRows();
	}
}
