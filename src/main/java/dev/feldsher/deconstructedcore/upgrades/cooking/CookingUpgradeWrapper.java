package dev.feldsher.deconstructedcore.upgrades.cooking;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class CookingUpgradeWrapper extends UpgradeWrapperBase<CookingUpgradeWrapper, CookingUpgradeItem> implements ITickableUpgrade, ICookingUpgrade {
	private static final int NOTHING_TO_DO_COOLDOWN = 10;
	private final CookingLogic cookingLogic;

	public CookingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		cookingLogic = new CookingLogic(upgrade, upgradeSaveHandler, upgradeItem.getCookingUpgradeConfig(), upgradeItem.getCookingType());
	}

	@Override
	public void tick(@Nullable Entity entity, World world, BlockPos pos) {
		if (isInCooldown(world)) {
			return;
		}
		if (!cookingLogic.tick(world)) {
			setCooldown(world, NOTHING_TO_DO_COOLDOWN);
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (!enabled) {
			cookingLogic.pause();
		}
		super.setEnabled(enabled);
	}

	@Override
	public void onBeforeRemoved() {
		cookingLogic.pause();
	}

	@Override
	public CookingLogic getCookingLogic() {
		return cookingLogic;
	}
}
