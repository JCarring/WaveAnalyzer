package com.carrington.WIA.stats;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class  StatTestResults {
	
	private TreeMap<StatTest, Double> testResults = new TreeMap<StatTest, Double>(new Comparator<StatTest>() {
		public int compare(StatTest o1,StatTest o2)
        {
			
			if (o1.getOrder() > o2.getOrder()) 
				return 1;
			else if (o1.getOrder() < o2.getOrder())
				return -1;
			else 
				return 0;

        }
	});
	
	private final String comparisonName;


	public StatTestResults(String comparisonName) {
		this.comparisonName = comparisonName;
	}
	
	public void addResult(StatTest testName, Double result) {
		if (this.testResults.containsKey(testName))
			throw new IllegalArgumentException("Test result already contained");
		
		this.testResults.put(testName, result);
	}
	
	public String getComparisonName() {
		return this.comparisonName;
	}
	
	public Double getResult(StatTest statTest) {
		return this.testResults.get(statTest);
	}
	
	/**
	 * @return unmodifiable TreeMap (ordering preserved by test name)
	 */
	public Map<StatTest, Double> getResults() {
		return Collections.unmodifiableMap(this.testResults);
	}
	


	
}