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

import org.apache.commons.math3.util.MathArrays;

import com.carrington.WIA.Utils;

/**
 * <h1>Convolution</h1> The Convolution class implements different variations of
 * convolution as provided in numpy <a href=
 * "https://docs.scipy.org/doc/numpy/reference/generated/numpy.convolve.html">convolve()</a>
 * function and scipy.ndimage <a href=
 * "https://docs.scipy.org/doc/scipy/reference/generated/scipy.ndimage.convolve1d.html">convolve1d()</a>
 * function
 * <p>
 *
 * @author Sambit Paul
 * @version 1.1
 */

public class Convolution {
	private double[] signal;
	private double[] kernel;
	private double[] output;

	/**
	 * This constructor initialises the prerequisites required to perform
	 * convolution.
	 * 
	 * @param signal Signal to be convolved
	 * @param window Kernel for convolution
	 */
	public Convolution(double[] signal, double[] window) {
		this.signal = signal;
		this.kernel = window;
		this.output = null;
	}

	/**
	 * This is the default discrete linear convolution procedure which works in full
	 * mode.
	 * 
	 * @return double[] The result of convolution.
	 */
	public double[] convolve() {
		// Works in "full" mode
		this.output = MathArrays.convolve(this.signal, this.kernel);
		return this.output;
	}

	/**
	 * This is the discrete linear convolution procedure which works in the
	 * specified mode.
	 * 
	 * @param mode Mode in which convolution will work. Can be 'full', 'same' or
	 *             'valid'
	 * @throws java.lang.IllegalArgumentException if mode is not full, same or valid
	 * @return double[] Result of convolution.
	 */
	public double[] convolve(String mode) {
		double[] temp = MathArrays.convolve(this.signal, this.kernel);
		if (mode.equals("full")) {
			this.output = temp;
		} else if (mode.equals("same")) {
			this.output = new double[this.signal.length];
			int iterator = Math.abs(temp.length - this.signal.length) / 2;
			for (int i = 0; i < this.output.length; i++) {
				this.output[i] = temp[iterator];
				iterator++;
			}
		} else if (mode.equals("valid")) {
			this.output = new double[this.signal.length - this.kernel.length + 1];
			int iterator = this.kernel.length - 1;
			;
			for (int i = 0; i < this.output.length; i++) {
				this.output[i] = temp[iterator];
				iterator++;
			}
		} else {
			throw new IllegalArgumentException("convolve modes can only be full, same or valid");
		}
		return this.output;
	}

	/**
	 * Performs a "full" discrete linear convolution on the given signal and window.
	 *
	 * @param sig The input signal array.
	 * @param w   The convolution kernel (window).
	 * @return The result of the full convolution.
	 */
	private double[] convolve(double[] sig, double[] w) {
		// Works in "full" mode
		double[] output;
		output = MathArrays.convolve(sig, w);
		return output;
	}

	/**
	 * This method perform convolution using padding in different modes.
	 * 
	 * @param mode Mode in which convolution will work. Can be 'reflect', 'constant'
	 *             or 'nearest', 'mirror' or 'wrap'
	 * @throws java.lang.IllegalArgumentException if kernel size is greater than or
	 *                                            equal to signal length
	 * @return double[] Result of convolution with same length as input signal
	 */
	public double[] convolve1d(String mode) {

		double[] output;
		double[] temp;

		if (mode.equals("reflect") || mode.equals("constant") || mode.equals("nearest") || mode.equals("mirror")
				|| mode.equals("wrap")) {
			int startVal = this.signal.length + this.kernel.length / 2;
			double[] newSignal = padSignal(this.signal, mode);
			temp = this.convolve(newSignal, this.kernel);
			output = splitByIndex(temp, startVal, startVal + this.signal.length);
		} else {
			throw new IllegalArgumentException(
					"convolve1d modes can only be reflect, constant, nearest mirror, " + "or wrap");
		}
		return output;
	}

	/**
	 * This method perform default convolution using padding in 'reflect' modes.
	 * 
	 * @return double[] Result of convolution with same length as input signal
	 */
	public double[] convolve1d() throws IllegalArgumentException {
		// Works in "reflect" mode
		double[] output;
		double[] temp;

		int startVal = this.signal.length + this.kernel.length / 2;
		double[] newSignal = padSignal(this.signal, "reflect");
		temp = this.convolve(newSignal, this.kernel);
		output = splitByIndex(temp, startVal, startVal + this.signal.length);
		return output;
	}
	

    /**
     * This function returns the subset if an array depending on start and stop indices provided.
     * @param arr Array to be manipulated
     * @param start Start index for split
     * @param end Stop index for split
     * @return double[] Sub-array generated by splitting input array by start and end indices
     */
    private static double[] splitByIndex(double[] arr, int start, int end) {
        double[] out = new double[end-start];
        System.arraycopy(arr, start, out, 0, out.length);
        return out;
    }
    
    // Different methods of padding a signal
    /**
     * This function returns the input signal by padding it.
     * The output differs based on the mode of operation.
     * @param signal Signal to be padded
     * @param mode The mode in which padding will take place
     * @throws java.lang.IllegalArgumentException If string mode is not "reflect", "constant", "nearest", "mirror" or "wrap"
     * Mode outputs for signal [a b c d]:
     * "reflect" : [d c b a | a b c d | d c b a]
     * "constant" : [0 0 0 0 | a b c d | 0 0 0 0]
     * "nearest" : [a a a a | a b c d | d d d d]
     * "mirror" : [c d c b | a b c d | c b a b]
     * "wrap" : [a b c d | a b c d | a b c d]
     * @return double[][] Pseudo-inverse of the input matrix
     */
    public static double[] padSignal(double[] signal, String mode) {
        double[] newSignal;
        switch (mode) {
            case "reflect": {
                double[] revSig = Utils.reverse(signal);
                double[] newSig = {};
                newSig = concatenateArray(newSig, revSig);
                newSig = concatenateArray(newSig, signal);
                newSig = concatenateArray(newSig, revSig);
                newSignal = newSig;
                break;
            }
            case "constant": {
                double[] cons = new double[signal.length];
                Arrays.fill(cons, 0);
                double[] newSig = {};
                newSig = concatenateArray(newSig, cons);
                newSig = concatenateArray(newSig, signal);
                newSig = concatenateArray(newSig, cons);
                newSignal = newSig;
                break;
            }
            case "nearest": {
                double[] left = new double[signal.length];
                Arrays.fill(left, signal[0]);
                double[] right = new double[signal.length];
                Arrays.fill(right, signal[signal.length - 1]);

                double[] newSig = {};
                newSig = concatenateArray(newSig, left);
                newSig = concatenateArray(newSig, signal);
                newSig = concatenateArray(newSig, right);
                newSignal = newSig;
                break;
            }
            case "mirror": {
                double[] temp = splitByIndex(signal, 1, signal.length);
                temp = Utils.reverse(temp);
                double[] val = new double[]{temp[1]};
                double[] left = concatenateArray(val, temp);

                temp = splitByIndex(signal, 0, signal.length - 1);
                temp = Utils.reverse(temp);
                val = new double[]{temp[temp.length - 2]};
                double[] right = concatenateArray(temp, val);

                double[] newSig = {};
                newSig = concatenateArray(newSig, left);
                newSig = concatenateArray(newSig, signal);
                newSig = concatenateArray(newSig, right);
                newSignal = newSig;
                break;
            }
            case "wrap": {
                double[] newSig = {};
                newSig = concatenateArray(newSig, signal);
                newSig = concatenateArray(newSig, signal);
                newSig = concatenateArray(newSig, signal);
                newSignal = newSig;
                break;
            }
            default:
                throw new IllegalArgumentException("padSignalforConvolution modes can only be reflect, constant, " +
                        "nearest, mirror, or wrap");
        }
        return newSignal;
    }
    
    

    // Concatenate 2 arrays
    /**
     * This function returns the concatenation of the 2 input arrays.
     * @param arr1 Array to be added first
     * @param arr2 Array to be added second
     * @return double[] Concatenated array
     */
    public static double[] concatenateArray(double[] arr1, double[] arr2) {
        double[] out = new double[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, out, 0, arr1.length);
        System.arraycopy(arr2, 0, out, arr1.length, arr2.length);
        return out;
    }
    



}
