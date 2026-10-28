package dev.feldsher.deconstructedcore.client.gui.utils;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import net.minecraft.item.Item;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import net.minecraft.util.text.translation.I18n;

/** The translation keys of a mod, by kind, as 1.21 names them. */
public class TranslationHelper {
	public static final String TOOLTIP_SUFFIX = ".tooltip";
	private static final String BUTTONS_SUFFIX = "buttons.";
	private static final String CONTROLS_SUFFIX = "controls.";
	/** 1.12's lang files keep a line break as the two characters \ and n. */
	private static final String LINE_BREAK = "\\n";

	public static final TranslationHelper INSTANCE = new TranslationHelper(DeconstructedCore.MOD_ID);

	private final String guiPrefix;
	private final String guiUpgradePrefix;
	private final String guiSettingsPrefix;
	private final String guiStatusPrefix;
	private final String buttonsPrefix;
	private final String itemUpgradePrefix;
	private final String upgradeGroupPrefix;
	private final String upgradeButtonsPrefix;
	private final String upgradeControlsPrefix;
	private final String keybindPrefix;

	public TranslationHelper(String modId) {
		guiPrefix = "gui." + modId + ".";
		keybindPrefix = "keybind." + modId + ".";
		itemUpgradePrefix = "item." + modId + ".";
		upgradeGroupPrefix = "upgrade_group." + modId + ".";
		guiUpgradePrefix = guiPrefix + "upgrades.";
		guiSettingsPrefix = guiPrefix + "settings.";
		guiStatusPrefix = guiPrefix + "status.";
		buttonsPrefix = guiPrefix + BUTTONS_SUFFIX;
		upgradeButtonsPrefix = guiUpgradePrefix + BUTTONS_SUFFIX;
		upgradeControlsPrefix = guiUpgradePrefix + CONTROLS_SUFFIX;
	}

	public ITextComponent translStatusMessage(String statusMessage, Object... params) {
		return new TextComponentTranslation(guiStatusPrefix + statusMessage, params);
	}

	public ITextComponent translUpgrade(String upgradeName, Object... params) {
		return new TextComponentTranslation(translUpgradeKey(upgradeName), params);
	}

	public String translUpgradeKey(String upgradeName) {
		return guiUpgradePrefix + upgradeName;
	}

	public String translUpgradeSlotTooltip(String tooltipName) {
		return guiUpgradePrefix + "slots." + tooltipName + TOOLTIP_SUFFIX;
	}

	public String translSettings(String categoryName) {
		return guiSettingsPrefix + categoryName;
	}

	public String translSettingsTooltip(String categoryName) {
		return translSettings(categoryName) + TOOLTIP_SUFFIX;
	}

	public String translSettingsButton(String buttonName) {
		return guiSettingsPrefix + BUTTONS_SUFFIX + buttonName;
	}

	public String translSettingsMessage(String messageName) {
		return guiSettingsPrefix + "message." + messageName;
	}

	public String translButton(String buttonName) {
		return buttonsPrefix + buttonName;
	}

	public ITextComponent translError(String key, Object... params) {
		return new TextComponentTranslation(guiPrefix + "error." + key, params);
	}

	public String translUpgradeGroup(String groupName) {
		return upgradeGroupPrefix + groupName;
	}

	public String translUpgradeButton(String buttonName) {
		return upgradeButtonsPrefix + buttonName;
	}

	public String translUpgradeControl(String controlName) {
		return upgradeControlsPrefix + controlName;
	}

	public String translItemTooltip(Item item) {
		return translItemTooltip(Objects.requireNonNull(item.getRegistryName()).getPath());
	}

	public String translItemTooltip(String itemName) {
		return itemUpgradePrefix + itemName + TOOLTIP_SUFFIX;
	}

	public String translKeybind(String keybindName) {
		return keybindPrefix + keybindName;
	}

	public String translGui(String guiTranslateKey) {
		return guiPrefix + guiTranslateKey;
	}

	public String translGuiTooltip(String guiTranslateKey) {
		return guiPrefix.substring(0, guiPrefix.length() - 1) + TOOLTIP_SUFFIX + "." + guiTranslateKey;
	}

	@SuppressWarnings("deprecation") // the translator both sides have; tooltips are built on the client anyway
	public String translate(String translateKey, Object... parameters) {
		return I18n.translateToLocalFormatted(translateKey, parameters);
	}

	@SuppressWarnings("deprecation")
	public boolean hasTranslation(String translateKey) {
		return I18n.canTranslate(translateKey);
	}

	/** A translation's lines, each formatted; none when the key has no translation. */
	public List<String> getTranslatedLines(String translateKey, TextFormatting... textFormattings) {
		return getTranslatedLines(translateKey, new Object[0], textFormattings);
	}

	/** As {@link #getTranslatedLines(String, TextFormatting...)}, with the translation's parameters. */
	public List<String> getTranslatedLines(String translateKey, Object[] parameters, TextFormatting... textFormattings) {
		List<String> ret = new ArrayList<>();
		if (!hasTranslation(translateKey)) {
			return ret;
		}
		StringBuilder prefix = new StringBuilder();
		for (TextFormatting formatting : textFormattings) {
			prefix.append(formatting);
		}
		for (String line : translate(translateKey, parameters).split(Pattern.quote(LINE_BREAK))) {
			ret.add(prefix + line);
		}
		return ret;
	}
}
