package dev.feldsher.deconstructedcore.renderdata;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a storage looks like, as clients draw it without its contents: kept on the storage's stack, which reaches every
 * client that sees it, as the stack in a placed storage, the one worn, or one in a hand. Built on the server from the
 * contents and their settings, so that every stack with the same contents shows the same.
 * <p>
 * It has the items displayed, the tanks and the battery, and what working upgrades show, by their type. The upgrade
 * items, which only placed storages show, a placed storage's render info has along, one per upgrade slot.
 */
public final class RenderInfo {
	public static final String RENDER_INFO_TAG = "renderInfo";
	private static final String ITEM_DISPLAY_TAG = "itemDisplay";
	private static final String TANKS_TAG = "tanks";
	private static final String TANK_POSITION_TAG = "position";
	private static final String TANK_INFO_TAG = "info";
	private static final String BATTERY_TAG = "battery";
	private static final String UPGRADES_TAG = "upgrades";
	private static final String UPGRADE_ITEMS_TAG = "upgradeItems";
	public static final RenderInfo EMPTY = new RenderInfo(ItemDisplayRenderInfo.EMPTY);

	private final ItemDisplayRenderInfo itemDisplayRenderInfo;
	private final Map<TankPosition, TankRenderInfo> tankRenderInfos;
	@Nullable
	private final BatteryRenderInfo batteryRenderInfo;
	private final Map<String, NBTTagCompound> upgradeRenderData;
	private final List<ItemStack> upgradeItems;

	public RenderInfo(ItemDisplayRenderInfo itemDisplayRenderInfo) {
		this(itemDisplayRenderInfo, Collections.emptyMap(), null, Collections.emptyMap());
	}

	public RenderInfo(ItemDisplayRenderInfo itemDisplayRenderInfo, Map<TankPosition, TankRenderInfo> tankRenderInfos, @Nullable BatteryRenderInfo batteryRenderInfo,
			Map<String, NBTTagCompound> upgradeRenderData) {
		this(itemDisplayRenderInfo, tankRenderInfos, batteryRenderInfo, upgradeRenderData, Collections.emptyList());
	}

	private RenderInfo(ItemDisplayRenderInfo itemDisplayRenderInfo, Map<TankPosition, TankRenderInfo> tankRenderInfos, @Nullable BatteryRenderInfo batteryRenderInfo,
			Map<String, NBTTagCompound> upgradeRenderData, List<ItemStack> upgradeItems) {
		this.itemDisplayRenderInfo = itemDisplayRenderInfo;
		this.tankRenderInfos = tankRenderInfos;
		this.batteryRenderInfo = batteryRenderInfo;
		this.upgradeRenderData = upgradeRenderData;
		this.upgradeItems = upgradeItems;
	}

	/** This with the upgrade items, single, empty for an empty upgrade slot. */
	public RenderInfo withUpgradeItems(List<ItemStack> upgradeItems) {
		return new RenderInfo(itemDisplayRenderInfo, tankRenderInfos, batteryRenderInfo, upgradeRenderData, upgradeItems);
	}

	public List<ItemStack> getUpgradeItems() {
		return upgradeItems;
	}

	public ItemDisplayRenderInfo getItemDisplayRenderInfo() {
		return itemDisplayRenderInfo;
	}

	public Map<TankPosition, TankRenderInfo> getTankRenderInfos() {
		return tankRenderInfos;
	}

	public Optional<BatteryRenderInfo> getBatteryRenderInfo() {
		return Optional.ofNullable(batteryRenderInfo);
	}

	public Map<String, NBTTagCompound> getUpgradeRenderData() {
		return upgradeRenderData;
	}

	public NBTTagCompound serialize() {
		NBTTagCompound tag = new NBTTagCompound();
		if (!itemDisplayRenderInfo.isEmpty()) {
			tag.setTag(ITEM_DISPLAY_TAG, itemDisplayRenderInfo.serialize());
		}
		if (!tankRenderInfos.isEmpty()) {
			NBTTagList tanks = new NBTTagList();
			tankRenderInfos.forEach((position, info) -> {
				NBTTagCompound tankTag = new NBTTagCompound();
				tankTag.setString(TANK_POSITION_TAG, position.getSerializedName());
				tankTag.setTag(TANK_INFO_TAG, info.serialize());
				tanks.appendTag(tankTag);
			});
			tag.setTag(TANKS_TAG, tanks);
		}
		if (batteryRenderInfo != null) {
			tag.setFloat(BATTERY_TAG, batteryRenderInfo.getChargeRatio());
		}
		if (!upgradeRenderData.isEmpty()) {
			NBTTagCompound upgrades = new NBTTagCompound();
			upgradeRenderData.forEach(upgrades::setTag);
			tag.setTag(UPGRADES_TAG, upgrades);
		}
		if (!upgradeItems.isEmpty()) {
			NBTTagList upgradeItemsTag = new NBTTagList();
			for (ItemStack upgradeItem : upgradeItems) {
				upgradeItemsTag.appendTag(upgradeItem.isEmpty() ? new NBTTagCompound() : upgradeItem.writeToNBT(new NBTTagCompound()));
			}
			tag.setTag(UPGRADE_ITEMS_TAG, upgradeItemsTag);
		}
		return tag;
	}

	public static RenderInfo deserialize(NBTTagCompound tag) {
		if (tag.isEmpty()) {
			return EMPTY;
		}
		ItemDisplayRenderInfo itemDisplay = tag.hasKey(ITEM_DISPLAY_TAG) ? ItemDisplayRenderInfo.deserialize(tag.getCompoundTag(ITEM_DISPLAY_TAG)) : ItemDisplayRenderInfo.EMPTY;
		Map<TankPosition, TankRenderInfo> tanks = new EnumMap<>(TankPosition.class);
		NBTTagList tanksTag = tag.getTagList(TANKS_TAG, Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < tanksTag.tagCount(); i++) {
			NBTTagCompound tankTag = tanksTag.getCompoundTagAt(i);
			tanks.put(TankPosition.fromName(tankTag.getString(TANK_POSITION_TAG)), TankRenderInfo.deserialize(tankTag.getCompoundTag(TANK_INFO_TAG)));
		}
		BatteryRenderInfo battery = tag.hasKey(BATTERY_TAG) ? new BatteryRenderInfo(tag.getFloat(BATTERY_TAG)) : null;
		Map<String, NBTTagCompound> upgrades = new HashMap<>();
		NBTTagCompound upgradesTag = tag.getCompoundTag(UPGRADES_TAG);
		for (String type : upgradesTag.getKeySet()) {
			upgrades.put(type, upgradesTag.getCompoundTag(type));
		}
		List<ItemStack> upgradeItems = new ArrayList<>();
		NBTTagList upgradeItemsTag = tag.getTagList(UPGRADE_ITEMS_TAG, Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < upgradeItemsTag.tagCount(); i++) {
			NBTTagCompound upgradeItemTag = upgradeItemsTag.getCompoundTagAt(i);
			upgradeItems.add(upgradeItemTag.isEmpty() ? ItemStack.EMPTY : new ItemStack(upgradeItemTag));
		}
		return new RenderInfo(itemDisplay, tanks, battery, upgrades, upgradeItems);
	}

	/**
	 * The items the storage shows, and which of the slots it shows are inaccessible. The storages that show how full
	 * their slots are, as limited barrels, have every slot's count and fill ratio too, and which slots are infinite.
	 */
	public static final class ItemDisplayRenderInfo {
		private static final String ITEMS_TAG = "items";
		private static final String INACCESSIBLE_SLOTS_TAG = "inaccessibleSlots";
		private static final String INFINITE_SLOTS_TAG = "infiniteSlots";
		private static final String SLOT_COUNTS_TAG = "slotCounts";
		private static final String SLOT_FILL_RATIOS_TAG = "slotFillRatios";
		public static final ItemDisplayRenderInfo EMPTY = new ItemDisplayRenderInfo(Collections.emptyList());
		private final List<DisplayItem> displayItems;
		private final List<Integer> inaccessibleSlots;
		private final List<Integer> infiniteSlots;
		private final List<Integer> slotCounts;
		private final List<Float> slotFillRatios;

		public ItemDisplayRenderInfo(List<DisplayItem> displayItems) {
			this(displayItems, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		}

		public ItemDisplayRenderInfo(List<DisplayItem> displayItems, List<Integer> inaccessibleSlots, List<Integer> infiniteSlots, List<Integer> slotCounts,
				List<Float> slotFillRatios) {
			this.displayItems = displayItems;
			this.inaccessibleSlots = inaccessibleSlots;
			this.infiniteSlots = infiniteSlots;
			this.slotCounts = slotCounts;
			this.slotFillRatios = slotFillRatios;
		}

		private boolean isEmpty() {
			return displayItems.isEmpty() && inaccessibleSlots.isEmpty() && slotCounts.isEmpty();
		}

		/** One item is kept flat, as 1.21 does, more in a list. */
		private NBTTagCompound serialize() {
			NBTTagCompound ret = new NBTTagCompound();
			if (displayItems.size() == 1) {
				displayItems.get(0).serialize(ret);
			} else if (displayItems.size() > 1) {
				NBTTagList items = new NBTTagList();
				displayItems.forEach(displayItem -> items.appendTag(displayItem.serialize(new NBTTagCompound())));
				ret.setTag(ITEMS_TAG, items);
			}
			putInts(ret, INACCESSIBLE_SLOTS_TAG, inaccessibleSlots);
			putInts(ret, INFINITE_SLOTS_TAG, infiniteSlots);
			putInts(ret, SLOT_COUNTS_TAG, slotCounts);
			if (!slotFillRatios.isEmpty()) {
				NBTTagList fillRatios = new NBTTagList();
				slotFillRatios.forEach(ratio -> fillRatios.appendTag(new NBTTagFloat(ratio)));
				ret.setTag(SLOT_FILL_RATIOS_TAG, fillRatios);
			}
			return ret;
		}

		private static void putInts(NBTTagCompound tag, String key, List<Integer> values) {
			if (!values.isEmpty()) {
				tag.setIntArray(key, values.stream().mapToInt(Integer::intValue).toArray());
			}
		}

		private static List<Integer> getInts(NBTTagCompound tag, String key) {
			List<Integer> ret = new ArrayList<>();
			for (int value : tag.getIntArray(key)) {
				ret.add(value);
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
			List<Float> slotFillRatios = new ArrayList<>();
			NBTTagList fillRatiosTag = tag.getTagList(SLOT_FILL_RATIOS_TAG, Constants.NBT.TAG_FLOAT);
			for (int i = 0; i < fillRatiosTag.tagCount(); i++) {
				slotFillRatios.add(fillRatiosTag.getFloatAt(i));
			}
			return new ItemDisplayRenderInfo(items, getInts(tag, INACCESSIBLE_SLOTS_TAG), getInts(tag, INFINITE_SLOTS_TAG), getInts(tag, SLOT_COUNTS_TAG), slotFillRatios);
		}

		public List<Integer> getInaccessibleSlots() {
			return inaccessibleSlots;
		}

		public List<Integer> getInfiniteSlots() {
			return infiniteSlots;
		}

		/** Every slot's count, in slot order; empty for a storage that doesn't show them. */
		public List<Integer> getSlotCounts() {
			return slotCounts;
		}

		/** How full each slot is, of what it can hold, in slot order; empty for a storage that doesn't show it. */
		public List<Float> getSlotFillRatios() {
			return slotFillRatios;
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
