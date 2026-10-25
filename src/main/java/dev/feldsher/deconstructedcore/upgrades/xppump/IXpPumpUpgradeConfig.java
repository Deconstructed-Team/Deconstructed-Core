package dev.feldsher.deconstructedcore.upgrades.xppump;

public interface IXpPumpUpgradeConfig {
	/** Whether the pump can mend items with mending at all. */
	boolean isMendingOn();

	/** The most points used to mend an item per operation, every 5 ticks. */
	int getMaxXpPointsPerMending();
}
