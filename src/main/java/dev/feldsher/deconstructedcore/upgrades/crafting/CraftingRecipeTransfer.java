package dev.feldsher.deconstructedcore.upgrades.crafting;

import dev.feldsher.deconstructedcore.common.gui.StorageContainerMenuBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.util.InventoryHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A recipe a recipe viewer shows, put into a crafting upgrade's grid: what's in the grid goes back to the storage,
 * then each grid slot takes one of the items it accepts, from the storage first, then the player's inventory.
 * With maxTransfer, as many sets as there are, up to a full stack in a slot.
 */
public class CraftingRecipeTransfer {
	private static final int GRID_SLOTS = 9;

	private CraftingRecipeTransfer() {}

	/** The crafting upgrade a recipe goes to: the open tab's, else the first. */
	public static Optional<CraftingUpgradeContainer> getCraftingContainer(StorageContainerMenuBase<?> menu) {
		CraftingUpgradeContainer first = null;
		for (UpgradeContainerBase<?, ?> container : menu.getUpgradeContainers().values()) {
			if (container instanceof CraftingUpgradeContainer) {
				if (container.isOpen()) {
					return Optional.of((CraftingUpgradeContainer) container);
				}
				if (first == null) {
					first = (CraftingUpgradeContainer) container;
				}
			}
		}
		return Optional.ofNullable(first);
	}

	/** The server's: the open tab id the client set before asking, else the first. */
	private static Optional<CraftingUpgradeContainer> getServerCraftingContainer(StorageContainerMenuBase<?> menu) {
		Optional<Integer> openTabId = menu.getStorageWrapper().getOpenTabId();
		if (openTabId.isPresent() && menu.getUpgradeContainers().get(openTabId.get()) instanceof CraftingUpgradeContainer) {
			return Optional.of((CraftingUpgradeContainer) menu.getUpgradeContainers().get(openTabId.get()));
		}
		return getCraftingContainer(menu);
	}

	/** The slots the items come from: the storage's, then the player's inventory, but for what can't be taken. */
	public static List<Slot> getSourceSlots(StorageContainerMenuBase<?> menu, EntityPlayer player) {
		List<Slot> storageSlots = new ArrayList<>();
		List<Slot> playerSlots = new ArrayList<>();
		for (Slot slot : menu.inventorySlots) {
			if (menu.isStorageInventorySlot(slot.slotNumber)) {
				storageSlots.add(slot);
			} else if (slot.inventory instanceof InventoryPlayer && slot.slotNumber != menu.getStorageItemSlotNumber()) {
				playerSlots.add(slot);
			}
		}
		storageSlots.addAll(playerSlots);
		storageSlots.removeIf(slot -> !slot.canTakeStack(player));
		return storageSlots;
	}

	/** Whether the stack is one a recipe slot showing this one accepts: wildcard damage as the ore dictionary's, its tag if it has one. */
	public static boolean matches(ItemStack accepted, ItemStack stack) {
		return !stack.isEmpty() && OreDictionary.itemMatches(accepted, stack, false) && (!accepted.hasTagCompound() || ItemStack.areItemStackTagsEqual(accepted, stack));
	}

	public static void transfer(EntityPlayer player, StorageContainerMenuBase<?> menu, List<List<ItemStack>> grid, ItemStack output, boolean maxTransfer) {
		getServerCraftingContainer(menu).ifPresent(crafting -> {
			List<Slot> gridSlots = crafting.getSlots().subList(0, GRID_SLOTS);
			returnGridContents(player, menu, gridSlots);

			List<Slot> sources = getSourceSlots(menu, player);
			ItemStack[] placed = new ItemStack[GRID_SLOTS];
			for (int i = 0; i < GRID_SLOTS; i++) {
				placed[i] = ItemStack.EMPTY;
			}
			int sets = 0;
			while ((sets == 0 || maxTransfer) && takeSet(player, grid, sources, placed)) {
				sets++;
			}
			for (int i = 0; i < GRID_SLOTS; i++) {
				if (!placed[i].isEmpty()) {
					gridSlots.get(i).putStack(placed[i]);
				}
			}
			selectResult(crafting, output);
			menu.detectAndSendChanges();
		});
	}

	private static void returnGridContents(EntityPlayer player, StorageContainerMenuBase<?> menu, List<Slot> gridSlots) {
		for (Slot slot : gridSlots) {
			if (slot.getHasStack()) {
				ItemStack stack = slot.decrStackSize(slot.getStack().getCount());
				ItemStack remaining = InventoryHelper.insertIntoInventory(stack, menu.getStorageWrapper().getInventoryHandler(), false);
				if (!remaining.isEmpty()) {
					ItemHandlerHelper.giveItemToPlayer(player, remaining);
				}
			}
		}
	}

	/** One more of each item, all or none: none when a slot can't have one more or is full. */
	private static boolean takeSet(EntityPlayer player, List<List<ItemStack>> grid, List<Slot> sources, ItemStack[] placed) {
		Map<Slot, Integer> taking = new HashMap<>();
		Slot[] takenFrom = new Slot[GRID_SLOTS];
		boolean anyIngredient = false;
		for (int i = 0; i < GRID_SLOTS && i < grid.size(); i++) {
			List<ItemStack> accepted = grid.get(i);
			if (accepted.isEmpty()) {
				continue;
			}
			anyIngredient = true;
			ItemStack inGrid = placed[i];
			if (!inGrid.isEmpty() && inGrid.getCount() >= inGrid.getMaxStackSize()) {
				return false;
			}
			Slot source = findSource(accepted, inGrid, sources, taking);
			if (source == null) {
				return false;
			}
			taking.merge(source, 1, Integer::sum);
			takenFrom[i] = source;
		}
		if (!anyIngredient) {
			return false;
		}
		for (int i = 0; i < GRID_SLOTS; i++) {
			if (takenFrom[i] == null) {
				continue;
			}
			ItemStack taken = takenFrom[i].decrStackSize(1);
			takenFrom[i].onTake(player, taken);
			if (placed[i].isEmpty()) {
				placed[i] = taken;
			} else {
				placed[i].grow(taken.getCount());
			}
		}
		return true;
	}

	private static Slot findSource(List<ItemStack> accepted, ItemStack inGrid, List<Slot> sources, Map<Slot, Integer> taking) {
		for (Slot slot : sources) {
			ItemStack stack = slot.getStack();
			if (stack.getCount() - taking.getOrDefault(slot, 0) <= 0) {
				continue;
			}
			if (!inGrid.isEmpty() && !ItemHandlerHelper.canItemStacksStack(inGrid, stack)) {
				continue;
			}
			for (ItemStack acceptedStack : accepted) {
				if (matches(acceptedStack, stack)) {
					return slot;
				}
			}
		}
		return null;
	}

	/** Of the results the grid matches, the one the viewer showed. */
	private static void selectResult(CraftingUpgradeContainer crafting, ItemStack output) {
		List<ItemStack> results = crafting.getMatchedCraftingResults();
		for (int i = 0; i < results.size(); i++) {
			ItemStack result = results.get(i);
			if (ItemStack.areItemsEqual(result, output) && ItemStack.areItemStackTagsEqual(result, output)) {
				crafting.selectCraftingResult(i);
				return;
			}
		}
	}
}
