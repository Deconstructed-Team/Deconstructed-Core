package dev.feldsher.deconstructedcore.upgrades.pump;

public interface IPumpUpgradeConfig {
	int getFilterSlots();

	/** In mB per operation, per row of the storage's slots, before the stack multiplier. */
	int getMaxInputOutput();

	/** How much of the storage's stack multiplier goes into what's moved per operation: 0 none, 1 all of it. */
	double getStackMultiplierRatio();
}
