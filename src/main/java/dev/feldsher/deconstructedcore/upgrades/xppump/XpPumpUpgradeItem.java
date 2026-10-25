package dev.feldsher.deconstructedcore.upgrades.xppump;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

public class XpPumpUpgradeItem extends UpgradeItemBase<XpPumpUpgradeWrapper> {
	public static final UpgradeType<XpPumpUpgradeWrapper> TYPE = new UpgradeType<>(XpPumpUpgradeWrapper::new);
	private final IXpPumpUpgradeConfig xpPumpUpgradeConfig;

	public XpPumpUpgradeItem(IXpPumpUpgradeConfig xpPumpUpgradeConfig, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.xpPumpUpgradeConfig = xpPumpUpgradeConfig;
	}

	public IXpPumpUpgradeConfig getXpPumpUpgradeConfig() {
		return xpPumpUpgradeConfig;
	}

	@Override
	public UpgradeType<XpPumpUpgradeWrapper> getType() {
		return TYPE;
	}
}
