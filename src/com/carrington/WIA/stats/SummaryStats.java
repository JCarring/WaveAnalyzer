package com.carrington.WIA.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.carrington.WIA.Utils;

/**
 * Calculates and holds summary statistics for a {@link DataCollection}. It can
 * handle both continuous and discrete boolean data types, providing relevant
 * metrics for each.
 */
public class SummaryStats {

	private final DataType dt;
	private final boolean expressContAsPercent;

	private final Integer numTrue;
	private final Integer numFalse;

	private final Integer contN;
	private final Double contMean;
	private final Double contRangeLow;
	private final Double contRangeHigh;
	private final Double contSD;

	private final String name;

	/**
	 * Creates a list of {@link SummaryStats} objects from lists of outcome names
	 * and data collections.
	 * 
	 * @param outcomeName    A list of names for each data collection.
	 * @param dataCollection A list of {@link DataCollection} objects.
	 * @return A list of {@link SummaryStats} objects.
	 * @throws IllegalArgumentException if data types are inconsistent across
	 *                                  collections.
	 */
	public static List<SummaryStats> createStats(List<String> outcomeName, List<DataCollection> dataCollection) {
		List<SummaryStats> stats = new ArrayList<SummaryStats>();

		DataType dt = null;
		int counter = 0;
		for (DataCollection dc : dataCollection) {
			if (dt == null)
				dt = dc.getDataType();
			else if (dt != dc.getDataType())
				throw new IllegalArgumentException("When creating SummaryStats, all data types must be the same");

			SummaryStats ss = new SummaryStats(outcomeName.get(counter), dc);
			stats.add(ss);
			counter++;

		}

		return stats;

	}

	/**
	 * Creates a single {@link SummaryStats} object from a {@link DataCollection},
	 * using "All" as the default name.
	 * 
	 * @param dataSet The {@link DataCollection} to summarize.
	 * @return A new {@link SummaryStats} object.
	 */
	public static SummaryStats createStats(DataCollection dataSet) {
		return new SummaryStats("All", dataSet);

	}

	/**
	 * Create summary stats instance
	 * 
	 * @param name The name associated with this set of statistics.
	 * @param dc   The {@link DataCollection} to be summarized.
	 * @throws IllegalArgumentException for undefined data types.
	 */
	private SummaryStats(String name, DataCollection dc) {
		this.name = name;
		dt = dc.getDataType();
		expressContAsPercent = dc.getExpressedAsPerc();
		switch (dt) {
		case DISCRETE_BOOLEAN:

			boolean[] valuesBoolean = dc.getBooleanValues();

			numTrue = Utils.countTrue(valuesBoolean);
			numFalse = valuesBoolean.length - numTrue;

			contN = null;
			contMean = null;
			contRangeLow = null;
			contRangeHigh = null;
			contSD = null;

			break;

		case CONTINUOUS:

			double[] valuesDouble = dc.getDoubleValues();
			SummaryStatistics ss = new SummaryStatistics();
			ss.getStandardDeviation();
			for (double d : valuesDouble) {
				ss.addValue(d);
			}

			numTrue = null;
			numFalse = null;

			contN = valuesDouble.length;
			contMean = ss.getMean();
			contRangeLow = ss.getMin();
			contRangeHigh = ss.getMax();
			contSD = valuesDouble.length >= 2 ? ss.getStandardDeviation() : null;

			break;
		default:
			throw new IllegalArgumentException("Undefined data type");

		}

	}

	/**
	 * Gets a map of statistic field names to their formatted string values.
	 * 
	 * @return A {@link LinkedHashMap} where keys are statistic names (e.g., "n",
	 *         "mean") and values are their formatted results.
	 */
	public LinkedHashMap<String, String> getFieldValues() {

		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		switch (dt) {
		case DISCRETE_BOOLEAN:
			map.put("n", (double) (numTrue + numFalse) + "");
			map.put("# true", (double) numTrue + "");
			map.put("# false", (double) numFalse + "");
			double value = (((double) numTrue / (numTrue + numFalse)) * 100);
			map.put("% true", (Double.isNaN(value) ? "0.0" : String.format("%.1f", value)) + "%");
			break;
		case CONTINUOUS:
			map.put("n", contN + "");
			if (contN == 0) {
				map.put("mean", "--");
				map.put("min", "--");
				map.put("max", "--");
				map.put("sd", "--");
			} else if (expressContAsPercent) {
				map.put("mean", convertToPercent((double) contMean));
				map.put("min", convertToPercent((double) contRangeLow));
				map.put("max", convertToPercent((double) contRangeHigh));
				if (contSD != null) {
					map.put("sd", convertToPercent((double) contSD));

				} else {
					map.put("sd", "--");
				}
			} else {
				map.put("mean", formatDouble((double) contMean));
				map.put("min", formatDouble((double) contRangeLow));
				map.put("max", formatDouble((double) contRangeHigh));
				if (contSD != null) {
					map.put("sd", formatDouble((double) contSD));

				} else {
					map.put("sd", "--");
				}
			}

			break;

		}

		return map;

	}

	/**
	 * Gets the data type for which these stats were calculated.
	 * 
	 * @return The DataType enum.
	 */
	public DataType getDataType() {
		return dt;
	}

	/**
	 * Provides a typical ordering of statistic field names.
	 * 
	 * @return A list of strings representing the desired order of fields.
	 */
	public static List<String> getTypicalStatOrder() {
		return Arrays.asList("n", "# true", "# false", "% true", "mean", "max", "min", "sd");
	}

	/**
	 * Gets the number of statistic fields available for the data type.
	 * 
	 * @return The count of fields.
	 */
	public Integer getFieldsNumber() {
		return getFields().size();
	}

	/**
	 * Gets the list of applicable statistic field names based on the data type.
	 * 
	 * @return A list of field names.
	 */
	public List<String> getFields() {

		List<String> list = new ArrayList<String>();
		switch (dt) {
		case DISCRETE_BOOLEAN:
			list.add("n");
			list.add("# true");
			list.add("# false");
			list.add("% true");
			break;
		case CONTINUOUS:
			list.add("n");
			list.add("mean");
			list.add("min");
			list.add("max");
			list.add("sd");
			break;

		}

		return list;

	}

	/**
	 * Gets the group name associated with these statistics.
	 * 
	 * @return The group name.
	 */
	public String getGroupName() {
		return this.name;
	}

	/**
	 * Converts a double value to a formatted percentage string (e.g., "55.25%").
	 * 
	 * @param value The value to convert.
	 * @return The formatted percentage string.
	 */
	private static String convertToPercent(double value) {
		return String.format("%.2f%%", value * 100);
	}

	/**
	 * Formats a double value to a string with two decimal places.
	 * 
	 * @param value The value to format.
	 * @return The formatted double string.
	 */
	public static String formatDouble(double value) {
		return String.format("%.2f", value);
	}

}
