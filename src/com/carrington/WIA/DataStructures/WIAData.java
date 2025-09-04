package com.carrington.WIA.DataStructures;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.DoubleStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

import com.carrington.WIA.Utils;
import com.carrington.WIA.Cardio.Wave;
import com.carrington.WIA.Cardio.Wave.WaveClassification;
import com.carrington.WIA.IO.Header;

/**
 * An object which carries wave intensity analysis data. This object can be
 * serialized to a file and later reopened.
 * <p>
 * It performs analysis on the provided HemoData, calculating metrics such as
 * wave speed, separated wave intensity, flow derivatives, and resistance. It
 * also supports serialization/deserialization, and allows updating of the raw
 * data.
 * </p>
 */
public class WIAData implements Serializable {

	private static final long serialVersionUID = 7046660855495204932L;

	//////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE - START - DO NOT EDIT
	//
	//////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////

	private String selectionName;
	private HemoData rawData;
	// by default, this data is kept in milliseconds for easy visual representation
	private HemoData sepWaveIntensity;
	private HemoData netWaveIntensity;
	private HemoData sepFlowDeriv;
	private double waveSpeedC;
	private double rho;
	private double flowAvg; // m/s
	private double pressureAvg; // mmHg

	private Double systoleTime = Double.NaN;
	private Double systolePressure = Double.NaN;
	private Double systoleFlow = Double.NaN;
	private Double diastoleTime = Double.NaN;
	private Double diastolePressure = Double.NaN;
	private Double diastoleFlow = Double.NaN;

	private Double resistCycle = Double.NaN;
	private Double resistSystole = Double.NaN;
	private Double resistDiastole = Double.NaN;

	private double cumulativeWIForward = Double.NaN;
	private double cumulativeWIBackward = Double.NaN;
	private double cumulativeWINet = Double.NaN;

	private final Set<Wave> waves = new HashSet<Wave>();

	///////////////////////////////////
	//
	// ADDED VALUES
	//
	///////////////////////////////////

	private Double vesselDiameter = null;
	/** Represents the original data before any shifting **/
	private HemoData originalData = null;
	private Double cycleEndManual = null; // TODO: could consider a smarter version where this is automatic

	//////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE - END - DO NOT EDIT
	//
	//////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////

	// These are calculated fields with the methods below. They are not stored in
	// the serialized object. This is because they are calculated based on multiple
	// WIAData objects together
	private transient File deserializedFile = null;
	private transient Double cfr = null;
	private transient Double percIncACh = null;
	private transient Double hMR = null;
	private transient Double rMR = null; // resting microvascular resistance

	/**
	 * constant denoting location of diameter of coronary artery used for
	 * calculation of flow
	 */
	public static transient final String DIAMETER_MID = "Mid";
	/** Constant used for calculation. Units are kg * m^-3 */
	private static final double density = 1050.0d;

	/**
	 * Constructs a new {@link WIAData} object. It will then run analysis via
	 * {@link WIAData#runAnalysis()}
	 * 
	 * @param selectionName the name of this selection (e.g., "adenosine")
	 * @param rawData       the {@link HemoData} used to perform wave intensity
	 *                      calculations
	 * @throws IllegalArgumentException if no pressure or flow data is flagged in
	 *                                  the supplied {@link HemoData}
	 */
	public WIAData(String selectionName, HemoData rawData) throws IllegalArgumentException {
		this.selectionName = selectionName;
		this.rawData = rawData;

		_initFieldsCurrData();
		runAnalysis();

	}

	/**
	 * Adds the specified wave
	 *
	 * @return true if the wave is added, false if not (in which the case a wave
	 *         with the same name has already been added.
	 */
	public boolean addWave(Wave wave) {
		return this.waves.add(wave);
	}

	/**
	 * @return file name, which is just the file name stored in the {@link HemoData}
	 *         object.
	 */
	public String getFileName() {
		return rawData.getFile().getPath();
	}

	/**
	 * @return this object's name
	 */
	public String getSelectionName() {
		return this.selectionName;
	}

	/**
	 * Sets the new name for this {@link WIAData} object.
	 * 
	 * @param name new name
	 * @throws IllegalArgumentException if the supplied name is null or blank
	 */
	public void setSelectionName(String name) throws IllegalArgumentException {
		if (name == null || name.length() == 0)
			throw new IllegalArgumentException("Invalid name for WIAData - blank or null");

		selectionName = name;

		// also set name in data files
		rawData.setName(name);
		if (originalData != null) {
			originalData.setName(name);
		}
	}

	/**
	 * Removes the specified {@link Wave} from this object, if it exists.
	 * 
	 * @param wave the {@link Wave} to remove
	 */
	public void removeWave(Wave wave) {
		this.waves.remove(wave);
	}

	/**
	 * Clears all waves.
	 */
	public void clearWaves() {
		this.waves.clear();
	}

	/**
	 * Checks if the specified {@link Wave} exists. Note, in the process of checking
	 * for the {@link Wave}, it uses the overridden equality method in {@link Wave}.
	 * This doesn't actually evaluate if it is the same object, just if the wave
	 * name is the same. Thus it looks for a matching wave (but not necessarily the
	 * same wave object).
	 * 
	 * @param wave the {@link Wave} to evaluate
	 * @return true if contains the wave
	 */
	public boolean containsMatchingWave(Wave wave) {
		return this.waves.contains(wave); // does not hash on the time frame, so will return true just
											// the name is equal
	}

	/**
	 * Gets the {@link HemoData} that is used to construct this {@link WIAData}
	 * obejct
	 * 
	 * @return the set of data used for wave intensity analysis
	 */
	public HemoData getData() {
		return this.rawData;
	}

	/**
	 * Gets the wave speed. This is a calculated field. Thus, if
	 * {@link #setWaveSpeed(double)} was not called previously, then the result of
	 * this method will be null.
	 * 
	 * @return the wave speed, C, or {@link Double#NaN} if not set.
	 */
	public double getWaveSpeed() {
		return this.waveSpeedC;
	}

	/**
	 * Sets the new wave speed C, which is a calculated field.
	 * 
	 * @param waveSpeedC the wave speed
	 */
	public void setWaveSpeed(double waveSpeedC) {
		this.waveSpeedC = waveSpeedC;
	}

	/**
	 * Gets the rho value. This is a calculated field. Thus, if
	 * {@link #setRho(double)} was not called previously, then the result of this
	 * method will be null.
	 * 
	 * @return the rho value, or {@link Double#NaN} if not set.
	 */
	public double getRho() {
		return this.rho;
	}

	/**
	 * Sets the rho value, which is a calculated field.
	 * 
	 * @param rho the rho value
	 */
	public void setRho(double rho) {
		this.rho = rho;
	}

	/**
	 * Returns the time array from the raw data.
	 *
	 * @return an array of time values
	 */
	public double[] getTime() {
		return this.rawData.getXData();
	}

	/**
	 * Returns the raw pressure data.
	 *
	 * @return an array of pressure values
	 */
	public double[] getRawPressure() {
		return this.rawData.getYData(rawData.getHeaderByFlag(HemoData.TYPE_PRESSURE).get(0));
	}

	/**
	 * Returns the raw flow data.
	 *
	 * @return an array of flow values
	 */
	public double[] getRawFlow() {

		return this.rawData.getYData(rawData.getHeaderByFlag(HemoData.TYPE_FLOW).get(0));
	}

	/**
	 * 
	 * @param mmHg if pressure should be in mmHg. Otherwise will be Pascals
	 * @return average pressure.
	 */
	public double getAvgPressure(boolean mmHg) {
		if (!mmHg) {
			return Utils.convertToPascals(pressureAvg);
		} else {
			return pressureAvg;
		}
	}

	/**
	 * 
	 * @param mmHg if pressure should be in mmHg. Otherwise will be Pascals
	 * @return min pressure
	 */
	public double getMinPressure(boolean mmHg) {
		boolean ismmHg = !rawData.getFlags(rawData.getHeaderByFlag(HemoData.TYPE_PRESSURE).get(0))
				.contains(HemoData.UNIT_PASCAL);
		double min = Utils.min(getRawPressure());
		if (mmHg && !ismmHg) {
			return Utils.convertPascalsToMMHG(min);
		} else if (!mmHg && ismmHg) {
			return Utils.convertToPascals(min);

		}
		return min;
	}

	/**
	 * 
	 * @param mmHg if pressure should be in mmHg. Otherwise will be Pascals
	 * @return max pressure
	 */
	public double getMaxPressure(boolean mmHg) {
		boolean ismmHg = !rawData.getFlags(rawData.getHeaderByFlag(HemoData.TYPE_PRESSURE).get(0))
				.contains(HemoData.UNIT_PASCAL);
		double max = Utils.max(getRawPressure());
		if (mmHg && !ismmHg) {
			return Utils.convertPascalsToMMHG(max);
		} else if (!mmHg && ismmHg) {
			return Utils.convertToPascals(max);

		}
		return max;
	}

	/**
	 * 
	 * @param cms if flow should be in cm/s. Otherwise will be m/s.
	 * @return average flow.
	 */
	public double getAvgFlow(boolean cms) {
		if (cms) {
			return Utils.multiply(flowAvg, 100).doubleValue();
		} else {
			return flowAvg;
		}
	}

	/**
	 * 
	 * @param cms if flow should be in cm/s. Otherwise will be m/s.
	 * @return min flow
	 */
	public double getMinFlow(boolean cms) {
		boolean isCMS = rawData.getFlags(rawData.getHeaderByFlag(HemoData.TYPE_FLOW).get(0))
				.contains(HemoData.UNIT_CMperS);
		double min = Utils.min(getRawFlow());
		if (cms && !isCMS) {
			return Utils.multiply(min, 100).doubleValue();
		} else if (!cms && isCMS) {
			return Utils.divide(min, 100).doubleValue();

		}

		return min;
	}

	/**
	 * 
	 * @param cms if flow should be in cm/s. Otherwise will be m/s.
	 * @return max flow
	 */
	public double getMaxFlow(boolean cms) {
		boolean isCMS = rawData.getFlags(rawData.getHeaderByFlag(HemoData.TYPE_FLOW).get(0))
				.contains(HemoData.UNIT_CMperS);
		double max = Utils.max(getRawFlow());
		if (cms && !isCMS) {
			return Utils.multiply(max, 100).doubleValue();
		} else if (!cms && isCMS) {
			return Utils.divide(max, 100).doubleValue();

		}

		return max;
	}

	/**
	 * Returns the calculated flow derivative.
	 *
	 * @return an array of flow derivative values
	 * @throws IllegalStateException if the flow derivative has not been calculated
	 */
	public double[] getFlowDeriv() {
		if (!this.rawData.isDerivativeCalculated(rawData.getHeaderByFlag(HemoData.TYPE_FLOW).get(0))) {
			throw new IllegalStateException("Flow derivative not calculated.");
		}
		return this.rawData.getCalculatedDeriv(rawData.getHeaderByFlag(HemoData.TYPE_FLOW).get(0));
	}

	/**
	 * Returns the calculated pressure derivative.
	 *
	 * @return an array of pressure derivative values
	 * @throws IllegalStateException if the pressure derivative has not been
	 *                               calculated
	 */
	public double[] getPressureDeriv() {
		if (!this.rawData.isDerivativeCalculated(rawData.getHeaderByFlag(HemoData.TYPE_PRESSURE).get(0))) {
			throw new IllegalStateException("Pressure derivative not calculated.");
		}
		return this.rawData.getCalculatedDeriv(rawData.getHeaderByFlag(HemoData.TYPE_PRESSURE).get(0));
	}

	/**
	 * Returns the net wave intensity.
	 *
	 * @return an array of net wave intensity values
	 * @throws IllegalStateException if the net wave intensity has not been stored
	 */
	public double[] getNetWaveIntensity() {
		if (this.netWaveIntensity == null) {
			throw new IllegalStateException("Net wave intenstiy not yet stored.");
		}
		return this.netWaveIntensity.getYData(netWaveIntensity.getHeaderByFlag(HemoData.TYPE_NET_WIA).get(0));
	}

	/**
	 * Returns the separated forward flow derivative.
	 *
	 * @return an array of separated forward flow derivative values
	 * @throws IllegalStateException if the separated flow derivative data has not
	 *                               been stored
	 */
	public double[] getSepFlowForwardDeriv() {
		if (this.sepFlowDeriv == null) {
			throw new IllegalStateException("Separated flow not yet stored.");
		}
		Header flowSepHeaderForw = sepFlowDeriv.getHeaderByFlag(HemoData.TYPE_SEP_FLOW_DERIV_FORW).get(0);

		return sepFlowDeriv.getYData(flowSepHeaderForw);
	}

	/**
	 * Returns the separated backward flow derivative.
	 *
	 * @return an array of separated backward flow derivative values
	 * @throws IllegalStateException if the separated flow derivative data has not
	 *                               been stored
	 */
	public double[] getSepFlowBackwardDeriv() {
		if (this.sepFlowDeriv == null) {
			throw new IllegalStateException("Separated flow not yet stored.");
		}
		Header flowSepHeaderBack = sepFlowDeriv.getHeaderByFlag(HemoData.TYPE_SEP_FLOW_DERIV_BACK).get(0);

		return sepFlowDeriv.getYData(flowSepHeaderBack);
	}

	/**
	 * Returns the separated forward wave intensity.
	 *
	 * @return an array of separated forward wave intensity values
	 * @throws IllegalStateException if the separated wave intensity data has not
	 *                               been stored
	 */
	public double[] getWIForward() {
		if (this.sepWaveIntensity == null) {
			throw new IllegalStateException("Separated wave intensity not yet stored.");
		}
		Header wisForward = this.sepWaveIntensity.getHeaderByFlag(HemoData.TYPE_SEP_WIA_FORW).get(0); // we know it
																										// exists
																										// because we
																										// validated on
																										// init
		return this.sepWaveIntensity.getYData(wisForward);
	}

	/**
	 * Returns the separated backward wave intensity.
	 *
	 * @return an array of separated backward wave intensity values
	 * @throws IllegalStateException if the separated wave intensity data has not
	 *                               been stored
	 */
	public double[] getWIBackward() {
		if (this.sepWaveIntensity == null) {
			throw new IllegalStateException("Separated wave intensity not yet stored.");
		}
		Header wisBackward = this.sepWaveIntensity.getHeaderByFlag(HemoData.TYPE_SEP_WIA_BACK).get(0); // we know it
																										// exists
																										// because we
																										// validated on
																										// init
		return this.sepWaveIntensity.getYData(wisBackward);
	}

	/**
	 * @return resistance during systole. Will return NaN is the time of systole or
	 *         diastole was not selected, or if the {@link #calculateResistance()}
	 *         method was not called previously.
	 */
	public Double getResistanceSystole() {
		return this.resistSystole;
	}

	/**
	 * @return resistance during diastole. Will return NaN is the time of diastole
	 *         was not selected, or if the {@link #calculateResistance()} method was
	 *         not called previously.
	 */
	public Double getResistanceDiastole() {
		return this.resistDiastole;
	}

	/**
	 * Uses the average flow and pressure across whole cycle.
	 * 
	 * @return overall resistance
	 */
	public double getResistanceOverall() {
		return this.resistCycle;

	}

	/**
	 * Returns the set of waves used in this analysis.
	 *
	 * @return a set of {@link Wave} objects
	 */
	public Set<Wave> getWaves() {
		return this.waves;
	}

	/**
	 * Checks if a wave with the specified name exists.
	 *
	 * @param name          the name of the wave
	 * @param caseSensitive if true the check is case sensitive; otherwise case
	 *                      insensitive
	 * @return true if a matching wave exists
	 */
	public boolean containsWave(String name, boolean caseSensitive) {
		if (caseSensitive) {
			return waves.stream().anyMatch(wave -> wave.getAbbrev().equals(name));
		} else {
			return waves.stream().anyMatch(wave -> wave.getAbbrev().equalsIgnoreCase(name));
		}
	}

	/**
	 * Returns the cumulative forward wave intensity.
	 *
	 * @return the cumulative forward intensity
	 */
	public double getCumWIForward() {
		return this.cumulativeWIForward;
	}

	/**
	 * Returns the cumulative backward wave intensity.
	 *
	 * @return the cumulative backward intensity
	 */
	public double getCumWIBackward() {
		return this.cumulativeWIBackward;
	}

	/**
	 * Returns the cumulative net wave intensity.
	 *
	 * @return the cumulative net intensity
	 */
	public double getCumWINet() {
		return this.cumulativeWINet;
	}

	/**
	 * Checks if the user selection is adequate.
	 * <p>
	 * By default, this method returns true if at least two waves have been added.
	 * </p>
	 *
	 * @return true if the user selection is adequate for analysis
	 */
	public boolean isUserSelectionAdequate() {
		return this.waves.size() >= 2;
	}

	/**
	 * Sets the diastolic point based on the specified time index.
	 *
	 * @param index the index in the time array corresponding to diastole; if
	 *              negative, diastolic values are reset to NaN
	 */
	public void setDiastoleByTimeIndex(int index) {
		if (index < 0) {
			this.diastoleTime = Double.NaN;
			this.diastoleFlow = Double.NaN;
			this.diastolePressure = Double.NaN;
			return;
		}
		this.diastoleTime = getTime()[index];
		this.diastoleFlow = getRawFlow()[index];
		this.diastolePressure = Utils.convertPascalsToMMHG(getRawPressure()[index]);
	}

	/**
	 * Sets the systolic point based on the specified time index.
	 *
	 * @param index the index in the time array corresponding to systole; if
	 *              negative, systolic values are reset to NaN
	 */
	public void setSystoleByTimeIndex(int index) {
		if (index < 0) {
			this.systoleTime = Double.NaN;
			this.systoleFlow = Double.NaN;
			this.systolePressure = Double.NaN;
			return;
		}
		this.systoleTime = getTime()[index];
		this.systoleFlow = getRawFlow()[index];
		this.systolePressure = Utils.convertPascalsToMMHG(getRawPressure()[index]);
	}

	/**
	 * Returns the systolic time.
	 *
	 * @return the systolic time, or {@link Double#NaN} if not set
	 */
	public Double getSystoleTime() {
		return this.systoleTime;
	}

	/**
	 * Returns the diastolic time.
	 *
	 * @return the diastolic time, or {@link Double#NaN} if not set
	 */
	public Double getDiastoleTime() {
		return this.diastoleTime;
	}

	/**
	 * Sets the cycle end by the index of the corresponding value in the time array.
	 * If outside of the array of time, will reset the value
	 * 
	 * @param index the index of end of cycle (inclusive)
	 */
	public void setManualCycleEndIndex(int index) {
		if (index < 0 || index >= getTime().length) {
			cycleEndManual = null;
			return;
		}
		cycleEndManual = getTime()[index];
	}

	/**
	 * Gets the cycle end by the index of the corresponding value in the time array.
	 * 
	 * @return the time (ms) of cycle end, or null if not picked
	 */
	public Double getManualCycleEnd() {
		return cycleEndManual;
	}

	/**
	 * Calculates the diastole duration. If systole or diastole are not set, will
	 * return null. This also assumes the data are 1 cardiac cyce, or if not the
	 * user has set a custom cycle end with {@link #setManualCycleEndIndex(int)}
	 * 
	 * @return diastole duration in milliseconds
	 */
	public Double getDiastoleDuration() {

		if (!isValidDouble(systoleTime, diastoleTime)) {
			// systole or diastole was not set by the user
			return null;
		}
		double[] time = getTime();
		double end = isValidDouble(cycleEndManual) ? cycleEndManual : time[time.length - 1];

		if (systoleTime > end && diastoleTime > end) {
			return null;
		}

		double diastoleDuration;
		if (diastoleTime > systoleTime) {
			diastoleDuration = (end - diastoleTime) + (systoleTime - time[0]);
		} else {
			diastoleDuration = systoleTime - diastoleTime;

		}

		return diastoleDuration;
	}

	/**
	 * Returns the time from onsets of diastole to peak flow. If systole or diastole
	 * are not set, will return null. This also assumes the data are 1 cardiac cyce,
	 * or if not the user has set a custom cycle end with
	 * {@link #setManualCycleEndIndex(int)}
	 *
	 * @return the duration
	 */
	public Double getDiastoleToFlowPeakDuration() {

		if (!isValidDouble(systoleTime, diastoleTime)) {
			// systole or diastole was not set by the user
			return null;
		}

		double[] flow = getRawFlow();
		double[] time = getTime();
		int indexSystole = Utils.getClosestIndex(systoleTime, time);
		int indexDiastole = Utils.getClosestIndex(diastoleTime, time);
		int indexEnd = isValidDouble(cycleEndManual) ? Utils.getClosestIndex(cycleEndManual, time) : flow.length - 1;

		if (indexSystole > indexEnd && indexDiastole > indexEnd) {
			return null;
		}

		double duration;

		if (diastoleTime > systoleTime) {
			int potentialMaxIndex1 = Utils.getIndexOfMax(flow, indexDiastole, indexEnd);
			int potentialMaxIndex2 = Utils.getIndexOfMax(flow, 0, indexSystole);
			if (flow[potentialMaxIndex1] > flow[potentialMaxIndex2]) {
				duration = time[potentialMaxIndex1] - diastoleTime;
			} else {
				duration = (time[indexEnd] - diastoleTime) + (time[potentialMaxIndex2] - time[0]);
			}

		} else {
			int maxIndex = Utils.getIndexOfMax(flow, indexDiastole, indexSystole);
			duration = time[maxIndex] - diastoleTime;

		}

		return duration;
	}

	/**
	 * Calculates the cycle length in milliseconds. Several checks are performed
	 * first:
	 * 
	 * <ol>
	 * <li>If cycle duration is < 250 ms despite above checks, returns null
	 * </ol>
	 * 
	 * @return cycle length or null if cannot calculate
	 */
	public Double getCycleDuration() {

		double[] time = getTime();
		double timeEnd = isValidDouble(cycleEndManual) ? cycleEndManual : time[time.length - 1];
		double cycleLength = timeEnd - time[0];

		if (cycleLength < 250)
			return null;

		return cycleLength;
	}

	/**
	 * Returns a CSV representation of the analysis data.
	 *
	 * @param name an optional name to include in the CSV data; if null, an empty
	 *             string is used
	 * @return a two-dimensional String array representing CSV rows and columns
	 */
	public String[][] toCSV(String name) {
		if (name == null)
			name = "";

		String[] avgRowHeaders = new String[] { "File Name", "Selection Name", "Wave Speed (C) m/s",
				"Avg Pressure (mmHg)", "Max Pressure (mmHg)", "Min Pressure (mmHg)", "Avg Flow (m/s)", "Max Flow (m/s)",
				"Min Flow (m/s)", "Systole Time", "Systole Pressure", "Systole Flow", "Diastole Time",
				"Diastole Pressure", "Diastole Flow", "Avg Resist (mmHg/cm/s)", "Systole Resist (mmHg/cm/s)",
				"Diastole Resist (mmHg/cm/s)", "Cumulative Net", "Cumulative Forw", "Cumulative Back" };

		double maxPressure = Utils.convertPascalsToMMHG(Utils.max(getRawPressure()));
		double minPressure = Utils.convertPascalsToMMHG(Utils.min(getRawPressure()));
		double maxFlow = Utils.max(getRawFlow());
		double minFlow = Utils.min(getRawFlow());
		String[] avgRow = new String[] { FilenameUtils.removeExtension(rawData.getFileName()), name,
				Double.toString(waveSpeedC), Double.toString(pressureAvg), Double.toString(maxPressure),
				Double.toString(minPressure), Double.toString(flowAvg), Double.toString(maxFlow),
				Double.toString(minFlow), Double.toString(systoleTime), Double.toString(systolePressure),
				Double.toString(systoleFlow), Double.toString(diastoleTime), Double.toString(diastolePressure),
				Double.toString(diastoleFlow), Double.toString(resistCycle), Double.toString(resistSystole),
				Double.toString(resistDiastole), Double.toString(cumulativeWINet), Double.toString(cumulativeWIForward),
				Double.toString(cumulativeWIBackward) };
		String[] spacerRow2 = new String[] { "" };

		ArrayList<String> rowOfDataHeaders = new ArrayList<String>();

		ArrayList<String> rowOfData = new ArrayList<String>();
		for (WaveClassification waveType : WaveClassification.getWavesTypesOrdered()) {

			if (waveType.equals(WaveClassification.OTHER)) {
				List<Wave> otherWavesAlphabetize = new ArrayList<Wave>();

				for (Wave wave : waves) {
					if (wave.getType().equals(WaveClassification.OTHER)) {
						otherWavesAlphabetize.add(wave);
					}
				}

				otherWavesAlphabetize.sort(new Comparator<Wave>() {
					@Override
					public int compare(Wave o1, Wave o2) {
						return o1.getAbbrev().compareTo(o2.getAbbrev());
					}
				});

				// add ALL waves designated as OTHER

				for (Wave wave : otherWavesAlphabetize) {
					rowOfDataHeaders.add(wave.getAbbrev() + " Cumul");
					rowOfDataHeaders.add(wave.getAbbrev() + " Cumul Ratio");

					rowOfDataHeaders.add(wave.getAbbrev() + " Peak");
					rowOfDataHeaders.add(wave.getAbbrev() + " Peak Time");
					rowOfData.add(wave.getCumulativeIntensity() + "");
					if (wave.isProximal()) {
						if (!Double.isNaN(cumulativeWIForward)) {
							rowOfData.add((wave.getCumulativeIntensity() / cumulativeWIForward) + "");
						} else {
							rowOfData.add("NaN");
						}
					} else {
						if (!Double.isNaN(cumulativeWIBackward)) {
							rowOfData.add((wave.getCumulativeIntensity() / cumulativeWIBackward) + "");
						} else {
							rowOfData.add("NaN");
						}
					}
					rowOfData.add(wave.getPeak() + "");
					rowOfData.add(wave.getPeakTime() + "");

				}
			} else {

				boolean foundWave = false;

				for (Wave wave : waves) {

					if (wave.getType().equals(waveType)) {
						rowOfDataHeaders.add(waveType.abbrev() + " Cumul");
						rowOfDataHeaders.add(waveType.abbrev() + " Cumul Ratio");
						rowOfDataHeaders.add(waveType.abbrev() + " Peak");
						rowOfDataHeaders.add(waveType.abbrev() + " Peak Time");
						rowOfData.add(wave.getCumulativeIntensity() + "");
						if (wave.isProximal()) {
							if (!Double.isNaN(cumulativeWIForward)) {
								rowOfData.add((wave.getCumulativeIntensity() / cumulativeWIForward) + "");
							} else {
								rowOfData.add("NaN");
							}
						} else {
							if (!Double.isNaN(cumulativeWIBackward)) {
								rowOfData.add((wave.getCumulativeIntensity() / cumulativeWIBackward) + "");
							} else {
								rowOfData.add("NaN");
							}
						}
						rowOfData.add(wave.getPeak() + "");
						rowOfData.add(wave.getPeakTime() + "");
						foundWave = true;
						// we will only add data for the first wave designated by this type. Really
						// there should only be one.
						break;
					}

				}

				if (!foundWave) {
					// did not find the wave, enter empty data
					rowOfDataHeaders.add(waveType.abbrev() + " Cumulative");
					rowOfDataHeaders.add(waveType.abbrev() + " Cumul Ratio");
					rowOfDataHeaders.add(waveType.abbrev() + " Peak");
					rowOfDataHeaders.add(waveType.abbrev() + " Peak Time");
					rowOfData.add("");
					rowOfData.add("");
					rowOfData.add("");
					rowOfData.add("");
				}
			}

		}

		return new String[][] { avgRowHeaders, avgRow, spacerRow2, rowOfDataHeaders.toArray(new String[0]),
				rowOfData.toArray(new String[0]) };

	}

	/**
	 * Recalculates the wave peaks and resistance.
	 */
	public void retryCalculations() {
		calculateWavePeaksAndSum();
		calculateResistance();
	}

	/**
	 * Calculates the peaks and cumulative intensities for each wave.
	 * <p>
	 * For each wave, this method extracts the corresponding segment of the
	 * separated wave intensity data, computes the area under the curve, and
	 * determines the peak value and its time.
	 * </p>
	 */
	public void calculateWavePeaksAndSum() {
		for (Wave wave : waves) {

			calculateWavePeaksAndSum(wave);

		}
	}

	/**
	 * Calculates the peaks and cumulative intensities for a wave.
	 */
	public void calculateWavePeaksAndSum(Wave wave) {
		double[] waveIntensity;
		if (wave.isProximal()) {
			waveIntensity = this.sepWaveIntensity
					.getYData(sepWaveIntensity.getHeaderByFlag(HemoData.TYPE_SEP_WIA_FORW).get(0));

		} else {
			waveIntensity = this.sepWaveIntensity
					.getYData(sepWaveIntensity.getHeaderByFlag(HemoData.TYPE_SEP_WIA_BACK).get(0));

		}

		int[] waveBoundIndices = wave.getBoundsTimeIndex();
		double[] subWaveIntensity = ArrayUtils.subarray(waveIntensity, waveBoundIndices[0], waveBoundIndices[1]);

		BigDecimal timeIntervalS = new BigDecimal(
				HemoData.calculateAverageInterval(sepWaveIntensity.convertXUnitsCopy(HemoData.UNIT_SECONDS)));

		wave.setCumulativeIntensity(Utils.getAreaUnderCurve(timeIntervalS, subWaveIntensity));
		double[] peakData = Utils.absoluteMax(
				ArrayUtils.subarray(sepWaveIntensity.getXData(), waveBoundIndices[0], waveBoundIndices[1]),
				subWaveIntensity);
		wave.setPeakTime(peakData[0]);
		wave.setPeak(peakData[1]);
	}

	/**
	 * Calculates the resistance based on average, systolic, and diastolic values.
	 * <p>
	 * Uses the average flow (converted to cm/s) and pressure to compute the overall
	 * resistance. If systolic/diastolic data is missing, their resistances are set
	 * to NaN.
	 * </p>
	 *
	 * @return an error message if resistance selection was incomplete; otherwise,
	 *         null
	 */
	public String calculateResistance() {

		// by default, flowAvg in the class is stored in M/S, must be CM/S for
		// resistance

		this.resistCycle = this.pressureAvg
				/ (this.flowAvg == 0 ? 0.00001 : Utils.multiply(this.flowAvg, 100).doubleValue());

		String errors = null;
		if (Double.isNaN(this.systolePressure) || Double.isNaN(this.systoleFlow) || Double.isNaN(this.diastolePressure)
				|| Double.isNaN(this.diastoleFlow)) {
			errors = "Resistance not selected by user.";
			this.resistSystole = Double.NaN;
			this.resistDiastole = Double.NaN;
		} else {
			this.resistSystole = this.systolePressure
					/ (this.systoleFlow == 0 ? 0.00001 : Utils.multiply(this.systoleFlow, 100).doubleValue());
			this.resistDiastole = this.diastolePressure
					/ (this.diastoleFlow == 0 ? 0.00001 : Utils.multiply(this.diastoleFlow, 100).doubleValue());
		}
		return errors;

	}

	/**
	 * Returns the file from which this {@link WIAData} was deserialized.
	 *
	 * @return the source file if deserialized; null otherwise
	 */
	public File getSerializeFileSource() {
		return deserializedFile;
	}

	/**
	 * Sets the CFR, and whether there is CMD based on the provided threshold (<
	 * threshold means CMD, usually 2.5)
	 * 
	 */
	public void setCFR(double CFR) {
		this.cfr = CFR;
	}

	/**
	 * @return CFR, if set by {@link #setCFR(double)}. Otherwise, null.
	 */
	public Double getCFR() {
		return this.cfr;
	}

	/**
	 * Sets the percent response to ACh compared to rest
	 * 
	 * @param percIncACh response of flow to ACh in %
	 */
	public void setPercIncACh(double percIncACh) {
		this.percIncACh = percIncACh;
	}

	/**
	 * @return Percent increase in flow with acetylcholine, if set by
	 *         {@link #setPercIncACh(double)}. Otherwise, null.
	 */
	public Double getPercIncACh() {
		return this.percIncACh;
	}

	/**
	 * 
	 * @return vessel diameter
	 */
	public Double getVesselDiameter() {
		return vesselDiameter;
	}

	/**
	 * 
	 * @param diameter the diameter at specified location
	 */
	public void setVesselDiameter(Double diameter) {
		if (diameter == null || diameter.isNaN()) {
			throw new IllegalArgumentException("Invalid null / NaN argument");
		}
		this.vesselDiameter = diameter;
	}

	/**
	 * Checks if diameter has been stored in this object
	 * 
	 * @return true if there is a non-null diameter value at the specified location
	 */
	public boolean hasVesselDiameter() {
		return vesselDiameter != null && !vesselDiameter.isNaN();
	}

	/**
	 * @return true if there is a separate store original data (i.e. current data is
	 *         modified version of the original)
	 */
	public boolean hasOriginal() {
		return originalData != null;
	}

	/**
	 * Reverts the current raw data to the originally stored data.
	 * <p>
	 * If an original data copy exists, it replaces the current raw data and re-runs
	 * the analysis.
	 * </p>
	 */
	public void revertToOriginalHemoData() {
		if (this.originalData != null) {
			this.rawData = this.originalData;
			this.originalData = null;

			_initFieldsCurrData();
			runAnalysis();
		}
	}

	/**
	 * Sets new raw data for analysis.
	 * <p>
	 * If this is the first update, the current raw data is stored as the original.
	 * The analysis is then re-performed with the new data.
	 * </p>
	 *
	 * @param newData the new {@link HemoData} to use
	 * @throws IllegalArgumentException if newData is null or missing required
	 *                                  headers
	 */
	public void setNewHemoData(HemoData newData) {
		if (newData == null) {
			throw new IllegalArgumentException("New HemoData cannot be null.");
		}
		// Store the current rawData as originalData if not already stored.
		if (this.originalData == null) {
			this.originalData = this.rawData;
		}
		// Update rawData.
		this.rawData = newData;
		_initFieldsCurrData();
		runAnalysis();
	}

	/**
	 * Initializes and recalculates basic values and clears transient data.
	 * <p>
	 * This method recalculates averages (pressure and flow), resistance, and clears
	 * previously stored wave selections and transient calculated parameters.
	 * </p>
	 *
	 * @throws IllegalArgumentException if the new data is missing pressure or flow
	 *                                  headers
	 */
	private void _initFieldsCurrData() {
		// Recalculate basic averages and resistance.
		List<Header> headerPressure = rawData.getHeaderByFlag(HemoData.TYPE_PRESSURE);
		List<Header> headerFlow = rawData.getHeaderByFlag(HemoData.TYPE_FLOW);
		if (headerPressure.isEmpty()) {
			throw new IllegalArgumentException("New HemoData has no pressure data.");
		}
		if (headerFlow.isEmpty()) {
			throw new IllegalArgumentException("New HemoData has no flow data.");
		}
		Mean mean = new Mean();
		double meanPressure = mean.evaluate(rawData.getYData(headerPressure.get(0)));
		double meanFlow = mean.evaluate(rawData.getYData(headerFlow.get(0)));
		if (rawData.getFlags(headerPressure.get(0)).contains(HemoData.UNIT_PASCAL)) {
			this.pressureAvg = Utils.convertPascalsToMMHG(meanPressure);
		} else {
			this.pressureAvg = meanPressure;
		}
		if (rawData.getFlags(headerFlow.get(0)).contains(HemoData.UNIT_CMperS)) {
			this.flowAvg = Utils.divide(meanFlow, 100).doubleValue();
		} else {
			this.flowAvg = meanFlow;
		}
		resistCycle = pressureAvg / (flowAvg == 0 ? 1.0 : Utils.multiply(flowAvg, 100).doubleValue());

		// clear selected values
		resistDiastole = Double.NaN;
		resistSystole = Double.NaN;
		systoleFlow = Double.NaN;
		systolePressure = Double.NaN;
		systoleTime = Double.NaN;
		diastoleFlow = Double.NaN;
		diastolePressure = Double.NaN;
		diastoleTime = Double.NaN;
		cycleEndManual = null;
		waves.clear();

		// Clear transient calculated parameters.
		cfr = null;
		percIncACh = null;
		hMR = null;

		// Clear any previously calculated derived HemoData.
		sepWaveIntensity = null;
		netWaveIntensity = null;
		sepFlowDeriv = null;
		cumulativeWIBackward = Double.NaN;
		cumulativeWIForward = Double.NaN;
		cumulativeWINet = Double.NaN;
		rho = Double.NaN;
		waveSpeedC = Double.NaN;

	}

	/**
	 * Performs the complete analysis on the raw data.
	 * <p>
	 * The analysis consists of converting units, calculating derivatives, computing
	 * wave speed, separated wave intensity, separated flow, cumulative intensities,
	 * and re-calculating wave peaks and resistance. Finally, the raw data's time
	 * units are restored.
	 * </p>
	 *
	 * @throws IllegalStateException if the analysis cannot be performed due to
	 *                               missing data
	 */
	public void runAnalysis() throws IllegalStateException {
		rawData.convertXUnits(HemoData.UNIT_SECONDS); // TODO: make this smarter
		Header headerPressure = rawData.getHeaderByFlag(HemoData.TYPE_PRESSURE).get(0);
		rawData.convertYUnits(headerPressure, HemoData.UNIT_PASCAL);
		rawData.calculateDerivative(headerPressure, null);
		rawData.calculateDiff(headerPressure);
		Header headerFlow = rawData.getHeaderByFlag(HemoData.TYPE_FLOW).get(0);
		rawData.convertYUnits(headerFlow, HemoData.UNIT_MperS);
		rawData.calculateDerivative(headerFlow, null);
		rawData.calculateDiff(headerFlow);

		// calculate the C value.
		_calculateSinglePointWavespeed();
		_calculateNetWaveIntensity();
		_calculateSeparatedWaveIntensity();
		_calculateSeparatedFlow();
		_calculateTotalCumulativeIntensities();

		retryCalculations();

		// kept in miliseconds at default for ease of displaying
		rawData.convertXUnits(HemoData.UNIT_MILLISECONDS);

	}

	/**
	 * Helper method to {@link WIAData#runAnalysis()}
	 */
	private void _calculateNetWaveIntensity() {
		// note the derivative arrays should be the same sime

		double[] pressureDeriv = getPressureDeriv();
		double[] flowDeriv = getFlowDeriv();

		double[] netWaveIntensityD = new double[pressureDeriv.length];

		for (int der = 0; der < pressureDeriv.length; der++) {
			netWaveIntensityD[der] = pressureDeriv[der] * flowDeriv[der];
		}

		HemoData hdNetWI = rawData.blankCopyOf(true, "Net Wave Intensity");

		hdNetWI.addYData(new Header("Net Wave Intensity", 1, false), netWaveIntensityD, HemoData.TYPE_NET_WIA,
				HemoData.UNIT_WAVE);

		this.netWaveIntensity = hdNetWI;
	}

	/**
	 * Helper method to {@link WIAData#runAnalysis()}
	 */
	private void _calculateSinglePointWavespeed() {

		double[] pressureDiff = getPressureDeriv();
		double[] flowDiff = getFlowDeriv();

		double[] squaredPressureDiff = new double[pressureDiff.length];
		double[] squaredFlowDiff = new double[flowDiff.length];

		for (int i = 0; i < flowDiff.length; i++) {

			squaredPressureDiff[i] = Math.pow(pressureDiff[i], 2);
			squaredFlowDiff[i] = Math.pow(flowDiff[i], 2);
		}

		double sumOfSquaresPressure = DoubleStream.of(squaredPressureDiff).sum();
		double sumOfSquaresFlow = DoubleStream.of(squaredFlowDiff).sum();

		double rhoC = Math.sqrt(sumOfSquaresPressure / sumOfSquaresFlow);

		double c = rhoC / density;

		rho = rhoC;
		waveSpeedC = c;

	}

	/**
	 * Helper method to {@link WIAData#runAnalysis()}
	 */
	private void _calculateSeparatedWaveIntensity() {
		double[] pressureDeriv = getPressureDeriv();
		double[] flowDeriv = getFlowDeriv();

		double multipConstantForward = 1.0 / (4.0 * rho);
		double multipConstantBackward = -1.0 / (4.0 * rho);

		double[] separatedWIForward = new double[pressureDeriv.length];
		double[] separatedWIBackward = new double[pressureDeriv.length];

		for (int i = 0; i < flowDeriv.length; i++) {
			double pcdUdT = rho * flowDeriv[i];
			double dPdT = pressureDeriv[i];
			separatedWIForward[i] = multipConstantForward * Math.pow((dPdT + pcdUdT), 2);
			separatedWIBackward[i] = multipConstantBackward * Math.pow((dPdT - pcdUdT), 2);

		}

		HemoData hdSepWI = rawData.blankCopyOf(true, "Separated Wave Intensity");

		hdSepWI.addYData(new Header("Separated WI Forward", 1, false), separatedWIForward, HemoData.TYPE_SEP_WIA_FORW,
				HemoData.UNIT_WAVE);
		hdSepWI.addYData(new Header("Separated WI Backward", 2, false), separatedWIBackward, HemoData.TYPE_SEP_WIA_BACK,
				HemoData.UNIT_WAVE);

		this.sepWaveIntensity = hdSepWI;

	}

	/**
	 * Helper method to {@link WIAData#runAnalysis()}
	 */
	private void _calculateSeparatedFlow() {

		// dU+ = (1 / [2 * p * c]) * (dP + [p * c * dU])
		// dU- = -1 / ( 2 * p * c * [ dP - [p * c * dU] ] )

		double[] pressureDeriv = getPressureDeriv();
		double[] flowDeriv = getFlowDeriv();
		double[] time = getTime();

		double[] dUPos = new double[pressureDeriv.length];
		double[] dUNeg = new double[pressureDeriv.length];

		for (int i = 0; i < time.length; i++) {
			dUPos[i] = (1.0 / (2.0 * rho)) * (pressureDeriv[i] + (rho * flowDeriv[i]));
			dUNeg[i] = -1.0 / (2.0 * rho * (pressureDeriv[i] - (rho * flowDeriv[i])));
		}

		HemoData hdSepFlowDeriv = rawData.blankCopyOf(true, "Separated Flow Derivative (Separated Acceleration)");

		hdSepFlowDeriv.addYData(new Header("Separated Flow Deriv Forward", 1, false), dUPos,
				HemoData.TYPE_SEP_FLOW_DERIV_FORW, HemoData.UNIT_ACCEL_MS);
		hdSepFlowDeriv.addYData(new Header("Separated Flow Deriv Backward", 2, false), dUNeg,
				HemoData.TYPE_SEP_FLOW_DERIV_BACK, HemoData.UNIT_ACCEL_MS);

		this.sepFlowDeriv = hdSepFlowDeriv;

	}

	/**
	 * Helper method to {@link WIAData#runAnalysis()}
	 */
	private void _calculateTotalCumulativeIntensities() {
		BigDecimal timeIntervalSepS = new BigDecimal(
				HemoData.calculateAverageInterval(sepWaveIntensity.convertXUnitsCopy(HemoData.UNIT_SECONDS)));

		this.cumulativeWIForward = Utils.getAreaUnderCurve(timeIntervalSepS, getWIForward());
		this.cumulativeWIBackward = Utils.getAreaUnderCurve(timeIntervalSepS, getWIBackward());

		// Convert from milliseconds to seconds (is in ms for easier display on graph)
		BigDecimal timeIntervalNetS = new BigDecimal(
				HemoData.calculateAverageInterval(netWaveIntensity.convertXUnitsCopy(HemoData.UNIT_SECONDS)));
		this.cumulativeWINet = Utils.getAreaUnderCurve(timeIntervalNetS, getNetWaveIntensity());

	}

	/**
	 * Sets the hMR, and whether there is functional disease based on the provided
	 * threshold (< threshold means functional, usually 2.5)
	 * 
	 * @param hMR the resistance to set
	 */
	public void setHMR(double hMR) {
		this.hMR = hMR;
	}

	/**
	 * @return hyperemic microvascular resistance if set by {@link #setHMR(double)},
	 *         otherwise null.
	 */
	public Double getHMR() {
		return this.hMR;
	}

	/**
	 * Sets the rMR (resting microvascular resistance)
	 * 
	 * @param rMR the resistance to set
	 */
	public void setRMR(double rMR) {
		this.rMR = rMR;
	}

	/**
	 * @return resting microvascular resistance if set by {@link #setRMR(double)},
	 *         otherwise null.
	 */
	public Double getRMR() {
		return this.rMR;
	}

	/**
	 * @return true if {@link #setCFR(double)} and {@link #setPercIncACh(double)}
	 *         have been called, and if CFR < 2.5 and percent increase in flow with
	 *         acetylcholine is < 50%. Otherwise null.
	 */
	public Boolean isCMD() {
		if (this.percIncACh != null && this.cfr != null) {
			return this.cfr < 2.5 || this.percIncACh < 50;
		} else {
			return null;
		}
	}

	/**
	 * Determines if functional Coronary Microvascular Dysfunction (CMD) is present.
	 * <p>
	 * Functional CMD is indicated if CMD is present (see {@link #isCMD()}) and the
	 * hyperemic microvascular resistance (HMR) is less than 2.5.
	 * </p>
	 * 
	 * @return {@link Boolean#TRUE} if functional CMD is indicated,
	 *         {@link Boolean#FALSE} if not, or null if CMD status or HMR is
	 *         undetermined.
	 */
	public Boolean isCMDFunctional() {

		Boolean isCMD = isCMD();
		if (isCMD != null && this.hMR != null) {
			return isCMD && hMR < 2.5;
		} else {
			return null;
		}
	}

	/**
	 * Determines if structural Coronary Microvascular Dysfunction (CMD) is present.
	 * <p>
	 * Structural CMD is indicated if CMD is present (see {@link #isCMD()}) and the
	 * hyperemic microvascular resistance (HMR) is greater than or equal to 2.5.
	 * </p>
	 * 
	 * @return {@link Boolean#TRUE} if structural CMD is indicated,
	 *         {@link Boolean#FALSE} if not, or null if CMD status or HMR is
	 *         undetermined.
	 */
	public Boolean isCMDStructural() {

		Boolean isCMD = isCMD();
		if (isCMD != null && this.hMR != null) {
			return isCMD && hMR >= 2.5;
		} else {
			return null;
		}
	}

	/**
	 * Determines if endothelium-dependent Coronary Microvascular Dysfunction (CMD)
	 * is present.
	 * <p>
	 * Endothelium-dependent CMD is indicated by a percent increase in flow with
	 * acetylcholine (ACh) of less than 50%.
	 * </p>
	 * 
	 * @param mutuallyExclusive if true, this method will only return true if
	 *                          endothelium-independent CMD is NOT also present
	 *                          (i.e., CFR >= 2.5).
	 * @return {@link Boolean#TRUE} if endothelium-dependent CMD is indicated,
	 *         {@link Boolean#FALSE} if not, or null if the percent increase with
	 *         ACh has not been set.
	 */
	public Boolean isCMDEndothelialDependent(boolean mutuallyExclusive) {

		if (this.percIncACh != null) {

			if (mutuallyExclusive) {
				// if no CFR value, assume not endo indep disease. Otherwise check it
				return this.percIncACh < 50 && (this.cfr != null ? cfr >= 2.5 : true);
			} else {
				return this.percIncACh < 50;
			}

		} else {
			return null;
		}
	}

	/**
	 * Determines if endothelium-independent Coronary Microvascular Dysfunction
	 * (CMD) is present.
	 * <p>
	 * Endothelium-independent CMD is indicated by a Coronary Flow Reserve (CFR) of
	 * less than 2.5.
	 * </p>
	 * 
	 * @param mutuallyExclusive if true, this method will only return true if
	 *                          endothelium-dependent CMD is NOT also present (i.e.,
	 *                          % increase in flow with ACh >= 50).
	 * @return {@link Boolean#TRUE} if endothelium-independent CMD is indicated,
	 *         {@link Boolean#FALSE} if not, or null if CFR has not been set.
	 */
	public Boolean isCMDEndothelialIndependent(boolean mutuallyExclusive) {

		if (this.cfr != null) {
			if (mutuallyExclusive) {
				return this.cfr < 2.5 && (this.percIncACh != null ? percIncACh >= 50 : true);

			} else {
				return this.cfr < 2.5;
			}
		} else {
			return null;
		}
	}

	/**
	 * Determines if both endothelium-independent and endothelium-dependent Coronary
	 * Microvascular Dysfunction (CMD) are present simultaneously.
	 * <p>
	 * This is indicated by a Coronary Flow Reserve (CFR) less than 2.5 AND a
	 * percent increase in flow with acetylcholine (ACh) less than 50%.
	 * </p>
	 * 
	 * @return {@link Boolean#TRUE} if both conditions are met,
	 *         {@link Boolean#FALSE} if not, or null if either CFR or the percent
	 *         increase with ACh has not been set.
	 */
	public Boolean isCMDEndotheliumIndepAndDep() {
		if (this.cfr != null && this.percIncACh != null) {
			if (this.cfr < 2.5 && this.percIncACh < 50) {
				return true;
			} else {
				return false;
			}
		} else {
			return null;
		}
	}
	

	/**
	 * Returns a map of analysis parameters suitable for display.
	 *
	 * @return a {@link LinkedHashMap} where keys are parameter names and values are
	 *         their string representations
	 */
	public LinkedHashMap<String, String> toPrintableMap() {
		LinkedHashMap<String, String> paramValues = new LinkedHashMap<String, String>();

		retryCalculations();

		double maxPressure = Utils.convertPascalsToMMHG(Utils.max(getRawPressure()));
		double minPressure = Utils.convertPascalsToMMHG(Utils.min(getRawPressure()));
		double maxFlow = Utils.max(getRawFlow());
		double minFlow = Utils.min(getRawFlow());

		paramValues.put("File Name", rawData.getFile().getPath());
		paramValues.put("Selection Name", this.selectionName == null ? "" : this.selectionName);
		paramValues.put("Wave Speed (C) m/s", Double.toString(waveSpeedC));
		paramValues.put("Avg Pressure (mmHg)", Double.toString(pressureAvg));
		paramValues.put("Max Pressure (mmHg)", Double.toString(maxPressure));
		paramValues.put("Min Pressure (mmHg)", Double.toString(minPressure));
		paramValues.put("Avg Flow (m/s)", Double.toString(flowAvg));
		paramValues.put("Max Flow (m/s)", Double.toString(maxFlow));
		paramValues.put("Min Flow (m/s)", Double.toString(minFlow));
		paramValues.put("Systole Time", Double.toString(systoleTime));
		paramValues.put("Systole Pressure", Double.toString(systolePressure));
		paramValues.put("Systole Flow", Double.toString(systoleFlow));
		paramValues.put("Diastole Time", Double.toString(diastoleTime));
		paramValues.put("Diastole Pressure", Double.toString(diastolePressure));
		paramValues.put("Diastole Flow", Double.toString(diastoleFlow));
		paramValues.put("Avg Resist (mmHg/cm/s)", Double.toString(resistCycle));
		paramValues.put("Systole Resist (mmHg/cm/s)", Double.toString(resistSystole));
		paramValues.put("Diastole Resist (mmHg/cm/s)", Double.toString(resistDiastole));
		paramValues.put("Cycle duration (ms)", convertDoubleToString(getCycleDuration()));
		paramValues.put("Diastole duration (ms)", convertDoubleToString(getDiastoleDuration()));
		paramValues.put("Diastole onset to peak velocity (ms)", convertDoubleToString(getDiastoleToFlowPeakDuration()));
		paramValues.put("Cumulative Net", Double.toString(cumulativeWINet));
		paramValues.put("Cumulative Forw", Double.toString(cumulativeWIForward));
		paramValues.put("Cumulative Back", Double.toString(cumulativeWIBackward));

		if (cfr != null && hMR != null && percIncACh != null) {
			paramValues.put("CFR", Double.toString(cfr));
			paramValues.put("HMR (mmHg cm s)", Double.toString(hMR));
			paramValues.put("Flow Increase with ACh (%)", Double.toString(percIncACh));
			paramValues.put("CMD", Boolean.toString(isCMD()));
			paramValues.put("Endothelium-dependent CMD", Boolean.toString(isCMDEndothelialDependent(false)));
			paramValues.put("Endothelium-independent CMD", Boolean.toString(isCMDEndothelialIndependent(false)));
			paramValues.put("Functional CMD (endothelium dependent)",
					Boolean.toString(isCMDEndothelialDependent(false) && isCMDFunctional()));
			paramValues.put("Functional CMD (endothelium independent)",
					Boolean.toString(isCMDEndothelialIndependent(false) && isCMDFunctional()));
			paramValues.put("Functional CMD", Boolean.toString(isCMDFunctional()));

		}

		for (WaveClassification waveType : WaveClassification.getWavesTypesOrdered()) {

			if (waveType.equals(WaveClassification.OTHER)) {
				List<Wave> otherWavesAlphabetize = new ArrayList<Wave>();

				for (Wave wave : waves) {
					if (wave.getType().equals(WaveClassification.OTHER)) {
						otherWavesAlphabetize.add(wave);
					}
				}

				otherWavesAlphabetize.sort(new Comparator<Wave>() {
					@Override
					public int compare(Wave o1, Wave o2) {
						return o1.getAbbrev().compareTo(o2.getAbbrev());
					}
				});

				// add ALL waves designated as OTHER

				for (Wave wave : otherWavesAlphabetize) {
					paramValues.put(wave.getAbbrev() + " Cumul", Double.toString(wave.getCumulativeIntensity()));

					String ratio = null;
					if (wave.isProximal()) {
						if (!Double.isNaN(cumulativeWIForward)) {
							ratio = (wave.getCumulativeIntensity() / cumulativeWIForward) + "";
						} else {
							ratio = "NaN";
						}
					} else {
						if (!Double.isNaN(cumulativeWIBackward)) {
							ratio = (wave.getCumulativeIntensity() / cumulativeWIBackward) + "";
						} else {
							ratio = "NaN";
						}
					}
					paramValues.put(wave.getAbbrev() + " Cumul Ratio", ratio);
					paramValues.put(wave.getAbbrev() + " Peak", Double.toString(wave.getPeak()));
					paramValues.put(wave.getAbbrev() + " Peak Time", Double.toString(wave.getPeakTime()));

				}
			} else {

				boolean foundWave = false;

				for (Wave wave : waves) {

					if (wave.getType().equals(waveType)) {

						paramValues.put(waveType.abbrev() + " Cumul", Double.toString(wave.getCumulativeIntensity()));

						String ratio = null;
						if (wave.isProximal()) {
							if (!Double.isNaN(cumulativeWIForward)) {
								ratio = (wave.getCumulativeIntensity() / cumulativeWIForward) + "";
							} else {
								ratio = "NaN";
							}
						} else {
							if (!Double.isNaN(cumulativeWIBackward)) {
								ratio = (wave.getCumulativeIntensity() / cumulativeWIBackward) + "";
							} else {
								ratio = "NaN";
							}
						}
						paramValues.put(waveType.abbrev() + " Cumul Ratio", ratio);
						paramValues.put(waveType.abbrev() + " Peak", Double.toString(wave.getPeak()));
						paramValues.put(waveType.abbrev() + " Peak Time", Double.toString(wave.getPeakTime()));

						foundWave = true;
						// we will only add data for the first wave designated by this type. Really
						// there should only be one.
						break;
					}

				}

				if (!foundWave) {
					// did not find the wave, enter empty data
					paramValues.put(waveType.abbrev() + " Cumul", "");
					paramValues.put(waveType.abbrev() + " Cumul Ratio", "");
					paramValues.put(waveType.abbrev() + " Peak", "");
					paramValues.put(waveType.abbrev() + " Peak Time", "");

				}
			}

		}

		return paramValues;

	}

	

	/**
	 * Returns the string representation of this object.
	 * <p>
	 * This is defined as the file name of the underlying HemoData.
	 * </p>
	 *
	 * @return the file name as a String
	 */
	@Override
	public String toString() {

		return getFileName();
	}


	/**
	 * Checks validity of Double
	 * 
	 * @param query the numbers to check
	 * @return true if all are NOT null and NOT {@link Double#NaN}
	 */
	public static boolean isValidDouble(Double... query) {
		for (Double d : query) {
			if (d == null || d.isNaN())
				return false;
		}
		return true;
	}

	/**
	 * Converts a double to string. If {@link Double#NaN} or {@code null} then
	 * returns "NaN"
	 * 
	 * @param d the input
	 * @return string as above
	 */
	public static String convertDoubleToString(Double d) {
		if (d == null) {
			return Double.toString(Double.NaN);
		} else {
			return Double.toString(d);
		}
		
	}

	/**
	 * Deserializes the object from a file.
	 * 
	 * @param file The file to load from
	 * @return the {@link WIAData} object if could be created
	 * @throws SerializationException if there was any issue during deserialization
	 */
	public static WIAData deserialize(File file) throws SerializationException {

		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			ObjectInputStream objInputStream = new ObjectInputStream(fileInputStream);

			WIAData data = (WIAData) objInputStream.readObject();
			data.deserializedFile = file;

			objInputStream.close();
			fileInputStream.close();

			return data;
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			throw new SerializationException(
					"Unable to read WIA data stored state from file. This may be due to lack of access / permission of this program "
							+ "to read files from your file system. Check administrator privileges. System error msg: <br><br>"
							+ e.getMessage());
		}

	}

	/**
	 * Serializes this object to file. Does not check if something already exists at
	 * the path - will just go ahead and overwrite it.
	 * 
	 * @throws SerializationException if there was an issue with saving.
	 */
	public static void serialize(WIAData serialize, File file) throws SerializationException {

		FileOutputStream fileOutStream = null;
		ObjectOutputStream objOutStream = null;
		try {
			fileOutStream = new FileOutputStream(file);
			objOutStream = new ObjectOutputStream(fileOutStream);
			objOutStream.writeObject(serialize);

			objOutStream.close();
			fileOutStream.close();
		} catch (IOException e) {
			throw new SerializationException(
					"Unable to write serialized version of WIA data to a file. This could be due to the program not being able to "
							+ "write to your file system (access / permissions issue), or improper file path. This means you will not be able to re-edit the "
							+ "waves you have selected at a later point. System error msg: " + e.getMessage());
		}

	}

}
