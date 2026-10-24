package dev.feldsher.deconstructedcore.inventory;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * A storage's inventory. Its contents are kept in the storage's contents NBT under {@link #INVENTORY_TAG},
 * and saved through the save handler whenever a slot changes.
 * <p>
 * The inventory partitioner, the slot tracker, the memory filter and the upgrades' insert/overflow hooks
 * come with the stages that port them.
 */
public abstract class InventoryHandler extends ItemStackHandler {
	public static final String INVENTORY_TAG = "inventory";
	protected final IStorageWrapper storageWrapper;
	private final NBTTagCompound contentsNbt;
	private final Runnable saveHandler;
	private final List<IntConsumer> onContentsChangedListeners = new ArrayList<>();
	private boolean persistent = true;
	private final Map<Integer, NBTBase> stackNbts = new LinkedHashMap<>();

	private int baseSlotLimit;

	protected InventoryHandler(int numberOfInventorySlots, IStorageWrapper storageWrapper, NBTTagCompound contentsNbt, Runnable saveHandler, int baseSlotLimit) {
		super(numberOfInventorySlots);
		this.storageWrapper = storageWrapper;
		this.contentsNbt = contentsNbt;
		this.saveHandler = saveHandler;
		this.baseSlotLimit = baseSlotLimit;
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

	@Override
	protected void onContentsChanged(int slot) {
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
	}

	@Override
	public int getSlotLimit(int slot) {
		return baseSlotLimit;
	}

	public int getBaseStackLimit(ItemStack stack) {
		int maxStackSize = stack.isEmpty() ? getBaseSlotLimit() : stack.getMaxStackSize();
		if (baseSlotLimit < 64) {
			return (int) Math.max(1, (double) maxStackSize * baseSlotLimit / 64);
		}
		return maxStackSize * (baseSlotLimit / 64) + (baseSlotLimit % 64) * maxStackSize / 64;
	}

	@Override
	protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
		return getBaseStackLimit(stack);
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
		return super.isItemValid(slot, stack) && isAllowed(stack);
	}

	protected abstract boolean isAllowed(ItemStack stack);

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	public void saveInventory() {
		contentsNbt.setTag(INVENTORY_TAG, serializeNBT());
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
