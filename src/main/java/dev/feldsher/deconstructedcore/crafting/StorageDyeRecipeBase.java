package dev.feldsher.deconstructedcore.crafting;

import dev.feldsher.deconstructedcore.util.ColorHelper;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A storage item and dyes, in any shape: the dyes in its column and left of it color its main part, those in its
 * column and right of it its trim.
 */
public abstract class StorageDyeRecipeBase extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
	@Override
	public boolean matches(InventoryCrafting inv, World world) {
		boolean storagePresent = false;
		boolean dyePresent = false;
		for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
			ItemStack slotStack = inv.getStackInSlot(slot);
			if (slotStack.isEmpty()) {
				continue;
			}
			if (isDyeableStorageItem(slotStack)) {
				if (storagePresent) {
					return false;
				}
				storagePresent = true;
			} else if (ColorHelper.getDyeColor(slotStack) != null) {
				dyePresent = true;
			} else {
				return false;
			}
		}
		return storagePresent && dyePresent;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {
		Map<Integer, List<EnumDyeColor>> columnDyes = new HashMap<>();
		ItemStack storage = ItemStack.EMPTY;
		int storageColumn = -1;

		for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
			ItemStack slotStack = inv.getStackInSlot(slot);
			if (slotStack.isEmpty()) {
				continue;
			}
			int column = slot % inv.getWidth();
			if (isDyeableStorageItem(slotStack)) {
				if (!storage.isEmpty()) {
					return ItemStack.EMPTY;
				}
				storage = slotStack;
				storageColumn = column;
			} else {
				EnumDyeColor dyeColor = ColorHelper.getDyeColor(slotStack);
				if (dyeColor == null) {
					return ItemStack.EMPTY;
				}
				columnDyes.computeIfAbsent(column, c -> new ArrayList<>()).add(dyeColor);
			}
		}
		if (storage.isEmpty()) {
			return ItemStack.EMPTY;
		}

		ItemStack coloredStorage = storage.copy();
		coloredStorage.setCount(1);
		List<EnumDyeColor> mainDyes = new ArrayList<>();
		List<EnumDyeColor> trimDyes = new ArrayList<>();
		for (Map.Entry<Integer, List<EnumDyeColor>> entry : columnDyes.entrySet()) {
			if (entry.getKey() <= storageColumn) {
				mainDyes.addAll(entry.getValue());
			}
			if (entry.getKey() >= storageColumn) {
				trimDyes.addAll(entry.getValue());
			}
		}
		applyColors(coloredStorage, mainDyes, trimDyes);
		return coloredStorage;
	}

	protected abstract boolean isDyeableStorageItem(ItemStack stack);

	protected abstract void applyColors(ItemStack coloredStorage, List<EnumDyeColor> mainDyes, List<EnumDyeColor> trimDyes);

	@Override
	public boolean canFit(int width, int height) {
		return width >= 2 && height >= 1;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}
}
