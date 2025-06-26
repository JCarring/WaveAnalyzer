package com.carrington.WIA.Cardio;

import java.io.Serializable;

/**
 * Representation of a coronary wave. Holds the values related to this. Does NOT
 * store many of the calculates performed on the waves though
 */
public class Wave implements Serializable {

	private static final long serialVersionUID = -5208094380301536330L;
	
	/** The unique name of this wave, used for identification and equality checks. */
	private final String name;
	/** The start time of the wave, by default in milliseconds. */
	private final double timeStart;
	/** The end time of the wave, by default in milliseconds. */
	private final double timeEnd; 
	/** Flag indicating the direction of the wave; true if proximal (forward), false otherwise. */
	private final boolean isProximal;
	/** The starting index of this wave within the source data array. */
	private final int timeArrayStart;
	/** The inclusive ending index of this wave within the source data array. */
	private final int timeArrayEndInclus;

	/** The peak amplitude of the wave. This value is calculated and set post-construction. */
	private double peak;
	/** The timestamp at which the peak amplitude occurs. This value is calculated and set post-construction. */
	private double peakTime;
	/** The calculated cumulative intensity of the wave. This value is set post-construction. */
	private double cumulativeIntensity;

	private final WaveClassification waveType;

	/**
	 * Constructs a new wave. Most of the data is final (except paek, peak time,
	 * cumulative intensity as these must be calculated later).
	 * 
	 * @param name               The name of this wave. Should be unique, because
	 *                           equality for {@link Wave}s is checked using the
	 *                           case sensitive name.
	 * @param waveType           The {@link WaveClassification} for this wave. Use
	 *                           {@link WaveClassification#OTHER} if not one of the
	 *                           pre-specified.
	 * @param timeStart          The time stamp for start of the wave (time value,
	 *                           not index in the time array)
	 * @param timeEnd            The time stamp for end of the wave (time value, not
	 *                           index in the time array)
	 * @param timeArrayStart     The start time array index
	 * @param timeArrayEndInclus The end time array index
	 * @param isProximal         True if the wave is proximal in direction, false
	 *                           otherwise
	 */
	public Wave(String name, WaveClassification waveType, double timeStart, double timeEnd, int timeArrayStart,
			int timeArrayEndInclus, boolean isProximal) {
		this.name = name;
		this.timeStart = timeStart;
		this.timeEnd = timeEnd;
		this.isProximal = isProximal;
		this.timeArrayStart = timeArrayStart;
		this.timeArrayEndInclus = timeArrayEndInclus;
		this.waveType = waveType;
	}

	/**
	 * @return true if the wave is proximal, false otherwise
	 */
	public boolean isProximal() {
		return this.isProximal;
	}

	/**
	 * @return the abbreviation set for this name when it was created
	 */
	public String getAbbrev() {
		return this.name;
	}

	/**
	 * @return array of size 2, as [<start time>, <end time>]
	 */
	public double[] getBoundsTime() {
		return new double[] { timeStart, timeEnd };
	}

	/**
	 * @return array of size 2, as [<start time index>, <end time index>]
	 */
	public int[] getBoundsTimeIndex() {
		return new int[] { timeArrayStart, timeArrayEndInclus };
	}

	/**
	 * @return {@link WaveClassification} type for this {@link Wave}
	 */
	public WaveClassification getType() {
		return this.waveType;
	}

	/**
	 * Sets the value (amplitude) of the peak for this wave.
	 */
	public void setPeak(double d) {
		this.peak = d;
	}

	/**
	 * @return peak (amplitude) of this wave. Must be manually set with
	 *         {@link Wave#setPeak(double)}
	 */
	public double getPeak() {
		return this.peak;
	}

	/**
	 * Sets the index of the time array containing the peak (amplitude) of this wave
	 */
	public void setPeakTime(double d) {
		this.peakTime = d;
	}

	/**
	 * @return index of the time array containing the peak (amplitude) of this wave.
	 *         Must be manually set with {@link Wave#setPeakTime(double)}
	 */
	public double getPeakTime() {
		return this.peakTime;
	}

	/**
	 * Sets the cumulative wave intensity for this wave to the specified value
	 */
	public void setCumulativeIntensity(double d) {
		this.cumulativeIntensity = d;
	}

	/**
	 * @return the cumulative wave intensity for this wave. Must be manually set
	 *         with {@link Wave#setCumulativeIntensity(double)}. If it is not, then
	 *         the value returned will be NaN
	 */
	public double getCumulativeIntensity() {
		return this.cumulativeIntensity;
	}

	/**
	 * @return string representation in the format "<start time> to <end time>"
	 */
	public String timeString() {
		return timeStart + " to " + timeEnd;
	}

	/**
	 * Returns the string representation of this wave, which is its name.
	 *
	 * @return The name of the wave.
	 */
	@Override
	public String toString() {
		return this.name;
	}

	/**
	 * Checks for equality based on the NAME variable for this {@link Wave}
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Wave)) {
			return false;
		}

		else
			return ((Wave) obj).name.equals(this.name);
	}

	/**
	 * Hashing is performed on the object's NAME variable only
	 */
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}



}
