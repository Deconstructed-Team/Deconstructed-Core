package dev.feldsher.deconstructedcore.upgrades.feeding;

import dev.feldsher.deconstructedcore.inventory.IItemHandlerSimpleInserter;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.IFilteredUpgrade;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.util.InventoryHelper;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Feeds the player wearing the backpack, or the players around a placed one, with the food from the storage that its
 * filter matches, when they're hungry enough for it or, if set, as soon as they're hurt.
 */
public class FeedingUpgradeWrapper extends UpgradeWrapperBase<FeedingUpgradeWrapper, FeedingUpgradeItem> implements ITickableUpgrade, IFilteredUpgrade {
	private static final int COOLDOWN = 100;
	private static final int STILL_HUNGRY_COOLDOWN = 10;
	private static final int FEEDING_RANGE = 3;
	private static final int MAX_FOOD_LEVEL = 20;
	private static final String FEED_AT_HUNGER_LEVEL_TAG = "feed_at_hunger_level";
	private static final String FEED_IMMEDIATELY_WHEN_HURT_TAG = "feed_immediately_when_hurt";
	private final FilterLogic filterLogic;

	public FeedingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(), s -> s.getItem() instanceof ItemFood, "filter_attributes");
	}

	@Override
	public void tick(@Nullable Entity entity, World world, BlockPos pos) {
		if (isInCooldown(world)) {
			return;
		}

		boolean hungryPlayer = false;
		if (!(entity instanceof EntityPlayer)) {
			for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(pos).grow(FEEDING_RANGE))) {
				hungryPlayer |= feedPlayerAndGetHungry(player, world);
			}
		} else {
			hungryPlayer = feedPlayerAndGetHungry((EntityPlayer) entity, world);
		}
		setCooldown(world, hungryPlayer ? STILL_HUNGRY_COOLDOWN : COOLDOWN);
	}

	private boolean feedPlayerAndGetHungry(EntityPlayer player, World world) {
		int hungerLevel = MAX_FOOD_LEVEL - player.getFoodStats().getFoodLevel();
		if (hungerLevel == 0) {
			return false;
		}
		return tryFeedingFoodFromStorage(world, hungerLevel, player) && player.getFoodStats().getFoodLevel() < MAX_FOOD_LEVEL;
	}

	private boolean tryFeedingFoodFromStorage(World world, int hungerLevel, EntityPlayer player) {
		IItemHandlerSimpleInserter inventory = storageWrapper.getInventoryForUpgradeProcessing();
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			if (tryFeedingStack(world, hungerLevel, player, slot, inventory.getStackInSlot(slot), inventory)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Eats one of the stack as the player would: put in the player's hand for the moment, so that the food's own use
	 * decides whether the player can eat it, then finished at once. What eating leaves, as a bowl, goes back to storage.
	 */
	private boolean tryFeedingStack(World world, int hungerLevel, EntityPlayer player, int slot, ItemStack stack, IItemHandlerSimpleInserter inventory) {
		boolean isHurt = player.getHealth() < player.getMaxHealth() - 0.1F;
		if (!isEdible(stack) || !filterLogic.matchesFilter(stack)
				|| !(isHungryEnoughForFood(hungerLevel, stack) || shouldFeedImmediatelyWhenHurt() && hungerLevel > 0 && isHurt)) {
			return false;
		}

		ItemStack singleItemCopy = stack.copy();
		singleItemCopy.setCount(1);
		ItemStack mainHandItem = player.getHeldItemMainhand();
		player.inventory.mainInventory.set(player.inventory.currentItem, singleItemCopy);
		try {
			if (singleItemCopy.getItem().onItemRightClick(world, player, EnumHand.MAIN_HAND).getType() != EnumActionResult.SUCCESS) {
				return false;
			}
			player.resetActiveHand();

			ItemStack remainder = stack.copy();
			remainder.shrink(1);
			inventory.setStackInSlot(slot, remainder);

			ItemStack resultItem = ForgeEventFactory.onItemUseFinish(player, singleItemCopy.copy(), 0, singleItemCopy.getItem().onItemUseFinish(singleItemCopy, world, player));
			if (!resultItem.isEmpty()) {
				ItemStack insertResult = inventory.insertItem(resultItem, false);
				if (!insertResult.isEmpty()) {
					InventoryHelper.insertOrDropItem(player, insertResult, new PlayerMainInvWrapper(player.inventory));
				}
			}
			return true;
		} finally {
			player.inventory.mainInventory.set(player.inventory.currentItem, mainHandItem);
		}
	}

	private static boolean isEdible(ItemStack stack) {
		return stack.getItem() instanceof ItemFood && ((ItemFood) stack.getItem()).getHealAmount(stack) >= 1;
	}

	private boolean isHungryEnoughForFood(int hungerLevel, ItemStack stack) {
		HungerLevel feedAtHungerLevel = getFeedAtHungerLevel();
		if (feedAtHungerLevel == HungerLevel.ANY) {
			return true;
		}

		int nutrition = ((ItemFood) stack.getItem()).getHealAmount(stack);
		return (feedAtHungerLevel == HungerLevel.HALF ? (nutrition / 2) : nutrition) <= hungerLevel;
	}

	@Override
	public FilterLogic getFilterLogic() {
		return filterLogic;
	}

	public HungerLevel getFeedAtHungerLevel() {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag != null && tag.hasKey(FEED_AT_HUNGER_LEVEL_TAG) ? HungerLevel.fromName(tag.getString(FEED_AT_HUNGER_LEVEL_TAG)) : HungerLevel.HALF;
	}

	public void setFeedAtHungerLevel(HungerLevel hungerLevel) {
		StorageNbt.getOrCreateTag(upgrade).setString(FEED_AT_HUNGER_LEVEL_TAG, hungerLevel.getSerializedName());
		save();
	}

	public boolean shouldFeedImmediatelyWhenHurt() {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag == null || !tag.hasKey(FEED_IMMEDIATELY_WHEN_HURT_TAG) || tag.getBoolean(FEED_IMMEDIATELY_WHEN_HURT_TAG);
	}

	public void setFeedImmediatelyWhenHurt(boolean feedImmediatelyWhenHurt) {
		StorageNbt.getOrCreateTag(upgrade).setBoolean(FEED_IMMEDIATELY_WHEN_HURT_TAG, feedImmediatelyWhenHurt);
		save();
	}
}
