package com.carrington.WIA.Cardio;

import java.util.ArrayList;
import java.util.List;

import com.carrington.WIA.Utils;

/**
 * Detects QRS complexes
 */
public abstract class QRSDetector {

	/**
	 * Given input time and amplitude arrays for the ECG strip as well as indices to
	 * start and stop analyzing, will supply a list of QRS complexes using moving
	 * integration of the squared differential
	 * 
	 * @param time                time array
	 * @param amplitude           ECG amplitude array
	 * @param start               index of the input arrays to end
	 * @param end                 index of the input arrays to start
	 * @param hz                  ECG sample rate
	 * @param adjustTo100msBefore Adjusts the supplied index of QRS to 100 ms
	 *                            before. This is useful if for instance you are
	 *                            detecting beats and want the beat to include a
	 *                            standard (100 ms) amount of time before the QRS,
	 *                            to include some of the flow or pressure data
	 *                            before QRS for instance.
	 * @return List of {@link QRS} complexes identified.
	 */
	public static List<QRS> getQRSOnSubset(double[] time, double[] amplitude, int start, int end, int hz,
			boolean adjustTo100msBefore) {

		double[] subarrayAmpl = copySubarray(amplitude, start, end);

		List<QRS> qrsComplexes = new ArrayList<QRS>();
		List<Integer> qrsIndices = getQRSIndices(subarrayAmpl, hz, adjustTo100msBefore);
		for (Integer i : qrsIndices) {
			qrsComplexes.add(new QRS(i + start, time[i + start]));
		}
		return qrsComplexes;

	}

	/**
	 * Given input time and amplitude arrays for the ECG strip, will supply a list
	 * of QRS complexes using moving integration of the squared differential
	 * 
	 * @param time                time array
	 * @param amplitude           ECG amplitude array
	 * @param hz                  ECG sample rate
	 * @param adjustTo100msBefore Adjusts the supplied index of QRS to 100 ms
	 *                            before. This is useful if for instance you are
	 *                            detecting beats and want the beat to include a
	 *                            standard (100 ms) amount of time before the QRS,
	 *                            to include some of the flow or pressure data
	 *                            before QRS for instance.
	 * @return List of {@link QRS} complexes identified.
	 */
	public static List<QRS> getQRS(double[] time, double[] amplitude, int hz, boolean adjustTo100msBefore) {

		List<QRS> qrsComplexes = new ArrayList<QRS>();
		List<Integer> qrsIndices = getQRSIndices(amplitude, hz, adjustTo100msBefore);
		for (Integer i : qrsIndices) {
			qrsComplexes.add(new QRS(i, time[i]));
		}
		return qrsComplexes;

	}

	/**
	 * Helper method. gets the index of all QRS copmlexes. This is the workhorse
	 * method. Differentiates, squares, integrates.
	 */
	private static List<Integer> getQRSIndices(double[] input, int hz, boolean adjustTo100msBefore) {

		// Differentiate the signal
		double[] differentiatedSignal = differentiate(input);

		// Square the signal
		double[] squaredSignal = square(differentiatedSignal);

		// Apply moving window integration
		double[] integratedSignal = movingWindowIntegration(squaredSignal, (int) ((double) hz / 6.0));

		// Automatically detect threshold
		double threshold = detectThreshold(integratedSignal);

		// Detect QRS complexes
		return detectQRSComplexes(integratedSignal, input, threshold, hz, adjustTo100msBefore);
	}

	/**
	 * Helper method, calculates the differential.
	 * 
	 * @return differential of input. Output array is same length as input, and
	 *         first elemtn is zero.
	 */
	private static double[] differentiate(double[] signal) {
		double[] differentiatedSignal = new double[signal.length];
		for (int i = 1; i < signal.length; i++) {
			differentiatedSignal[i] = signal[i] - signal[i - 1];
		}
		return differentiatedSignal;
	}

	/**
	 * Helper method, calculates the square.
	 * 
	 * @return square of input.
	 */
	private static double[] square(double[] signal) {
		double[] squaredSignal = new double[signal.length];
		for (int i = 0; i < signal.length; i++) {
			squaredSignal[i] = signal[i] * signal[i];
		}
		return squaredSignal;
	}

	/**
	 * Helper method. Performs moving window integration on the supplied input
	 * 
	 * @param signal     input to evaluate
	 * @param windowSize the window size to use
	 * @return
	 */
	private static double[] movingWindowIntegration(double[] signal, int windowSize) {
		double[] integratedSignal = new double[signal.length];
		for (int i = 0; i < signal.length; i++) {
			double sum = 0;
			for (int j = 0; j < windowSize; j++) {
				if (i - j >= 0) {
					sum += signal[i - j];
				}
			}
			integratedSignal[i] = sum / windowSize;
		}
		return integratedSignal;
	}

	/**
	 * Helper method. Finds an adequate threshold for the input signal. It does this
	 * by calculating the mean and standard deviation, then adding one standard
	 * deviation to half of the mean.
	 * 
	 * @param signal input
	 * @return threshold
	 */
	private static double detectThreshold(double[] signal) {
		double mean = 0;
		for (double value : signal) {
			mean += value;
		}
		mean /= signal.length;

		double stdDev = 0;
		for (double value : signal) {
			stdDev += Math.pow(value - mean, 2);
		}
		stdDev = Math.sqrt(stdDev / signal.length);

		return mean + 0.5 * stdDev;
	}

	/**
	 * Helper method. Finds array indices of QRS complex based on integrated signal.
	 * The methods {@link #detectThreshold(double[])},
	 * {@link #movingWindowIntegration(double[], int)},
	 * {@link #differentiate(double[])}, and {@link #square(double[])} should have
	 * been called already.
	 */
	public static ArrayList<Integer> detectQRSComplexes(double[] integratedSignal, double[] originalSignal,
			double threshold, int hz, boolean adjustTo100msBefore) {
		ArrayList<Integer> qrsComplexes = new ArrayList<>();
		int addition = (int) (((double) hz) / 4.0);
		for (int i = 0; i < integratedSignal.length; i++) {
			if (integratedSignal[i] > threshold) {
				int startIndex;

				if (adjustTo100msBefore) {
					startIndex = adjustTimeFromPeak(centerPeak(originalSignal, i, hz), hz);
				} else {
					startIndex = findStartOfQRS(originalSignal, centerPeak(originalSignal, i, hz));
				}
				qrsComplexes.add(startIndex);
				i += (addition); // Sk

			}

		}
		return qrsComplexes;
	}

	/**
	 * Helper method. Centers the peak. Looks 100 ms before and after and finds the
	 * maximum peak.
	 */
	private static int centerPeak(double[] signal, int peakIndex, int hz) {

		int numIndex = (int) (0.1 * ((double) hz));
		int start = Math.max(0, peakIndex - numIndex);
		int end = Math.min(signal.length - 1, peakIndex + numIndex);

		return Utils.getIndexOfMax(signal, start, end);

	}

	/**
	 * Helper method. Given the index of the peak, and sample rate, will move the
	 * output QRS index 100 ms before the actually detect QRS peak.
	 * 
	 * @param peakIndex The index of the peak
	 * @param hz        The sample rate
	 * @return the adjust index
	 */
	private static int adjustTimeFromPeak(int peakIndex, int hz) {
		int numIndex = (int) (0.1 * ((double) hz));
		int start = Math.max(0, peakIndex - numIndex);
		return start;

	}

	/**
	 * @param signal    input signal
	 * @param peakIndex the peak index for the QRS complex
	 * @return the index of the start of the QRS (where starts going positive, so
	 *         start of the R basically)
	 */
	public static int findStartOfQRS(double[] signal, int peakIndex) {
		Boolean positive = null;
		int foundIndex = -1;
		int i;
		for (i = peakIndex; i > 0; i--) {
			double diff = signal[i] - signal[i - 1];
			if (positive == null) {
				positive = diff >= 0 ? true : false;
				continue;
			} else {

				if (positive) {

					if (diff >= 0) {
						foundIndex = -1;
					} else if (foundIndex == -1) {
						foundIndex = i;
					} else {
						return foundIndex;
					}

				} else {
					if (diff <= 0) {
						foundIndex = -1;
					} else if (foundIndex == -1) {
						foundIndex = i;
					} else {
						return foundIndex;
					}
				}

			}
		}
		return i;
	}

	

	/**
	 * Helper method used to make an array copy
	 * 
	 */
	private static double[] copySubarray(double[] originalArray, int start, int end) {
		// Validate the input range
		if (start < 0 || end > originalArray.length || start > end) {
			throw new IllegalArgumentException("Invalid range specified");
		}

		double[] subarray = new double[end - start];
		System.arraycopy(originalArray, start, subarray, 0, end - start);

		return subarray;
	}
}
