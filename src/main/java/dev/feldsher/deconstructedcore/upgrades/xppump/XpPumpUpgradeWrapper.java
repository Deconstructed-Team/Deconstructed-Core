package dev.feldsher.deconstructedcore.upgrades.xppump;

import dev.feldsher.deconstructedcore.api.IStorageFluidHandler;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.fluid.ExperienceFluids;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import dev.feldsher.deconstructedcore.util.XpHelper;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Moves experience between the player carrying the storage, or the players near it, and its tanks, as liquid
 * experience: keeping them above, below or at a level, or a number of levels at a click. It also mends their items
 * with mending from it.
 */
public class XpPumpUpgradeWrapper extends UpgradeWrapperBase<XpPumpUpgradeWrapper, XpPumpUpgradeItem> implements ITickableUpgrade {
	private static final String DIRECTION_TAG = "direction";
	private static final String LEVEL_TAG = "level";
	private static final String LEVELS_TO_STORE_TAG = "levelsToStore";
	private static final String LEVELS_TO_TAKE_TAG = "levelsToTake";
	private static final String MEND_ITEMS_TAG = "mendItems";
	private static final int DEFAULT_LEVEL = 10;
	private static final int COOLDOWN = 5;
	private static final int ALL_LEVELS = 10000;
	private static final int PLAYER_SEARCH_RANGE = 3;

	private final IXpPumpUpgradeConfig xpPumpUpgradeConfig;

	public XpPumpUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		xpPumpUpgradeConfig = upgradeItem.getXpPumpUpgradeConfig();
	}

	@Override
	public void tick(@Nullable Entity entity, World world, BlockPos pos) {
		if ((entity != null && !(entity instanceof EntityPlayer)) || isInCooldown(world)) {
			return;
		}

		if (entity == null) {
			AxisAlignedBB searchBox = new AxisAlignedBB(pos).grow(PLAYER_SEARCH_RANGE);
			for (EntityPlayer player : world.playerEntities) {
				if (searchBox.contains(player.getPositionVector())) {
					interactWithPlayer(player);
					mendItems(player);
				}
			}
		} else {
			interactWithPlayer((EntityPlayer) entity);
			mendItems((EntityPlayer) entity);
		}

		setCooldown(world, COOLDOWN);
	}

	private void mendItems(EntityPlayer player) {
		if (!xpPumpUpgradeConfig.isMendingOn() || !shouldMendItems()) {
			return;
		}

		List<ItemStack> damagedMendingItems = new ArrayList<>();
		for (ItemStack stack : Enchantments.MENDING.getEntityEquipment(player)) {
			if (!stack.isEmpty() && stack.isItemDamaged() && EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, stack) > 0) {
				damagedMendingItems.add(stack);
			}
		}
		if (damagedMendingItems.isEmpty()) {
			return;
		}
		ItemStack itemStack = damagedMendingItems.get(player.getRNG().nextInt(damagedMendingItems.size()));
		float xpRepairRatio = itemStack.getItem().getXpRepairRatio(itemStack);
		if (xpRepairRatio <= 0) {
			return;
		}
		int xpToTryDrain = (int) Math.min(xpPumpUpgradeConfig.getMaxXpPointsPerMending(), Math.ceil(itemStack.getItemDamage() / xpRepairRatio));
		if (xpToTryDrain > 0) {
			storageWrapper.getFluidHandler().ifPresent(fluidHandler -> {
				int xpDrained = takeExperienceFromTanks(fluidHandler, xpToTryDrain, false);
				itemStack.setItemDamage(Math.max(0, itemStack.getItemDamage() - (int) (xpDrained * xpRepairRatio)));
			});
		}
	}

	private void interactWithPlayer(EntityPlayer player) {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> {
			int level = getLevel();
			AutomationDirection direction = getDirection();
			if (direction == AutomationDirection.OFF) {
				return;
			}

			if ((direction == AutomationDirection.INPUT || direction == AutomationDirection.KEEP)
					&& (level < player.experienceLevel || (level == player.experienceLevel && player.experience > 0))) {
				tryFillTankWithPlayerExperience(player, fluidHandler, level, false);
			} else if ((direction == AutomationDirection.OUTPUT || direction == AutomationDirection.KEEP) && level > player.experienceLevel) {
				tryGivePlayerExperienceFromTank(player, fluidHandler, level, false);
			}
		});
	}

	private void tryGivePlayerExperienceFromTank(EntityPlayer player, IStorageFluidHandler fluidHandler, int stopAtLevel, boolean ignoreInOutLimit) {
		int maxXpPointsToGive = XpHelper.getExperienceForLevel(stopAtLevel) - XpHelper.getPlayerTotalExperience(player);
		int given = takeExperienceFromTanks(fluidHandler, maxXpPointsToGive, ignoreInOutLimit);
		if (given > 0) {
			player.addExperience(given);
		}
	}

	private void tryFillTankWithPlayerExperience(EntityPlayer player, IStorageFluidHandler fluidHandler, int stopAtLevel, boolean ignoreInOutLimit) {
		int maxXpPointsToTake = XpHelper.getPlayerTotalExperience(player) - XpHelper.getExperienceForLevel(stopAtLevel);
		int stored = storeExperienceInTanks(fluidHandler, maxXpPointsToTake, ignoreInOutLimit);
		if (stored > 0) {
			XpHelper.takeExperience(player, stored);
		}
	}

	/** Whole points only, so that no part of one is made or lost. */
	private static int storeExperienceInTanks(IStorageFluidHandler fluidHandler, int points, boolean ignoreInOutLimit) {
		FluidStack xpFluid = getStoredOrDefaultXpFluid(fluidHandler);
		int mbPerPoint = ExperienceFluids.getMbPerExperience(xpFluid);
		if (points <= 0 || mbPerPoint <= 0) {
			return 0;
		}
		int storable = fluidHandler.fill(new FluidStack(xpFluid, toMb(points, mbPerPoint)), false, ignoreInOutLimit) / mbPerPoint;
		if (storable <= 0) {
			return 0;
		}
		return fluidHandler.fill(new FluidStack(xpFluid, storable * mbPerPoint), true, ignoreInOutLimit) / mbPerPoint;
	}

	private static int takeExperienceFromTanks(IStorageFluidHandler fluidHandler, int points, boolean ignoreInOutLimit) {
		if (points <= 0) {
			return 0;
		}
		for (IFluidTankProperties tank : fluidHandler.getTankProperties()) {
			FluidStack contents = tank.getContents();
			int mbPerPoint = ExperienceFluids.getMbPerExperience(contents);
			if (mbPerPoint <= 0) {
				continue;
			}
			FluidStack drainable = fluidHandler.drain(new FluidStack(contents, toMb(points, mbPerPoint)), false, ignoreInOutLimit);
			int takeable = drainable == null ? 0 : drainable.amount / mbPerPoint;
			if (takeable > 0) {
				FluidStack drained = fluidHandler.drain(new FluidStack(contents, takeable * mbPerPoint), true, ignoreInOutLimit);
				return drained == null ? 0 : drained.amount / mbPerPoint;
			}
		}
		return 0;
	}

	private static int toMb(int points, int mbPerPoint) {
		return (int) Math.min(Integer.MAX_VALUE, (long) points * mbPerPoint);
	}

	/** The experience fluid in the tanks, or else the one to store it as; null when the game has none. */
	@Nullable
	private static FluidStack getStoredOrDefaultXpFluid(IStorageFluidHandler fluidHandler) {
		for (IFluidTankProperties tank : fluidHandler.getTankProperties()) {
			FluidStack contents = tank.getContents();
			if (ExperienceFluids.getMbPerExperience(contents) > 0) {
				return contents;
			}
		}
		Fluid defaultFluid = ExperienceFluids.getDefaultFluid();
		return defaultFluid == null ? null : new FluidStack(defaultFluid, 1);
	}

	public void takeLevelsFromPlayer(EntityPlayer player) {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> tryFillTankWithPlayerExperience(player, fluidHandler, Math.max(player.experienceLevel - getLevelsToStore(), 0), true));
	}

	public void takeAllExperienceFromPlayer(EntityPlayer player) {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> tryFillTankWithPlayerExperience(player, fluidHandler, 0, true));
	}

	public void giveLevelsToPlayer(EntityPlayer player) {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> tryGivePlayerExperienceFromTank(player, fluidHandler, player.experienceLevel + getLevelsToTake(), true));
	}

	public void giveAllExperienceToPlayer(EntityPlayer player) {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> tryGivePlayerExperienceFromTank(player, fluidHandler, ALL_LEVELS, true));
	}

	private NBTTagCompound getTag() {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag == null ? new NBTTagCompound() : tag;
	}

	private int getInt(String key, int defaultValue) {
		NBTTagCompound tag = getTag();
		return tag.hasKey(key) ? tag.getInteger(key) : defaultValue;
	}

	private void setInt(String key, int value) {
		StorageNbt.getOrCreateTag(upgrade).setInteger(key, value);
		save();
	}

	public AutomationDirection getDirection() {
		return AutomationDirection.fromName(getTag().getString(DIRECTION_TAG));
	}

	public void setDirection(AutomationDirection direction) {
		StorageNbt.getOrCreateTag(upgrade).setString(DIRECTION_TAG, direction.getName());
		save();
	}

	public void setLevel(int level) {
		setInt(LEVEL_TAG, level);
	}

	public int getLevel() {
		return getInt(LEVEL_TAG, DEFAULT_LEVEL);
	}

	public void setLevelsToStore(int levelsToStore) {
		setInt(LEVELS_TO_STORE_TAG, levelsToStore);
	}

	public int getLevelsToStore() {
		return getInt(LEVELS_TO_STORE_TAG, 1);
	}

	public void setLevelsToTake(int levelsToTake) {
		setInt(LEVELS_TO_TAKE_TAG, levelsToTake);
	}

	public int getLevelsToTake() {
		return getInt(LEVELS_TO_TAKE_TAG, 1);
	}

	public boolean shouldMendItems() {
		NBTTagCompound tag = getTag();
		return !tag.hasKey(MEND_ITEMS_TAG) || tag.getBoolean(MEND_ITEMS_TAG);
	}

	public void setMendItems(boolean mendItems) {
		StorageNbt.getOrCreateTag(upgrade).setBoolean(MEND_ITEMS_TAG, mendItems);
		save();
	}
}
