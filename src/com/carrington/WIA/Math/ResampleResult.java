package com.carrington.WIA.Math;

public class ResampleResult {
	public double[] timePoints;
	public double[][] values;

	public ResampleResult(double[] timePoints, double[][] values) {
		this.timePoints = timePoints;
		this.values = values;
	}
}
