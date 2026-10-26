package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.renderdata.BatteryRenderInfo;

/** A battery upgrade, shown on its storage. */
public interface IRenderedBatteryUpgrade {
	BatteryRenderInfo getBatteryRenderInfo();
}
