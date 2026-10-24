package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.controls.Button;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinitions;
import dev.feldsher.deconstructedcore.client.gui.controls.CompositeWidgetBase;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.client.gui.controls.WidgetBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

import static dev.feldsher.deconstructedcore.upgrades.FilterLogicControlBase.MatchButton.ALLOW_LIST;
import static dev.feldsher.deconstructedcore.upgrades.FilterLogicControlBase.MatchButton.DURABILITY;
import static dev.feldsher.deconstructedcore.upgrades.FilterLogicControlBase.MatchButton.NBT;
import static dev.feldsher.deconstructedcore.upgrades.FilterLogicControlBase.MatchButton.PRIMARY_MATCH;

/** A filter's controls: its buttons above, then its filter slots, or the tag list when it matches by tags. */
public abstract class FilterLogicControlBase<F extends FilterLogic, S extends Slot, C extends FilterLogicContainerBase<F, S>>
		extends CompositeWidgetBase<WidgetBase> {
	public static final int TAG_FONT_COLOR = 16383998;
	public static final int MORE_TAGS_FONT_COLOR = 13882323;
	private static final int MAX_TAG_NAME_WIDTH = 68;

	protected final MatchButton[] showMatchButtons;
	protected final int slotsTopYOffset;
	protected final int slotsPerRow;
	protected final int slotsInExtraRow;
	protected final int fullSlotRows;
	private final int totalSlotRows;
	private final StorageScreenBase<?> screen;
	protected final C container;
	private final List<ITextComponent> addTagTooltip = new ArrayList<>();
	private final List<ITextComponent> removeTagTooltip = new ArrayList<>();
	private final List<ITextComponent> tagListTooltip = new ArrayList<>();
	@Nullable
	private ToggleButton<Boolean> nbtButton = null;
	@Nullable
	private ToggleButton<Boolean> durabilityButton = null;
	private int tagButtonsYOffset;

	protected FilterLogicControlBase(StorageScreenBase<?> screen, C container, Position position, boolean buttonsVisible, int slotsPerRow, MatchButton... showMatchButtons) {
		super(position, new Dimension(0, 0));
		this.screen = screen;
		this.container = container;
		slotsTopYOffset = buttonsVisible ? 21 : 0;
		this.slotsPerRow = slotsPerRow;
		this.showMatchButtons = showMatchButtons;
		fullSlotRows = container.getFilterSlots().size() / slotsPerRow;
		slotsInExtraRow = container.getFilterSlots().size() % slotsPerRow;
		totalSlotRows = fullSlotRows + (slotsInExtraRow > 0 ? 1 : 0);

		if (shouldShow(ALLOW_LIST)) {
			addChild(new ToggleButton<>(new Position(x, y), ButtonDefinitions.ALLOW_LIST, button -> container.setAllowList(!container.isAllowList()), container::isAllowList));
		}
		if (shouldShow(PRIMARY_MATCH)) {
			addChild(new ToggleButton<>(new Position(x + 18, y), ButtonDefinitions.PRIMARY_MATCH,
					button -> {
						PrimaryMatch next = container.getPrimaryMatch().next();
						if (next == PrimaryMatch.TAGS) {
							container.getFilterSlots().forEach(slot -> slot.xPos = StorageScreenBase.DISABLED_SLOT_X_POS);
							onTagsMatchSelected();
						}
						container.setPrimaryMatch(next);
						setDurabilityAndNbtButtonsVisibility();
						moveSlotsToView();
					}, container::getPrimaryMatch));
			addTagButtons();
		}
		if (shouldShow(DURABILITY)) {
			durabilityButton = new ToggleButton<>(new Position(x + 36, y), ButtonDefinitions.MATCH_DURABILITY,
					button -> container.setMatchDurability(!container.shouldMatchDurability()), container::shouldMatchDurability);
			addChild(durabilityButton);
		}
		if (shouldShow(NBT)) {
			nbtButton = new ToggleButton<>(new Position(x + 54, y), ButtonDefinitions.MATCH_NBT,
					button -> container.setMatchNbt(!container.shouldMatchNbt()), container::shouldMatchNbt);
			addChild(nbtButton);
		}
		updateDimensions(Math.max(slotsPerRow * 18, getMaxButtonWidth()), totalSlotRows * 18 + slotsTopYOffset);
		setDurabilityAndNbtButtonsVisibility();
	}

	private void setDurabilityAndNbtButtonsVisibility() {
		boolean visible = container.getPrimaryMatch() != PrimaryMatch.TAGS;
		if (nbtButton != null) {
			nbtButton.setVisible(visible);
		}
		if (durabilityButton != null) {
			durabilityButton.setVisible(visible);
		}
	}

	protected void onTagsMatchSelected() {
		//noop
	}

	private void addTagButtons() {
		tagButtonsYOffset = slotsTopYOffset + getTagListHeight();
		addChild(new TagButton(new Position(x + 36, y + tagButtonsYOffset), ButtonDefinitions.REMOVE_TAG, button -> {
			container.removeSelectedTag();
			updateTagListAndRemoveTooltips();
			updateAddTooltip();
		}, delta -> {
			if (delta < 0) {
				container.selectNextTagToRemove();
			} else {
				container.selectPreviousTagToRemove();
			}
			updateTagListAndRemoveTooltips();
		}) {
			@Override
			protected List<ITextComponent> getTooltip() {
				return removeTagTooltip;
			}
		});
		updateTagListAndRemoveTooltips();

		addChild(new TagButton(new Position(x + 18, y + tagButtonsYOffset), ButtonDefinitions.ADD_TAG, button -> {
			container.addSelectedTag();
			updateAddTooltip();
			updateTagListAndRemoveTooltips();
		}, delta -> {
			if (delta < 0) {
				container.selectNextTagToAdd();
			} else {
				container.selectPreviousTagToAdd();
			}
			updateAddTooltip();
		}) {
			@Override
			protected List<ITextComponent> getTooltip() {
				return addTagTooltip;
			}
		});
		updateAddTooltip();
		container.getTagSelectionSlot().setOnUpdate(this::updateAddTooltip);

		addChild(new ToggleButton<Boolean>(new Position(x + 54, y + tagButtonsYOffset), ButtonDefinitions.MATCH_ANY_TAG, button -> container.setMatchAnyTag(!container.shouldMatchAnyTag()), container::shouldMatchAnyTag) {
			@Override
			public void render(int mouseX, int mouseY, float partialTicks) {
				if (container.getPrimaryMatch() == PrimaryMatch.TAGS) {
					super.render(mouseX, mouseY, partialTicks);
				}
			}

			@Override
			public boolean isMouseOver(double mouseX, double mouseY) {
				return container.getPrimaryMatch() == PrimaryMatch.TAGS && super.isMouseOver(mouseX, mouseY);
			}
		});
	}

	private static ITextComponent styled(ITextComponent component, TextFormatting... formattings) {
		for (TextFormatting formatting : formattings) {
			if (formatting == TextFormatting.ITALIC) {
				component.getStyle().setItalic(true);
			} else {
				component.getStyle().setColor(formatting);
			}
		}
		return component;
	}

	private void updateTagListAndRemoveTooltips() {
		updateTagListTooltip();
		updateRemoveTooltip();
	}

	private void updateTagListTooltip() {
		tagListTooltip.clear();
		tagListTooltip.add(new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey("tag_list.title")));
		Set<String> tagNames = container.getTagNames();
		if (tagNames.isEmpty()) {
			tagListTooltip.add(styled(new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey("tag_list.empty")), TextFormatting.DARK_GRAY));
			return;
		}

		for (String tagName : tagNames) {
			tagListTooltip.add(styled(new TextComponentString("> " + tagName), TextFormatting.GRAY));
		}
	}

	private void updateRemoveTooltip() {
		removeTagTooltip.clear();
		removeTagTooltip.add(new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("remove_tag")));
		Set<String> tagNames = container.getTagNames();
		if (tagNames.isEmpty()) {
			removeTagTooltip.add(styled(new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("remove_tag.empty")), TextFormatting.RED));
			return;
		}

		int curIndex = 0;
		for (String tagName : tagNames) {
			if (curIndex == container.getSelectedTagToRemove()) {
				removeTagTooltip.add(styled(new TextComponentString("-> " + tagName), TextFormatting.RED));
			} else {
				removeTagTooltip.add(styled(new TextComponentString("> " + tagName), TextFormatting.GRAY));
			}
			curIndex++;
		}
		removeTagTooltip.add(styled(new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("remove_tag.controls")), TextFormatting.ITALIC, TextFormatting.DARK_GRAY));
	}

	private void updateAddTooltip() {
		addTagTooltip.clear();
		addTagTooltip.add(new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("add_tag")));
		if (container.getTagSelectionSlot().getStack().isEmpty()) {
			addTagTooltip.add(styled(new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("add_tag.no_item")), TextFormatting.ITALIC, TextFormatting.DARK_GRAY));
			return;
		}
		Set<String> tagsToAdd = container.getTagsToAdd();
		int curIndex = 0;
		for (String tagName : tagsToAdd) {
			if (curIndex == container.getSelectedTagToAdd()) {
				addTagTooltip.add(styled(new TextComponentString("-> " + tagName), TextFormatting.GREEN));
			} else {
				addTagTooltip.add(styled(new TextComponentString("> " + tagName), TextFormatting.GRAY));
			}
			curIndex++;
		}
		if (tagsToAdd.isEmpty()) {
			addTagTooltip.add(styled(new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("add_tag.no_additional_tags")), TextFormatting.ITALIC, TextFormatting.YELLOW));
		} else {
			addTagTooltip.add(styled(new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeButton("add_tag.controls")), TextFormatting.ITALIC, TextFormatting.DARK_GRAY));
		}
	}

	protected int getMaxButtonWidth() {
		int maxWidth = 0;
		for (WidgetBase w : children) {
			maxWidth = Math.max(maxWidth, w.getX() + w.getWidth() - x);
		}
		return maxWidth;
	}

	protected boolean shouldShow(MatchButton matchButton) {
		for (MatchButton showMatchButton : showMatchButtons) {
			if (showMatchButton == matchButton) {
				return true;
			}
		}
		return false;
	}

	public void moveSlotsToView() {
		if (container.getPrimaryMatch() == PrimaryMatch.TAGS) {
			Slot slot = container.getTagSelectionSlot();
			slot.xPos = x - screen.getGuiLeft() + 1;
			slot.yPos = y - screen.getGuiTop() + tagButtonsYOffset + 1;
			container.getFilterSlots().forEach(s -> s.xPos = StorageScreenBase.DISABLED_SLOT_X_POS);
		} else {
			int upgradeSlotNumber = 0;
			for (S slot : container.getFilterSlots()) {
				slot.xPos = x - screen.getGuiLeft() + 1 + (upgradeSlotNumber % slotsPerRow) * 18;
				slot.yPos = y - screen.getGuiTop() + slotsTopYOffset + 1 + (upgradeSlotNumber / slotsPerRow) * 18;
				upgradeSlotNumber++;
			}
			container.getTagSelectionSlot().xPos = StorageScreenBase.DISABLED_SLOT_X_POS;
		}
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		super.renderWidget(mouseX, mouseY, partialTicks);
		if (container.getPrimaryMatch() == PrimaryMatch.TAGS) {
			renderTagNames();
		}
	}

	private void renderTagNames() {
		int count = 0;
		int prefixWidth = font.getStringWidth("...");
		Set<String> tagNames = container.getTagNames();
		int maxTagNameLines = getTagListHeight() / 10;
		for (String name : tagNames) {
			if (tagNames.size() > maxTagNameLines && count == maxTagNameLines - 1) {
				font.drawString(new TextComponentTranslation(TranslationHelper.INSTANCE.translUpgradeKey("tag_list.tag_overflow"), String.valueOf(tagNames.size() - (maxTagNameLines - 1))).getFormattedText(),
						x + 2, y + 23 + count * 10, MORE_TAGS_FONT_COLOR);
				break;
			}
			String shortened = name;
			if (font.getStringWidth(name) > MAX_TAG_NAME_WIDTH) {
				shortened = "..." + font.trimStringToWidth(name, MAX_TAG_NAME_WIDTH - prefixWidth, true);
			}
			font.drawString(shortened, x + 2, y + 23 + count * 10, TAG_FONT_COLOR);
			count++;
		}
	}

	@Override
	public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
		super.renderTooltip(screen, mouseX, mouseY);
		if (container.getPrimaryMatch() == PrimaryMatch.TAGS && isMouseOverTagList(mouseX, mouseY)) {
			GuiHelper.renderTooltip(screen, GuiHelper.toLines(tagListTooltip), mouseX, mouseY);
		}
	}

	private int getTagListHeight() {
		return Math.max(0, totalSlotRows - 1) * 18;
	}

	private boolean isMouseOverTagList(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + getTagListWidth() && mouseY >= y + slotsTopYOffset && mouseY < y + slotsTopYOffset + getTagListHeight();
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		if (container.getPrimaryMatch() != PrimaryMatch.TAGS) {
			GuiHelper.renderSlotsBackground(x, y + slotsTopYOffset, slotsPerRow, fullSlotRows, slotsInExtraRow);
		} else {
			GuiHelper.renderSlotsBackground(x, y + tagButtonsYOffset, 1, 1, 0);
			GuiHelper.renderControlBackground(x, y + slotsTopYOffset, getTagListWidth(), getTagListHeight());
		}
	}

	private int getTagListWidth() {
		return slotsPerRow * 18;
	}

	private class TagButton extends Button {
		private final DoubleConsumer onScroll;

		public TagButton(Position position, ButtonDefinition buttonDefinition, IntConsumer onClick, DoubleConsumer onScroll) {
			super(position, buttonDefinition, onClick);
			this.onScroll = onScroll;
		}

		@Override
		public void render(int mouseX, int mouseY, float partialTicks) {
			if (container.getPrimaryMatch() == PrimaryMatch.TAGS) {
				super.render(mouseX, mouseY, partialTicks);
			}
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return container.getPrimaryMatch() == PrimaryMatch.TAGS && super.isMouseOver(mouseX, mouseY);
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
			if (container.getPrimaryMatch() != PrimaryMatch.TAGS) {
				return false;
			}
			onScroll.accept(delta);
			return true;
		}
	}

	public enum MatchButton {
		ALLOW_LIST,
		PRIMARY_MATCH,
		DURABILITY,
		NBT
	}
}
