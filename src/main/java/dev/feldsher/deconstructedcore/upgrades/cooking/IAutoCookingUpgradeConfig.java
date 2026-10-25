package dev.feldsher.deconstructedcore.upgrades.cooking;

public interface IAutoCookingUpgradeConfig extends ICookingUpgradeConfig {
	int getInputFilterSlots();

	int getInputFilterSlotsInRow();

	int getFuelFilterSlots();

	int getFuelFilterSlotsInRow();
}
