package dev.feldsher.deconstructedcore.renderdata;

/** Where on a storage a tank upgrade is shown. */
public enum TankPosition {
	LEFT("left"), RIGHT("right");

	private final String name;

	TankPosition(String name) {
		this.name = name;
	}

	public String getSerializedName() {
		return name;
	}

	public static TankPosition fromName(String name) {
		return RIGHT.name.equals(name) ? RIGHT : LEFT;
	}
}
