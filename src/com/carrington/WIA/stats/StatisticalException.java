package com.carrington.WIA.stats;

public class StatisticalException extends Exception {

	private static final long serialVersionUID = 3356230328307442417L;
	
	
	public StatisticalException(Exception e) {
		super(e);
	}
	public StatisticalException(String msg) {
		super(msg);
	}
	
}
