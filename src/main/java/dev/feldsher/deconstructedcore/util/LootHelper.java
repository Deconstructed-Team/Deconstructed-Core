package dev.feldsher.deconstructedcore.util;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.List;
import java.util.Random;

public class LootHelper {
	private LootHelper() {}

	/** What the loot table gives, as a chest's does to the player opening it. */
	public static List<ItemStack> getLoot(ResourceLocation lootTableName, WorldServer world, EntityPlayer player) {
		LootTable lootTable = world.getLootTableManager().getLootTableFromLocation(lootTableName);
		LootContext context = new LootContext.Builder(world).withLuck(player.getLuck()).withPlayer(player).build();
		return lootTable.generateLootForPools(world.rand, context);
	}

	/** Puts the loot in the inventory's empty slots at random, spread out as a loot chest's is. */
	public static void fillWithLoot(Random rand, List<ItemStack> loot, IItemHandlerModifiable inventory) {
		List<Integer> slots = InventoryHelper.getEmptySlotsRandomized(inventory, rand);
		InventoryHelper.shuffleItems(loot, slots.size(), rand);

		for (ItemStack lootStack : loot) {
			if (slots.isEmpty()) {
				DeconstructedCore.LOGGER.warn("Too much loot to add to container. Overflow is voided.");
				return;
			}

			if (!lootStack.isEmpty()) {
				inventory.setStackInSlot(slots.remove(slots.size() - 1), lootStack);
			}
		}
	}
}
