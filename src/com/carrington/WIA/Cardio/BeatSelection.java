package com.carrington.WIA.Cardio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * A selection of {@link Beat}. Just stores a little bit more than what could be
 * stored in a generic {@link List} data structure.
 */
public class BeatSelection {

	private final String name; // variable is final as it is used for hashing, equality
	// key = selection type, value = list of the beats
	private final TreeMap<String, List<Beat>> beatSelections = new TreeMap<String, List<Beat>>(
			String.CASE_INSENSITIVE_ORDER);
	private final TreeMap<String, List<String>> beatSelectionsImages = new TreeMap<String, List<String>>(
			String.CASE_INSENSITIVE_ORDER);

	/**
	 * Create the selection with the specified name, which cannot be modified
	 * (because it is used to test equality).
	 */
	public BeatSelection(String name) {
		this.name = name;
	}

	/**
	 * Adds beat to the specified selection type, which is case insensitive. If the
	 * selection type does not exist then it is created.
	 * 
	 * @param beat				the beat to add
	 * @param selectionSubType	the selection subtype
	 */
	public void addBeat(Beat beat, String selectionSubType) {

		List<Beat> beats = beatSelections.get(selectionSubType);
		if (beats == null) {
			beats = new ArrayList<Beat>();
			beatSelections.put(selectionSubType, beats);
		}
		beats.add(beat);

	}

	/**
	 * Adds beat to the specified selection type, which is case insensitive. If the
	 * selection type does not exist then it is created.
	 * 
	 * @param beat             The beat of interest
	 * @param svgString        The string representation for SVG, which can be saved
	 *                         to a .svg file
	 * @param selectionSubType The type / category for this selection
	 */
	public void addBeat(Beat beat, String svgString, String selectionSubType) {
		List<Beat> beats = beatSelections.get(selectionSubType);
		List<String> beatsImages = beatSelectionsImages.get(selectionSubType);

		if (beats == null) {
			beats = new ArrayList<Beat>();
			beatSelections.put(selectionSubType, beats);
		}
		if (beatsImages == null) {
			beatsImages = new ArrayList<String>();
			beatSelectionsImages.put(selectionSubType, beatsImages);
		}
		beats.add(beat);
		beatsImages.add(svgString);
	}

	/**
	 * @return the beats selected for the specified subtype. Returned list is
	 *         immutable.
	 */
	public List<Beat> getBeats(String selectionSubType) {
		if (!beatSelections.containsKey(selectionSubType)) {
			throw new IllegalArgumentException("Developer error. Selection '" + selectionSubType + "' not found.");
		}
		return Collections.unmodifiableList(beatSelections.get(selectionSubType));
	}

	/**
	 * @return all beats, regardless of subtype. Returned list is immutable.
	 */
	public List<Beat> getBeats() {
		List<Beat> tempBeats = new LinkedList<Beat>();
		for (List<Beat> values : beatSelections.values()) {
			tempBeats.addAll(values);
		}
		return Collections.unmodifiableList(tempBeats);
	}
	
	/**
	 * @return all beat images, regardless of subtype. Returned list is immutable.
	 */
	public List<String> getBeatImages() {
		List<String> tempImages = new LinkedList<String>();
		for (List<String> values : beatSelectionsImages.values()) {
			tempImages.addAll(values);
		}
		return Collections.unmodifiableList(tempImages);
	}

	/**
	 * Gets the beat images selected for the specified subtype. Returned list is immutable
	 */
	public List<String> getBeatsImages(String selectionSubType) {
		if (!beatSelectionsImages.containsKey(selectionSubType)) {
			throw new IllegalArgumentException("Developer error. Selection '" + selectionSubType + "' not found.");
		}
		return Collections.unmodifiableList(beatSelectionsImages.get(selectionSubType));
	}

	/**
	 * @return name of this {@link BeatSelection}
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Compares this {@link BeatSelection} to the specified object. The result is {@code true}
	 * if and only if the argument is not {@code null} and is a {@link BeatSelection}
	 * object that has the same name as this object.
	 *
	 * @param other The object to compare this {@link BeatSelection} against.
	 * @return {@code true} if the given object represents a {@link BeatSelection} equivalent
	 * to this one, {@code false} otherwise.
	 */
	public boolean equals(Object other) {
		if (other == null || !(other instanceof BeatSelection))
			return false;

		return this.name.equals(((BeatSelection) other).name);
	}

	/**
	 * Returns a hash code based on hash code of the name of the selection
	 *
	 * @return A hash code value for this object.
	 */
	public int hashCode() {
		return this.name.hashCode();
	}

	/**
	 * Returns the name of this {@link BeatSelection}
	 *
	 * @return The name of the beat selection.
	 */
	public String toString() {
		return this.name;
	}

	/**
	 * @return total number of beat in this selection, including among all the
	 *         subtype
	 */
	public int getTotalNumberBeats() {
		int counter = 0;

		for (List<Beat> beatSelection : beatSelections.values()) {
			counter += beatSelection.size();
		}
		return counter;
	}

	/**
	 * @return total number of beat in this selection for this given subtype
	 */
	public int getNumberBeats(String selectionSubType) {
		return beatSelections.containsKey(selectionSubType) ? beatSelections.get(selectionSubType).size() : 0;
	}
	
	/**
	 * Prints the start and end times of each beat to the standard output,
	 * categorized by their selection subtype. The format for each line is
	 * "subtype: startTime, endTime".
	 */
	public void print() {
		
		for (Entry<String, List<Beat>> beats : beatSelections.entrySet()) {
			for (Beat beat : beats.getValue()) {
				System.out.println(beats.getKey() + ": " + beat.getTime()[0] + ", " + beat.getTime()[beat.getTime().length - 1]);
			}
		}
		
	}
}
