package com.carrington.WIA.Cardio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import com.carrington.WIA.DataStructures.HemoData;
import com.carrington.WIA.IO.Header;
import com.carrington.WIA.Math.DataResampler;

/**
 * Class that represents a heart beat. It will contain data for a beat, in the
 * form of {@link HemoData}.
 */
public class Beat {

	private final HemoData data;
	
	private static int lastSerializedNumberForEquality = 0;
	private int serializedNumberForEquality;

	/**
	 * @param primary   full primary data set
	 * @param indexFrom start of the crop
	 * @param indexTo   non inclusive
	 */
	public Beat(HemoData primary, int indexFrom, int indexTo) {

		if (primary == null)
			throw new IllegalArgumentException("Primary data for selecting beat cannot be null - developer error.");

		if (indexFrom < 0 || indexTo >= primary.getXData().length) {
			throw new IllegalArgumentException("Your selection falls outside of the data range.");
		}

		if (indexTo - indexFrom <= 3) {
			throw new IllegalArgumentException("Too small of selection for a beat");
		}

		lastSerializedNumberForEquality++;
		this.serializedNumberForEquality = lastSerializedNumberForEquality;
		this.data = primary.subset("Beat " + serializedNumberForEquality, indexFrom, indexTo + 1);

	}

	/**
	 * Used for constructing an ensemble
	 * 
	 * @param beatData
	 */
	private Beat(HemoData beatData) {
		this.data = beatData;
		lastSerializedNumberForEquality++;
		this.serializedNumberForEquality = lastSerializedNumberForEquality;
	}
	
	/**
	 * Used for creating a copy with the same serial number
	 * 
	 * @param beatData
	 * @param serializedNumber
	 */
	private Beat(HemoData beatData, int serializedNumber) {
		this.data = beatData;
		this.serializedNumberForEquality = serializedNumber;
	}

	/**
	 * @return raw {@link HemoData} object containing this {@link Beat}'s data
	 */
	public HemoData getData() {
		return this.data;
	}

	/**
	 * @return array of time values. The time will probably be seconds or
	 *         milliseconds (whatever unit is being used by the {@link HemoData}
	 *         object
	 */
	public double[] getTime() {
		return this.data.getXData();
	}

	/**
	 * @return pressure values from the underlying {@link HemoData}. The units will
	 *         be according to whatever is stored in the {@link HemoData}. The units
	 *         could be checked using {@link HemoData#flagExists(String)}
	 */
	public double[] getPressure() {
		List<Header> pressures = this.data.getHeaderByFlag(HemoData.TYPE_PRESSURE);

		if (pressures == null || pressures.isEmpty())
			return null;
		else
			return this.data.getYData(pressures.get(0));
	}

	/**
	 * @return flow values from the underlying {@link HemoData}. The units will be
	 *         according to whatever is stored in the {@link HemoData}. The units
	 *         could be checked using {@link HemoData#flagExists(String)}
	 */
	public double[] getFlow() {
		List<Header> flow = this.data.getHeaderByFlag(HemoData.TYPE_FLOW);

		if (flow == null || flow.isEmpty())
			return null;
		else
			return this.data.getYData(flow.get(0));
	}

	/**
	 * @return ECG amplitude values from the underlying {@link HemoData}.
	 */
	public double[] getECG() {
		List<Header> ecg = this.data.getHeaderByFlag(HemoData.TYPE_ECG);

		if (ecg == null || ecg.isEmpty())
			return null;
		else
			return this.data.getYData(ecg.get(0));
	}


	/**
	 * Performs ensemble averaging, however different beats can have different data.
	 * 
	 * @param beatsOrig		Set of beats to ensemble
	 * @param zeroBasedTime	If should restart the time stamp at zero.
	 * @param convertToMS	true if should convert the product to milliseconds
	 * @param ensembleType	One of {@link HemoData#ENSEMBLE_SCALE} or {@link HemoData#ENSEMBLE_TRIM}
	 * @return the ensembled {@link Beat} (a new object)
	 */
	public static Beat ensembleAverage(List<Beat> beatsOrig, boolean zeroBasedTime, boolean convertToMS,
			int ensembleType) {

		// make a new list, because we are going to edit the list and don't want ot edit
		// what was passed in
		List<Beat> beats = new ArrayList<Beat>(beatsOrig);

		// find the shortest beat
		int indexBeatMinLength = -1;
		int size = -1;
		int counter = 0;
		for (Beat beat : beats) {
			if (indexBeatMinLength == -1 || beat.getData().getSize() < size) {
				indexBeatMinLength = counter;
				size = beat.getData().getSize();
			}
			counter++;
		}

		// This is the beat we will take. Now simply align all the others
		Beat minBeat = beats.remove(indexBeatMinLength);

		List<HemoData> otherBeatsData = new ArrayList<HemoData>();
		for (Beat beat : beats) {
			otherBeatsData.add(beat.data);
		}
		HemoData ensembledData = minBeat.getData().ensembleAverage(otherBeatsData, ensembleType);
		if (zeroBasedTime) {
			ensembledData.shiftXToZero();
		}
		if (convertToMS) {
			// TODO make this smarter
			ensembledData.convertXUnits(HemoData.UNIT_MILLISECONDS);
		}

		return new Beat(ensembledData);

	}

	/**
	 * Performs ensemble averaging, however different beats can have different data.
	 * Must be either pressure or flow.
	 * 
	 * @param beatsOrig		 Set of beats to ensemble
	 * @param ensembleType	One of {@link HemoData#ENSEMBLE_SCALE} or {@link HemoData#ENSEMBLE_TRIM}
	 * @param name The name of the ensemble beat
	 * @return the ensembled {@link Beat} (a new object)
	 */
	public static Beat ensembleFlowPressure(List<Beat> beatsOrig, int ensembleType, String name) {

		// make a new list, because we are going to edit the list and don't want ot edit
		// what was passed in
		List<Beat> beats = new ArrayList<Beat>(beatsOrig);

		Double avgTimeinterval = null;

		for (Beat beat : beats) {
			double nextAvtTimeInterval = HemoData.calculateAverageInterval(beat.getData().getXData());
			if (avgTimeinterval == null) {
				avgTimeinterval = nextAvtTimeInterval;
			} else if (Math.abs(avgTimeinterval - nextAvtTimeInterval) > 0.0001) {
				throw new IllegalArgumentException("When ensembling beats, all must have the same domain intervals");
			}
		}

		// Accumulate each beat's data
		List<double[]> beatsWithPressure = new ArrayList<double[]>();
		List<double[]> beatsWithFlow = new ArrayList<double[]>();

		String flowUnitFlag = null;
		String pressureUnitFlag = null;

		for (Beat beat : beats) {
			List<Header> qPressureHeader = beat.getData().getHeaderByFlag(HemoData.TYPE_PRESSURE);
			List<Header> qFlowHeader = beat.getData().getHeaderByFlag(HemoData.TYPE_FLOW);

			if (qPressureHeader != null && !qPressureHeader.isEmpty()) {
				beatsWithPressure.add(beat.getData().getYData(qPressureHeader.get(0)));
				List<String> flags = beat.getData().getFlags(qPressureHeader.get(0)).stream()
						.filter(s -> s.equals(HemoData.UNIT_MMHG) || s.equals(HemoData.UNIT_PASCAL))
						.collect(Collectors.toList());

				if (flags.size() != 1) {
					throw new IllegalArgumentException(
							"When ensembling beats, all pressure data must be flagged with a unit type");
				} else if (pressureUnitFlag != null && !flags.get(0).equals(pressureUnitFlag)) {
					throw new IllegalArgumentException(
							"When ensembling beats, all pressure data must have the same units");
				} else {
					pressureUnitFlag = flags.get(0);
				}

			}
			if (qFlowHeader != null && !qFlowHeader.isEmpty()) {
				beatsWithFlow.add(beat.getData().getYData(qFlowHeader.get(0)));

				List<String> flags = beat.getData().getFlags(qFlowHeader.get(0)).stream()
						.filter(s -> s.equals(HemoData.UNIT_MperS) || s.equals(HemoData.UNIT_CMperS))
						.collect(Collectors.toList());

				if (flags.size() != 1) {
					throw new IllegalArgumentException(
							"When ensembling beats, all flow data must be flagged with a unit type");
				} else if (flowUnitFlag != null && !flags.get(0).equals(flowUnitFlag)) {
					throw new IllegalArgumentException("When ensembling beats, all flow data must have the same units");
				} else {
					flowUnitFlag = flags.get(0);
				}
			}
		}

		if (beatsWithPressure.isEmpty() || beatsWithFlow.isEmpty()) {
			throw new IllegalArgumentException("Tried to ensemble beats, however either missing pressure or flow");
		}


		
		int targetArraysSize = -1; 
		
		switch (ensembleType) {
		case HemoData.ENSEMBLE_SCALE:
			// want the largest one (to preserve its data)
			targetArraysSize = 0;
			for (double[] data : beatsWithPressure) {
				targetArraysSize = Math.max(targetArraysSize, data.length);
			}
			for (double[] data : beatsWithFlow) {
				targetArraysSize = Math.max(targetArraysSize, data.length);
			}
			break;
		case HemoData.ENSEMBLE_TRIM:
			// want shortest (trim all larger than the shortest)
			targetArraysSize = Integer.MAX_VALUE;
			for (double[] data : beatsWithPressure) {
				targetArraysSize = Math.min(targetArraysSize, data.length);
			}
			for (double[] data : beatsWithFlow) {
				targetArraysSize = Math.min(targetArraysSize, data.length);
			}
			break;
		}
		

		_ensembleFlowPressureHelperResize(beatsWithPressure, ensembleType, targetArraysSize);
		_ensembleFlowPressureHelperResize(beatsWithFlow, ensembleType, targetArraysSize);

		double[] pressureEnsemble = _ensembleFlowPressureHelperAverage(beatsWithPressure, targetArraysSize);
		double[] flowEnsemble = _ensembleFlowPressureHelperAverage(beatsWithFlow, targetArraysSize);

		HemoData hdOriginalSample = beatsOrig.get(0).getData();
		String xUnits = hdOriginalSample.hasFlag(hdOriginalSample.getXHeader(), HemoData.UNIT_MILLISECONDS)
				? HemoData.UNIT_MILLISECONDS
				: HemoData.UNIT_SECONDS;

		HemoData hd = new HemoData(hdOriginalSample.getFile(), hdOriginalSample.getFileName(), name);

		hd.setXData(new Header("Time", 0, true),
				_fillTimeArray(targetArraysSize, HemoData.calculateAverageInterval(hdOriginalSample.getXData())), xUnits);
		hd.addYData(new Header("Pressure", 1, false), pressureEnsemble, HemoData.TYPE_PRESSURE, pressureUnitFlag);
		hd.addYData(new Header("Flow", 2, false), flowEnsemble, HemoData.TYPE_FLOW, flowUnitFlag);

		Beat ensembledBeat = new Beat(hd);

		return ensembledBeat;

	}

	/**
	 * Helper to {@link Beat#ensembleFlowPressure(List, int, String)} for readability.
	 */
	private static double[] _ensembleFlowPressureHelperAverage(List<double[]> original, int size) {
		double[] output = new double[size];

		for (double[] originalValues : original) {
			for (int i = 0; i < size; i++) {
				output[i] += originalValues[i];
			}
		}

		int originalSize = original.size();

		for (int i = 0; i < size; i++) {
			output[i] = output[i] / (double) originalSize;
		}

		return output;

	}

	/**
	 * Helper to {@link Beat#ensembleFlowPressure(List, int, String)} for readability.
	 */
	private static void _ensembleFlowPressureHelperResize(List<double[]> original, int ensembleType, int minimumSize) {
		ListIterator<double[]> itr = original.listIterator();
		while (itr.hasNext()) {
			double[] originalValues = itr.next();
			double[] resampledValues = null;
			switch (ensembleType) {
			case HemoData.ENSEMBLE_SCALE:
				resampledValues = DataResampler.resample(originalValues, minimumSize);
				break;
			case HemoData.ENSEMBLE_TRIM:
				resampledValues = Arrays.copyOf(originalValues, minimumSize);
				break;
			default:
				throw new IllegalArgumentException("Illegal ensemble type.");
			}
			itr.set(resampledValues);
		}
	}

	/**
	 * Checks if this {@link Beat}'s time intersets with the time of another beat.
	 * Intersection is defined as any time point being a part of both beats. If one
	 * beat ends at 2.0, and another starts at 2.0, this would also be considered
	 * overlap.
	 * 
	 * @param otherBeat to compare
	 * @return true if overlap
	 */
	public boolean overlaps(Beat otherBeat) {

		if (otherBeat == null)
			return false;

		// just need to compare start and end times, because it is a requirement for
		// beats that all of the time data is ASCENDING.

		double[] thisTime = this.getTime();
		double[] otherTime = otherBeat.getTime();

		if (thisTime[0] > otherTime[0]) {

			return !(thisTime[0] >= otherTime[otherTime.length - 1]);

		} else {

			return !(thisTime[thisTime.length - 1] <= otherTime[0]);

		}

	}

	/**
	 * Helper method to fill an array of time stamps, incremented at the specified
	 * interval, for the specified number of samples. This is used when ensembling
	 * beats to create zero-based time at the specified interval for the ensemble
	 */
	private static double[] _fillTimeArray(int numberOfSamples, double interval) {
		BigDecimal increment = new BigDecimal(interval);
		double[] array = new double[numberOfSamples];
		for (int i = 0; i < numberOfSamples; i++) {
			array[i] = increment.multiply(new BigDecimal(i)).doubleValue();
		}
		return array;
	}

	/**
	 * Returns a String representation of this object in the form of "<start time>
	 * to <end time>". The time will probably be seconds or milliseconds (whatever
	 * unit is being used by the {@link HemoData} object
	 */
	@Override
	public String toString() {
		double[] time = getTime();
		return time[0] + " to " + time[time.length - 1];
	}

	@Override
	/**
	 * Checks for equality purely based on an integer ID assigned when this Beat
	 * object is created. No two beats will have the same integer, and the integer
	 * cannot be modified after creation
	 */
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Beat))
			return false;

		return this.serializedNumberForEquality == ((Beat) obj).serializedNumberForEquality;
	}

	@Override
	/**
	 * Hashes based on the unique integer designated to this {@link Beat}
	 */
	public int hashCode() {
		return this.serializedNumberForEquality;
	}
	
	/**
	 * Creates and returns a deep copy of this {@code Beat} object. 
	 * The new {@code Beat} will have a copy of the underlying {@code HemoData} 
	 * and the same serialization number (which is otherwise incremented every time a new Beat() method is created).
	 *
	 * @return A new {@code Beat} object that is a copy of this instance.
	 */
	public Beat makeCopy() {
		return new Beat(data.copy(), serializedNumberForEquality);
	}

}
