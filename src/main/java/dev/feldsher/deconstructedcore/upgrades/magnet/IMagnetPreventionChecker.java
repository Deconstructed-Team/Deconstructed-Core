package dev.feldsher.deconstructedcore.upgrades.magnet;

import net.minecraft.entity.Entity;

/** Something that keeps magnets off an item or experience orb, as Botania's Solegnolia does around itself. */
public interface IMagnetPreventionChecker {
	boolean isBlocked(Entity entity);
}
