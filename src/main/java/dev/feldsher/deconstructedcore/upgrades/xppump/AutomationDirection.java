package dev.feldsher.deconstructedcore.upgrades.xppump;

import net.minecraft.util.IStringSerializable;

import java.util.HashMap;
import java.util.Map;

public enum AutomationDirection implements IStringSerializable {
	INPUT("input"),
	OUTPUT("output"),
	KEEP("keep"),
	OFF("off");

	private static final Map<String, AutomationDirection> NAME_VALUES = new HashMap<>();
	private static final AutomationDirection[] VALUES = values();

	static {
		for (AutomationDirection value : VALUES) {
			NAME_VALUES.put(value.getName(), value);
		}
	}

	private final String name;

	AutomationDirection(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public AutomationDirection next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	public static AutomationDirection fromName(String name) {
		return NAME_VALUES.getOrDefault(name, INPUT);
	}
}
