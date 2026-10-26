package dev.feldsher.deconstructedcore.upgrades;

import net.minecraft.item.ItemStack;

public interface IUpgradeWrapper {
	boolean isEnabled();

	void setEnabled(boolean enabled);

	default boolean canBeDisabled() {
		return true;
	}

	ItemStack getUpgradeStack();

	default boolean hideSettingsTab() {
		return false;
	}

	default void onBeforeRemoved() {
		//noop
	}

	default void onAdded() {
		//noop
	}

	/**
	 * False once the storage's upgrades were made anew without this one: its upgrade was taken out or replaced. What
	 * still holds it, as another player's open tab or a pipe, must not use it, or there'd be two copies of what it holds.
	 */
	default boolean isValid() {
		return true;
	}

	default void invalidate() {
		//noop
	}
}
