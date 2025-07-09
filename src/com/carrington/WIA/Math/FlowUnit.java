package com.carrington.WIA.Math;

/**
 * Represents common units for flow velocity measurements, particularly for
 * coronary blood flow.
 */
@SuppressWarnings("javadoc")
public enum FlowUnit {
	MPS("m/s"), 
	CPS("cm/s"), NEITHER("Select unit...");

	private String str;

	/**
	 * @param name The string representation of the unit.
	 */
	private FlowUnit(String name) {
		str = name;
	}

	/**
	 * Returns the string representation of the flow unit.
	 *
	 * @return The unit as a string
	 */
	public String toString() {
		return str;
	}
}