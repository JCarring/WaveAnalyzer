package com.carrington.WIA.stats;

import org.apache.commons.math3.distribution.TDistribution;

/**
 * Performs Welch's t-test, which is an adaptation of Student's t-test for
 * comparing the means of two independent samples with unequal variances.
 */
public abstract class WelchTTest {

	/**
	 * Performs Welch's T-test and returns the p-value.
	 * 
	 * @param sample1   The first data sample.
	 * @param sample2   The second data sample.
	 * @param twoTailed If true, performs a two-tailed test; otherwise, performs the
	 *                  more significant one-tailed test.
	 * @return The calculated p-value, or Double.NaN if the total sample size is
	 *         less than 4. Returns 1.0 if degrees of freedom is 0.
	 */
	public static double calculatePValue(double[] sample1, double[] sample2, boolean twoTailed) {

		if (sample1.length + sample2.length < 4) {
			return Double.NaN;
		}
		double tStatistic = calculateTStatistic(sample1, sample2);
		int degreesOfFreedom = calculateDegreesOfFreedom(sample1, sample2);

		if (degreesOfFreedom == 0) {
			return 1.0;
		}

		TDistribution tDist = new TDistribution(degreesOfFreedom);

		if (twoTailed) {
			// Two-tailed test
			double pValue = 2 * (1 - tDist.cumulativeProbability(tStatistic));
			if (pValue == 0) {
				return 2.2e-16;
			} else {
				return pValue;

			}
		} else {

			// One-tailed test (right-tailed)
			// Calculate p-value for the right-tailed test
			double pValueRight = 1 - tDist.cumulativeProbability(tStatistic);

			// Calculate p-value for the left-tailed test
			double pValueLeft = tDist.cumulativeProbability(-tStatistic);

			// Return the minimum of the two p-values
			double pValue = Math.min(pValueRight, pValueLeft);

			if (pValue == 0) {
				return 2.2e-16;
			} else {
				return pValue;

			}

		}

	}

	/**
	 * Performs Welch's T-test and returns the p-value as a string.
	 * 
	 * @param sample1   The first data sample.
	 * @param sample2   The second data sample.
	 * @param twoTailed If true, performs a two-tailed test; otherwise, performs the
	 *                  more significant one-tailed test.
	 * @return A string representation of the p-value. Returns an empty string if
	 *         total sample size is less than 4.
	 */
	public static String perform(double[] sample1, double[] sample2, boolean twoTailed) {

		if (sample1.length + sample2.length < 4) {
			return "";
		}
		double tStatistic = calculateTStatistic(sample1, sample2);
		int degreesOfFreedom = calculateDegreesOfFreedom(sample1, sample2);

		if (degreesOfFreedom == 0) {
			return "1.0";
		}

		TDistribution tDist = new TDistribution(degreesOfFreedom);

		if (twoTailed) {
			// Two-tailed test
			double pValue = 2 * (1 - tDist.cumulativeProbability(tStatistic));
			if (pValue == 0) {
				return 2.2e-16 + "";
			} else {
				return pValue + "";

			}
		} else {

			// One-tailed test (right-tailed)
			// Calculate p-value for the right-tailed test
			double pValueRight = 1 - tDist.cumulativeProbability(tStatistic);

			// Calculate p-value for the left-tailed test
			double pValueLeft = tDist.cumulativeProbability(-tStatistic);

			// Return the minimum of the two p-values
			double pValue = Math.min(pValueRight, pValueLeft);

			if (pValue == 0) {
				return 2.2e-16 + "";
			} else {
				return pValue + "";

			}

		}

	}

	/**
	 * Calculates the t-statistic for Welch's t-test.
	 * 
	 * @param sample1 The first data sample.
	 * @param sample2 The second data sample.
	 * @return The absolute value of the t-statistic.
	 */
	private static double calculateTStatistic(double[] sample1, double[] sample2) {
		double mean1 = calculateMean(sample1);
		double mean2 = calculateMean(sample2);
		double var1 = calculateVariance(sample1);
		double var2 = calculateVariance(sample2);
		int n1 = sample1.length;
		int n2 = sample2.length;

		double tStatistic = Math.abs((mean1 - mean2) / Math.sqrt((var1 / n1) + (var2 / n2)));
		return tStatistic;
	}

	/**
	 * Calculates the mean of a data sample.
	 * 
	 * @param sample The array of doubles representing the sample.
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
	 * Calculates the sample variance.
	 * 
	 * @param sample The array of doubles representing the sample.
	 * @return The variance of the sample.
	 */
	private static double calculateVariance(double[] sample) {
		double mean = calculateMean(sample);
		double sum = 0;
		for (double value : sample) {
			sum += Math.pow(value - mean, 2);
		}
		return sum / (sample.length - 1);
	}

	/**
	 * Calculates the degrees of freedom using the Welch-Satterthwaite equation.
	 * 
	 * @param sample1 The first data sample.
	 * @param sample2 The second data sample.
	 * @return The calculated degrees of freedom as an integer.
	 */
	private static int calculateDegreesOfFreedom(double[] sample1, double[] sample2) {
		double var1 = calculateVariance(sample1);
		double var2 = calculateVariance(sample2);
		int n1 = sample1.length;
		int n2 = sample2.length;
		double numerator = Math.pow((var1 / n1) + (var2 / n2), 2);
		double denominator = Math.pow(var1 / n1, 2) / (n1 - 1) + Math.pow(var2 / n2, 2) / (n2 - 1);
		return (int) Math.floor(numerator / denominator);
	}

}
