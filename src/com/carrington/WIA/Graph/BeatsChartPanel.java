package com.carrington.WIA.Graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.carrington.WIA.Utils;
import com.carrington.WIA.Cardio.Beat;
import com.carrington.WIA.Cardio.BeatSelection;
import com.carrington.WIA.Cardio.QRS;
import com.carrington.WIA.Cardio.QRSDetector;
import com.carrington.WIA.DataStructures.HemoData;
import com.carrington.WIA.GUIs.Components.KeyChecker;
import com.carrington.WIA.IO.Header;

/**
 * A specialized ChartPanel for displaying and interacting with hemodynamic data
 * traces (ECG, Pressure, Flow). It provides functionality for selecting,
 * annotating, and grouping individual cardiac beats through keyboard shortcuts
 * and mouse interactions.
 */
public class BeatsChartPanel extends ChartPanel {

	private static final long serialVersionUID = 3309074239826055959L;

	/**
	 * A constant string representing the option to display all available data
	 * traces.
	 */
	public static final String DISPLAY_TRACE_ALL = "All traces (Q)";
	/**
	 * A constant string representing the option to display only the ECG data trace.
	 */
	public static final String DISPLAY_TRACE_ECG = "EKG trace only (W)";
	/**
	 * A constant string representing the option to display only the pressure data
	 * trace.
	 */
	public static final String DISPLAY_TRACE_PRESSURE = "Pressure trace only (E)";
	/**
	 * A constant string representing the option to display only the flow data
	 * trace.
	 */
	public static final String DISPLAY_TRACE_FLOW = "Flow trace only (R)";
	/** An immutable list containing all available trace display options. */
	public static final List<String> DISPLAY_TRACE_OPTIONS =
	        Collections.unmodifiableList(Arrays.asList(
	                DISPLAY_TRACE_ALL,
	                DISPLAY_TRACE_ECG,
	                DISPLAY_TRACE_PRESSURE,
	                DISPLAY_TRACE_FLOW
	        ));

	private static final Color alignColor = new Color(105, 105, 105, 60);
	private static final Color[] standardGraphColors = new Color[] { Color.GREEN.darker(), Color.BLUE.brighter(),
			Color.ORANGE, Color.CYAN };
	private final Color[] standardAnnotColors = { new Color(245, 137, 137, 75), new Color(245, 211, 137, 75),
			new Color(177, 245, 137, 75), new Color(137, 245, 231, 75), new Color(137, 144, 245, 75),
			new Color(234, 137, 245, 75), new Color(130, 130, 130, 75), // gray color last
	};
	private int standardAnnotColorIndex = 0;

	private final HemoData data;

	private Map<BeatSelection, BeatAnnotations[]> beatSelections = new LinkedHashMap<BeatSelection, BeatAnnotations[]>();
	private List<TentativeBeat> currBeats = new ArrayList<TentativeBeat>();

	private Double selStart = null;
	private Double selEnd = null;
	private ValueMarker selStartMarker = null;
	private ValueMarker selEndMarker = null;

	private boolean autoR = false;
	private boolean autoBeat = true;
	private boolean autoDetect = true;
	private int hz = 0;

	private Rectangle2D highlightBox = null;
	private double startX, startY;

	private BeatsChartPanelListener listener = null;
	private WeakReference<BeatsChartPanel> ref = new WeakReference<BeatsChartPanel>(this);

	/**
	 * Constructs a new BeatsChartPanel. * @param data The hemodynamic data to
	 * display. Cannot be null.
	 * 
	 * @param listener   The listener for panel events. Cannot be null.
	 * @param autoSnapR  If true, beat selections will snap to the nearest R-wave.
	 * @param autoBeat   If true, a single click will attempt to select an entire
	 *                   beat automatically.
	 * @param autoDetect If true, R-waves will be detected automatically; otherwise,
	 *                   it will use pre-marked R-waves from the data file.
	 * @param overlap    The percentage of overlap between the pressure and flow
	 *                   traces (0-100).
	 */
	public BeatsChartPanel(HemoData data, BeatsChartPanelListener listener, boolean autoSnapR, boolean autoBeat,
			boolean autoDetect, int overlap) {
		super(createChart(data, false, overlap, alignColor, standardGraphColors));

		if (listener == null)
			throw new IllegalArgumentException("Listener cannot be null");

		if (data == null)
			throw new IllegalArgumentException("Data cannot be null");

		this.data = data;
		this.listener = listener;
		this.autoBeat = autoBeat;
		this.autoDetect = autoDetect;
		this.hz = HemoData.calculateHz(data.getXData());

		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_0, 0, false),
				"beatStart");
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0, false),
				"beatStart");
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_2, 0, false), "beatEnd");
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_3, 0, false), "beatAdd");
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, false),
				"beatReset");
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0, false),
				"windowReset");
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false),
				"deleteBeat");
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, false),
				"lockAlignment");

		getActionMap().put("beatStart", new AbstractAction() {
			private static final long serialVersionUID = -7703504349811078217L;

			public void actionPerformed(ActionEvent e) {
				attemptBoundSelection(1);
			}

		});

		getActionMap().put("beatEnd", new AbstractAction() {

			private static final long serialVersionUID = -621545746817409315L;

			public void actionPerformed(ActionEvent e) {
				attemptBoundSelection(2);
			}

		});

		getActionMap().put("beatAdd", new AbstractAction() {

			private static final long serialVersionUID = 1294198338559989263L;

			public void actionPerformed(ActionEvent e) {
				attemptBoundSelection(3);
			}

		});

		getActionMap().put("beatReset", new AbstractAction() {

			private static final long serialVersionUID = -5577532617835400092L;

			public void actionPerformed(ActionEvent e) {
				clearCurrentSelection();
				listener.setNumBeats(0);
			}

		});
		getActionMap().put("windowReset", new AbstractAction() {
			private static final long serialVersionUID = -4270361060714981927L;

			public void actionPerformed(ActionEvent e) {
				refresh();

			}

		});
		getActionMap().put("deleteBeat", new AbstractAction() {

			private static final long serialVersionUID = -5326260792050664910L;

			public void actionPerformed(ActionEvent e) {
				attemptBeatDeletion();

			}

		});

		setOpaque(false);
		setBackground(null);
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

		MouseListener ml = new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
				ref.get().grabFocus();
			}

			public void mousePressed(MouseEvent e) {
				setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));

			}

			public void mouseReleased(MouseEvent e) {
				setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));

			}

			public void mouseEntered(MouseEvent e) {
				setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			}

			public void mouseExited(MouseEvent e) {
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		};
		addMouseListener(ml);

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				ref.get().requestFocusInWindow();

			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (KeyChecker.isControlPressed()) {
					startX = e.getX();
					startY = e.getY();
					highlightBox = new Rectangle2D.Double(startX, startY, 0, 0);
				}

			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (highlightBox != null) {
					if (KeyChecker.isControlPressed()) {
						Point p1 = new Point((int) highlightBox.getMinX(), (int) highlightBox.getMinY());
						Point p2 = new Point((int) highlightBox.getMaxX(), (int) highlightBox.getMaxY());

						SwingUtilities.convertPointToScreen(p1, ref.get());
						SwingUtilities.convertPointToScreen(p2, ref.get());

						attemptBoundSelectionAuto(p1, p2);
					}

				}

				highlightBox = null;
				repaint();
			}

		});

		addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
				// Change cursor when mouse enters the chart area
				setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				// Reset cursor when mouse is dragged (panning)
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				if (autoBeat) {
					if (KeyChecker.isControlPressed()) {
						updateHighlightBox(e.getX(), e.getY());

					} else {
						highlightBox = null;
					}
					repaint();
				} else {
					highlightBox = null;
				}

			}
		});

		setAlignLines(false);
		refresh();

	}

	/**
	 * Determines which traces to be shown.
	 * 
	 * @param trace one of {@link BeatsChartPanel#DISPLAY_TRACE_ALL},
	 *              {@link BeatsChartPanel#DISPLAY_TRACE_ECG},
	 *              {@link BeatsChartPanel#DISPLAY_TRACE_FLOW},
	 *              {@link BeatsChartPanel#DISPLAY_TRACE_PRESSURE}
	 */
	public void showVisibleTraces(String trace) {

		XYPlot plot = getChart().getXYPlot();

		switch (trace) {
		case DISPLAY_TRACE_ALL:
			plot.getRenderer(0).setSeriesVisible(0, true);
			plot.getRenderer(1).setSeriesVisible(0, true);
			plot.getRenderer(2).setSeriesVisible(0, true);
			break;
		case DISPLAY_TRACE_ECG:
			plot.getRenderer(0).setSeriesVisible(0, true);
			plot.getRenderer(1).setSeriesVisible(0, false);
			plot.getRenderer(2).setSeriesVisible(0, false);
			break;
		case DISPLAY_TRACE_PRESSURE:
			plot.getRenderer(0).setSeriesVisible(0, false);
			plot.getRenderer(1).setSeriesVisible(0, true);
			plot.getRenderer(2).setSeriesVisible(0, false);

			break;
		case DISPLAY_TRACE_FLOW:
			plot.getRenderer(0).setSeriesVisible(0, false);
			plot.getRenderer(1).setSeriesVisible(0, false);
			plot.getRenderer(2).setSeriesVisible(0, true);
			break;
		default:
			throw new IllegalArgumentException("Trace option not specified: " + trace);
		}

	}

	/**
	 * Refreshes the chart's display, updating axis ranges and redrawing
	 * annotations.
	 */
	public void refresh() {
		((CustomXYPlot) getChart().getXYPlot()).update(true, beatSelections, currBeats);
		getChart().fireChartChanged();

	}

	/**
	 * If true, then when a user attempts to select a beat it will automatically try
	 * to select a whole beat in one click
	 * 
	 * @param autoBeat true if should automatically select whole beats
	 */
	public void setAutoBeat(boolean autoBeat) {
		this.autoBeat = autoBeat;
		clearCurrentBounds();
	}

	/**
	 * If true, then when a user attempts to select a beat it will snap to the
	 * nearest R wave
	 * 
	 * @param autoSnapToR true if should automatically select whole beats
	 */
	public void setAutoSnapToR(boolean autoSnapToR) {
		this.autoR = autoSnapToR;
		clearCurrentBounds();
	}

	/**
	 * 
	 * @param autoDetect whether to auto detect R waves. If false, will try to use
	 *                   the R wave designation column in the input file
	 */
	public void setAutoDetectRWave(boolean autoDetect) {
		if (!autoDetect) {
			if (!data.containsHeaderByFlag(HemoData.TYPE_R_WAVE)) {
				autoDetect = true;
			}
		}
		this.autoDetect = autoDetect;
		clearCurrentBounds();
	}

	/**
	 * Clears current bounds, including the start and end of a current beat for both
	 * top and bottom graph, as well as removing the associated domain marker
	 */
	public void clearCurrentBounds() {
		selStart = null;
		selEnd = null;
		if (selStartMarker != null) {
			getChart().getXYPlot().removeDomainMarker(selStartMarker);
			selStartMarker = null;
		}
		if (selEndMarker != null) {
			getChart().getXYPlot().removeDomainMarker(selEndMarker);
			selEndMarker = null;
		}

	}

	/**
	 * Clears all beats currently selected as well as the current bounds for another
	 * beat, if set. Reflects this in the charts.
	 */
	public void clearCurrentSelection() {
		clearCurrentBounds();
		for (TentativeBeat tentativeBeat : currBeats) {
			clearAnnotations(tentativeBeat);
		}

		currBeats.clear();
	}

	/**
	 * Clears the annotations (highlight box and start/end lines) for a given
	 * tentative beat from the plot. * @param tentativeBeat The beat whose
	 * annotations are to be cleared.
	 */
	private void clearAnnotations(TentativeBeat tentativeBeat) {
		XYPlot plot = getChart().getXYPlot();
		plot.getRenderer(1).removeAnnotation(tentativeBeat.annotations.boxHighlight);
		plot.removeDomainMarker(tentativeBeat.annotations.startLine);
		plot.removeDomainMarker(tentativeBeat.annotations.endLine);

	}

	/**
	 * Clears the annotations (highlight box and start/end lines) for a given beat
	 * from the plot. * @param annot The annotations object to be cleared.
	 */
	private void clearAnnotations(BeatAnnotations annot) {
		XYPlot plot = getChart().getXYPlot();
		plot.getRenderer(1).removeAnnotation(annot.boxHighlight);
		plot.removeDomainMarker(annot.startLine);
		plot.removeDomainMarker(annot.endLine);

	}

	/**
	 * Clears all current selections and removes annotations. Only affects graphs
	 * and no other components
	 */
	public void clearAllSelections() {
		for (Entry<BeatSelection, BeatAnnotations[]> en : beatSelections.entrySet()) {
			if (en.getValue() != null) {
				for (BeatAnnotations annot : en.getValue()) {
					clearAnnotations(annot);
				}
			}
		}
		beatSelections.clear();
		clearCurrentBounds();
	}

	/**
	 * Removes a selection of beats, including adjusting annotations as necessary
	 * 
	 * @param beatSel the beat to remove
	 */
	public void removeBeatsSelection(BeatSelection beatSel) {
		BeatAnnotations[] annotations = this.beatSelections.remove(beatSel);
		if (annotations != null) {
			for (BeatAnnotations annot : annotations) {
				clearAnnotations(annot);
			}

		}
	}

	/**
	 * @return all current selections
	 */
	public Set<BeatSelection> getAllSelections() {
		return this.beatSelections.keySet();
	}

	/**
	 * f Whether or not the alignment ligns should be displayed
	 * 
	 * @param enabled if should enable the alignment lines
	 */
	public void setAlignLines(boolean enabled) {
		((CustomXYPlot) getChart().getXYPlot()).paintLines = enabled;
		getChart().setTitle(getChart().getTitle());

	}

	/**
	 * Set the amount of overlap between pressure and flow traces
	 * 
	 * @param overlap (0 = no overlap, 100= full overlap)
	 */
	public void setOverlap(int overlap) {
		if (overlap < 0 || overlap > 100) {
			throw new IllegalArgumentException("Illegal overlap value: " + overlap);
		}
		((CustomXYPlot) getChart().getXYPlot()).setOverlap(overlap);
		((CustomXYPlot) getChart().getXYPlot()).update(false, null, null);

		getChart().setTitle(getChart().getTitle()); // forces a refresh

	}

	/**
	 * @return current number of selections
	 */
	public int getNumberOfSelections() {
		return this.beatSelections.size();
	}

	/**
	 * Pans the domain axis by a relative percentage of its current range.
	 *
	 * @param percent the percentage to pan (positive => right, negative => left)
	 */
	public void panByPercent(double percent) {
		// percent > 0 → pan right, percent < 0 → pan left
		XYPlot plot = getChart().getXYPlot();
		plot.panDomainAxes(percent, getChartRenderingInfo().getPlotInfo(), null);
	}

	/**
	 * Zooms in on both ECG, pressure, and flow plots by reducing the domain range
	 * by 10%.
	 */
	public void zoomIn() {
		getChart().getXYPlot().getDomainAxis().resizeRange(0.9);

	}

	/**
	 * Zooms out on both ECG, pressure, and flow plots by increasing the domain
	 * range by 10%.
	 */
	public void zoomOut() {
		getChart().getXYPlot().getDomainAxis().resizeRange(1.1);

	}

	/**
	 * Attempts to delete a beat annotation under the current mouse cursor, if any.
	 * Updates the listener with the new count of selected beats.
	 */
	public void attemptBeatDeletion() {

		ClickResponse cr = getXValueFromScreenPos();
		if (cr == null)
			return;

		Iterator<TentativeBeat> itr = currBeats.iterator();
		while (itr.hasNext()) {
			TentativeBeat tb = itr.next();
			if (_containsBeat(tb.annotations.boxHighlight, cr.x)) {
				clearAnnotations(tb);
				itr.remove();
			}
		}

		listener.setNumBeats(currBeats.size());

	}

	/**
	 * Returns true if the given annotation’s box spans the specified x–value.
	 *
	 * @param annotation the beat’s box annotation
	 * @param xValue     the domain coordinate to test
	 * @return whether xValue falls between the annotation’s X0 and X1
	 */
	public boolean _containsBeat(XYBoxAnnotation annotation, double xValue) {
		return xValue >= annotation.getX0() && xValue <= annotation.getX1();
	}

	/**
	 * @return the current visible Y–axis range for the pressure plot (the plot used
	 *         for all annotations)
	 */
	private Range getCurrYRange() {
		return getChart().getXYPlot().getRangeAxis(1).getRange();
	}

	/**
	 * Attempts to select bounds for another beat
	 * 
	 * @param keyNum Number of the key that was pressed
	 */
	private void attemptBoundSelection(int keyNum) {

		if (autoBeat) {

			attemptBoundSelectionAuto();
			return;
		}

		ClickResponse cr = getXValueFromScreenPos();

		if (keyNum == 3) {
			// Try to add a beat

			// Validate that things are set
			if (selStart == null || selEnd == null) {
				Utils.showMessage(Utils.ERROR, "Both bounds of the beat must be set first.", this);
				return;
			}
			Range xRange = new Range(selStart, selEnd);
			Range yRange = getCurrYRange();

			Color annotationColor = standardAnnotColors[standardAnnotColorIndex];
			XYBoxAnnotation highlightAnnotation = new XYBoxAnnotation(xRange.getLowerBound(), yRange.getLowerBound(),
					xRange.getUpperBound(), yRange.getUpperBound(), null, null, annotationColor);

			ValueMarker vmStart = new ValueMarker(xRange.getLowerBound());
			ValueMarker vmEnd = new ValueMarker(xRange.getUpperBound());
			vmStart.setStroke(new BasicStroke(2f));
			vmStart.setPaint(annotationColor.darker().darker());
			vmEnd.setStroke(new BasicStroke(2f));
			vmEnd.setPaint(annotationColor.darker().darker());

			getChart().getXYPlot().getRenderer(1).addAnnotation(highlightAnnotation);
			getChart().getXYPlot().addDomainMarker(vmStart);
			getChart().getXYPlot().addDomainMarker(vmEnd);

			String svgString = null;
			try {
				svgString = ComboChartSaver.getSaveAsSVGString(getChart(), getWidth(), getHeight());
			} catch (Exception e) {
				e.printStackTrace();
				svgString = null;
			}

			currBeats.add(_createBeat(xRange.getLowerBound(), xRange.getUpperBound(), vmStart, vmEnd,
					highlightAnnotation, svgString));

			clearCurrentBounds();
			listener.setNumBeats(currBeats.size());
			return;
		}

		double xValue = cr.x;
		if (autoR) {
			QRS qrs = getClosestQRS(cr);
			if (qrs == null) {
				Utils.showMessage(Utils.ERROR, "Could not identify R Waves in the currently displayed domain", this);
				return;
			}
			xValue = qrs.getArrayValue();
		}

		if (keyNum == 0 || keyNum == 1) {
			// Set the first bound
			if (selEnd != null && xValue >= selEnd) {
				Utils.showMessage(Utils.ERROR, "Beat start point must be before the end point.", this);
				return;
			}
			if (selStartMarker != null) {
				getChart().getXYPlot().removeDomainMarker(selStartMarker);
				selStartMarker = null;
			}

			ValueMarker vm = new ValueMarker(xValue);
			vm.setStroke(new BasicStroke(2f));
			vm.setPaint(Utils.colorPurpleDarker);
			getChart().getXYPlot().addDomainMarker(vm);
			selStartMarker = vm;
			selStart = xValue;

		} else if (keyNum == 2) {

			// Set the second
			if (selStart != null && xValue <= selStart) {
				Utils.showMessage(Utils.ERROR, "Beat end point must be after the start point.", this);
				return;
			}
			if (selEndMarker != null) {
				getChart().getXYPlot().removeDomainMarker(selEndMarker);
				selEndMarker = null;
			}

			ValueMarker vm = new ValueMarker(xValue);
			vm.setStroke(new BasicStroke(2f));
			vm.setPaint(Utils.colorPurpleDarker);
			getChart().getXYPlot().addDomainMarker(vm);
			selEndMarker = vm;
			selEnd = xValue;

		}

	}

	/**
	 * Attempts to automatically select a range of beats based on a highlighted
	 * region
	 * 
	 * @param highlightBox the selected region
	 */
	private void attemptBoundSelectionAuto(Point p1, Point p2) {

		ClickResponse[] cr = getXValueFromScreenPos(p1, p2);
		if (cr == null)
			return;

		QRS[] complexes = getClosestQRSComplexes(cr[0].x, cr[1].x);
		if (complexes == null)
			return;
		String svgString = null;
		try {
			svgString = ComboChartSaver.getSaveAsSVGString(getChart(), getWidth(), getHeight());
		} catch (Exception e) {
			e.printStackTrace();
			svgString = null;
		}

		Range yRange = getCurrYRange();
		for (int i = 0; i < complexes.length - 1; i++) {
			QRS complexStart = complexes[i];
			QRS complexEnd = complexes[i + 1];

			Range xRange = new Range(complexStart.getArrayValue(), complexEnd.getArrayValue());
			Color annotationColor = standardAnnotColors[standardAnnotColorIndex];
			XYBoxAnnotation highlightAnnotation = new XYBoxAnnotation(xRange.getLowerBound(), yRange.getLowerBound(),
					xRange.getUpperBound(), yRange.getUpperBound(), null, null, annotationColor);

			ValueMarker vmStart = new ValueMarker(xRange.getLowerBound());
			ValueMarker vmEnd = new ValueMarker(xRange.getUpperBound());
			vmStart.setStroke(new BasicStroke(2f));
			vmStart.setPaint(annotationColor.darker());
			vmEnd.setStroke(new BasicStroke(2f));
			vmEnd.setPaint(annotationColor.darker());

			getChart().getXYPlot().getRenderer(1).addAnnotation(highlightAnnotation);
			getChart().getXYPlot().addDomainMarker(vmStart);
			getChart().getXYPlot().addDomainMarker(vmEnd);

			currBeats.add(_createBeat(complexStart.getArrayValue(), complexEnd.getArrayValue(), vmStart, vmEnd,
					highlightAnnotation, svgString));
			listener.setNumBeats(currBeats.size());
		}

		clearCurrentBounds();
	}

	/**
	 * attempts to make a selection automatically by findings R Waves
	 */
	private void attemptBoundSelectionAuto() {
		ClickResponse cr = getXValueFromScreenPos();
		if (cr == null) {
			return;
		}

		QRS[] closest = getClosestQRSAboveBelow(cr);
		if (closest == null) {
			Utils.showMessage(Utils.ERROR, "Could not identify R Waves in the currently displayed domain", this);
			return;
		}

		Range xRange = new Range(closest[0].getArrayValue(), closest[1].getArrayValue());
		Range yRange = getCurrYRange();

		Color annotationColor = standardAnnotColors[standardAnnotColorIndex];
		XYBoxAnnotation highlightAnnotation = new XYBoxAnnotation(xRange.getLowerBound(), yRange.getLowerBound(),
				xRange.getUpperBound(), yRange.getUpperBound(), null, null, annotationColor);

		ValueMarker vmStart = new ValueMarker(xRange.getLowerBound());
		ValueMarker vmEnd = new ValueMarker(xRange.getUpperBound());
		vmStart.setStroke(new BasicStroke(2f));
		vmStart.setPaint(annotationColor.darker());
		vmEnd.setStroke(new BasicStroke(2f));
		vmEnd.setPaint(annotationColor.darker());

		getChart().getXYPlot().getRenderer(1).addAnnotation(highlightAnnotation);
		getChart().getXYPlot().addDomainMarker(vmStart);
		getChart().getXYPlot().addDomainMarker(vmEnd);

		String svgString = null;
		try {
			svgString = ComboChartSaver.getSaveAsSVGString(getChart(), getWidth(), getHeight());
		} catch (Exception e) {
			e.printStackTrace();
			svgString = null;
		}

		currBeats.add(_createBeat(closest[0].getArrayValue(), closest[1].getArrayValue(), vmStart, vmEnd,
				highlightAnnotation, svgString));
		listener.setNumBeats(currBeats.size());
		clearCurrentBounds();
	}

	/**
	 * Attempts to create a {@link BeatSelection} from the currently selected
	 * {@link Beat}s. If there are not enough beats selected, will display an error
	 * in the GUI and return null.
	 * 
	 * @return {@link BeatSelection} representing the beats of interest
	 */
	public BeatSelection attemptBeatGrouping() {
		clearCurrentBounds();

		// Validate
		if (currBeats.isEmpty()) {
			Utils.showMessage(Utils.ERROR, "At least one beat needs to be selected", this);
			return null;
		}

		// pick name
		String name = Utils.promptTextInput("Selection name?", this);

		if (name == null || name.length() == 0)
			return null; // user cancelled

		name = name.trim();

		// see if one exists with similar name
		for (BeatSelection bs : beatSelections.keySet()) {
			if (name.equalsIgnoreCase(bs.getName().trim())) {
				Utils.showMessage(Utils.ERROR, "There is already a selection with this name!", null);
				return null;
			}
		}

		// Create new beat selection, combined all current XYAnnotations for each
		// individual beat
		BeatSelection newBeatSelection = new BeatSelection(name);

		ArrayList<BeatAnnotations> annotations = new ArrayList<BeatAnnotations>();
		for (TentativeBeat tentBeat : currBeats) {
			if (tentBeat.svg != null) {
				newBeatSelection.addBeat(tentBeat.beat, tentBeat.svg, "Combo");
			} else {
				newBeatSelection.addBeat(tentBeat.beat, "Combo");
			}
			annotations.add(tentBeat.annotations);
		}

		beatSelections.put(newBeatSelection, annotations.toArray(new BeatAnnotations[0]));
		currBeats.clear();

		if (standardAnnotColorIndex < standardAnnotColors.length - 1) {
			standardAnnotColorIndex++;
		}

		return newBeatSelection;

	}

	/**
	 * Gets the closest QRS complex given the clicked spot. Will return null if none
	 * were found.
	 */
	private QRS getClosestQRS(ClickResponse cr) {

		QRS[] closestQRS = null;
		if (!autoDetect && data.containsHeaderByFlag(HemoData.TYPE_R_WAVE)) {
			// R waves already identified in the file
			closestQRS = _getClosestRecordedQRS(cr.x);
		} else {
			// Need to find R waves ourselves
			closestQRS = _getClosestDetectedQRS(cr.x);
		}

		if (closestQRS[0] == null && closestQRS[1] == null) {
			return null;
		}

		double diffBelow = closestQRS[0] != null ? Math.abs(cr.x - closestQRS[0].getArrayValue()) : Double.MAX_VALUE;
		double diffAbove = closestQRS[1] != null ? Math.abs(cr.x - closestQRS[1].getArrayValue()) : Double.MAX_VALUE;

		if (diffBelow < diffAbove) {
			return closestQRS[0];
		} else {
			return closestQRS[1];
		}

	}

	/**
	 * Gets the QRS complexes above and below clicked spot. Will return null if both
	 * weren't found.
	 */
	private QRS[] getClosestQRSAboveBelow(ClickResponse cr) {

		QRS[] closestQRSAboveBelow = null;
		if (!autoDetect && data.containsHeaderByFlag(HemoData.TYPE_R_WAVE)) {
			// R waves already identified in the file
			closestQRSAboveBelow = _getClosestRecordedQRS(cr.x);
		} else {
			// Need to find R waves ourselves
			closestQRSAboveBelow = _getClosestDetectedQRS(cr.x);
		}

		if (closestQRSAboveBelow[0] == null || closestQRSAboveBelow[1] == null) {
			return null;
		} else {
			return closestQRSAboveBelow;
		}

	}

	/**
	 * Gets the QRS complexes within the section, as well as one below and above
	 */
	private QRS[] getClosestQRSComplexes(double x1, double x2) {

		QRS[] closestQRSAboveBelow = null;
		if (!autoDetect && data.containsHeaderByFlag(HemoData.TYPE_R_WAVE)) {
			// R waves already identified in the file
			closestQRSAboveBelow = _getRecordedQRSComplexes(x1, x2); //
		} else {
			// Need to find R waves ourselves
			closestQRSAboveBelow = _getDetectedQRSComplexes(x1, x2);
		}
		return closestQRSAboveBelow;

	}

	/**
	 * Detects the two nearest R-wave complexes around a given time by running the
	 * {@link QRSDetector} on the currently visible ECG subset.
	 *
	 * @param tQuery the domain time around which to search for R waves
	 * @return an array of two QRS objects: [closestBelow, closestAbove], or null if
	 *         fewer than two complexes are found
	 */
	private QRS[] _getClosestDetectedQRS(double tQuery) {

		Range range = getChart().getXYPlot().getDomainAxis().getRange();
		int startIndex = getClosestTimeIndex(range.getLowerBound());
		int endIndex = getClosestTimeIndex(range.getUpperBound());
		if ((endIndex - startIndex) < 3) {
			return null;
		}
		Header ecgHeader = data.getHeaderByFlag(HemoData.TYPE_ECG).get(0);
		List<QRS> qrsComplexes = QRSDetector.getQRSOnSubset(data.getXData(), data.getYData(ecgHeader), startIndex,
				endIndex, hz, true);
		if (qrsComplexes.size() < 2) {
			return null;
		}

		QRS closestQRSBelow = null;
		QRS closestQRSAbove = null;

		for (QRS qrs : qrsComplexes) {
			if (qrs.getArrayValue() < tQuery) {
				if (closestQRSBelow == null) {
					closestQRSBelow = qrs;
				} else if (Math.abs(tQuery - qrs.getArrayValue()) < Math
						.abs(tQuery - closestQRSBelow.getArrayValue())) {
					closestQRSBelow = qrs;
				}
			} else {
				if (closestQRSAbove == null) {
					closestQRSAbove = qrs;
				} else if (Math.abs(tQuery - qrs.getArrayValue()) < Math
						.abs(tQuery - closestQRSAbove.getArrayValue())) {
					closestQRSAbove = qrs;
				}
			}

		}
		return new QRS[] { closestQRSBelow, closestQRSAbove };
	}

	/**
	 * Finds the two nearest recorded R-wave markers in the data around a given
	 * time.
	 *
	 * @param tQuery the domain time around which to search for R waves
	 * @return an array of two QRS objects from the R-wave header: [closestBelow,
	 *         closestAbove], or null if no R-wave header exists
	 */
	private QRS[] _getClosestRecordedQRS(double tQuery) {

		if (data.getHeaderByFlag(HemoData.TYPE_R_WAVE).isEmpty())
			return null; // no R wave data to

		double[] time = data.getXData();
		double[] rWave = data.getYData(data.getHeaderByFlag(HemoData.TYPE_R_WAVE).get(0));

		int indexBelow = -1;
		int indexAbove = -1;

		for (int i = 0; i < rWave.length; i++) {

			if (rWave[i] > 0) {

				if (time[i] < tQuery) {
					if (indexBelow == -1 || i > indexBelow) {
						indexBelow = i;
					}
				} else {
					if (indexAbove == -1 || i < indexAbove) {
						indexAbove = i;
					}
				}
			}
		}

		QRS qrsBelow = indexBelow != -1 ? new QRS(indexBelow, time[indexBelow]) : null;
		QRS qrsAbove = indexAbove != -1 ? new QRS(indexAbove, time[indexAbove]) : null;

		return new QRS[] { qrsBelow, qrsAbove };

	}

	/**
	 * Detects all QRS complexes between x1 and x2 (inclusive), plus one immediately
	 * before and one immediately after, by running the {@link QRSDetector} on the
	 * visible ECG subset.
	 *
	 * @param x1 the start of the time window
	 * @param x2 the end of the time window
	 * @return a QRS array spanning the complexes, or null if insufficient data
	 */
	private QRS[] _getDetectedQRSComplexes(double x1, double x2) {

		Range range = getChart().getXYPlot().getDomainAxis().getRange();
		int startIndex = getClosestTimeIndex(range.getLowerBound());
		int endIndex = getClosestTimeIndex(range.getUpperBound());
		if ((endIndex - startIndex) < 3) {
			return null;
		}
		Header alignHeader = data.getHeaderByFlag(HemoData.TYPE_ECG).get(0);
		if (alignHeader == null) {
			return null;
		}
		List<QRS> qrsComplexes = QRSDetector.getQRSOnSubset(data.getXData(), data.getYData(alignHeader), startIndex,
				endIndex, hz, true);
		if (qrsComplexes.size() < 2) {
			return null;
		}

		double distanceAbove = Double.MAX_VALUE;
		double distanceBelow = Double.MAX_VALUE;
		int indexClosestBelow = -1;
		int indexClosestAbove = -1;
		int counter = 0;
		for (QRS qrs : qrsComplexes) {
			double queryDistance = qrs.getArrayValue() - x2;
			if (queryDistance >= 0 && queryDistance < distanceAbove) {
				indexClosestAbove = counter;
				distanceAbove = queryDistance;
			}
			counter++;
		}
		counter = 0;
		for (QRS qrs : qrsComplexes) {
			double queryDistance = x1 - qrs.getArrayValue();
			if (queryDistance >= 0 && queryDistance < distanceBelow) {
				indexClosestBelow = counter;
				distanceBelow = queryDistance;
			}
			counter++;
		}
		if (indexClosestBelow == -1 || indexClosestAbove == -1 || (indexClosestAbove - indexClosestBelow) < 2) {
			return null;
		}

		return qrsComplexes.subList(indexClosestBelow, indexClosestAbove + 1).toArray(new QRS[0]);
	}

	/**
	 * Returns all recorded R-wave complexes between x1 and x2 (inclusive) from the
	 * R-wave header, without detection.
	 *
	 * @param x1 the start of the time window
	 * @param x2 the end of the time window
	 * @return a QRS array of recorded complexes, or null if no R-wave header
	 */
	private QRS[] _getRecordedQRSComplexes(double x1, double x2) {

		if (data.getHeaderByFlag(HemoData.TYPE_R_WAVE).isEmpty())
			return null; // no R wave data to

		double[] time = data.getXData();
		double[] rWave = data.getYData(data.getHeaderByFlag(HemoData.TYPE_R_WAVE).get(0));

		ArrayList<QRS> complexes = new ArrayList<QRS>();

		for (int i = 0; i < rWave.length; i++) {

			if (time[i] >= x1) {
				if (time[i] > x2) {
					break;
				}
				if (rWave[i] > 0) {
					complexes.add(new QRS(i, time[i]));

				}

			}

		}

		return complexes.toArray(new QRS[0]);

	}

	/**
	 * Creates a new beat with the specific start and end points. Does not validate
	 * these inputs
	 */
	private TentativeBeat _createBeat(double start, double end, ValueMarker annotStart, ValueMarker annotEnd,
			XYBoxAnnotation annotHighlight, String chartImgSVGString) {
		int startIndex = getClosestTimeIndex(start);
		int endIndex = getClosestTimeIndex(end);
		Beat beat = new Beat(data, startIndex, endIndex);
		return new TentativeBeat(getChart().getXYPlot(), beat, annotStart, annotEnd, annotHighlight, chartImgSVGString);
	}

	/**
	 * Gets the index of the time array closest to the specified time
	 * 
	 * @param timeSelected
	 * @return index
	 */
	private int getClosestTimeIndex(double timeSelected) {

		int bestIndex = 0;

		double[] time = data.getXData();
		double bestDistance = timeSelected - time[0];

		for (int i = 1; i < time.length; i++) {
			double qDistance = Math.abs(timeSelected - time[i]);
			if (qDistance < bestDistance) {
				bestIndex = i;
				bestDistance = qDistance;
			}
		}
		return bestIndex;

	}

	/**
	 * Used to determine if mouse pointer is over the chart
	 * 
	 * @return 0 for no, 1 for top chart, 2 for bottom chart
	 */
	private boolean pointIsOverCharts(Point p) {
		Rectangle2D chartrect = getScreenDataArea();

		Point parentUp1 = getLocationOnScreen();

		Rectangle newRect1 = new Rectangle(parentUp1.x + (int) chartrect.getX(), parentUp1.y + (int) chartrect.getY(),
				(int) chartrect.getWidth(), (int) chartrect.getHeight());

		return newRect1.contains(p);

	}

	/**
	 * Translates the current mouse pointer into a domain‐axis value, stored in a
	 * {@link ClickResponse}, if over the chart.
	 *
	 * @return a ClickResponse containing the domain x–value, or null if outside the
	 *         plot
	 */
	private ClickResponse getXValueFromScreenPos() {

		Point mousePoint = MouseInfo.getPointerInfo().getLocation();

		if (!pointIsOverCharts(mousePoint)) {
			return null;
		}

		SwingUtilities.convertPointFromScreen(mousePoint, this); // edits in place without return

		Point2D point2dPnl = translateScreenToJava2D(mousePoint);

		Rectangle2D plotArea = getScreenDataArea();
		XYPlot plot = (XYPlot) getChart().getPlot();

		ClickResponse cr = new ClickResponse(
				plot.getDomainAxis().java2DToValue(point2dPnl.getX(), plotArea, plot.getDomainAxisEdge()));

		if (data.containsXData(cr.x)) {
			return cr;
		} else {
			return null;
		}

	}

	/**
	 * Translates two screen points into domain‐axis values, stored in a
	 * {@link ClickResponse}, if both are over the plot.
	 *
	 * @param mousePoint1 first screen location
	 * @param mousePoint2 second screen location
	 * @return an array [cr1, cr2] of ClickResponses, or null if either is outside
	 *         the plot
	 */
	private ClickResponse[] getXValueFromScreenPos(Point mousePoint1, Point mousePoint2) {

		if (!pointIsOverCharts(mousePoint1) || !pointIsOverCharts(mousePoint2)) {
			return null;
		}

		SwingUtilities.convertPointFromScreen(mousePoint1, this); // edits in place without return
		SwingUtilities.convertPointFromScreen(mousePoint2, this); // edits in place without return

		Point2D point2dPnl1 = translateScreenToJava2D(mousePoint1);
		Point2D point2dPnl2 = translateScreenToJava2D(mousePoint2);

		Rectangle2D plotArea = getScreenDataArea();

		XYPlot plot = (XYPlot) getChart().getPlot();

		ClickResponse cr1 = new ClickResponse(
				plot.getDomainAxis().java2DToValue(point2dPnl1.getX(), plotArea, plot.getDomainAxisEdge()));
		ClickResponse cr2 = new ClickResponse(
				plot.getDomainAxis().java2DToValue(point2dPnl2.getX(), plotArea, plot.getDomainAxisEdge()));

		if (data.containsXData(cr1.x) && data.containsXData(cr2.x)) {
			return new ClickResponse[] { cr1, cr2 };
		} else {
			return null;
		}

	}

	/**
	 * Updates the temporary highlight rectangle during a drag operation.
	 *
	 * @param x the current mouse x–coordinate in panel space
	 * @param y the current mouse y–coordinate in panel space
	 */
	private void updateHighlightBox(int x, int y) {
		if (highlightBox != null) {
			double x0 = Math.min(startX, x);
			double y0 = Math.min(startY, y);
			double width = Math.abs(startX - x);
			double height = Math.abs(startY - y);
			highlightBox.setRect(x0, y0, width, height);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Additionally, this method draws the highlight selection box if one is being
	 * created.
	 */
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (highlightBox != null) {
			Graphics2D g2 = (Graphics2D) g.create();
			Color color = standardAnnotColors[standardAnnotColorIndex];
			g2.setColor(color);
			g2.fill(highlightBox);
			g2.setStroke(new BasicStroke(1.0f));
			g2.setColor(color.darker().darker());
			g2.draw(highlightBox);
			g2.dispose();
		}
	}

	/////////////////////////////////////////////////////
	// Chart Set up
	/////////////////////////////////////////////////////

	/**
	 * Builds the overall JFreeChart containing ECG, pressure and flow sub-plots.
	 *
	 * @param hd         the hemodynamic data source
	 * @param paintLines whether to draw the vertical alignment lines
	 * @param overlap    the desired percent overlap between pressure & flow plots
	 *                   (0–100)
	 * @param alignColor the color to use for alignment lines
	 * @param colors     an array of series colors for the traces
	 * @return a fully configured JFreeChart instance
	 */
	private static JFreeChart createChart(HemoData hd, boolean paintLines, int overlap, Color aligncolor,
			Color[] colors) {
		JFreeChart chart = new JFreeChart(Utils.getShortenedFilePath(hd.getFile().getParent(), 80),
				Utils.getSubTitleSubFont(), createPlot(hd, paintLines, overlap, alignColor, colors), true);
		chart.getTitle().setPaint(Color.GRAY);
		chart.setBackgroundImageAlpha(0);
		chart.setBackgroundPaint(new Color(0, 0, 0, 0));
		chart.setBorderVisible(false);
		chart.getXYPlot().setBackgroundAlpha(0);
		chart.setBorderPaint(new Color(0, 0, 0, 0));
		chart.getXYPlot().setDomainPannable(true);
		chart.getXYPlot().setRangePannable(false);

		return chart;
	}

	/**
	 * Creates and configures the XYPlot: axes, datasets and renderers for ECG,
	 * pressure, and flow.
	 *
	 * @param hd         the hemodynamic data source
	 * @param paintLines whether to draw the vertical alignment lines
	 * @param overlap    percent overlap (0–100) for pressure/flow ranges
	 * @param alignColor the color for alignment guides
	 * @param colors     series colors for pressure and flow
	 * @return the configured XYPlot
	 */
	private static XYPlot createPlot(HemoData hd, boolean paintLines, int overlap, Color alignColor, Color[] colors) {
		XYPlot plot = new CustomXYPlot(paintLines, overlap);
		Font font = Utils.getTextFont(false);
		int textFontSize = font.getSize();
		float tickStroke = Math.max(textFontSize / 8.0f, 1.5f);
		BasicStroke strokeThickSolid = new BasicStroke(tickStroke);

		// DOMAIN AXIS
		NumberAxis domainAxis = new PanModifiedAxis(hd.getXHeader().getName());
		domainAxis.setAutoRange(true);
		domainAxis.setAutoRangeIncludesZero(false);

		domainAxis.setLabelFont(font);
		domainAxis.setTickLabelFont(font);
		domainAxis.setAutoTickUnitSelection(true);
		domainAxis.setTickMarkStroke(strokeThickSolid);
		domainAxis.setTickMarkInsideLength(textFontSize / 2);
		domainAxis.setTickMarkPaint(Color.BLACK);
		domainAxis.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
		domainAxis.setAxisLineStroke(strokeThickSolid);
		domainAxis.setAxisLinePaint(Color.BLACK);
		plot.setDomainAxis(domainAxis);

		// add data sets (ECG, pressure, flow)

		addDataSet(plot, hd, hd.getHeaderByFlag(HemoData.TYPE_ECG).get(0), "ECG", alignColor, strokeThickSolid,
				tickStroke, font, 0, null);
		addDataSet(plot, hd, hd.getHeaderByFlag(HemoData.TYPE_PRESSURE).get(0), "Pressure", colors[0], strokeThickSolid,
				tickStroke, font, 1, true);
		addDataSet(plot, hd, hd.getHeaderByFlag(HemoData.TYPE_FLOW).get(0), "Flow", colors[1], strokeThickSolid,
				tickStroke, font, 2, false);

		// finalize display settings for the plot
		plot.setOutlineVisible(false);
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(false);
		return plot;

	}

	/**
	 * Adds one data series to the given plot, with its own renderer and range axis.
	 *
	 * @param plot           the plot to which to add the dataset
	 * @param hd             the hemodynamic data source
	 * @param header         identifies which channel (ECG/pressure/flow) to plot
	 * @param name           the series name (also used as axis label)
	 * @param color          paint for the line renderer
	 * @param stroke         stroke for the line renderer
	 * @param tickStroke     stroke width for tick marks
	 * @param font           font for axis labels and tick labels
	 * @param dataSetCounter index at which to insert the dataset
	 * @param positionLeft   if true, axis on left; false, axis on right; null → no
	 *                       axis drawn
	 */
	private static void addDataSet(XYPlot plot, HemoData hd, Header header, String name, Color color, Stroke stroke,
			float tickStroke, Font font, int dataSetCounter, Boolean positionLeft) {

		double[] time = hd.getXData();
		double[] yValues = hd.getYData(header);
		XYSeries ySeries = new XYSeries(name);

		for (int i = 0; i < time.length; i++) {
			ySeries.add(time[i], yValues[i]);
			ySeries.add(time[i], yValues[i]);
		}
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(ySeries);
		plot.setDataset(dataSetCounter, dataset);

		SamplingXYLineRenderer renderer = new SamplingXYLineRenderer();

		renderer.setSeriesPaint(0, color);

		renderer.setSeriesStroke(0, stroke, false);
		renderer.setAutoPopulateSeriesShape(true);
		renderer.setLegendTextPaint(dataSetCounter, Color.RED);
		renderer.setSeriesVisibleInLegend(0, false, true);

		// renderer.setSeriesShape(0, null); // may need to delete
		plot.setRenderer(dataSetCounter, renderer);

		NumberAxis rangeAxis = new NumberAxis(name);
		rangeAxis.setLabelFont(font);
		rangeAxis.setLabelPaint(renderer.lookupSeriesPaint(0));
		rangeAxis.setAutoRange(true);

		if (positionLeft == null) {
			rangeAxis.setVisible(false);
			rangeAxis.setAxisLineVisible(false);
			plot.setRangeAxisLocation(dataSetCounter, AxisLocation.TOP_OR_RIGHT);

		} else {
			rangeAxis.setVisible(true);
			rangeAxis.setTickLabelsVisible(true);
			rangeAxis.setTickMarksVisible(true);
			plot.setRangeAxisLocation(dataSetCounter,
					positionLeft ? AxisLocation.TOP_OR_LEFT : AxisLocation.TOP_OR_RIGHT);
			rangeAxis.setLabel(name);
			rangeAxis.setAutoRangeIncludesZero(false);
			rangeAxis.setAutoTickUnitSelection(true);
			rangeAxis.setTickMarkStroke(stroke);
			rangeAxis.setTickMarkInsideLength(font.getSize() / 2);
			rangeAxis.setTickLabelFont(font);
			rangeAxis.setTickMarkPaint(Color.BLACK);
			rangeAxis.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
			rangeAxis.setAxisLineStroke(stroke);
			rangeAxis.setAxisLinePaint(Color.BLACK);
		}

		plot.setRangeAxis(dataSetCounter, rangeAxis);
		plot.mapDatasetToRangeAxis(dataSetCounter, dataSetCounter);

	}

	/**
	 * A simple container for the domain-axis value corresponding to a mouse click.
	 */
	private static class ClickResponse {
		private final double x;

		/**
		 * Constructs a ClickResponse. * @param x The domain (x-axis) value of the
		 * click.
		 */
		private ClickResponse(double x) {
			this.x = x;
		}
	}

	/**
	 * Wraps the visual annotations for a single beat selection: the start/end
	 * markers and the rectangular highlight.
	 */
	private static class BeatAnnotations {

		private final ValueMarker startLine;
		private final ValueMarker endLine;
		private XYBoxAnnotation boxHighlight;
		private XYPlot plot;

		/**
		 * Constructs a BeatAnnotations bundle.
		 *
		 * @param plot             the plot to render on
		 * @param annotMarkerStart the marker for the beat start
		 * @param annotMarkerEnd   the marker for the beat end
		 * @param annotHighlight   the box annotation for the beat region
		 */
		private BeatAnnotations(XYPlot plot, ValueMarker annotMarkerStart, ValueMarker annotMarkerEnd,
				XYBoxAnnotation annotHighlight) {
			this.startLine = annotMarkerStart;
			this.endLine = annotMarkerEnd;
			this.boxHighlight = annotHighlight;
			this.plot = plot;
		}

		/**
		 * Updates the highlight box to span a new vertical (Y-axis) range while
		 * preserving its existing X-coordinates (i.e. when the chart is fit /
		 * refreshed)
		 *
		 * @param yRange the new Y-axis range to apply
		 */
		private void modifyXYBoxAnnotation(Range yRange) {
			plot.getRenderer(1).removeAnnotation(boxHighlight);
			boxHighlight = new XYBoxAnnotation(boxHighlight.getX0(), yRange.getLowerBound(), boxHighlight.getX1(),
					yRange.getUpperBound(), null, null, boxHighlight.getFillPaint());

			plot.getRenderer(1).addAnnotation(boxHighlight);

		}

	}

	/**
	 * Represents a beat that has been selected by the user but not yet finalized
	 * into a {@link BeatSelection}. It holds the core {@link Beat} data, its visual
	 * annotations, and an SVG snapshot of its appearance at selection time.
	 */
	private static class TentativeBeat {

		private final String svg;
		private final Beat beat;
		private final BeatAnnotations annotations;

		/**
		 * Constructs a new TentativeBeat.
		 *
		 * @param plot             The XYPlot on which the beat is drawn.
		 * @param beat             The underlying beat data.
		 * @param annotMarkerStart The value marker for the beat's start.
		 * @param annotMarkerEnd   The value marker for the beat's end.
		 * @param annotHighlight   The box annotation highlighting the beat's area.
		 * @param svg              An SVG string representing the chart view when the
		 *                         beat was selected.
		 */
		private TentativeBeat(XYPlot plot, Beat beat, ValueMarker annotMarkerStart, ValueMarker annotMarkerEnd,
				XYBoxAnnotation annotHighlight, String svg) {
			this.svg = svg;
			this.beat = beat;
			annotations = new BeatAnnotations(plot, annotMarkerStart, annotMarkerEnd, annotHighlight);

		}

		/**
		 * Modifies the Y-axis range of the beat's highlight box annotation.
		 * 
		 * @param yRange The new Y-range to apply to the annotation.
		 */
		private void modifyXYBoxAnnotation(Range yRange) {
			annotations.modifyXYBoxAnnotation(yRange);

		}

	}

	/**
	 * A custom XYPlot implementation that manages multiple, potentially overlapping range axes 
	 * for different data series (e.g., pressure and flow). It also supports drawing 
	 * optional vertical guide lines.
	 */
	private static class CustomXYPlot extends XYPlot {

		private static final long serialVersionUID = -7545876073336403620L;
		private static final BasicStroke stroke = new BasicStroke(1f);
		private boolean paintLines = false;
		private double overlap = 0.5;

		private Range rangeActualDataECG = null;
		private Range rangeActualDataPressure = null;
		private Range rangeActualDataFlow = null;

		/**
		 * Constructs a Custom CY Plot.
		 *
		 * @param paintLines whether to draw guide lines
		 * @param overlap    percent overlap (0–100)
		 */
		private CustomXYPlot(boolean paintLines, int overlap) {
			this.paintLines = paintLines;
			setOverlap(overlap);
		}

		/**
		 * Sets the overlap percentage for pressure/flow.
		 *
		 * @param overlapPerc integer 0–100
		 */
		private void setOverlap(int overlapPerc) {
			this.overlap = Math.max(0, Math.min(overlapPerc, 100)) / 100.0;
		}

		/**
		 * Updates all axes ranges, optionally fitting to visible data.
		 *
		 * @param fitRange      whether to recalc min/max ranges
		 * @param beatsSelected existing selections to adjust
		 * @param possibleBeats tentative beats to adjust
		 */
		private void update(boolean fitRange, Map<BeatSelection, BeatAnnotations[]> beatsSelected,
				List<TentativeBeat> possibleBeats) {

			Range rangeECG = null;
			Range rangePressure = null;
			Range rangeFlow = null;

			if (fitRange || rangeActualDataECG == null) {
				rangeActualDataECG = getMinMaxVisibleRange(0);
				rangeECG = rangeActualDataECG;
				rangeActualDataPressure = getMinMaxVisibleRange(1);
				rangePressure = rangeActualDataPressure;
				rangeActualDataFlow = getMinMaxVisibleRange(2);
				rangeFlow = rangeActualDataFlow;

			} else {
				rangeECG = rangeActualDataECG;
				rangePressure = rangeActualDataPressure;
				rangeFlow = rangeActualDataFlow;
			}

			rangePressure = applyOverlapPressure(rangePressure);
			rangeFlow = applyOverlapFlow(rangeFlow);

			rangeECG = new Range(rangeECG.getLowerBound(), (rangeECG.getLength() * 2) + rangeECG.getUpperBound());
			rangePressure = new Range(rangePressure.getLowerBound() - (rangePressure.getLength() / 2),
					rangePressure.getUpperBound());
			rangeFlow = new Range(rangeFlow.getLowerBound() - (rangeFlow.getLength() / 2), rangeFlow.getUpperBound());

			getRangeAxis(0).setRangeWithMargins(rangeECG, true, false);
			getRangeAxis(1).setRangeWithMargins(rangePressure, true, false);
			getRangeAxis(2).setRangeWithMargins(rangeFlow, true, false);

			if (fitRange) {
				Range rangeSet = getRangeAxis(1).getRange();
				for (TentativeBeat tb : possibleBeats) {
					tb.modifyXYBoxAnnotation(rangeSet);
				}

				for (Entry<BeatSelection, BeatAnnotations[]> beatSelection : beatsSelected.entrySet()) {
					for (BeatAnnotations annot : beatSelection.getValue()) {
						annot.modifyXYBoxAnnotation(rangeSet);
					}
				}
			}

		}

		/**
		 * Applies the configured overlap to the pressure range.
		 *
		 * @param rangePressure the un-overlapped range
		 * @return the overlapped range
		 */
		private Range applyOverlapPressure(Range rangePressure) {

			return new Range(rangePressure.getLowerBound(),
					(rangePressure.getLength() * (1.0 - overlap)) + rangePressure.getUpperBound());
		}

		/**
		 * Applies the configured overlap to the flow range.
		 *
		 * @param rangeFlow the un-overlapped range
		 * @return the overlapped range
		 */
		private Range applyOverlapFlow(Range rangeFlow) {

			return new Range(rangeFlow.getLowerBound() - (rangeFlow.getLength() * (1.0 - overlap)),
					rangeFlow.getUpperBound());
		}

		/**
		 * Finds the min/max Y among the visible domain for a dataset.
		 *
		 * @param dataset the dataset index
		 * @return the Range of visible Y-values
		 */
		private Range getMinMaxVisibleRange(int dataset) {
			double xLower = getDomainAxis().getLowerBound();
			double xUpper = getDomainAxis().getUpperBound();
			XYDataset ds = getDataset(dataset);
			double yMin = Double.POSITIVE_INFINITY;
			double yMax = Double.NEGATIVE_INFINITY;

			int itemCount = ds.getItemCount(0);
			for (int item = 0; item < itemCount; item++) {
				double x = ds.getXValue(0, item);
				if (x >= xLower && x <= xUpper) {
					double y = ds.getYValue(0, item);
					if (!Double.isNaN(y)) {
						yMin = Math.min(yMin, y);
						yMax = Math.max(yMax, y);
					}
				}
			}

			return (yMin == Double.POSITIVE_INFINITY || yMax == Double.NEGATIVE_INFINITY) ? null
					: new Range(yMin, yMax);

		}

		/**
		 * Draws optional vertical guide lines at 25%, 50%, 75% of the domain.
		 */
		@Override
		public void drawOutline(Graphics2D graphics, Rectangle2D rectangle) {
			super.drawOutline(graphics, rectangle);

			if (paintLines) {
				graphics.setPaint(Color.BLACK);
				graphics.setStroke(stroke);

				int y1 = (int) rectangle.getY();
				int y2 = (int) (rectangle.getY() + rectangle.getHeight());
				int x = (int) (rectangle.getX() + ((1.0 / 4.0) * rectangle.getWidth()));
				graphics.drawLine(x, y1, x, y2);

				x = (int) (rectangle.getX() + ((1.0 / 2.0) * rectangle.getWidth()));
				graphics.drawLine(x, y1, x, y2);

				x = (int) (rectangle.getX() + ((3.0 / 4.0) * rectangle.getWidth()));
				graphics.drawLine(x, y1, x, y2);
			}

		}

	}

	/**
	 * An interface defining callbacks for events originating from the BeatsChartPanel,
	 * such as beat selections being finalized or the number of tentative beats changing.
	 */
	public static interface BeatsChartPanelListener {

		/**
		 * Called to signal that the current group of tentative beats should be finalized
		 * into a new {@link BeatSelection}.
		 */
		public void triggerAddSelection();

		/**
		 * Called to update the listener with the current number of tentative beats selected.
		 *
		 * @param numberOfBeats The count of beats currently in the "tentative" selection group.
		 */
		public void setNumBeats(int numberOfBeats);

	}

}
