package dev.feldsher.deconstructedcore.upgrades.battery;

public interface IBatteryUpgradeConfig {
	/** In FE, per row of the storage's slots. */
	int getEnergyPerSlotRow();

	/** How much of the storage's stack multiplier goes into the energy and the transfer rate: 0 none, 1 all of it. */
	double getStackMultiplierRatio();

	/** In FE per operation, per row of the storage's slots, before the stack multiplier. */
	int getMaxInputOutput();
}
