package dev.feldsher.deconstructedcore.upgrades;

/** An upgrade whose capacity grows with stack upgrades, which can't be taken out while its contents need them. */
public interface IStackableContentsUpgrade {
	int getMinimumMultiplierRequired();
}
