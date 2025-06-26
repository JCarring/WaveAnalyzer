package com.carrington.WIA.stats;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.carrington.WIA.Utils;

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
	
	public static SummaryStats createStats(DataCollection dataSet) {
		return new SummaryStats("All", dataSet);
		
	}
	
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

	public LinkedHashMap<String, String> getFieldValues() {

		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		switch (dt) {
		case DISCRETE_BOOLEAN:
			map.put("n",  (double) (numTrue + numFalse) + "" );
			map.put("# true",  (double) numTrue  + "");
			map.put("# false",  (double) numFalse  + "");
			double value = (((double) numTrue / (numTrue + numFalse)) * 100);
			map.put("% true",  (Double.isNaN(value) ? "0.0" : String.format("%.1f", value) )+ "%"  );
			break;
		case CONTINUOUS:
			map.put("n",  contN  + "");
			if (contN == 0) {
				map.put("mean",  "--" );
				map.put("min",  "--"  );
				map.put("max",  "--"  );
				map.put("sd", "--" );
			}  else if (expressContAsPercent) {
				map.put("mean",  convertToPercent((double) contMean));
				map.put("min",  convertToPercent((double) contRangeLow));
				map.put("max",  convertToPercent((double) contRangeHigh));
				if (contSD != null) {
					map.put("sd", convertToPercent((double) contSD));

				} else {
					map.put("sd", "--");
				}
			} else {
				map.put("mean",  formatDouble((double) contMean));
				map.put("min",  formatDouble((double) contRangeLow));
				map.put("max",  formatDouble((double) contRangeHigh));
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
	
	public DataType getDataType() {
		return dt;
	}
	
	public static List<String> getTypicalStatOrder() {
		return Arrays.asList("n","# true","# false","% true","mean","max","min","sd");
	}
	
	public Integer getFieldsNumber() {
		return getFields().size();
	}

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

	public String getGroupName() {
		return this.name;
	}

	private static String convertToPercent(double value) {
        return String.format("%.2f%%", value * 100);
    }
	
    public static String formatDouble(double value) {
        return String.format("%.2f", value);
    }

}
