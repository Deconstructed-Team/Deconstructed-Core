package dev.feldsher.deconstructedcore.settings;

import java.util.Optional;

/** A settings category that marks the slots it sets with a color, in the storage's screen too. */
public interface ISlotColorCategory {
	Optional<Integer> getSlotColor(int slotNumber);
}
