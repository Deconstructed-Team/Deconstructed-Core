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
	private boolean valid = true;

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

	/** Not once invalid: the stack may be out of the storage by then, and saving would put it back. */
	protected void save() {
		if (valid) {
			upgradeSaveHandler.accept(upgrade);
		}
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public void invalidate() {
		valid = false;
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
		if (!valid) {
			return;
		}
		StorageNbt.getOrCreateTag(upgrade).setBoolean(ENABLED_TAG, enabled);
		save();
		storageWrapper.getUpgradeHandler().refreshWrappersThatImplementAndTypeWrappers();
	}
}
