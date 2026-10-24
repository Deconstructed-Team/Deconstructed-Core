package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.common.gui.UpgradeSlotChangeResult;
import dev.feldsher.deconstructedcore.util.InventoryHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public interface IUpgradeItem<T extends IUpgradeWrapper> {
	UpgradeType<T> getType();

	default UpgradeSlotChangeResult canAddUpgradeTo(IStorageWrapper storageWrapper, ItemStack upgradeStack, boolean firstLevelStorage, boolean isClientSide) {
		UpgradeSlotChangeResult result = checkUpgradePerStorageTypeLimit(storageWrapper);

		if (!result.successful()) {
			return result;
		}

		result = checkForConflictingUpgrades(storageWrapper, getUpgradeConflicts(), -1);
		if (!result.successful()) {
			return result;
		}

		result = checkThisForConflictsWithExistingUpgrades(upgradeStack, storageWrapper, -1);
		if (!result.successful()) {
			return result;
		}

		return checkExtraInsertConditions(upgradeStack, storageWrapper, isClientSide, null);
	}

	default UpgradeSlotChangeResult checkThisForConflictsWithExistingUpgrades(ItemStack upgradeStack, IStorageWrapper storageWrapper, int excludeUpgradeSlot) {
		AtomicReference<UpgradeSlotChangeResult> result = new AtomicReference<>(UpgradeSlotChangeResult.success());
		InventoryHelper.iterate(storageWrapper.getUpgradeHandler(), (slot, stack) -> {
			if (slot != excludeUpgradeSlot && stack.getItem() instanceof IUpgradeItem<?>) {
				for (UpgradeConflictDefinition conflictDefinition : ((IUpgradeItem<?>) stack.getItem()).getUpgradeConflicts()) {
					//only checking for single item conflicts here would need to be expanded to support multiple,
					// but there isn't a case like that at the moment because the other conflict check (the one where item inserted checks items that exist) covers all multiple item cases
					if (conflictDefinition.maxConflictingAllowed == 0 && conflictDefinition.isConflictingItem.test(upgradeStack.getItem())) {
						result.set(UpgradeSlotChangeResult.fail(conflictDefinition.otherBeingAddedErrorMessage, Collections.singleton(slot), Collections.emptySet()));
						return;
					}
				}
			}
		}, () -> !result.get().successful());
		return result.get();
	}

	default UpgradeSlotChangeResult checkForConflictingUpgrades(IStorageWrapper storageWrapper, List<UpgradeConflictDefinition> upgradeConflicts, int excludeUpgradeSlot) {
		for (UpgradeConflictDefinition conflictDefinition : upgradeConflicts) {
			AtomicInteger conflictingCount = new AtomicInteger(0);
			Set<Integer> conflictingSlots = new HashSet<>();
			InventoryHelper.iterate(storageWrapper.getUpgradeHandler(), (slot, stack) -> {
				if (slot != excludeUpgradeSlot && !stack.isEmpty() && conflictDefinition.isConflictingItem.test(stack.getItem())) {
					conflictingCount.incrementAndGet();
					conflictingSlots.add(slot);
				}
			});

			if (conflictingCount.get() > conflictDefinition.maxConflictingAllowed) {
				return UpgradeSlotChangeResult.fail(conflictDefinition.errorMessage, conflictingSlots, Collections.emptySet());
			}
		}
		return UpgradeSlotChangeResult.success();
	}

	List<UpgradeConflictDefinition> getUpgradeConflicts();

	default UpgradeSlotChangeResult checkUpgradePerStorageTypeLimit(IStorageWrapper storageWrapper) {
		int upgradesPerStorage = getUpgradesPerStorage(storageWrapper.getStorageType());
		int upgradesInGroupPerStorage = getUpgradesInGroupPerStorage(storageWrapper.getStorageType());

		if (upgradesPerStorage == Integer.MAX_VALUE && upgradesInGroupPerStorage == Integer.MAX_VALUE) {
			return UpgradeSlotChangeResult.success();
		}

		if (upgradesPerStorage == 0) {
			return UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("add.upgrade_not_allowed", getName(), storageWrapper.getDisplayName()), Collections.emptySet(), Collections.emptySet());
		} else if (upgradesInGroupPerStorage == 0) {
			return UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("add.upgrade_not_allowed", new TextComponentTranslation(getUpgradeGroup().translName()), storageWrapper.getDisplayName()), Collections.emptySet(), Collections.emptySet());
		}

		Set<Integer> slotsWithUpgrade = new HashSet<>();
		InventoryHelper.iterate(storageWrapper.getUpgradeHandler(), (slot, stack) -> {
			if (stack.getItem() == this) {
				slotsWithUpgrade.add(slot);
			}
		});

		if (slotsWithUpgrade.size() >= upgradesPerStorage) {
			return UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("add.only_x_upgrades_allowed", upgradesPerStorage, getName(), storageWrapper.getDisplayName()), slotsWithUpgrade, Collections.emptySet());
		}

		Set<Integer> slotsWithUpgradeGroup = new HashSet<>();
		InventoryHelper.iterate(storageWrapper.getUpgradeHandler(), (slot, stack) -> {
			if (stack.getItem() instanceof IUpgradeItem<?> && ((IUpgradeItem<?>) stack.getItem()).getUpgradeGroup() == getUpgradeGroup()) {
				slotsWithUpgradeGroup.add(slot);
			}
		});

		if (slotsWithUpgradeGroup.size() >= upgradesInGroupPerStorage) {
			return UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("add.only_x_upgrades_allowed", upgradesInGroupPerStorage, new TextComponentTranslation(getUpgradeGroup().translName()), storageWrapper.getDisplayName()), slotsWithUpgradeGroup, Collections.emptySet());
		}

		return UpgradeSlotChangeResult.success();
	}

	default UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper, boolean isClientSide, EntityPlayer player) {
		return canRemoveUpgradeFrom(storageWrapper, isClientSide);
	}

	default UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper, boolean isClientSide) {
		return UpgradeSlotChangeResult.success();
	}

	default UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack upgradeStackToPut, int upgradeSlot, IStorageWrapper storageWrapper, boolean isClientSide) {
		if (upgradeStackToPut.getItem() == this) {
			return UpgradeSlotChangeResult.success();
		}

		if (upgradeStackToPut.getItem() instanceof IUpgradeItem<?>) {
			IUpgradeItem<?> upgradeToPut = (IUpgradeItem<?>) upgradeStackToPut.getItem();
			int upgradesPerStorage = upgradeToPut.getUpgradesPerStorage(storageWrapper.getStorageType());
			int upgradesInGroupPerStorage = upgradeToPut.getUpgradesInGroupPerStorage(storageWrapper.getStorageType());

			if (upgradesPerStorage < upgradesInGroupPerStorage || upgradeToPut.getUpgradeGroup() != getUpgradeGroup()) {
				UpgradeSlotChangeResult result = upgradeToPut.checkUpgradePerStorageTypeLimit(storageWrapper);
				if (!result.successful()) {
					return result;
				}
			}

			UpgradeSlotChangeResult result = checkForConflictingUpgrades(storageWrapper, upgradeToPut.getUpgradeConflicts(), upgradeSlot);
			if (!result.successful()) {
				return result;
			}
			return upgradeToPut.checkExtraInsertConditions(upgradeStackToPut, storageWrapper, isClientSide, this);
		}

		return UpgradeSlotChangeResult.success();
	}

	default UpgradeSlotChangeResult checkExtraInsertConditions(ItemStack upgradeStack, IStorageWrapper storageWrapper, boolean isClientSide, @Nullable IUpgradeItem<?> upgradeInSlot) {
		return UpgradeSlotChangeResult.success();
	}

	default int getInventoryColumnsTaken() {
		return 0;
	}

	default ItemStack getCleanedUpgradeStack(ItemStack upgradeStack) {
		return upgradeStack;
	}

	int getUpgradesPerStorage(String storageType);

	int getUpgradesInGroupPerStorage(String storageType);

	default UpgradeGroup getUpgradeGroup() {
		return UpgradeGroup.NONE;
	}

	ITextComponent getName();

	final class UpgradeConflictDefinition {
		public final Predicate<Item> isConflictingItem;
		public final int maxConflictingAllowed;
		public final ITextComponent errorMessage;
		public final ITextComponent otherBeingAddedErrorMessage;

		public UpgradeConflictDefinition(Predicate<Item> isConflictingItem, int maxConflictingAllowed, ITextComponent errorMessage, ITextComponent otherBeingAddedErrorMessage) {
			this.isConflictingItem = isConflictingItem;
			this.maxConflictingAllowed = maxConflictingAllowed;
			this.errorMessage = errorMessage;
			this.otherBeingAddedErrorMessage = otherBeingAddedErrorMessage;
		}

		public UpgradeConflictDefinition(Predicate<Item> isConflictingItem, int maxConflictingAllowed, ITextComponent errorMessage) {
			this(isConflictingItem, maxConflictingAllowed, errorMessage, errorMessage);
		}
	}
}
