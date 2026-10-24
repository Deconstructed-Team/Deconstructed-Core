package dev.feldsher.deconstructedcore.client.gui.utils;

public final class Dimension {
	public static final Dimension SQUARE_256 = new Dimension(256, 256);
	public static final Dimension SQUARE_16 = new Dimension(16, 16);
	public static final Dimension SQUARE_12 = new Dimension(12, 12);
	public static final Dimension SQUARE_18 = new Dimension(18, 18);
	public static final Dimension SQUARE_24 = new Dimension(24, 24);
	public static final Dimension RECTANGLE_6_12 = new Dimension(6, 12);
	public static final Dimension RECTANGLE_4_10 = new Dimension(4, 10);
	public static final Dimension EMPTY = new Dimension(0, 0);

	private final int width;
	private final int height;

	public Dimension(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}
}
