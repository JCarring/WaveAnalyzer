package com.carrington.WIA.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import com.carrington.WIA.Utils;
import com.carrington.WIA.IO.SheetWriter;

public class StatisticalComparison {

	private final String nameOfComparison;
	private ArrayList<Outcome> outcomes = new ArrayList<Outcome>();

	public StatisticalComparison(String nameOfComparison) {
		this.nameOfComparison = nameOfComparison;
	}

	public String getNameOfComparison() {
		return this.nameOfComparison;
	}

	/**
	 * @return outcomes in the order in which they were inserted
	 */
	public List<Outcome> getOutcomes() {
		return this.outcomes;
	}

	public void addOutcome(Outcome... outcomesToAdd) {
		for (Outcome out : outcomesToAdd) {
			this.outcomes.add(out);
			out.runStats();
		}
	}

	public void print(boolean includeRawVals) {

		System.out.println("=================================");
		System.out.println("Comparison: " + nameOfComparison);
		System.out.println();
		for (Outcome out : outcomes) {
			System.out.println(out.getName());
			if (out.getStats() == null) {
				System.out.println("  No stats calculated");
				continue;
			}
			for (Entry<StatTest, Double> en : out.getStats().getResults().entrySet()) {
				System.out.println("  " + en.getKey() + ": " + en.getValue());
			}
			if (includeRawVals) {
				System.out.println("Data:");
				for (Entry<String, DataCollection> en : out.getDataSets().entrySet()) {
					if (en.getValue().isBinary()) {
						boolean[] vals = en.getValue().getBooleanValues();
						int countTrue = Utils.countTrue(vals);
						System.out.println("--> " + en.getKey() + ": " + countTrue + " / " + vals.length + " ("
								+ (countTrue / (double) vals.length) + ")");

					} else {
						StringBuilder sb = new StringBuilder();
						for (double d : en.getValue().getDoubleValues()) {
							sb.append(d).append(", ");
						}
						System.out.println("--> " + en.getKey() + ": " + sb.toString());
					}
				}
			}
		}
		System.out.println();
		System.out.println("=================================");

	}

	public void write(SheetWriter sw) {
		sw.writeMergedRow(nameOfComparison, 5,  SheetWriter.FONT_TITLE_LARGE);
		sw.setCurrentWidths(Map.of(0,50));
		sw.setCurrentWidths(2, 26, 20);
		sw.writeData(new Object[] { " " }); // print space

		List<Outcome> continuous = outcomes.stream().filter(out -> out.getDataType() == DataType.CONTINUOUS).collect(Collectors.toList());
		List<Outcome> binary = outcomes.stream().filter(out -> out.getDataType() == DataType.DISCRETE_BOOLEAN)
				.collect(Collectors.toList());

		if (!continuous.isEmpty()) {
			printSubList(continuous, sw, "Continuous");
			sw.writeData(new Object[] { " " }); // print space
		}
		
		if (!binary.isEmpty()) {
			printSubList(binary, sw, "Binary");
			sw.writeData(new Object[] { " " }); // print space
		}
		sw.writeData(new Object[] { " " }); // print space

	}


	private void printSubList(List<Outcome> comparisonSubList, SheetWriter sw, String typeOfOutcome) {

		int compSize = comparisonSubList.size();

		// Prepare a map to hold arrays of statistical test values.
		// Each key is a StatTest and its value is an array (one element per outcome).
		LinkedHashMap<StatTest, Double[]> statTestValues = new LinkedHashMap<StatTest, Double[]>();

		// Prepare a map to hold summary statistics.
		// The outer map's key is the name of the summary stat.
		// The inner map maps field names to a 2D Object array (each row corresponding
		// to an outcome).
		LinkedHashMap<String, SummaryStats[]> groups = new LinkedHashMap<String, SummaryStats[]>();

		for (StatTest test : StatTest.values()) {
			if (comparisonSubList.stream().anyMatch(c -> c.getStats().getResult(test) != null)) {
				statTestValues.put(test, new Double[compSize]);
			}
		}

		int counter = 0;

		for (Outcome comp : comparisonSubList) {
			Map<StatTest, Double> statsForComp = comp.getStats().getResults();
			for (Entry<StatTest, Double> entry : statsForComp.entrySet()) {
				statTestValues.get(entry.getKey())[counter] = entry.getValue();
			}
			counter++;
		}

		counter = 0;

		// run thru each outcome, find groups

		for (int i = 0; i < compSize; i++) {
			for (SummaryStats ss : comparisonSubList.get(i).getSummaryStats()) {
				String groupName = ss.getGroupName();
				if (!groups.containsKey(groupName)) {
					groups.put(groupName, new SummaryStats[compSize]);
				}
			}
		}

		// Run through each outcome, and fill existing groups with SummaryStats data,
		// otherwise null
		for (int i = 0; i < compSize; i++) {
			Outcome outcome = comparisonSubList.get(i);
			List<SummaryStats> sumStats = outcome.getSummaryStats();
			final int queryIndex = i;
			groups.forEach((groupName, statArray) -> statArray[queryIndex] = sumStats.stream()
					.filter(s -> s.getGroupName().equals(groupName)).findFirst().orElse(null));
		}

		// Confirm all SummaryStats within a given group have the same type
		groups.forEach((group, statsArray) -> {
			// Confirm that all SummaryStats in the group have the same data type
			List<DataType> distinctTypes = Arrays.stream(statsArray).filter(Objects::nonNull)
					.map(SummaryStats::getDataType).distinct().collect(Collectors.toList());
			if (distinctTypes.size() > 1) {
				for (SummaryStats ss : statsArray) {
					System.out.println(ss.getGroupName() + ": " + ss.getDataType());
				}
				throw new IllegalStateException("Inconsistent data types found in group: " + group);
			}
		});

		ArrayList<Object> topHeader = new ArrayList<Object>();
		ArrayList<Object> bottomHeader = new ArrayList<Object>();
		topHeader.add(" ");
		bottomHeader.add("Outcome - " + typeOfOutcome);
		topHeader.add("   ");
		bottomHeader.add("   ");
		for (StatTest st : statTestValues.keySet()) {
			bottomHeader.add(st.toString());
		}
		int numToAdd = bottomHeader.size() - topHeader.size();
		numToAdd--;
		topHeader.add("Stats");

		for (int i = 1; i <= numToAdd; i++) {
			topHeader.add(" ");
		}

		topHeader.add(" ");
		bottomHeader.add(" ");

		// stop added

		for (Entry<String, SummaryStats[]> en : groups.entrySet()) {
			List<String> groupWidths = getMaximumFieldsNames(en.getValue());
			topHeader.add(en.getKey()); // first cell is the group name
			// Add blanks for the rest of the cells
			if (groupWidths.size() > 1) {
				topHeader.addAll(Collections.nCopies(groupWidths.size() - 1, " "));
			}
			bottomHeader.addAll(groupWidths);

		}
		int[] boldDesignation = new int[topHeader.size()];
		for (int i = 0; i < boldDesignation.length; i++) {
			boldDesignation[i] = SheetWriter.FONT_BOLD;
		}
		sw.writeData(topHeader.toArray(new Object[0]), boldDesignation);
		sw.writeData(bottomHeader.toArray(new Object[0]), boldDesignation);

		// done with headers

		counter = 0;
		for (Outcome comp : comparisonSubList) {
			ArrayList<Object> row = new ArrayList<Object>();
			ArrayList<Integer> rowFormat = new ArrayList<Integer>();
			row.add(comp.getName());
			rowFormat.add(SheetWriter.FONT_MAIN);
			row.add(" ");
			rowFormat.add(SheetWriter.FONT_MAIN);

			for (Entry<StatTest, Double[]> en : statTestValues.entrySet()) {

				Double value = en.getValue()[counter];
				if (value == null) {
					row.add("--");
					rowFormat.add(SheetWriter.FONT_MAIN);

				} else if (Double.isNaN(value)) {

					row.add("n too small");
					rowFormat.add(SheetWriter.FONT_MAIN);

				} else {

					row.add(value);
					if (value < 0.05) {
						rowFormat.add(SheetWriter.FONT_GREEN);
					} else {
						rowFormat.add(SheetWriter.FONT_MAIN);
					}
				}

			}

			row.add(" ");
			rowFormat.add(SheetWriter.FONT_MAIN);

			for (Entry<String, SummaryStats[]> sum : groups.entrySet()) {
				
				int numberNeededToFill = getMaximumFieldsSize(sum.getValue());
				SummaryStats ss = sum.getValue()[counter];
				if (ss != null) {
					for (Object value : ss.getFieldValues().values()) {
						row.add(value);
						rowFormat.add(SheetWriter.FONT_MAIN);
						numberNeededToFill--;
					}
				}
				for (int i = numberNeededToFill; i > 0; i--) {
					row.add(" ");
					rowFormat.add(SheetWriter.FONT_MAIN);

				}

			}

			sw.writeData(row.toArray(new Object[0]), Utils.toPrimitiveInteger(rowFormat));
			counter++;
		}

	}

	public int getMaximumFieldsSize(SummaryStats[] sumStats) {
		SummaryStats sampleStat = Arrays.stream(sumStats).filter(Objects::nonNull).findFirst().orElse(null);
		List<String> fields = sampleStat != null ? sampleStat.getFields() : Collections.emptyList();
		return fields.size();
	}
	
	public List<String> getMaximumFieldsNames(SummaryStats[] sumStats) {
		SummaryStats sampleStat = Arrays.stream(sumStats).filter(Objects::nonNull).findFirst().orElse(null);
		List<String> fields = sampleStat != null ? sampleStat.getFields() : Collections.emptyList();
		return fields;
	}
//
//	public String[][] getReport() {
//
//		ArrayList<String[]> rows = new ArrayList<String[]>();
//		rows.add(new String[] { nameOfComparison });
//
//		// Builder headers
//		ArrayList<String> headerGroups = new ArrayList<String>();
//
//		ArrayList<String> headerColumnName = new ArrayList<String>();
//		headerGroups.add("");
//		headerColumnName.add("Outcome");
//
//		for (StatTest st : StatTest.values()) {
//			headerGroups.add("");
//			headerColumnName.add(st.toString());
//		}
//
//		headerGroups.add("");
//		headerColumnName.add("");
//
//		InsensitiveNonDupList groups = new InsensitiveNonDupList();
//
//		for (Outcome outcome : outcomes) {
//			for (String group : outcome.getGroups()) {
//				groups.add(group);
//			}
//		}
//
//		for (String group : groups) {
//			boolean first = true;
//			for (OutcomeParameter outParam : OutcomeParameter.values()) {
//				if (first) {
//					first = false;
//					headerGroups.add(group);
//				} else {
//					headerGroups.add("");
//				}
//				headerColumnName.add(outParam.toString());
//			}
//		}
//
//		rows.add(headerGroups.toArray(new String[0]));
//		rows.add(headerColumnName.toArray(new String[0]));
//
//		for (Outcome outcome : outcomes) {
//			ArrayList<String> outcomeString = new ArrayList<String>();
//			outcomeString.add(outcome.getName());
//			Map<StatTest, String> statsValues = outcome.getArrayOfStatsStringAll();
//			for (StatTest statTest : StatTest.values()) {
//				String value = statsValues.get(statTest);
//				if (value == null)
//					value = "--";
//				outcomeString.add(value);
//			}
//			outcomeString.add("");
//			TreeMap<String, Map<OutcomeParameter, String>> parameterValues = outcome.getArrayOfData();
//
//			for (String group : groups) {
//				Map<OutcomeParameter, String> params = parameterValues.get(group);
//
//				for (OutcomeParameter outParam : OutcomeParameter.values()) {
//					String paramValue = params == null ? null : params.get(outParam);
//					if (paramValue == null || paramValue.isBlank()) {
//						paramValue = "--";
//					}
//					outcomeString.add(paramValue);
//				}
//			}
//
//			rows.add(outcomeString.toArray(new String[0]));
//		}
//
//		return rows.toArray(new String[0][]);
//
//	}

}
