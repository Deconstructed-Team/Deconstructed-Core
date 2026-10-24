package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;

public class FilterLogicControl<L extends FilterLogic, C extends FilterLogicContainer<L>>
		extends FilterLogicControlBase<L, FilterLogicContainer.FilterLogicSlot, C> {
	public FilterLogicControl(StorageScreenBase<?> screen, Position position, C filterLogicContainer, int slotsPerRow, MatchButton... showMatchButtons) {
		this(screen, position, filterLogicContainer, slotsPerRow, showMatchButtons.length > 0, showMatchButtons);
	}

	protected FilterLogicControl(StorageScreenBase<?> screen, Position position, C filterLogicContainer, int slotsPerRow, boolean buttonsVisible, MatchButton... showMatchButtons) {
		super(screen, filterLogicContainer, position, buttonsVisible, slotsPerRow, showMatchButtons);
	}

	public static class Basic extends FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> {
		public Basic(StorageScreenBase<?> screen, Position position, FilterLogicContainer<FilterLogic> filterLogicContainer, int slotsPerRow) {
			super(screen, position, filterLogicContainer, slotsPerRow, MatchButton.ALLOW_LIST);
		}
	}

	public static class Advanced extends FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> {
		public Advanced(StorageScreenBase<?> screen, Position position, FilterLogicContainer<FilterLogic> filterLogicContainer, int slotsPerRow) {
			super(screen, position, filterLogicContainer, slotsPerRow, MatchButton.ALLOW_LIST, MatchButton.PRIMARY_MATCH, MatchButton.DURABILITY, MatchButton.NBT);
		}
	}
}
