package com.carrington.WIA.stats;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.inference.TestUtils;

/**
 * Utility class for performing one-way ANOVA (Analysis of Variance) tests. This
 * class relies on the Apache Commons Math library for the statistical
 * calculations.
 */
public abstract class ANOVATest {

	/**
	 * Calculates the p-value for a one-way ANOVA test from an array of double
	 * arrays.
	 * 
	 * @param data The groups of data to compare, as a variable number of double
	 *             arrays.
	 * @return The p-value, or a minimum value of 2.2e-16 if the result is zero.
	 */
	public static double calculatePValue(double[]... data) {

		List<double[]> col = new ArrayList<double[]>();
		for (double[] dat : data) {
			col.add(dat);
		}
		double result = TestUtils.oneWayAnovaPValue(col);
		if (result == 0) {
			return 2.2e-16;
		} else {
			return result;

		}
	}

	/**
	 * Calculates the p-value for a one-way ANOVA test from a list of double arrays.
	 * 
	 * @param data A list containing the groups of data to compare.
	 * @return The p-value, or a minimum value of 2.2e-16 if the result is zero.
	 */
	public static double pValue(List<double[]> data) {

		double result = TestUtils.oneWayAnovaPValue(data);
		if (result == 0) {
			return 2.2e-16;
		} else {
			return result;

		}
	}

}
