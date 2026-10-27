package dev.feldsher.deconstructedcore.upgrades.magnet;

import dev.feldsher.deconstructedcore.inventory.IItemHandlerSimpleInserter;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterLogic;
import dev.feldsher.deconstructedcore.upgrades.IContentsFilteredUpgrade;
import dev.feldsher.deconstructedcore.upgrades.IPickupResponseUpgrade;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pulls the item entities around the storage into it. Picking up experience goes into a tank, and so comes with the
 * tank upgrade: until then the switch is there and does nothing.
 */
public class MagnetUpgradeWrapper extends UpgradeWrapperBase<MagnetUpgradeWrapper, MagnetUpgradeItem>
		implements IContentsFilteredUpgrade, ITickableUpgrade, IPickupResponseUpgrade {
	private static final String PREVENT_REMOTE_MOVEMENT = "PreventRemoteMovement";
	private static final String ALLOW_MACHINE_MOVEMENT = "AllowMachineRemoteMovement";
	private static final String PICKUP_ITEMS_TAG = "pickup_items";
	private static final String PICKUP_XP_TAG = "pickup_xp";

	private static final Set<IMagnetPreventionChecker> MAGNET_CHECKERS = new HashSet<>();
	private static final int COOLDOWN_TICKS = 10;
	private static final int FULL_COOLDOWN_TICKS = 40;
	private final ContentsFilterLogic filterLogic;

	public MagnetUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new ContentsFilterLogic(upgrade, this.upgradeSaveHandler, upgradeItem.getFilterSlotCount(), storageWrapper::getInventoryHandler, "filter_attributes");
	}

	@Override
	public ContentsFilterLogic getFilterLogic() {
		return filterLogic;
	}

	@Override
	public ItemStack pickup(World world, ItemStack stack, boolean simulate) {
		if (!shouldPickupItems() || !filterLogic.matchesFilter(stack)) {
			return stack;
		}
		return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
	}

	@Override
	public void tick(@Nullable Entity entity, World world, BlockPos pos) {
		if (isInCooldown(world)) {
			return;
		}
		setCooldown(world, shouldPickupItems() ? pickupItems(entity, world, pos) : FULL_COOLDOWN_TICKS);
	}

	private int pickupItems(@Nullable Entity entity, World world, BlockPos pos) {
		List<EntityItem> itemEntities = world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos).grow(upgradeItem.getRadius()));
		if (itemEntities.isEmpty()) {
			return COOLDOWN_TICKS;
		}

		int cooldown = COOLDOWN_TICKS;
		EntityPlayer player = entity instanceof EntityPlayer ? (EntityPlayer) entity : null;

		for (EntityItem itemEntity : itemEntities) {
			if (itemEntity.isDead || !filterLogic.matchesFilter(itemEntity.getItem()) || canNotPickup(itemEntity, entity) || isNotPickableYet(itemEntity, entity)) {
				continue;
			}
			if (tryToInsertItem(player, itemEntity)) {
				if (player != null) {
					world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F,
							(world.rand.nextFloat() - world.rand.nextFloat()) * 1.4F + 2.0F);
				}
			} else {
				cooldown = FULL_COOLDOWN_TICKS;
			}
		}
		return cooldown;
	}

	public static void addMagnetPreventionChecker(IMagnetPreventionChecker checker) {
		MAGNET_CHECKERS.add(checker);
	}

	private static boolean isBlockedBySomething(Entity entity) {
		for (IMagnetPreventionChecker checker : MAGNET_CHECKERS) {
			if (checker.isBlocked(entity)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * As a player picks up: not while the item's pickup delay runs, so that one just thrown isn't pulled back at once,
	 * nor while it's kept for the player it belongs to, as vanilla keeps it until 10 seconds before it despawns.
	 */
	private static boolean isNotPickableYet(EntityItem itemEntity, @Nullable Entity entity) {
		if (itemEntity.cannotPickup()) {
			return true;
		}
		String owner = itemEntity.getOwner();
		return owner != null && itemEntity.lifespan - itemEntity.getAge() > 200 && !(entity instanceof EntityPlayer && owner.equals(entity.getName()));
	}

	private static boolean canNotPickup(Entity pickedUpEntity, @Nullable Entity entity) {
		if (isBlockedBySomething(pickedUpEntity)) {
			return true;
		}
		NBTTagCompound data = pickedUpEntity.getEntityData();
		return entity != null ? data.hasKey(PREVENT_REMOTE_MOVEMENT) : data.hasKey(PREVENT_REMOTE_MOVEMENT) && !data.hasKey(ALLOW_MACHINE_MOVEMENT);
	}

	private boolean tryToInsertItem(@Nullable EntityPlayer player, EntityItem itemEntity) {
		ItemStack stack = itemEntity.getItem();
		IItemHandlerSimpleInserter inventory = storageWrapper.getInventoryForUpgradeProcessing();
		ItemStack remaining = inventory.insertItem(stack, true);
		if (remaining.getCount() == stack.getCount()) {
			return false;
		}
		int originalCount = stack.getCount();
		Item item = stack.getItem();
		remaining = inventory.insertItem(stack, false);
		itemEntity.setItem(remaining);
		if (remaining.isEmpty()) {
			itemEntity.setDead();
		}
		if (player != null) {
			player.addStat(StatList.getObjectsPickedUpStats(item), originalCount - remaining.getCount());
		}
		return true;
	}

	private boolean getBoolean(String key) {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag == null || !tag.hasKey(key) || tag.getBoolean(key);
	}

	public void setPickupItems(boolean pickupItems) {
		StorageNbt.getOrCreateTag(upgrade).setBoolean(PICKUP_ITEMS_TAG, pickupItems);
		save();
	}

	public boolean shouldPickupItems() {
		return getBoolean(PICKUP_ITEMS_TAG);
	}

	public void setPickupXp(boolean pickupXp) {
		StorageNbt.getOrCreateTag(upgrade).setBoolean(PICKUP_XP_TAG, pickupXp);
		save();
	}

	public boolean shouldPickupXp() {
		return getBoolean(PICKUP_XP_TAG);
	}
}
