package com.carrington.WIA.Math;

import java.util.Arrays;

import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.util.CombinatoricsUtils;

import com.carrington.WIA.Utils;

/**
 * <h1>Savitzky–Golay Filter</h1> The Savgol class implements the Savitzky–Golay
 * filter in 4 modes of operation: 'nearest', 'constant', 'mirror', 'wrap'.
 * Reference <a href=
 * "https://en.wikipedia.org/wiki/Savitzky%E2%80%93Golay_filter">article</a> for
 * more information on Savitzky–Golay Filters.
 * <p>
 *
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
	 * 
	 * @param windowSize      Size of the filter window/kernel
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
	 * 
	 * @param windowSize      Size of the filter window/kernel
	 * @param polynomialOrder The order of the polynomial used to fit the samples
	 * @param deriv           The order of the derivative to compute
	 * @param delta           The spacing of the samples to which the filter will be
	 *                        applied. Used only if deriv greater than 0
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
	 * Compute the coefficients for a 1-d Savitzky-Golay FIR filter based on the
	 * parameters provided.
	 * 
	 * @throws java.lang.IllegalArgumentException if window size is even
	 * @return the coefficients for a 1-d Savitzky-Golay FIR filter
	 */
	public double[] savgolCoeffs() throws IllegalArgumentException {
		int halflen = this.windowSize / 2;
		int rem = this.windowSize % 2;

		if (rem == 0) {
			throw new IllegalArgumentException("windowSize must be odd");
		}
		double pos = halflen;

		double[] x = arange(-pos, this.windowSize - pos, 1);
		x = Utils.reverse(x);

		int[] order = arange(0, polyOrder + 1, 1);

		double[][] A = new double[order.length][x.length];
		for (int i = 0; i < order.length; i++) {
			for (int j = 0; j < x.length; j++) {
				A[i][j] = Math.pow(x[j], order[i]);
			}
		}

		double[] y = new double[order.length];
		Arrays.fill(y, 0);

		y[this.deriv] = CombinatoricsUtils.factorial(this.deriv) / (Math.pow(this.delta, this.deriv));
		A = pseudoInverse(A);
		this.coeffs = MatrixUtils.createRealMatrix(A).operate(y);

		return this.coeffs;
	}

	/**
	 * Convolves the 1-d Savitzky-Golay coefficients with the signals in "nearest"
	 * mode
	 * 
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
	 * Convolves the 1-d Savitzky-Golay coefficients with the signals Operates in 4
	 * modes of convolution for filtering: "nearest", "constant", "mirror", "wrap"
	 * 
	 * @param signal Signal to be filtered
	 * @param mode   Mode of Filter operation
	 * @throws java.lang.IllegalArgumentException if mode is not nearest, constant,
	 *                                            mirror or wrap
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

	/**
	 * Generates and validates Savitzky-Golay filter settings.
	 *
	 * @param window    The filter window size (must be odd, >= 3).
	 * @param polyOrder The polynomial order (must be >= 2 and less than window).
	 * @return A valid {@code SavGolSettings} object.
	 * @throws IllegalArgumentException if the parameters are invalid.
	 */
	public static SavGolSettings generateSettings(int window, int polyOrder) {
		if (window < 3 || window % 2 == 0) {
			throw new IllegalArgumentException("Window must be an odd number, greater than or equal to 3.");
		}
		if (polyOrder >= window || polyOrder < 2) {
			throw new IllegalArgumentException(
					"Polynomial order must be a number greater than or equal to 2, and smaller than window size");
		}
		return new SavGolSettings(window, polyOrder);
	}

	/**
	 * Generates and validates Savitzky-Golay filter settings from string inputs.
	 *
	 * @param windowString    The filter window size as a string.
	 * @param polyOrderString The polynomial order as a string.
	 * @return A valid {@code SavGolSettings} object.
	 * @throws IllegalArgumentException if the parameters are invalid.
	 */
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
			throw new IllegalArgumentException(
					"Polynomial order must be a number greater than or equal to 2, and smaller than window size");
		}
		return generateSettings(window, polyOrder);
	}


    /**
     * This function returns evenly spaced number over a specified interval with a specific step in between each element.
     * This is equivalent of the numpy <a href="https://docs.scipy.org/doc/numpy/reference/generated/numpy.linspace.html">linspace()</a> function.
     * @param start Start value of the sequence
     * @param stop Stop value of the sequence
     * @param step Spacing between elements
     * @throws java.lang.IllegalArgumentException If start value is greater than stop value
     * @return int[] Generated sequence
     */
    public static int[] arange(int start, int stop, int step) {
        if (start > stop) {
            throw new IllegalArgumentException("start cannot be greater than stop");
        }
        int size = (stop - start)/step;
        int[] arr = new int[size];

        int temp = start;
        for (int i=0; i<size; i++){
            arr[i] = temp;
            temp = temp + step;
        }
        return arr;
    }


    /**
     * This function returns evenly spaced number over a specified interval with a specific step in between each element.
     * This is equivalent of the numpy <a href="https://docs.scipy.org/doc/numpy/reference/generated/numpy.linspace.html">linspace()</a> function.
     * @param start Start value of the sequence
     * @param stop Stop value of the sequence
     * @param step Spacing between elements
     * @throws java.lang.IllegalArgumentException If start value is greater than stop value
     * @return double[] Generated sequence
     */
    public static double[] arange(double start, double stop, double step) {
        if (start > stop) {
            throw new IllegalArgumentException("start cannot be greater than stop");
        }
        int size = (int)((stop-start)/step);
        double[] arr = new double[size];

        double temp = start;
        for (int i=0; i<size; i++){
            arr[i] = temp;
            temp = temp + step;
        }
        return arr;
    }

    /**
     * This function returns the pseudo-inverse of a 2D matrix.
     * @param mtrx 2D matrix
     * @return double[][] Pseudo-inverse of the input matrix
     */
    public static double[][] pseudoInverse(double[][] mtrx) {
        RealMatrix M = MatrixUtils.createRealMatrix(mtrx);
        DecompositionSolver solver = new SingularValueDecomposition(M).getSolver();
        return solver.getInverse().getData();
    }
	
	/**
	 * A container for validated Savitzky-Golay filter parameters.
	 */
	public static class SavGolSettings {
		/** The filter window size; must be an odd integer. */
		public final int window;
		/** The order of the polynomial for the filter. */
		public final int polyOrder;

		/**
		 * Constructs a SavGolSettings object.
		 *
		 * @param window    The filter window size.
		 * @param polyOrder The polynomial order.
		 */
		private SavGolSettings(int window, int polyOrder) {
			if (window < 3 || polyOrder < 2) {

			}
			this.window = window;
			this.polyOrder = polyOrder;
		}

		/**
    	 * Creates a copy of the current settings object.
    	 *
    	 * @return A new {@code SavGolSettings} instance with the same values.
    	 */
		public SavGolSettings copy() {
			return new SavGolSettings(window, polyOrder);
		}
	}

}
