package dev.feldsher.deconstructedcore.upgrades.pump;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

/** Moves fluids between the storage's tanks and what's around it; whether also with the world and hands is its default. */
public class PumpUpgradeItem extends UpgradeItemBase<PumpUpgradeWrapper> {
	private static final UpgradeType<PumpUpgradeWrapper> TYPE = new UpgradeType<>(PumpUpgradeWrapper::new);
	private final boolean interactWithHandDefault;
	private final boolean interactWithWorldDefault;
	private final IPumpUpgradeConfig pumpUpgradeConfig;

	public PumpUpgradeItem(boolean interactWithHandDefault, boolean interactWithWorldDefault, IPumpUpgradeConfig pumpUpgradeConfig, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.interactWithHandDefault = interactWithHandDefault;
		this.interactWithWorldDefault = interactWithWorldDefault;
		this.pumpUpgradeConfig = pumpUpgradeConfig;
	}

	@Override
	public UpgradeType<PumpUpgradeWrapper> getType() {
		return TYPE;
	}

	public boolean getInteractWithHandDefault() {
		return interactWithHandDefault;
	}

	public boolean getInteractWithWorldDefault() {
		return interactWithWorldDefault;
	}

	public IPumpUpgradeConfig getPumpUpgradeConfig() {
		return pumpUpgradeConfig;
	}
}
