package com.carrington.WIA.Cardio;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	/**
	 * Representatino of the ccommon wave classifications. It includes their more
	 * detailed description, abbreviation, typical direction, and whether is is
	 * accelerating or decelerating
	 */
	@SuppressWarnings("javadoc")
	public static enum WaveClassification implements Serializable {

		// proximally (forward traveling)
		FCW("Forward Compression Wave (FCW)", "FCW", true, true, 1, new Color(245, 137, 137, 50)),
		FDW("Forward Decompression Wave (FDW)", "FDW", false, true, 2, new Color(245, 211, 137, 50)),
		LFCW("Late Forward Compression Wave (FCW2)", "FCW2", true, true, 3, new Color(177, 245, 137, 50)),

		// distally (backward traveling)
		EBCW("Early Backward Compression Wave (BCWearly)", "BCWearly", false, false, 1, new Color(137, 245, 231, 50)),
		LBCW("Late Backward Compression Wave (BCWlate)", "BCWlate", false, false, 2, new Color(137, 144, 245, 50)),
		BDW("Backward Decompression Wave (BEW)", "BEW", true, false, 3, new Color(234, 137, 245, 50)),

		OTHER("Other", "Other", false, false, 0, new Color(130, 130, 130, 50));

		private String label;
		private String abbreviation;
		private boolean accelerating;
		private boolean proximal;
		private int naturalOrder;
		private Color color;

		private WaveClassification(String label, String abbreviation, boolean accelerating, boolean proximal,
				int naturalOrder, Color color) {
			this.label = label;
			this.accelerating = accelerating;
			this.abbreviation = abbreviation;
			this.proximal = proximal;
			this.naturalOrder = naturalOrder;
			this.color = color;
		}

		/**
		 * @return this program's color representation of this wave
		 */
		public Color getColor() {
			return this.color;
		}

		/**
		 * @return label / longer description of this wave
		 */
		public String label() {
			return this.label;
		}

		/**
		 * @return true if the wave is typically accelerating, false otherwise
		 */
		public boolean isAcclerating() {
			return this.accelerating;
		}

		/**
		 * @return abbreviation for the wave
		 */
		public String abbrev() {
			return this.abbreviation;
		}

		/**
		 * @return true if the wave is proximal, false if distal.
		 */
		public boolean isProximal() {
			return this.proximal;
		}

		/**
		 * @return integer representing the order of waves specified to how this program
		 *         uses them
		 */
		public int getNaturalOrder() {
			return this.naturalOrder;
		}

		/**
		 * @return All enum constant in their order of declaration
		 */
		public static WaveClassification[] getWavesTypesOrdered() {
			return WaveClassification.values();
		}

		/**
		 * @return all enum constants, with the exception that
		 *         {@link WaveClassification#OTHER} is put first.
		 */
		public static WaveClassification[] valuesOtherFirst() {
			List<WaveClassification> classifications = new ArrayList<WaveClassification>();
			classifications.add(WaveClassification.OTHER);
			List<WaveClassification> classifNotOther = new ArrayList<WaveClassification>(Arrays.asList(values()));
			classifNotOther.remove(WaveClassification.OTHER);
			classifications.addAll(classifNotOther);
			return classifications.toArray(new WaveClassification[0]);
		}

		/**
		 * Checks if the waves are ordered correctly physiologically. Goes through all
		 * the proximal then all the distal waves, and makes sure if we are using the
		 * prototypical waves that they are in the correct ordered.
		 * 
		 * @param orderedClassifications The query list
		 * @return true if naturally ordered in terms of physiology
		 */
		public boolean naturallyOrdered(List<WaveClassification> orderedClassifications) {

			int lastProxOrderIndex = -1;
			int lastDistOrderIndex = -1;

			for (WaveClassification classif : orderedClassifications) {
				if (classif == WaveClassification.OTHER)
					continue;

				if (classif.isProximal()) {
					if (lastProxOrderIndex == -1 || classif.naturalOrder >= lastProxOrderIndex) {
						lastProxOrderIndex = classif.naturalOrder;
					} else {
						return false;
					}
				} else {
					if (lastDistOrderIndex == -1 || classif.naturalOrder >= lastDistOrderIndex) {
						lastDistOrderIndex = classif.naturalOrder;
					} else {
						return false;
					}
				}
			}
			return true;
		}

		/**
		 * @return String representation, which is this enum constants abbreviation
		 */
		public String toString() {
			return this.abbreviation;
		}

	}

}

