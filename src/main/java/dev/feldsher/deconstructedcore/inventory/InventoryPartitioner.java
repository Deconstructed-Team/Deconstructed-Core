package dev.feldsher.deconstructedcore.inventory;

import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Splits an inventory into runs of slots, each handled by its own {@link IInventoryPartHandler}: by default one plain
 * part over all of it. Upgrades put their parts over slots and take them off again; the split is kept with the
 * inventory, by the parts' names and the first slot of each.
 */
public class InventoryPartitioner {
	public static final String BASE_INDEXES_TAG = "baseIndexes";
	private static final String INVENTORY_PART_NAMES_TAG = "inventoryPartNames";
	private IInventoryPartHandler[] inventoryPartHandlers;
	private int[] baseIndexes;
	private final InventoryHandler parent;

	public InventoryPartitioner(NBTTagCompound tag, InventoryHandler parent, Supplier<MemorySettingsCategory> getMemorySettings) {
		this.parent = parent;
		deserializeNBT(tag, getMemorySettings);
	}

	private int getIndexForSlot(int slot) {
		if (slot < 0) {
			return -1;
		}

		int i = 0;
		for (; i < baseIndexes.length; i++) {
			if (slot - baseIndexes[i] < 0) {
				return i - 1;
			}
		}
		return i - 1;
	}

	public IInventoryPartHandler getPartBySlot(int slot) {
		if (slot < 0 || slot >= parent.getSlots()) {
			return IInventoryPartHandler.EMPTY;
		}
		int index = getIndexForSlot(slot);
		if (index < 0 || index >= inventoryPartHandlers.length) {
			return IInventoryPartHandler.EMPTY;
		}
		return inventoryPartHandlers[index];
	}

	public void onSlotLimitChange() {
		for (IInventoryPartHandler inventoryPartHandler : inventoryPartHandlers) {
			inventoryPartHandler.onSlotLimitChange();
		}
	}

	public Set<Integer> getNoSortSlots() {
		Set<Integer> noSortSlots = new HashSet<>();
		for (IInventoryPartHandler inventoryPartHandler : inventoryPartHandlers) {
			noSortSlots.addAll(inventoryPartHandler.getNoSortSlots());
		}
		return noSortSlots;
	}

	public void onInit() {
		for (IInventoryPartHandler inventoryPartHandler : inventoryPartHandlers) {
			inventoryPartHandler.onInit();
		}
	}

	/** The first plain run of slots, at most so many of them, to put a part over. */
	public Optional<SlotRange> getFirstSpace(int maxNumberOfSlots) {
		for (int partIndex = 0; partIndex < inventoryPartHandlers.length; partIndex++) {
			if (inventoryPartHandlers[partIndex].canBeReplaced()) {
				int firstSlot = baseIndexes[partIndex];
				int numberOfSlots = baseIndexes.length > partIndex + 1 ? baseIndexes[partIndex + 1] - firstSlot : parent.getSlots() - firstSlot;
				numberOfSlots = Math.min(numberOfSlots, maxNumberOfSlots);
				return numberOfSlots > 0 ? Optional.of(new SlotRange(baseIndexes[partIndex], numberOfSlots)) : Optional.empty();
			}
		}
		return Optional.empty();
	}

	/** The part over the run that starts at the part beginning at that slot; what's left of that run stays plain. */
	public void addInventoryPart(int inventorySlot, int numberOfSlots, IInventoryPartHandler inventoryPartHandler) {
		int index = getIndexForSlot(inventorySlot);
		if (index < 0 || index >= inventoryPartHandlers.length || baseIndexes[index] != inventorySlot) {
			return;
		}

		List<IInventoryPartHandler> newParts = new ArrayList<>();
		List<Integer> newBaseIndexes = new ArrayList<>();

		for (int i = 0; i < index; i++) {
			newParts.add(inventoryPartHandlers[i]);
			newBaseIndexes.add(baseIndexes[i]);
		}

		newParts.add(inventoryPartHandler);
		newBaseIndexes.add(inventorySlot);

		long newNextSlot = (long) inventorySlot + numberOfSlots;
		if (inventoryPartHandlers[index].getSlots() > newNextSlot) {
			newParts.add(new IInventoryPartHandler.Default(parent, parent.getSlots() - (int) newNextSlot));
			newBaseIndexes.add((int) newNextSlot);
		}

		for (int i = index + 1; i < inventoryPartHandlers.length; i++) {
			newParts.add(inventoryPartHandlers[i]);
			newBaseIndexes.add(baseIndexes[i]);
		}

		updatePartsAndIndexesFromLists(newParts, newBaseIndexes);

		inventoryPartHandler.onInit();
	}

	/** The part beginning at that slot gives way to a plain one, joined with the plain ones around it. */
	public void removeInventoryPart(int inventorySlot) {
		int index = getIndexForSlot(inventorySlot);

		if (index < 0 || index >= inventoryPartHandlers.length || baseIndexes[index] != inventorySlot) {
			return;
		}

		if (inventoryPartHandlers.length == 1) {
			updatePartsAndIndexesFromLists(Collections.singletonList(new IInventoryPartHandler.Default(parent, parent.getSlots())), Collections.singletonList(0));
			return;
		}

		int slotsAtPartIndex = (baseIndexes.length > index + 1 ? baseIndexes[index + 1] : parent.getSlots()) - baseIndexes[index];

		List<IInventoryPartHandler> newParts = new ArrayList<>();
		List<Integer> newBaseIndexes = new ArrayList<>();

		boolean replacedNext = false;
		for (int i = 0; i < index; i++) {
			if (i == index - 1 && inventoryPartHandlers[i] instanceof IInventoryPartHandler.Default && baseIndexes.length > index + 1 && inventoryPartHandlers[index + 1] instanceof IInventoryPartHandler.Default) {
				newParts.add(new IInventoryPartHandler.Default(parent, inventoryPartHandlers[i].getSlots() + inventoryPartHandlers[index + 1].getSlots() + slotsAtPartIndex));
				newBaseIndexes.add(baseIndexes[i]);
				replacedNext = true;
				continue;
			}

			newParts.add(inventoryPartHandlers[i]);
			newBaseIndexes.add(baseIndexes[i]);
		}

		if (!replacedNext && baseIndexes.length > index + 1) {
			if (inventoryPartHandlers[index + 1] instanceof IInventoryPartHandler.Default) {
				newParts.add(new IInventoryPartHandler.Default(parent, inventoryPartHandlers[index + 1].getSlots() + slotsAtPartIndex));
				newBaseIndexes.add(inventorySlot);
			} else {
				newParts.add(new IInventoryPartHandler.Default(parent, slotsAtPartIndex));
				newBaseIndexes.add(inventorySlot);
				newParts.add(inventoryPartHandlers[index + 1]);
				newBaseIndexes.add(baseIndexes[index + 1]);
			}
		} else if (!replacedNext) {
			newParts.add(new IInventoryPartHandler.Default(parent, slotsAtPartIndex));
			newBaseIndexes.add(inventorySlot);
		}

		for (int i = index + 2; i < inventoryPartHandlers.length; i++) {
			newParts.add(inventoryPartHandlers[i]);
			newBaseIndexes.add(baseIndexes[i]);
		}

		updatePartsAndIndexesFromLists(newParts, newBaseIndexes);
	}

	private void updatePartsAndIndexesFromLists(List<IInventoryPartHandler> newParts, List<Integer> newBaseIndexes) {
		inventoryPartHandlers = newParts.toArray(new IInventoryPartHandler[0]);
		baseIndexes = new int[newBaseIndexes.size()];
		for (int i = 0; i < newBaseIndexes.size(); i++) {
			baseIndexes[i] = newBaseIndexes.get(i);
		}
		parent.saveInventory();
	}

	public NBTTagCompound serializeNBT() {
		NBTTagCompound ret = new NBTTagCompound();
		ret.setIntArray(BASE_INDEXES_TAG, baseIndexes);
		NBTTagList partNames = new NBTTagList();
		for (IInventoryPartHandler inventoryPartHandler : inventoryPartHandlers) {
			partNames.appendTag(new NBTTagString(inventoryPartHandler.getName()));
		}
		ret.setTag(INVENTORY_PART_NAMES_TAG, partNames);
		return ret;
	}

	private void deserializeNBT(NBTTagCompound tag, Supplier<MemorySettingsCategory> getMemorySettings) {
		if (!tag.hasKey(BASE_INDEXES_TAG)) {
			inventoryPartHandlers = new IInventoryPartHandler[] {new IInventoryPartHandler.Default(parent, parent.getSlots())};
			baseIndexes = new int[] {0};
			return;
		}

		baseIndexes = tag.getIntArray(BASE_INDEXES_TAG);
		inventoryPartHandlers = new IInventoryPartHandler[baseIndexes.length];
		NBTTagList partNamesTag = tag.getTagList(INVENTORY_PART_NAMES_TAG, Constants.NBT.TAG_STRING);
		for (int i = 0; i < baseIndexes.length; i++) {
			SlotRange slotRange = new SlotRange(baseIndexes[i], (i + 1 < baseIndexes.length ? baseIndexes[i + 1] : parent.getSlots()) - baseIndexes[i]);
			inventoryPartHandlers[i] = InventoryPartRegistry.instantiatePart(i < partNamesTag.tagCount() ? partNamesTag.getStringTagAt(i) : IInventoryPartHandler.Default.NAME,
					parent, slotRange, getMemorySettings);
		}
	}

	public boolean isInfinite(int slot) {
		return getPartBySlot(slot).isInfinite(slot);
	}
}
