package com.carrington.WIA.Math;

/**
 * Represents common units for pressure measurements, particularly for blood
 * pressure.
 */
public enum PressureUnit {
	MMHG("mmHg"), PASCALS("Pascals"), NEITHER("Select unit...");

	private String str;

	private PressureUnit(String name) {
		str = name;
	}

	public String toString() {
		return str;
	}
}
