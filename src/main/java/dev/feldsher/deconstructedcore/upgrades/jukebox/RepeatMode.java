package dev.feldsher.deconstructedcore.upgrades.jukebox;

import net.minecraft.util.IStringSerializable;

import java.util.HashMap;
import java.util.Map;

public enum RepeatMode implements IStringSerializable {
	ALL("all"),
	ONE("one"),
	NO("no");

	private static final Map<String, RepeatMode> NAME_VALUES = new HashMap<>();
	private static final RepeatMode[] VALUES = values();

	static {
		for (RepeatMode value : VALUES) {
			NAME_VALUES.put(value.getName(), value);
		}
	}

	private final String name;

	RepeatMode(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public RepeatMode next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	public static RepeatMode fromName(String name) {
		return NAME_VALUES.getOrDefault(name, NO);
	}
}
