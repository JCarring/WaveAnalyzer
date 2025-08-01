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
import java.io.File;
import java.lang.reflect.Field;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.carrington.WIA.Utils;
import com.carrington.WIA.Cardio.Wave;
import com.carrington.WIA.Cardio.Wave.WaveClassification;
import com.carrington.WIA.DataStructures.WIAData;
import com.carrington.WIA.GUIs.KeyActionReceiver;

/**
 * An interactive {@link ChartPanel} for displaying and selecting separated
 * (forward and backward) wave intensity data. Users can select wave boundaries
 * and classify them. It can operate in a full interaction mode or a
 * preview-only mode.
 */
public class SepWavePanel extends ChartPanel implements KeyActionReceiver {

	private static final long serialVersionUID = 3212824059614453595L;

	private static final BasicStroke markerStroke = new BasicStroke(2f);

	private final WaveBound[] currentBounds = new WaveBound[2];
	private final Map<Wave, XYAnnotation[]> waveAnnotations = new HashMap<Wave, XYAnnotation[]>();

	private WavePickListener waveListener = null;
	private WIAData wiaData;
	private final boolean isPreviewOnly;
	private final Font fontCustom;
	private boolean trace = false;

	/**
	 * Factory method to generate a {@link SepWavePanel} instance.
	 * 
	 * @param wiaData       The wave intensity data to display.
	 * @param font          The font for styling chart elements.
	 * @param isPreviewOnly If true, the panel operates in a limited, preview-only
	 *                      mode.
	 * 
	 * @return A new {@link SepWavePanel} instance.
	 */
	public static SepWavePanel generate(WIAData wiaData, Font font, boolean isPreviewOnly) {
		// create the chart panel

		if (wiaData == null) {
			throw new IllegalArgumentException("WIA data cannot be null. Developer error.");
		}
		return new SepWavePanel(WavePickerChart.generate(false, font, wiaData), wiaData, font, isPreviewOnly);
	}

	/**
	 * Private constructor for the panel.
	 * 
	 * @param chart         The {@link WavePickerChart} to display.
	 * @param wiaData       The associated wave data.
	 * @param font          The font for styling.
	 * @param isPreviewOnly If true, sets up the panel for limited interaction.
	 */
	private SepWavePanel(WavePickerChart chart, WIAData wiaData, Font font, boolean isPreviewOnly) {
		super(chart);
		this.wiaData = wiaData;
		this.isPreviewOnly = isPreviewOnly;
		this.fontCustom = font;

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

		if (!isPreviewOnly) {
			
			trace = true;
			setHorizontalAxisTrace(true);
			
		} else {
			trace = false;
			setHorizontalAxisTrace(false);
			
		}

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
				if (trace) {
					setHorizontalAxisTrace(true);
					repaint();
				}
				setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			}

			public void mouseExited(MouseEvent e) {
				setHorizontalAxisTrace(false);
				repaint();
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		});

		Utils.unfocusButtons(this);

	}

	/**
	 * Renders annotations for waves that already exist in the WIAData object.
	 */
	public void displayExistingChoices() {
		attemptSelectPreexisting();
	}

	/**
	 * Enables or disables the vertical trace line that follows the mouse cursor.
	 * 
	 * @param trace True to enable the trace, false to disable.
	 */
	public void setTrace(boolean trace) {
		this.trace = trace;
		setHorizontalAxisTrace(trace);
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
	 * Adds a listener to be notified of wave selection events.
	 * 
	 * @param listener The listener to add.
	 */
	public void setWavePickListener(WavePickListener listener) {
		this.waveListener = listener;
	}

	/**
	 * Renders annotations for all pre-existing waves in the dataset.
	 */
	private void attemptSelectPreexisting() {

		XYPlot plot = getChart().getXYPlot();
		for (Wave wave : wiaData.getWaves()) {

			Range yRange = plot.getRangeAxis().getRange();
			Range xRange = new Range(wave.getBoundsTime()[0], wave.getBoundsTime()[1]);
			double yLow = wave.isProximal() ? 0 : yRange.getLowerBound();
			double yHigh = wave.isProximal() ? yRange.getUpperBound() : 0;
			double yForLabel = wave.isProximal() ? getYForTopPos(Utils.getSmallTextFont())
					: getYForBottomPos(Utils.getSmallTextFont());

			XYBoxAnnotation highlightAnnotation = new XYBoxAnnotation(xRange.getLowerBound(), yLow,
					xRange.getUpperBound(), yHigh, null, null, wave.getType().getColor());

			XYTextAnnotation textAnnotation = new XYTextAnnotation(wave.getAbbrev(), xRange.getCentralValue(),
					yForLabel);
			textAnnotation.setFont(Utils.getSmallTextFont());
			textAnnotation.setPaint(Utils.getDarkerColor(wave.getType().getColor()));
			textAnnotation.setOutlinePaint(Utils.getDarkerColor(wave.getType().getColor()));

			plot.getRenderer().addAnnotation(textAnnotation, Layer.FOREGROUND);
			plot.getRenderer().addAnnotation(highlightAnnotation, Layer.BACKGROUND);

			this.waveAnnotations.put(wave, new XYAnnotation[] { textAnnotation, highlightAnnotation });
		}
	}

	/**
	 * Handles a wave boundary selection in the preview-only mode.
	 */
	private void attemptDummySelection() {
		if (!isPreviewOnly)
			return;

		double[] xy = getXYValueFromScreenPos();

		if (xy == null)
			return; // point was not within the chart

		double[] validTime = this.wiaData.getData().getXData();
		// double[] validTime = time; // for testing

		if (xy[0] < validTime[0] || xy[0] > validTime[validTime.length - 1]) {
			Utils.showMessage(Utils.ERROR, "Selected point was outside of the acceptable range.", this);
			return;
		}

		boolean isProx = (xy[1] >= 0);

		Range yRange = getChart().getXYPlot().getRangeAxis().getRange();

		XYLineAnnotation line = new XYLineAnnotation(xy[0], 0, xy[0],
				isProx ? yRange.getUpperBound() : yRange.getLowerBound(), markerStroke, new Color(161, 0, 132, 255));

		WaveBound bound = new WaveBound(xy[0], isProx, line);

		if (currentBounds[0] == null) {
			currentBounds[0] = bound;
			getChart().getXYPlot().addAnnotation(line);
			if (waveListener != null) {
				waveListener.updateDisplayForAddedWave(null);
			}
		} else if (currentBounds[1] == null) {
			currentBounds[1] = bound;

			if (this.currentBounds[0].isProx != currentBounds[1].isProx) {
				Utils.showMessage(Utils.ERROR, 
						"You clicked part of a proximal and part of a distal wave. Wave can only be proximal or distal.",
						this);
				resetCurrWaveSelection();
				return;
			}

			double[] time = wiaData.getData().getXData();
			int index1 = Utils.getClosestIndex(currentBounds[0].xVal, time);
			int index2 = Utils.getClosestIndex(currentBounds[1].xVal, time);
			int indexStart = Math.min(index1, index2);
			int indexEnd = Math.max(index1, index2);

			if (index1 == index2) {
				Utils.showMessage(Utils.ERROR, "Start and end points of the wave are the same", this);
				resetCurrWaveSelection();
				return;
			}

			XYPlot plot = getChart().getXYPlot();
			plot.addAnnotation(line);

			Range xRange = new Range(time[indexStart], time[indexEnd]);
			double yLow = currentBounds[0].isProx ? 0 : yRange.getLowerBound();
			double yHigh = currentBounds[0].isProx ? yRange.getUpperBound() : 0;

			XYBoxAnnotation highlightAnnotation = new XYBoxAnnotation(xRange.getLowerBound(), yLow,
					xRange.getUpperBound(), yHigh, null, null, new Color(161, 0, 132, 50));

			plot.getRenderer().addAnnotation(highlightAnnotation, Layer.BACKGROUND);

			Wave wave = new Wave("Preview", WaveClassification.OTHER, time[indexStart], time[indexEnd], indexStart,
					indexEnd, this.currentBounds[0].isProx);
			wiaData.calculateWavePeaksAndSum(wave);
			if (waveListener != null) {
				waveListener.updateDisplayForAddedWave(wave); 
			}
		} else {
			resetCurrWaveSelection();
			if (waveListener != null) {
				waveListener.updateDisplayForAddedWave(null);
			}
			getChart().getXYPlot().addAnnotation(line);
			currentBounds[0] = bound;
			currentBounds[1] = null;
		}

	}
	
	/**
	 * Deletes wave where the mouse is at, if one exists
	 */
	private void deleteWaveSelection() {
		if (isPreviewOnly) return;
		
		double[] xy = getXYValueFromScreenPos();

		if (xy == null)
			return; // point was not within the chart

		double[] validTime = this.wiaData.getData().getXData();

		if (xy[0] < validTime[0] || xy[0] > validTime[validTime.length - 1]) {
			// there wouldn't be a wave here
			return;
		}

		boolean isProx = (xy[1] >= 0);
		
		List<Wave> wavesToRemove = new ArrayList<Wave>();
		
		for (Wave waveQuery : wiaData.getWaves()) {
			
			if (isProx != waveQuery.isProximal()) {
				continue;
			}
			double[] waveTime = waveQuery.getBoundsTime();
			
			if (xy[0] >= waveTime[0] && xy[0] <= waveTime[1]) {
				wavesToRemove.add(waveQuery);
			}
		}
		
		if (!wavesToRemove.isEmpty()) {
			for (Wave waveToRemove : wavesToRemove) {
				removeWave(waveToRemove);
				if (waveListener != null) {
					waveListener.updateDisplayForRemovedWave(waveToRemove);
				}
			}
			
		}

		
	}

	/**
	 * Attempts to select a wave point. Will display error message if needed.
	 */
	private void attemptSelection(WaveClassification waveType) {

		if (isPreviewOnly)
			return;

		if (this.currentBounds[0] != null && this.currentBounds[1] != null) {
			// two bounds were already picked. Now we need to add the wave.

			if (this.currentBounds[0].isProx != this.currentBounds[1].isProx) {
				Utils.showMessage(Utils.ERROR, 
						"You clicked part of a proximal and part of a distal wave. Wave can only be proximal or distal.",
						this);
				resetCurrWaveSelection();
				return;
			}
			double[] time = wiaData.getData().getXData(); // for testing, comment out and use below
			// double[] time = WavePickerChartPanel.time;
			int index1 = Utils.getClosestIndex(currentBounds[0].xVal, time);
			int index2 = Utils.getClosestIndex(currentBounds[1].xVal, time);

			if (index1 == index2) {
				Utils.showMessage(Utils.ERROR, "Start and end points of the wave are the same", this);
				resetCurrWaveSelection();
				return;
			}

			int indexStart = Math.min(index1, index2);
			int indexEnd = Math.max(index1, index2);
			WaveClassification selectedWaveType = waveType;
			String abbrev;

			if (selectedWaveType == null) {
				selectedWaveType = Utils.promptSelection(WaveClassification.valuesOtherFirst(), "Select Wave Name", -1,
						this);
				if (selectedWaveType == null) {
					// did not select name, cancelled
					return;
				} else if (selectedWaveType == WaveClassification.OTHER) {
					abbrev = Utils.promptTextInput("Abbreviation for name of wave?", this);
					if (abbrev == null) {
						Utils.showMessage(Utils.ERROR, "You must select an abbreviation for the wave.", this);
						return;
					}
				} else {
					abbrev = selectedWaveType.abbrev();
				}
			} else {
				abbrev = selectedWaveType.abbrev();
			}
			if (selectedWaveType != WaveClassification.OTHER) {
				if (currentBounds[0].isProx != selectedWaveType.isProximal()) {
					String region1 = currentBounds[0].isProx ? "proximal" : "distal";
					String region2 = selectedWaveType.isProximal() ? "PROXIMAL" : "DISTAL";
					Utils.showMessage(Utils.ERROR, "<html><center>You tried to add a " + selectedWaveType.label() + " as a " + region1
							+ " wave,<br>" + "but this wave is classically a " + region2 + " wave.</center></html>",
							this);
					resetCurrWaveSelection();
					return;
				}
			}

			Wave wave = new Wave(abbrev, selectedWaveType, time[indexStart], time[indexEnd], indexStart, indexEnd,
					this.currentBounds[0].isProx);
			wiaData.calculateWavePeaksAndSum(wave);

			if (wiaData.containsMatchingWave(wave)) {
				Utils.showMessage(Utils.ERROR, "There is already a wave with the name " + abbrev + ".", this);
				return;
			}
			XYPlot plot = getChart().getXYPlot();
			plot.removeAnnotation(currentBounds[0].boundMarker);
			plot.removeAnnotation(currentBounds[1].boundMarker);

			Range yRange = plot.getRangeAxis().getRange();
			Range xRange = new Range(time[indexStart], time[indexEnd]);
			double yLow = currentBounds[0].isProx ? 0 : yRange.getLowerBound();
			double yHigh = currentBounds[0].isProx ? yRange.getUpperBound() : 0;
			double yForLabel = currentBounds[0].isProx ? getYForTopPos(Utils.getSmallTextFont())
					: getYForBottomPos(Utils.getSmallTextFont());

			XYBoxAnnotation highlightAnnotation = new XYBoxAnnotation(xRange.getLowerBound(), yLow,
					xRange.getUpperBound(), yHigh, null, null, selectedWaveType.getColor());

			XYTextAnnotation textAnnotation = new XYTextAnnotation(abbrev, xRange.getCentralValue(), yForLabel);
			textAnnotation.setFont(Utils.getSmallTextFont());
			textAnnotation.setPaint(Utils.getDarkerColor(selectedWaveType.getColor()));
			textAnnotation.setOutlinePaint(Utils.getDarkerColor(selectedWaveType.getColor()));

			plot.getRenderer().addAnnotation(textAnnotation, Layer.FOREGROUND);
			plot.getRenderer().addAnnotation(highlightAnnotation, Layer.BACKGROUND);
			this.waveAnnotations.put(wave, new XYAnnotation[] { textAnnotation, highlightAnnotation });

			currentBounds[0] = null;
			currentBounds[1] = null;

			this.wiaData.addWave(wave);
			// testing need to commen tout

			if (this.waveListener != null) {
				this.waveListener.updateDisplayForAddedWave(wave);
			}
			return;

		}

		double[] xy = getXYValueFromScreenPos();

		if (xy == null)
			return; // point was not within the chart

		double[] validTime = this.wiaData.getData().getXData();

		if (xy[0] < validTime[0] || xy[0] > validTime[validTime.length - 1]) {
			Utils.showMessage(Utils.ERROR, "Selected point was outside of the acceptable range.", this);
			return;
		}

		boolean isProx = (xy[1] >= 0);

		Range yRange = getChart().getXYPlot().getRangeAxis().getRange();
		XYLineAnnotation line = new XYLineAnnotation(xy[0], 0, xy[0],
				isProx ? yRange.getUpperBound() : yRange.getLowerBound(), markerStroke, new Color(161, 0, 132, 255));

		getChart().getXYPlot().addAnnotation(line);

		WaveBound bound = new WaveBound(xy[0], isProx, line);
		if (currentBounds[0] == null) {
			currentBounds[0] = bound;
		} else {
			currentBounds[1] = bound;
		}

	}

	/**
	 * sends call back to listener
	 */
	public void resetCurrWaveSelection() {

		XYPlot plot = getChart().getXYPlot();
		if (currentBounds[0] != null) {
			plot.removeAnnotation(currentBounds[0].boundMarker);
		}
		if (currentBounds[1] != null) {
			plot.removeAnnotation(currentBounds[1].boundMarker);
		}

		currentBounds[0] = null;
		currentBounds[1] = null;
	}

	/**
	 * This method clears any existing wave selections and annotations, updates the
	 * internal WIAData reference, generates a new chart based on the new data, and
	 * repaints the panel.
	 *
	 * @param newData the new WIAData instance to display.
	 */
	public void resetWIAData(WIAData newData) {

		if (newData == null) {
			throw new IllegalArgumentException("New WIAData file cannot be null.");
		}

		// Clear any current wave selections and annotations.
		resetCurrWaveSelection();
		removeAllWaves();

		// Update the internal data reference.
		this.wiaData = newData;

		// Generate a new chart using the new data and update the ChartPanel.
		JFreeChart newChart = WavePickerChart.generate(false, fontCustom, newData);
		setChart(newChart);

		// Repaint the panel to reflect the new data.
		repaint();
	}

	/**
	 * Removes the specified wave and its associated annotations from the chart.
	 * This does not notify the {@link WavePickListener}.
	 * 
	 * @param wave The {@link Wave} to remove.
	 */
	public void removeWave(Wave wave) {

		if (isPreviewOnly)
			return;

		wiaData.removeWave(wave);
		XYAnnotation[] annotations = this.waveAnnotations.get(wave);
		if (annotations != null) {
			for (XYAnnotation annotation : annotations) {
				this.getChart().getXYPlot().getRenderer().removeAnnotation(annotation);
			}
		}
	}

	/**
	 * Removes all {@link Wave} and their annotations from the chart. This does not
	 * notify the WavePickListener.
	 */
	public void removeAllWaves() {

		if (isPreviewOnly)
			return;

		XYItemRenderer render = getChart().getXYPlot().getRenderer();
		for (Entry<Wave, XYAnnotation[]> waveEn : this.waveAnnotations.entrySet()) {
			wiaData.removeWave(waveEn.getKey());
			for (XYAnnotation annotation : waveEn.getValue()) {
				render.removeAnnotation(annotation);
			}
		}
		this.waveAnnotations.clear();
		getChart().getXYPlot().clearAnnotations();
		wiaData.clearWaves();
	}

	/**
	 * Enables the difference renderer, which fills the area between the wave
	 * intensity line and the zero baseline.
	 */
	public void disableDifferencing() {
		((WavePickerChart) getChart()).disableDifferencing();

	}


	/**
	 * Disables the difference renderer, showing only the wave intensity line.
	 */
	public void enableDifferencing() {
		((WavePickerChart) getChart()).enableDifferencing();

	}

	/**
	 * Calculates the Y-coordinate value for placing text at the top of the plot
	 * area.
	 * 
	 * @param font The font of the text, used to calculate height.
	 * @return The Y-coordinate value.
	 */
	private double getYForTopPos(Font font) {

		Canvas c = new Canvas();
		FontMetrics fm = c.getFontMetrics(font);
		int size = fm.getAscent();

		XYPlot plot = getChart().getXYPlot();
		double buffer = getScreenDataArea().getY();
		Rectangle2D plotArea = getChartRenderingInfo().getPlotInfo().getDataArea();
		return plot.getRangeAxis().java2DToValue(buffer + size, plotArea, plot.getRangeAxisEdge());

	}

	/**
	 * Calculates the Y-coordinate value for placing text at the bottom of the plot
	 * area.
	 * 
	 * @param font The font of the text, used to calculate height.
	 * @return The Y-coordinate value.
	 */
	private double getYForBottomPos(Font font) {
		Canvas c = new Canvas();
		FontMetrics fm = c.getFontMetrics(font);
		int size = fm.getAscent();

		XYPlot plot = getChart().getXYPlot();
		double buffer = getScreenDataArea().getY();
		Rectangle2D plotArea = getChartRenderingInfo().getPlotInfo().getDataArea();
		return plot.getRangeAxis().java2DToValue(buffer + plotArea.getHeight() - size, plotArea,
				plot.getRangeAxisEdge());

	}

	/**
	 * Return the xValue at which the mouse pointer is over vertically on the graph.
	 * 
	 * It will return null if the mouse point was not actually over the graph.
	 * 
	 * This does not actually confirm the xValue actually has any data.
	 */
	public double[] getXYValueFromScreenPos() {

		Point mousePoint = MouseInfo.getPointerInfo().getLocation();

		if (!pointIsOverChart(mousePoint)) {
			return null;
		}

		SwingUtilities.convertPointFromScreen(mousePoint, this); // edits in place without return
		Point2D point2d = translateScreenToJava2D(mousePoint);

		Rectangle2D plotArea = getScreenDataArea();
		XYPlot plot = (XYPlot) getChart().getPlot(); // your plot

		double xVal = plot.getDomainAxis().java2DToValue(point2d.getX(), plotArea, plot.getDomainAxisEdge());
		double yVal = plot.getRangeAxis().java2DToValue(point2d.getY(), plotArea, plot.getRangeAxisEdge());
		return new double[] { xVal, yVal };
	}

	/**
	 * Saves the current chart view as an SVG file.
	 * 
	 * @param file The file to save the SVG to.
	 * @return An error message string if saving fails, otherwise null.
	 */
	public String saveChartAsSVG(File file) {
		try {
			ComboChartSaver.saveAsSVG(getChart(), file, this.getWidth(), this.getHeight());
		} catch (Exception e) {
			e.printStackTrace();
			return "Could not save wave selection display as SVG. System error message: " + e.getMessage();
		}
		return null;
	}

	@Override
	public void keyPressed(int key) {
		
		if (isPreviewOnly) {
			switch (key) {
			case KeyEvent.VK_SPACE:
				attemptDummySelection();
				break;
			case KeyEvent.VK_R:
				resetCurrWaveSelection();
				break;
			}
			
		} else {
			switch (key) {
			case KeyEvent.VK_R:
				resetCurrWaveSelection();
				break;
			case KeyEvent.VK_SPACE:
				attemptSelection(null);
				break;
			case KeyEvent.VK_D:
				deleteWaveSelection();
				break;
			case KeyEvent.VK_1:
				attemptSelection(WaveClassification.FCW);
				break;
			case KeyEvent.VK_2:
				attemptSelection(WaveClassification.FDW);
				break;
			case KeyEvent.VK_3:
				attemptSelection(WaveClassification.LFCW);
				break;
			case KeyEvent.VK_4:
				attemptSelection(WaveClassification.EBCW);
				break;
			case KeyEvent.VK_5:
				attemptSelection(WaveClassification.LBCW);
				break;
			case KeyEvent.VK_6:
				attemptSelection(WaveClassification.BDW);
				break;

			}
		}
		
				
	}
	
	/**
	 * A private helper class to store information about a selected wave boundary.
	 */
	private class WaveBound {
		private final double xVal;
		private final boolean isProx;
		private final XYLineAnnotation boundMarker;

		/**
		 * Constructs a WaveBound object.
		 * 
		 * @param xVal   The x-coordinate (time) of the boundary.
		 * @param isProx True if the boundary is for a proximal (forward) wave.
		 * @param marker The annotation marker displayed on the chart for this bound.
		 */
		private WaveBound(double xVal, boolean isProx, XYLineAnnotation marker) {
			this.xVal = xVal;
			this.isProx = isProx;
			this.boundMarker = marker;
		}
	}

	/**
	 * An interface for listeners that need to respond to wave creation events.
	 */
	public interface WavePickListener {

		/**
		 * Wave chart takes care of storing the wave, this just needs to handle any
		 * display of wave data outside of the chart.
		 * 
		 * @param wave the {@link Wave} added
		 */
		public void updateDisplayForAddedWave(Wave wave);
		
		/**
		 * Wave chart takes care of removing the wave, this just needs to handle any
		 * display of wave data outside of the chart.
		 * 
		 * @param wave the {@link Wave} removed
		 */
		public void updateDisplayForRemovedWave(Wave wave);

	}

	/**
	 * A custom {@link JFreeChart} class for displaying separated wave intensity,
	 * with logic to toggle between filled (difference) and simple line renderers.
	 */
	private static class WavePickerChart extends JFreeChart {

		private static final long serialVersionUID = -1372038490696834353L;
		private static final Color noneColor = new Color(0, 0, 0, 0);
		private static final Color solidGrayColor = new Color(115, 115, 115);

		private XYDifferenceRenderer rendererPositiveDiff = null;
		private XYDifferenceRenderer rendererNegativeDiff = null;
		private XYLineAndShapeRenderer rendererPositive = null;
		private XYLineAndShapeRenderer rendererNegative = null;

		/**
		 * Factory method to generate a {@link WavePickerChart}
		 * 
		 * @param printable If true, configures the chart for printing.
		 * @param font The font for styling.
		 * @param wiaData The data to be plotted.
		 * @return A new {@link WavePickerChart} instance.
		 */
		private static WavePickerChart generate(boolean printable, Font font, WIAData wiaData) {

			return new WavePickerChart(_createPlot(printable, font, wiaData), font);

		}

		/**
		 * Constructor for the chart.
		 * 
		 * @param plot The fully configured {@link XYPlot}.
		 * @param font The font for styling.
		 */
		private WavePickerChart(Plot plot, Font font) {
			super("Separated Wave Intensity", new Font(font.getFamily(), Font.BOLD, (int) (font.getSize() * 1.2)), plot,
					true);
			removeLegend();
			setBorderVisible(false);
			setBorderPaint(new Color(0, 0, 0, 0));
			getXYPlot().setDomainPannable(true);
			getXYPlot().setRangePannable(false);

			this.rendererPositiveDiff = (XYDifferenceRenderer) getXYPlot().getRenderer(0);
			this.rendererNegativeDiff = (XYDifferenceRenderer) getXYPlot().getRenderer(1);

			this.rendererPositive = new XYLineAndShapeRenderer();
			this.rendererNegative = new XYLineAndShapeRenderer();

			rendererPositive.setSeriesPaint(0, solidGrayColor);
			rendererPositive.setSeriesStroke(0, rendererPositiveDiff.getSeriesStroke(0), false);
			rendererPositive.setSeriesPaint(1, noneColor);
			rendererPositive.setSeriesVisible(1, false);
			rendererPositive.setAutoPopulateSeriesShape(false);
			rendererPositive.setSeriesShapesVisible(0, false);
			rendererPositive.setSeriesShapesVisible(1, false);

			rendererNegative.setSeriesPaint(0, solidGrayColor);
			rendererNegative.setSeriesStroke(0, rendererNegativeDiff.getSeriesStroke(0), false);
			rendererNegative.setSeriesPaint(1, noneColor);
			rendererNegative.setSeriesVisible(1, false);
			rendererNegative.setAutoPopulateSeriesShape(false);
			rendererNegative.setSeriesShapesVisible(0, false);
			rendererNegative.setSeriesShapesVisible(1, false);

		}

		/**
		 * Disables the difference renderer, showing only the data lines.
		 */
		private void disableDifferencing() {
			getXYPlot().setRenderer(0, rendererPositive, true);
			getXYPlot().setRenderer(1, rendererNegative, true);

		}

		/**
		 * Enables the difference renderer, filling the area between the lines and the zero axis.
		 */
		private void enableDifferencing() {
			getXYPlot().setRenderer(0, rendererPositiveDiff, true);
			getXYPlot().setRenderer(1, rendererNegativeDiff, true);
		}

		/**
		 * Creates and configures the {@link XYPlot} for the separated wave intensity chart.
		 * 
		 * @param printable If true, configures the plot for printing.
		 * @param textFont The font for text elements.
		 * @param wiaData The data to be plotted.
		 * @return A fully configured XYPlot.
		 */
		private static XYPlot _createPlot(boolean printable, Font textFont, WIAData wiaData) {

			double[] time = wiaData.getTime();
			double[] waveForwardLarge = wiaData.getWIForward();
			double[] waveBackwardLarge = wiaData.getWIBackward();
			double[] accelForward = wiaData.getSepFlowForwardDeriv();
			double[] accelBackward = wiaData.getSepFlowBackwardDeriv();

			XYPlot plot = new XYPlot();

			int textFontSize = textFont.getSize();
			float tickStroke = Math.max(textFontSize / 8.0f, 1.5f);

			Object[] scaled = Utils.scaleToScientific(waveForwardLarge, waveBackwardLarge);
			double[] waveForward = (double[]) scaled[0];
			double[] waveBackward = (double[]) scaled[1];
			int numScientific = (int) scaled[2];

			// set domain
			NumberAxis domainAxis = new AutoRangeLimitedValueAxis("Time (ms)");
			domainAxis.setAutoRange(true);
			domainAxis.setAutoRangeIncludesZero(false);

			domainAxis.setLabelFont(textFont);
			domainAxis.setTickLabelFont(textFont);
			domainAxis.setAutoTickUnitSelection(false);

			domainAxis.setTickUnit(
					new NumberTickUnit(Utils.findOptimalTickInterval(time[0], time[time.length - 1], false)));
			domainAxis.setTickMarkStroke(new BasicStroke(tickStroke));
			domainAxis.setTickMarkInsideLength(textFontSize / 2);
			domainAxis.setTickMarkPaint(Color.BLACK);
			domainAxis.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
			domainAxis.setAxisLineStroke(new BasicStroke(tickStroke));
			domainAxis.setAxisLinePaint(Color.BLACK);

			plot.setDomainAxis(domainAxis);

			if (printable) {
				domainAxis.setTickLabelsVisible(false);
				domainAxis.setTickMarksVisible(false);
				domainAxis.setLabel("");
			}

			XYSeries forwardSeries = new XYSeries("Forward");
			XYSeries forwardFillSeries = new XYSeries("Forward Fill");
			XYSeries backwardSeries = new XYSeries("Backward");
			XYSeries backwardFillSeries = new XYSeries("Backward Fill");

			for (int i = 0; i < waveForward.length; i++) {
				forwardSeries.add(time[i], waveForward[i]);
			}

			for (int i = 0; i < waveBackward.length; i++) {
				backwardSeries.add(time[i], waveBackward[i]);
			}

			for (int i = 0; i < accelForward.length; i++) {
				if (accelForward[i] > 0) {
					if (i > 0 && accelForward[i - 1] <= 0) {
						forwardFillSeries.add(time[i] - 0.0001, waveForward[i]);
					}
					forwardFillSeries.add(time[i], 0);

				} else {
					if (i > 0 && accelForward[i - 1] > 0) {
						forwardFillSeries.add(time[i - 1] + 0.001, waveForward[i - 1]);
					}
					forwardFillSeries.add(time[i], waveForward[i]);

				}
			}

			for (int i = 0; i < accelBackward.length; i++) {
				if (accelBackward[i] > 0) {
					if (i > 0 && accelBackward[i - 1] <= 0) {
						backwardFillSeries.add(time[i] - 0.0001, waveBackward[i]);
					}
					backwardFillSeries.add(time[i], 0);

				} else {
					if (i > 0 && accelBackward[i - 1] > 0) {
						backwardFillSeries.add(time[i - 1] + 0.001, waveBackward[i - 1]);
					}
					backwardFillSeries.add(time[i], waveBackward[i]);

				}
			}

			XYSeriesCollection forwardDataset = new XYSeriesCollection();
			forwardDataset.addSeries(forwardSeries);
			forwardDataset.addSeries(forwardFillSeries);

			XYSeriesCollection backwardDataset = new XYSeriesCollection();
			backwardDataset.addSeries(backwardSeries);
			backwardDataset.addSeries(backwardFillSeries);

			plot.setDataset(0, forwardDataset);
			plot.setDataset(1, backwardDataset);

			XYDifferenceRenderer renderer1 = new XYDifferenceRenderer(solidGrayColor, noneColor, false);
			renderer1.setSeriesPaint(0, solidGrayColor);
			renderer1.setSeriesStroke(0, new BasicStroke(tickStroke), false);

			renderer1.setSeriesPaint(1, noneColor);
			renderer1.setSeriesVisible(1, false);
			XYDifferenceRenderer renderer2 = new XYDifferenceRenderer(noneColor, solidGrayColor, false);
			renderer2.setSeriesPaint(0, solidGrayColor);
			renderer2.setSeriesStroke(0, new BasicStroke(tickStroke), false);

			// renderer2.setSeriesPaint(1, Color.RED);
			renderer2.setSeriesPaint(1, noneColor);
			renderer2.setSeriesVisible(1, false);

			plot.setRenderer(0, renderer1);
			plot.setRenderer(1, renderer2);
			plot.setOutlineVisible(false);

			NumberAxis rangeAxis = new NumberAxis("Wave Intensity");
			rangeAxis.setLabelFont(textFont);
			rangeAxis.setAutoRange(true);
			rangeAxis.setAutoRangeIncludesZero(true);

			String s = "Wave Intensity (W m-2 s-2)";
			if (numScientific > 0) {
				s = s + "   x10" + numScientific;
			}
			AttributedString as = new AttributedString(s);
			as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, 19, 21);
			as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, 23, 25);
			if (numScientific > 0) {
				as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, 32, 33);
				as.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_EXTRABOLD, 29, 33);
				as.addAttribute(TextAttribute.FOREGROUND, Color.GRAY, 29, 33);

			}

			as.addAttribute(TextAttribute.SIZE, textFont.getSize());
			as.addAttribute(TextAttribute.FAMILY, textFont.getFamily());

			rangeAxis.setAttributedLabel(as);
			rangeAxis.setLabel(s);
			rangeAxis.setTickLabelFont(textFont);
			rangeAxis.setAutoTickUnitSelection(false);
			double min = Utils.min(waveForward, waveBackward);
			double max = Utils.max(waveForward, waveBackward);
			rangeAxis.setTickUnit(new NumberTickUnit(Utils.findOptimalTickInterval(min, max, false)));
			rangeAxis.setTickMarkStroke(new BasicStroke(tickStroke));
			rangeAxis.setTickMarkInsideLength(textFontSize / 2);
			rangeAxis.setTickMarkPaint(Color.BLACK);
			rangeAxis.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
			rangeAxis.setAxisLineStroke(new BasicStroke(tickStroke));
			rangeAxis.setAxisLinePaint(Color.BLACK);

			ValueMarker marker = new ValueMarker(0); // basically to display an axis line
			marker.setPaint(Color.black);
			marker.setStroke(new BasicStroke(1.5f));

			plot.setRangeAxis(rangeAxis);
			plot.mapDatasetToRangeAxis(0, 0);
			plot.mapDatasetToRangeAxis(1, 0);
			plot.setDomainGridlinesVisible(false);
			plot.setRangeGridlinesVisible(false);
			plot.addRangeMarker(marker);
			plot.setDomainCrosshairPaint(Color.BLACK);

			return plot;

		}

	}

}
