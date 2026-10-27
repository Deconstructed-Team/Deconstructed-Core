package dev.feldsher.deconstructedcore.upgrades.cooking;

import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import dev.feldsher.deconstructedcore.upgrades.IRenderDataUpgrade;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class CookingUpgradeWrapper extends UpgradeWrapperBase<CookingUpgradeWrapper, CookingUpgradeItem> implements ITickableUpgrade, ICookingUpgrade, IRenderDataUpgrade {
	/** As of its last tick, for its flames on the storage. */
	private boolean burning = false;
	private static final int NOTHING_TO_DO_COOLDOWN = 10;
	private final CookingLogic cookingLogic;

	public CookingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		cookingLogic = new CookingLogic(upgrade, this.upgradeSaveHandler, upgradeItem.getCookingUpgradeConfig(), upgradeItem.getCookingType());
	}

	@Override
	public void tick(@Nullable Entity entity, World world, BlockPos pos) {
		burning = cookingLogic.isBurning(world);
		if (isInCooldown(world)) {
			return;
		}
		if (!cookingLogic.tick(world)) {
			setCooldown(world, NOTHING_TO_DO_COOLDOWN);
		}
	}

	/** Its flames, while it burns. */
	@Override
	public void addRenderData(Map<String, NBTTagCompound> renderData) {
		if (burning) {
			renderData.put(CookingLogic.RENDER_DATA_TYPE, new NBTTagCompound());
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (!enabled) {
			burning = false;
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
