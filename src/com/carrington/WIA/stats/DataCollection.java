package com.carrington.WIA.stats;

import java.util.ArrayList;
import java.util.List;

import com.carrington.WIA.Utils;

/**
 * A container for a collection of data points for a specific group and data
 * type. It can hold either double values (for continuous data) or boolean
 * values (for discrete binary data).
 */
public class DataCollection {

	private List<Double> valuesD = new ArrayList<Double>();
	private List<Boolean> valuesB = new ArrayList<Boolean>();

	private String groupName;
	private final DataType dataType;
	private final boolean expressContAsPercent;

	/**
	 * Constructs a new DataCollection.
	 * 
	 * @param groupName            The name of the data group.
	 * @param dataType             The type of data
	 * @param expressContAsPercent If true, continuous data will be expressed as a
	 *                             percentage.
	 */
	public DataCollection(String groupName, DataType dataType, boolean expressContAsPercent) {
		this.groupName = groupName;
		this.dataType = dataType;
		if (dataType == DataType.CONTINUOUS) {
			this.expressContAsPercent = expressContAsPercent;
		} else {
			this.expressContAsPercent = false;
		}
	}

	/**
	 * Checks if continuous data should be expressed as a percentage.
	 * 
	 * @return True if data is formatted as a percentage, false otherwise.
	 */
	public boolean getExpressedAsPerc() {
		return this.expressContAsPercent;
	}

	/**
	 * Adds a double value to the collection.
	 * 
	 * @param value The double value to add.
	 */
	public void addValue(double value) {
		this.valuesD.add(value);
	}

	/**
	 * Adds a boolean value to the collection.
	 * 
	 * @param value The boolean value to add.
	 */
	public void addValue(boolean value) {
		this.valuesB.add(value);
	}

	/**
	 * Gets all double values as a primitive array.
	 * 
	 * @return A primitive double array of the values.
	 */
	public double[] getDoubleValues() {
		return Utils.toPrimitiveDouble(valuesD);
	}

	/**
	 * Gets all boolean values as a primitive array.
	 * 
	 * @return A primitive boolean array of the values.
	 */
	public boolean[] getBooleanValues() {
		return Utils.toPrimitiveBoolean(valuesB);
	}

	/**
	 * Gets the name of the group.
	 * 
	 * @return The group name.
	 */
	public String getGroupName() {
		return this.groupName;
	}

	/**
	 * @return true if data type is {@link DataType#DISCRETE_BOOLEAN}
	 */
	public boolean isBinary() {
		return dataType.isBinary();
	}

	/**
	 * @return the {@link DataType} of this data
	 */
	public DataType getDataType() {
		return this.dataType;
	}

}
