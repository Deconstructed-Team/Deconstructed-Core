package dev.feldsher.deconstructedcore.upgrades;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public interface IUpgradeCountLimitConfig {
	int getMaxUpgradesPerStorage(String storageType, @Nullable ResourceLocation upgradeRegistryName);

	int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup upgradeGroup);

	/** No limits. */
	IUpgradeCountLimitConfig UNLIMITED = new IUpgradeCountLimitConfig() {
		@Override
		public int getMaxUpgradesPerStorage(String storageType, @Nullable ResourceLocation upgradeRegistryName) {
			return Integer.MAX_VALUE;
		}

		@Override
		public int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup upgradeGroup) {
			return Integer.MAX_VALUE;
		}
	};
}
