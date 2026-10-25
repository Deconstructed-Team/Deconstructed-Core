package dev.feldsher.deconstructedcore.upgrades.jukebox;

import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeGroup;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;

import java.util.function.IntSupplier;

public class JukeboxUpgradeItem extends UpgradeItemBase<JukeboxUpgradeWrapper> {
	public static final UpgradeGroup UPGRADE_GROUP = new UpgradeGroup("jukebox_upgrades", TranslationHelper.INSTANCE.translUpgradeGroup("jukebox_upgrades"));
	public static final UpgradeType<JukeboxUpgradeWrapper> TYPE = new UpgradeType<>(JukeboxUpgradeWrapper::new);
	private final IntSupplier numberOfSlots;
	private final IntSupplier slotsInRow;

	public JukeboxUpgradeItem(IUpgradeCountLimitConfig upgradeTypeLimitConfig, IntSupplier numberOfSlots, IntSupplier slotsInRow) {
		super(upgradeTypeLimitConfig);
		this.numberOfSlots = numberOfSlots;
		this.slotsInRow = slotsInRow;
	}

	@Override
	public UpgradeType<JukeboxUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public UpgradeGroup getUpgradeGroup() {
		return UPGRADE_GROUP;
	}

	public int getNumberOfSlots() {
		return numberOfSlots.getAsInt();
	}

	public int getSlotsInRow() {
		return slotsInRow.getAsInt();
	}
}
