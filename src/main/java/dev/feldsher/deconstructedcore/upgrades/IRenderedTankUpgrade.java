package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.renderdata.TankRenderInfo;

/** A tank upgrade, shown on its storage. */
public interface IRenderedTankUpgrade {
	TankRenderInfo getTankRenderInfo();
}
