package dev.feldsher.deconstructedcore.util;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.inventory.ItemStackKey;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static dev.feldsher.deconstructedcore.util.RecipeHelper.CompactingShape.NONE;
import static dev.feldsher.deconstructedcore.util.RecipeHelper.CompactingShape.THREE_BY_THREE;
import static dev.feldsher.deconstructedcore.util.RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE;
import static dev.feldsher.deconstructedcore.util.RecipeHelper.CompactingShape.TWO_BY_TWO;
import static dev.feldsher.deconstructedcore.util.RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE;

/**
 * What an item compacts into in a 2x2 or 3x3 grid of itself, and whether that uncrafts back into the same items, as
 * nuggets, ingots and blocks do. 1.12's recipes are all in the registry, so a lookup goes through every crafting recipe;
 * the results are cached, and the caches are cleared when the server starts, as recipe scripts may have changed them.
 */
public class RecipeHelper {
	private static final int MAX_FOLLOW_UP_COMPACTING_RECIPES = 30;
	private static final Map<CompactedItem, CompactingResult> COMPACTING_RESULTS = new ConcurrentHashMap<>();
	private static final Map<ItemStackKey, Set<CompactingShape>> ITEM_COMPACTING_SHAPES = new ConcurrentHashMap<>();
	private static final Map<ItemStackKey, UncompactingResult> UNCOMPACTING_RESULTS = new ConcurrentHashMap<>();

	private RecipeHelper() {}

	public static void clearCache() {
		COMPACTING_RESULTS.clear();
		ITEM_COMPACTING_SHAPES.clear();
		UNCOMPACTING_RESULTS.clear();
	}

	/** Few recipes look at the world; those that do get one, the server's when there is one here. */
	@Nullable
	private static World getWorld() {
		World world = DimensionManager.getWorld(0);
		if (world == null && FMLCommonHandler.instance().getSide().isClient()) {
			world = ClientWorld.get();
		}
		return world;
	}

	public static Set<CompactingShape> getItemCompactingShapes(ItemStack stack) {
		if (stack.isEmpty()) {
			return Collections.emptySet();
		}
		return ITEM_COMPACTING_SHAPES.computeIfAbsent(ItemStackKey.of(stack), key -> {
			Set<CompactingShape> compactingShapes = EnumSet.noneOf(CompactingShape.class);
			getCompactingShape(key.getStack(), 2, 2, TWO_BY_TWO_UNCRAFTABLE, TWO_BY_TWO).ifPresent(compactingShapes::add);
			getCompactingShape(key.getStack(), 3, 3, THREE_BY_THREE_UNCRAFTABLE, THREE_BY_THREE).ifPresent(compactingShapes::add);
			if (compactingShapes.isEmpty()) {
				compactingShapes.add(NONE);
			}
			return compactingShapes;
		});
	}

	private static java.util.Optional<CompactingShape> getCompactingShape(ItemStack stack, int width, int height, CompactingShape uncraftableShape, CompactingShape shape) {
		CompactingResult compactingResult = getCompactingResult(stack, width, height);
		if (compactingResult.getResult().isEmpty() || isSameItem(stack, compactingResult.getResult())
				|| isPartOfCompactingLoop(stack, compactingResult.getResult())) {
			return java.util.Optional.empty();
		}
		return java.util.Optional.of(uncompactMatchesItem(compactingResult.getResult(), stack, width * height) ? uncraftableShape : shape);
	}

	private static boolean isSameItem(ItemStack a, ItemStack b) {
		return ItemHandlerHelper.canItemStacksStack(a, b);
	}

	/** Whether compacting the result, and what that gives, and so on, ever gives the first item back. */
	private static boolean isPartOfCompactingLoop(ItemStack firstCompacted, ItemStack firstCompactResult) {
		int iterations = 0;
		Set<ItemStackKey> compactedItems = new HashSet<>();
		Queue<ItemStack> itemsToCompact = new LinkedList<>();
		itemsToCompact.add(firstCompactResult);
		while (!itemsToCompact.isEmpty()) {
			ItemStack itemToCompact = itemsToCompact.poll();
			for (int size = 2; size <= 3; size++) {
				ItemStack compactingResultStack = getCompactingResult(itemToCompact, size, size).getResult();
				if (!compactingResultStack.isEmpty()) {
					if (isSameItem(compactingResultStack, firstCompacted)) {
						return true;
					} else if (compactedItems.contains(ItemStackKey.of(compactingResultStack))) {
						// a loop the first item isn't part of: it's compacted, but nothing further is followed
						return false;
					}
					itemsToCompact.add(compactingResultStack);
				}
			}
			compactedItems.add(ItemStackKey.of(itemToCompact));
			iterations++;
			if (iterations > MAX_FOLLOW_UP_COMPACTING_RECIPES) {
				// too many follow up recipes to tell, so not compacted at all
				return true;
			}
		}
		return false;
	}

	private static boolean uncompactMatchesItem(ItemStack itemToUncompact, ItemStack itemToMatch, int count) {
		for (ItemStack uncompactResult : getUncompactResultItems(itemToUncompact)) {
			if (isSameItem(uncompactResult, itemToMatch) && uncompactResult.getCount() == count) {
				return true;
			}
		}
		return false;
	}

	/** What the item uncrafts into, alone in the grid, when those compact back into it. */
	public static UncompactingResult getUncompactingResult(ItemStack uncompactedItem) {
		return UNCOMPACTING_RESULTS.computeIfAbsent(ItemStackKey.of(uncompactedItem), key -> {
			for (ItemStack uncompactResultItem : getUncompactResultItems(key.getStack())) {
				if (uncompactResultItem.getCount() == 9) {
					if (isSameItem(getCompactingResult(uncompactResultItem, 3, 3).getResult(), key.getStack())) {
						return new UncompactingResult(uncompactResultItem, THREE_BY_THREE_UNCRAFTABLE);
					}
				} else if (uncompactResultItem.getCount() == 4 && isSameItem(getCompactingResult(uncompactResultItem, 2, 2).getResult(), key.getStack())) {
					return new UncompactingResult(uncompactResultItem, TWO_BY_TWO_UNCRAFTABLE);
				}
			}
			return UncompactingResult.EMPTY;
		});
	}

	private static List<ItemStack> getUncompactResultItems(ItemStack itemToUncompact) {
		InventoryCrafting craftingInventory = getFilledCraftingInventory(itemToUncompact, 1, 1);
		List<ItemStack> results = new ArrayList<>();
		for (IRecipe recipe : getMatchingRecipes(craftingInventory)) {
			results.add(recipe.getCraftingResult(craftingInventory));
		}
		return results;
	}

	public static CompactingResult getCompactingResult(ItemStack stack, CompactingShape shape) {
		if (shape == TWO_BY_TWO_UNCRAFTABLE || shape == TWO_BY_TWO) {
			return getCompactingResult(stack, 2, 2);
		} else if (shape == THREE_BY_THREE_UNCRAFTABLE || shape == THREE_BY_THREE) {
			return getCompactingResult(stack, 3, 3);
		}
		return CompactingResult.EMPTY;
	}

	/** Of the recipes that take the grid, the one that uncrafts back into the item, else the first. */
	public static CompactingResult getCompactingResult(ItemStack stack, int width, int height) {
		CompactedItem compactedItem = new CompactedItem(stack, width, height);
		CompactingResult cached = COMPACTING_RESULTS.get(compactedItem);
		if (cached != null) {
			return cached;
		}

		InventoryCrafting craftingInventory = getFilledCraftingInventory(stack, width, height);
		List<IRecipe> compactingRecipes = getMatchingRecipes(craftingInventory);

		CompactingResult compactingResult = CompactingResult.EMPTY;
		if (compactingRecipes.size() == 1) {
			compactingResult = getCompactingResult(compactingRecipes.get(0), craftingInventory, compactingRecipes.get(0).getCraftingResult(craftingInventory));
		} else if (!compactingRecipes.isEmpty()) {
			for (IRecipe recipe : compactingRecipes) {
				ItemStack result = recipe.getCraftingResult(craftingInventory);
				if (uncompactMatchesItem(result, stack, width * height)) {
					compactingResult = getCompactingResult(recipe, craftingInventory, result);
					break;
				}
			}
			if (compactingResult == CompactingResult.EMPTY) {
				compactingResult = getCompactingResult(compactingRecipes.get(0), craftingInventory, compactingRecipes.get(0).getCraftingResult(craftingInventory));
			}
		}
		COMPACTING_RESULTS.put(compactedItem, compactingResult);
		return compactingResult;
	}

	private static CompactingResult getCompactingResult(IRecipe recipe, InventoryCrafting craftingInventory, ItemStack result) {
		if (result.isEmpty()) {
			return CompactingResult.EMPTY;
		}
		List<ItemStack> remainingItems = new ArrayList<>();
		for (ItemStack stack : recipe.getRemainingItems(craftingInventory)) {
			if (!stack.isEmpty()) {
				remainingItems.add(stack);
			}
		}
		return new CompactingResult(result, remainingItems);
	}

	private static List<IRecipe> getMatchingRecipes(InventoryCrafting craftingInventory) {
		World world = getWorld();
		List<IRecipe> recipes = new ArrayList<>();
		for (IRecipe recipe : ForgeRegistries.RECIPES.getValuesCollection()) {
			try {
				if (recipe.matches(craftingInventory, world)) {
					recipes.add(recipe);
				}
			} catch (RuntimeException e) {
				DeconstructedCore.LOGGER.debug("Recipe {} failed to match", recipe.getRegistryName(), e);
			}
		}
		return recipes;
	}

	private static InventoryCrafting getFilledCraftingInventory(ItemStack stack, int width, int height) {
		InventoryCrafting craftingInventory = new InventoryCrafting(new Container() {
			@Override
			public boolean canInteractWith(EntityPlayer player) {
				return false;
			}

			@Override
			public void onCraftMatrixChanged(net.minecraft.inventory.IInventory inventory) {
				//noop
			}
		}, width, height);

		for (int i = 0; i < craftingInventory.getSizeInventory(); i++) {
			craftingInventory.setInventorySlotContents(i, ItemHandlerHelper.copyStackWithSize(stack, 1));
		}
		return craftingInventory;
	}

	public enum CompactingShape {
		NONE,
		TWO_BY_TWO_UNCRAFTABLE,
		TWO_BY_TWO,
		THREE_BY_THREE_UNCRAFTABLE,
		THREE_BY_THREE
	}

	public static class CompactingResult {
		public static final CompactingResult EMPTY = new CompactingResult(ItemStack.EMPTY, Collections.emptyList());
		private final ItemStack result;
		private final List<ItemStack> remainingItems;

		public CompactingResult(ItemStack result, List<ItemStack> remainingItems) {
			this.result = result;
			this.remainingItems = remainingItems;
		}

		public ItemStack getResult() {
			return result;
		}

		public List<ItemStack> getRemainingItems() {
			return remainingItems;
		}
	}

	public static class UncompactingResult {
		public static final UncompactingResult EMPTY = new UncompactingResult(ItemStack.EMPTY, NONE);
		private final ItemStack result;
		private final CompactingShape compactUsingShape;

		public UncompactingResult(ItemStack result, CompactingShape compactUsingShape) {
			this.result = ItemHandlerHelper.copyStackWithSize(result, 1);
			this.compactUsingShape = compactUsingShape;
		}

		public ItemStack getResult() {
			return result;
		}

		public CompactingShape getCompactUsingShape() {
			return compactUsingShape;
		}
	}

	private static final class CompactedItem {
		private final ItemStackKey item;
		private final int width;
		private final int height;

		private CompactedItem(ItemStack item, int width, int height) {
			this.item = ItemStackKey.of(item);
			this.width = width;
			this.height = height;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			CompactedItem that = (CompactedItem) o;
			return width == that.width && height == that.height && item.equals(that.item);
		}

		@Override
		public int hashCode() {
			return Objects.hash(item, width, height);
		}
	}

	/** Kept apart, so that the client's classes load only on the client. */
	private static final class ClientWorld {
		private ClientWorld() {}

		@Nullable
		static World get() {
			return net.minecraft.client.Minecraft.getMinecraft().world;
		}
	}
}
