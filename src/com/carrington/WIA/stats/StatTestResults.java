package com.carrington.WIA.stats;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stores the results of various statistical tests performed on a dataset,
 * maintaining a specific order for the tests.
 */
public class StatTestResults {

	private TreeMap<StatTest, Double> testResults = new TreeMap<StatTest, Double>(new Comparator<StatTest>() {
		public int compare(StatTest o1, StatTest o2) {

			if (o1.getOrder() > o2.getOrder())
				return 1;
			else if (o1.getOrder() < o2.getOrder())
				return -1;
			else
				return 0;

		}
	});

	private final String comparisonName;

	/**
	 * Constructs a new {@link StatTestResults} object for a given comparison.
	 * 
	 * @param comparisonName The name of the comparison being performed.
	 */
	public StatTestResults(String comparisonName) {
		this.comparisonName = comparisonName;
	}

	/**
	 * Adds a test result to the collection.
	 * 
	 * @param testName The enum of the statistical test performed.
	 * @param result   The resulting p-value or test statistic.
	 * @throws IllegalArgumentException if a result for the given test already
	 *                                  exists.
	 */
	public void addResult(StatTest testName, Double result) {
		if (this.testResults.containsKey(testName))
			throw new IllegalArgumentException("Test result already contained");

		this.testResults.put(testName, result);
	}

	/**
	 * Gets the name of the comparison.
	 * 
	 * @return The comparison name.
	 */
	public String getComparisonName() {
		return this.comparisonName;
	}

	/**
	 * Retrieves the result for a specific statistical test.
	 * 
	 * @param statTest The statistical test for which to retrieve the result.
	 * @return The result as a {@link Double}, or null if not found.
	 */
	public Double getResult(StatTest statTest) {
		return this.testResults.get(statTest);
	}

	/**
	 * @return unmodifiable {@link TreeMap} (ordering preserved by test name)
	 */
	public Map<StatTest, Double> getResults() {
		return Collections.unmodifiableMap(this.testResults);
	}

}