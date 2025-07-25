package com.carrington.WIA.DataStructures;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;

import com.carrington.WIA.Utils;
import com.carrington.WIA.GUIs.BackgroundProgressRecorder;
import com.carrington.WIA.IO.Header;
import com.carrington.WIA.Math.DataResampler;
import com.carrington.WIA.Math.DataResampler.ResampleException;
import com.carrington.WIA.Math.ResampleResult;
import com.opencsv.CSVWriter;

/**
 * The main data structure used to carry information in this program.
 */
@SuppressWarnings("javadoc")
public class HemoData implements Serializable {

	private static final long serialVersionUID = -4771987296718864051L;

	public static final String FILTER_PASCAL = "Pascals";
	public static final String FILTER_PASCAL_SG = "Pascals_SG";
	public static final String FILTER_MS = "Meterssec";
	public static final String FILTER_MS_SG = "Meterssec_SG";
	public static final String FILTER_SG = "SG";

	public static final String TYPE_PRESSURE = "Type_Pressure";
	public static final String TYPE_FLOW = "Type_Flow";
	public static final String TYPE_ECG = "Type_ECG";
	public static final String TYPE_R_WAVE = "R_WAVE";
	public static final String TYPE_SEP_WIA_FORW = "Type_WIA_Forward";
	public static final String TYPE_SEP_WIA_BACK = "Type_WIA_Backward";
	public static final String TYPE_SEP_FLOW_DERIV_FORW = "Type_Flow_Deriv_Forward";
	public static final String TYPE_SEP_FLOW_DERIV_BACK = "Type_Flow_Deriv_Backward";
	public static final String TYPE_NET_WIA = "Type_WIA_Net";

	public static final String UNIT_WAVE = "Unit_Wave";
	public static final String UNIT_ACCEL_MS = "Unit_MS_Squared";
	public static final String UNIT_SECONDS = "Unit_Seconds";
	public static final String UNIT_MILLISECONDS = "Unit_MS";
	public static final String UNIT_MMHG = "Unit_MMHG";
	public static final String UNIT_PASCAL = "Unit_Pascal";
	public static final String UNIT_MperS = "Unit_MperS";
	public static final String UNIT_CMperS = "Unit_CMperS";
	public static final String UNIT_WI = "Unit_WI";

	public static final String OTHER_ALIGN = "Other_Align";

	public static final int ENSEMBLE_TRIM = 1;
	public static final int ENSEMBLE_SCALE = 2;

	//////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE - START - DO NOT EDIT
	//
	//////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////

	/** The file from which this data was originally derived. May not exist anymore. */
	private File file;
	/** The name of the file from which this data was originally derived. May not exist anymore or it may have changed.*/
	private String fileName;
	/** The specific name given to this HemoData instance. */
	private String name = null;
	/** The header for the primary independent variable data (typically time). */
	private Header xHeader = null;
	/** The array of primary independent variable data (typically time). */
	private double[] xData = null;
	
	/** A map of headers to their corresponding primary dependent variable data arrays. */
	private LinkedHashMap<Header, double[]> yValues = new LinkedHashMap<Header, double[]>();
	
	/** A map of headers to their corresponding calculated differential data arrays. */
	private LinkedHashMap<Header, double[]> yValuesDiff = new LinkedHashMap<Header, double[]>();
	
	/** A map of headers to their corresponding calculated derivative data arrays. */
	private LinkedHashMap<Header, double[]> yValuesDeriv = new LinkedHashMap<Header, double[]>();

	/** A map of headers to a set of associated string flags (e.g., units, types). */
	private final HashMap<Header, Set<String>> flaggedHeaders = new LinkedHashMap<Header, Set<String>>();

	//////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE - END - DO NOT EDIT
	//
	//////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////

	/**
	 * Creates a new object. It stores the arguments passed in, but does nothing
	 * else.
	 * 
	 * @param file     The file which this data is originally derived from, even if
	 *                 tangentially. Cannot be null.
	 * @param fileName The file name from which this data is originally derived
	 *                 from, even if tangentially. Cannot be null.
	 * @param name     The name of this {@link HemoData} object
	 */
	public HemoData(File file, String fileName, String name) {
		if (file == null || fileName == null) {
			throw new IllegalArgumentException("Null inputs for data structure");
		}
		this.file = file;
		this.fileName = fileName;
		this.name = name;

	}

	/**
	 * Only acts on primary data, not any filtered, differentials, or derivatives.
	 * Trims based on the zero-based indexing of the data.
	 * 
	 * warning: RESETS derivatives / differentials / filters
	 */
	public void trimByIndex(int startIndex, int endIndex) {

		if (isValid() != null) {
			throw new IllegalArgumentException("Invalid state for data structure");
		}
		if (startIndex < 0 || endIndex >= this.xData.length) {
			new IllegalArgumentException("too high of index");
		}

		this.xData = copyAndShift(xData, startIndex, endIndex);

		LinkedHashMap<Header, double[]> newyVals = new LinkedHashMap<Header, double[]>();

		Iterator<Entry<Header, double[]>> enitr = this.yValues.entrySet().iterator();
		while (enitr.hasNext()) {
			Entry<Header, double[]> en = enitr.next();
			newyVals.put(en.getKey(), Arrays.copyOfRange(en.getValue(), startIndex, endIndex + 1));
			enitr.remove();
		}
		this.yValues = newyVals;

		this.yValuesDeriv.clear();
		this.yValuesDiff.clear();

	}

	/**
	 * Copies a subrange of the provided time data array and shifts the values so
	 * that the first element is zero.
	 *
	 * @param xData      the original time values array (in seconds)
	 * @param startIndex the starting index of the subrange (inclusive)
	 * @param endIndex   the ending index of the subrange (inclusive)
	 * @return a new double array containing the subrange with values shifted so
	 *         that the first element becomes 0
	 * @throws IllegalArgumentException if xData is null or the indices are invalid
	 */
	public static double[] copyAndShift(double[] xData, int startIndex, int endIndex) {

		// Copy the desired subrange (endIndex + 1 because the upper bound is exclusive)
		double[] subArray = Arrays.copyOfRange(xData, startIndex, endIndex + 1);

		// If the subArray isn't empty, shift all values so that the first element
		// becomes 0
		if (subArray.length > 0) {
			double shift = subArray[0];
			for (int i = 0; i < subArray.length; i++) {
				subArray[i] -= shift;
			}
		}

		return subArray;
	}

	/**
	 * Resamples the data at the specified resampling rate.
	 * 
	 * @param resampleRate Resample rate (x intervals)
	 * @return new {@link HemoData} object which has been resampled
	 * @throws ResampleException if there was an issue with resampling
	 */
	public HemoData resampleAt(double resampleRate, BackgroundProgressRecorder progRecorder) throws ResampleException {

		if (resampleRate <= 0 || this.xData == null || this.xHeader == null || this.yValues == null
				|| this.yValues.isEmpty()) {
			throw new IllegalStateException("Data structure not prepared properly for resampling...");
		}
		HemoData resampled = new HemoData(this.file, this.fileName, this.name);
		ResampleResult rr = DataResampler.resample(resampleRate, true, progRecorder, this.xData,
				yValues.values().toArray(new double[0][]));

		String[] flags = this.flaggedHeaders.containsKey(this.xHeader) ? getFlags(this.xHeader).toArray(new String[0])
				: new String[0];
		resampled.setXData(this.xHeader, rr.timePoints, flags);

		int counter = 0;
		for (Entry<Header, double[]> origEn : yValues.entrySet()) {
			resampled.addYData(origEn.getKey(), rr.values[counter], getFlagsAsArray(origEn.getKey()));
			counter++;
		}

		return resampled;

	}
	
	/**
	 * Resamples the data at the specified resampling rate.
	 * 
	 * @param resampleRate Resample rate (x intervals)
	 * @return new {@link HemoData} object which has been resampled
	 * @throws ResampleException if there was an issue with resampling
	 */
	public HemoData resampleAt(double resampleRate) throws ResampleException {

		return resampleAt(resampleRate, null);

	}

	/**
	 * Calculates and stores the derivative for the specified {@link Header}. This
	 * can then be retrieved using {@link #getCalculatedDeriv(Header)} in the
	 * future.
	 * 
	 * @param yHeader       the {@link Header} to calculate derivative.
	 * @param fixedInterval interval to integrate over, or null to use X.
	 * @throws IllegalArgumentException if the passed {@link Header} is not
	 *                                  contained or has no data
	 */
	public void calculateDerivative(Header yHeader, Double fixedInterval) throws IllegalArgumentException {
		yHeader = resolveHeader(yHeader);
		if (!yValues.containsKey(yHeader))
			throw new IllegalArgumentException("Y values for " + yHeader + " do not exist");

		double[] dataToCalcDerivFor = yValues.get(yHeader);

		if (dataToCalcDerivFor == null)
			throw new IllegalArgumentException("Y values not found for " + yHeader);

		if (fixedInterval == null || fixedInterval <= 0) {
			yValuesDeriv.put(yHeader, calculateDerivative(xData, dataToCalcDerivFor));
		} else {
			yValuesDeriv.put(yHeader, calculateDerivativeByFixedXInterval(fixedInterval, dataToCalcDerivFor));
		}
	}

	/**
	 * Calculates and stores the differential for the specified {@link Header}. This
	 * can then be retrieved using {@link #getCalculatedDiff(Header)} in the future.
	 * 
	 * @param yHeader the {@link Header} to calculate differential.
	 * @throws IllegalArgumentException if the passed {@link Header} is not
	 *                                  contained or has no data.
	 */
	public void calculateDiff(Header yHeader) throws IllegalArgumentException {
		yHeader = resolveHeader(yHeader);
		if (!yValues.containsKey(yHeader))
			throw new IllegalArgumentException("Y values for " + yHeader + " do not exist");

		double[] dataToCalcDiffFor = yValues.get(yHeader);

		if (dataToCalcDiffFor == null)
			throw new IllegalArgumentException("Y values not found for " + yHeader);

		this.yValuesDiff.put(yHeader, calculateDifferential(yValues.get(yHeader)));

	}

	/**
	 * Gets the calculated differential for the specified data.
	 * 
	 * @param header the {@link Header} for data for which we will retrieve the
	 *               differential
	 * @return the calculated differential, null if not calculated via
	 *         {@link #calculateDiff(Header)}
	 * @throws IllegalArgumentException if this {@link HemoData} does not contain
	 *                                  the specified {@link Header}
	 */
	public double[] getCalculatedDiff(Header header) throws IllegalArgumentException {
		if (!yValues.containsKey(header))
			throw new IllegalArgumentException("No y values contained for " + header);

		return this.yValuesDiff.get(header);
	}

	/**
	 * Gets the calculated derivative for the specified data.
	 * 
	 * @param header the {@link Header} for data for which we will retrieve the
	 *               derivative
	 * @return the calculated derivative, null if not calculated via
	 *         {@link #calculateDerivative(Header, Double)}
	 * @throws IllegalArgumentException if this {@link HemoData} does not contain
	 *                                  the specified {@link Header}
	 */
	public double[] getCalculatedDeriv(Header header) throws IllegalArgumentException {
		if (!yValues.containsKey(header))
			throw new IllegalArgumentException("No y values contained for " + header);

		return this.yValuesDeriv.get(header);
	}

	/**
	 * Checks if the derivative was calculated (i.e. via
	 * {@link #calculateDerivative(Header, Double)}) for the specified
	 * {@link Header}
	 */
	public boolean isDerivativeCalculated(Header header) {
		return this.yValuesDeriv.containsKey(header);
	}

	/**
	 * Checks if the derivative was calculated (i.e. via
	 * {@link #calculateDiff(Header)}) for the specified {@link Header}
	 */
	public boolean isDiffCalculated(Header header) {
		return this.yValuesDiff.containsKey(header);
	}

	/**
	 * @return values for the y variable specified. returns null if the header is
	 *         not contained
	 */
	public double[] getYData(Header header) {
		return this.yValues.get(header);
	}

	/**
	 * @return values for the y variable specified. returns null if the header is
	 *         not contained
	 */
	public double[] getYData(String name) {

		for (Entry<Header, double[]> en : yValues.entrySet()) {
			if (en.getKey().getName().equalsIgnoreCase(name)) {
				return en.getValue();
			}
		}

		return null;

	}

	/**
	 * Gets the {@link Header}s for the data in this {@link HemoData}
	 * 
	 * @return list of {@link Header}s
	 */
	public List<Header> getYHeaders() {
		return new ArrayList<Header>(this.yValues.keySet());

	}

	/**
	 * @return the {@link Header} for the X data.
	 */
	public Header getXHeader() {
		return this.xHeader;
	}

	/**
	 * Checks if the point is within the domian range (not if there actually is an X
	 * data point at the passed parameter)
	 * 
	 * @param point query
	 * @return true if point > min domain and < max domain
	 */
	public boolean containsXData(double point) {
		return point >= this.xData[0] && point <= this.xData[this.xData.length - 1];
	}

	/**
	 * @return raw domain (x) data
	 */
	public double[] getXData() {
		return this.xData;
	}

	/**
	 * Sets the header, cannot be null, values cannot be null, and the xHeader
	 * cannot be one of the Y headers.
	 * 
	 */
	public void setXData(Header xHeader, double[] values, String... flags) {
		xHeader = resolveHeader(xHeader);
		if (xHeader == null || values == null)
			throw new IllegalArgumentException("Cannot have null input as X variable: " + xHeader + " : " + values);

		if (!validateSize(values)) {
			throw new IllegalArgumentException("Number of X values is not the same as other data.");
		}

		if (yValues.containsKey(xHeader)) {
			throw new IllegalArgumentException("X header is already contained as a y value.");
		}

		if (!isAscending(values)) {
			throw new IllegalArgumentException("X values must be ascending.");
		}

		this.xData = values;
		this.xHeader = xHeader;

		if (flags != null && flags.length != 0) {
			addFlags(xHeader, flags);
		}
	}

	/**
	 * Modifies this {@link HemoData} object. Shifts all X values to zero.
	 */
	public void shiftXToZero() {
		this.xData = Utils.shiftToZero(this.xData);
	}

	/**
	 * Converts units to the specified units. If there are no units specified
	 * already for this X data then nothing will happen. If derivative/differential
	 * information is contained, this is adjusted.
	 * 
	 * @param units the specified units
	 */
	public void convertXUnits(String units) {
		Set<String> xFlags = getFlags(xHeader);
		if (xFlags.contains(units))
			return; // already in the specific units

		if (units.equals(UNIT_SECONDS)) {
			if (xFlags.contains(UNIT_MILLISECONDS)) {
				this.xData = Utils.divideArray(xData, 1000);
				xFlags.remove(UNIT_MILLISECONDS);
				xFlags.add(UNIT_SECONDS);
				for (Header header : new HashSet<Header>(yValuesDiff.keySet())) {
					calculateDiff(header);
				}
				for (Header header : new HashSet<Header>(yValuesDeriv.keySet())) {
					calculateDerivative(header, null);
				}
			}
		} else if (units.equals(UNIT_MILLISECONDS)) {
			if (xFlags.contains(UNIT_SECONDS)) {
				this.xData = Utils.multiplyArray(xData, 1000);
				xFlags.remove(UNIT_SECONDS);
				xFlags.add(UNIT_MILLISECONDS);
				for (Header header : new HashSet<Header>(yValuesDiff.keySet())) {
					calculateDiff(header);
				}
				for (Header header : new HashSet<Header>(yValuesDeriv.keySet())) {
					calculateDerivative(header, null);
				}
			}
		}

	}

	/**
	 * Converts units to the specified units, providing a copy. If there are no
	 * units specified already for this X data then nothing will happen.
	 * 
	 * @param units the specified units
	 */
	public double[] convertXUnitsCopy(String units) {
		Set<String> xFlags = getFlags(xHeader);
		double[] copy = Arrays.copyOf(xData, xData.length);
		if (xFlags.contains(units))
			return copy; // already in the specific units

		if (units.equals(UNIT_SECONDS)) {
			if (xFlags.contains(UNIT_MILLISECONDS)) {
				copy = Utils.divideArray(copy, 1000);
			}
		} else if (units.equals(UNIT_MILLISECONDS)) {
			if (xFlags.contains(UNIT_SECONDS)) {
				copy = Utils.multiplyArray(copy, 1000);

			}
		}

		return copy;
	}

	/**
	 * Converts units to the specified units. If there is derivative / differential,
	 * also updated.
	 * 
	 * @param units the specified units
	 * @throws IllegalStateException    if does not contain adequate unit to
	 *                                  transform
	 * @throws IllegalArgumentException if the unit selection is invalid
	 */
	public void convertYUnits(Header header, String units) {
		header = resolveHeader(header);
		Set<String> yFlags = getFlags(header);
		if (yFlags.contains(units))
			return; // already in the specific units

		boolean edited = false;

		if (units.equals(UNIT_MMHG)) {
			if (yFlags.contains(UNIT_PASCAL)) {
				yValues.put(header, Utils.convertPascalsToMMHG(yValues.get(header)));
				yFlags.remove(UNIT_PASCAL);
				yFlags.add(UNIT_MMHG);
				edited = true;
			} else if (!yFlags.contains(UNIT_MMHG)) {
				throw new IllegalStateException("Invalid units already - cannot convert.");
			}
		} else if (units.equals(UNIT_PASCAL)) {
			if (yFlags.contains(UNIT_MMHG)) {
				yValues.put(header, Utils.convertToPascals(yValues.get(header)));
				yFlags.remove(UNIT_MMHG);
				yFlags.add(UNIT_PASCAL);
				edited = true;
			} else if (!yFlags.contains(UNIT_PASCAL)) {
				throw new IllegalStateException("Invalid units already - cannot convert.");
			}
		} else if (units.equals(UNIT_MperS)) {
			if (yFlags.contains(UNIT_CMperS)) {

				yValues.put(header, Utils.divideArray(yValues.get(header), 100));
				yFlags.remove(UNIT_CMperS);
				yFlags.add(UNIT_MperS);
				edited = true;
			} else if (!yFlags.contains(UNIT_MperS)) {
				throw new IllegalStateException("Invalid units already - cannot convert.");
			}
		} else if (units.equals(UNIT_CMperS)) {
			if (yFlags.contains(UNIT_MperS)) {
				yValues.put(header, Utils.multiplyArray(yValues.get(header), 100));
				yFlags.remove(UNIT_MperS);
				yFlags.add(UNIT_CMperS);
				edited = true;
			} else if (!yFlags.contains(UNIT_CMperS)) {
				throw new IllegalStateException("Invalid units already - cannot convert.");
			}
		} else {
			throw new IllegalArgumentException();
		}

		if (edited) {
			calculateDiff(header);
			calculateDerivative(header, null);
		}
	}

	/**
	 * Adds y data, CANNOT have same header as X, and values length must be the same
	 * as X data and other Y data in this data set.
	 * 
	 * 
	 * @param yHeader the name of the {@link Header}
	 * @param values  The data to add
	 * @param flags   flags for the data, can be null or empty
	 * @throws IllegalArgumentException if the passed {@link Header} or values are
	 *                                  null, or if passed values are not of same
	 *                                  size as rest of Y data, or if the passed
	 *                                  {@link Header} is already contained
	 */
	public void addYData(Header yHeader, double[] values, String... flags) throws IllegalArgumentException {

		if (yHeader == null || values == null)
			throw new IllegalArgumentException("Cannot have null input as Y variable: " + yHeader + " : " + values);
		if (!validateSize(values)) {
			throw new IllegalArgumentException("Number of Y values is not the same as other data.");
		}
		if (yValues.containsKey(yHeader) || (xHeader != null && xHeader.equals(yHeader))) {
			throw new IllegalArgumentException("Y header is already contained in this data structure.");
		}

		this.yValues.put(yHeader, values);

		if (flags != null && flags.length != 0) {
			addFlags(yHeader, flags);
		}

	}

	/**
	 * Replaces the data for the specified Y {@link Header}.
	 * 
	 * @param yHeader the {@link Header} whose data should be replaced
	 * @param values  the new values
	 * @throws IllegalArgumentException if the passed {@link Header} is null or not
	 *                                  contained, if the passed values array is not
	 *                                  the same size as the original data
	 */
	public void replaceYData(Header yHeader, double[] values) throws IllegalArgumentException {
		if (yHeader == null || values == null)
			throw new IllegalArgumentException("Cannot have null input as Y variable: " + yHeader + " : " + values);
		
		yHeader = resolveHeader(yHeader);
		
		if (!validateSize(values)) {
			throw new IllegalArgumentException("Number of Y values is not the same as other data.");
		}
		if (!yValues.containsKey(yHeader)) {
			throw new IllegalArgumentException("Tried to replace a Y header that does not exist.");
		}
		this.yValues.put(yHeader, values);

	}

	/**
	 * Deletes Y {@link Header} data
	 * 
	 * @param headersToIgnore list of {@link Header}s that should be ignored in the
	 *                        process
	 */
	public void deleteYVars(Header... headersToIgnore) {

		Iterator<Header> itr = this.yValues.keySet().iterator();
		while (itr.hasNext()) {
			Header thisHeader = itr.next();
			boolean keep = false;
			for (Header header : headersToIgnore) {
				if (header.equals(thisHeader)) {
					keep = true;
					break;
				}
			}
			if (!keep) {
				itr.remove();
				this.yValuesDeriv.remove(thisHeader);
				this.yValuesDiff.remove(thisHeader);
				this.flaggedHeaders.remove(thisHeader);

			}
		}
	}

	/**
	 * always returns a list - empty if no headers with the flag
	 */
	public List<Header> getHeaderByFlag(String flag) {
		List<Header> headers = new ArrayList<Header>();

		for (Entry<Header, Set<String>> flagsEn : this.flaggedHeaders.entrySet()) {

			Set<String> flags = flagsEn.getValue();
			if (flags == null) {
				continue;
			}

			for (String qFlag : flags) {
				if (qFlag.equalsIgnoreCase(flag)) {
					headers.add(flagsEn.getKey());
					break;
				}
			}

		}

		return headers;
	}
	
	/**
	 * @param flag the flag to look for
	 * @return true if there is a {@link Header} with the given flag
	 */
	public boolean containsHeaderByFlag(String flag) {
		
		if (flag == null || flag.isBlank())
			throw new IllegalArgumentException("Null flag");
		
		for (Entry<Header, Set<String>> flagsEn : this.flaggedHeaders.entrySet()) {

			Set<String> flags = flagsEn.getValue();
			if (flags != null) {
				for (String qFlag : flags) {
					if (qFlag.equalsIgnoreCase(flag)) {
						return true;
					}
				}
			}

		}

		return false;
	}

	/**
	 * Adds flags to data.
	 * 
	 * @param header the {@link Header} of data to add flags
	 * @param flags  flag to add
	 * @throws IllegalArgumentException if any of arguments are null
	 */
	public void addFlags(Header header, String... flags) throws IllegalArgumentException {

		if (flags == null || header == null) {
			throw new IllegalArgumentException("Cannot accept null argument");
		}
		if (!header.equals(xHeader) && !this.yValues.containsKey(header)) {
			throw new IllegalArgumentException(
					"Cannot flag a header " + header + " which is not contained in this data structure.");
		}
		Header cannonical = resolveHeader(header);
		Set<String> existingFlags = this.flaggedHeaders.get(cannonical);
		if (existingFlags == null) {
			existingFlags = new HashSet<String>();
		}
		existingFlags.addAll(Arrays.asList(flags));

		this.flaggedHeaders.put(cannonical, existingFlags);

	}

	/**
	 * Removes flags from data.
	 * 
	 * @param header the {@link Header} of data to remove flags
	 * @param flags  flag to remove
	 * @throws IllegalArgumentException if any of arguments are null
	 */
	public void removeFlags(Header header, String... flags) throws IllegalArgumentException {
		if (flags == null || header == null) {
			throw new IllegalArgumentException("Cannot accept null argument");
		}
		if (!header.equals(xHeader) && !this.yValues.containsKey(header)) {
			return;
		}

		Set<String> existingFlags = this.flaggedHeaders.get(resolveHeader(header));
		if (existingFlags == null) {
			return;
		} else {
			existingFlags.removeAll(Arrays.asList(flags));
		}

	}

	/**
	 * @return true if the header is contained either as X or Y
	 */
	public boolean hasHeader(Header query) {
		return xHeader.equals(query) || yValues.containsKey(query);
	}

	/**
	 * @param header Header to look for (can be X or Y)
	 * @return flags in an {@link Set} form. Does not return null.
	 */
	public Set<String> getFlags(Header header) {
		return flaggedHeaders.containsKey(header) ? flaggedHeaders.get(header) : new HashSet<String>();
	}

	/**
	 * 
	 * @param header Header to look for (can be X or Y)
	 * @return flags in an array form. Does not return null.
	 */
	public String[] getFlagsAsArray(Header header) {
		Set<String> flags = getFlags(header);
		if (flags == null) {
			return new String[0];
		} else {
			return flags.toArray(new String[0]);
		}
	}

	/**
	 * Checks if there is a flag for thte specified data
	 * 
	 * @param header the header (X or Y data name) to look for
	 * @param flag   the flag to check
	 * @return true if X or Y data based on the passed {@link Header} has the
	 *         specified flag
	 */
	public boolean hasFlag(Header header, String flag) {
		if (this.flaggedHeaders.containsKey(header)) {

			Set<String> flags = this.flaggedHeaders.get(header);
			if (flags != null && flags.contains(flag)) {
				return true;
			}

		}

		return false;
	}

	/**
	 * Checks if only the Y data contains the specified flag
	 * 
	 * @param flag query
	 * @return true if flag is assigned for the Y data
	 */
	public boolean flagExistsInYData(String flag) {
		for (Entry<Header, Set<String>> entry : this.flaggedHeaders.entrySet()) {

			if (entry.getKey().equals(xHeader))
				continue;

			for (String qFlag : entry.getValue()) {
				if (qFlag.equalsIgnoreCase(flag))
					return true;
			}
		}

		return false;
	}

	/**
	 * @param flag query
	 * @return true if flag exists for the X data or any of the Y data
	 */
	public boolean flagExists(String flag) {
		for (Set<String> flags : this.flaggedHeaders.values()) {

			for (String qFlag : flags) {
				if (qFlag.equalsIgnoreCase(flag))
					return true;
			}
		}

		return false;
	}

	/**
	 * Ensembles the data with the specified type of ensemble method, one of
	 * {@link HemoData#ENSEMBLE_SCALE} or {@link HemoData#ENSEMBLE_TRIM}<br>
	 * <br>
	 * 
	 * <ul>
	 * <li>If ensemble scale, all passed data will be scaled to the domain range of
	 * this {@link HemoData}</li>
	 * <li>If ensemble trim, all passed data will be scaled to the domain range of
	 * this {@link HemoData}</li>
	 * </ul>
	 * 
	 * @param otherData Ensembles other {@link HemoData} with this one
	 * @param type      the type of ensemble to perform, either
	 *                  {@link #ENSEMBLE_SCALE} or {@link #ENSEMBLE_TRIM}
	 * @return new {@link HemoData} object which has been ensemble averaged
	 */
	public HemoData ensembleAverage(Collection<HemoData> otherData, int type) {

		HemoData subData = new HemoData(this.file, this.fileName, this.name);
		
		if (type == ENSEMBLE_TRIM) {
			// other data must be equal to or larger in size
			for (HemoData data : otherData) {
				if (data.getSize() < this.getSize()) {
					throw new IllegalArgumentException("Incompatible beat size for ensemble.");
				}
			}
		}
		

		subData.setXData(this.xHeader, this.xData, getFlagsAsArray(xHeader));

		int numBeats = otherData.size() + 1; // +1 to include this beat.

		for (Entry<Header, double[]> en : this.yValues.entrySet()) {

			double[] ensemble = Arrays.copyOf(en.getValue(), en.getValue().length);

			for (HemoData hdOther : otherData) {
				double[] hdOtherYValues = null;

				switch (type) {
				case ENSEMBLE_TRIM:
					hdOtherYValues = hdOther.getYData(en.getKey());
					break;
				case ENSEMBLE_SCALE:
					hdOtherYValues = DataResampler.resample(hdOther.getYData(en.getKey()), ensemble.length);
					break;
				default:
					throw new IllegalArgumentException("Invalid ensemble type");
				}
				if (hdOtherYValues == null)
					throw new IllegalArgumentException(
							"For ensembling, all data structures must have same Y data headers, and same number of values");

				for (int i = 0; i < ensemble.length; i++) {
					ensemble[i] += hdOtherYValues[i];
				}
			}

			for (int i = 0; i < ensemble.length; i++) {
				ensemble[i] = ensemble[i] / numBeats;
			}

			subData.addYData(en.getKey(), ensemble, getFlagsAsArray(en.getKey()));

		}

		for (Entry<Header, double[]> en : this.yValuesDiff.entrySet()) {

			double[] ensembleDiff = Arrays.copyOf(en.getValue(), en.getValue().length);

			for (HemoData hdOther : otherData) {
				double[] hdOtherYDiffValues = null;

				switch (type) {
				case ENSEMBLE_TRIM:
					hdOtherYDiffValues = hdOther.getCalculatedDiff(en.getKey());
					break;
				case ENSEMBLE_SCALE:
					hdOtherYDiffValues = DataResampler.resample(hdOther.getCalculatedDiff(en.getKey()),
							ensembleDiff.length);
					break;
				default:
					throw new IllegalArgumentException("Invalid ensemble type");
				}
				if (hdOtherYDiffValues == null)
					throw new IllegalArgumentException(
							"For ensembling, all data structures must have derivatives for same Y columns, and same number of values");

				for (int i = 0; i < ensembleDiff.length; i++) {
					ensembleDiff[i] += hdOtherYDiffValues[i];
				}
			}

			for (int i = 0; i < ensembleDiff.length; i++) {
				ensembleDiff[i] = ensembleDiff[i] / numBeats;
			}
			subData.yValuesDiff.put(en.getKey(), ensembleDiff);

		}

		for (Entry<Header, double[]> en : this.yValuesDeriv.entrySet()) {

			double[] ensembleDeriv = Arrays.copyOf(en.getValue(), en.getValue().length);

			for (HemoData hdOther : otherData) {
				double[] hdOtherYDerivValues = null;

				switch (type) {
				case ENSEMBLE_TRIM:
					hdOtherYDerivValues = hdOther.getCalculatedDeriv(en.getKey());
					break;
				case ENSEMBLE_SCALE:
					hdOtherYDerivValues = DataResampler.resample(hdOther.getCalculatedDeriv(en.getKey()),
							ensembleDeriv.length);
					break;
				default:
					throw new IllegalArgumentException("Invalid ensemble type");
				}
				if (hdOtherYDerivValues == null)
					throw new IllegalArgumentException(
							"For ensembling, all data structures must have derivatives for same Y columns, and same number of values");

				for (int i = 0; i < ensembleDeriv.length; i++) {
					ensembleDeriv[i] += hdOtherYDerivValues[i];
				}
			}

			for (int i = 0; i < ensembleDeriv.length; i++) {
				ensembleDeriv[i] = ensembleDeriv[i] / numBeats;
			}
			subData.yValuesDeriv.put(en.getKey(), ensembleDeriv);

		}

		_copyFlags(subData);

		return subData;

	}

	/**
	 * subset of raw data, derivatives and differentials. does NOT subset the
	 * filters becasue they may not be same length as primary data. copies flags as
	 * well
	 * 
	 * end index non inclusive.
	 * 
	 * returns a new HemoData;
	 */
	public HemoData subset(String name, int startIndex, int endIndex) {
		HemoData subData = new HemoData(this.file, this.fileName, name);
		subData.xData = Arrays.copyOfRange(xData, startIndex, endIndex);
		subData.xHeader = xHeader;

		_copyDataRange(this.yValues, subData.yValues, startIndex, endIndex);
		_copyDataRange(this.yValuesDeriv, subData.yValuesDeriv, startIndex, endIndex);
		_copyDataRange(this.yValuesDiff, subData.yValuesDiff, startIndex, endIndex);
		_copyFlags(subData);

		return subData;

	}

	/**
	 * Utility method
	 */
	private void _copyDataRange(LinkedHashMap<Header, double[]> source, LinkedHashMap<Header, double[]> target,
			int startIndex, int endIndex) {

		if (source == null || source.isEmpty())
			return;

		for (Entry<Header, double[]> sourceEn : source.entrySet()) {
			target.put(sourceEn.getKey(), Arrays.copyOfRange(sourceEn.getValue(), startIndex, endIndex));
		}

	}

	/**
	 * The Y header to apply the offset to. Negative values will move the field back
	 * compared to others, and vice versa.
	 * 
	 * @param header        the {@link Header} for which to apply offset
	 * @param numberOfIndices the offset.
	 */
	public void applyIndexOffset(Header header, int numberOfIndices) {

		if (header == null || numberOfIndices == 0 || !this.yValues.containsKey(header)) {
			throw new IllegalArgumentException("Invalid header or number of shift units.");
		}
		if (xData.length - Math.abs(numberOfIndices) <= 0) {
			throw new IllegalArgumentException("Cannot offset data that far.");
		}

		int startTarget = -1;
		int endTarget = -1;
		int startOthers = -1;
		int endOthers = -1;

		if (numberOfIndices < 0) {
			// shift this header backward. basically trim the front end of it. trim back end
			// of all the others.
			startTarget = Math.abs(numberOfIndices);
			endTarget = xData.length;
			startOthers = 0;
			endOthers = xData.length - Math.abs(numberOfIndices);

		} else {
			// shift this header forward. basically trim the back end of it. trim front end
			// of all the others.

			startTarget = 0;
			endTarget = xData.length - numberOfIndices;
			startOthers = numberOfIndices;
			endOthers = xData.length;

		}

		int startTargetDD = startTarget;
		int endTargetDD = endTarget - 1;
		int startOthersDD = startOthers;
		int endOthersDD = endOthers - 1;

		LinkedHashMap<Header, double[]> data = new LinkedHashMap<Header, double[]>();
		Iterator<Entry<Header, double[]>> entryItr = this.yValues.entrySet().iterator();
		while (entryItr.hasNext()) {
			Entry<Header, double[]> entry = entryItr.next();
			if (entry.getKey().equals(header)) {
				data.put(entry.getKey(), Arrays.copyOfRange(entry.getValue(), startTarget, endTarget));
			} else {
				data.put(entry.getKey(), Arrays.copyOfRange(entry.getValue(), startOthers, endOthers));
			}
			entryItr.remove();
		}

		this.yValues = data;
		this.xData = Arrays.copyOfRange(xData, startOthers, endOthers);

		LinkedHashMap<Header, double[]> dataDiff = new LinkedHashMap<Header, double[]>();
		Iterator<Entry<Header, double[]>> entryItrDiff = this.yValuesDiff.entrySet().iterator();
		while (entryItrDiff.hasNext()) {
			Entry<Header, double[]> entry = entryItrDiff.next();
			if (entry.getKey().equals(header)) {
				dataDiff.put(entry.getKey(), Arrays.copyOfRange(entry.getValue(), startTargetDD, endTargetDD));
			} else {
				dataDiff.put(entry.getKey(), Arrays.copyOfRange(entry.getValue(), startOthersDD, endOthersDD));
			}
			entryItrDiff.remove();
		}
		this.yValuesDiff = dataDiff;

		LinkedHashMap<Header, double[]> dataDeriv = new LinkedHashMap<Header, double[]>();
		Iterator<Entry<Header, double[]>> entryItrDeriv = this.yValuesDeriv.entrySet().iterator();
		while (entryItrDeriv.hasNext()) {
			Entry<Header, double[]> entry = entryItrDeriv.next();
			if (entry.getKey().equals(header)) {
				dataDeriv.put(entry.getKey(), Arrays.copyOfRange(entry.getValue(), startTargetDD, endTargetDD));
			} else {
				dataDeriv.put(entry.getKey(), Arrays.copyOfRange(entry.getValue(), startOthersDD, endOthersDD));
			}
			entryItrDeriv.remove();
		}
		this.yValuesDeriv = dataDeriv;

	}
	
	/**
	 * Applies an X-value shift to the specified header's Y data.
	 * This trims data such that the specified header is shifted by a given X offset.
	 * Positive shifts move the header forward in time (trim the beginning),
	 * negative shifts move it backward (trim the end).
	 *
	 * @param header  the {@link Header} to shift
	 * @param xShift  the amount to shift in X units
	 */
	public void applyXOffset(Header header, double xShift) {
		if (header == null || xShift == 0.0 || !this.yValues.containsKey(header)) {
			throw new IllegalArgumentException("Invalid header or zero shift.");
		}

		int shiftIndex = 0;
		double baseX = xData[0];
		double shiftedX = baseX + xShift;

		if (xShift > 0) {
			// find first index where x >= base + xShift
			while (shiftIndex < xData.length && xData[shiftIndex] < shiftedX) {
				shiftIndex++;
			}
		} else {
			// find how many steps from the end to go back
			while (shiftIndex < xData.length && xData[xData.length - 1 - shiftIndex] > shiftedX) {
				shiftIndex++;
			}
		}

		if (xData.length - shiftIndex <= 0) {
			throw new IllegalArgumentException("Cannot offset data that far.");
		}

		int startTarget = (xShift > 0) ? shiftIndex : 0;
		int endTarget = (xShift > 0) ? xData.length : xData.length - shiftIndex;
		int startOthers = (xShift > 0) ? 0 : shiftIndex;
		int endOthers = (xShift > 0) ? xData.length - shiftIndex : xData.length;

		LinkedHashMap<Header, double[]> data = new LinkedHashMap<>();
		for (Entry<Header, double[]> entry : this.yValues.entrySet()) {
			Header h = entry.getKey();
			double[] values = entry.getValue();
			if (h.equals(header)) {
				data.put(h, Arrays.copyOfRange(values, startTarget, endTarget));
			} else {
				data.put(h, Arrays.copyOfRange(values, startOthers, endOthers));
			}
		}
		this.yValues = data;
		this.xData = Arrays.copyOfRange(xData, startOthers, endOthers);

		LinkedHashMap<Header, double[]> dataDiff = new LinkedHashMap<>();
		for (Entry<Header, double[]> entry : this.yValuesDiff.entrySet()) {
			Header h = entry.getKey();
			double[] values = entry.getValue();
			if (h.equals(header)) {
				dataDiff.put(h, Arrays.copyOfRange(values, startTarget, endTarget - 1));
			} else {
				dataDiff.put(h, Arrays.copyOfRange(values, startOthers, endOthers - 1));
			}
		}
		this.yValuesDiff = dataDiff;

		LinkedHashMap<Header, double[]> dataDeriv = new LinkedHashMap<>();
		for (Entry<Header, double[]> entry : this.yValuesDeriv.entrySet()) {
			Header h = entry.getKey();
			double[] values = entry.getValue();
			if (h.equals(header)) {
				dataDeriv.put(h, Arrays.copyOfRange(values, startTarget, endTarget - 1));
			} else {
				dataDeriv.put(h, Arrays.copyOfRange(values, startOthers, endOthers - 1));
			}
		}
		this.yValuesDeriv = dataDeriv;
	}

	/**
	 * Sets a new column of Y Data with the filtered data array. Must be the same
	 * length as the original array. Flags for the Y data are retained. However, if
	 * any derivative or differential data has been calculated then that data is
	 * reset for this header (but not others).
	 * 
	 * @param header       the header to set new filtered data
	 * @param filteredData filtered data
	 */
	public void applyFilter(Header header, double[] filteredData) {
		header = resolveHeader(header);
		if (!yValues.containsKey(header)) {
			throw new IllegalArgumentException(
					"Error filtering HD. Header " + header.getName() + " was not contained.");
		} else if (filteredData == null || yValues.get(header).length != filteredData.length) {
			throw new IllegalArgumentException(
					"Error filtering HD. Filtered data is null or not the same size as existing Y values.");
		}

		yValues.put(header, filteredData);
		yValuesDeriv.remove(header);
		yValuesDiff.remove(header);

	}

	/**
	 * @return name of this {@link HemoData} which was set during initialization
	 *         (and possibly edited)
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets a new name for this {@link HemoData} object.
	 * 
	 * @param name new name
	 * @throws IllegalArgumentException if name is null or blank
	 */
	public void setName(String name) throws IllegalArgumentException {
		if (name == null || name.isBlank())
			throw new IllegalArgumentException("Blank name for HemoData");
		this.name = name;
	}

	/**
	 * @return the number of data points in this {@link HemoData} (i.e. number of X
	 *         values stored)
	 */
	public int getSize() {
		if (this.xData == null)
			return 0;
		else
			return this.xData.length;
	}

	/**
	 * @return the {@link File} represented either directly or indirectly (ancestor)
	 *         to the data in this object
	 */
	public File getFile() {
		return this.file;
	}

	/**
	 * @return the name of the {@link File} represented either directly or
	 *         indirectly (ancestor) to the data in this object
	 */
	public String getFileName() {
		return this.fileName;
	}
	
	/**
	 * Sets the file reference for this HemoData object.
	 *
	 * @param file The new {@link File} to associate with this data.
	 */
	public void setFile(File file) {
		this.file = file;
	}
	
	/**
	 * Sets the file name for this HemoData object.
	 *
	 * @param name The new file name to associate with this data.
	 */
	public void setFileName(String name) {
		this.fileName = name;
	}

	/**
	 * Gets the range of data for the specified header.
	 * 
	 * @param header the {@link Header} of interest, can be X or Y.
	 * @return range, in the format of array of size 2: [<min>,<max>]
	 * @throws IllegalArgumentException if header is null or not contained in this
	 *                                  {@link HemoData} object
	 */
	public double[] getRange(Header header) throws IllegalArgumentException {

		if (header == null)
			throw new IllegalArgumentException("Null argument");

		if (header.equals(xHeader)) {
			return new double[] { xData[0], xData[xData.length - 1] };
		} else if (yValues.containsKey(header)) {

			double[] yData = yValues.get(header);

			double min = Double.NaN;
			double max = Double.NaN;
			for (Double d : yData) {
				if (Double.isNaN(min) || Double.isNaN(max)) {
					min = d;
					max = d;
				} else {
					if (d < min) {
						min = d;
					} else if (d > max) {
						max = d;
					}
				}
			}
			return new double[] { min, max };

		} else {
			throw new IllegalArgumentException("No values for " + header + " in this data set.");
		}

	}

	/**
	 * Creates a blank copy of this {@link HemoData} and returns it. The new object
	 * will have the same {@link File} and {@link File} name.
	 * 
	 * @param copyXData whether to copy the X data and flags
	 * @param name      the name of the new object. If null, uses this object's
	 *                  name.
	 * @return the blank {@link HemoData} copy
	 */
	public HemoData blankCopyOf(boolean copyXData, String name) {
		HemoData hd = new HemoData(this.file, this.fileName, name == null ? this.name : name);

		if (copyXData) {
			hd.setXData(this.xHeader, Arrays.copyOf(xData, xData.length), getFlagsAsArray(xHeader));
		}
		return hd;
	}

	/**
	 * Copies data into a new {@link HemoData}. Retains the x values. If Y headers
	 * are specified then it retains these as well
	 * 
	 * @param name     name of this data object
	 * @param yHeaders y data to copy, or null if should not include any
	 * @return new copy
	 */
	public HemoData blankCopyOf(String name, Header... yHeaders) {
		HemoData hd = new HemoData(this.file, this.fileName, name);

		hd.setXData(this.xHeader, Arrays.copyOf(this.xData, this.xData.length), this.getFlagsAsArray(xHeader));

		// If no yHeaders were specified then this will not run
		for (Header header : yHeaders) {
			header = resolveHeader(header);
			double[] yData = this.getYData(header);
			if (yData == null)
				throw new IllegalArgumentException(
						"Tried to make copy of HemoData but could not because header was null.");
			hd.addYData(header, Arrays.copyOf(yData, yData.length), this.getFlagsAsArray(header));

		}
		return hd;
	}

	/**
	 * Copies data into a new {@link HemoData}. Retains the x values and all y
	 * headers
	 * 
	 * @param name name of this data object
	 * @return new copy
	 */
	public HemoData blankCopy(String name) {
		HemoData hd = new HemoData(this.file, this.fileName, name);
		hd.setXData(this.xHeader, Arrays.copyOf(this.xData, this.xData.length), this.getFlagsAsArray(xHeader));
		for (Header header : this.getYHeaders()) {

			double[] yData = this.getYData(header);
			if (yData == null)
				throw new IllegalArgumentException(
						"Tried to make copy of HemoData but could not because header was null.");
			hd.addYData(header, Arrays.copyOf(yData, yData.length), this.getFlagsAsArray(header));

		}
		return hd;
	}

	/**
	 * Confirms the input data array has the same size as the other data arrays in
	 * this structure
	 *
	 * @return true if same size
	 */
	private boolean validateSize(double[] data) {

		if (xHeader != null) {
			if (data.length != xData.length) {
				return false;
			}
		}

		for (Entry<Header, double[]> en : yValues.entrySet()) {
			if (en.getValue().length != data.length) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @return true if X data is set, X header is set, there are Y values, and the
	 *         number of data points for the X and Y data are the same.
	 */
	public String isValid() {

		if (this.xData == null || this.xHeader == null || this.yValues.isEmpty())
			return "Lacking either X or Y data.";
		else if (!isAscending(xData)) {
			return "X values must be ascending only.";

		} else {

			for (double[] values : this.yValues.values()) {
				if (values.length != this.xData.length) {
					return "Number of X and Y data points are not the same. Check input file.";
				}
			} 

		}
		
		return null;

	}

	/**
	 * @return a deep copy of this object, including all sub-objects / data
	 *         structures.
	 */
	public HemoData copy() {
		return SerializationUtils.clone(this);
	}

	/**
	 * Creates and returns a new HemoData copy in which the Y data for one header 
	 * ("adjustedHeader") is shifted so that its value at index indexAdjusted aligns with 
	 * the value of the Y data for another header ("fixedHeader") at index indexFixed.
	 * 
	 * <p>This method supports two alignment modes:
	 * <ul>
	 *   <li>
	 *     <strong>Wrap-Around Mode</strong> – If {@code allowWrap} is true, the method attempts
	 *     to circularly rotate the adjusted header’s data. It computes a dynamic threshold as 
	 *     3× the mean adjacent difference (i.e. the mean of the absolute differences between 
	 *     consecutive elements) in the original adjusted array. It then computes the boundary 
	 *     difference as the absolute difference between the first and last elements of the original 
	 *     adjusted array. If either {@code allowExcessiveDiscordance} is true or the boundary 
	 *     difference is less than or equal to the dynamic threshold, the rotation (wrap‐around) is 
	 *     accepted, and the entire array is used (without cropping). Otherwise, an {@code ArithmeticException}
	 *     is thrown.
	 *   </li>
	 *   <li>
	 *     <strong>Cropping Mode</strong> – If wrapping is not allowed (i.e. {@code allowWrap} is false) 
	 *     or if the dynamic threshold condition is not met, the method falls back to cropping. In 
	 *     cropping mode, the overlapping length is computed as L = N – |delta| (with N being the length 
	 *     of the data arrays and delta = indexAdjusted – indexFixed). The fixed header (and all non‐adjusted 
	 *     headers) are cropped from indices [cropStart, cropStart+L), where cropStart is 0 if delta ≥ 0 or 
	 *     |delta| if delta < 0. The adjusted header is cropped from indices [delta, delta+L) (if delta ≥ 0) 
	 *     or from [0, L) (if delta < 0). The X data are cropped in the same way and then shifted so that 
	 *     the first element becomes 0.
	 * </ul>
	 * 
	 * <p>In either mode, the derived arrays (differentials and derivatives) are cleared and recalculated 
	 * based on the new X and Y data. All flags from the original data are copied to the new HemoData object.
	 * 
	 * @param fixedHeader    the header whose data remains unchanged (reference)
	 * @param adjustedHeader the header whose data will be shifted to align with the fixed header
	 * @param indexFixed     the index in the fixed header’s data to be used for alignment
	 * @param indexAdjusted  the index in the adjusted header’s data to be used for alignment
	 * @param allowWrap      if true, the method attempts a wrap‐around (rotation) of the adjusted data;
	 *                       if false, the method always uses cropping
	 * @param allowExcessiveDiscordance if true, wrapping is allowed even if the boundary difference exceeds
	 *                       the dynamic threshold; if false, excessive discordance causes an exception.
	 * @return a new HemoData copy with the specified alignment applied
	 * @throws IllegalArgumentException if either header is null, if the headers are not found in the data,
	 *         or if the specified alignment indices are out of range
	 * @throws ArithmeticException if (in cropping mode) the overlapping region has zero length or
	 *         (in wrap mode) the boundary difference exceeds the dynamic threshold and excessive discordance is not allowed
	 */
	public HemoData copyWithYAlignment(Header fixedHeader, Header adjustedHeader, int indexFixed, int indexAdjusted,
			boolean allowWrap, boolean allowExcessiveDiscordance) {
		if (fixedHeader == null || adjustedHeader == null) {
			throw new IllegalArgumentException("Headers cannot be null.");
		}
		fixedHeader = resolveHeader(fixedHeader);
		adjustedHeader = resolveHeader(adjustedHeader);
		if (!this.yValues.containsKey(fixedHeader) || !this.yValues.containsKey(adjustedHeader)) {
			throw new IllegalArgumentException("Both fixed and adjusted headers must be present in the data.");
		}

		int N = this.xData.length;
		if (indexFixed < 0 || indexFixed >= N || indexAdjusted < 0 || indexAdjusted >= N) {
			throw new IllegalArgumentException("Alignment indices must be within the bounds of the data arrays.");
		}

		int delta = indexAdjusted - indexFixed;

		// --- WRAP-AROUND MODE ---
		if (allowWrap) {
			int k = ((delta % N) + N) % N; // rotation amount in [0,N)
			double[] origAdjusted = this.getYData(adjustedHeader);
			double[] rotated = new double[N];
			for (int i = 0; i < N; i++) {
				rotated[i] = origAdjusted[(i + k) % N];
			}
			// Compute boundary difference for the join in the rotated array.
			double boundaryDiff = Math.abs(origAdjusted[N - 1] - origAdjusted[0]);
			// Compute the maximum adjacent difference from the rotated array.
			double meanAdjDiff = java.util.stream.IntStream.range(1, origAdjusted.length)
					.mapToDouble(i -> Math.abs(origAdjusted[i] - origAdjusted[i - 1])).average().orElse(0);

			if (meanAdjDiff == 0) {
				meanAdjDiff = 1e-6;
			}
			double dynamicThreshold = 3 * meanAdjDiff;
			// If the join difference is too large, we reject wrapping.
			if (allowExcessiveDiscordance || boundaryDiff <= dynamicThreshold) {
				// Wrap-around accepted.
				HemoData newData = this.blankCopyOf(true, this.name + "_alignedWrap");
				for (Header header : this.getYHeaders()) {
					double[] origY = this.getYData(header);
					if (header.equals(adjustedHeader)) {
						newData.addYData(header, rotated, this.getFlagsAsArray(header));
					} else {
						newData.addYData(header, Arrays.copyOf(origY, origY.length), this.getFlagsAsArray(header));
					}
				}
				newData.yValuesDiff.clear();
				newData.yValuesDeriv.clear();
				for (Header header : newData.getYHeaders()) {
					newData.calculateDerivative(header, null);
					newData.calculateDiff(header);
				}
				newData.flaggedHeaders.clear();
				newData.flaggedHeaders.putAll(this.flaggedHeaders);
				return newData;
			} else {
				throw new ArithmeticException("Cannot wrap - excessive discordance at the ends");
			}
			// If wrapping fails (boundaryDiff > dynamicThreshold), fall through to cropping
			// mode.
		} else {
			// --- CROPPING MODE ---
			int absDelta = Math.abs(delta);
			int L = N - absDelta;
			if (L <= 0) {
				throw new ArithmeticException("Alignment shift is too large; no overlapping data remains.");
			}
			int cropStart = (delta >= 0) ? 0 : absDelta;
			int cropEnd = cropStart + L;
			HemoData newData = this.blankCopyOf(false, this.name + "_aligned");
			double[] newX = Arrays.copyOfRange(this.xData, cropStart, cropEnd);
			newX = Utils.shiftToZero(newX);
			newData.setXData(this.xHeader, newX, this.getFlagsAsArray(this.xHeader));

			for (Header header : this.getYHeaders()) {
				double[] origY = this.getYData(header);
				double[] newY;
				if (header.equals(adjustedHeader)) {
					if (delta >= 0) {
						newY = Arrays.copyOfRange(origY, delta, delta + L);
					} else {
						newY = Arrays.copyOfRange(origY, 0, L);
					}
				} else {
					newY = Arrays.copyOfRange(origY, cropStart, cropEnd);
				}
				newData.addYData(header, newY, this.getFlagsAsArray(header));
			}

			LinkedHashMap<Header, double[]> newDiff = new LinkedHashMap<>();
			for (Entry<Header, double[]> entry : this.yValuesDiff.entrySet()) {
				double[] origDiff = entry.getValue();
				double[] newDiffArr;
				if (entry.getKey().equals(adjustedHeader)) {
					newDiffArr = (delta >= 0) ? Arrays.copyOfRange(origDiff, delta, delta + L)
							: Arrays.copyOfRange(origDiff, 0, L);
				} else {
					newDiffArr = Arrays.copyOfRange(origDiff, cropStart, cropEnd);
				}
				newDiff.put(entry.getKey(), newDiffArr);
			}
			newData.yValuesDiff.clear();
			newData.yValuesDiff.putAll(newDiff);

			LinkedHashMap<Header, double[]> newDeriv = new LinkedHashMap<>();
			for (Entry<Header, double[]> entry : this.yValuesDeriv.entrySet()) {
				double[] origDeriv = entry.getValue();
				double[] newDerivArr;
				if (entry.getKey().equals(adjustedHeader)) {
					newDerivArr = (delta >= 0) ? Arrays.copyOfRange(origDeriv, delta, delta + L)
							: Arrays.copyOfRange(origDeriv, 0, L);
				} else {
					newDerivArr = Arrays.copyOfRange(origDeriv, cropStart, cropEnd);
				}
				newDeriv.put(entry.getKey(), newDerivArr);
			}
			newData.yValuesDeriv.clear();
			newData.yValuesDeriv.putAll(newDeriv);

			newData.flaggedHeaders.clear();
			newData.flaggedHeaders.putAll(this.flaggedHeaders);

			for (Header header : newData.getYHeaders()) {
				newData.calculateDerivative(header, null);
				newData.calculateDiff(header);
			}

			return newData;
		}

	}

	/**
	 * Prints this {@link HemoData} object's data to file. Will display a maximum of
	 * 100 values for X, Y, etc in order to avoid overloading the console. It will
	 * first display the data starting proximall then distally (i.e. for the first
	 * 100 values for X, Y, etc... and then the last 100 values for X, Y, etc...
	 * skipping values in the middle if there are essentially over 100 values)
	 */
	public void printToConsole() {
		int numberResults = Math.min(100, this.xData.length);
		System.out.println("========= HemoData READ FROM START =========");
		System.out.println(xHeader + " " + Utils.getStringFromArray(xData, numberResults));
		for (Entry<Header, double[]> yEn : this.yValues.entrySet()) {
			System.out.println(yEn.getKey() + " ::: " + Utils.getStringFromArray(yEn.getValue(), numberResults));
		}
		for (Entry<Header, double[]> yEn : this.yValuesDiff.entrySet()) {
			System.out.println("Diff " + yEn.getKey() + " ::: " + Utils.getStringFromArray(yEn.getValue(), numberResults));
		}
		for (Entry<Header, double[]> yEn : this.yValuesDeriv.entrySet()) {
			System.out.println("Deriv " + yEn.getKey() + " ::: " + Utils.getStringFromArray(yEn.getValue(), numberResults));
		}
		for (Entry<Header, Set<String>> flags : this.flaggedHeaders.entrySet()) {
			System.out.println("FLAGS for " + flags.getKey() + " are ::: " + flags.getValue().toString());
		}
		System.out.println("========= HemoData READ FROM END =========");
		System.out.println(xHeader + " " + Utils.getStringFromTerminalArray(xData, numberResults));
		for (Entry<Header, double[]> yEn : this.yValues.entrySet()) {
			System.out.println(yEn.getKey() + " ::: " + Utils.getStringFromTerminalArray(yEn.getValue(), numberResults));
		}
		for (Entry<Header, double[]> yEn : this.yValuesDiff.entrySet()) {
			System.out
					.println("Diff " + yEn.getKey() + " ::: " + Utils.getStringFromTerminalArray(yEn.getValue(), numberResults));
		}
		for (Entry<Header, double[]> yEn : this.yValuesDeriv.entrySet()) {
			System.out
					.println("Deriv " + yEn.getKey() + " ::: " + Utils.getStringFromTerminalArray(yEn.getValue(), numberResults));
		}
		for (Entry<Header, Set<String>> flags : this.flaggedHeaders.entrySet()) {
			System.out.println("FLAGS for " + flags.getKey() + " are ::: " + flags.getValue().toString());
		}
		System.out.println("========= DONE =========");

	}

	/**
	 * Compiles this {@link HemoData} object and others into a 2D string array suitable
	 * for printing or saving (outer array is row, inner array has the columns)
	 *
	 * @param data Additional {@link HemoData} objects to include in the compilation.
	 * @return A 2D string array representing the combined data.
	 */
	public String[][] compileForPrint(HemoData... data) {
		return toSaveableStringArray(data);
	}

	/**
	 * Saves this data to a sheet. This must be a CSV file. It will include headers,
	 * X and Y data, differential and derivative data, as well as any flags for all
	 * of the columns.
	 * 
	 * @param file The specified file to save to
	 * @return null if successful, otherwise String describing an error that occured
	 */
	public String saveToSheet(File file) {
		FileWriter outputfile;
		try {
			outputfile = new FileWriter(file);
			CSVWriter writer = new CSVWriter(outputfile);

			writer.writeNext(new String[] { "PRIMARY DATA" });
			ArrayList<String> headers = new ArrayList<String>();
			headers.add(xHeader.getName());
			for (Header header : this.yValues.keySet()) {
				headers.add(header.getName());
			}

			writer.writeNext(headers.toArray(new String[0]));

			// now write the data
			String[][] rows = new String[xData.length][yValues.size() + 1]; // + 1 to include the x columns
			DecimalFormat formatter = new DecimalFormat("#.#####");
			// X
			for (int i = 0; i < rows.length; i++) {
				rows[i][0] = formatter.format(xData[i]);
			}
			// Y
			int columnCurr = 1;
			for (Entry<Header, double[]> yColumn : this.yValues.entrySet()) {
				double[] yData = yColumn.getValue();
				for (int i = 0; i < rows.length; i++) {
					rows[i][columnCurr] = formatter.format(yData[i]);
				}
				columnCurr++;
			}

			for (String[] row : rows) {
				writer.writeNext(row);
			}
			rows = null;
			columnCurr = 0;
			headers.clear();

			// Write differentials
			writer.writeNext(new String[] { "DIFFERENTIALS" });

			if (!yValuesDiff.isEmpty()) {

				// write headers
				for (Header header : this.yValuesDiff.keySet()) {
					headers.add(header.getName());
				}

				writer.writeNext(headers.toArray(new String[0]));

				// write data
				int numVals = yValuesDiff.values().iterator().next().length;
				rows = new String[numVals][yValuesDiff.size()];

				for (Entry<Header, double[]> yDiff : yValuesDiff.entrySet()) {
					double[] yData = yDiff.getValue();
					for (int i = 0; i < rows.length; i++) {

						rows[i][columnCurr] = formatter.format(yData[i]);
					}
					columnCurr++;
				}

				for (String[] row : rows) {
					writer.writeNext(row);
				}
			}

			rows = null;
			columnCurr = 0;
			headers.clear();

			// Write derivatives
			writer.writeNext(new String[] { "" });
			writer.writeNext(new String[] { "DERIVATIVES" });

			if (!yValuesDeriv.isEmpty()) {

				// write headers
				for (Header header : this.yValuesDeriv.keySet()) {
					headers.add(header.getName());
				}

				writer.writeNext(headers.toArray(new String[0]));

				// write data
				int numVals = yValuesDeriv.values().iterator().next().length;
				rows = new String[numVals][yValuesDeriv.size()];

				for (Entry<Header, double[]> yDiff : yValuesDeriv.entrySet()) {
					double[] yData = yDiff.getValue();
					for (int i = 0; i < rows.length; i++) {
						rows[i][columnCurr] = formatter.format(yData[i]);
					}
					columnCurr++;
				}

				for (String[] row : rows) {
					writer.writeNext(row);
				}
			}

			rows = null;
			columnCurr = 0;
			headers.clear();

			// Flags
			writer.writeNext(new String[] { "" });
			writer.writeNext(new String[] { "Flags" });
			for (Entry<Header, Set<String>> flags : this.flaggedHeaders.entrySet()) {
				List<String> flagsForHeader = new ArrayList<String>();
				flagsForHeader.add(flags.getKey().getName());
				for (String flag : flags.getValue()) {
					flagsForHeader.add(flag);
				}
				writer.writeNext(flagsForHeader.toArray(new String[0]));
			}

			writer.close();

		} catch (IOException e) {
			return e.getMessage();
		}

		return null;
	}

	/**
	 * @return maximum number of rows in this {@link HemoData}, which is calculated
	 *         by finding the Y data (or derivative or differential) with the
	 *         largest length
	 */
	public int maxRows() {

		int maxRows = xData.length;
		for (double[] yValue : yValues.values()) {
			if (yValue.length > maxRows) {
				maxRows = yValue.length;
			}
		}
		for (double[] yValueDiff : yValuesDiff.values()) {
			if (yValueDiff.length > maxRows) {
				maxRows = yValueDiff.length;
			}
		}
		for (double[] yValueDeriv : yValuesDeriv.values()) {
			if (yValueDeriv.length > maxRows) {
				maxRows = yValueDeriv.length;
			}
		}

		return maxRows;
	}

	/**
	 * Helpers method. Copies all flags from this {@link HemoData} into the
	 * specified target {@link HemoData}
	 * 
	 * @param target the target {@link HemoData} to copy this object's flags into.
	 */
	private void _copyFlags(HemoData target) {
		for (Entry<Header, Set<String>> flags : this.flaggedHeaders.entrySet()) {
			if (flags.getValue() == null || flags.getValue().isEmpty())
				continue;

			target.flaggedHeaders.put(flags.getKey(), new HashSet<String>(flags.getValue()));

		}

	}

	/**
	 * @param datas array of {@link HemoData} to check
	 * @return maximum number of rows amongst all of the input data
	 */
	public static int maxRows(HemoData... datas) {
		int max = 0;
		for (HemoData hd : datas) {
			int qMax = hd.maxRows();
			if (qMax > max) {
				max = qMax;
			}
		}

		return max;
	}

	/**
	 * Based on the input, calculates the differential. Output arrays is same length
	 * as input. The final array value is the same as the second to last.
	 *
	 * @param input the array to calculate differential
	 * @return output
	 */
	public static double[] calculateDifferential(double[] input) {
		double[] differential = new double[input.length];

		for (int i = 0; i < differential.length - 1; i++) {
			differential[i] = input[i + 1] - input[i];
		}

		differential[differential.length - 1] = differential[differential.length - 2];

		return differential;
	}

	/**
	 * Based on the input, calculates the derivative. Output arrays is same length
	 * as input. The final array value is the same as the second to last.
	 *
	 * @param x the array of X values
	 * @param y the array of Y values
	 * @return output
	 */
	public static double[] calculateDerivative(double[] x, double[] y) {

		double[] deriv = new double[y.length];

		for (int i = 0; i < deriv.length - 1; i++) {
			deriv[i] = (y[i + 1] - y[i]) / (x[i + 1] - x[i]);
		}

		deriv[deriv.length - 1] = deriv[deriv.length - 2];

		return deriv;

	}

	/**
	 * Based on the input, calculates the derivative. Output arrays is same length
	 * as input. The final array value is the same as the second to last.
	 *
	 * @param fixedX the X value to use for calculating derivative (dy/dx)
	 * @param y      the array of Y values
	 * @return output
	 */
	public static double[] calculateDerivativeByFixedXInterval(double fixedX, double[] y) {

		double[] deriv = new double[y.length];

		for (int i = 0; i < deriv.length - 1; i++) {
			deriv[i] = (y[i + 1] - y[i]) / fixedX;
		}

		deriv[deriv.length - 1] = deriv[deriv.length - 2];

		return deriv;

	}

	/**
	 * @return true if the values in the specified list are ascending
	 */
	public static boolean isAscending(double[] list) {
		for (int i = 0; i < list.length - 1; i++) {
			if (list[i] > list[i + 1]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Takes multiple {@link HemoData} and creates a 2D array of [rows][columns].
	 * First row is name row, second row is header row. From there on the
	 * {@link HemoData} is displaced. Raw data, then any differential and derivative
	 * data, is recorded.
	 * 
	 * @param datas The list of {@link HemoData} to print
	 * @return a 2d array of Strings [rows][columns] which could be saved to a file
	 *         (i.e. excel, CSV, tab-separated, etc)
	 */
	public static String[][] toSaveableStringArray(HemoData... datas) {

		String[][] topRows = _listOfHeadersString(datas);

		String[][] concatData = new String[maxRows(datas) + 2][topRows[0].length]; // plus 2 to include name row and
																					// header row

		concatData[0] = topRows[0];
		concatData[1] = topRows[1];

		int columnLevel = 0;
		int rowLevel = 2;
		for (HemoData hd : datas) {
			for (double dTime : hd.xData) {
				concatData[rowLevel][columnLevel] = String.valueOf(dTime);
				rowLevel++;
			}
			rowLevel = 2;
			columnLevel++;

			for (Entry<Header, double[]> yValueCol : hd.yValues.entrySet()) {
				for (double yValAtRow : yValueCol.getValue()) {
					concatData[rowLevel][columnLevel] = String.valueOf(yValAtRow);
					rowLevel++;

				}
				columnLevel++;
				rowLevel = 2;
			}
			rowLevel = 2;

			for (Entry<Header, double[]> yValueDiffCol : hd.yValuesDiff.entrySet()) {
				for (double yValDiffAtRow : yValueDiffCol.getValue()) {
					concatData[rowLevel][columnLevel] = String.valueOf(yValDiffAtRow);
					rowLevel++;
				}
				columnLevel++;
				rowLevel = 2;
			}
			rowLevel = 2;

			for (Entry<Header, double[]> yValueDerivCol : hd.yValuesDeriv.entrySet()) {
				for (double yValDerivAtRow : yValueDerivCol.getValue()) {
					concatData[rowLevel][columnLevel] = String.valueOf(yValDerivAtRow);
					rowLevel++;
				}
				columnLevel++;
				rowLevel = 2;
			}
			rowLevel = 2;

		}

		_fillNulls(concatData);

		return concatData;
	}

	/**
	 * Utility method
	 */
	private static String[][] _listOfHeadersString(HemoData... datas) {
		List<String> sectionName = new ArrayList<String>();
		List<String> headers = new ArrayList<String>();

		for (HemoData data : datas) {
			sectionName.add(data.name);
			headers.add(data.xHeader.getName());
			for (Header header : data.yValues.keySet()) {
				sectionName.add("");
				headers.add(header.getName());
			}
			for (Header header : data.yValuesDiff.keySet()) {
				sectionName.add("");
				headers.add("Diff " + header.getName());
			}
			for (Header header : data.yValuesDeriv.keySet()) {
				sectionName.add("");
				headers.add("Deriv " + header.getName());
			}

		}

		return new String[][] { sectionName.toArray(new String[0]), headers.toArray(new String[0]) };

	}

	/**
	 * Utility method
	 */
	private static void _fillNulls(String[][] target) {

		for (int i = 0; i < target.length; i++) {

			String[] row = target[i];
			for (int j = 0; j < row.length; j++) {
				if (row[j] == null) {
					row[j] = "";
				}
			}
		}
	}

	/**
	 * Calculates the average interval for the specified timeVaues. Uses
	 * {@link BigDecimal} to do this most accurately.
	 */
	public static double calculateAverageInterval(double[] timeValues) {

		// Use BigDecimal for precise calculations
		BigDecimal first = BigDecimal.valueOf(timeValues[0]);
		BigDecimal last = BigDecimal.valueOf(timeValues[timeValues.length - 1]);
		int numberOfIntervals = timeValues.length - 1;

		// Calculate the interval
		BigDecimal totalRange = last.subtract(first);
		BigDecimal interval = totalRange.divide(BigDecimal.valueOf(numberOfIntervals), 20, RoundingMode.HALF_UP);
		interval = interval.setScale(9, RoundingMode.HALF_UP);

		return interval.doubleValue();

	}

	/**
	 * Calculates the frequency of the input time array. Used ultimately for finding
	 * R waves
	 * 
	 * @return frequency in hertz (Hz)
	 */
	public static int calculateHz(double[] timeValues) {
		// Ensure the array is not empty and has at least two values to calculate a
		// period
		if (timeValues == null || timeValues.length < 2) {
			throw new IllegalArgumentException("Array must contain at least two time values.");
		}

		// Sort the array to ensure time values are in ascending order
		Arrays.sort(timeValues);

		// Calculate the periods (time intervals between successive values)
		double[] periods = new double[timeValues.length - 1];
		for (int i = 0; i < timeValues.length - 1; i++) {
			periods[i] = timeValues[i + 1] - timeValues[i];
		}

		// Calculate the average period
		double sum = 0;
		for (double period : periods) {
			sum += period;
		}
		double averagePeriod = sum / periods.length;

		// Calculate the frequency as the inverse of the average period
		double frequency = 1.0 / averagePeriod;

		// Return the frequency as an integer
		return (int) Math.round(frequency);
	}
	
	/**
	 * Finds and returns the canonical {@link Header} instance from this
	 * {@link HemoData} object that is equal to the provided input header. This is
	 * used to ensure object identity when using headers as map keys.
	 *
	 * @param input The header to resolve.
	 * @return The canonical {@link Header} instance if found; otherwise, returns
	 * the original input header, which may be new to this data structure.
	 */
	private Header resolveHeader(Header input) {
	    if (input == null) return null;

	    if (xHeader != null && xHeader.equals(input)) return xHeader;

	    for (Header h : yValues.keySet()) {
	        if (h.equals(input)) return h;
	    }

	    return input; // not found — might be new
	}

	/**
	 * Creates a {@link HemoData} object from a specified {@link File}.
	 * 
	 * @param loadLocation input file, must not be null, exist in the file system,
	 *                     be readable
	 * @return HemoData
	 * @throws SerializationException if there are any issues creating the
	 *                                {@link HemoData} object
	 */
	public static HemoData deserialize(File loadLocation) throws SerializationException {

		if (loadLocation == null)
			throw new SerializationException(
					"Unable to read HemoData stored state from file as the input file was null. ");

		try {
			FileInputStream fileInputStream = new FileInputStream(loadLocation);
			ObjectInputStream objInputStream = new ObjectInputStream(fileInputStream);

			HemoData data = (HemoData) objInputStream.readObject();
			objInputStream.close();
			fileInputStream.close();

			return data;
		} catch (IOException | ClassNotFoundException e) {
			throw new SerializationException(
					"Unable to read WIA data stored state from file. This may be due to lack of access / permission of this program "
							+ "to read files from your file system. Check administrator privileges. System error msg: "
							+ e.getMessage());
		}

	}

	/**
	 * Serializes a {@link HemoData} object to file. Does not check if something
	 * already exists at the path - will just go ahead and overwrite it.
	 * 
	 * @throws SerializationException if there was an issue with saving.
	 */
	public static void serialize(HemoData objToSerialize, File saveLocation) throws SerializationException {

		if (saveLocation == null)
			throw new SerializationException("Unable to save HemoData as the input file was null. ");

		if (!Utils.hasOkayExtension(saveLocation, ".hd")) {
			throw new SerializationException("Passed file with name '" + saveLocation.getName()
					+ "', which has incorrect extension. Expected \".hd\"");
		}

		FileOutputStream fileOutStream = null;
		ObjectOutputStream objOutStream = null;
		try {
			fileOutStream = new FileOutputStream(saveLocation);
			objOutStream = new ObjectOutputStream(fileOutStream);
			objOutStream.writeObject(objToSerialize);

			objOutStream.close();
			fileOutStream.close();
		} catch (IOException e) {
			throw new SerializationException(
					"Unable to write serialized version of HemoData to a file. This could be due to the program not being able to "
							+ "write to your file system (access / permissions issue), or improper file path. This means you will not be able to re-edit the "
							+ "waves you have selected at a later point. System error msg: " + e.getMessage());
		}

	}

}
