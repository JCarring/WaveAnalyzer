package com.carrington.WIA.Math;

/**
 * Represents common units for flow velocity measurements, particularly for
 * coronary blood flow.
 */
public enum FlowUnit {
	MPS("m/s"), CPS("cm/s"), NEITHER("Select unit...");

	private String str;

	private FlowUnit(String name) {
		str = name;
	}

	public String toString() {
		return str;
	}
}