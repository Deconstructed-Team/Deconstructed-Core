package dev.feldsher.deconstructedcore.upgrades;

import java.util.List;

/**
 * How a storage's upgrades are found by what they implement: by default its own, and with an upgrade that changes
 * this, as inception, also those of the storages inside it.
 */
public interface IUpgradeWrapperAccessor {
	<T> List<T> getWrappersThatImplement(Class<T> upgradeClass);

	<T> List<T> getWrappersThatImplementFromMainStorage(Class<T> upgradeClass);

	void clearCache();

	default void onBeforeDeconstruct() {
		//noop
	}
}
