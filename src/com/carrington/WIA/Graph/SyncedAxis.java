package com.carrington.WIA.Graph;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.data.Range;

import com.carrington.WIA.GUIs.Components.KeyChecker;

public class SyncedAxis extends NumberAxis {
	private static final long serialVersionUID = -4303533823977940696L;

	// private Range sharedRange = null;
	private SyncedAxis otherToSyncWith = null;

	protected double offset = 0;
	
	public long lastZoom = System.currentTimeMillis();

	private boolean locked = false;

	public SyncedAxis(String label) {
		super(label);
		KeyChecker.isShiftPressed();

	}

	public void setOtherToSyncWith(SyncedAxis otherToSyncWith) {
		this.otherToSyncWith = otherToSyncWith;

	}
	
	public void zoomInCentrally() {
		resizeRange2(0.9,getRange().getCentralValue());

	}
	
	public void zoomOutCentrally() {
		resizeRange2(1.1,getRange().getCentralValue());

	}

	/**
	 * Increases or decreases the axis range by the specified percentage about the
	 * specified anchor value and sends an {@link AxisChangeEvent} to all registered
	 * listeners.
	 * <P>
	 * To double the length of the axis range, use 200% (2.0). To halve the length
	 * of the axis range, use 50% (0.5).
	 * 
	 * @param percent     the resize factor.
	 * @param anchorValue the new central value after the resize.
	 * @see #resizeRange(double)
	 * @since 1.0.13
	 */
	@Override
	public void resizeRange2(double percent, double anchorValue) {
		/*if ((System.currentTimeMillis() - lastZoom) < 50) {
			return;
		} else {
			lastZoom = System.currentTimeMillis();
		}*/
		double left = anchorValue - getLowerBound();
		double right = getUpperBound() - anchorValue;
		Range adjusted = new Range(anchorValue - left * percent, anchorValue + right * percent);

		setRange(adjusted);
		
		double otherAnchor = anchorValue - this.offset + otherToSyncWith.offset;

		double left2 = otherAnchor - otherToSyncWith.getLowerBound();
		double right2 = otherToSyncWith.getUpperBound() - otherAnchor;
		Range adjusted2 = new Range(otherAnchor - left2 * percent, otherAnchor + right2 * percent);

		otherToSyncWith.setRange(adjusted2);

	}



	@Override
	public void pan(double percent) {
		if (percent == 0.0d) {
			return;
		} else if (KeyChecker.isControlPressed()) {
			return;
		}
		
		// shift (sync) or sync pan is enabled
		if (KeyChecker.isShiftPressed() || locked) {
			Range r1 = getRange();
			double length1 = r1.getLength();
			double adj1 = length1 * percent;
			double lower1 = r1.getLowerBound() + adj1;
			double upper1 = r1.getUpperBound() + adj1;
			this.setRange(lower1, upper1);
			otherToSyncWith.setRange(otherToSyncWith.getLowerBound() + adj1, otherToSyncWith.getUpperBound() + adj1);

		} else {
			Range r = getRange();

			double length = r.getLength();
			double adj = length * percent;
			double lower = r.getLowerBound() + adj;
			double upper = r.getUpperBound() + adj;

			this.offset = this.offset + (lower - r.getLowerBound());
			Range newRange = new Range(lower, upper);

			setRange(newRange);
		}

		return;

	}
	
	public void setPan(double xValueCenter, Range defaultRange) {
		//offset = xValueCenter - defaultRange.getCentralValue();
		
		Range currentRange = getRange();
		double change = xValueCenter - currentRange.getCentralValue();
		offset = offset + change;
		setRange(Range.shift(currentRange, change));
	}
	
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
	public boolean isLocked() {
		return this.locked;
	}
}
