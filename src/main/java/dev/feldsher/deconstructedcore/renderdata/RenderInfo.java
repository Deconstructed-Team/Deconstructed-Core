package dev.feldsher.deconstructedcore.renderdata;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * What a storage looks like, as clients draw it without its contents: kept on the storage's stack, which reaches every
 * client that sees it, as the stack in a placed storage, the one worn, or one in a hand. Built on the server from the
 * contents and their settings, so that every stack with the same contents shows the same.
 * <p>
 * The item display is here; the upgrade items, the tanks and the battery come with the upgrades that show them.
 */
public final class RenderInfo {
	public static final String RENDER_INFO_TAG = "renderInfo";
	private static final String ITEM_DISPLAY_TAG = "itemDisplay";
	public static final RenderInfo EMPTY = new RenderInfo(ItemDisplayRenderInfo.EMPTY);

	private final ItemDisplayRenderInfo itemDisplayRenderInfo;

	public RenderInfo(ItemDisplayRenderInfo itemDisplayRenderInfo) {
		this.itemDisplayRenderInfo = itemDisplayRenderInfo;
	}

	public ItemDisplayRenderInfo getItemDisplayRenderInfo() {
		return itemDisplayRenderInfo;
	}

	public NBTTagCompound serialize() {
		NBTTagCompound tag = new NBTTagCompound();
		if (!itemDisplayRenderInfo.getDisplayItems().isEmpty()) {
			tag.setTag(ITEM_DISPLAY_TAG, itemDisplayRenderInfo.serialize());
		}
		return tag;
	}

	public static RenderInfo deserialize(NBTTagCompound tag) {
		if (!tag.hasKey(ITEM_DISPLAY_TAG)) {
			return EMPTY;
		}
		return new RenderInfo(ItemDisplayRenderInfo.deserialize(tag.getCompoundTag(ITEM_DISPLAY_TAG)));
	}

	/** The items the storage shows. The slot counts and fill ratios come with the storages that show them. */
	public static final class ItemDisplayRenderInfo {
		private static final String ITEMS_TAG = "items";
		public static final ItemDisplayRenderInfo EMPTY = new ItemDisplayRenderInfo(Collections.emptyList());
		private final List<DisplayItem> displayItems;

		public ItemDisplayRenderInfo(List<DisplayItem> displayItems) {
			this.displayItems = displayItems;
		}

		/** One item is kept flat, as 1.21 does, more in a list. */
		private NBTTagCompound serialize() {
			NBTTagCompound ret = new NBTTagCompound();
			if (displayItems.size() == 1) {
				displayItems.get(0).serialize(ret);
			} else {
				NBTTagList items = new NBTTagList();
				displayItems.forEach(displayItem -> items.appendTag(displayItem.serialize(new NBTTagCompound())));
				ret.setTag(ITEMS_TAG, items);
			}
			return ret;
		}

		private static ItemDisplayRenderInfo deserialize(NBTTagCompound tag) {
			List<DisplayItem> items = new ArrayList<>();
			if (tag.hasKey(DisplayItem.ITEM_TAG)) {
				items.add(DisplayItem.deserialize(tag));
			} else {
				NBTTagList itemsTag = tag.getTagList(ITEMS_TAG, Constants.NBT.TAG_COMPOUND);
				for (int i = 0; i < itemsTag.tagCount(); i++) {
					items.add(DisplayItem.deserialize(itemsTag.getCompoundTagAt(i)));
				}
			}
			return new ItemDisplayRenderInfo(items);
		}

		public Optional<DisplayItem> getDisplayItem() {
			return displayItems.isEmpty() ? Optional.empty() : Optional.of(displayItems.get(0));
		}

		public List<DisplayItem> getDisplayItems() {
			return displayItems;
		}
	}

	public static final class DisplayItem {
		private static final String ITEM_TAG = "item";
		private static final String ROTATION_TAG = "rotation";
		private static final String SLOT_INDEX_TAG = "slotIndex";
		private static final String DISPLAY_SIDE_TAG = "displaySide";
		private final ItemStack item;
		private final int rotation;
		private final int slotIndex;
		private final DisplaySide displaySide;

		public DisplayItem(ItemStack item, int rotation, int slotIndex, DisplaySide displaySide) {
			this.item = item;
			this.rotation = rotation;
			this.slotIndex = slotIndex;
			this.displaySide = displaySide;
		}

		private NBTTagCompound serialize(NBTTagCompound tag) {
			tag.setTag(ITEM_TAG, item.writeToNBT(new NBTTagCompound()));
			tag.setInteger(ROTATION_TAG, rotation);
			tag.setInteger(SLOT_INDEX_TAG, slotIndex);
			tag.setString(DISPLAY_SIDE_TAG, displaySide.getSerializedName());
			return tag;
		}

		private static DisplayItem deserialize(NBTTagCompound tag) {
			return new DisplayItem(new ItemStack(tag.getCompoundTag(ITEM_TAG)), tag.getInteger(ROTATION_TAG), tag.getInteger(SLOT_INDEX_TAG),
					DisplaySide.fromName(tag.getString(DISPLAY_SIDE_TAG)));
		}

		public ItemStack getItem() {
			return item;
		}

		public int getRotation() {
			return rotation;
		}

		public int getSlotIndex() {
			return slotIndex;
		}

		public DisplaySide getDisplaySide() {
			return displaySide;
		}
	}
}
