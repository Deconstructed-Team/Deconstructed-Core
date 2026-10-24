package dev.feldsher.deconstructedcore.client.gui;

import dev.feldsher.deconstructedcore.client.gui.controls.ImageButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import net.minecraft.util.text.TextComponentTranslation;

/** The first tab of a storage's screen: it opens the storage's settings. */
public class StorageSettingsTab extends Tab {
	private static final TextureBlitData ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(16, 96), Dimension.SQUARE_16);
	private final StorageScreenBase<?> screen;

	public StorageSettingsTab(Position position, StorageScreenBase<?> screen, String tabTooltip) {
		super(position, new TextComponentTranslation(tabTooltip), onTabIconClicked -> new ImageButton(new Position(position.x() + 1, position.y() + 4), Dimension.SQUARE_16, ICON, onTabIconClicked));
		this.screen = screen;
	}

	@Override
	protected void onTabIconClicked(int button) {
		screen.getMenu().openSettings();
	}
}
