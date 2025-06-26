package com.carrington.WIA.stats;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;

public class ChiSqaureTest {

	/**
	 * Each row is a group. The column should be length two (successes and failures)
	 * @return p-Value
	 */
    public static double calculatePValue(int[][] observed) {
        // Compute the expected frequencies
        double[] rowSums = new double[observed.length];
        double[] colSums = new double[observed[0].length];
        double totalSum = 0.0;
        
        for (int i = 0; i < observed.length; i++) {
            for (int j = 0; j < observed[0].length; j++) {
                rowSums[i] += observed[i][j];
                colSums[j] += observed[i][j];
                totalSum += observed[i][j];
            }
        }

        double[][] expected = new double[observed.length][observed[0].length];
        for (int i = 0; i < observed.length; i++) {
            for (int j = 0; j < observed[0].length; j++) {
                expected[i][j] = (rowSums[i] * colSums[j]) / totalSum;
            }
        }

        // Compute the chi-squared statistic
        double chiSquared = 0.0;
        for (int i = 0; i < observed.length; i++) {
            for (int j = 0; j < observed[0].length; j++) {
                double numerator = observed[i][j] - expected[i][j];
                chiSquared += (numerator * numerator) / expected[i][j];
            }
        }

        // Degrees of freedom
        int df = (observed.length - 1) * (observed[0].length - 1);

        // Create a chi-squared distribution with the appropriate degrees of freedom
        ChiSquaredDistribution chiSquaredDist = new ChiSquaredDistribution(df);
        // Calculate the p-value
        double pValue = 1.0 - chiSquaredDist.cumulativeProbability(chiSquared);
        
        if (Double.isNaN(pValue)) {
        	return 1.0;
        } else {
            return pValue;
        }
    }

}
