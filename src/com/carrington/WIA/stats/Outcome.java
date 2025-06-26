package com.carrington.WIA.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.inference.TestUtils;

import com.carrington.WIA.Utils;

/**
 * Represents an outcome (dependent variable), and stores different groups of
 * data which can be statistically compared.
 */
public class Outcome {

	/** Dependent variable **/
	private final String name;
	// Key = comparison group
	private List<String> groups = new ArrayList<String>();
	private List<DataCollection> values = new ArrayList<DataCollection>();
	private DataType dataType = null;
	/** statistics. Includes test name and test statistic **/
	private StatTestResults stats = null;
	private ArrayList<SummaryStats> summaryStats = new ArrayList<SummaryStats>();


	public Outcome(String name) {
		this.name = name;
	}

	public void addDataSet(String group, DataCollection data) {
		if (dataType == null) {
			dataType = data.getDataType();
		} else if (dataType != data.getDataType()) {
			throw new IllegalArgumentException("Data type of all data collections for outcome must be the same!");
		}
		groups.add(group);
		values.add(data);

	}
	
	public DataType getDataType() {
		return this.dataType;
		
	}

	public LinkedHashMap<String, DataCollection> getDataSets() {
		Iterator<String> itrGr = groups.iterator();
		Iterator<DataCollection> itrVal = values.iterator();
		LinkedHashMap<String, DataCollection> datasets = new LinkedHashMap<String, DataCollection>();
		while (itrGr.hasNext()) {
			datasets.put(itrGr.next(), itrVal.next());
		}
		return datasets;
	}

	public String getName() {
		return this.name;
	}

	public List<String> getGroups() {
		return this.groups;
	}
	

	public List<SummaryStats> getSummaryStats() {
		return this.summaryStats;
	}

	/**
	 * Runs the statistics and stores them. It wll NOT run them if the datasets do
	 * not have the same type OR there is a data set with less than 2 data points.
	 */
	public void runStats() {

		summaryStats.clear();
		summaryStats.addAll(SummaryStats.createStats(groups, values));

		stats = new StatTestResults(name);

		if (values.size() < 2) {
			return;
		}
		DataType dt = values.get(0).getDataType();
		for (DataCollection dc : values) {
			if (dt != dc.getDataType())
				return;
			if (dc.getDoubleValues().length < 2 && dc.getBooleanValues().length < 2) {
				// cannot run stats.
				switch (dt) {
				case CONTINUOUS:
					stats.addResult(StatTest.KOLMOGOROV_SMIRNOV, Double.NaN);
					stats.addResult(StatTest.STUDENT_T_TEST_TWOTAIL, Double.NaN);
					stats.addResult(StatTest.STUDENT_T_TEST_ONETAIL, Double.NaN);
					stats.addResult(StatTest.WELCH_T_TEST_TWOTAIL, Double.NaN);
					stats.addResult(StatTest.WELCH_T_TEST_ONETAIL, Double.NaN);
					stats.addResult(StatTest.MANN_WHITNEY, Double.NaN);
					break;
				case DISCRETE_BOOLEAN:
					stats.addResult(StatTest.FISHER_EXACT_TWOTAIL, Double.NaN);
					stats.addResult(StatTest.FISHER_EXACT_ONETAIL, Double.NaN);
					stats.addResult(StatTest.CHI_SQUARE, Double.NaN);
					stats.addResult(StatTest.ZTEST_PROP_TWOTAIL, Double.NaN);
					stats.addResult(StatTest.ZTEST_PROP_ONETAIL, Double.NaN);
					break;
				}
				return;
			}
		}

		if (values.size() == 2) {
			switch (dt) {
			case CONTINUOUS:
				MannWhitneyUTest mwut = new MannWhitneyUTest();
				double[] dc1 = values.get(0).getDoubleValues();
				double[] dc2 = values.get(1).getDoubleValues();
				
				try {
					stats.addResult(StatTest.KOLMOGOROV_SMIRNOV,TestUtils.kolmogorovSmirnovTest(dc1, dc2));
				} catch (Exception e) {
					stats.addResult(StatTest.KOLMOGOROV_SMIRNOV, Double.NaN);
				}

				if (StudentTTest.getP(dc1, dc2, true) == 0) {
					System.out.println(" ");
					System.out.println("Name: " + name);
					System.out.println(" " + Arrays.toString(dc1));
					System.out.println(" " + Arrays.toString(dc2));

					for (double d : dc1) {
						System.out.println(d);

					}
					System.out.println(" ");
					for (double d : dc2) {
						System.out.println(d);

					}


				}
				stats.addResult(StatTest.STUDENT_T_TEST_TWOTAIL, StudentTTest.getP(dc1, dc2, true));
				stats.addResult(StatTest.STUDENT_T_TEST_ONETAIL, StudentTTest.getP(dc1, dc2, false));
				stats.addResult(StatTest.WELCH_T_TEST_TWOTAIL, WelchTTest.getP(dc1, dc2, true));

				stats.addResult(StatTest.WELCH_T_TEST_ONETAIL, WelchTTest.getP(dc1, dc2, false));
				stats.addResult(StatTest.MANN_WHITNEY, mwut.mannWhitneyUTest(dc1, dc2));

				break;
			case DISCRETE_BOOLEAN:
				boolean[] b1 = values.get(0).getBooleanValues();
				boolean[] b2 = values.get(1).getBooleanValues();
				int numTrueb1 = Utils.countTrue(b1);
				int numFalseb1 = b1.length - numTrueb1;
				int numTrueb2 = Utils.countTrue(b2);
				int numFalseb2 = b2.length - numTrueb2;


				stats = new StatTestResults(name);
				FisherExact fe = new FisherExact(b1.length + b2.length);
				stats.addResult(StatTest.FISHER_EXACT_TWOTAIL, fe.getTwoTailedP(numTrueb1, numFalseb1, numTrueb2, numFalseb2));
				stats.addResult(StatTest.FISHER_EXACT_ONETAIL, fe.getCumlativeP(numTrueb1, numFalseb1, numTrueb2, numFalseb2));
				ArrayList<int[]> list = new ArrayList<int[]>();
				for (DataCollection dc : values) {
					int countTrue = Utils.countTrue(dc.getBooleanValues());
					int countFalse = dc.getBooleanValues().length - countTrue;
					list.add(new int[] { countTrue, countFalse });

				}
				stats.addResult(StatTest.CHI_SQUARE, ChiSqaureTest.calculatePValue(list.toArray(new int[0][])));
				stats.addResult(StatTest.ZTEST_PROP_TWOTAIL, TwoSampleProportionZTest.calcPValueTwoTail(numTrueb1, numTrueb2, 
						b1.length, b2.length));
				stats.addResult(StatTest.ZTEST_PROP_ONETAIL, TwoSampleProportionZTest.calcPValueOneTail(numTrueb1, numTrueb2, 
						b1.length, b2.length));
				break;

			}
		} else {

			switch (dt) {
			case CONTINUOUS:
				List<double[]> data = new ArrayList<double[]>();
				values.stream().forEach(cd -> data.add(cd.getDoubleValues()));
				stats.addResult(StatTest.ONE_WAY_ANOVA, ANOVATest.pValue(data));
				break;
			case DISCRETE_BOOLEAN:
				ArrayList<int[]> list = new ArrayList<int[]>();
				for (DataCollection dc : values) {
					int countTrue = Utils.countTrue(dc.getBooleanValues());
					int countFalse = dc.getBooleanValues().length - countTrue;
					list.add(new int[] { countTrue, countFalse });

				}
				stats.addResult(StatTest.CHI_SQUARE,ChiSqaureTest.calculatePValue(list.toArray(new int[0][])));

				break;

			}

		}

	}

	/**
	 * @return most recently computed statistics returned by {@link #runStats()}. If not computed, then null.
	 */
	public StatTestResults getStats() {
		return this.stats;
	}
	
	public void printOutcome() {
		System.out.println(this.name);
		System.out.println("Groups: " + String.join(", ", this.groups));
		for (DataCollection value : this.values) {
			if (value.isBinary()) {
				System.out.println(value.getGroupName() + ": " + Utils.getStringFromArray(value.getBooleanValues(), 10000));

			} else {
				System.out.println(value.getGroupName() + ": " + Utils.getStringFromArray(value.getDoubleValues(), 10000));
			}
		}
	}
//
//	public TreeMap<String, Map<OutcomeParameter, String>> getArrayOfData() {
//
//		TreeMap<String, Map<OutcomeParameter, String>> map = new TreeMap<String, Map<OutcomeParameter, String>>(
//				String.CASE_INSENSITIVE_ORDER);
//
//		Iterator<String> grpItr = groups.iterator();
//		Iterator<DataCollection> dcItr = values.iterator();
//		while (grpItr.hasNext()) {
//
//			String group = grpItr.next();
//			DataCollection dc = dcItr.next();
//
//			Map<OutcomeParameter, String> subMap = new HashMap<OutcomeParameter, String>();
//			if (dc.isBinary()) {
//
//				for (OutcomeParameter outParam : OutcomeParameter.getParameters(false)) {
//					subMap.put(outParam, "--");
//				}
//
//				subMap.put(OutcomeParameter.N, dc.getBooleanValues().length + "");
//
//				SummaryStatisticsBool ssb = dc.getSummaryStatsBinary();
//
//				subMap.put(OutcomeParameter.NumberTrue, ssb.getNumTrue() + "");
//				subMap.put(OutcomeParameter.NumberFalse, ssb.getNumFalse() + "");
//				subMap.put(OutcomeParameter.Proportion, ssb.getProportionTrue() + "");
//
//			} else {
//				for (OutcomeParameter outParam : OutcomeParameter.getParameters(true)) {
//					subMap.put(outParam, "--");
//				}
//				subMap.put(OutcomeParameter.N, dc.getDoubleValues().length + "");
//
//				DescriptiveStatistics ds = dc.getSummaryStatsContinuous();
//				subMap.put(OutcomeParameter.Mean, ds.getMean() + "");
//				subMap.put(OutcomeParameter.Max, ds.getMax() + "");
//				subMap.put(OutcomeParameter.Min, ds.getMin() + "");
//				subMap.put(OutcomeParameter.StDev, ds.getStandardDeviation() + "");
//				subMap.put(OutcomeParameter.Variance, ds.getVariance() + "");
//
//			}
//
//			map.put(group, subMap);
//
//		}
//
//		return map;
//
//	}

	public Map<StatTest, String> getArrayOfStatsStringAll() {
		Map<StatTest, String> mapWithAllStatTests = new HashMap<StatTest, String>();
		Map<StatTest, Double> existingResults = stats.getResults();
		for (StatTest st : StatTest.values()) {

			Double result = existingResults.get(st);
			if (Double.isNaN(result)) {
				
				mapWithAllStatTests.put(st, result + "n to small");

			} else {
				mapWithAllStatTests.put(st, result + "");
			}

		}

		return mapWithAllStatTests;

	}

	public static enum OutcomeParameter {

		N(null), Mean(false), Max(false), Min(false), StDev(false), Variance(false), NumberTrue(true),
		NumberFalse(true), Proportion(true);

		private Boolean dt;

		private OutcomeParameter(Boolean dt) {
			this.dt = dt;
		}

		public static OutcomeParameter[] getParameters(boolean isBinary) {

			ArrayList<OutcomeParameter> params = new ArrayList<OutcomeParameter>(values().length);

			for (OutcomeParameter outParam : values()) {
				if (isBinary) {
					if (outParam.dt == Boolean.TRUE) {
						params.add(outParam);
					}
				} else {
					if (outParam.dt == Boolean.FALSE) {
						params.add(outParam);
					}
				}

			}

			return params.toArray(new OutcomeParameter[0]);

		}

	}
	
	

}
