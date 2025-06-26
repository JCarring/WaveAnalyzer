package com.carrington.WIA.stats;

import org.apache.commons.math3.distribution.TDistribution;


public class WelchTTest {

	// Function to perform Welch's T-test
		public static double getP(double[] sample1, double[] sample2, boolean twoTailed) {
				
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

	
	// Function to perform Welch's T-test
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

	// Function to calculate the t-statistic
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

	// Function to calculate the mean
	private static double calculateMean(double[] sample) {
		double sum = 0;
		for (double value : sample) {
			sum += value;
		}
		return sum / sample.length;
	}

	// Function to calculate the variance
	private static double calculateVariance(double[] sample) {
		double mean = calculateMean(sample);
		double sum = 0;
		for (double value : sample) {
			sum += Math.pow(value - mean, 2);
		}
		return sum / (sample.length - 1);
	}

	// Function to calculate degrees of freedom for Welch's T-test
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
