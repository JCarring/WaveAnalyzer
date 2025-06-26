package com.carrington.WIA.stats;
import org.apache.commons.math3.distribution.NormalDistribution;

public class TwoSampleProportionZTest {
    
    public static double calcPValueOneTail(int successes1, int successes2, int n1, int n2) {
        // Calculate the pooled proportion
    	
    	if (n1 == 0 || n2 == 0) {
    		return 1.0;
    	}
        
        return calcPValueTwoTail(successes1, successes2, n1, n2) / 2.0;
    }
    
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
