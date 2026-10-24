package dev.feldsher.deconstructedcore.upgrades;

/** What a filter matches first. TAGS are 1.12's ore dictionary names. */
public enum PrimaryMatch {
	ITEM("item"),
	MOD("mod"),
	TAGS("tags");

	private static final PrimaryMatch[] VALUES = values();
	private final String name;

	PrimaryMatch(String name) {
		this.name = name;
	}

	public String getSerializedName() {
		return name;
	}

	public PrimaryMatch next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	public static PrimaryMatch fromName(String name) {
		for (PrimaryMatch value : VALUES) {
			if (value.name.equals(name)) {
				return value;
			}
		}
		return ITEM;
	}
}
