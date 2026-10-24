package dev.feldsher.deconstructedcore.settings;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Optional;
import java.util.function.BiFunction;

/** A setting of the main settings tab: one value, kept by the player and, where it differs, by the storage. */
public class MainSetting<T> {
	private final String tagName;
	private final BiFunction<NBTTagCompound, String, Optional<T>> getValue;
	private final ValueSetter<T> setValue;
	private final T defaultValue;

	public MainSetting(String tagName, BiFunction<NBTTagCompound, String, Optional<T>> getValue, ValueSetter<T> setValue, T defaultValue) {
		this.tagName = tagName;
		this.getValue = getValue;
		this.setValue = setValue;
		this.defaultValue = defaultValue;
	}

	public static MainSetting<Boolean> ofBoolean(String tagName, boolean defaultValue) {
		return new MainSetting<>(tagName, (tag, key) -> tag.hasKey(key) ? Optional.of(tag.getBoolean(key)) : Optional.empty(), NBTTagCompound::setBoolean, defaultValue);
	}

	public static MainSetting<String> ofString(String tagName, String defaultValue) {
		return new MainSetting<>(tagName, (tag, key) -> tag.hasKey(key) ? Optional.of(tag.getString(key)) : Optional.empty(), NBTTagCompound::setString, defaultValue);
	}

	public String getName() {
		return tagName;
	}

	public void setValue(NBTTagCompound tag, T value) {
		setValue.set(tag, tagName, value);
	}

	public void removeFrom(NBTTagCompound tag) {
		tag.removeTag(tagName);
	}

	public Optional<T> getValue(NBTTagCompound tag) {
		return getValue.apply(tag, tagName);
	}

	public T getDefaultValue() {
		return defaultValue;
	}

	public interface ValueSetter<T> {
		void set(NBTTagCompound tag, String key, T value);
	}
}
