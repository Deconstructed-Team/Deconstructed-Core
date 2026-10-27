package dev.feldsher.deconstructedcore.upgrades.stonecutter;

import dev.feldsher.deconstructedcore.compat.futuremc.FutureMcStonecutterRecipes;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

/**
 * A cutting block in the storage's upgrade tab, one input item made into another, one for one: the stonecutter, with
 * the recipes Future MC's has, or another of the same kind, as the chisel with Chisel's carving groups.
 */
public class StonecutterUpgradeItem extends UpgradeItemBase<StonecutterUpgradeWrapper> {
	// each kind of its own type, so that upgrade count limits and lookups by type keep them apart
	private final UpgradeType<StonecutterUpgradeWrapper> type = new UpgradeType<>(StonecutterUpgradeWrapper::new);
	private final ICuttingResults cuttingResults;
	private final String tabName;

	public StonecutterUpgradeItem(IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		this(upgradeTypeLimitConfig, FutureMcStonecutterRecipes.RESULTS, "stonecutter");
	}

	/** @param tabName the name its tab's title and tooltip are translated by */
	public StonecutterUpgradeItem(IUpgradeCountLimitConfig upgradeTypeLimitConfig, ICuttingResults cuttingResults, String tabName) {
		super(upgradeTypeLimitConfig);
		this.cuttingResults = cuttingResults;
		this.tabName = tabName;
	}

	@Override
	public UpgradeType<StonecutterUpgradeWrapper> getType() {
		return type;
	}

	public ICuttingResults getCuttingResults() {
		return cuttingResults;
	}

	public String getTabName() {
		return tabName;
	}
}
