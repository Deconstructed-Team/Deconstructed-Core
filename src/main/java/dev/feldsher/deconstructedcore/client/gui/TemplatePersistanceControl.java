package dev.feldsher.deconstructedcore.client.gui;

import dev.feldsher.deconstructedcore.client.gui.controls.Button;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.CompositeWidgetBase;
import dev.feldsher.deconstructedcore.client.gui.controls.TextBox;
import dev.feldsher.deconstructedcore.client.gui.controls.WidgetBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.Rect2i;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import dev.feldsher.deconstructedcore.common.gui.TemplatePersistanceContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_BACKGROUND;
import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND;

/**
 * The save, load and export buttons beside the settings screen. The wheel over save or load picks the slot; while the
 * mouse is over save on a slot to name, or over export, what's typed goes into the text box beside it.
 */
public class TemplatePersistanceControl extends CompositeWidgetBase<WidgetBase> {
	private static final TextureBlitData SAVE_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(240, 16), Dimension.SQUARE_16);
	public static final ButtonDefinition SAVE_TEMPLATE = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, SAVE_FOREGROUND,
			new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("save_template")));
	private static final TextureBlitData LOAD_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(240, 32), Dimension.SQUARE_16);
	public static final ButtonDefinition LOAD_TEMPLATE = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, LOAD_FOREGROUND,
			new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("load_template")));
	private static final TextureBlitData EXPORT_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(240, 48), Dimension.SQUARE_16);
	public static final ButtonDefinition EXPORT_TEMPLATE = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, EXPORT_FOREGROUND,
			new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("export_template")));
	private static final int BUTTON_HEIGHT = 18;
	private final Button loadTemplateButton;
	private final Button saveTemplateButton;
	private final TextBox saveInput;
	private final TextBox exportInput;
	private final TemplatePersistanceContainer container;

	public TemplatePersistanceControl(Position position, TemplatePersistanceContainer container) {
		super(position, new Dimension(18, 3 * BUTTON_HEIGHT));
		this.container = container;
		container.setOnSlotsRefreshed(() -> {
			setSaveTooltip();
			setLoadTooltip();
		});
		saveInput = new TextBox(new Position(x + 20, y), new Dimension(192, 18)) {
			@Override
			protected void onEnterPressed() {
				saveTemplate();
			}
		};
		saveInput.setVisible(false);
		addChild(saveInput);
		saveTemplateButton = new Button(new Position(x, y), SAVE_TEMPLATE, button -> saveTemplate()) {
			@Override
			public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
				container.scrollSaveSlot(delta > 0);
				setSaveTooltip();
				return true;
			}

			@Override
			public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
				boolean mouseOver = isMouseOver(mouseX, mouseY);
				boolean showTextBox = container.showsTextbox() && mouseOver;
				if (mouseOver) {
					GuiHelper.renderTooltip(screen, GuiHelper.toLines(getTooltip()), getX() + 10, getY() + (showTextBox ? -13 : 6));
				}
				saveInput.setVisible(showTextBox);
				saveInput.setFocused(showTextBox);
			}
		};
		setSaveTooltip();
		addChild(saveTemplateButton);

		loadTemplateButton = new Button(new Position(x, y + BUTTON_HEIGHT), LOAD_TEMPLATE, button -> container.loadTemplate()) {
			@Override
			public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
				container.scrollLoadSlot(delta > 0);
				setLoadTooltip();
				return true;
			}

			@Override
			public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
				if (isMouseOver(mouseX, mouseY)) {
					GuiHelper.renderTooltip(screen, GuiHelper.toLines(getTooltip()), getX() + 10, getY() + 6);
				}
			}
		};
		setLoadTooltip();
		addChild(loadTemplateButton);

		exportInput = new TextBox(new Position(x + 20, y + 2 * BUTTON_HEIGHT), new Dimension(171, 18)) {
			@Override
			protected void onEnterPressed() {
				exportTemplate();
			}
		};
		exportInput.setVisible(false);
		addChild(exportInput);
		Button exportTemplateButton = new Button(new Position(x, y + 2 * BUTTON_HEIGHT), EXPORT_TEMPLATE, button -> exportTemplate()) {
			@Override
			public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
				boolean mouseOver = isMouseOver(mouseX, mouseY);
				if (mouseOver) {
					GuiHelper.renderTooltip(screen, GuiHelper.toLines(getTooltip()), getX() + 10, getY() - 13);
				}
				exportInput.setVisible(mouseOver);
				exportInput.setFocused(mouseOver);
			}
		};
		exportTemplateButton.setTooltip(Arrays.asList(
				new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("export_template"),
						new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("export_template.enter_file_name")).setStyle(color(TextFormatting.GREEN))),
				new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("export_template.file_info")).setStyle(color(TextFormatting.DARK_GRAY).setItalic(true))
		));
		addChild(exportTemplateButton);
	}

	private static Style color(TextFormatting color) {
		return new Style().setColor(color);
	}

	private void saveTemplate() {
		container.saveTemplate(saveInput.getValue());
		saveInput.setValue("");
		setSaveTooltip();
	}

	private void exportTemplate() {
		container.exportTemplate(exportInput.getValue());
		exportInput.setValue("");
	}

	private void setLoadTooltip() {
		if (container.getLoadSlot() == -1) {
			loadTemplateButton.setTooltip(Collections.singletonList(
					new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("load_template.no_save")).setStyle(color(TextFormatting.RED))));
			return;
		}
		List<ITextComponent> tooltip = new ArrayList<>();
		tooltip.add(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("load_template"), container.getLoadSlotTooltipName().setStyle(color(TextFormatting.GREEN))));
		container.getLoadSlotSource().ifPresent(source -> tooltip.add(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("load_template.file_source"),
				new TextComponentString(source).setStyle(color(TextFormatting.GRAY).setItalic(true))).setStyle(color(TextFormatting.DARK_GRAY).setItalic(true))));
		tooltip.add(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("load_template.controls")).setStyle(color(TextFormatting.DARK_GRAY).setItalic(true)));
		if (container.templateHasTooManySlots()) {
			tooltip.add(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("load_template.too_many_setting_slots")).setStyle(color(TextFormatting.RED)));
		}
		loadTemplateButton.setTooltip(tooltip);
	}

	private void setSaveTooltip() {
		saveTemplateButton.setTooltip(Arrays.asList(
				new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("save_template"), container.getSaveSlotTooltipName().setStyle(color(TextFormatting.GREEN))),
				new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("save_template.controls")).setStyle(color(TextFormatting.DARK_GRAY).setItalic(true))
		));
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	/** Whether a focused text box took the key. */
	public boolean keyTyped(char typedChar, int keyCode) {
		return saveInput.keyTyped(typedChar, keyCode) || exportInput.keyTyped(typedChar, keyCode);
	}

	/** While it is, the slots show what the selected template would set. */
	public boolean isTemplateLoadHovered() {
		return loadTemplateButton.isHovered();
	}

	/** The buttons; the text boxes show only while the mouse is on them. */
	public Rect2i getRectangle() {
		return new Rect2i(x, y, getWidth(), getHeight());
	}
}
