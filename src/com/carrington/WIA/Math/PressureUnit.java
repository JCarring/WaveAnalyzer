package com.carrington.WIA.Math;

/**
 * Represents common units for pressure measurements, particularly for blood
 * pressure.
 */
@SuppressWarnings("javadoc")
public enum PressureUnit {
	MMHG("mmHg"), PASCALS("Pascals"), NEITHER("Select unit...");

	private String str;

	/**
	 * @param name The string representation of the unit.
	 */
	private PressureUnit(String name) {
		str = name;
	}

	/**
	 * Returns the string representation of the pressure unit.
	 *
	 * @return The unit as a string
	 */
	public String toString() {
		return str;
	}
}
