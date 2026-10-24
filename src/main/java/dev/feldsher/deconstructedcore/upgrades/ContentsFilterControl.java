package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;

/** A filter control whose first button picks allow, block or what the storage holds. */
public abstract class ContentsFilterControl extends FilterLogicControl<ContentsFilterLogic, ContentsFilterLogicContainer> {

	protected ContentsFilterControl(StorageScreenBase<?> screen, Position position, ContentsFilterLogicContainer filterLogicContainer, int slotsPerRow, ButtonDefinition.Toggle<ContentsFilterType> contentsFilterButton, MatchButton... matchButtons) {
		super(screen, position, filterLogicContainer, slotsPerRow, true, matchButtons);
		addChild(new ToggleButton<>(new Position(x, y), contentsFilterButton, button -> updateFilterType(), container::getFilterType));
	}

	private void updateFilterType() {
		ContentsFilterType next = container.getFilterType().next();
		if (container.getPrimaryMatch() == PrimaryMatch.TAGS && next == ContentsFilterType.STORAGE) {
			next = next.next();
		}
		container.setFilterType(next);

		boolean slotsEnabled = next != ContentsFilterType.STORAGE;
		container.getFilterSlots().forEach(slot -> slot.setEnabled(slotsEnabled));
	}

	@Override
	protected void onTagsMatchSelected() {
		if (container.getFilterType() == ContentsFilterType.STORAGE) {
			updateFilterType();
		}
	}

	public static class Basic extends ContentsFilterControl {
		public Basic(StorageScreenBase<?> screen, Position position, ContentsFilterLogicContainer filterLogicContainer, int slotsPerRow, ButtonDefinition.Toggle<ContentsFilterType> contentsFilterButton) {
			super(screen, position, filterLogicContainer, slotsPerRow, contentsFilterButton);
		}
	}

	public static class Advanced extends ContentsFilterControl {
		public Advanced(StorageScreenBase<?> screen, Position position, ContentsFilterLogicContainer filterLogicContainer, int slotsPerRow, ButtonDefinition.Toggle<ContentsFilterType> contentsFilterButton) {
			super(screen, position, filterLogicContainer, slotsPerRow, contentsFilterButton, MatchButton.PRIMARY_MATCH, MatchButton.DURABILITY, MatchButton.NBT);
		}
	}
}
