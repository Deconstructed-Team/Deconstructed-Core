package dev.feldsher.deconstructedcore.upgrades.infinity;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.common.gui.UpgradeSlotChangeResult;
import dev.feldsher.deconstructedcore.inventory.IInventoryPartHandler;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.inventory.InventoryPartRegistry;
import dev.feldsher.deconstructedcore.inventory.SlotRange;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Makes everything in the storage infinite: its whole inventory becomes one {@link InfinityInventoryPart}. It goes
 * only into a storage with no other upgrade and lets none in beside it. The admin one only admins can take out; the
 * survival one anyone.
 */
public class InfinityUpgradeItem extends UpgradeItemBase<InfinityUpgradeItem.Wrapper> {
	public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);
	private static final List<UpgradeConflictDefinition> UPGRADE_CONFLICT_DEFINITIONS = Collections.singletonList(new UpgradeConflictDefinition(i -> true, 0,
			TranslationHelper.INSTANCE.translError("add.any_upgrade_exists"), TranslationHelper.INSTANCE.translError("add.no_upgrade_can_be_added")));

	static {
		InventoryPartRegistry.registerFactory(InfinityInventoryPart.Admin.NAME, (parent, slotRange, getMemorySettings) -> new InfinityInventoryPart.Admin(parent, slotRange));
		InventoryPartRegistry.registerFactory(InfinityInventoryPart.Survival.NAME, (parent, slotRange, getMemorySettings) -> new InfinityInventoryPart.Survival(parent, slotRange));
	}

	private final boolean admin;

	public InfinityUpgradeItem(IUpgradeCountLimitConfig upgradeTypeLimitConfig, boolean admin) {
		super(upgradeTypeLimitConfig);
		this.admin = admin;
	}

	@Override
	public UpgradeType<Wrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return UPGRADE_CONFLICT_DEFINITIONS;
	}

	@Override
	public UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper, boolean isClientSide, EntityPlayer player) {
		if (hasPermission(player, getPermissionLevel())) {
			return super.canRemoveUpgradeFrom(storageWrapper, isClientSide, player);
		}
		return UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("remove.infinity_upgrade_only_admin"),
				Collections.emptySet(), Collections.emptySet());
	}

	public int getPermissionLevel() {
		return admin ? 2 : 0;
	}

	/** Level 0 is everyone's: 1.12 gives a player who isn't an op no command level at all. */
	public static boolean hasPermission(EntityPlayer player, int permissionLevel) {
		return permissionLevel <= 0 || player.canUseCommand(permissionLevel, "");
	}

	public IInventoryPartHandler createInventoryPartHandler(InventoryHandler parent, SlotRange slotRange) {
		return admin ? new InfinityInventoryPart.Admin(parent, slotRange) : new InfinityInventoryPart.Survival(parent, slotRange);
	}

	public static class Wrapper extends UpgradeWrapperBase<Wrapper, InfinityUpgradeItem> {
		protected Wrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler);
		}

		@Override
		public boolean canBeDisabled() {
			return false;
		}

		@Override
		public void onAdded() {
			super.onAdded();
			InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
			inventoryHandler.getInventoryPartitioner().addInventoryPart(0, Integer.MAX_VALUE,
					upgradeItem.createInventoryPartHandler(inventoryHandler, new SlotRange(0, inventoryHandler.getSlots())));
		}

		@Override
		public void onBeforeRemoved() {
			super.onBeforeRemoved();
			storageWrapper.getInventoryHandler().getInventoryPartitioner().removeInventoryPart(0);
			save();
		}

		public int getPermissionLevel() {
			return upgradeItem.getPermissionLevel();
		}
	}
}
