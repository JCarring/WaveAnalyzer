package com.carrington.WIA.Math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.exception.MathIllegalArgumentException;

import com.carrington.WIA.Utils;
import com.carrington.WIA.GUIs.BackgroundProgressRecorder;
import com.carrington.WIA.IO.Header;

/**
 * Provides static methods for resampling one-dimensional data series,
 * particularly for time-series data in hemodynamic analysis. It supports both
 * spline interpolation for continuous data and a nearest-neighbor approach for
 * binary-like data.
 */
public abstract class DataResampler {

	/**
	 * Resamples a single data array to the specified number of samples using cubic
	 * spline interpolation. Assumes that the original data is sampled at equal time
	 * intervals (e.g., x = 0, 1, 2, ...).
	 *
	 * @param data            the original data array
	 * @param numberOfSamples the desired number of samples in the output
	 * @return a new data array of length numberOfSamples
	 * @throws MathIllegalArgumentException if spline interpolation fails
	 */
	public static double[] resample(double[] data, int numberOfSamples) throws MathIllegalArgumentException {
		if (data == null || data.length < 2) {
			throw new IllegalArgumentException("Data array must contain at least two points.");
		}
		if (numberOfSamples < 2) {
			throw new IllegalArgumentException("numberOfSamples must be at least 2.");
		}

		// Generate xData assuming unit spacing.
		double[] xData = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			xData[i] = i;
		}

		// Create the spline interpolator.
		SplineInterpolator interpolator = new SplineInterpolator();
		UnivariateFunction spline = interpolator.interpolate(xData, data);

		// Prepare the output array.
		double[] output = new double[numberOfSamples];
		double xMin = xData[0];
		double xMax = xData[xData.length - 1];
		double step = (xMax - xMin) / (numberOfSamples - 1);

		// Evaluate the spline at equally spaced new x-values.
		for (int i = 0; i < numberOfSamples; i++) {
			double x;
			if (i == numberOfSamples - 1) {
				// Use xMax - epsilon on the last point to stay within the allowed domain.
				x = xMax - 1e-8;
			} else {
				x = xMin + i * step;
			}
			output[i] = spline.value(x);
		}

		return output;
	}

	/**
	 * Resamples data using the provided sample rate and xData. For continuous
	 * series a cubic spline is used; for series that have one or two unique values,
	 * a nearest-neighbor approach is applied.
	 *
	 * @param resampleRate the sample rate
	 * @param shiftToZero  if true, the new x-values will start at 0.
	 * @param xData        an array of x (time) values in ascending order
	 * @param yValues      a varargs parameter for one or more y-value arrays
	 * @return a ResampleResult containing the new time points and resampled y
	 *         values
	 * @throws ResampleException if an error occurs during resampling
	 */
	public static ResampleResult resample(double resampleRate, boolean shiftToZero,
			BackgroundProgressRecorder progressRecorder, double[] xData, double[]... yValues) throws ResampleException {
		return doResample(resampleRate, xData, yValues, shiftToZero, progressRecorder);
	}

	/**
	 * Resamples data that is provided in a LinkedHashMap with Headers. This method
	 * converts the map into arrays, calls the common resample logic, and then
	 * reassembles the result back into a LinkedHashMap with the original headers.
	 *
	 * @param yValuesMap   a LinkedHashMap where each key is a Header and each value
	 *                     is an array of y values
	 * @param xHeader      the header corresponding to the x (time) values
	 * @param xData        the array of x (time) values (must be in ascending order)
	 * @param resampleRate the sample rate
	 * @param shiftToZero  if true, the new x-values will start at 0.
	 * @return a LinkedHashMap where the xHeader maps to the new time points and the
	 *         other Headers map to the resampled y series
	 * @throws ResampleException if an error occurs during resampling
	 */
	public static LinkedHashMap<Header, double[]> resample(LinkedHashMap<Header, double[]> yValuesMap, Header xHeader,
			double[] xData, double resampleRate, boolean shiftToZero) throws ResampleException {

		// Convert the map to arrays
		int numSeries = yValuesMap.size();
		double[][] yValues = new double[numSeries][];
		List<Header> headers = new ArrayList<>(yValuesMap.keySet());
		for (int i = 0; i < numSeries; i++) {
			yValues[i] = yValuesMap.get(headers.get(i));
		}

		// Perform the resampling using the common method.
		ResampleResult result = doResample(resampleRate, xData, yValues, shiftToZero, null);

		// Reassemble the result into a LinkedHashMap
		LinkedHashMap<Header, double[]> resampledMap = new LinkedHashMap<>();
		resampledMap.put(xHeader, result.timePoints);
		for (int i = 0; i < headers.size(); i++) {
			resampledMap.put(headers.get(i), result.values[i]);
		}
		return resampledMap;
	}

	/**
	 * The core resampling logic shared by both public methods.
	 *
	 * @param resampleRate     the sample rate
	 * @param xData            the original x (time) data, assumed to be in
	 *                         ascending order
	 * @param yValues          a 2D array of y values; each row is a data series
	 * @param shiftToZero      if true, xData is shifted so that the first value
	 *                         becomes zero.
	 * @param progressRecorder An object to record background progress (can be
	 *                         null).
	 * @return a ResampleResult containing the new time points and resampled y
	 *         values
	 * @throws ResampleException if an error occurs during resampling
	 */
	private static ResampleResult doResample(double resampleRate, double[] xData, double[][] yValues,
			boolean shiftToZero, BackgroundProgressRecorder progressRecorder) throws ResampleException {

		if (xData == null)
			throw new ResampleException("xData cannot be null.");
		if (!isAscending(xData))
			throw new ResampleException("xData must be in ascending order.");
		if (!sameSize(yValues, xData))
			throw new ResampleException("Each y array must have the same length as xData.");
		
		if (progressRecorder != null) {
			progressRecorder.setProgressBarEnabled(true, 1, 100);
		}
		// Shift xData if requested.
		double offset = shiftToZero ? xData[0] : 0.0;
		double[] xDataAdjusted = new double[xData.length];
		for (int i = 0; i < xData.length; i++) {
			xDataAdjusted[i] = xData[i] - offset;
		}

		// Determine new x sample points based on the adjusted data.
		BigDecimal lstart = BigDecimal.valueOf(Utils.getNearestMultipleAbove(xDataAdjusted[0], resampleRate));
		double lend = xDataAdjusted[xDataAdjusted.length - 1];
		int numberOfResamples = (int) BigDecimal.valueOf(lend).subtract(lstart, MathContext.DECIMAL128)
				.divide(BigDecimal.valueOf(resampleRate)).setScale(0, RoundingMode.FLOOR).doubleValue() + 1;

		double[] newXVals = new double[numberOfResamples];
		BigDecimal currentX = lstart;
		BigDecimal sampleRateBD = BigDecimal.valueOf(resampleRate);
		for (int i = 0; i < numberOfResamples; i++) {
			newXVals[i] = currentX.doubleValue();
			currentX = currentX.add(sampleRateBD);
		}

		double[][] newYVals = new double[yValues.length][numberOfResamples];

		// Identify which series are "binary" (only one or two distinct values).
		boolean[] isBinaryArray = new boolean[yValues.length];
		for (int i = 0; i < yValues.length; i++) {
			isBinaryArray[i] = isBinary(yValues[i]);
		}

		// For continuous series, create spline interpolators using the adjusted xData.
		SplineInterpolator interpolator = new SplineInterpolator();
		UnivariateFunction[] splineFunctions = new UnivariateFunction[yValues.length];
		for (int i = 0; i < yValues.length; i++) {
			if (!isBinaryArray[i]) {
				try {
					splineFunctions[i] = interpolator.interpolate(xDataAdjusted, yValues[i]);
				} catch (MathIllegalArgumentException e) {
					throw new ResampleException("Error creating spline for series " + i + ": " + e.getMessage());
				}
			}
		}

		// Processing each set of Y values
		for (int j = 0; j < yValues.length; j++) {

			if (isBinaryArray[j]) {
				// For binary arrays, find the closest new X for each original non-zero point.

				// start wtih all zeros
				for (int i = 0; i < numberOfResamples; i++) {
					newYVals[j][i] = 0.0;
				}

				// place the original non-zero values at the closest new time point.
				for (int k = 0; k < yValues[j].length; k++) {
					if (yValues[j][k] > 0.00001) { // detected non-zero (active) binary state
						double origX = xDataAdjusted[k]; // Use adjusted X for correct mapping
						int idx = Utils.getClosestIndex(origX, newXVals);
						if (idx >= 0 && idx < newYVals[j].length) {
							newYVals[j][idx] = yValues[j][k]; // assign the binary value
						}
					}
				}

			} else {
				// For continuous (non-binary) arrays, use spline interpolation.
				for (int i = 0; i < numberOfResamples; i++) {
					newYVals[j][i] = splineFunctions[j].value(newXVals[i]);
				}
			}

			// Update progress after each series is processed.
			if (progressRecorder != null) {
				int progress = (int) Math.round(((double) (j + 1) / yValues.length) * 100.0);
				progressRecorder.setProgressBarProgress(progress);
			}
		}
		
		if (progressRecorder != null) {
			progressRecorder.setProgressBarEnabled(false, -1, -1);
		}

		return new ResampleResult(newXVals, newYVals);
	}

	// Helper: ensures that each row in yValues has the same length as xData.
	private static boolean sameSize(double[][] yValues, double[] xData) {
		for (double[] y : yValues) {
			if (y.length != xData.length)
				return false;
		}
		return true;
	}

	// Helper: checks if an array is "binary", meaning it contains one or two
	// distinct values.
	private static boolean isBinary(double[] array) {
		Set<Double> uniqueValues = new HashSet<>();
		for (double v : array) {
			uniqueValues.add(v);
			if (uniqueValues.size() > 2)
				return false;
		}
		return true;
	}

	/**
	 * Checks if an array of doubles is sorted in ascending order.
	 *
	 * @param list The array to check.
	 * @return True if the array is ascending, false otherwise.
	 */
	private static boolean isAscending(double[] list) {
		return ArrayUtils.isSorted(list);
	}

	/**
	 * Calculates the appropriate resample frequency based on the string, such that
	 * it can't be invalid or too big based on the passed arrays
	 *
	 * @param sampleFreq The user-supplied desired sample frequency as a string.
	 * @param lists      The data lists to analyze for determining the longest
	 *                   frequency.
	 * @return The resample frequency
	 */
	public static double calculateReSampleFrequency(String sampleFreq, double[]... lists) {

		double suppliedResFreq;
		try {
			suppliedResFreq = Double.valueOf(sampleFreq);

		} catch (NumberFormatException e) {
			throw new ResampleException("Sample frequency is invalid - not a number");
		}

		if (suppliedResFreq <= 0) {
			throw new ResampleException("Sample frequency is invalid - not greater than zero");
		}
		double minimumDiff = Utils.getMinimumRange(lists);

		if (suppliedResFreq >= minimumDiff) {
			throw new ResampleException("Sample frequency is larger than range");
		}

		return suppliedResFreq;

	}

	/**
	 * Custom exception for errors occurring during the data resampling process.
	 */
	public static class ResampleException extends RuntimeException {

		private static final long serialVersionUID = 5907837010601024697L;

		/**
		 * Constructs a ResampleException with the specified detail message.
		 *
		 * @param msg The detail message.
		 */
		public ResampleException(String msg) {
			super(msg);
		}

	}

}
