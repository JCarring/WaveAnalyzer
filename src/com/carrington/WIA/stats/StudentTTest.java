package com.carrington.WIA.stats;

import org.apache.commons.math3.distribution.TDistribution;

/**
 * Performs a Student's t-test to compare the means of two independent samples.
 * This implementation assumes equal variances between samples.
 */
public abstract class StudentTTest {

	/**
	 * Performs a Student's T-test and returns the p-value.
	 * 
	 * @param sample1   The first data sample.
	 * @param sample2   The second data sample.
	 * @param twoTailed If true, a two-tailed test is performed; otherwise, a
	 *                  one-tailed test is performed.
	 * @return The calculated p-value, or Double.NaN if samples are too small. A
	 *         value of 2.2e-16 is returned for p-values of 0.
	 */
	public static double calculatePValue(double[] sample1, double[] sample2, boolean twoTailed) {

		if (sample1.length < 2 || sample2.length < 2) {
			return Double.NaN;
		}
		try {
			double tStatistic = calculateTStatistic(sample1, sample2);
			int degreesOfFreedom = calculateDegreesOfFreedom(sample1, sample2);
			if (Double.isNaN(tStatistic)) {
				return 1.0;
			}
			TDistribution tDist = new TDistribution(degreesOfFreedom);
			if (twoTailed) {
				// Two-tailed test
				float pValue = (float) (2.0 * (1.0 - tDist.cumulativeProbability(Math.abs(tStatistic))));
				if (pValue == 0) {
					return 2.2e-16;
				} else {
					return pValue;

				}
			} else {

				// Return the minimum of the two p-values
				double pValue = 1.0 - tDist.cumulativeProbability(Math.abs(tStatistic));
				if (pValue == 0) {
					return 2.2e-16;
				} else {
					return pValue;

				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unable to perform (samples to small or contain all zeros)");
			return Double.NaN;
		}

	}

	/**
	 * Performs a Student's T-test and returns the p-value as a formatted string.
	 * 
	 * @param sample1   The first data sample.
	 * @param sample2   The second data sample.
	 * @param twoTailed If true, a two-tailed test is performed; otherwise, a
	 *                  one-tailed test is performed.
	 * @return A string representation of the p-value.
	 */
	public static String perform(double[] sample1, double[] sample2, boolean twoTailed) {

		if (sample1.length < 2 || sample2.length < 2) {
			return "1.0";
		}
		try {
			double tStatistic = calculateTStatistic(sample1, sample2);
			int degreesOfFreedom = calculateDegreesOfFreedom(sample1, sample2);
			if (Double.isNaN(tStatistic)) {
				return "1.0";
			}
			TDistribution tDist = new TDistribution(degreesOfFreedom);
			if (twoTailed) {
				// Two-tailed test
				float pValue = (float) (2 * (1 - tDist.cumulativeProbability(Math.abs(tStatistic))));

				return pValue + "";
			} else {

				// Return the minimum of the two p-values
				return (1 - tDist.cumulativeProbability(Math.abs(tStatistic))) + "";

			}
		} catch (Exception e) {
			e.printStackTrace();
			return "Unable to perform (samples to small or contain all zeros)";
		}

	}

	/**
	 * Calculates the t-statistic for two samples.
	 * 
	 * @param sample1 The first data sample.
	 * @param sample2 The second data sample.
	 * @return The calculated t-statistic.
	 */
	private static double calculateTStatistic(double[] sample1, double[] sample2) {
		double mean1 = calculateMean(sample1);
		double mean2 = calculateMean(sample2);
		double var1 = calculateVariance(sample1, mean1);
		double var2 = calculateVariance(sample2, mean2);
		int n1 = sample1.length;
		int n2 = sample2.length;

		double pooledVariance = ((n1 - 1) * var1 + (n2 - 1) * var2) / (n1 + n2 - 2);
		double tStatistic = (mean1 - mean2) / Math.sqrt(pooledVariance * (1.0 / n1 + 1.0 / n2));

		return tStatistic;
	}

	/**
	 * Calculates the mean of a single data sample.
	 * 
	 * @param sample The data sample.
	 * @return The mean of the sample.
	 */
	private static double calculateMean(double[] sample) {
		double sum = 0;
		for (double value : sample) {
			sum += value;
		}
		return sum / sample.length;
	}

	/**
	 * Calculates the variance of a single data sample.
	 * 
	 * @param sample The data sample.
	 * @param mean   The pre-calculated mean of the sample.
	 * @return The variance of the sample.
	 */
	private static double calculateVariance(double[] sample, double mean) {
		float sum = 0;
		for (double value : sample) {
			sum += Math.pow(value - mean, 2);
		}
		return sum / (sample.length - 1);
	}

	/**
	 * Calculates the degrees of freedom for the t-test.
	 * 
	 * @param sample1 The first data sample.
	 * @param sample2 The second data sample.
	 * @return The degrees of freedom.
	 */
	private static int calculateDegreesOfFreedom(double[] sample1, double[] sample2) {
		int n1 = sample1.length;
		int n2 = sample2.length;
		return n1 + n2 - 2;
	}

}
