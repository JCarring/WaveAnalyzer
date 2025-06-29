package com.carrington.WIA.GUIs.Components;

/**
 * A simple, immutable class to represent the dimensions (height and width) of a
 * component.
 */
public class JCDimension {

	private final int height;
	private final int width;

	/**
	 * Constructs a JCDimension with specified height and width.
	 * 
	 * @param height the height dimension.
	 * @param width  the width dimension.
	 */
	public JCDimension(int height, int width) {
		this.height = height;
		this.width = width;
	}

	/**
	 * Gets the height.
	 * 
	 * @return the height.
	 */
	public int getHeight() {
		return this.height;
	}

	/**
	 * Gets the width.
	 * 
	 * @return the width.
	 */
	public int getWidth() {
		return this.width;
	}

}
