package com.carrington.WIA.stats;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.inference.TestUtils;

public class ANOVATest {
	
	public static double pValue(double[]... data) {
		
		List<double[]> col = new ArrayList<double[]>();
		for (double[] dat : data) {
			col.add(dat);
		}
		double result = TestUtils.oneWayAnovaPValue(col);
		if (result == 0) {
			return 2.2e-16;
		} else {
			return result;

		}
	}
	
	public static double pValue(List<double[]> data) {
		
		double result =  TestUtils.oneWayAnovaPValue(data);
		if (result == 0) {
			return 2.2e-16;
		} else {
			return result;

		}
	}

}
