package dev.feldsher.deconstructedcore.upgrades.cooking;

public interface ICookingUpgradeConfig {
	/** 1.0 cooks as fast as vanilla's furnace. */
	double getCookingSpeedMultiplier();

	/** 1.0 burns fuel as fast as vanilla's furnace. */
	double getFuelEfficiencyMultiplier();
}
