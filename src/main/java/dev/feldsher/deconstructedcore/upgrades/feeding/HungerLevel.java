package dev.feldsher.deconstructedcore.upgrades.feeding;

import java.util.HashMap;
import java.util.Map;

/** How hungry the player has to be to be fed a food: at all, by half its hunger points, or by all of them. */
public enum HungerLevel {
	ANY("any"),
	HALF("half"),
	FULL("full");

	private static final Map<String, HungerLevel> NAME_VALUES = new HashMap<>();
	private static final HungerLevel[] VALUES = values();

	static {
		for (HungerLevel value : VALUES) {
			NAME_VALUES.put(value.getSerializedName(), value);
		}
	}

	private final String name;

	HungerLevel(String name) {
		this.name = name;
	}

	public String getSerializedName() {
		return name;
	}

	public HungerLevel next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	public static HungerLevel fromName(String name) {
		return NAME_VALUES.getOrDefault(name, HALF);
	}
}
