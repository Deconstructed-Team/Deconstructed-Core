package dev.feldsher.deconstructedcore.settings.main;

/** Whose main settings the main settings tab edits: the player's, for every storage, or this storage's own. */
public enum Context {
	PLAYER(0),
	STORAGE(1);

	private final int id;

	Context(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static Context fromId(int id) {
		return PLAYER.id == id ? PLAYER : STORAGE;
	}
}
