package dev.feldsher.deconstructedcore.upgrades.filter;

public enum Direction {
	BOTH("both"),
	INPUT("input"),
	OUTPUT("output");

	private static final Direction[] VALUES = values();
	private final String name;

	Direction(String name) {
		this.name = name;
	}

	public String getSerializedName() {
		return name;
	}

	public Direction next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	public static Direction fromName(String name) {
		for (Direction value : VALUES) {
			if (value.name.equals(name)) {
				return value;
			}
		}
		return BOTH;
	}
}
