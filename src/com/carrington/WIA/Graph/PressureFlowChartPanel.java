package com.carrington.WIA.Graph;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.text.AttributedString;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.carrington.WIA.Utils;
import com.carrington.WIA.DataStructures.HemoData;
import com.carrington.WIA.DataStructures.WIAData;
import com.carrington.WIA.GUIs.KeyActionReceiver;
import com.carrington.WIA.IO.Header;

/**
 * An interactive {@link ChartPanel} for displaying pressure and flow data from
 * a {@link WIAData} object. It provides different modes for user interaction,
 * such as selecting systole/diastole points and aligning the pressure and flow
 * waveforms.
 */
public class PressureFlowChartPanel extends ChartPanel implements KeyActionReceiver {

	private static final long serialVersionUID = 5158158439696012597L;

	/** The standard color for the pressure line, light blue. */
	public static final Color standardPressureLineColor = new Color(123, 178, 224, 255);
	/** A darker color for the pressure line, used for highlighting, blue. */
	public static final Color darkerPressureLineColor = new Color(0, 82, 153, 255);

	/** The standard color for the flow line, light orange-red. */
	public static final Color standardFlowLineColor = new Color(239, 111, 90, 255);
	/** A darker color for the flow line, used for highlighting, red. */
	public static final Color darkerFlowLineColor = new Color(239, 32, 0, 255);

	/** Mode indicating no selection is active. */
	public static final int MODE_NONE = 0;
	/** Mode for selecting cycle parameters (systole, diastole, wrap around). */
	public static final int MODE_CYCLE = 1;
	/** Mode for aligning waveforms by selecting their peaks. */
	public static final int MODE_ALIGN_PEAK = 2;
	/** Mode for aligning waveforms by manually selecting points. */
	public static final int MODE_ALIGN_MANUAL = 3;

	private Double timeXSystole = null;
	private ValueMarker timeXSystoleMarker = null;
	private Double timeXDiastole = null;
	private ValueMarker timeXDiastoleMarker = null;
	
	private Double timeXCycleEnd = null;
	private ValueMarker timeXCycleEndMarker = null;

	private Double timeXPressureAlign = null;
	private ValueMarker timeXPressureAlignMarker = null;
	private Double timeXFlowAlign = null;
	private ValueMarker timeXFlowAlignMarker = null;

	private WIAData wiaData = null;

	private int mode = MODE_NONE;

	private PFPickListener cyclePickListener;

	private final Font fontCustom;
	private final boolean isPreview;

	/**
	 * Factory method to generate a {@link PressureFlowChartPanel} instance.
	 *
	 * @param data The {@link WIAData} containing the pressure and flow data.
	 * @param font The font to be used for chart labels and titles.
	 * @return A new {@link PressureFlowChartPanel} instance.
	 */
	public static PressureFlowChartPanel generate(WIAData data, boolean isPreview, Font font) {

		return new PressureFlowChartPanel(PressureFlowChart.generate(data, font), data, isPreview, font);

	}

	/**
	 * Constructor to initialize the chart panel.
	 * 
	 * @param chart   The {@link PressureFlowChart} to be displayed.
	 * @param wiaData The data associated with the chart.
	 * @param font    The font used for styling.
	 */
	private PressureFlowChartPanel(PressureFlowChart chart, WIAData wiaData, boolean isPreview, Font font) {
		super(chart);
		this.wiaData = wiaData;
		this.fontCustom = font;
		this.isPreview = isPreview;

		if (this.wiaData.getData().getHeaderByFlag(HemoData.TYPE_PRESSURE).isEmpty()) {
			throw new IllegalArgumentException("No pressure data - developer error");
		} else if (this.wiaData.getData().getHeaderByFlag(HemoData.TYPE_FLOW).isEmpty()) {
			throw new IllegalArgumentException("No flow data - developer error");
		}

		setMouseZoomable(false);
		setMouseWheelEnabled(true);
		setDomainZoomable(true);
		setRangeZoomable(false);
		setFillZoomRectangle(false);

		// for max quality
		setMaximumDrawHeight(5000);
		setMaximumDrawWidth(5000);
		setMinimumDrawHeight(80);
		setMinimumDrawWidth(80);
		setHorizontalAxisTrace(false);
		setVerticalAxisTrace(false);

		try {
			Field mask = ChartPanel.class.getDeclaredField("panMask");
			mask.setAccessible(true);
			mask.set(this, 0);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		this.setPreferredSize(new Dimension(380, 420)); // may not need

		addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			}

			public void mousePressed(MouseEvent e) {
				setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			}

			public void mouseReleased(MouseEvent e) {
				setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			}

			public void mouseEntered(MouseEvent e) {
				if (mode != MODE_NONE) {
					setHorizontalAxisTrace(true);
					setVerticalAxisTrace(true);
					repaint();
					setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
				}

			}

			public void mouseExited(MouseEvent e) {
				setHorizontalAxisTrace(false);
				setVerticalAxisTrace(false);
				repaint();
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		});

	}

	/**
	 * Renders markers on the chart for systole and diastole times if they already
	 * exist in the WIAData.
	 */
	public void displayExistingChoices() {
		XYPlot plot = getChart().getXYPlot();

		if (WIAData.isValidDouble(wiaData.getSystoleTime())) {

			ValueMarker vm = new ValueMarker(wiaData.getSystoleTime());
			vm.setLabel("Systole");
			int[] fontParams = Utils.getFontParams(Utils.getSmallTextFont(), "Systole");

			vm.setLabelOffset(new RectangleInsets((fontParams[0]), -1 * ((3.0 / 4.0) * fontParams[1]), 0, 0));
			vm.setLabelFont(Utils.getSmallTextFont());
			vm.setPaint(new Color(161, 0, 180, 110));
			vm.setStroke(new BasicStroke(1f));
			plot.addDomainMarker(vm);

			this.timeXSystole = wiaData.getSystoleTime();
			this.timeXSystoleMarker = vm;
		}

		if (WIAData.isValidDouble(wiaData.getDiastoleTime())) {

			ValueMarker vm = new ValueMarker(wiaData.getDiastoleTime());
			vm.setLabel("Diastole");
			int[] fontParams = Utils.getFontParams(Utils.getSmallTextFont(), "Diastole");

			vm.setLabelOffset(new RectangleInsets((fontParams[0]), ((3.0 / 4.0) * fontParams[1]), 0, 0));
			vm.setLabelFont(Utils.getSmallTextFont());
			vm.setPaint(new Color(161, 0, 180, 110));
			vm.setStroke(new BasicStroke(1f));
			plot.addDomainMarker(vm);
			this.timeXDiastole = wiaData.getDiastoleTime();
			this.timeXDiastoleMarker = vm;
		}
		
		if (WIAData.isValidDouble(wiaData.getManualCycleEnd())) {

			ValueMarker vm = new ValueMarker(wiaData.getDiastoleTime());
			vm.setPaint(new Color(0, 0, 0, 255));
			vm.setStroke(new BasicStroke(2f));
			plot.addDomainMarker(vm);
			this.timeXCycleEnd = wiaData.getManualCycleEnd();
			this.timeXCycleEndMarker = vm;
			
		}
	}

	/**
	 * Handles a user's selection attempt based on the current interaction mode.
	 * 
	 * @param selectionType 0 for systolic, 1 for diastole
	 */
	private void attemptSelectAlignValues(int selectionType) {

		if (mode != MODE_ALIGN_PEAK && mode != MODE_ALIGN_MANUAL)
			return;

		double[] xy = getXYValueFromScreenPos(
				selectionType == 1 ? true : (selectionType == 2 ? false : (timeXPressureAlign == null ? true : false)));

		if (xy == null)
			return;

		double[] validTime = wiaData.getTime();

		if (xy[0] < validTime[0] || xy[0] > validTime[validTime.length - 1]) {
			Utils.showMessage(Utils.ERROR, "Selection outside of valid range.", this);
			return;
		}

		int xValueIndex = Utils.getClosestIndex(xy[0], validTime);
		double xValueNearest = validTime[xValueIndex];

		if (mode == MODE_ALIGN_PEAK) {

			boolean pressure = selectionType == 1 ? true
					: (selectionType == 2 ? false : (timeXPressureAlign == null ? true : false));

			// Retrieve the dataset from the chart's plot
			XYPlot plot = getChart().getXYPlot();
			int seriesIndex = pressure ? 0 : 1;

			XYDataset dataset = plot.getDataset(seriesIndex);

			// Create an array of y-values from the dataset
			int itemCount = dataset.getItemCount(0);
			double[] dataY = new double[itemCount];
			for (int i = 0; i < itemCount; i++) {
				dataY[i] = dataset.getYValue(0, i);
			}

			if (xy[1] > dataY[xValueIndex]) {
				// search for min
				int indexUpper = -1;
				int indexLower = -1;
				int indexMin = -1;
				// start at the selected x, going upwards, finding when the graph exceeds the
				// selected y (mouse click)
				for (int i = xValueIndex + 1; i < wiaData.getData().getXData().length; i++) {
					if (dataY[i] > xy[1]) {
						indexUpper = i;
						break;
					}
				}

				// start at the selected x, going downwards, finding when the graph exceeds the
				// selected y (mouse click)
				for (int i = xValueIndex - 1; i >= 0; i--) {
					if (dataY[i] > xy[1]) {
						indexLower = i;
						break;
					}
				}

				if (indexUpper != -1 && indexLower != -1) {
					// upper and lower found, i.e. there is a point on graph above and below
					// selected X that exceeds selected Y

					indexMin = Utils.findMinMaxIndex(dataY, indexLower, indexUpper, true);
				} else {
					// upper or lower not found, change the strategy
					// now look for essentially change in sign of derivative and break there

					indexUpper = xValueIndex;
					indexLower = xValueIndex;

					for (int i = xValueIndex + 1; i < wiaData.getData().getXData().length; i++) {
						if (dataY[i] <= dataY[indexUpper]) {
							indexUpper = i;
						} else {
							break;
						}
					}
					for (int i = xValueIndex - 1; i >= 0; i--) {
						if (dataY[i] <= dataY[indexLower]) {
							indexLower = i;
						} else {
							break;
						}
					}

					if (indexUpper == indexLower) {
						// would happen if the selected point is a local max and Y doesn't go down on
						// either side (or there are no more points)
						// UNLIKELY
						indexMin = indexUpper;
					} else {
						indexMin = dataY[indexUpper] < dataY[indexLower] ? indexUpper : indexLower;
					}
				}

				double minValue = wiaData.getData().getXData()[indexMin];
				ValueMarker vm = new ValueMarker(minValue);
				vm.setStroke(new BasicStroke(2f));

				if (pressure) {
					vm.setPaint(darkerPressureLineColor);
					if (this.timeXPressureAlignMarker != null) {
						plot.removeDomainMarker(this.timeXPressureAlignMarker);
					}
					plot.addDomainMarker(vm);
					this.timeXPressureAlign = minValue;
					this.timeXPressureAlignMarker = vm;
				} else {
					vm.setPaint(darkerFlowLineColor);
					if (this.timeXFlowAlignMarker != null) {
						plot.removeDomainMarker(this.timeXFlowAlignMarker);
					}
					plot.addDomainMarker(vm);
					this.timeXFlowAlign = minValue;
					this.timeXFlowAlignMarker = vm;
				}

			} else {
				// search for max

				int indexUpper = -1;
				int indexLower = -1;

				int indexMax = -1;
				// start at the selected x, going upwards, finding when the graph drops below
				// the selected y (mouse click)
				for (int i = xValueIndex + 1; i < wiaData.getData().getXData().length; i++) {
					if (dataY[i] < xy[1]) {
						indexUpper = i;
						break;
					}
				}

				// start at the selected x, going downwards, finding when the graph drops below
				// the selected y (mouse click)
				for (int i = xValueIndex - 1; i >= 0; i--) {
					if (dataY[i] < xy[1]) {
						indexLower = i;
						break;
					}
				}

				if (indexUpper != -1 && indexLower != -1) {
					// upper and lower found, i.e. there is a point on graph above and below
					// selected X that drops below selected Y

					indexMax = Utils.findMinMaxIndex(dataY, indexLower, indexUpper, false);
				} else {
					// upper or lower not found, change the strategy
					// now look for essentially change in sign of derivative and break there

					indexUpper = xValueIndex;
					indexLower = xValueIndex;

					for (int i = xValueIndex + 1; i < wiaData.getData().getXData().length; i++) {
						if (dataY[i] >= dataY[indexUpper]) {
							indexUpper = i;
						} else {
							break;
						}
					}
					for (int i = xValueIndex - 1; i >= 0; i--) {
						if (dataY[i] >= dataY[indexLower]) {
							indexLower = i;
						} else {
							break;
						}
					}

					if (indexUpper == indexLower) {
						// would happen if the selected point is a local max and Y doesn't go down on
						// either side (or there are no more points)
						// UNLIKELY
						indexMax = indexUpper;
					} else {
						indexMax = dataY[indexUpper] < dataY[indexLower] ? indexLower : indexUpper;
					}
				}

				double maxValue = wiaData.getData().getXData()[indexMax];
				ValueMarker vm = new ValueMarker(maxValue);
				vm.setStroke(new BasicStroke(2f));

				if (pressure) {
					vm.setPaint(darkerPressureLineColor);
					if (this.timeXPressureAlignMarker != null) {
						plot.removeDomainMarker(this.timeXPressureAlignMarker);
					}
					plot.addDomainMarker(vm);
					this.timeXPressureAlign = maxValue;
					this.timeXPressureAlignMarker = vm;
				} else {
					vm.setPaint(darkerFlowLineColor);
					if (this.timeXFlowAlignMarker != null) {
						plot.removeDomainMarker(this.timeXFlowAlignMarker);
					}
					plot.addDomainMarker(vm);
					this.timeXFlowAlign = maxValue;
					this.timeXFlowAlignMarker = vm;
				}

			}

			if (cyclePickListener != null) {
				cyclePickListener.setReadyAlign(timeXPressureAlignMarker != null && timeXFlowAlignMarker != null);
			}

		} else if (mode == MODE_ALIGN_MANUAL) {

			boolean pressure = selectionType == 1 ? true
					: (selectionType == 2 ? false : (timeXPressureAlign == null ? true : false));

			ValueMarker vm = new ValueMarker(xValueNearest);
			XYPlot plot = getChart().getXYPlot();
			vm.setStroke(new BasicStroke(2f));

			if (pressure) {
				vm.setPaint(darkerPressureLineColor);
				if (this.timeXPressureAlignMarker != null) {
					plot.removeDomainMarker(this.timeXPressureAlignMarker);
				}
				plot.addDomainMarker(vm);
				this.timeXPressureAlign = xValueNearest;
				this.timeXPressureAlignMarker = vm;
			} else {
				vm.setPaint(darkerFlowLineColor);
				if (this.timeXFlowAlignMarker != null) {
					plot.removeDomainMarker(this.timeXFlowAlignMarker);
				}
				plot.addDomainMarker(vm);
				this.timeXFlowAlign = xValueNearest;
				this.timeXFlowAlignMarker = vm;
			}

			if (cyclePickListener != null) {
				cyclePickListener.setReadyAlign(timeXPressureAlignMarker != null && timeXFlowAlignMarker != null);
			}
		}

	}

	/**
	 * Handles a user's selection attempt based on the current interaction mode.
	 */
	private void attemptSelectingCycleEnd() {
		if (mode != MODE_CYCLE)
			return;

		double[] xy = getXYValueFromScreenPos(true);

		if (xy == null) // wasn't over the graph
			return;

		double[] validTime = wiaData.getTime();

		if (xy[0] < validTime[0]) {
			Utils.showMessage(Utils.ERROR, "Selection outside of valid range.", this);
			return;
		} else if (xy[0] > validTime[validTime.length - 1]) {
			// point clicked was to right of graph, so just keep the end of the cycle where
			// it is (or remove previously send ending)
			resetCycleEnd();
			return;
		}
		
		int xValueIndex = Utils.getClosestIndex(xy[0], validTime);
		double xValueNearest = validTime[xValueIndex];
		if ((xValueNearest - validTime[0]) < 200) { // in milliseconds
			// heart rate should not be > 300 bpm. 
			Utils.showMessage(Utils.ERROR, "The cycle end is too early. Cycle too short.", this);
			return;
		}
		
		
		
		ValueMarker vm = new ValueMarker(xValueNearest);
		vm.setPaint(new Color(0, 0, 0, 255));
		vm.setStroke(new BasicStroke(2f));

		XYPlot plot = getChart().getXYPlot();
		if (timeXCycleEndMarker != null) {
			plot.removeDomainMarker(timeXCycleEndMarker);
		}
		plot.addDomainMarker(vm);

		timeXCycleEnd = xValueNearest;
		timeXCycleEndMarker = vm;
		wiaData.setManualCycleEndIndex(xValueIndex);
		

	}

	/**
	 * Handles a user's selection attempt based on the current interaction mode.
	 * 
	 * @param selectionType 0 = systole, 1 = diastole
	 */
	private void attemptSelectCycle(int selectionType) {

		if (mode != MODE_CYCLE)
			return;

		double[] xy = getXYValueFromScreenPos(true);

		if (xy == null) // wasn't over the graph
			return;

		double[] validTime = wiaData.getTime();

		if (xy[0] < validTime[0] || xy[0] > validTime[validTime.length - 1]) {
			Utils.showMessage(Utils.ERROR, "Selection outside of valid range.", this);
			return;
		}

		int xValueIndex = Utils.getClosestIndex(xy[0], validTime);
		double xValueNearest = validTime[xValueIndex];

		boolean systole;

		switch (selectionType) {
		case 0:
			systole = true;
			break;
		case 1:
			systole = false;
			break;
		default:
			throw new IllegalArgumentException("Invalid selection type in cycle select: " + selectionType);
		}

		if (systole) {
			if (this.timeXDiastole != null && xValueNearest > this.timeXDiastole) {
				Utils.showMessage(Utils.WARN, "Usually systole comes before diastole in these traces.",
						this);
			}
		} else {
			if (this.timeXSystole != null && xValueNearest < this.timeXSystole) {
				Utils.showMessage(Utils.WARN, "Usually diastole comes after systole in these traces.",
						this);
			}
		}

		ValueMarker vm = new ValueMarker(xValueNearest);
		Canvas c = new Canvas();
		FontMetrics fm = c.getFontMetrics(Utils.getSmallTextFont());
		vm.setLabel(systole ? "Systole" : "Diastole");
		int height = fm.getAscent();
		if (systole) {
			int width = fm.stringWidth("Systole");
			vm.setLabelOffset(new RectangleInsets(height, -1 * ((3.0 / 4.0) * width), 0, 0));
		} else {
			int width = fm.stringWidth("Diastole");
			vm.setLabelOffset(new RectangleInsets(height, ((3.0 / 4.0) * width), 0, 0));

		}
		vm.setLabelFont(Utils.getSmallTextFont());
		vm.setPaint(new Color(161, 0, 180, 110));
		vm.setStroke(new BasicStroke(1f));

		XYPlot plot = getChart().getXYPlot();

		if (systole) {
			if (timeXSystoleMarker != null) {
				plot.removeDomainMarker(timeXSystoleMarker);
			}
			plot.addDomainMarker(vm);

			this.timeXSystole = xValueNearest;
			this.timeXSystoleMarker = vm;
			this.wiaData.setSystoleByTimeIndex(xValueIndex);
		} else {
			if (timeXDiastoleMarker != null) {
				plot.removeDomainMarker(timeXDiastoleMarker);
			}
			plot.addDomainMarker(vm);

			this.timeXDiastole = xValueNearest;
			this.timeXDiastoleMarker = vm;
			this.wiaData.setDiastoleByTimeIndex(xValueIndex);
		}

	}

	/**
	 * @return the time marked by the user as being systole. Will be NULL if the
	 *         user did not select a value.
	 */
	public Double getSystole() {
		return this.timeXSystole;
	}

	/**
	 * @return the time marked by the user as being diastole. Will be NULL if the
	 *         user did not select a value.
	 */
	public Double getDiastole() {
		return this.timeXDiastole;
	}

	/**
	 * @return the x value for aligning flow, or null if not set
	 */
	public Double getFlowAlignTime() {
		return timeXFlowAlign;
	}

	/**
	 * @return the x value for aligning pressure, or null if not set
	 */
	public Double getPressureAlignTime() {
		return timeXPressureAlign;
	}

	/**
	 * Resets systole and diastole, as well as align selections, reflexes call to
	 * the {@link PFPickListener}
	 */
	public void resetAllSelections() {

		if (timeXSystole != null) {
			timeXSystole = null;
			getChart().getXYPlot().removeDomainMarker(timeXSystoleMarker);
			timeXSystoleMarker = null;
		}
		if (timeXDiastole != null) {
			timeXDiastole = null;
			getChart().getXYPlot().removeDomainMarker(timeXDiastoleMarker);
			timeXDiastoleMarker = null;
		}
		if (timeXPressureAlign != null) {
			timeXPressureAlign = null;
			getChart().getXYPlot().removeDomainMarker(timeXPressureAlignMarker);
			timeXPressureAlignMarker = null;
		}
		if (timeXFlowAlign != null) {
			timeXFlowAlign = null;
			getChart().getXYPlot().removeDomainMarker(timeXFlowAlignMarker);
			timeXFlowAlignMarker = null;
		}
		if (timeXCycleEnd != null) {
			timeXCycleEnd = null;
			getChart().getXYPlot().removeDomainMarker(timeXCycleEndMarker);
			timeXCycleEndMarker = null;
		}

		wiaData.setSystoleByTimeIndex(-1);
		wiaData.setDiastoleByTimeIndex(-1);
		wiaData.setManualCycleEndIndex(-1);

		if (cyclePickListener != null) {
			cyclePickListener.setReadyAlign(false);
		}

	}
	
	/**
	 * Resets systole only, reflexes call to the {@link PFPickListener}
	 */
	public void resetCycleEnd() {

		if (timeXCycleEnd != null) {
			timeXCycleEnd = null;
			getChart().getXYPlot().removeDomainMarker(timeXCycleEndMarker);
			timeXCycleEndMarker = null;
		}

		wiaData.setManualCycleEndIndex(-1);

	}

	/**
	 * Resets systole only, reflexes call to the {@link PFPickListener}
	 */
	public void resetSystole() {

		if (timeXSystole != null) {
			timeXSystole = null;
			getChart().getXYPlot().removeDomainMarker(timeXSystoleMarker);
			timeXSystoleMarker = null;
		}

		wiaData.setSystoleByTimeIndex(-1);

	}

	/**
	 * Resets diastole only, reflexes call to the {@link PFPickListener}
	 */
	public void resetDiastole() {

		if (timeXDiastole != null) {
			timeXDiastole = null;
			getChart().getXYPlot().removeDomainMarker(timeXDiastoleMarker);
			timeXDiastoleMarker = null;
		}

		this.wiaData.setDiastoleByTimeIndex(-1);

	}

	/**
	 * Resets align selections
	 */
	public void resetAlignSelections() {
		if (timeXPressureAlign != null) {
			timeXPressureAlign = null;
			getChart().getXYPlot().removeDomainMarker(timeXPressureAlignMarker);
			timeXPressureAlignMarker = null;
		}
		if (timeXFlowAlign != null) {
			timeXFlowAlign = null;
			getChart().getXYPlot().removeDomainMarker(timeXFlowAlignMarker);
			timeXFlowAlignMarker = null;
		}
	}

	/**
	 * Used to determine if mouse pointer is over the chart
	 */
	private boolean pointIsOverChart(Point p) {
		Rectangle2D chartrect = getScreenDataArea();
		Point parentUp = getLocationOnScreen();
		Rectangle newRect = new Rectangle(parentUp.x + (int) chartrect.getX(), parentUp.y + (int) chartrect.getY(),
				(int) chartrect.getWidth(), (int) chartrect.getHeight());

		return (newRect.contains(p));

	}

	/**
	 * Return the xValue at which the mouse pointer is over vertically on the graph.
	 * 
	 * It will return null if the mouse point was not actually over the graph.
	 * 
	 * This does not actually confirm the xValue actually has any data.
	 */
	public double[] getXYValueFromScreenPos(Boolean pressureAxis) {

		Point mousePoint = MouseInfo.getPointerInfo().getLocation();

		if (!pointIsOverChart(mousePoint)) {
			return null;
		}

		SwingUtilities.convertPointFromScreen(mousePoint, this); // edits in place without return
		Point2D point2d = translateScreenToJava2D(mousePoint);

		Rectangle2D plotArea = getScreenDataArea();
		XYPlot plot = (XYPlot) getChart().getPlot(); // your plot

		double xVal = plot.getDomainAxis().java2DToValue(point2d.getX(), plotArea, plot.getDomainAxisEdge());
		double yVal;

		if (pressureAxis == null)
			pressureAxis = true;

		if (pressureAxis) {
			yVal = plot.getRangeAxis(0).java2DToValue(point2d.getY(), plotArea, plot.getRangeAxisEdge());

		} else {
			yVal = plot.getRangeAxis(1).java2DToValue(point2d.getY(), plotArea, plot.getRangeAxisEdge());

		}
		return new double[] { xVal, yVal };
	}

	/**
	 * Adds a listener to be notified of selection events.
	 * 
	 * @param listener The listener to add.
	 */
	public void setCyclePickListener(PFPickListener listener) {
		this.cyclePickListener = listener;
	}

	/**
	 * Sets the selection mode
	 * 
	 * @param mode 0 = no selection, 1 = systolic / diastolic, 2 = alignment (1,2),
	 *             3 = align manual
	 */
	public void setSelectMode(int mode) {
		this.mode = mode;
		setHorizontalAxisTrace(mode != 0);
		setVerticalAxisTrace(mode != 0);

		if (mode != 2 && mode != 3) {
			if (timeXPressureAlign != null) {
				timeXPressureAlign = null;
				getChart().getXYPlot().removeDomainMarker(timeXPressureAlignMarker);
				timeXPressureAlignMarker = null;
			}
			if (timeXFlowAlign != null) {
				timeXFlowAlign = null;
				getChart().getXYPlot().removeDomainMarker(timeXFlowAlignMarker);
				timeXFlowAlignMarker = null;
			}
			if (cyclePickListener != null) {
				cyclePickListener.setReadyAlign(false);
			}
		}
		repaint();

	}

	/**
	 * 
	 * This method validates the new data, clears any previous selections/markers,
	 * updates the internal data reference, generates a new chart using the new
	 * data, and repaints the panel.
	 * 
	 * @param newData the new WIAData instance to display
	 */
	public void resetWIAData(WIAData newData) {

		if (newData.getData().getHeaderByFlag(HemoData.TYPE_PRESSURE).isEmpty()) {
			throw new IllegalArgumentException("No pressure data in new WIAData file.");
		}
		if (newData.getData().getHeaderByFlag(HemoData.TYPE_FLOW).isEmpty()) {
			throw new IllegalArgumentException("No flow data in new WIAData file.");
		}

		// Clear any existing selections and markers
		resetAllSelections();

		// Update the data
		this.wiaData = newData;

		// Generate a new chart based on the new data and update the ChartPanel
		JFreeChart newChart = PressureFlowChart.generate(newData, fontCustom);
		setChart(newChart);

		// Repaint the panel to reflect the new data
		repaint();
	}
	
	@Override
	public void keyPressed(int key) {
		
		if (isPreview) {
			switch (key) {
			case KeyEvent.VK_P:
				attemptSelectAlignValues(0);
				break;
			case KeyEvent.VK_F:
				attemptSelectAlignValues(1);
				break;
			}
		} else {
			switch (key) {
			case KeyEvent.VK_S:
				attemptSelectCycle(0);
				break;
			case KeyEvent.VK_D:
				attemptSelectCycle(1);
				break;
			case KeyEvent.VK_E:
				attemptSelectingCycleEnd();
				break;
			case KeyEvent.VK_P:
				attemptSelectAlignValues(0);
				break;
			case KeyEvent.VK_F:
				attemptSelectAlignValues(1);
				break;
			case KeyEvent.VK_R:
				resetAllSelections();
				break;
			}
		}
		
				
	}

	/**
	 * An interface for listeners that need to respond to selection events on the
	 * PressureFlowChartPanel, such as picking systole and diastole times.
	 */
	public interface PFPickListener {

		/**
		 * Called to indicate whether alignment points have been selected and alignment
		 * is ready.
		 * 
		 * @param ready True if both alignment points are set, false otherwise.
		 */
		public void setReadyAlign(boolean ready);

	}

	/**
	 * A custom JFreeChart class for displaying pressure and flow waveforms.
	 */
	private static class PressureFlowChart extends JFreeChart {

		private static final long serialVersionUID = 1822270300297602851L;

		/**
		 * Factory method to generate a {@link PressureFlowChart}
		 * 
		 * @param data The {@link WIAData} for the chart.
		 * @param font The font to use for styling.
		 * @return A new {@link PressureFlowChart} instance.
		 */
		private static PressureFlowChart generate(WIAData data, Font font) {
			return new PressureFlowChart(_createPlot(false, font, data), font);

		}

		/**
		 * Private constructor for the chart.
		 * 
		 * @param plot The fully configured {@link XYPlot}.
		 * @param font The font for styling.
		 */
		private PressureFlowChart(Plot plot, Font font) {

			super("Pressure and Flow", new Font(font.getFamily(), Font.BOLD, (int) (font.getSize() * 1.2)), plot, true);

			setBorderVisible(false);
			setBorderPaint(new Color(0, 0, 0, 0));
			getXYPlot().setDomainPannable(true);
			getXYPlot().setRangePannable(false);

		}

		/**
		 * Creates and configures the {@link XYPlot} for the chart with a specific font.
		 * 
		 * @param printable If true, configures the plot for printing.
		 * @param textFont  The font to use for all text elements.
		 * @param data      The {@link WIAData} to plot.
		 * @return A configured {@link XYPlot}.
		 */
		private static XYPlot _createPlot(boolean printable, Font textFont, WIAData data) {
			XYPlot plot = new XYPlot();

			int textFontSize = textFont.getSize();
			float tickStroke = Math.max(textFontSize / 8.0f, 1.5f);
			double[] time = data.getTime();

			BasicStroke strokeDotted = new BasicStroke(tickStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
					new float[] { 6f, 6f }, 0.0f);

			BasicStroke strokeThickSolid = new BasicStroke(tickStroke);

			// DOMAIN AXIS
			NumberAxis domainAxis = new AutoRangeLimitedValueAxis("Time (ms)");
			domainAxis.setAutoRange(true);
			domainAxis.setAutoRangeIncludesZero(false);

			domainAxis.setLabelFont(textFont);
			domainAxis.setTickLabelFont(textFont);
			domainAxis.setAutoTickUnitSelection(false);
			domainAxis.setTickUnit(
					new NumberTickUnit(Utils.findOptimalTickInterval(time[0], time[time.length - 1], false)));
			domainAxis.setTickMarkStroke(strokeThickSolid);
			domainAxis.setTickMarkInsideLength(textFontSize / 2);
			domainAxis.setTickMarkPaint(Color.BLACK);
			domainAxis.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
			domainAxis.setAxisLineStroke(strokeThickSolid);
			domainAxis.setAxisLinePaint(Color.BLACK);

			plot.setDomainAxis(domainAxis);

			// RANGE AXES

			XYSeries pressureSeries = new XYSeries("Pressure");
			XYSeries flowSeries = new XYSeries("Velocity");

			double[] pressure = convertPressureUnits(data.getData());
			double[] flow = data.getRawFlow();

			for (int i = 0; i < time.length; i++) {
				pressureSeries.add(time[i], pressure[i]);
				flowSeries.add(time[i], flow[i]);
			}

			XYSeriesCollection datasetPressure = new XYSeriesCollection();
			XYSeriesCollection datasetFlow = new XYSeriesCollection();
			datasetPressure.addSeries(pressureSeries);
			datasetFlow.addSeries(flowSeries);

			plot.setDataset(0, datasetPressure);
			plot.setDataset(1, datasetFlow);
			XYLineAndShapeRenderer rendererPressure = new XYLineAndShapeRenderer();
			XYLineAndShapeRenderer rendererFlow = new XYLineAndShapeRenderer();

			// pressure
			rendererPressure.setSeriesPaint(0, standardPressureLineColor);
			rendererPressure.setSeriesStroke(0, strokeThickSolid, false);
			rendererPressure.setAutoPopulateSeriesShape(false);
			rendererPressure.setSeriesShapesVisible(0, false);

			// flow
			rendererFlow.setSeriesPaint(0, standardFlowLineColor);
			rendererFlow.setSeriesStroke(0, strokeDotted, true);
			rendererFlow.setAutoPopulateSeriesShape(false);
			rendererFlow.setSeriesShapesVisible(0, false);
			rendererFlow.setDrawSeriesLineAsPath(true);

			plot.setRenderer(0, rendererPressure);
			plot.setRenderer(1, rendererFlow);

			plot.setOutlineVisible(false);

			NumberAxis rangeAxisPressure = new NumberAxis("BP (mm Hg)");
			rangeAxisPressure.setLabelFont(textFont);
			rangeAxisPressure.setAutoRange(true);
			rangeAxisPressure.setAutoRangeIncludesZero(false);
			rangeAxisPressure.setAutoTickUnitSelection(true);
			rangeAxisPressure.setTickMarkStroke(strokeThickSolid);
			rangeAxisPressure.setTickMarkInsideLength(textFontSize / 2);

			NumberAxis rangeAxisFlow = new NumberAxis("Velocity (m s-1)");
			rangeAxisFlow.setLabelFont(textFont);
			rangeAxisFlow.setAutoRange(true);
			rangeAxisFlow.setAutoRangeIncludesZero(false);
			rangeAxisFlow.setAutoTickUnitSelection(true);
			rangeAxisFlow.setTickMarkStroke(strokeThickSolid);
			rangeAxisFlow.setTickMarkInsideLength(textFontSize / 2);

			AttributedString as = new AttributedString("Velocity (m s-1)");
			as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, 13, 15);
			as.addAttribute(TextAttribute.SIZE, textFont.getSize());
			as.addAttribute(TextAttribute.FAMILY, textFont.getFamily());
			rangeAxisFlow.setAttributedLabel(as);

			rangeAxisPressure.setTickLabelFont(textFont);
			rangeAxisFlow.setTickLabelFont(textFont);
			rangeAxisPressure.setTickMarkPaint(Color.BLACK);
			rangeAxisFlow.setTickMarkPaint(Color.BLACK);
			rangeAxisPressure.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
			rangeAxisFlow.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
			rangeAxisPressure.setAxisLineStroke(strokeThickSolid);
			rangeAxisFlow.setAxisLineStroke(strokeThickSolid);
			rangeAxisPressure.setAxisLinePaint(Color.BLACK);
			rangeAxisFlow.setAxisLinePaint(Color.BLACK);

			plot.setRangeAxis(0, rangeAxisPressure);
			plot.setRangeAxis(1, rangeAxisFlow);
			plot.mapDatasetToRangeAxis(0, 0);
			plot.mapDatasetToRangeAxis(1, 1);
			plot.setDomainGridlinesVisible(false);
			plot.setRangeGridlinesVisible(false);
			plot.setDomainCrosshairPaint(Color.BLACK);

			return plot;
		}

		/**
		 * Converts pressure data to mmHg if it is in Pascals.
		 * 
		 * @param hd The {@link HemoData} containing the pressure data.
		 * @return The pressure data in mmHg.
		 */
		private static double[] convertPressureUnits(HemoData hd) {

			Header pressHeader = hd.getHeaderByFlag(HemoData.TYPE_PRESSURE).get(0);
			double[] data = hd.getYData(pressHeader);

			if (hd.hasFlag(pressHeader, HemoData.UNIT_PASCAL)) {
				data = Utils.convertPascalsToMMHG(data);
			}
			return data;
		}

	}

}
