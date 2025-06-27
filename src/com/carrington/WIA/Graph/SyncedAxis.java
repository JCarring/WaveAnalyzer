package com.carrington.WIA.Graph;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.data.Range;

import com.carrington.WIA.GUIs.Components.KeyChecker;

/**
 * A custom {@link NumberAxis} that synchronizes its panning and zooming
 * behavior with another SyncedAxis. This allows two separate charts to be
 * navigated together. Panning can be locked to be synchronous or individual.
 */
public class SyncedAxis extends NumberAxis {
	private static final long serialVersionUID = -4303533823977940696L;

	// private Range sharedRange = null;
	private SyncedAxis otherToSyncWith = null;

	private double offset = 0;

	private boolean locked = false;

	/**
	 * Constructs a new SyncedAxis.
	 * @param label The label for the axis.
	 */
	public SyncedAxis(String label) {
		super(label);
		KeyChecker.isShiftPressed();

	}

	/**
	 * Sets the other axis that this axis should synchronize with.
	 * @param otherToSyncWith The other {@link SyncedAxis}.
	 */
	public void setOtherToSyncWith(SyncedAxis otherToSyncWith) {
		this.otherToSyncWith = otherToSyncWith;

	}


	/**
	 * Zooms in on the central value of the axis range by 10%
	 */
	public void zoomInCentrally() {
		resizeRange2(0.9, getRange().getCentralValue());

	}

	/**
	 * Zooms out from the central value of the axis range by 10%
	 */
	public void zoomOutCentrally() {
		resizeRange2(1.1, getRange().getCentralValue());

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
		/*
		 * if ((System.currentTimeMillis() - lastZoom) < 50) { return; } else { lastZoom
		 * = System.currentTimeMillis(); }
		 */
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

	/**
	 * Pans the axis by a given percentage. If Shift is pressed or the axis is locked,
	 * it will also pan the synchronized axis by the same amount. Otherwise, it pans
	 * individually and updates its internal offset.
	 *
	 * @param percent The percentage of the current range to pan by.
	 */
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

	/**
	 * Pans the axis so that its new central value is the one specified.
	 * 
	 * @param xValueCenter The desired new central value of the axis range.
	 * @param defaultRange The default range of the axis, used for context (currently unused).
	 */
	public void setPan(double xValueCenter, Range defaultRange) {

		Range currentRange = getRange();
		double change = xValueCenter - currentRange.getCentralValue();
		offset = offset + change;
		setRange(Range.shift(currentRange, change));
	}

	/**
	 * Locks or unlocks synchronized panning.
	 * 
	 * @param locked True to lock panning, false to unlock.
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	/**
	 * Checks if the axis panning is locked.
	 * 
	 * @return True if locked, false otherwise.
	 */
	public boolean isLocked() {
		return this.locked;
	}
}
