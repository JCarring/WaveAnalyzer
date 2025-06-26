package com.carrington.WIA.IO;

import com.carrington.WIA.DataStructures.HemoData;

public class ReadResult {
	
	public final HemoData data;
	public final String errors;
	public ReadResult(HemoData data, String errors) {
		this.data = data;
		this.errors = errors;
	}
	
}
