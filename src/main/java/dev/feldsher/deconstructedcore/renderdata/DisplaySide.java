package dev.feldsher.deconstructedcore.renderdata;

/** The side of a placed storage its displayed items are shown on. */
public enum DisplaySide {
	FRONT("front"), LEFT("left"), RIGHT("right");

	private static final DisplaySide[] VALUES = values();
	private final String name;

	DisplaySide(String name) {
		this.name = name;
	}

	public String getSerializedName() {
		return name;
	}

	public DisplaySide next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	public DisplaySide previous() {
		return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
	}

	public static DisplaySide fromName(String name) {
		for (DisplaySide value : VALUES) {
			if (value.name.equals(name)) {
				return value;
			}
		}
		return FRONT;
	}
}
