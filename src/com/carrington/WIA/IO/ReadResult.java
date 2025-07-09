package com.carrington.WIA.IO;

import com.carrington.WIA.DataStructures.HemoData;

/**
 * A data structure to hold the result of a file read operation, containing
 * the hemodynamic data and any errors that occurred.
 */
public class ReadResult {
	
	private final HemoData data;
	private final String errors;
	
	/**
	 * Constructs a new ReadResult.
	 *
	 * @param data   The {@link HemoData} object created from the file.
	 * @param errors A string describing any errors encountered during reading, or null if none
	 */
	public ReadResult(HemoData data, String errors) {
		this.data = data;
		this.errors = errors;
	}
	
	/**
	 * @return the {@link HemoData} constructed by reading file
	 */
	public HemoData getData() {
		return data;
	}
	
	/**
	 * @return errors that occurred, if any ({@code null} if none)
	 */
	public String getErrors() {
		return errors;
	}
	
}
