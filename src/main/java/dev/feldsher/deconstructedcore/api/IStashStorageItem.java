package dev.feldsher.deconstructedcore.api;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/**
 * A storage item that items can be stashed into in any inventory screen: right clicked with it onto an item, or with
 * the item onto it. 1.21 does it through the item's stacked-on hooks, which 1.12 doesn't have; here the click is caught
 * on the client and done on the server.
 */
public interface IStashStorageItem {
	/** What the stack in a slot of the storage stack can be: also run on the client, to show which ones can. */
	StashResult getItemStashable(ItemStack storageStack, ItemStack stack);

	/** Puts the stack into the storage; what doesn't fit is given back. */
	ItemStack stash(ItemStack storageStack, ItemStack stack, boolean simulate);

	/** After a stash on the server, as for the client to see the storage's new contents. */
	default void onStashed(EntityPlayerMP player, ItemStack storageStack) {
		//noop
	}

	enum StashResult {
		/** It has the item already, or remembers it in a slot, and room for it. */
		MATCH_AND_SPACE,
		SPACE,
		NO_SPACE
	}
}
