package com.carrington.WIA.Math;

/**
 * A container class to hold the results of a resampling operation, which
 * includes the new time points and the corresponding resampled data values.
 */
public class ResampleResult {
	/** The new, resampled array of time points. */
	public final double[] timePoints;
	/** A 2D array where each row represents a resampled data series. */
	public final double[][] values;

	/**
	 * Constructs a ResampleResult object.
	 *
	 * @param timePoints The array of new time points.
	 * @param values The 2D array of resampled y-values.
	 */
	public ResampleResult(double[] timePoints, double[][] values) {
		this.timePoints = timePoints;
		this.values = values;
	}
}
