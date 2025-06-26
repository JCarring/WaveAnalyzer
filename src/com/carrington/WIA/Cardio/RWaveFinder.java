package com.carrington.WIA.Cardio;

import com.carrington.WIA.Utils;

/**
 * Provides static methods for detecting R-waves in ECG waveform
 */
public abstract class RWaveFinder {
	
	
	/**
	 * sample interval in milliseconds
	 * bufferType of 0 = no buffer, 1 = go to beginning of peak, 2 = highlight whole peak
	 */
	public static double[] findRWave(double[] input, int bufferType, double sampleInterval) {
		//Savgol sav = new Savgol((int)(55000.0 * sampleInterval), 3);
		
		//double[] filtered = sav.filter(input);
		double[] filtered = input;
		double[] d1 = new double[filtered.length - 1];
		
		for (int i = 0; i < d1.length; i++) {
			d1[i] = filtered[i + 1] - filtered[i];
		}
		
		double[] d2 = new double[filtered.length - 2];
		for (int i = 0; i< d2.length; i++) {
			d2[i] = d1[i + 1] - d1[i];
		}
		
		double[] d = new double[d2.length];
		
		for (int i = 0; i < d2.length; i++) {
			d[i] = Math.pow(d2[i], 2.0);
		}
		//double maxRealisticBeats = input.length / 250.0; // 240 bpm
		


		double[][] ordered = Utils.convertToDescending(d);


		double maxRealisticBeats = input.length / 10.0; // 200 bpm
		//double maxRealisticBeats = input.length / 400.0; // 150 bpm
		
		double[] rWaves = new double[filtered.length];

		int beats = -1;
		
		for (int i = 0; i < ordered.length; i++) {
			beats++;
			
			if (beats > maxRealisticBeats) {
				break;
			}

			double[] curr = ordered[i];
			if (rWaves[i] > 0.5) {
				continue;
			}
			
			int currInd = (int) Math.round(curr[0]);
			int start = Math.max(currInd - 75, 0);
			int end = Math.min(currInd + 75, rWaves.length - 1);
			
			for (int r = start; r <= end; r++) {
				rWaves[r] = 1;
			}
			
			

		}
		

		double[] newRwave = new double[rWaves.length];
		
		int beatStart = -1;
		for (int i = 0; i < newRwave.length; i++) {
			if (rWaves[i] > 0.5) {
				if (beatStart == -1) {
					beatStart = i;
				}
			} else {
				
				if (beatStart != -1) {
					
					
					int start = beatStart;
					int end = i - 1;
					
					newRwave[Utils.getIndexOfMax(filtered, start, end)] = 1;
					beatStart = -1;
				}
				
			}
			
		}
		if (bufferType == 1) {
			bufferPeaks(input, true, newRwave, false);

		} else if (bufferType == 2) {
			bufferPeaks(input, false, newRwave, false);
		}
		//increment(newRwave);
		
		return newRwave;
	}
	
	
	/**
	 * A helper method to adjust the detected R-wave markers in the rWaves array.
	 * Depending on the parameters, it can expand the marker to cover the entire peak
	 * or shift the marker to the beginning of the peak's upslope. This method modifies
	 * the rWaves array in place.
	 *
	 * @param original          The original signal data, used to find the edges of a peak.
	 * @param shiftToBeginning  If true, moves the R-wave marker to the start of the peak.
	 * @param rWaves            The array containing R-wave markers (1.0 for a peak) to be modified.
	 * @param fixed             If true, applies a fixed-width buffer around the peak.
	 */
	private static void bufferPeaks(double[] original, boolean shiftToBeginning, double[] rWaves, boolean fixed) {
		
		if (fixed) {
			for (int i = 0 ; i < rWaves.length; i++) {
				
				if (rWaves[i] > 0.5) {
					
					int counter = 0;
					for (int j = i - 1; j >= 0 && j > (i - 5); j--) {
						if (rWaves[j] < 0.5) {
							rWaves[j] = 1;
						} else {
							break;
						}
						counter++;
					}
					
					for (int k = i + 1; k < rWaves.length && k < (i + 5); k++) {
						if (rWaves[k] < 0.5) {
							rWaves[k] = 1;
						} else {
							break;
						}
						counter++;
					}
					
					if (counter > 20) {
						throw new IllegalArgumentException("ASDfa");
					}
					
				}
				
			}
		} else {
			for (int i = 0 ; i < rWaves.length; i++) {
				
				if (rWaves[i] > 0.5) {
					
					// peak found, buffer it.
					
					if (shiftToBeginning) {
						int indexToSet = i;
						for (int j = i - 1; j >= 0; j--) {
							if (original[j] < original[j + 1] && rWaves[j] < 0.5) {
								indexToSet = j;
							} else {
								break;
							}
						}
						
						rWaves[indexToSet] = 1;
						rWaves[i] = 0;
					} else {
						for (int j = i - 1; j >= 0; j--) {
							if (original[j] < original[j + 1] && rWaves[j] < 0.5) {
								rWaves[j] = 1;
							} else {
								break;
							}
						}
						
						for (int k = i + 1; k < rWaves.length; k++) {
							if (original[k] < original[k - 1] && rWaves[k] < 0.5) {
								rWaves[k] = 1;
							} else {
								break;
							}
						}
					}

					
				}
				
			}
		}

	}
	
	/**
	 * Calculates a probability score for each point in the input signal representing
	 * the likelihood of it being an R-wave peak. The score is calculated based on
	 * the values of neighboring points within a fixed window that are above a given
	 * threshold.
	 *
	 * @param input     The input signal data.
	 * @param threshold The amplitude threshold for a point to be considered in the
	 * probability calculation.
	 * @return A double array of the same length as the input, where each element
	 * is the calculated probability score for that index.
	 */
	public static double[] probabilityPeaks(double[] input, double threshold) {
		
		double[] probs = new double[input.length];
		for (int queryIndex = 0 ; queryIndex < input.length ; queryIndex++) {

			
			double prob = 0;
			
			for (int i = queryIndex + 1; i < (queryIndex + 75) && i < input.length; i++) {
				if (input[i] > threshold) {
					prob += ((75 - (i - queryIndex)));
				}
			}
			
			for (int i = queryIndex - 1; i > (queryIndex - 75) && i >= 0; i--) {
				if (input[i] > threshold) {
					
					prob += ((75 - (queryIndex - i)));

				}
			}
			probs[queryIndex] = prob / 11250;

		}
		
		
		return probs;
	}
	
	
	/**
	 * Converts a double array into a binary integer array.
	 *
	 * @param in The input double array. Values greater than 0.5 will be converted to 1,
	 * and all other values will be converted to 0.
	 * @return A new integer array containing only 0s and 1s.
	 */
	public static int[] convertToInt(double[] in) {
		int[] ints = new int[in.length];
		for (int i = 0; i < in.length; i ++) {
			if (in[i] > 0.5) {
				ints[i] = 1;
			} else {
				ints[i] = 0;

			}
		}
		return ints;
	}
	
	
	
}
