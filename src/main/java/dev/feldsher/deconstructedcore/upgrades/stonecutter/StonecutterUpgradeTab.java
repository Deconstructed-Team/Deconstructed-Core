package dev.feldsher.deconstructedcore.upgrades.stonecutter;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.UpgradeSettingsTab;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import net.minecraft.util.text.TextComponentTranslation;

public class StonecutterUpgradeTab extends UpgradeSettingsTab<StonecutterUpgradeContainer> {
	private final StonecutterRecipeControl recipeControl;

	public StonecutterUpgradeTab(StonecutterUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, ButtonDefinition.Toggle<Boolean> shiftClickTargetButton) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("stonecutter"),
				new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey("stonecutter") + TranslationHelper.TOOLTIP_SUFFIX));
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), shiftClickTargetButton,
				button -> getContainer().setShiftClickIntoStorage(!getContainer().shouldShiftClickIntoStorage()), () -> getContainer().shouldShiftClickIntoStorage()));
		recipeControl = addHideableChild(new StonecutterRecipeControl(screen, upgradeContainer.getRecipeContainer(), new Position(x + 3, y + 24)));
	}

	@Override
	protected void moveSlotsToTab() {
		recipeControl.moveSlotsToView();
	}
}
