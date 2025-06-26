package com.carrington.WIA.Cardio;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Representatino of the ccommon wave classifications. It includes their more
 * detailed description, abbreviation, typical direction, and whether is is
 * accelerating or decelerating
 */
public enum WaveClassification implements Serializable {

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