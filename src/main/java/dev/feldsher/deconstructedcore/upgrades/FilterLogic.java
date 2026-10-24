package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.util.InventoryHelper;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * An upgrade's filter: allow or block list, matched by item, mod or tags, and its filter slots.
 * It keeps its attributes in the upgrade's NBT under its own key, as 1.21 keeps them in a data component of their own.
 * Tags are 1.12's ore dictionary names. An item with subtypes always matches on its metadata, which is what tells
 * those subtypes apart in 1.12; durability matching is about damageable items.
 */
public class FilterLogic {
	private static final String TAG_KEYS = "tag_keys";
	private static final String IS_ALLOW_LIST = "is_allow_list";
	private static final String MATCH_DURABILITY = "match_durability";
	private static final String MATCH_COMPONENTS = "match_components";
	private static final String PRIMARY_MATCH = "primary_match";
	private static final String MATCH_ANY_TAG = "match_any_tag";
	private static final String FILTER_ITEMS = "filter_items";

	protected final ItemStack upgrade;
	protected final Consumer<ItemStack> saveHandler;
	protected final String attributesKey;
	private final int defaultFilterSlotCount;
	private final Predicate<ItemStack> isItemValid;
	@Nullable
	protected Set<String> tagKeys = null;
	@Nullable
	private ObservableFilterItemStackHandler filterHandler = null;
	private boolean emptyAllowListMatchesEverything = false;
	private boolean allowListDefault = false;

	public FilterLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, int defaultFilterSlotCount, String attributesKey) {
		this(upgrade, saveHandler, defaultFilterSlotCount, s -> true, attributesKey);
	}

	public FilterLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, int defaultFilterSlotCount, Predicate<ItemStack> isItemValid, String attributesKey) {
		this.upgrade = upgrade;
		this.saveHandler = saveHandler;
		this.attributesKey = attributesKey;
		this.defaultFilterSlotCount = defaultFilterSlotCount;
		this.isItemValid = isItemValid;
	}

	public void setEmptyAllowListMatchesEverything() {
		emptyAllowListMatchesEverything = true;
	}

	public void setAllowByDefault(boolean allowListDefault) {
		this.allowListDefault = allowListDefault;
	}

	@Nullable
	private NBTTagCompound getAttributes() {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag != null && tag.hasKey(attributesKey, Constants.NBT.TAG_COMPOUND) ? tag.getCompoundTag(attributesKey) : null;
	}

	protected NBTTagCompound getOrCreateAttributes() {
		NBTTagCompound tag = StorageNbt.getOrCreateTag(upgrade);
		if (!tag.hasKey(attributesKey, Constants.NBT.TAG_COMPOUND)) {
			tag.setTag(attributesKey, new NBTTagCompound());
		}
		return tag.getCompoundTag(attributesKey);
	}

	protected boolean getBoolean(String key, boolean defaultValue) {
		NBTTagCompound attributes = getAttributes();
		return attributes != null && attributes.hasKey(key) ? attributes.getBoolean(key) : defaultValue;
	}

	protected void setBoolean(String key, boolean value) {
		getOrCreateAttributes().setBoolean(key, value);
		save();
	}

	public ObservableFilterItemStackHandler getFilterHandler() {
		if (filterHandler == null) {
			NBTTagCompound attributes = getAttributes();
			NBTTagList items = attributes != null ? attributes.getTagList(FILTER_ITEMS, Constants.NBT.TAG_COMPOUND) : new NBTTagList();
			int filterSlotCount = Math.max(items.tagCount(), defaultFilterSlotCount);
			filterHandler = new ObservableFilterItemStackHandler(filterSlotCount);
			for (int slot = 0; slot < items.tagCount() && slot < filterSlotCount; slot++) {
				filterHandler.setStackInSlotQuietly(slot, new ItemStack(items.getCompoundTagAt(slot)));
			}
		}
		return filterHandler;
	}

	public boolean matchesFilter(ItemStack stack) {
		if (isAllowList()) {
			if (getPrimaryMatch() == PrimaryMatch.TAGS) {
				return isTagMatch(stack);
			}
			return (getFilterHandler().hasOnlyEmptyFilters() && emptyAllowListMatchesEverything)
					|| InventoryHelper.hasItem(getFilterHandler(), filter -> stackMatchesFilter(stack, filter));
		} else {
			if (getPrimaryMatch() == PrimaryMatch.TAGS) {
				return !isTagMatch(stack);
			}
			return getFilterHandler().hasOnlyEmptyFilters() || !InventoryHelper.hasItem(getFilterHandler(), filter -> stackMatchesFilter(stack, filter));
		}
	}

	private boolean isTagMatch(ItemStack stack) {
		Set<String> stackTags = getStackTags(stack);
		Set<String> keys = getTagKeys();
		if (shouldMatchAnyTag()) {
			return stackTags.stream().anyMatch(keys::contains);
		}
		return !keys.isEmpty() && stackTags.containsAll(keys);
	}

	public static Set<String> getStackTags(ItemStack stack) {
		Set<String> tags = new TreeSet<>();
		if (stack.isEmpty()) {
			return tags;
		}
		for (int id : OreDictionary.getOreIDs(stack)) {
			tags.add(OreDictionary.getOreName(id));
		}
		return tags;
	}

	protected void save() {
		saveHandler.accept(upgrade);
	}

	public boolean stackMatchesFilter(ItemStack stack, ItemStack filter) {
		if (filter.isEmpty()) {
			return false;
		}

		PrimaryMatch primaryMatch = getPrimaryMatch();
		if (primaryMatch == PrimaryMatch.MOD) {
			if (!Objects.equals(Objects.requireNonNull(stack.getItem().getRegistryName()).getNamespace(), Objects.requireNonNull(filter.getItem().getRegistryName()).getNamespace())) {
				return false;
			}
		} else if (primaryMatch == PrimaryMatch.ITEM) {
			if (stack.getItem() != filter.getItem() || (stack.getHasSubtypes() && stack.getMetadata() != filter.getMetadata())) {
				return false;
			}
		}

		if (shouldMatchDurability() && stack.isItemStackDamageable() && stack.getItemDamage() != filter.getItemDamage()) {
			return false;
		}

		return !shouldMatchComponents() || ItemStack.areItemStackTagsEqual(stack, filter);
	}

	public Set<String> getTagKeys() {
		if (tagKeys == null) {
			initTags();
		}
		return Collections.unmodifiableSet(tagKeys);
	}

	public void addTag(String tagName) {
		if (tagKeys == null) {
			initTags();
		}
		tagKeys.add(tagName);
		serializeTags();
		save();
	}

	public void removeTagName(String tagName) {
		if (tagKeys == null) {
			initTags();
		}
		tagKeys.remove(tagName);
		serializeTags();
		save();
	}

	private void serializeTags() {
		if (tagKeys == null) {
			return;
		}
		NBTTagList list = new NBTTagList();
		tagKeys.forEach(tagKey -> list.appendTag(new NBTTagString(tagKey)));
		getOrCreateAttributes().setTag(TAG_KEYS, list);
	}

	protected void initTags() {
		tagKeys = new TreeSet<>();
		NBTTagCompound attributes = getAttributes();
		if (attributes != null) {
			NBTTagList list = attributes.getTagList(TAG_KEYS, Constants.NBT.TAG_STRING);
			for (int i = 0; i < list.tagCount(); i++) {
				tagKeys.add(list.getStringTagAt(i));
			}
		}
	}

	public void setAllowList(boolean isAllowList) {
		setBoolean(IS_ALLOW_LIST, isAllowList);
	}

	public boolean isAllowList() {
		return getBoolean(IS_ALLOW_LIST, allowListDefault);
	}

	public boolean shouldMatchDurability() {
		return getBoolean(MATCH_DURABILITY, false);
	}

	public void setMatchDurability(boolean matchDurability) {
		setBoolean(MATCH_DURABILITY, matchDurability);
	}

	public void setMatchComponents(boolean matchComponents) {
		setBoolean(MATCH_COMPONENTS, matchComponents);
	}

	public boolean shouldMatchComponents() {
		return getBoolean(MATCH_COMPONENTS, false);
	}

	public void setPrimaryMatch(PrimaryMatch primaryMatch) {
		getOrCreateAttributes().setString(PRIMARY_MATCH, primaryMatch.getSerializedName());
		save();
	}

	public PrimaryMatch getPrimaryMatch() {
		NBTTagCompound attributes = getAttributes();
		return attributes != null && attributes.hasKey(PRIMARY_MATCH) ? PrimaryMatch.fromName(attributes.getString(PRIMARY_MATCH)) : PrimaryMatch.ITEM;
	}

	public boolean shouldMatchAnyTag() {
		return getBoolean(MATCH_ANY_TAG, true);
	}

	public void setMatchAnyTag(boolean matchAnyTag) {
		setBoolean(MATCH_ANY_TAG, matchAnyTag);
	}

	/** The filter slots: ghost copies of one item each, saved into the upgrade as they change. */
	public class ObservableFilterItemStackHandler extends ItemStackHandler {
		private IntConsumer onSlotChange = s -> {
		};
		private boolean loading = false;

		public ObservableFilterItemStackHandler(int filterSlotCount) {
			super(filterSlotCount);
		}

		void setStackInSlotQuietly(int slot, ItemStack stack) {
			loading = true;
			setStackInSlot(slot, stack);
			loading = false;
		}

		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			if (loading) {
				return;
			}
			NBTTagList items = new NBTTagList();
			for (ItemStack stack : stacks) {
				items.appendTag(stack.writeToNBT(new NBTTagCompound()));
			}
			getOrCreateAttributes().setTag(FILTER_ITEMS, items);
			save();
			onSlotChange.accept(slot);
		}

		public void setOnSlotChange(IntConsumer onSlotChange) {
			this.onSlotChange = onSlotChange;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return stack.isEmpty() || (doesNotContain(stack) && isItemValid.test(stack));
		}

		private boolean doesNotContain(ItemStack stack) {
			return !InventoryHelper.hasItem(this, s -> ItemStack.areItemsEqual(s, stack) && ItemStack.areItemStackTagsEqual(s, stack));
		}

		public boolean hasOnlyEmptyFilters() {
			for (ItemStack stack : stacks) {
				if (!stack.isEmpty()) {
					return false;
				}
			}
			return true;
		}
	}
}
