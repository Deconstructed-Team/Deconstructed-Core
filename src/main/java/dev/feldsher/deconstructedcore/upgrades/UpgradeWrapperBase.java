package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.function.Consumer;

public abstract class UpgradeWrapperBase<W extends IUpgradeWrapper, T extends UpgradeItemBase<W>> implements IUpgradeWrapper {
	private static final String ENABLED_TAG = "enabled";
	protected final IStorageWrapper storageWrapper;
	protected final Consumer<ItemStack> upgradeSaveHandler;
	protected final ItemStack upgrade;
	protected final T upgradeItem;

	private long cooldown = 0;

	protected UpgradeWrapperBase(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		this.storageWrapper = storageWrapper;
		this.upgrade = upgrade;
		//noinspection unchecked
		upgradeItem = (T) upgrade.getItem();
		this.upgradeSaveHandler = upgradeSaveHandler;
	}

	@Override
	public ItemStack getUpgradeStack() {
		return upgrade;
	}

	protected void save() {
		upgradeSaveHandler.accept(upgrade);
	}

	protected void setCooldown(World world, int time) {
		cooldown = world.getTotalWorldTime() + time;
	}

	public long getCooldownTime() {
		return cooldown;
	}

	public boolean isInCooldown(World world) {
		return getCooldownTime() > world.getTotalWorldTime();
	}

	@Override
	public boolean isEnabled() {
		return upgrade.getTagCompound() == null || !upgrade.getTagCompound().hasKey(ENABLED_TAG) || upgrade.getTagCompound().getBoolean(ENABLED_TAG);
	}

	@Override
	public void setEnabled(boolean enabled) {
		StorageNbt.getOrCreateTag(upgrade).setBoolean(ENABLED_TAG, enabled);
		save();
		storageWrapper.getUpgradeHandler().refreshWrappersThatImplementAndTypeWrappers();
	}
}
