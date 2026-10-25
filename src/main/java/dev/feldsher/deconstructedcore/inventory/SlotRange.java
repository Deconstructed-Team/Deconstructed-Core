package dev.feldsher.deconstructedcore.inventory;

/** A run of an inventory's slots: from the first, so many of them. */
public final class SlotRange {
	private final int firstSlot;
	private final int numberOfSlots;

	public SlotRange(int firstSlot, int numberOfSlots) {
		this.firstSlot = firstSlot;
		this.numberOfSlots = numberOfSlots;
	}

	public int firstSlot() {
		return firstSlot;
	}

	public int numberOfSlots() {
		return numberOfSlots;
	}
}
