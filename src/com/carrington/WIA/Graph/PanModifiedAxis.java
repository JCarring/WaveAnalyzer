package com.carrington.WIA.Graph;

import org.jfree.chart.axis.NumberAxis;

import com.carrington.WIA.GUIs.Components.KeyChecker;

public class PanModifiedAxis extends NumberAxis {

	private static final long serialVersionUID = -3909851611382126600L;
	
	public PanModifiedAxis(String name) {
		super(name);
	}
	
	@Override
	public void pan(double percent) {
		if (percent == 0.0d) {
			return;
		} else if (KeyChecker.isControlPressed()) {
			return;
		} else {
			super.pan(percent);
		}
	}
	
}
