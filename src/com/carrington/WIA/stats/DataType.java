package com.carrington.WIA.stats;

public enum DataType {
	
	CONTINUOUS(false),
	DISCRETE_BOOLEAN(true);
	
	private boolean isBinary;
	
	private DataType(boolean isBinary) {
		this.isBinary = isBinary;
	}
	
	public boolean isBinary() {
		return this.isBinary;
	}
	
}
