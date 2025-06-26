package com.carrington.WIA.Graph;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueAxisPlot;
import org.jfree.data.Range;
import org.jfree.data.RangeType;

import com.carrington.WIA.GUIs.Components.KeyChecker;

/**
 * An extension of {@link NumberAxis} that limits the panning and zooming
 * operations to the bounds of the axis's calculated auto-range. This prevents
 * the user from navigating into an area where there is no data.
 */
public class AutoRangeLimitedValueAxis extends NumberAxis {
	private static final long serialVersionUID = -4303533823977940696L;


	/**
	 * Constructs a new axis with a given label.
	 *
	 * @param label  the axis label (<code>null</code> permitted).
	 */
	public AutoRangeLimitedValueAxis(String label) {
		super(label);
		KeyChecker.isShiftPressed();

	}
	
	/**
	 * Calculates the auto-adjusted range for the axis. This method is used as the
	 * basis for determining the boundaries for panning and zooming operations.
	 *
	 * @return The calculated auto range.
	 */
	protected Range calcAutoRange() {
		Plot plot = getPlot();
		if (plot == null) {
			return getDefaultAutoRange(); // no plot, no data
		}

		if (plot instanceof ValueAxisPlot) {
			ValueAxisPlot vap = (ValueAxisPlot) plot;

			Range r =vap.getDataRange(this);
			if (r == null) {
				r = getDefaultAutoRange();
			}

			double upper = r.getUpperBound();
			double lower = r.getLowerBound();
			if (getRangeType() == RangeType.POSITIVE) {
				lower = Math.max(0.0, lower);
				upper = Math.max(0.0, upper);
			} else if (getRangeType() == RangeType.NEGATIVE) {
				lower = Math.min(0.0, lower);
				upper = Math.min(0.0, upper);
			}

			if (getAutoRangeIncludesZero()) {
				lower = Math.min(lower, 0.0);
				upper = Math.max(upper, 0.0);
			}
			double range = upper - lower;

			// if fixed auto range, then derive lower bound...
			double fixedAutoRange = getFixedAutoRange();
			if (fixedAutoRange > 0.0) {
				lower = upper - fixedAutoRange;
			} else {
				// ensure the autorange is at least <minRange> in size...
				double minRange = getAutoRangeMinimumSize();
				if (range < minRange) {
					double expand = (minRange - range) / 2;
					upper = upper + expand;
					lower = lower - expand;
					if (lower == upper) { // see bug report 1549218
						double adjust = Math.abs(lower) / 10.0;
						lower = lower - adjust;
						upper = upper + adjust;
					}
					if (getRangeType() == RangeType.POSITIVE) {
						if (lower < 0.0) {
							upper = upper - lower;
							lower = 0.0;
						}
					} else if (getRangeType() == RangeType.NEGATIVE) {
						if (upper > 0.0) {
							lower = lower - upper;
							upper = 0.0;
						}
					}
				}

				if (getAutoRangeStickyZero()) {
					if (upper <= 0.0) {
						upper = Math.min(0.0, upper + getUpperMargin() * range);
					} else {
						upper = upper + getUpperMargin() * range;
					}
					if (lower >= 0.0) {
						lower = Math.max(0.0, lower - getLowerMargin() * range);
					} else {
						lower = lower - getLowerMargin() * range;
					}
				} else {
					upper = upper + getUpperMargin() * range;
					lower = lower - getLowerMargin() * range;
				}
			}

			return new Range(lower, upper);
		}
		return getDefaultAutoRange();
	}

	/**
	 * Zooms in on a specified range of values. The resulting range is constrained
	 * to stay within the calculated auto-range of the axis.
	 *
	 * @param lowerPercent  the new lower bound as a percentage of the current range.
	 * @param upperPercent  the new upper bound as a percentage of the current range.
	 */
	@Override
	public void zoomRange(double lowerPercent, double upperPercent) {

		Range r = getRange();
		Range autoRange = calcAutoRange();

		double start = r.getLowerBound();
		double length = r.getLength();

		double lower;
		double upper;

		if (isInverted()) {
			lower = start + (length * (1 - upperPercent));
			upper = start + (length * (1 - lowerPercent));
		} else {
			lower = start + length * lowerPercent;
			upper = start + length * upperPercent;
		}

		double corrlower = autoRange.constrain(lower);
		double corrupper = autoRange.constrain(upper);
		if (corrlower >= corrupper) {
			corrupper = corrlower + getAutoRangeMinimumSize();
		}

		setRange(corrlower, corrupper);
	}

	/**
	 * Increases or decreases the axis range by the specified percentage about the
	 * specified anchor value and sends an {@link AxisChangeEvent} to all registered
	 * listeners. The new range is constrained to stay within the calculated
	 * auto-range of the axis.
	 * <P>
	 * To double the length of the axis range, use 200% (2.0). To halve the length
	 * of the axis range, use 50% (0.5).
	 *
	 * @param percent     the resize factor.
	 * @param anchorValue the new central value after the resize.
	 * @see #resizeRange(double)
	 */
	@Override
	public void resizeRange(double percent, double anchorValue) {

		if (percent > 0.0) {
			double halfLength = getRange().getLength() * percent / 2;
			Range adjusted = new Range(anchorValue - halfLength, anchorValue + halfLength);

			Range autoRange = calcAutoRange();
			double corrlower = autoRange.constrain(adjusted.getLowerBound());
			double corrupper = autoRange.constrain(adjusted.getUpperBound());
			if (corrlower >= corrupper) {
				corrupper = corrlower + getAutoRangeMinimumSize();
			}

			setRange(corrlower, corrupper);
		} else {
			setAutoRange(true);
		}
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

		
		if (percent > 0.0) {
			double left = anchorValue - getLowerBound();
			double right = getUpperBound() - anchorValue;
			Range adjusted = new Range(anchorValue - left * percent, anchorValue + right * percent);

			Range autoRange = calcAutoRange();

			double corrlower = autoRange.constrain(adjusted.getLowerBound());
			double corrupper = autoRange.constrain(adjusted.getUpperBound());
			if (corrlower >= corrupper) {
				corrupper = corrlower + getAutoRangeMinimumSize();
			}

			setRange(corrlower, corrupper);
		} else {
			setAutoRange(true);
		}
	}

	/**
	 * Pans the axis by a given percentage. The panning operation is limited by the
	 * calculated auto-range, preventing the view from moving beyond the data bounds.
	 *
	 * @param percent the percentage to pan the axis by (a negative value pans left,
	 * a positive value pans right).
	 */
	@Override
	public void pan(double percent) {
		if (percent == 0.0d) {
			return;
		}

		Range r = getRange();
		Range autoRange = calcAutoRange();

		double length = r.getLength();
		double adj = length * percent;
		double lower = r.getLowerBound() + adj;
		double upper = r.getUpperBound() + adj;

		if (lower < autoRange.getLowerBound()) {
			// calc the max percentage that is possible to pan
			double diff = autoRange.getLowerBound() - r.getLowerBound();
			double perc = diff / r.getLength();
			if (perc == 0.0) {

				return;
			}

			lower = autoRange.getLowerBound();
			upper = r.getUpperBound() + r.getLength() * perc;
		} else if (upper > autoRange.getUpperBound()) {
			// calc the max percentage that is possible to pan
			double diff = r.getUpperBound() - autoRange.getUpperBound();
			double perc = diff / r.getLength();
			if (perc == 0.0) {
				return;
			}

			lower = r.getLowerBound() + r.getLength() * perc;
			upper = autoRange.getUpperBound();
		}

		if (lower >= upper) {
			upper = lower + getAutoRangeMinimumSize();
		}

		setRange(lower, upper);

	}
}
