package dev.feldsher.deconstructedcore.controller;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.inventory.IItemHandlerSimpleInserter;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.EmptyHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A controller: the storages around it, joined through each other, and those linked to it, as one inventory, their
 * slots one after another. Items put into it go where the same stack is, then where the same item is, then to storages
 * that remember the item or are set for it, and then to the nearest storage with room.
 * <p>
 * 1.21 keeps an index of what each storage holds, which the storages keep up to date; here the controller asks each
 * storage, which keeps its own, as it puts items away. Each storage then tells it only when its number of slots changes.
 */
public abstract class ControllerBlockEntityBase extends TileEntity implements IItemHandlerSimpleInserter {
	public static final int SEARCH_RANGE = 15;
	private static final String STORAGE_POSITIONS_TAG = "storagePositions";
	private static final String CONNECTING_BLOCKS_TAG = "connectingBlocks";
	private static final String NON_CONNECTING_BLOCKS_TAG = "nonConnectingBlocks";
	private static final String LINKED_BLOCKS_TAG = "linkedBlocks";
	private static final String BASE_INDEXES_TAG = "baseIndexes";
	private static final String TOTAL_SLOTS_TAG = "totalSlots";

	private List<BlockPos> storagePositions = new ArrayList<>();
	private List<Integer> baseIndexes = new ArrayList<>();
	private int totalSlots = 0;
	private final Comparator<BlockPos> distanceComparator = Comparator.<BlockPos>comparingDouble(p -> p.distanceSq(getPos())).thenComparing(Comparator.naturalOrder());
	private Set<BlockPos> linkedBlocks = new LinkedHashSet<>();
	private Set<BlockPos> connectingBlocks = new LinkedHashSet<>();
	private Set<BlockPos> nonConnectingBlocks = new LinkedHashSet<>();
	@Nullable
	private List<BlockPos> storagePositionsByDistance = null;

	/** The block entity at pos, when its chunk is loaded: a network doesn't load chunks. */
	public static <T> Optional<T> getLoadedTileEntity(@Nullable World world, BlockPos pos, Class<T> type) {
		if (world == null || !world.isBlockLoaded(pos)) {
			return Optional.empty();
		}
		TileEntity te = world.getTileEntity(pos);
		return type.isInstance(te) ? Optional.of(type.cast(te)) : Optional.empty();
	}

	public boolean addLinkedBlock(BlockPos linkedPos) {
		if (world != null && !world.isRemote && isWithinRange(linkedPos) && !linkedBlocks.contains(linkedPos) && !storagePositions.contains(linkedPos)) {
			linkedBlocks.add(linkedPos);
			markDirty();
			getLoadedTileEntity(world, linkedPos, ILinkable.class).ifPresent(linkable -> {
				if (linkable.connectLinkedSelf()) {
					Set<BlockPos> positionsToCheck = new LinkedHashSet<>();
					positionsToCheck.add(linkedPos);
					searchAndAddBoundables(positionsToCheck, true);
				}
				searchAndAddBoundables(new LinkedHashSet<>(linkable.getConnectablePositions()), false);
			});
			notifyBlockUpdate();
			return true;
		}
		return false;
	}

	public void removeLinkedBlock(BlockPos linkedPos) {
		linkedBlocks.remove(linkedPos);
		markDirty();
		verifyStoragesConnected();
		notifyBlockUpdate();
	}

	/** Loaded again, the storages it has are told whose they are. */
	@Override
	public void onLoad() {
		super.onLoad();
		if (world != null && !world.isRemote) {
			storagePositions.forEach(pos -> getLoadedTileEntity(world, pos, IControllableStorage.class).ifPresent(storage -> storage.registerController(this)));
		}
	}

	public boolean isStorageConnected(BlockPos storagePos) {
		return storagePositions.contains(storagePos);
	}

	/** Placed, it takes in what's around it. */
	public void searchAndAddBoundables() {
		Set<BlockPos> positionsToCheck = new LinkedHashSet<>();
		for (EnumFacing dir : EnumFacing.values()) {
			positionsToCheck.add(getPos().offset(dir));
		}
		searchAndAddBoundables(positionsToCheck, false);
	}

	/** A storage's inventory grew or shrank: the slots after its own move. */
	public void changeSlots(BlockPos storagePos, int newSlots) {
		int index = storagePositions.indexOf(storagePos);
		if (index < 0) {
			return;
		}
		int diff = newSlots - getStorageSlots(index);
		for (int i = index; i < baseIndexes.size(); i++) {
			baseIndexes.set(i, baseIndexes.get(i) + diff);
		}
		totalSlots += diff;
		markDirty();
		notifyBlockUpdate();
	}

	private int getStorageSlots(int index) {
		int previousBaseIndex = index == 0 ? 0 : baseIndexes.get(index - 1);
		return baseIndexes.get(index) - previousBaseIndex;
	}

	public int getSlots(int storageIndex) {
		if (storageIndex < 0 || storageIndex >= baseIndexes.size()) {
			return 0;
		}
		return getStorageSlots(storageIndex);
	}

	private void searchAndAddBoundables(Set<BlockPos> positionsToCheck, boolean addingLinkedSelf) {
		Set<BlockPos> positionsChecked = new HashSet<>();
		boolean first = true;
		while (!positionsToCheck.isEmpty()) {
			Iterator<BlockPos> it = positionsToCheck.iterator();
			BlockPos posToCheck = it.next();
			it.remove();
			Optional<IControllerBoundable> boundable = getLoadedTileEntity(world, posToCheck, IControllerBoundable.class);
			if (boundable.isPresent()) {
				tryToConnect(positionsToCheck, addingLinkedSelf, positionsChecked, posToCheck, first, boundable.get());
			} else {
				positionsChecked.add(posToCheck);
			}
			first = false;
		}
	}

	private void tryToConnect(Set<BlockPos> positionsToCheck, boolean addingLinkedSelf, Set<BlockPos> positionsChecked, BlockPos posToCheck, boolean first,
			IControllerBoundable boundable) {
		if (!boundable.canBeConnected() && !(addingLinkedSelf && first)) {
			return;
		}
		if (boundable instanceof ILinkable && ((ILinkable) boundable).isLinked() && (!addingLinkedSelf || !first)) {
			linkedBlocks.remove(posToCheck);
			((ILinkable) boundable).setNotLinked();
		} else if (boundable instanceof IControllableStorage && ((IControllableStorage) boundable).hasStorageData()) {
			addStorageData(posToCheck, (IControllableStorage) boundable);
		} else {
			if (boundable.canConnectStorages()) {
				connectingBlocks.add(posToCheck);
			} else {
				nonConnectingBlocks.add(posToCheck);
			}
			boundable.registerController(this);
			markDirty();
		}
		if (boundable.canConnectStorages()) {
			addUncheckedPositionsAround(positionsToCheck, positionsChecked, posToCheck);
		}
	}

	private void addUncheckedPositionsAround(Set<BlockPos> positionsToCheck, Set<BlockPos> positionsChecked, BlockPos currentPos) {
		for (EnumFacing dir : EnumFacing.values()) {
			BlockPos pos = currentPos.offset(dir);
			if (!positionsChecked.contains(pos) && ((!storagePositions.contains(pos) && !connectingBlocks.contains(pos) && !nonConnectingBlocks.contains(pos))
					|| linkedBlocks.contains(pos)) && isWithinRange(pos)) {
				positionsToCheck.add(pos);
			}
		}
	}

	private boolean isWithinRange(BlockPos pos) {
		return Math.abs(pos.getX() - getPos().getX()) <= SEARCH_RANGE && Math.abs(pos.getY() - getPos().getY()) <= SEARCH_RANGE
				&& Math.abs(pos.getZ() - getPos().getZ()) <= SEARCH_RANGE;
	}

	/** A storage placed next to the network, or that joined it otherwise: it, and what it connects, join. */
	public void addStorage(BlockPos storagePos) {
		if (storagePositions.contains(storagePos)) {
			removeStorageInventoryData(storagePos);
		}
		if (isWithinRange(storagePos)) {
			Set<BlockPos> positionsToCheck = new LinkedHashSet<>();
			positionsToCheck.add(storagePos);
			searchAndAddBoundables(positionsToCheck, false);
		}
	}

	private void addStorageData(BlockPos storagePos, IControllableStorage storage) {
		storagePositions.add(storagePos);
		storagePositionsByDistance = null;
		totalSlots += storage.getControllerInventory().getSlots();
		baseIndexes.add(totalSlots);
		storage.registerController(this);
		markDirty();
		notifyBlockUpdate();
	}

	public void removeStorage(BlockPos storagePos) {
		if (connectingBlocks.remove(storagePos)) {
			markDirty();
		}
		removeStorageInventoryDataAndUnregisterController(storagePos);
		verifyStoragesConnected();
	}

	public void removeNonConnectingBlock(BlockPos pos) {
		if (nonConnectingBlocks.remove(pos)) {
			getLoadedTileEntity(world, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController);
			markDirty();
		}
	}

	private void removeConnectingBlock(BlockPos pos) {
		if (connectingBlocks.remove(pos)) {
			getLoadedTileEntity(world, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController);
			markDirty();
		}
	}

	private void removeStorageInventoryDataAndUnregisterController(BlockPos storagePos) {
		if (!storagePositions.contains(storagePos)) {
			return;
		}
		removeStorageInventoryData(storagePos);
		linkedBlocks.remove(storagePos);
		getLoadedTileEntity(world, storagePos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController);
		markDirty();
		notifyBlockUpdate();
	}

	private void removeStorageInventoryData(BlockPos storagePos) {
		int idx = storagePositions.indexOf(storagePos);
		totalSlots -= getStorageSlots(idx);
		storagePositions.remove(idx);
		storagePositionsByDistance = null;
		int slotsRemoved = idx == 0 ? baseIndexes.get(0) : baseIndexes.get(idx) - baseIndexes.get(idx - 1);
		baseIndexes.remove(idx);
		for (int i = idx; i < baseIndexes.size(); i++) {
			baseIndexes.set(i, baseIndexes.get(i) - slotsRemoved);
		}
	}

	/** What's no longer joined to the controller, through its neighbours or a link, leaves the network. */
	private void verifyStoragesConnected() {
		Set<BlockPos> toVerify = new HashSet<>(storagePositions);
		toVerify.addAll(connectingBlocks);
		toVerify.addAll(nonConnectingBlocks);

		Set<BlockPos> positionsToCheck = new HashSet<>();
		for (EnumFacing dir : EnumFacing.values()) {
			BlockPos offsetPos = getPos().offset(dir);
			if (toVerify.contains(offsetPos)) {
				positionsToCheck.add(offsetPos);
			}
		}
		Set<BlockPos> positionsChecked = new HashSet<>();
		verifyConnected(toVerify, positionsToCheck, positionsChecked);

		for (BlockPos linkedPosition : linkedBlocks) {
			getLoadedTileEntity(world, linkedPosition, ILinkable.class).ifPresent(linkable -> {
				if (linkable.connectLinkedSelf() && toVerify.contains(linkedPosition)) {
					positionsToCheck.add(linkedPosition);
				}
				for (BlockPos p : linkable.getConnectablePositions()) {
					if (toVerify.contains(p)) {
						positionsToCheck.add(p);
					}
				}
			});
		}
		verifyConnected(toVerify, positionsToCheck, positionsChecked);

		for (BlockPos pos : toVerify) {
			removeConnectingBlock(pos);
			removeNonConnectingBlock(pos);
			removeStorageInventoryDataAndUnregisterController(pos);
		}
	}

	private void verifyConnected(Set<BlockPos> toVerify, Set<BlockPos> positionsToCheck, Set<BlockPos> positionsChecked) {
		while (!positionsToCheck.isEmpty()) {
			Iterator<BlockPos> it = positionsToCheck.iterator();
			BlockPos posToCheck = it.next();
			it.remove();
			positionsChecked.add(posToCheck);
			getLoadedTileEntity(world, posToCheck, IControllerBoundable.class).ifPresent(boundable -> {
				toVerify.remove(posToCheck);
				if (boundable.canConnectStorages()) {
					for (EnumFacing dir : EnumFacing.values()) {
						BlockPos pos = posToCheck.offset(dir);
						if (!positionsChecked.contains(pos) && toVerify.contains(pos)) {
							positionsToCheck.add(pos);
						}
					}
				}
			});
		}
	}

	@Override
	public int getSlots() {
		return totalSlots;
	}

	private int getIndexForSlot(int slot) {
		if (slot < 0) {
			return -1;
		}
		for (int i = 0; i < baseIndexes.size(); i++) {
			if (slot < baseIndexes.get(i)) {
				return i;
			}
		}
		return -1;
	}

	private Optional<IControllableStorage> getStorage(int index) {
		if (index < 0 || index >= storagePositions.size()) {
			return Optional.empty();
		}
		return getLoadedTileEntity(world, storagePositions.get(index), IControllableStorage.class);
	}

	private IItemHandlerModifiable getHandlerFromIndex(int index) {
		return getStorage(index).<IItemHandlerModifiable>map(IControllableStorage::getControllerInventory).orElse((IItemHandlerModifiable) EmptyHandler.INSTANCE);
	}

	private int getSlotFromIndex(int slot, int index) {
		if (index <= 0 || index >= baseIndexes.size()) {
			return slot;
		}
		return slot - baseIndexes.get(index - 1);
	}

	private boolean isSlotIndexInvalid(int slot) {
		return slot < 0 || slot >= totalSlots;
	}

	private boolean validateHandlerSlotIndex(IItemHandlerModifiable handler, int handlerIndex, int slot, String methodName) {
		if (slot >= 0 && slot < handler.getSlots()) {
			return true;
		}
		if (handlerIndex < 0 || handlerIndex >= storagePositions.size()) {
			DeconstructedCore.LOGGER.debug("Invalid handler index calculated {} in controller's {} method. If you see many of these messages try replacing controller at {}",
					handlerIndex, methodName, getPos());
		} else {
			DeconstructedCore.LOGGER.debug("Invalid slot {} passed into controller's {} method for storage at {}. If you see many of these messages try replacing controller at {}",
					slot, methodName, storagePositions.get(handlerIndex), getPos());
		}
		return false;
	}

	@Nonnull
	@Override
	public ItemStack getStackInSlot(int slot) {
		if (isSlotIndexInvalid(slot)) {
			return ItemStack.EMPTY;
		}
		int handlerIndex = getIndexForSlot(slot);
		IItemHandlerModifiable handler = getHandlerFromIndex(handlerIndex);
		int localSlot = getSlotFromIndex(slot, handlerIndex);
		return validateHandlerSlotIndex(handler, handlerIndex, localSlot, "getStackInSlot") ? handler.getStackInSlot(localSlot) : ItemStack.EMPTY;
	}

	/** Into any slot goes as into the whole: where it best fits, as the controller puts items away. */
	@Nonnull
	@Override
	public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
		if (isSlotIndexInvalid(slot)) {
			return stack;
		}
		return insertItem(stack, simulate, true);
	}

	@Override
	public ItemStack insertItem(ItemStack stack, boolean simulate) {
		return insertItem(stack, simulate, true);
	}

	/**
	 * Where the same stack is, then where the same item is with room left, then into storages that remember it or are
	 * set for it, and only then, if asked, into the nearest storages with empty slots.
	 */
	protected ItemStack insertItem(ItemStack stack, boolean simulate, boolean insertIntoAnyEmpty) {
		if (stack.isEmpty() || world == null) {
			return stack;
		}
		ItemStack remaining = insertIntoStorages(storagePositions, stack, simulate, (storage, inventory) -> inventory.hasStack(stack));
		if (remaining.isEmpty()) {
			return remaining;
		}
		remaining = insertIntoStorages(storagePositions, remaining, simulate,
				(storage, inventory) -> inventory.hasItem(stack.getItem()) && inventory.hasEmptySlots() && storage.emptySlotsAcceptItems());
		if (remaining.isEmpty()) {
			return remaining;
		}
		remaining = insertIntoStorages(storagePositions, remaining, simulate, (storage, inventory) -> getMemorySettings(storage).matchesFilter(stack));
		if (remaining.isEmpty()) {
			return remaining;
		}
		remaining = insertIntoStorages(storagePositions, remaining, simulate, (storage, inventory) -> inventory.isFilterItem(stack.getItem()));
		if (remaining.isEmpty() || !insertIntoAnyEmpty) {
			return remaining;
		}
		return insertIntoStorages(getStoragePositionsByDistance(), remaining, simulate, (storage, inventory) -> inventory.hasEmptySlots() && storage.emptySlotsAcceptItems());
	}

	private interface StorageMatcher {
		boolean matches(IControllableStorage storage, InventoryHandler inventory);
	}

	private ItemStack insertIntoStorages(Collection<BlockPos> positions, ItemStack stack, boolean simulate, StorageMatcher matcher) {
		ItemStack remaining = stack;
		// copied: putting items away can change the network, as a void upgrade's storage
		for (BlockPos storagePos : new ArrayList<>(positions)) {
			Optional<IControllableStorage> storage = getLoadedTileEntity(world, storagePos, IControllableStorage.class);
			if (!storage.isPresent() || !matcher.matches(storage.get(), storage.get().getStorageWrapper().getInventoryHandler())) {
				continue;
			}
			remaining = storage.get().getControllerInventory().insertItem(remaining, simulate);
			if (remaining.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}
		return remaining;
	}

	private static MemorySettingsCategory getMemorySettings(IControllableStorage storage) {
		return storage.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
	}

	private List<BlockPos> getStoragePositionsByDistance() {
		if (storagePositionsByDistance == null) {
			storagePositionsByDistance = new ArrayList<>(storagePositions);
			storagePositionsByDistance.sort(distanceComparator);
		}
		return storagePositionsByDistance;
	}

	/** Whether a storage of the network holds this item, remembers it, or is set for it: what a deposit takes. */
	protected boolean canDepositStack(ItemStack stack) {
		return anyStorageMatches((storage, inventory) -> inventory.hasItem(stack.getItem()) || getMemorySettings(storage).matchesFilter(stack)
				|| inventory.isFilterItem(stack.getItem()));
	}

	private boolean anyStorageMatches(StorageMatcher matcher) {
		for (BlockPos storagePos : storagePositions) {
			Optional<IControllableStorage> storage = getLoadedTileEntity(world, storagePos, IControllableStorage.class);
			if (storage.isPresent() && matcher.matches(storage.get(), storage.get().getStorageWrapper().getInventoryHandler())) {
				return true;
			}
		}
		return false;
	}

	@Nonnull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (isSlotIndexInvalid(slot)) {
			return ItemStack.EMPTY;
		}
		int handlerIndex = getIndexForSlot(slot);
		IItemHandlerModifiable handler = getHandlerFromIndex(handlerIndex);
		int localSlot = getSlotFromIndex(slot, handlerIndex);
		return validateHandlerSlotIndex(handler, handlerIndex, localSlot, "extractItem") ? handler.extractItem(localSlot, amount, simulate) : ItemStack.EMPTY;
	}

	@Override
	public int getSlotLimit(int slot) {
		if (isSlotIndexInvalid(slot)) {
			return 0;
		}
		int handlerIndex = getIndexForSlot(slot);
		IItemHandlerModifiable handler = getHandlerFromIndex(handlerIndex);
		int localSlot = getSlotFromIndex(slot, handlerIndex);
		return validateHandlerSlotIndex(handler, handlerIndex, localSlot, "getSlotLimit") ? handler.getSlotLimit(localSlot) : 0;
	}

	@Override
	public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
		if (isSlotIndexInvalid(slot)) {
			return;
		}
		int handlerIndex = getIndexForSlot(slot);
		IItemHandlerModifiable handler = getHandlerFromIndex(handlerIndex);
		int localSlot = getSlotFromIndex(slot, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, localSlot, "setStackInSlot")) {
			handler.setStackInSlot(localSlot, stack);
		}
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		detachFromStoragesAndUnlinkBlocks();
	}

	/** Gone, its storages are on their own again, and what was linked to it unlinked. */
	public void detachFromStoragesAndUnlinkBlocks() {
		storagePositions.forEach(pos -> getLoadedTileEntity(world, pos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController));
		connectingBlocks.forEach(pos -> getLoadedTileEntity(world, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
		nonConnectingBlocks.forEach(pos -> getLoadedTileEntity(world, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
		// copied: these unlinking remove themselves from the set
		new HashSet<>(linkedBlocks).forEach(linkedPos -> getLoadedTileEntity(world, linkedPos, ILinkable.class).ifPresent(ILinkable::unlinkFromController));
	}

	protected void notifyBlockUpdate() {
		if (world != null && !world.isRemote) {
			IBlockState state = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, state, state, 3);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		return saveData(tag);
	}

	private NBTTagCompound saveData(NBTTagCompound tag) {
		tag.setTag(STORAGE_POSITIONS_TAG, writePositions(storagePositions));
		tag.setTag(CONNECTING_BLOCKS_TAG, writePositions(connectingBlocks));
		tag.setTag(NON_CONNECTING_BLOCKS_TAG, writePositions(nonConnectingBlocks));
		tag.setTag(LINKED_BLOCKS_TAG, writePositions(linkedBlocks));
		tag.setIntArray(BASE_INDEXES_TAG, baseIndexes.stream().mapToInt(Integer::intValue).toArray());
		tag.setInteger(TOTAL_SLOTS_TAG, totalSlots);
		return tag;
	}

	private static NBTTagList writePositions(Collection<BlockPos> positions) {
		NBTTagList list = new NBTTagList();
		positions.forEach(p -> list.appendTag(new NBTTagLong(p.toLong())));
		return list;
	}

	private static <C extends Collection<BlockPos>> C readPositions(NBTTagCompound tag, String key, C positions) {
		NBTTagList list = tag.getTagList(key, Constants.NBT.TAG_LONG);
		for (int i = 0; i < list.tagCount(); i++) {
			positions.add(BlockPos.fromLong(((NBTTagLong) list.get(i)).getLong()));
		}
		return positions;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		loadData(tag);
	}

	private void loadData(NBTTagCompound tag) {
		storagePositions = readPositions(tag, STORAGE_POSITIONS_TAG, new ArrayList<>());
		storagePositionsByDistance = null;
		connectingBlocks = readPositions(tag, CONNECTING_BLOCKS_TAG, new LinkedHashSet<>());
		nonConnectingBlocks = readPositions(tag, NON_CONNECTING_BLOCKS_TAG, new LinkedHashSet<>());
		linkedBlocks = readPositions(tag, LINKED_BLOCKS_TAG, new LinkedHashSet<>());
		baseIndexes = new ArrayList<>();
		for (int baseIndex : tag.getIntArray(BASE_INDEXES_TAG)) {
			baseIndexes.add(baseIndex);
		}
		totalSlots = tag.getInteger(TOTAL_SLOTS_TAG);
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, 0, saveData(new NBTTagCompound()));
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		loadData(pkt.getNbtCompound());
	}

	public Set<BlockPos> getLinkedBlocks() {
		return linkedBlocks;
	}

	public List<BlockPos> getStoragePositions() {
		return storagePositions;
	}

	/** Storages of the network that match, loaded. */
	public <T> List<T> getStorages(Class<T> type, Predicate<T> filter) {
		List<T> ret = new ArrayList<>();
		for (BlockPos storagePos : storagePositions) {
			getLoadedTileEntity(world, storagePos, type).filter(filter).ifPresent(ret::add);
		}
		return ret;
	}
}
