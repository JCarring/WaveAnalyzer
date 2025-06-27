package com.carrington.WIA.Graph;

import org.jfree.chart.axis.NumberAxis;

import com.carrington.WIA.GUIs.Components.KeyChecker;

/**
 * A custom {@link NumberAxis} that modifies the default panning behavior. Panning is
 * disabled when the Control key is pressed, allowing for other mouse-driven
 * actions to occur without moving the axis.
 */
public class PanModifiedAxis extends NumberAxis {

	private static final long serialVersionUID = -3909851611382126600L;
	
	/**
	 * Constructs a new axis
	 *
	 * @param name The label for the axis.
	 */
	public PanModifiedAxis(String name) {
		super(name);
	}
	
	/**
	 * Overrides the default pan method to prevent panning if the Control key is held down.
	 *
	 * @param percent The percentage to pan the axis.
	 */
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
