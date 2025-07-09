package com.carrington.WIA.stats;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * Performs a two-sample Z-test for proportions. This test is used to compare
 * the proportions of two independent samples to determine if they are
 * significantly different.
 */
public abstract class TwoSampleProportionZTest {

	/**
	 * Calculates the one-tailed p-value for a two-sample proportion Z-test.
	 * 
	 * @param successes1 The number of successes in the first sample.
	 * @param successes2 The number of successes in the second sample.
	 * @param n1         The total size of the first sample.
	 * @param n2         The total size of the second sample.
	 * @return The one-tailed p-value. Returns 1.0 if either sample size is zero.
	 */
	public static double calcPValueOneTail(int successes1, int successes2, int n1, int n2) {
		// Calculate the pooled proportion

		if (n1 == 0 || n2 == 0) {
			return 1.0;
		}

		return calcPValueTwoTail(successes1, successes2, n1, n2) / 2.0;
	}

	/**
	 * Calculates the two-tailed p-value for a two-sample proportion Z-test.
	 * 
	 * @param successes1 The number of successes in the first sample.
	 * @param successes2 The number of successes in the second sample.
	 * @param n1         The total size of the first sample.
	 * @param n2         The total size of the second sample.
	 * @return The two-tailed p-value. Returns 1.0 if either sample size or the
	 *         standard error is zero.
	 */
	public static double calcPValueTwoTail(int successes1, int successes2, int n1, int n2) {

		if (n1 == 0 || n2 == 0) {
			return 1.0;
		}

		double p1 = ((double) successes1) / n1;
		double p2 = ((double) successes2) / n2;

		double pooledP = (p1 * n1 + p2 * n2) / (n1 + n2);

		// Calculate the standard error assuming pooled proportions
		double standardError = Math.sqrt(pooledP * (1 - pooledP) * ((1.0 / n1) + (1.0 / n2)));

		if (standardError == 0) {
			return 1.0;
		}
		// Calculate the Z score
		double zScore = (p1 - p2) / standardError;

		// Create a standard normal distribution
		NormalDistribution standardNormal = new NormalDistribution(0, 1);

		// Calculate the probability in the upper tail
		double upperTailProb = standardNormal.cumulativeProbability(-Math.abs(zScore));

		// Calculate the probability in the lower tail
		double lowerTailProb = 1 - standardNormal.cumulativeProbability(Math.abs(zScore));

		// Calculate the two-tailed p-value
		double pValue = upperTailProb + lowerTailProb;

		return pValue;
	}

}
