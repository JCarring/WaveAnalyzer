package com.carrington.WIA.Cardio;

/**
 * Class representing a QRS complex, with an index indicating the peak location
 * in the source array, as well as the value at the index (the peak value)
 */
public class QRS {
	
	private final int arrayIndex;
	private final double arrayValue;

	/**
	 * Constructs a new QRS complex.
	 *
	 * @param arrayIndex The index of the QRS peak within the source data array.
	 * @param arrayValue The value (e.g., amplitude) at the QRS peak.
	 */
	public QRS(int arrayIndex, double arrayValue) {
		this.arrayIndex = arrayIndex;
		this.arrayValue = arrayValue;
	}
	
	/**
	 * Gets the index of the QRS peak within the source data array.
	 *
	 * @return The index of the QRS peak.
	 */
	public int getArrayIndex() {
		return arrayIndex;
	}
	
	/**
	 * Gets the value (e.g., amplitude) at the QRS peak.
	 *
	 * @return The value at the QRS peak.
	 */
	public double getArrayValue() {
		return arrayValue;
	}
}