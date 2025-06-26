package com.carrington.WIA.Math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.exception.MathIllegalArgumentException;

import com.carrington.WIA.Utils;
import com.carrington.WIA.IO.Header;

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
		         //Use xMax - epsilon on the last point to stay within the allowed domain.
		        x = xMax - 1e-8;
		    } else {
		        x = xMin + i * step;
		    }
		    output[i] = spline.value(x);
		}

		return output;
	}

	private static int roundDown(double d) {
		return (int) d;
	}

	@SuppressWarnings("unused")
	private static int roundUp(double d) {
		return roundDown(d) + 1;
	}

	public static void main(String[] args) {
		Header headerX = new Header("name", 2, true);
		Header headerY = new Header("values", 2, true);
		Header headerY2 = new Header("binary", 2, true);

		double x[] = new double[] { 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
		double y[] = new double[] { 2, 6, 7, 8, 10, 12, 14, 16, 18, 20 };
		double y2[] = new double[] { 0, 0, 0, 0, 0, 0, 1, 0, 0, 0 };
		
		

		for (double d : resample(y, 19)) {
			System.out.println(d);
		}

		for (double d : x) {
			System.out.println(d);

		}

		for (double d : y) {
			System.out.println(d);

		}
		LinkedHashMap<Header, double[]> yValues = new LinkedHashMap<Header, double[]>();
		yValues.put(headerY, y);
		yValues.put(headerY2, y2);

		try {
			LinkedHashMap<Header, double[]> newValues = resample(yValues, headerX, x, 0.5, true);
			for (Entry<Header, double[]> en : newValues.entrySet()) {
				System.out.println(en.getKey().getName());
				for (double d : en.getValue()) {
					System.out.println(d);
				}
			}

			ResampleResult rr = resample(0.5, true, x, new double[][] { y, y2 });
			for (double[] en : rr.values) {
				System.out.println("NEXT");
				for (double d : en) {
					System.out.println(d);
				}
			}
		} catch (ResampleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
	public static ResampleResult resample(double resampleRate, boolean shiftToZero, double[] xData, double[]... yValues)
			throws ResampleException {
		return doResample(resampleRate, xData, yValues, shiftToZero);
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
		ResampleResult result = doResample(resampleRate, xData, yValues, shiftToZero);

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
	 * @param resampleRate the sample rate
	 * @param xData        the original x (time) data, assumed to be in ascending
	 *                     order
	 * @param yValues      a 2D array of y values; each row is a data series
	 * @param shiftToZero  if true, xData is shifted so that the first value becomes
	 *                     zero.
	 * @return a ResampleResult containing the new time points and resampled y
	 *         values
	 * @throws ResampleException if an error occurs during resampling
	 */
	private static ResampleResult doResample(double resampleRate, double[] xData, double[][] yValues,
			boolean shiftToZero) throws ResampleException {

		if (xData == null)
			throw new ResampleException("xData cannot be null.");
		if (!isAscending(xData))
			throw new ResampleException("xData must be in ascending order.");
		if (!sameSize(yValues, xData))
			throw new ResampleException("Each y array must have the same length as xData.");

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

		BigDecimal sampleRateBD = BigDecimal.valueOf(resampleRate);
		// Loop over new x values and compute resampled y values, for arrays that are NOT binary
		for (int i = 0; i < numberOfResamples; i++) {
			double newX = lstart.doubleValue();
			newXVals[i] = newX;
			for (int j = 0; j < yValues.length; j++) {
				if (!isBinaryArray[j]) {
					newYVals[j][i] = splineFunctions[j].value(newX);
				} else {
					newYVals[j][i] = 0; // will have non-zero binary values applied later
				}
			}
			lstart = lstart.add(sampleRateBD);
		}
		
		// Loop over new x values and compute resampled y values, for arrays that ARE BINARY
		for (int j = 0; j < yValues.length; j++) {
		    if (isBinaryArray[j]) {
		        // For each original data point in the binary array
		        for (int k = 0; k < yValues[j].length; k++) {
		            if (yValues[j][k] > 0.00001) {  // detected non-zero (active) binary state
		                double origX = xData[k];
		                // Determine the closest new sample index for this original x value.
		                int idx = Utils.getClosestIndex(origX, newXVals);
		                newYVals[j][idx] = yValues[j][k]; // assign the binary value (typically 1)
		            }
		        }
		    }
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


	private static boolean isAscending(double[] list) {
		return ArrayUtils.isSorted(list);
	}

	public static int numberOfDecimals(String str) {
		if (!str.contains("."))
			return 0;

		str = Utils.removeTrailingZeros(str);
		return str.length() - str.indexOf('.') - 1;

	}

	@SafeVarargs
	public static double _minimumAverageDifference(double[]... lists) {

		double minAvgDiff = Double.NaN;
		for (double[] list : lists) {
			int size = list.length;
			List<Double> differences = new ArrayList<Double>();
			for (int i = 0; i < (size - 1); i++) {
				differences.add(Math.abs(list[i + 1] - list[i]));
			}

			double sum = 0;
			for (double d : differences) {
				sum = sum + d;
			}

			double difference = sum / ((double) differences.size());
			if (Double.isNaN(minAvgDiff)) {
				minAvgDiff = difference;
			} else if (difference < minAvgDiff) {
				minAvgDiff = difference;
			}

		}

		return minAvgDiff;

	}

	@SafeVarargs
	public static double calculateReSampleFrequency(String sampleFreq, int overSampleLvl, double[]... lists) {
		double suppliedResReq = Double.valueOf(sampleFreq);

		if (overSampleLvl <= 0) {
			return suppliedResReq;
		}

		double longestResFreq = _minimumAverageDifference(lists) / (double) overSampleLvl;

		return Math.min(longestResFreq, suppliedResReq);

	}

	public static class ResampleException extends RuntimeException {

		private static final long serialVersionUID = 5907837010601024697L;

		public ResampleException(String msg) {
			super(msg);
		}

	}

}
