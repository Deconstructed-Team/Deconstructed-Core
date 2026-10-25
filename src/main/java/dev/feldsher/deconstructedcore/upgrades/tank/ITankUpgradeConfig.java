package dev.feldsher.deconstructedcore.upgrades.tank;

public interface ITankUpgradeConfig {
	/** In mB, per row of the storage's slots. */
	int getCapacityPerSlotRow();

	/** How much of the storage's stack multiplier goes into the capacity: 0 none, 1 all of it. */
	double getStackMultiplierRatio();

	/** Ticks between the bucket's worth the tank moves from or to the container in each of its slots. */
	int getAutoFillDrainContainerCooldown();

	/** In mB per operation, per row of the storage's slots, before the stack multiplier. */
	int getMaxInputOutput();
}
