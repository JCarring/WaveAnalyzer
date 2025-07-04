/*
 * Copyright (c) 2020 Sambit Paul
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.carrington.WIA.Math;

import java.util.Arrays;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * <h1>Savitzky–Golay Filter</h1>
 * The Savgol class implements the Savitzky–Golay filter in 4 modes of operation: 'nearest', 'constant', 'mirror', 'wrap'.
 * Reference <a href="https://en.wikipedia.org/wiki/Savitzky%E2%80%93Golay_filter">article</a> for more information on Savitzky–Golay Filters.
 * <p>
 *
 * @author  Sambit Paul
 * @version 2.0
 */
public class Savgol {

    private int windowSize;
    private int polyOrder;
    private double[] output;
    private int deriv;
    private double delta;

    private double[] coeffs;

    /**
     * This constructor initialises the prerequisites required to use Savgol filter.
     * deriv is set to 0 and delta is set to 1
     * @param windowSize Size of the filter window/kernel
     * @param polynomialOrder The order of the polynomial used to fit the samples
     */
    public Savgol(int windowSize, int polynomialOrder) {
        if (polynomialOrder >= windowSize) {
            throw new IllegalArgumentException("polynomialOrder must be less that windowSize");
        }
        this.windowSize = windowSize;
        this.polyOrder = polynomialOrder;
        this.deriv = 0;
        this.delta = 1;
    }

    /**
     * This constructor initialises the prerequisites required to use Savgol filter.
     * @param windowSize Size of the filter window/kernel
     * @param polynomialOrder The order of the polynomial used to fit the samples
     * @param deriv The order of the derivative to compute
     * @param delta The spacing of the samples to which the filter will be applied. Used only if deriv greater than 0
     */
    public Savgol(int windowSize, int polynomialOrder, int deriv, double delta) {
        if (polynomialOrder >= windowSize) {
            throw new IllegalArgumentException("polynomialOrder must be less that windowSize");
        }
        this.windowSize = windowSize;
        this.polyOrder = polynomialOrder;
        this.deriv = deriv;
        this.delta = delta;
    }

    /**
     * Compute the coefficients for a 1-d Savitzky-Golay FIR filter based on the parameters provided.
     * @throws java.lang.IllegalArgumentException if window size is even
     * @return the coefficients for a 1-d Savitzky-Golay FIR filter
     */
    public double[] savgolCoeffs() throws IllegalArgumentException {
        int halflen = this.windowSize/2;
        int rem = this.windowSize%2;

        if (rem == 0) {
            throw new IllegalArgumentException("windowSize must be odd");
        }
        double pos = halflen;

        double[] x = UtilMethods.arange(-pos, this.windowSize-pos, 1);
        x = UtilMethods.reverse(x);

        int[] order = UtilMethods.arange(0, polyOrder+1, 1);

        double[][] A = new double[order.length][x.length];
        for (int i = 0; i<order.length; i++) {
            for (int j = 0; j<x.length; j++) {
                A[i][j] = Math.pow(x[j], order[i]);
            }
        }

        double[] y = new double[order.length];
        Arrays.fill(y, 0);

        y[this.deriv] = CombinatoricsUtils.factorial(this.deriv)/(Math.pow(this.delta, this.deriv));
        A = UtilMethods.pseudoInverse(A);
        this.coeffs = MatrixUtils.createRealMatrix(A).operate(y);

        return this.coeffs;
    }

    /**
     * Convolves the 1-d Savitzky-Golay coefficients with the signals in "nearest" mode
     * @param signal Signal to be filtered
     * @return double[] Filtered signal
     */
    public double[] filter(double[] signal) {
        this.savgolCoeffs();
        Convolution c = new Convolution(signal, this.coeffs);
        this.output = c.convolve1d("nearest");
        return this.output;
    }

    /**
     * Convolves the 1-d Savitzky-Golay coefficients with the signals
     * Operates in 4 modes of convolution for filtering: "nearest", "constant", "mirror", "wrap"
     * @param signal Signal to be filtered
     * @param mode Mode of Filter operation
     * @throws java.lang.IllegalArgumentException if mode is not nearest, constant, mirror or wrap
     * @return double[] Filtered signal
     */
    public double[] filter(double[] signal, String mode) throws IllegalArgumentException {
        if (!mode.equals("nearest") && !mode.equals("constant") && !mode.equals("mirror") && !mode.equals("wrap")) {
            throw new IllegalArgumentException("mode must be mirror, constant, nearest or wrap");
        }
        this.savgolCoeffs();
        Convolution c = new Convolution(signal, this.coeffs);
        this.output = c.convolve1d(mode);
        return this.output;
    }
    
    public static SavGolSettings generateSettings(int window, int polyOrder) {
		if (window < 3 || window % 2 == 0) {
			throw new IllegalArgumentException("Window must be an odd number, greater than or equal to 3.");
		}
		if (polyOrder >= window || polyOrder < 2) {
			throw new IllegalArgumentException("Polynomial order must be a number greater than or equal to 2, and smaller than window size");
		}
    	return new SavGolSettings(window, polyOrder);
    }
    
    public static SavGolSettings generateSettings(String windowString, String polyOrderString) {
    	int window;
		try {
			window = Integer.parseInt(windowString.trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Window must be an odd number, greater than or equal to 3.");
		}

		int polyOrder;
		try {
			polyOrder = Integer.parseInt(polyOrderString.trim());
			
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Polynomial order must be a number greater than or equal to 2, and smaller than window size");
		}
		return generateSettings(window, polyOrder);
    }
    
    public static class SavGolSettings {
    	public final int window;
    	public final int polyOrder;
    	private SavGolSettings(int window, int polyOrder) {
    		this.window = window;
    		this.polyOrder = polyOrder;
    	}
    	
    	public SavGolSettings copy() {
    		return new SavGolSettings(window, polyOrder);
    	}
    }
        
}
