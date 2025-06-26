package com.carrington.WIA.stats;

import java.util.ArrayList;
import java.util.List;

import com.carrington.WIA.Utils;


public class  DataCollection {
	
	private List<Double> valuesD = new ArrayList<Double>();
	private List<Boolean> valuesB = new ArrayList<Boolean>();

	private String groupName;
	private final DataType dataType;
	private final boolean expressContAsPercent;
	

	public DataCollection(String groupName, DataType dataType, boolean expressContAsPercent) {
		this.groupName = groupName;
		this.dataType = dataType;
		if (dataType == DataType.CONTINUOUS) {
			this.expressContAsPercent = expressContAsPercent;
		} else {
			this.expressContAsPercent = false;
		}
	}
	
	public boolean getExpressedAsPerc() {
		return this.expressContAsPercent;
	}
	
	public void addValue(double value) {
		this.valuesD.add(value);
	}
	
	public void addValue(boolean value) {
		this.valuesB.add(value);
	}
	
	public double[] getDoubleValues() {
		return Utils.toPrimitiveDouble(valuesD);
	}
	
	public boolean[] getBooleanValues() {
		return Utils.toPrimitiveBoolean(valuesB);
	}
	
	
	public String getGroupName() {
		return this.groupName;
	}
	
	/**
	 * @return true if data type is  {@link DataType#DISCRETE_BOOLEAN}
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
