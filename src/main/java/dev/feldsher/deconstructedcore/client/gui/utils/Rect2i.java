package dev.feldsher.deconstructedcore.client.gui.utils;

/** A rectangle on the screen, as 1.21's Rect2i. */
public final class Rect2i {
	private final int x;
	private final int y;
	private final int width;
	private final int height;

	public Rect2i(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public boolean contains(double pointX, double pointY) {
		return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
	}
}
