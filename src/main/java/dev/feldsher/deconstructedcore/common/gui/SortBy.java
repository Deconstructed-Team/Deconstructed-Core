package dev.feldsher.deconstructedcore.common.gui;

import java.util.HashMap;
import java.util.Map;

public enum SortBy {
	NAME("name"),
	MOD("mod"),
	COUNT("count"),
	TAGS("tags");

	private static final SortBy[] VALUES = values();
	private static final Map<String, SortBy> NAME_VALUES = new HashMap<>();

	static {
		for (SortBy value : VALUES) {
			NAME_VALUES.put(value.getSerializedName(), value);
		}
	}

	private final String name;

	SortBy(String name) {
		this.name = name;
	}

	public String getSerializedName() {
		return name;
	}

	public SortBy next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	public static SortBy fromName(String name) {
		return NAME_VALUES.getOrDefault(name, NAME);
	}
}
