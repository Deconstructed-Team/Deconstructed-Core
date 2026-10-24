package dev.feldsher.deconstructedcore.upgrades;

public enum ContentsFilterType {
	ALLOW("allow"),
	BLOCK("block"),
	STORAGE("storage");

	private static final ContentsFilterType[] VALUES = values();
	private final String name;

	ContentsFilterType(String name) {
		this.name = name;
	}

	public String getSerializedName() {
		return name;
	}

	public ContentsFilterType next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	public static ContentsFilterType fromName(String name) {
		for (ContentsFilterType value : VALUES) {
			if (value.name.equals(name)) {
				return value;
			}
		}
		return BLOCK;
	}
}
