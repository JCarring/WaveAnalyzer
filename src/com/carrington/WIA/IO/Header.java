package com.carrington.WIA.IO;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Header implements Serializable {
	
	private static final long serialVersionUID = -6505057350436957984L;
	public static final String META_ALIGN = "Align";
	public static final String META_COLOR = "Color";
	public static final String META_DISPLAY_NAME = "Display_Name";
	public static final String META_NO_AXIS = "NO_AXIS";

	
	private final String name;
	private final int columnNumber;
	private final boolean isPrimaryX;
	private Map<String, Object> additionalMeta = new HashMap<String, Object>();
	/**
	 * Generate a new header
	 * 
	 * @param name			Name of the header
	 * @param columnNumber	Column number, CANNOT be less than zero
	 * @param isPrimaryX	If this is a domain header
	 * @throws NullPointerException if name is blank or columnNumber < 0
	 */
	public Header(String name, int columnNumber, boolean isPrimaryX) {
		if (name == null || name.isBlank() || columnNumber < 0)
			throw new NullPointerException("Some header data was null");
		this.columnNumber = columnNumber;
		this.name = name;
		this.isPrimaryX = isPrimaryX;

	}
	public int getCol() {return this.columnNumber;}
	
	public String getName() {return this.name;}
	
	public boolean isPrimaryX() {return this.isPrimaryX;}
	
	public void addAdditionalMeta(String s, Object obj) {
		if (s == null)
			return;
		
		additionalMeta.put(s, obj);
	}
	
	public boolean hasAdditionalMeta(String s) {
		return additionalMeta.containsKey(s);
	}
	
	public Object getAdditionalMeta(String s) {
		return additionalMeta.get(s);
	}
	
	@Override
	public String toString() {
		return this.name + " (column " + this.columnNumber + ")";
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		try {
			Header header = (Header) other;
			return this.name.equalsIgnoreCase(header.name);
		} catch (Exception e) {
			return false;
		}
	
	}
	
	@Override
	public int hashCode() {
        return this.name.hashCode();
    }

	
	
}
