package dev.feldsher.deconstructedcore.upgrades.jukebox;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.UpgradeSettingsTab;
import dev.feldsher.deconstructedcore.client.gui.controls.Button;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_BACKGROUND;
import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND;
import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.ICONS;

/** The disc slots and the buttons to play them: the advanced one also goes back and forth, shuffles and repeats. */
public abstract class JukeboxUpgradeTab extends UpgradeSettingsTab<JukeboxUpgradeContainer> {
	private static final ButtonDefinition PLAY = button(new UV(16, 64), "play");
	private static final ButtonDefinition STOP = button(new UV(0, 64), "stop");
	private static final ButtonDefinition PREVIOUS = button(new UV(48, 96), "previous_disc");
	private static final ButtonDefinition NEXT = button(new UV(32, 96), "next_disc");
	private static final ButtonDefinition.Toggle<Boolean> SHUFFLE;
	private static final ButtonDefinition.Toggle<RepeatMode> REPEAT;
	private static final int BUTTON_PADDING = 3;
	public static final int TOP_Y = 24;

	static {
		Map<Boolean, ToggleButton.StateData> shuffleStates = new HashMap<>();
		shuffleStates.put(true, stateData(new UV(96, 80), "shuffle_on"));
		shuffleStates.put(false, stateData(new UV(112, 80), "shuffle_off"));
		SHUFFLE = new ButtonDefinition.Toggle<>(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND, shuffleStates, DEFAULT_BUTTON_HOVERED_BACKGROUND);

		Map<RepeatMode, ToggleButton.StateData> repeatStates = new EnumMap<>(RepeatMode.class);
		repeatStates.put(RepeatMode.ALL, stateData(new UV(128, 80), "repeat_all"));
		repeatStates.put(RepeatMode.ONE, stateData(new UV(144, 80), "repeat_one"));
		repeatStates.put(RepeatMode.NO, stateData(new UV(160, 80), "no_repeat"));
		REPEAT = new ButtonDefinition.Toggle<>(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND, repeatStates, DEFAULT_BUTTON_HOVERED_BACKGROUND);
	}

	private static TextureBlitData icon(UV uv) {
		return new TextureBlitData(ICONS, new Position(1, 1), Dimension.SQUARE_256, uv, Dimension.SQUARE_16);
	}

	private static ButtonDefinition button(UV uv, String name) {
		return new ButtonDefinition(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, icon(uv),
				new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton(name)));
	}

	private static ToggleButton.StateData stateData(UV uv, String name) {
		return new ToggleButton.StateData(icon(uv), new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton(name)));
	}

	private static ITextComponent tooltip(String upgradeName) {
		return new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey(upgradeName) + TranslationHelper.TOOLTIP_SUFFIX);
	}

	private final int slotsInRow;

	protected JukeboxUpgradeTab(JukeboxUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsInRow, String upgradeName) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade(upgradeName), tooltip(upgradeName));
		this.slotsInRow = slotsInRow;
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		super.renderBg(minecraft, mouseX, mouseY);
		if (getContainer().isOpen()) {
			int slots = getContainer().getSlots().size();
			GuiHelper.renderSlotsBackground(x + 3, y + TOP_Y, slotsInRow, slots / slotsInRow, slots % slotsInRow);
		}
	}

	@Override
	protected void moveSlotsToTab() {
		int slotIndex = 0;
		for (Slot discSlot : getContainer().getSlots()) {
			discSlot.xPos = x - screen.getGuiLeft() + 4 + (slotIndex % slotsInRow) * 18;
			discSlot.yPos = y - screen.getGuiTop() + TOP_Y + 1 + (slotIndex / slotsInRow) * 18;
			slotIndex++;
		}
	}

	protected int getBottomSlotY() {
		int slots = getContainer().getSlots().size();
		return TOP_Y + (slots / slotsInRow) * 18 + (slots % slotsInRow > 0 ? 18 : 0);
	}

	public static class Basic extends JukeboxUpgradeTab {
		public Basic(JukeboxUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
			super(upgradeContainer, position, screen, 4, "jukebox");
			int bottomSlotY = getBottomSlotY();
			addHideableChild(new Button(new Position(x + 3, y + bottomSlotY + BUTTON_PADDING), STOP, button -> {
				if (button == 0) {
					getContainer().stop();
				}
			}));
			addHideableChild(new Button(new Position(x + 21, y + bottomSlotY + BUTTON_PADDING), PLAY, button -> {
				if (button == 0) {
					getContainer().play();
				}
			}));
		}
	}

	public static class Advanced extends JukeboxUpgradeTab {
		private static final int PLAYTIME_OVERLAY_COLOR = 0x5500CC00;

		public Advanced(JukeboxUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsInRow) {
			super(upgradeContainer, position, screen, slotsInRow, "advanced_jukebox");
			int bottomSlotY = getBottomSlotY();
			addHideableChild(new Button(new Position(x + 3, y + bottomSlotY + BUTTON_PADDING), PREVIOUS, button -> {
				if (button == 0) {
					getContainer().previous();
				}
			}));
			addHideableChild(new Button(new Position(x + 21, y + bottomSlotY + BUTTON_PADDING), STOP, button -> {
				if (button == 0) {
					getContainer().stop();
				}
			}));
			addHideableChild(new Button(new Position(x + 39, y + bottomSlotY + BUTTON_PADDING), PLAY, button -> {
				if (button == 0) {
					getContainer().play();
				}
			}));
			addHideableChild(new Button(new Position(x + 57, y + bottomSlotY + BUTTON_PADDING), NEXT, button -> {
				if (button == 0) {
					getContainer().next();
				}
			}));
			addHideableChild(new ToggleButton<>(new Position(x + 12, y + bottomSlotY + BUTTON_PADDING + 20), SHUFFLE, button -> {
				if (button == 0) {
					getContainer().toggleShuffle();
				}
			}, () -> getContainer().isShuffleEnabled()));
			addHideableChild(new ToggleButton<>(new Position(x + 48, y + bottomSlotY + BUTTON_PADDING + 20), REPEAT, button -> {
				if (button == 0) {
					getContainer().toggleRepeat();
				}
			}, () -> getContainer().getRepeatMode()));
		}

		@Override
		protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
			super.renderBg(minecraft, mouseX, mouseY);
			getContainer().getDiscSlotActive().ifPresent(slot ->
					renderPlaytimeOverlay(screen.getGuiLeft() + slot.xPos, screen.getGuiTop() + slot.yPos, 16, 16));
		}

		/** Unknown for discs that aren't vanilla's, which then show none. */
		private float getPlaybackRemainingProgress() {
			int length = getContainer().getDiscLengthInTicks();
			if (length <= 0 || minecraft.world == null) {
				return 0;
			}
			long remaining = getContainer().getDiscFinishTime() - minecraft.world.getTotalWorldTime();
			return remaining / (float) length;
		}

		/** Over the part of the disc's slot still to play. */
		private void renderPlaytimeOverlay(int xPos, int yPos, int width, int height) {
			float remainingProgress = getPlaybackRemainingProgress();
			if (remainingProgress <= 0) {
				return;
			}
			int progressOver = width - (int) (width * Math.min(remainingProgress, 1));
			GlStateManager.disableDepth();
			GlStateManager.colorMask(true, true, true, false);
			Gui.drawRect(xPos + progressOver, yPos, xPos + width, yPos + height, PLAYTIME_OVERLAY_COLOR);
			GlStateManager.colorMask(true, true, true, true);
			GlStateManager.enableDepth();
		}
	}
}
