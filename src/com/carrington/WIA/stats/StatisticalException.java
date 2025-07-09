package com.carrington.WIA.stats;

/**
 * Custom exception class for errors that occur during statistical calculations.
 */
public class StatisticalException extends Exception {

	private static final long serialVersionUID = 3356230328307442417L;

	/**
	 * Constructs a new StatisticalException that wraps another exception.
	 * 
	 * @param e The original exception to be wrapped.
	 */
	public StatisticalException(Exception e) {
		super(e);
	}

	/**
	 * Constructs a new StatisticalException with a specified detail message.
	 * 
	 * @param msg The detail message.
	 */
	public StatisticalException(String msg) {
		super(msg);
	}

}
