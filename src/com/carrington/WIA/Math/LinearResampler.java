package com.carrington.WIA.Math;

import java.math.BigDecimal;
import java.math.MathContext;


public class LinearResampler {
   
    
    /**
     * First and last points of the included data will be included be default.
     * 
     * @return resampled data
     */
	public static double[] resampleDatas(double[] data, int numberOfSamples) {
		double[] output = new double[numberOfSamples];

		double scaler = ((double) (data.length -1)) / ((double) (numberOfSamples - 1));

		for (int i = 0; i < numberOfSamples; i++) {

			double indexOfData = i * scaler;

			if (isAlmostInteger(indexOfData)) {
				output[i] = data[(int) Math.round(indexOfData)];
				continue;
			}

			int lower = roundDown(indexOfData);
			int upper = roundUp(indexOfData);
			if (upper > data.length - 1) {
				lower--;
				upper--;
			}
			double fracBelow = (indexOfData - lower);
			double fracAbove = (upper - indexOfData);

			output[i] = (fracAbove * data[lower]) + (fracBelow * data[upper]);

		}

		return output;
	}


    public static ResampleResult resampleDatas(double[] timePoints, double interval, double[]... yValuesArrays) {
        int resampledLength = (int) Math.ceil(timePoints[timePoints.length - 1] / interval) + 1;
        double[] resampledTimePoints = new double[resampledLength];
        double[][] resampledValues = new double[yValuesArrays.length][resampledLength];
        boolean[] isBinaryArray = new boolean[yValuesArrays.length];

        // Precompute whether each yValues array is binary
        for (int i = 0; i < yValuesArrays.length; i++) {
            isBinaryArray[i] = isBinaryArray(yValuesArrays[i]);
        }

        int currentIndex = 0;
        for (int i = 0; i < resampledLength; i++) {
            double timeValue = i * interval;
            resampledTimePoints[i] = timeValue;
            for (int j = 0; j < yValuesArrays.length; j++) {
                double interpolatedValue;
                if (isBinaryArray[j]) {
                    interpolatedValue = 0.0; // Default value for binary arrays
                } else {
                    interpolatedValue = interpolateValue(timePoints, yValuesArrays[j], timeValue, interval, currentIndex);
                }
                resampledValues[j][i] = interpolatedValue;
            }

            // Update the currentIndex for the next iteration
            while (currentIndex < timePoints.length - 1 && timePoints[currentIndex + 1] <= timeValue) {
                currentIndex++;
            }
        }

        // Handle binary values separately to assign them to the nearest resample time
        for (int i = 0; i < yValuesArrays.length; i++) {
            if (isBinaryArray[i]) {
                assignBinaryValuesToResampledPoints(timePoints, yValuesArrays[i], resampledTimePoints, resampledValues[i]);
            }
        }

        return new ResampleResult(resampledTimePoints, resampledValues);
    }

    private static double interpolateValue(double[] timePoints, double[] yValues, double t, double interval, int currentIndex) {
        if (currentIndex == timePoints.length - 1 || timePoints[currentIndex] == t) {
            return yValues[currentIndex];
        }

        // If the resample time is exactly at the current time point
        if (timePoints[currentIndex] == t) {
            return yValues[currentIndex];
        }

        // Perform linear interpolation if within the interval
        if (timePoints[currentIndex] < t && timePoints[currentIndex + 1] > t) {
            double t1 = timePoints[currentIndex];
            double t2 = timePoints[currentIndex + 1];
            double y1 = yValues[currentIndex];
            double y2 = yValues[currentIndex + 1];

            return y1 + (y2 - y1) * ((t - t1) / (t2 - t1));
        }

        // Look forward to find the closest point
        for (int i = currentIndex + 1; i < timePoints.length && Math.abs(timePoints[i] - t) < interval; i++) {
            if (timePoints[i] == t) {
                return yValues[i];
            }
            if (timePoints[i] > t) {
                double t1 = timePoints[currentIndex];
                double t2 = timePoints[i];
                double y1 = yValues[currentIndex];
                double y2 = yValues[i];
                return y1 + (y2 - y1) * ((t - t1) / (t2 - t1));
            }
            currentIndex = i;
        }

        return yValues[currentIndex];
    }

    private static void assignBinaryValuesToResampledPoints(double[] timePoints, double[] binaryValues, double[] resampledTimePoints, double[] resampledValues) {
        double epsilon = 1e-9;

        for (int i = 0; i < timePoints.length; i++) {
            if (Math.abs(binaryValues[i]) > epsilon) {
                double originalTime = timePoints[i];
                double nonZeroValue = binaryValues[i];

                // Find the closest resample time using binary search
                int closestIndex = binarySearchClosest(resampledTimePoints, originalTime);

                // Assign the non-zero value to the closest resample time point
                if (closestIndex != -1) {
                    resampledValues[closestIndex] = nonZeroValue;
                }
            }
        }
    }

    private static int binarySearchClosest(double[] resampledTimePoints, double targetTime) {
        int low = 0;
        int high = resampledTimePoints.length - 1;

        while (low < high) {
            int mid = (low + high) / 2;
            if (resampledTimePoints[mid] < targetTime) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        // Check if low or low-1 is closer to targetTime
        if (low > 0 && Math.abs(resampledTimePoints[low] - targetTime) >= Math.abs(resampledTimePoints[low - 1] - targetTime)) {
            return low - 1;
        } else {
            return low;
        }
    }

    private static boolean isBinaryArray(double[] array) {
        if (array.length == 0) {
            return false;
        }

        BigDecimal epsilon = new BigDecimal("1e-9"); // Tolerance for floating point comparison
        BigDecimal nonZeroValue = null;
        MathContext mc = new MathContext(10);

        for (double v : array) {
            BigDecimal bdValue = BigDecimal.valueOf(v);
            if (bdValue.abs().compareTo(epsilon) <= 0) {
                continue;
            } else if (nonZeroValue == null) {
                nonZeroValue = bdValue;
            } else if (bdValue.subtract(nonZeroValue, mc).abs().compareTo(epsilon) > 0) {
                return false;
            }
        }

        return true;
    }
    

	private static int roundDown(double d) {
		return (int) d;
	}

	private static int roundUp(double d) {
		return roundDown(d) + 1;
	}
	
    private static boolean isAlmostInteger(double value) {
        return Math.abs(value - Math.round(value)) < 1e-5;
    }

}

