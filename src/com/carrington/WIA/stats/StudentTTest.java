package com.carrington.WIA.stats;

import org.apache.commons.math3.distribution.TDistribution;

public class StudentTTest {

	// Function to perform T-test
	public static double getP(double[] sample1, double[] sample2, boolean twoTailed) {

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

	// Function to perform T-test
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

	// Function to calculate the mean
	private static double calculateMean(double[] sample) {
		double sum = 0;
		for (double value : sample) {
			sum += value;
		}
		return sum / sample.length;
	}

	// Function to calculate the variance
	private static double calculateVariance(double[] sample, double mean) {
		float sum = 0;
		for (double value : sample) {
			sum += Math.pow(value - mean, 2);
		}
		return sum / (sample.length - 1);
	}

	// Function to calculate the degrees of freedom
	private static int calculateDegreesOfFreedom(double[] sample1, double[] sample2) {
		int n1 = sample1.length;
		int n2 = sample2.length;
		return n1 + n2 - 2;
	}

}
