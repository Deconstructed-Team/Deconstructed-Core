package dev.feldsher.deconstructedcore.inventory;

import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.upgrades.IInsertResponseUpgrade;
import dev.feldsher.deconstructedcore.upgrades.IOverflowResponseUpgrade;
import dev.feldsher.deconstructedcore.upgrades.ISlotLimitUpgrade;
import dev.feldsher.deconstructedcore.upgrades.stack.StackUpgradeConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * A storage's inventory. Its contents are kept in the storage's contents NBT under {@link #INVENTORY_TAG},
 * and saved through the save handler whenever a slot changes.
 * <p>
 * Memorized slots take only what they remember, and are filled with it before empty slots that remember nothing.
 * Runs of slots can be handled by parts other than the plain one, as the infinity upgrade's, through its
 * {@link InventoryPartitioner}, kept under {@link #PARTITIONER_TAG}. The slot tracker comes with the stage that ports it.
 */
public abstract class InventoryHandler extends ItemStackHandler {
	public static final String INVENTORY_TAG = "inventory";
	private static final String PARTITIONER_TAG = "partitioner";
	protected final IStorageWrapper storageWrapper;
	private final NBTTagCompound contentsNbt;
	private final Runnable saveHandler;
	private final List<IntConsumer> onContentsChangedListeners = new ArrayList<>();
	private boolean persistent = true;
	private final Map<Integer, NBTBase> stackNbts = new LinkedHashMap<>();

	private int baseSlotLimit;
	private final StackUpgradeConfig stackUpgradeConfig;
	private final InventoryPartitioner inventoryPartitioner;

	protected InventoryHandler(int numberOfInventorySlots, IStorageWrapper storageWrapper, NBTTagCompound contentsNbt, Runnable saveHandler, int baseSlotLimit,
			StackUpgradeConfig stackUpgradeConfig) {
		super(numberOfInventorySlots);
		this.stackUpgradeConfig = stackUpgradeConfig;
		this.storageWrapper = storageWrapper;
		this.contentsNbt = contentsNbt;
		this.saveHandler = saveHandler;
		this.baseSlotLimit = baseSlotLimit;
		inventoryPartitioner = new InventoryPartitioner(contentsNbt.getCompoundTag(PARTITIONER_TAG), this, this::getMemorySettings);
		deserializeNBT(contentsNbt.getCompoundTag(INVENTORY_TAG));
		initStackNbts();
	}

	@Override
	public void setSize(int size) {
		super.setSize(stacks.size());
	}

	private void initStackNbts() {
		stackNbts.clear();
		for (int slot = 0; slot < stacks.size(); slot++) {
			ItemStack slotStack = stacks.get(slot);
			if (!slotStack.isEmpty()) {
				stackNbts.put(slot, getSlotsStackNbt(slot, slotStack));
			}
		}
	}

	/** Public: slots tell the handler when vanilla changed a stack in place, which the handler can't see. */
	@Override
	public void onContentsChanged(int slot) {
		super.onContentsChanged(slot);
		if (persistent && updateSlotNbt(slot)) {
			saveInventory();
			triggerOnChangeListeners(slot);
		}
	}

	public void triggerOnChangeListeners(int slot) {
		for (IntConsumer onContentsChangedListener : onContentsChangedListeners) {
			onContentsChangedListener.accept(slot);
		}
	}

	private boolean updateSlotNbt(int slot) {
		ItemStack slotStack = getSlotStack(slot);
		if (slotStack.isEmpty()) {
			if (stackNbts.containsKey(slot)) {
				stackNbts.remove(slot);
				return true;
			}
		} else {
			NBTBase itemTag = getSlotsStackNbt(slot, slotStack);
			if (!stackNbts.containsKey(slot) || !stackNbts.get(slot).equals(itemTag)) {
				stackNbts.put(slot, itemTag);
				return true;
			}
		}
		return false;
	}

	private NBTBase getSlotsStackNbt(int slot, ItemStack slotStack) {
		NBTTagCompound itemTag = new NBTTagCompound();
		itemTag.setInteger("Slot", slot);
		slotStack.writeToNBT(itemTag);
		// ItemStack keeps its count in a byte; stacks above 127 would wrap
		itemTag.setInteger("Count", slotStack.getCount());
		return itemTag;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		setSize(nbt.hasKey("Size", Constants.NBT.TAG_INT) ? nbt.getInteger("Size") : stacks.size());
		NBTTagList tagList = nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < tagList.tagCount(); i++) {
			NBTTagCompound itemTag = tagList.getCompoundTagAt(i);
			int slot = itemTag.getInteger("Slot");
			if (slot >= 0 && slot < stacks.size()) {
				ItemStack stack = new ItemStack(itemTag);
				stack.setCount(itemTag.getInteger("Count"));
				stacks.set(slot, stack);
			}
		}
		onLoad();
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagList nbtTagList = new NBTTagList();
		stackNbts.values().forEach(nbtTagList::appendTag);
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setTag("Items", nbtTagList);
		nbt.setInteger("Size", getSlots());
		return nbt;
	}

	public int getBaseSlotLimit() {
		return baseSlotLimit;
	}

	public void setBaseSlotLimit(int baseSlotLimit) {
		this.baseSlotLimit = baseSlotLimit;
		if (inventoryPartitioner != null) {
			inventoryPartitioner.onSlotLimitChange();
		}
	}

	private IInventoryPartHandler getPart(int slot) {
		return inventoryPartitioner.getPartBySlot(slot);
	}

	public InventoryPartitioner getInventoryPartitioner() {
		return inventoryPartitioner;
	}

	@Override
	public int getSlotLimit(int slot) {
		int slotLimit = baseSlotLimit;
		for (ISlotLimitUpgrade slotLimitUpgrade : storageWrapper.getUpgradeHandler().getWrappersThatImplement(ISlotLimitUpgrade.class)) {
			slotLimit = Math.max(slotLimit, slotLimitUpgrade.getSlotLimit());
		}
		return slotLimit > baseSlotLimit ? slotLimit : getPart(slot).getSlotLimit(slot);
	}

	/** Into this slot, the upgrades' insert responses around it; what doesn't fit next to the same item goes to the overflow upgrades. */
	@Nonnull
	@Override
	public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
		ItemStack remaining = insertItemInternal(slot, stack, simulate);
		if (!remaining.isEmpty() && ItemHandlerHelper.canItemStacksStack(getStackInSlot(slot), remaining)) {
			remaining = triggerOverflowUpgrades(remaining);
		}
		return remaining;
	}

	/**
	 * Into the whole inventory, as 1.21's slot tracker does: next to the same item first, then into empty slots that
	 * remember it, then into other empty slots; what the slots with that item can't take goes to the overflow upgrades.
	 */
	public ItemStack insertItem(ItemStack stack, boolean simulate) {
		if (stack.isEmpty()) {
			return stack;
		}
		boolean hasFullStack = false;
		ItemStack remaining = stack;
		for (int slot = 0; slot < getSlots() && !remaining.isEmpty(); slot++) {
			ItemStack slotStack = getStackInSlot(slot);
			if (!slotStack.isEmpty() && ItemHandlerHelper.canItemStacksStack(slotStack, remaining)) {
				if (slotStack.getCount() >= getStackLimit(slot, slotStack)) {
					hasFullStack = true;
				} else {
					remaining = insertItemInternal(slot, remaining, simulate);
				}
			}
		}
		MemorySettingsCategory memory = getMemorySettings();
		for (int slot = 0; slot < getSlots() && !remaining.isEmpty(); slot++) {
			if (getStackInSlot(slot).isEmpty() && memory.isSlotSelected(slot) && memory.matchesFilter(slot, remaining)) {
				remaining = insertItemInternal(slot, remaining, simulate);
			}
		}
		for (int slot = 0; slot < getSlots() && !remaining.isEmpty(); slot++) {
			if (getStackInSlot(slot).isEmpty() && !memory.isSlotSelected(slot)) {
				remaining = insertItemInternal(slot, remaining, simulate);
			}
		}
		if (!remaining.isEmpty() && (hasFullStack || remaining.getCount() < stack.getCount())) {
			remaining = triggerOverflowUpgrades(remaining);
		}
		return remaining;
	}

	private ItemStack insertItemInternal(int slot, ItemStack stack, boolean simulate) {
		ItemStack remaining = stack;
		for (IInsertResponseUpgrade upgrade : storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class)) {
			remaining = upgrade.onBeforeInsert(this, slot, remaining, simulate);
			if (remaining.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}

		// 1.12's ItemStackHandler inserts whatever it's given; 1.21's checks isItemValid first, which the storages rely on
		if (!isItemValid(slot, remaining)) {
			return remaining;
		}
		ItemStack ret = getPart(slot).insertItem(slot, remaining, simulate, (s, st, sim) -> super.insertItem(s, st, sim));
		if (ret != remaining && !simulate) {
			storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class).forEach(u -> u.onAfterInsert(this, slot));
		}
		return ret;
	}

	private ItemStack triggerOverflowUpgrades(ItemStack stack) {
		ItemStack ret = stack;
		for (IOverflowResponseUpgrade overflowUpgrade : storageWrapper.getUpgradeHandler().getWrappersThatImplement(IOverflowResponseUpgrade.class)) {
			ret = overflowUpgrade.onOverflow(ret);
			if (ret.isEmpty()) {
				break;
			}
		}
		return ret;
	}

	public int getBaseStackLimit(ItemStack stack) {
		if (!stackUpgradeConfig.canStackItem(stack.getItem())) {
			return stack.getMaxStackSize();
		}
		int maxStackSize = stack.isEmpty() ? getBaseSlotLimit() : stack.getMaxStackSize();
		if (baseSlotLimit < 64) {
			return (int) Math.max(1, (double) maxStackSize * baseSlotLimit / 64);
		}
		long limit = (long) maxStackSize * (baseSlotLimit / 64) + (long) (baseSlotLimit % 64) * maxStackSize / 64;
		return (int) Math.min(Integer.MAX_VALUE, limit);
	}

	@Override
	public int getStackLimit(int slot, @Nonnull ItemStack stack) {
		return getPart(slot).getStackLimit(slot, stack);
	}

	@Nonnull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return getPart(slot).extractItem(slot, amount, simulate);
	}

	/** Extraction as a plain inventory does it, for the parts to use. */
	public ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
		return super.extractItem(slot, amount, simulate);
	}

	@Override
	public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
		getPart(slot).setStackInSlot(slot, stack, (s, st) -> super.setStackInSlot(s, st));
	}

	@Nonnull
	@Override
	public ItemStack getStackInSlot(int slot) {
		return getPart(slot).getStackInSlot(slot, s -> super.getStackInSlot(s));
	}

	public boolean isSlotAccessible(int slot) {
		return getPart(slot).isSlotAccessible(slot);
	}

	public boolean isInfinite(int slot) {
		return inventoryPartitioner.isInfinite(slot);
	}

	public ItemStack getSlotStack(int slot) {
		return stacks.get(slot);
	}

	public void setSlotStack(int slot, ItemStack stack) {
		stacks.set(slot, stack);
		onContentsChanged(slot);
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		return isItemValid(slot, stack, null);
	}

	/** As the slot's part allows, for the player who puts it in, if a player does. */
	public boolean isItemValid(int slot, ItemStack stack, @Nullable EntityPlayer player) {
		return getPart(slot).isItemValid(slot, stack, player, (s, st) -> super.isItemValid(s, st))
				&& isAllowed(stack) && getMemorySettings().matchesFilter(slot, stack);
	}

	private MemorySettingsCategory getMemorySettings() {
		return storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
	}

	protected abstract boolean isAllowed(ItemStack stack);

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	public void saveInventory() {
		contentsNbt.setTag(INVENTORY_TAG, serializeNBT());
		if (inventoryPartitioner != null) {
			contentsNbt.setTag(PARTITIONER_TAG, inventoryPartitioner.serializeNBT());
		}
		saveHandler.run();
	}

	public void copyStacksTo(InventoryHandler otherHandler) {
		for (int slot = 0; slot < getSlots() && slot < otherHandler.getSlots(); slot++) {
			otherHandler.setStackInSlot(slot, getStackInSlot(slot).copy());
		}
	}

	public void addListener(IntConsumer onContentsChanged) {
		onContentsChangedListeners.add(onContentsChanged);
	}

	public void clearListeners() {
		onContentsChangedListeners.clear();
	}

	public void changeSlots(int diff) {
		NonNullList<ItemStack> previousStacks = stacks;
		stacks = NonNullList.withSize(previousStacks.size() + diff, ItemStack.EMPTY);
		for (int slot = 0; slot < previousStacks.size() && slot < stacks.size(); slot++) {
			stacks.set(slot, previousStacks.get(slot));
		}
		initStackNbts();
		saveInventory();
	}
}
