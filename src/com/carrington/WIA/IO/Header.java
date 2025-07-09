package com.carrington.WIA.IO;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a data column header, containing its name, column number, primary
 * axis status, and other optional metadata.
 */
public class Header implements Serializable {

	private static final long serialVersionUID = -6505057350436957984L;
	/** Metadata key indicating this {@link Header} is used for alignment. */
	public static final String META_ALIGN = "Align";
	/**
	 * Metadata key indicating color for this {@link Header}; used for graphing
	 * purposes.
	 */
	public static final String META_COLOR = "Color";
	/** Metadata key, used for specifying a display name for this {@link Header} */
	public static final String META_DISPLAY_NAME = "Display_Name";
	/**
	 * Metadata key indicating that this header/column should not have an axis
	 * displayed on a graphs.
	 */
	public static final String META_NO_AXIS = "NO_AXIS";

	private final String name;
	private final int columnNumber;
	private final boolean isPrimaryX;
	private Map<String, Object> additionalMeta = new HashMap<String, Object>();

	/**
	 * Generate a new header
	 * 
	 * @param name         Name of the header
	 * @param columnNumber Column number, CANNOT be less than zero
	 * @param isPrimaryX   If this is a domain header
	 * @throws NullPointerException if name is blank or columnNumber < 0
	 */
	public Header(String name, int columnNumber, boolean isPrimaryX) {
		if (name == null || name.isBlank() || columnNumber < 0)
			throw new NullPointerException("Some header data was null");
		this.columnNumber = columnNumber;
		this.name = name;
		this.isPrimaryX = isPrimaryX;

	}

	/**
	 * Gets the column number (0-based index).
	 * 
	 * @return The column number.
	 */
	public int getCol() {
		return this.columnNumber;
	}

	/**
	 * Gets the name of the header.
	 * 
	 * @return The header name string.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Checks if this header represents the primary X-axis (domain).
	 * 
	 * @return true if this is the primary X-axis header, otherwise false.
	 */
	public boolean isX() {
		return this.isPrimaryX;
	}

	/**
	 * Adds a piece of metadata to the header.
	 * 
	 * @param s   The metadata key.
	 * @param obj The metadata value.
	 */
	public void addAdditionalMeta(String s, Object obj) {
		if (s == null)
			return;

		additionalMeta.put(s, obj);
	}

	/**
	 * Checks if the header contains a specific piece of metadata.
	 * 
	 * @param s The metadata key.
	 * @return true if the metadata key exists, otherwise false.
	 */
	public boolean hasAdditionalMeta(String s) {
		return additionalMeta.containsKey(s);
	}

	/**
	 * Retrieves a piece of metadata from the header.
	 * 
	 * @param s The metadata key.
	 * @return The metadata value object, or null if the key doesn't exist.
	 */
	public Object getAdditionalMeta(String s) {
		return additionalMeta.get(s);
	}

	/**
	 * Returns a string representation of the header, in the format of "[name]
	 * (column [number])"
	 * 
	 * @return A formatted string.
	 */
	@Override
	public String toString() {
		return this.name + " (column " + this.columnNumber + ")";
	}

	/**
	 * Compares this header to another object for equality. Equality is based on a
	 * case-insensitive comparison of header names only
	 * 
	 * @param other The object to compare with.
	 * @return true if the objects are equal, otherwise false.
	 */
	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		try {
			Header header = (Header) other;
			return this.name.equalsIgnoreCase(header.name);
		} catch (Exception e) {
			return false;
		}

	}

	/**
	 * Generates a hash code for the header, based on its String name.
	 * 
	 * @return The hash code.
	 */
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

}
