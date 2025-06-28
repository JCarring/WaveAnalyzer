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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.Range;
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
 * A specialized JPanel that displays two synchronized charts, one above the other,
 * designed for visually aligning and selecting corresponding "beats" or segments
 * from two different {@link HemoData} sets.
 * <p>
 * This panel provides functionalities for:
 * <ul>
 * <li>Synchronized panning and zooming of two charts.
 * <li>Manual and automatic (R-wave based) selection of beat boundaries.
 * <li>Grouping selected beats into named selections.
 * <li>Toggling visual aids like alignment lines and trace visibility.
 * <li>Locking the relative alignment of the charts to prevent accidental changes.
 * <li>Communicating user actions to a parent component via the {@link AlignChartPanelListener}.
 * </ul>
 * It uses keyboard shortcuts and mouse gestures to facilitate the alignment and
 * selection process.
 * </p>
 */
public class AlignChartPanel extends JPanel {

	private static final long serialVersionUID = 1963870844428241002L;

	private volatile boolean paintLines = true;
	private volatile boolean locked = true;

	private CustomChartPanel topChartPnl = null;
	private CustomChartPanel bottomChartPnl = null;
	private final Range yRangeTop;
	private final Range yRangeBottom;

	private final HemoData data1;
	private final HemoData data2;

	private Map<BeatSelection, BeatAnnotations[][]> beatSelections = new LinkedHashMap<BeatSelection, BeatAnnotations[][]>();
	private List<TentativeBeat> currBeatsTop = new ArrayList<TentativeBeat>();
	private List<TentativeBeat> currBeatsBott = new ArrayList<TentativeBeat>();

	private Double selTopStart = null;
	private Double selBottStart = null;
	private Double selTopEnd = null;
	private Double selBottEnd = null;
	private ValueMarker selTopStartMarker = null;
	private ValueMarker selBottStartMarker = null;
	private ValueMarker selTopEndMarker = null;
	private ValueMarker selBottEndMarker = null;

	private boolean autoBeat = false;
	private boolean autoR = false;
	private int hz1 = 0;
	private int hz2 = 0;

	private AlignChartPanelListener listener = null;
	private WeakReference<AlignChartPanel> ref = new WeakReference<AlignChartPanel>(this);

	private final Range visibleStartingRange;

	private static final Color alignColor = new Color(105, 105, 105, 60);
	private final Color[] standardGraphColors = new Color[] { Color.GREEN.darker(), Color.BLUE.brighter(), Color.ORANGE,
			Color.CYAN };
	private volatile int standardGraphColorIndex = 0;
	private final Color[] standardAnnotColors = { new Color(245, 137, 137, 75), new Color(245, 211, 137, 75),
			new Color(177, 245, 137, 75), new Color(137, 245, 231, 75), new Color(137, 144, 245, 75),
			new Color(234, 137, 245, 75), new Color(130, 130, 130, 75), // gray color last
	};
	private int standardAnnotColorIndex = 0;

	/**
	 * Creates a dummy AlignChartPanel with sample data for testing or demonstration
	 * purposes.
	 *
	 * @param listener The listener to handle events from this panel.
	 * @return A new instance of {@link AlignChartPanel} populated with sample data.
	 */
	public static AlignChartPanel createDummy(AlignChartPanelListener listener) {

		double[] time = new double[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 };
		double[] pressure = new double[] { 60, 65, 75, 88, 105, 115, 120, 125, 124, 118, 113, 108, 104, 98, 90, 84, 76,
				68, 64, 60 };
		double[] flow = new double[] { 0.05, 0.08, 0.1, 0.14, 0.17, 0.2, 0.23, 0.26, 0.28, 0.26, 0.23, 0.2, 0.17, 0.14,
				0.1, 0.08, 0.09, 0.07, 0.05, 0.04 };
		double[] time2 = new double[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
				22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39 };
		double[] pressure2 = new double[] { 60, 65, 75, 88, 105, 115, 120, 125, 124, 118, 113, 108, 104, 98, 90, 84, 76,
				68, 64, 60, 60, 65, 75, 88, 105, 115, 120, 125, 124, 118, 113, 108, 104, 98, 90, 84, 76, 68, 64, 60 };
		double[] flow2 = new double[] { 0.05, 0.08, 0.1, 0.14, 0.17, 0.2, 0.23, 0.26, 0.28, 0.26, 0.23, 0.2, 0.17, 0.14,
				0.1, 0.08, 0.09, 0.07, 0.05, 0.04, 0.05, 0.08, 0.1, 0.14, 0.17, 0.2, 0.23, 0.26, 0.28, 0.26, 0.23, 0.2,
				0.17, 0.14, 0.1, 0.08, 0.09, 0.07, 0.05, 0.04 };

		Header timeHeader1 = new Header("Time", 0, true);
		Header timeHeader2 = new Header("Time", 0, true);

		Header pressureHeader1 = new Header("Pressure", 1, false);
		Header pressureHeader2 = new Header("Pressure", 1, false);

		Header flowHeader1 = new Header("Flow", 2, false);
		Header flowHeader2 = new Header("Flow", 2, false);

		HemoData hd1 = new HemoData(new File(""), "trace 1", "Sample 1");
		hd1.setXData(timeHeader1, time);
		hd1.addYData(pressureHeader1, pressure);
		hd1.addYData(flowHeader1, flow, HemoData.OTHER_ALIGN);
		HemoData hd2 = new HemoData(new File(""), "trace 2", "Sample 2");
		hd2.setXData(timeHeader2, time2);
		hd2.addYData(pressureHeader2, pressure2);
		hd2.addYData(flowHeader2, flow2, HemoData.OTHER_ALIGN);

		return new AlignChartPanel(hd1, hd2, listener, false, false);

	}

	/**
	 * Constructs a new AlignChartPanel for comparing and aligning two sets of
	 * hemodynamic data.
	 *
	 * @param dataSet1  The first {@link HemoData} set, which will be displayed in
	 *                  the top chart.
	 * @param dataSet2  The second {@link HemoData} set, which will be displayed in
	 *                  the bottom chart.
	 * @param listener  The listener to notify of user actions and state changes.
	 * @param autoBeat  If true, a single click attempts to select an entire beat
	 *                  automatically.
	 * @param autoSnapR If true, beat boundary selections will snap to the nearest
	 *                  R-wave.
	 */
	public AlignChartPanel(HemoData dataSet1, HemoData dataSet2, AlignChartPanelListener listener, boolean autoBeat,
			boolean autoSnapR) {

		this.data1 = dataSet1;
		this.data2 = dataSet2;
		this.listener = listener;
		this.autoBeat = autoBeat;
		this.hz1 = HemoData.calculateHz(dataSet1.getXData());
		this.hz2 = HemoData.calculateHz(dataSet2.getXData());

		topChartPnl = generatePanel(dataSet1);
		bottomChartPnl = generatePanel(dataSet2);

		Range range1 = topChartPnl.getChart().getXYPlot()
				.getDataRange(topChartPnl.getChart().getXYPlot().getDomainAxis());
		Range range2 = bottomChartPnl.getChart().getXYPlot()
				.getDataRange(bottomChartPnl.getChart().getXYPlot().getDomainAxis());
		visibleStartingRange = Range.combine(range1, range2);

		SyncedAxis domainTop = (SyncedAxis) topChartPnl.getChart().getXYPlot().getDomainAxis();
		SyncedAxis domainBottom = (SyncedAxis) bottomChartPnl.getChart().getXYPlot().getDomainAxis();
		domainTop.setOtherToSyncWith(domainBottom);
		domainBottom.setOtherToSyncWith(domainTop);

		domainTop.setRange(visibleStartingRange);
		domainBottom.setRange(visibleStartingRange);

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
				listener.setNumBeatsTop(0);
				listener.setNumBeatsBottom(0);
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

		getActionMap().put("lockAlignment", new AbstractAction() {

			private static final long serialVersionUID = 6045636711572465888L;

			public void actionPerformed(ActionEvent e) {
				if (ref.get().listener != null) {
					ref.get().listener.triggerLockToggle();
				}

			}

		});

		GroupLayout gl_pnlDisplay = new GroupLayout(this);
		gl_pnlDisplay.setHorizontalGroup(gl_pnlDisplay.createParallelGroup(Alignment.LEADING)
				.addComponent(topChartPnl, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addComponent(bottomChartPnl, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
		gl_pnlDisplay.setVerticalGroup(gl_pnlDisplay.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_pnlDisplay.createSequentialGroup()
						.addComponent(topChartPnl, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE)
						.addComponent(bottomChartPnl, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE)));
		setLayout(gl_pnlDisplay);

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
		topChartPnl.addMouseListener(ml);
		bottomChartPnl.addMouseListener(ml);

		yRangeTop = topChartPnl.getChart().getXYPlot().getRangeAxis().getRange();
		yRangeBottom = bottomChartPnl.getChart().getXYPlot().getRangeAxis().getRange();

	}

	/**
	 * Determines which traces to be shown.
	 * 
	 * @param traces 1 = all; 2 = align only; 3 = all but align
	 */
	public void showVisibleTraces(int traces) {
		if (traces < 1 || traces > 3) {
			throw new IllegalArgumentException("Input out of bounds: " + traces);
		}
		XYPlot plot1 = topChartPnl.getChart().getXYPlot();
		XYPlot plot2 = bottomChartPnl.getChart().getXYPlot();

		boolean alignVisible = (traces == 1 || traces == 2);
		boolean nonAlignVisible = (traces == 1 || traces == 3);

		plot1.getRangeAxis(0).setVisible(alignVisible);
		plot2.getRangeAxis(0).setVisible(alignVisible);
		plot1.getRenderer(0).setSeriesVisible(0, alignVisible);
		plot2.getRenderer(0).setSeriesVisible(0, alignVisible);

		for (int i = 1; i < plot1.getDatasetCount(); i++) {
			plot1.getRangeAxis(i).setVisible(nonAlignVisible);
			plot1.getRenderer(i).setSeriesVisible(0, nonAlignVisible);

		}

		for (int i = 1; i < plot2.getDatasetCount(); i++) {
			plot2.getRangeAxis(i).setVisible(nonAlignVisible);
			plot2.getRenderer(i).setSeriesVisible(0, nonAlignVisible);

		}
	}

	/**
	 * Refreshes the visibility of all series renderers in both the top and bottom
	 * charts. This can be used to force a visual update if the chart state appears
	 * inconsistent.
	 */
	public void refresh() {

		for (Entry<Integer, XYItemRenderer> en : topChartPnl.getChart().getXYPlot().getRenderers().entrySet()) {
			en.getValue().setSeriesVisible(0, en.getValue().getSeriesVisible(0));
		}
		for (Entry<Integer, XYItemRenderer> en : bottomChartPnl.getChart().getXYPlot().getRenderers().entrySet()) {
			en.getValue().setSeriesVisible(0, en.getValue().getSeriesVisible(0));
		}

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
	}

	/**
	 * Clears current bounds, including the start and end of a current beat for both
	 * top and bottom graph, as well as removing the associated domain marker
	 */
	public void clearCurrentBounds() {
		selTopStart = null;
		selTopEnd = null;
		selBottStart = null;
		selBottEnd = null;
		if (selTopStartMarker != null) {
			topChartPnl.getChart().getXYPlot().removeDomainMarker(selTopStartMarker);
			selTopStartMarker = null;
		}
		if (selTopEndMarker != null) {
			topChartPnl.getChart().getXYPlot().removeDomainMarker(selTopEndMarker);
			selTopEndMarker = null;
		}
		if (selBottStartMarker != null) {
			bottomChartPnl.getChart().getXYPlot().removeDomainMarker(selBottStartMarker);
			selBottStartMarker = null;
		}
		if (selBottEndMarker != null) {
			bottomChartPnl.getChart().getXYPlot().removeDomainMarker(selBottEndMarker);
			selBottEndMarker = null;
		}
	}

	/**
	 * Clears all beats currently selected as well as the current bounds for another
	 * beat, if set. Reflects this in the charts.
	 */
	public void clearCurrentSelection() {
		clearCurrentBounds();
		for (TentativeBeat tentativeBeat : currBeatsTop) {
			clearAnnotations(tentativeBeat);
		}
		for (TentativeBeat tentativeBeat : currBeatsBott) {
			clearAnnotations(tentativeBeat);
		}
		currBeatsTop.clear();
		currBeatsBott.clear();
	}

	/**
	 * Removes the visual annotations (highlight box, start/end lines) for a given tentative beat from the chart.
	 *
	 * @param tentativeBeat The beat whose annotations should be cleared.
	 */
	private void clearAnnotations(TentativeBeat tentativeBeat) {
		XYPlot plot = tentativeBeat.annotations.plot;
		plot.removeAnnotation(tentativeBeat.annotations.boxHighlight);
		plot.removeDomainMarker(tentativeBeat.annotations.startLine);
		plot.removeDomainMarker(tentativeBeat.annotations.endLine);

	}

	/**
	 * Removes a set of beat annotations from their associated plot.
	 *
	 * @param annot The {@link BeatAnnotations} object to remove.
	 */
	private void clearAnnotations(BeatAnnotations annot) {
		XYPlot plot = annot.plot;
		plot.removeAnnotation(annot.boxHighlight);
		plot.removeDomainMarker(annot.startLine);
		plot.removeDomainMarker(annot.endLine);

	}

	/**
	 * Clears all current selections and removes annotations. Only affects graphs
	 * and no other components
	 */
	public void clearAllSelections() {
		for (Entry<BeatSelection, BeatAnnotations[][]> en : beatSelections.entrySet()) {
			if (en.getValue() != null) {
				for (BeatAnnotations annot : en.getValue()[0]) {
					clearAnnotations(annot);
				}
				for (BeatAnnotations annot : en.getValue()[1]) {
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
		BeatAnnotations[][] annotations = this.beatSelections.remove(beatSel);
		if (annotations != null) {
			for (BeatAnnotations annot : annotations[0]) {
				clearAnnotations(annot);
			}
			for (BeatAnnotations annot : annotations[1]) {
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
	 * Zooms into both graphs simultaneously
	 */
	public void zoomIn() {
		((SyncedAxis) this.topChartPnl.getChart().getXYPlot().getDomainAxis()).zoomInCentrally();
		// causes the other to zoom

	}

	/**
	 * Zoom out of both graphs simultaneously
	 */
	public void zoomOut() {
		((SyncedAxis) this.topChartPnl.getChart().getXYPlot().getDomainAxis()).zoomOutCentrally();

	}

	/**
	 * f Whether or not the alignment ligns should be displayed
	 * 
	 * @param enabled if should enable the alignment lines
	 */
	public void setAlignLines(boolean enabled) {
		this.paintLines = enabled;
		this.topChartPnl.getChart().setTitle(topChartPnl.getChart().getTitle());
		this.bottomChartPnl.getChart().setTitle(bottomChartPnl.getChart().getTitle());

	}

	/**
	 * @return current number of selections
	 */
	public int getNumberOfSelections() {
		return this.beatSelections.size();
	}

	/**
	 * Locks the current alignment of the top and bottom graphs so that the user
	 * cannot accidentally change it
	 * 
	 * @param locked true if should lock
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;

		((SyncedAxis) topChartPnl.getChart().getXYPlot().getDomainAxis()).setLocked(locked);
		((SyncedAxis) bottomChartPnl.getChart().getXYPlot().getDomainAxis()).setLocked(locked);

		if (locked) {
			topChartPnl.getChart().getXYPlot().setBackgroundPaint(Color.LIGHT_GRAY);
			topChartPnl.getChart().getXYPlot().setBackgroundAlpha(0.5f);
			bottomChartPnl.getChart().getXYPlot().setBackgroundPaint(Color.LIGHT_GRAY);
			bottomChartPnl.getChart().getXYPlot().setBackgroundAlpha(0.5f);
		} else {
			topChartPnl.getChart().getXYPlot().setBackgroundPaint(null);
			topChartPnl.getChart().getXYPlot().setBackgroundAlpha(0f);
			bottomChartPnl.getChart().getXYPlot().setBackgroundPaint(null);
			bottomChartPnl.getChart().getXYPlot().setBackgroundAlpha(0f);
		}

	}

	/**
	 * Programmatically sets the time alignment between the two charts by panning
	 * them to specific time points. All current beat selections are cleared when
	 * this is called.
	 *
	 * @param xValue1 The time value (in ms) to which the top chart should pan.
	 * @param xValue2 The time value (in ms) to which the bottom chart should pan.
	 * @return true if the alignment was successful, false if a time value was
	 *         outside the data range.
	 */
	public boolean setTimeAlignment(Double xValue1, Double xValue2) {
		clearAllSelections();
		double[] time1 = data1.getXData();
		double[] time2 = data2.getXData();
		if (!_verifyAlign(xValue1, time1) || !_verifyAlign(xValue2, time2)) {
			return false;
		}
		if (xValue1 != null) {
			((SyncedAxis) topChartPnl.getChart().getXYPlot().getDomainAxis()).setPan(xValue1, visibleStartingRange);
		}
		if (xValue2 != null) {
			((SyncedAxis) bottomChartPnl.getChart().getXYPlot().getDomainAxis()).setPan(xValue2, visibleStartingRange);
		}

		return true;
	}

	/**
	 * Verifies that a given alignment time value is within the valid domain (time range) of a dataset.
	 *
	 * @param val The time value to check. Can be null.
	 * @param domain The time data array to check against.
	 * @return true if the value is null or within the domain's bounds, false otherwise.
	 */
	private boolean _verifyAlign(Double val, double[] domain) {
		if (val == null)
			return true;
		else if (val > domain[domain.length - 1] || val < domain[0])
			return false;
		else
			return true;
	}

	/**
	 * Attempts to delete a previously selected tentative beat based on the current
	 * mouse position. If the mouse is hovering over a beat in either the top or
	 * bottom chart, that beat is removed. Otherwise nothing happens
	 */
	public void attemptBeatDeletion() {

		ClickResponse cr = getXValueFromScreenPos();
		if (cr == null)
			return;

		Iterator<TentativeBeat> itr = currBeatsTop.iterator();
		while (itr.hasNext()) {
			TentativeBeat tb = itr.next();
			if (_containsBeat(tb.annotations.boxHighlight, cr.x)) {
				clearAnnotations(tb);
				itr.remove();
			}
		}
		itr = currBeatsBott.iterator();
		while (itr.hasNext()) {
			TentativeBeat tb = itr.next();
			if (_containsBeat(tb.annotations.boxHighlight, cr.x)) {
				clearAnnotations(tb);
				itr.remove();
			}
		}
		listener.setNumBeatsTop(currBeatsTop.size());
		listener.setNumBeatsBottom(currBeatsBott.size());

	}

	/**
	 * Checks if a given x-axis value falls within the horizontal bounds of a beat's box annotation.
	 *
	 * @param annotation The {@link XYBoxAnnotation} representing the beat.
	 * @param xValue The x-axis value to check.
	 * @return true if the xValue is within the annotation's bounds, false otherwise.
	 */
	public boolean _containsBeat(XYBoxAnnotation annotation, double xValue) {
		return xValue >= annotation.getX0() && xValue <= annotation.getX1();
	}

	
	/**
	 * Attempts to select bounds for another beat
	 * 
	 * @param keyNum Number of the key that was pressed
	 */
	public void attemptBoundSelection(int keyNum) {

		if (autoBeat) {
			attemptBoundSelectionAuto();
			return;
		}

		ClickResponse cr = getXValueFromScreenPos();

		if (keyNum == 3) {
			// Try to add a beat

			// Validate that things are set
			Range xRange = null;
			Range yRange = null;
			List<TentativeBeat> currBeats = cr.isTopChart ? currBeatsTop : currBeatsBott;
			if (cr.isTopChart) {
				if (selTopStart == null || selTopEnd == null) {
					Utils.showError("Both bounds of the beat must be set first.", this);
					return;
				}
				xRange = new Range(selTopStart, selTopEnd);
				yRange = yRangeTop;
			} else {
				if (selBottStart == null || selBottEnd == null) {
					Utils.showError("Both bounds of the beat must be set first.", this);
					return;
				}
				xRange = new Range(selBottStart, selBottEnd);
				yRange = yRangeBottom;
			}

			Color annotationColor = standardAnnotColors[standardAnnotColorIndex];
			XYBoxAnnotation highlightAnnotation = new XYBoxAnnotation(xRange.getLowerBound(), yRange.getLowerBound(),
					xRange.getUpperBound(), yRange.getUpperBound(), null, null, annotationColor);
			ValueMarker vmStart = new ValueMarker(xRange.getLowerBound());
			ValueMarker vmEnd = new ValueMarker(xRange.getUpperBound());
			vmStart.setStroke(new BasicStroke(2f));
			vmStart.setPaint(annotationColor.darker().darker());
			vmEnd.setStroke(new BasicStroke(2f));
			vmEnd.setPaint(annotationColor.darker().darker());

			cr.cp.getChart().getXYPlot().addAnnotation(highlightAnnotation);
			cr.cp.getChart().getXYPlot().addDomainMarker(vmStart);
			cr.cp.getChart().getXYPlot().addDomainMarker(vmEnd);

			String svgString = null;
			try {
				svgString = ComboChartSaver.getSaveAsSVGString(cr.cp.getChart(), cr.cp.getWidth(), cr.cp.getHeight());
			} catch (Exception e) {
				e.printStackTrace();
				svgString = null;
			}

			currBeats.add(_createBeat(cr.cp, cr.hd, xRange.getLowerBound(), xRange.getUpperBound(), vmStart, vmEnd,
					highlightAnnotation, svgString));

			clearCurrentBounds();
			listener.setNumBeatsTop(currBeatsTop.size());
			listener.setNumBeatsBottom(currBeatsBott.size());
			return;
		}

		double xValue = cr.x;
		if (autoR) {
			QRS qrs = getClosestQRS(cr);
			if (qrs == null) {
				Utils.showError("Could not identify R Waves in the currently displayed domain", this);
				return;
			}
			xValue = qrs.getArrayValue();
		}

		if (keyNum == 0 || keyNum == 1) {
			// Set the first bound

			if (cr.isTopChart) {
				if (selTopEnd != null && xValue >= selTopEnd) {
					Utils.showError("Beat start point must be before the end point.", this);
					return;
				}
				if (selTopStartMarker != null) {
					this.topChartPnl.getChart().getXYPlot().removeDomainMarker(selTopStartMarker);
					selTopStartMarker = null;
				}
			} else {
				if (selBottEnd != null && xValue >= selBottEnd) {
					Utils.showError("Beat start point must be before the end point.", this);
					return;
				}
				if (selBottStartMarker != null) {
					this.bottomChartPnl.getChart().getXYPlot().removeDomainMarker(selBottStartMarker);
					selBottStartMarker = null;
				}
			}

			ValueMarker vm = new ValueMarker(xValue);
			vm.setStroke(new BasicStroke(2f));
			vm.setPaint(Utils.colorPurpleDarker);
			cr.cp.getChart().getXYPlot().addDomainMarker(vm);
			if (cr.isTopChart) {
				selTopStartMarker = vm;
				selTopStart = xValue;
			} else {
				selBottStartMarker = vm;
				selBottStart = xValue;
			}
		} else if (keyNum == 2) {

			// Set the second

			if (cr.isTopChart) {
				if (selTopStart != null && xValue <= selTopStart) {
					Utils.showError("Beat end point must be after the start point.", this);
					return;
				}
				if (selTopEndMarker != null) {
					this.topChartPnl.getChart().getXYPlot().removeDomainMarker(selTopEndMarker);
					selTopEndMarker = null;
				}
			} else {
				if (selBottStart != null && xValue <= selBottStart) {
					Utils.showError("Beat end point must be after the start point.", this);
					return;
				}
				if (selBottEndMarker != null) {
					this.bottomChartPnl.getChart().getXYPlot().removeDomainMarker(selBottEndMarker);
					selBottEndMarker = null;
				}
			}

			ValueMarker vm = new ValueMarker(xValue);
			vm.setStroke(new BasicStroke(2f));
			vm.setPaint(Utils.colorPurpleDarker);
			cr.cp.getChart().getXYPlot().addDomainMarker(vm);
			if (cr.isTopChart) {
				selTopEndMarker = vm;
				selTopEnd = xValue;
			} else {
				selBottEndMarker = vm;
				selBottEnd = xValue;
			}

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

		List<TentativeBeat> currBeats = cr[0].isTopChart ? currBeatsTop : currBeatsBott;
		Range yRange = cr[0].isTopChart ? yRangeTop : yRangeBottom;

		QRS[] complexes = getClosestQRSComplexes(cr[0].cp, cr[0].hd, cr[0].isTopChart ? hz1 : hz2, cr[0].x, cr[1].x);
		if (complexes == null)
			return;
		String svgString = null;
		try {
			svgString = ComboChartSaver.getSaveAsSVGString(cr[0].cp.getChart(), cr[0].cp.getWidth(),
					cr[0].cp.getHeight());
		} catch (Exception e) {
			e.printStackTrace();
			svgString = null;
		}

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

			cr[0].cp.getChart().getXYPlot().addAnnotation(highlightAnnotation);
			cr[0].cp.getChart().getXYPlot().addDomainMarker(vmStart);
			cr[0].cp.getChart().getXYPlot().addDomainMarker(vmEnd);

			currBeats.add(_createBeat(cr[0].cp, cr[0].hd, complexStart.getArrayValue(), complexEnd.getArrayValue(),
					vmStart, vmEnd, highlightAnnotation, svgString));
			listener.setNumBeatsTop(currBeatsTop.size());
			listener.setNumBeatsBottom(currBeatsBott.size());
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
			Utils.showError("Could not identify R Waves in the currently displayed domain", this);
			return;
		}

		List<TentativeBeat> currBeats;
		if (cr.isTopChart) {
			currBeats = currBeatsTop;
		} else {
			currBeats = currBeatsBott;
		}

		Range xRange = new Range(closest[0].getArrayValue(), closest[1].getArrayValue());
		Range yRange = cr.isTopChart ? yRangeTop : yRangeBottom;
		Color annotationColor = standardAnnotColors[standardAnnotColorIndex];
		XYBoxAnnotation highlightAnnotation = new XYBoxAnnotation(xRange.getLowerBound(), yRange.getLowerBound(),
				xRange.getUpperBound(), yRange.getUpperBound(), null, null, annotationColor);
		ValueMarker vmStart = new ValueMarker(xRange.getLowerBound());
		ValueMarker vmEnd = new ValueMarker(xRange.getUpperBound());
		vmStart.setStroke(new BasicStroke(2f));
		vmStart.setPaint(annotationColor.darker());
		vmEnd.setStroke(new BasicStroke(2f));
		vmEnd.setPaint(annotationColor.darker());

		cr.cp.getChart().getXYPlot().addAnnotation(highlightAnnotation);
		cr.cp.getChart().getXYPlot().addDomainMarker(vmStart);
		cr.cp.getChart().getXYPlot().addDomainMarker(vmEnd);

		String svgString = null;
		try {
			svgString = ComboChartSaver.getSaveAsSVGString(cr.cp.getChart(), cr.cp.getWidth(), cr.cp.getHeight());
		} catch (Exception e) {
			e.printStackTrace();
			svgString = null;
		}

		currBeats.add(_createBeat(cr.cp, cr.hd, closest[0].getArrayValue(), closest[1].getArrayValue(), vmStart, vmEnd,
				highlightAnnotation, svgString));
		listener.setNumBeatsTop(currBeatsTop.size());
		listener.setNumBeatsBottom(currBeatsBott.size());
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
		if (currBeatsBott.isEmpty() || currBeatsTop.isEmpty()) {
			Utils.showError("At least one beat needs to be selected for both data sets", this);
			return null;
		}

		// pick name
		String name = Utils.promptTextInput("Selection name?", this);

		if (name == null || name.isBlank())
			return null; // user cancelled

		name = name.trim();

		// see if one exists with similar name
		for (BeatSelection bs : beatSelections.keySet()) {
			if (name.equalsIgnoreCase(bs.getName().trim())) {
				Utils.showError("There is already a selection with this name!", null);
				return null;
			}
		}

		// Create new beat selection, combined all current XYAnnotations for each
		// individual beat
		BeatSelection newBeatSelection = new BeatSelection(name);

		ArrayList<BeatAnnotations> topAnnotations = new ArrayList<BeatAnnotations>();
		ArrayList<BeatAnnotations> bottomAnnotations = new ArrayList<BeatAnnotations>();
		for (TentativeBeat tentBeat : currBeatsTop) {
			if (tentBeat.svg != null) {
				newBeatSelection.addBeat(tentBeat.beat, tentBeat.svg, "Top");
			} else {
				newBeatSelection.addBeat(tentBeat.beat, "Top");
			}
			topAnnotations.add(tentBeat.annotations);
		}
		for (TentativeBeat tentBeat : currBeatsBott) {
			if (tentBeat.svg != null) {
				newBeatSelection.addBeat(tentBeat.beat, tentBeat.svg, "Bottom");
			} else {
				newBeatSelection.addBeat(tentBeat.beat, "Bottom");
			}
			bottomAnnotations.add(tentBeat.annotations);
		}
		BeatAnnotations[][] beatSelectionAnnotation = new BeatAnnotations[][] {
				topAnnotations.toArray(new BeatAnnotations[0]), bottomAnnotations.toArray(new BeatAnnotations[0]) };

		beatSelections.put(newBeatSelection, beatSelectionAnnotation);
		currBeatsBott.clear();
		currBeatsTop.clear();

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
		int hz;
		if (cr.isTopChart) {
			hz = hz1;
		} else {
			hz = hz2;
		}
		Range range = cr.cp.getChart().getXYPlot().getDomainAxis().getRange();

		int startIndex = getClosestTimeIndex(range.getLowerBound(), cr.hd);
		int endIndex = getClosestTimeIndex(range.getUpperBound(), cr.hd);

		if ((endIndex - startIndex) < 3) {
			return null;
		}
		Header alignHeader = cr.hd.getHeaderByFlag(HemoData.OTHER_ALIGN).get(0);
		if (alignHeader == null) {
			return null;
		}
		List<QRS> qrsComplexes = QRSDetector.getQRSOnSubset(cr.hd.getXData(), cr.hd.getYData(alignHeader), startIndex,
				endIndex, hz, true);
		if (qrsComplexes.size() < 2) {
			return null;
		}
		double distance = Double.MAX_VALUE;
		QRS closestQRS = null;
		for (QRS qrs : qrsComplexes) {
			double queryDistance = Math.abs(cr.x - qrs.getArrayValue());
			if (queryDistance < distance) {
				distance = queryDistance;
				closestQRS = qrs;
			}
		}
		return closestQRS;
	}

	/**
	 * Gets the QRS complexes above and below clicked spot. Will return null if both
	 * weren't found.
	 */
	private QRS[] getClosestQRSAboveBelow(ClickResponse cr) {
		int hz;
		if (cr.isTopChart) {
			hz = hz1;
		} else {
			hz = hz2;
		}
		Range range = cr.cp.getChart().getXYPlot().getDomainAxis().getRange();
		int startIndex = getClosestTimeIndex(range.getLowerBound(), cr.hd);
		int endIndex = getClosestTimeIndex(range.getUpperBound(), cr.hd);
		if ((endIndex - startIndex) < 3) {
			return null;
		}
		Header alignHeader = cr.hd.getHeaderByFlag(HemoData.OTHER_ALIGN).get(0);
		if (alignHeader == null) {
			return null;
		}
		List<QRS> qrsComplexes = QRSDetector.getQRSOnSubset(cr.hd.getXData(), cr.hd.getYData(alignHeader), startIndex,
				endIndex, hz, true);
		if (qrsComplexes.size() < 2) {
			return null;
		}
		double distanceAbove = Double.MAX_VALUE;
		double distanceBelow = Double.MAX_VALUE;
		QRS closestQRSAbove = null;
		QRS closestQRSBelow = null;
		for (QRS qrs : qrsComplexes) {
			double queryDistance = qrs.getArrayValue() - cr.x;
			if (queryDistance >= 0 && queryDistance < distanceAbove) {
				closestQRSAbove = qrs;
				distanceAbove = queryDistance;
			}
		}
		for (QRS qrs : qrsComplexes) {
			double queryDistance = cr.x - qrs.getArrayValue();
			if (queryDistance >= 0 && queryDistance < distanceBelow) {
				closestQRSBelow = qrs;
				distanceBelow = queryDistance;
			}
		}
		if (closestQRSAbove == null || closestQRSBelow == null)
			return null;

		return new QRS[] { closestQRSBelow, closestQRSAbove };
	}

	/**
	 * Gets the QRS complexes within the section, as well as one below and above
	 * weren't found.
	 */
	private QRS[] getClosestQRSComplexes(ChartPanel cp, HemoData hd, int hz, double x1, double x2) {

		Range range = cp.getChart().getXYPlot().getDomainAxis().getRange();
		int startIndex = getClosestTimeIndex(range.getLowerBound(), hd);
		int endIndex = getClosestTimeIndex(range.getUpperBound(), hd);
		if ((endIndex - startIndex) < 3) {
			return null;
		}
		Header alignHeader = hd.getHeaderByFlag(HemoData.OTHER_ALIGN).get(0);
		if (alignHeader == null) {
			return null;
		}
		List<QRS> qrsComplexes = QRSDetector.getQRSOnSubset(hd.getXData(), hd.getYData(alignHeader), startIndex,
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
	 * Creates a new beat with the specific start and end points. Does not validate
	 * these inputs
	 */
	private TentativeBeat _createBeat(ChartPanel chartPanel, HemoData hd, double start, double end,
			ValueMarker annotStart, ValueMarker annotEnd, XYBoxAnnotation annotHighlight, String chartImgSVGString) {
		int startIndex = getClosestTimeIndex(start, hd);
		int endIndex = getClosestTimeIndex(end, hd);
		Beat beat = new Beat(hd, startIndex, endIndex);
		return new TentativeBeat(chartPanel, beat, annotStart, annotEnd, annotHighlight, chartImgSVGString);
	}

	/**
	 * Gets the index of the time array closest to the specified time
	 * 
	 * @param timeSelected
	 * @return index
	 */
	private int getClosestTimeIndex(double timeSelected, HemoData hd) {

		int bestIndex = 0;

		double[] time = hd.getXData();
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
	private int pointIsOverCharts(Point p) {
		Rectangle2D chartrect1 = topChartPnl.getScreenDataArea();
		Rectangle2D chartrect2 = bottomChartPnl.getScreenDataArea();

		Point parentUp1 = topChartPnl.getLocationOnScreen();
		Point parentUp2 = bottomChartPnl.getLocationOnScreen();

		Rectangle newRect1 = new Rectangle(parentUp1.x + (int) chartrect1.getX(), parentUp1.y + (int) chartrect1.getY(),
				(int) chartrect1.getWidth(), (int) chartrect1.getHeight());
		Rectangle newRect2 = new Rectangle(parentUp2.x + (int) chartrect2.getX(), parentUp2.y + (int) chartrect2.getY(),
				(int) chartrect2.getWidth(), (int) chartrect2.getHeight());
		if (newRect1.contains(p))
			return 1;
		else if (newRect2.contains(p))
			return 2;
		else
			return 0;

	}

	/**
	 * Return the xValue at which the mouse pointer is over vertically on the graph.
	 * 
	 * It will return null if the mouse point was not actually over the graph.
	 */
	private ClickResponse getXValueFromScreenPos() {

		Point mousePoint = MouseInfo.getPointerInfo().getLocation();

		int chart = pointIsOverCharts(mousePoint);
		ClickResponse cr = null;

		if (chart == 1) {
			cr = new ClickResponse(true, topChartPnl, data1);
		} else if (chart == 2) {
			cr = new ClickResponse(false, bottomChartPnl, data2);
		} else {
			return null;
		}

		SwingUtilities.convertPointFromScreen(mousePoint, this); // edits in place without return

		Point2D point2dPnl = cr.cp.translateScreenToJava2D(mousePoint);

		Rectangle2D plotArea = cr.cp.getScreenDataArea();
		XYPlot plot = (XYPlot) cr.cp.getChart().getPlot();

		cr.x = plot.getDomainAxis().java2DToValue(point2dPnl.getX(), plotArea, plot.getDomainAxisEdge());

		if (cr.hd.containsXData(cr.x)) {
			return cr;
		} else {
			return null;
		}

	}

	/**
	 * Calculates the data-space x-values from two screen points, typically from a mouse drag.
	 *
	 * @param mousePoint1 The first screen point.
	 * @param mousePoint2 The second screen point.
	 * @return An array of two {@link ClickResponse} objects with calculated x-values, or null if the
	 * points are not over the same chart or are out of bounds.
	 */
	private ClickResponse[] getXValueFromScreenPos(Point mousePoint1, Point mousePoint2) {

		int chart1 = pointIsOverCharts(mousePoint1);
		int chart2 = pointIsOverCharts(mousePoint2);
		if (chart1 != chart2) {
			return null;
		}

		ClickResponse cr1 = null;
		ClickResponse cr2 = null;

		if (chart1 == 1) {
			cr1 = new ClickResponse(true, topChartPnl, data1);
			cr2 = new ClickResponse(true, topChartPnl, data1);
		} else if (chart1 == 2) {
			cr1 = new ClickResponse(false, bottomChartPnl, data2);
			cr2 = new ClickResponse(false, bottomChartPnl, data2);
		} else {

			return null;
		}

		SwingUtilities.convertPointFromScreen(mousePoint1, this); // edits in place without return
		SwingUtilities.convertPointFromScreen(mousePoint2, this); // edits in place without return

		Point2D point2dPnl1 = cr1.cp.translateScreenToJava2D(mousePoint1);
		Point2D point2dPnl2 = cr1.cp.translateScreenToJava2D(mousePoint2);

		Rectangle2D plotArea = cr1.cp.getScreenDataArea();

		XYPlot plot = (XYPlot) cr1.cp.getChart().getPlot();

		cr1.x = plot.getDomainAxis().java2DToValue(point2dPnl1.getX(), plotArea, plot.getDomainAxisEdge());
		cr2.x = plot.getDomainAxis().java2DToValue(point2dPnl2.getX(), plotArea, plot.getDomainAxisEdge());

		if (cr1.hd.containsXData(cr1.x) && cr2.hd.containsXData(cr2.x)) {
			return new ClickResponse[] { cr1, cr2 };
		} else {
			return null;
		}

	}

	/////////////////////////////////////////////////////
	// Chart Set up
	/////////////////////////////////////////////////////

	/**
	 * Generates a {@link CustomChartPanel} for displaying a given set of hemodynamic data.
	 *
	 * @param hd The {@link HemoData} to display in the chart.
	 * @return A configured {@link CustomChartPanel}.
	 */
	private CustomChartPanel generatePanel(HemoData hd) {

		CustomChartPanel cp = new CustomChartPanel(createChart(hd), this);

		return cp;
	}

	/**
	 * Creates and configures a JFreeChart instance for displaying hemodynamic data.
	 *
	 * @param hd The {@link HemoData} to be plotted.
	 * @return A configured {@link JFreeChart} instance.
	 */
	private JFreeChart createChart(HemoData hd) {
		JFreeChart chart = new JFreeChart(Utils.getShortenedFilePath(hd.getFile().getParent(), 80),
				Utils.getSubTitleSubFont(), createPlot(hd), true);
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
	 * Creates and configures the XYPlot for a chart, setting up all axes, datasets, and renderers.
	 *
	 * @param hd The {@link HemoData} containing the series to plot.
	 * @return A configured {@link XYPlot} instance.
	 */
	private XYPlot createPlot(HemoData hd) {
		XYPlot plot = new CustomXYPlot(this);
		Font font = Utils.getTextFont(false);
		int textFontSize = font.getSize();
		float tickStroke = Math.max(textFontSize / 8.0f, 1.5f);
		double[] time = hd.getXData();
		BasicStroke strokeThickSolid = new BasicStroke(tickStroke);

		// DOMAIN AXIS
		NumberAxis domainAxis = new SyncedAxis(hd.getXHeader().getName());
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

		// ranges

		int datasetCounterOther = 1;
		int datasetCounterTemp = 1;

		Header alignHeader = hd.getHeaderByFlag(HemoData.OTHER_ALIGN).get(0);

		Color nextColor = null;
		for (Header yHeader : hd.getYHeaders()) {

			if (yHeader.equals(alignHeader)) {
				datasetCounterTemp = 0;
				nextColor = alignColor;
			} else {
				nextColor = standardGraphColors[standardGraphColorIndex];
				if (standardGraphColorIndex < standardGraphColors.length - 1) {
					standardGraphColorIndex++;
				}
			}
			double[] yValues = hd.getYData(yHeader);
			XYSeries ySeries = new XYSeries(yHeader.getName());
			for (int i = 0; i < time.length; i++) {
				ySeries.add(time[i], yValues[i]);
				ySeries.add(time[i], yValues[i]);
			}
			XYSeriesCollection dataset = new XYSeriesCollection();
			dataset.addSeries(ySeries);
			plot.setDataset(datasetCounterTemp, dataset);

			SamplingXYLineRenderer renderer = new SamplingXYLineRenderer();

			renderer.setSeriesPaint(0, nextColor);

			renderer.setSeriesStroke(0, strokeThickSolid, false);
			renderer.setAutoPopulateSeriesShape(true);
			renderer.setLegendTextPaint(datasetCounterTemp, Color.RED);
			renderer.setSeriesVisibleInLegend(0, false, true);

			// renderer.setSeriesShape(0, null); // may need to delete
			plot.setRenderer(datasetCounterTemp, renderer);

			NumberAxis rangeAxis = new NumberAxis(yHeader.getName());
			rangeAxis.setLabelFont(font);
			rangeAxis.setLabelPaint(renderer.lookupSeriesPaint(0));
			rangeAxis.setTickLabelsVisible(false);
			rangeAxis.setTickMarksVisible(false);
			if (datasetCounterTemp >= 2) {
				rangeAxis.setVisible(false);
			}
			rangeAxis.setLabel("");
			rangeAxis.setAutoRange(true);
			rangeAxis.setAutoRangeIncludesZero(false);
			rangeAxis.setAutoTickUnitSelection(true);
			rangeAxis.setTickMarkStroke(strokeThickSolid);
			rangeAxis.setTickMarkInsideLength(textFontSize / 2);
			rangeAxis.setTickLabelFont(font);
			rangeAxis.setTickMarkPaint(Color.BLACK);
			rangeAxis.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
			rangeAxis.setAxisLineStroke(strokeThickSolid);
			rangeAxis.setAxisLinePaint(Color.BLACK);
			plot.setRangeAxis(datasetCounterTemp, rangeAxis);
			plot.mapDatasetToRangeAxis(datasetCounterTemp, datasetCounterTemp);
			if (datasetCounterTemp == 0) {
				datasetCounterTemp = datasetCounterOther;
			} else {
				datasetCounterOther++;
				datasetCounterTemp = datasetCounterOther;
			}

		}

		plot.setOutlineVisible(false);
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(false);
		return plot;

	}

	/**
	 * A helper class to encapsulate the response from a mouse click, indicating which
	 * chart was clicked, a reference to it, its associated data, and the calculated x-axis value.
	 */
	private static class ClickResponse {
		private final boolean isTopChart;
		private final ChartPanel cp;
		private final HemoData hd;
		private double x = 0;

		private ClickResponse(boolean isTopChart, ChartPanel cp, HemoData hd) {
			this.isTopChart = isTopChart;
			this.cp = cp;
			this.hd = hd;
		}
	}

	/**
	 * A helper class to hold the JFreeChart annotations that visually represent a single selected beat.
	 * This includes the start and end markers and the highlighted background box.
	 */
	private static class BeatAnnotations {

		private final XYPlot plot;
		private final ValueMarker startLine;
		private final ValueMarker endLine;
		private final XYBoxAnnotation boxHighlight;

		private BeatAnnotations(XYPlot plot, ValueMarker annotMarkerStart, ValueMarker annotMarkerEnd,
				XYBoxAnnotation annotHighlight) {
			this.startLine = annotMarkerStart;
			this.endLine = annotMarkerEnd;
			this.boxHighlight = annotHighlight;
			this.plot = plot;
		}

	}

	/**
	 * Basically represents a beat, but includes an SVG image capture of the beat
	 * selection. These beats are not included in a {@link BeatSelection} and thus
	 * are tentative.
	 */
	private static class TentativeBeat {

		private final String svg;
		private final Beat beat;
		private final BeatAnnotations annotations;

		private TentativeBeat(ChartPanel cp, Beat beat, ValueMarker annotMarkerStart, ValueMarker annotMarkerEnd,
				XYBoxAnnotation annotHighlight, String svg) {
			this.svg = svg;
			this.beat = beat;
			annotations = new BeatAnnotations(cp.getChart().getXYPlot(), annotMarkerStart, annotMarkerEnd,
					annotHighlight);

		}

	}

	/**
	 * A custom ChartPanel that supports custom background painting and mouse listeners for
	 * selecting beats by clicking and dragging.
	 */
	private static class CustomChartPanel extends ChartPanel {

		private static final long serialVersionUID = 7471981154377369968L;
		private Rectangle2D highlightBox = null;
		private double startX, startY;

		private final WeakReference<CustomChartPanel> ref = new WeakReference<CustomChartPanel>(this);
		private final WeakReference<AlignChartPanel> parent;

		/**
		 * Constructs a custom chart panel.
		 *
		 * @param chart  The JFreeChart to display.
		 * @param parent A reference to the parent {@link AlignChartPanel}.
		 */
		private CustomChartPanel(JFreeChart chart, AlignChartPanel parent) {
			super(chart);
			this.parent = new WeakReference<AlignChartPanel>(parent);
			setOpaque(false);
			setBackground(null);
			setMouseZoomable(false);
			setMouseWheelEnabled(true);
			setDomainZoomable(true);
			setRangeZoomable(false);
			// setZoomTriggerDistance(Integer.MAX_VALUE); may need this
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

							parent.attemptBoundSelectionAuto(p1, p2);
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
					if (parent.autoBeat) {
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

		}

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
		 * Overrides the default paint method to draw a custom highlight box when the user is
		 * drag-selecting a region.
		 */
		@Override
		public void paint(Graphics g) {
			super.paint(g);
			if (highlightBox != null) {
				Graphics2D g2 = (Graphics2D) g.create();
				Color color = parent.get().standardAnnotColors[parent.get().standardAnnotColorIndex];
				g2.setColor(color);
				g2.fill(highlightBox);
				g2.setStroke(new BasicStroke(1.0f));
				g2.setColor(color.darker().darker());
				g2.draw(highlightBox);
				g2.dispose();
			}
		}

	}

	/**
	 * A custom XYPlot that can draw vertical helper lines on the chart background to aid in alignment.
	 */
	private static class CustomXYPlot extends XYPlot {

		private static final long serialVersionUID = -7545876073336403620L;
		private static final BasicStroke stroke = new BasicStroke(1f);
		private AlignChartPanel pnl;

		private CustomXYPlot(AlignChartPanel panel) {
			this.pnl = panel;
		}

		/**
		 * Draws the plot outline and, if enabled, adds vertical alignment helper lines.
		 *
		 * @param graphics the graphics context.
		 * @param rectangle the data area.
		 */
		@Override
		public void drawOutline(Graphics2D graphics, Rectangle2D rectangle) {
			super.drawOutline(graphics, rectangle);

			if (this.pnl.paintLines && this.pnl.locked) {
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
	 * An interface for listeners that want to receive events from the {@link AlignChartPanel}.
	 * This allows parent GUI components to react to user actions within the charts.
	 */
	public static interface AlignChartPanelListener {

		/**
		 * Called when the user initiates an action to add the currently selected tentative beats
		 * as a new, named {@link BeatSelection}.
		 */
		public void triggerAddSelection();

		/**
		 * Called when the user initiates an action to toggle the lock state of the chart alignment.
		 */
		public void triggerLockToggle();

		/**
		 * Notifies the listener of the current number of tentative beats selected in the top chart.
		 *
		 * @param numberOfBeats The count of selected beats in the top chart.
		 */
		public void setNumBeatsTop(int numberOfBeats);

		/**
		 * Notifies the listener of the current number of tentative beats selected in the bottom chart.
		 *
		 * @param numberOfBeats The count of selected beats in the bottom chart.
		 */
		public void setNumBeatsBottom(int numberOfBeats);

	}

}
