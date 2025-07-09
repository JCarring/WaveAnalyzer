package com.carrington.WIA.stats;

/**
 * Defines the type of data being handled, which can be either continuous or discrete (binary).
 */
@SuppressWarnings("javadoc")
public enum DataType {
	
	CONTINUOUS(false),
	DISCRETE_BOOLEAN(true);
	
	private boolean isBinary;
	
	private DataType(boolean isBinary) {
		this.isBinary = isBinary;
	}
	
	/**
	 * @return true if data type is binary in nature (could be yes/no)
	 */
	public boolean isBinary() {
		return this.isBinary;
	}
	
}
