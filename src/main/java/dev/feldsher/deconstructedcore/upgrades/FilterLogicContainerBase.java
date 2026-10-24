package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.common.gui.IFilterSlot;
import dev.feldsher.deconstructedcore.common.gui.IServerUpdater;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The container side of a filter's controls: its filter slots, the tag (ore dictionary name) picking, and the
 * settings its buttons change.
 */
public class FilterLogicContainerBase<T extends FilterLogic, S extends Slot> {
	private static final String DATA_IS_ALLOW_LIST = "isAllowList";
	private static final String DATA_MATCH_DURABILITY = "matchDurability";
	private static final String DATA_MATCH_NBT = "matchNbt";
	private static final String DATA_PRIMARY_MATCH = "primaryMatch";
	private static final String DATA_ADD_TAG_NAME = "addTagName";
	private static final String DATA_REMOVE_TAG_NAME = "removeTagName";
	private static final String DATA_MATCH_ANY_TAG = "matchAnyTag";
	private static final String DATA_ATTRIBUTES_KEY = "parentTagKey";

	protected final List<S> filterSlots = new ArrayList<>();
	protected final IServerUpdater serverUpdater;
	protected final Supplier<T> filterLogic;

	private final TagSelectionSlot tagSelectionSlot;
	private int selectedTagToAdd = 0;
	private int selectedTagToRemove = 0;
	private final Set<String> tagsToAdd = new TreeSet<>();

	public FilterLogicContainerBase(IServerUpdater serverUpdater, Supplier<T> filterLogic, Consumer<Slot> addSlot) {
		this.serverUpdater = serverUpdater;
		this.filterLogic = filterLogic;

		tagSelectionSlot = new TagSelectionSlot();
		addSlot.accept(tagSelectionSlot);
	}

	public int getSelectedTagToAdd() {
		return selectedTagToAdd;
	}

	public int getSelectedTagToRemove() {
		return selectedTagToRemove;
	}

	public TagSelectionSlot getTagSelectionSlot() {
		return tagSelectionSlot;
	}

	public List<S> getFilterSlots() {
		return filterSlots;
	}

	public Set<String> getTagNames() {
		return filterLogic.get().getTagKeys();
	}

	public Set<String> getTagsToAdd() {
		return tagsToAdd;
	}

	public void addSelectedTag() {
		getTagAtIndex(tagsToAdd, selectedTagToAdd).ifPresent(tagName -> {
			filterLogic.get().addTag(tagName);
			sendDataToServer(() -> {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setString(DATA_ADD_TAG_NAME, tagName);
				return tag;
			});
			selectedTagToRemove = 0;
			tagsToAdd.remove(tagName);
			selectedTagToAdd = Math.max(0, selectedTagToAdd - 1);
		});
	}

	public void removeSelectedTag() {
		getTagAtIndex(getTagNames(), selectedTagToRemove).ifPresent(tagName -> {
			filterLogic.get().removeTagName(tagName);
			sendDataToServer(() -> {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setString(DATA_REMOVE_TAG_NAME, tagName);
				return tag;
			});
			if (FilterLogic.getStackTags(tagSelectionSlot.getStack()).contains(tagName)) {
				tagsToAdd.add(tagName);
			}
			selectedTagToRemove = Math.max(0, selectedTagToRemove - 1);
		});
	}

	public void selectNextTagToRemove() {
		selectedTagToRemove = getNextIndex(getTagNames().size(), selectedTagToRemove);
	}

	public void selectPreviousTagToRemove() {
		selectedTagToRemove = getPreviousIndex(getTagNames().size(), selectedTagToRemove);
	}

	public void selectNextTagToAdd() {
		selectedTagToAdd = getNextIndex(tagsToAdd.size(), selectedTagToAdd);
	}

	public void selectPreviousTagToAdd() {
		selectedTagToAdd = getPreviousIndex(tagsToAdd.size(), selectedTagToAdd);
	}

	private static int getNextIndex(int colSize, int selectedIndex) {
		return selectedIndex + 1 >= colSize ? 0 : selectedIndex + 1;
	}

	private static int getPreviousIndex(int colSize, int selectedIndex) {
		return selectedIndex == 0 ? Math.max(0, colSize - 1) : selectedIndex - 1;
	}

	private static Optional<String> getTagAtIndex(Set<String> col, int index) {
		int curIndex = 0;
		for (String tagName : col) {
			if (curIndex == index) {
				return Optional.of(tagName);
			}
			curIndex++;
		}
		return Optional.empty();
	}

	public boolean isAllowList() {
		return filterLogic.get().isAllowList();
	}

	public boolean shouldMatchDurability() {
		return filterLogic.get().shouldMatchDurability();
	}

	public boolean shouldMatchNbt() {
		return filterLogic.get().shouldMatchComponents();
	}

	public PrimaryMatch getPrimaryMatch() {
		return filterLogic.get().getPrimaryMatch();
	}

	public boolean shouldMatchAnyTag() {
		return filterLogic.get().shouldMatchAnyTag();
	}

	public void setAllowList(boolean isAllowList) {
		filterLogic.get().setAllowList(isAllowList);
		sendBooleanToServer(DATA_IS_ALLOW_LIST, isAllowList);
	}

	public void setMatchDurability(boolean matchDurability) {
		filterLogic.get().setMatchDurability(matchDurability);
		sendBooleanToServer(DATA_MATCH_DURABILITY, matchDurability);
	}

	public void setMatchNbt(boolean matchNbt) {
		filterLogic.get().setMatchComponents(matchNbt);
		sendBooleanToServer(DATA_MATCH_NBT, matchNbt);
	}

	public void setPrimaryMatch(PrimaryMatch primaryMatch) {
		filterLogic.get().setPrimaryMatch(primaryMatch);
		sendDataToServer(() -> {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString(DATA_PRIMARY_MATCH, primaryMatch.getSerializedName());
			return tag;
		});
	}

	public void setMatchAnyTag(boolean matchAnyTag) {
		filterLogic.get().setMatchAnyTag(matchAnyTag);
		sendBooleanToServer(DATA_MATCH_ANY_TAG, matchAnyTag);
	}

	private void sendBooleanToServer(String dataId, boolean value) {
		sendDataToServer(() -> {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setBoolean(dataId, value);
			return tag;
		});
	}

	/** Every message says which filter of the upgrade it is for: some upgrades have more than one. */
	protected void sendDataToServer(Supplier<NBTTagCompound> dataSupplier) {
		serverUpdater.sendDataToServer(() -> {
			NBTTagCompound tag = dataSupplier.get();
			tag.setString(DATA_ATTRIBUTES_KEY, filterLogic.get().attributesKey);
			return tag;
		});
	}

	public boolean handlePacket(NBTTagCompound data) {
		if (isDifferentFilterLogicsData(data)) {
			return false;
		}

		for (String key : data.getKeySet()) {
			switch (key) {
				case DATA_IS_ALLOW_LIST:
					setAllowList(data.getBoolean(DATA_IS_ALLOW_LIST));
					return true;
				case DATA_MATCH_DURABILITY:
					setMatchDurability(data.getBoolean(DATA_MATCH_DURABILITY));
					return true;
				case DATA_MATCH_NBT:
					setMatchNbt(data.getBoolean(DATA_MATCH_NBT));
					return true;
				case DATA_PRIMARY_MATCH:
					setPrimaryMatch(PrimaryMatch.fromName(data.getString(DATA_PRIMARY_MATCH)));
					return true;
				case DATA_ADD_TAG_NAME:
					filterLogic.get().addTag(data.getString(DATA_ADD_TAG_NAME));
					return true;
				case DATA_REMOVE_TAG_NAME:
					filterLogic.get().removeTagName(data.getString(DATA_REMOVE_TAG_NAME));
					return true;
				case DATA_MATCH_ANY_TAG:
					setMatchAnyTag(data.getBoolean(DATA_MATCH_ANY_TAG));
					return true;
				default:
					break;
			}
		}
		return false;
	}

	protected boolean isDifferentFilterLogicsData(NBTTagCompound data) {
		return data.hasKey(DATA_ATTRIBUTES_KEY) && !filterLogic.get().attributesKey.equals(data.getString(DATA_ATTRIBUTES_KEY));
	}

	/** The ghost slot an item is put into to pick its tags from. */
	public class TagSelectionSlot extends Slot implements IFilterSlot {
		private ItemStack stack = ItemStack.EMPTY;
		private Runnable onUpdate = () -> {
		};

		public TagSelectionSlot() {
			super(new InventoryBasic("", false, 1), 0, -1, -1);
		}

		public void setOnUpdate(Runnable onUpdate) {
			this.onUpdate = onUpdate;
		}

		@Override
		public boolean isItemValid(@Nonnull ItemStack stack) {
			return stack.isEmpty() || !FilterLogic.getStackTags(stack).isEmpty();
		}

		@Override
		public boolean canTakeStack(EntityPlayer player) {
			return false;
		}

		@Nonnull
		@Override
		public ItemStack getStack() {
			return stack;
		}

		@Override
		public int getSlotStackLimit() {
			return 1;
		}

		@Nonnull
		@Override
		public ItemStack decrStackSize(int amount) {
			stack = ItemStack.EMPTY;
			return stack;
		}

		@Override
		public boolean isSameInventory(Slot other) {
			return false;
		}

		@Override
		public void putStack(@Nonnull ItemStack stack) {
			this.stack = stack;
			tagsToAdd.clear();
			tagsToAdd.addAll(FilterLogic.getStackTags(stack));
			getTagNames().forEach(tagsToAdd::remove);
			selectedTagToAdd = 0;
			onUpdate.run();
		}

		@Override
		public void onSlotChanged() {
			//noop
		}
	}
}
