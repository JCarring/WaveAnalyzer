 package com.carrington.WIA.DataStructures;

import java.util.ArrayList;
import java.util.Collection;

/**
 * ArrayList with String type, which is case insensitive and does not allow duplicates.
 */
public class InsensitiveNonDupList extends ArrayList<String> {

	private static final long serialVersionUID = 284118891586559569L;

	public boolean contains(Object ob) {
		if (!(ob instanceof String))
			return false;
		
		String query = (String) ob;
		for (String item : this) {
			if (item.equalsIgnoreCase(query)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean add(String str) {
		
		if (!contains(str)) {
			return super.add(str);

		} else {
			return false;
		}
		
	}
	
	public boolean addAll(Collection<? extends String> col) {
		
		Collection<String> str = new ArrayList<String>();
		
		for (String string : col) {
			
			if (!contains(string)) {
				str.add(string);
			}
		}
		
		return super.addAll(str);
		
	}
	
}
