package dev.feldsher.deconstructedcore.upgrades;

public interface IContentsFilteredUpgrade extends IFilteredUpgrade {
	@Override
	ContentsFilterLogic getFilterLogic();
}
