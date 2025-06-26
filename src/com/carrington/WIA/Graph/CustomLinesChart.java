package com.carrington.WIA.Graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.carrington.WIA.Utils;
import com.carrington.WIA.DataStructures.HemoData;
import com.carrington.WIA.IO.Header;

public class CustomLinesChart {
	
	public static final BasicStroke strokeDotted = /*new BasicStroke(1f);*/
			new BasicStroke(
			        1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
			        1.0f, new float[] {3.0f, 10.0f}, 0.0f
			    );
	public static final BasicStroke strokeThickSolid = /*new BasicStroke(1f);*/
			new BasicStroke(2f);
	
	public static final Color GRAPH_COLOR_GRAY = new Color(105, 105, 105, 60);
	public static final Color[] GRAPH_COLOR_STANDARD_SET = new Color[] { Color.GREEN.darker(), Color.BLUE.brighter(),
			Color.ORANGE, Color.CYAN };
	private volatile int standardGraphColorIndex = 0;
	
	private String chartTitle;
	

	private JFreeChart chart = null;
	private Map<Header, SamplingXYLineRenderer> renderers = new HashMap<Header, SamplingXYLineRenderer>();
	private Map<Object, XYBoxAnnotation> annotations = new HashMap<Object, XYBoxAnnotation>();
	
	private Map<String, Set<Marker>> markers = new HashMap<String, Set<Marker>>();
	
	public CustomLinesChart(String chartTitle, HemoData data) {
		
		this(chartTitle, data, null, null, null);

	}
	
	public CustomLinesChart(String chartTitle, HemoData data, LinkedHashMap<Header, Double> downsample, Collection<Header> headersToIgnore) {
		this(chartTitle, data, downsample, null, null);
	}

	
	public CustomLinesChart(String chartTitle, HemoData data, LinkedHashMap<Header, Double> downsample, Map<Header, Color> colors, Collection<Header> headersToIgnore) {
		
		String errors = data.isValid();
		if (errors != null)
			throw new IllegalArgumentException(errors);
		
		this.chartTitle = chartTitle;

		// xHeader will not be null as it already passed the validation above.
		_createDataset(data, downsample, colors, headersToIgnore);

	}

	public JFreeChart getChart() {
		return this.chart;
	}

	private void _createDataset(HemoData data, LinkedHashMap<Header, Double> downSample, Map<Header, Color> colors, Collection<Header> headersToIgnore) {
		
		XYPlot plot = new XYPlot();
		
		double[] xData = data.getXData();
		
		int seriesCounter = 0;
		for (Header yHeader : data.getYHeaders()) {
			
			if (headersToIgnore != null && headersToIgnore.contains(yHeader)) {
				continue;
			}

			double[] yValues = data.getYData(yHeader);
			
			plot.setDataset(seriesCounter, _makeSeriesCollection(data.getXHeader(), xData, yHeader, yValues));
			SamplingXYLineRenderer render = _makeRenderer(yHeader, xData.length);
			
			if (colors != null && colors.containsKey(yHeader)) {
				render.setSeriesPaint(0, colors.get(yHeader));
			} else {
				Color color = GRAPH_COLOR_STANDARD_SET[standardGraphColorIndex];
				if (standardGraphColorIndex < GRAPH_COLOR_STANDARD_SET.length - 1) {
					standardGraphColorIndex++;
				}
				render.setSeriesPaint(0, color);
			}
			

			this.renderers.put(yHeader, render);
			plot.setRenderer(seriesCounter, render);
			NumberAxis axis = _makeYAxis(yHeader);
			if (yHeader.hasAdditionalMeta(Header.META_NO_AXIS)) {
				System.out.println("Invisible");
				axis.setAxisLineVisible(false);
				axis.setVisible(false);
			} else {
				System.out.println("no");

			}
			 
			plot.setRangeAxis(seriesCounter, axis);
			plot.mapDatasetToRangeAxis(seriesCounter, seriesCounter);
			adjustRange(axis, downSample != null ? downSample.get(yHeader) : null);

			seriesCounter++;
		}
		
		Header xHeader = data.getXHeader();
		String xname = xHeader.hasAdditionalMeta(Header.META_DISPLAY_NAME) ? (String) xHeader.getAdditionalMeta(Header.META_DISPLAY_NAME) : xHeader.getName();

		NumberAxis domainAxis = new AutoRangeLimitedValueAxis(xname);
		//NumberAxis domainAxis = new NumberAxis(xHeader.getName());

		domainAxis.setAutoRange(true);
		domainAxis.setAutoRangeIncludesZero(false);

		domainAxis.setLabelFont(Utils.getSubTitleSubFont());
		domainAxis.setTickLabelFont(Utils.getSmallTextFont());
		domainAxis.setTickMarkPaint(Color.BLACK);
		domainAxis.setTickMarkStroke(strokeThickSolid);

		plot.setDomainAxis(domainAxis);
		plot.setDomainGridlinesVisible(true);
		plot.setRangeGridlinesVisible(false);
		//plot.setRenderer(new SamplingXYLineRenderer());

		this.chart = new JFreeChart(this.chartTitle, Utils.getSubTitleSubFont(), plot, true);
		this.chart.getLegend().setPosition(RectangleEdge.BOTTOM);
		
		chart.getXYPlot().setDomainPannable(true);
		chart.getXYPlot().setRangePannable(false);

	}
	
	private void adjustRange(NumberAxis axis, Double sizeFactor) {
		
		if (sizeFactor == null || sizeFactor == 0)  {
			axis.setAutoRange(true);
			axis.setAutoRangeIncludesZero(false);
		} else {
			double upper = axis.getUpperBound();
			double lower = axis.getLowerBound();
			double margin = (upper - lower) * axis.getUpperMargin();

			upper = upper + margin;
			lower = lower - margin;

			double diff = upper - lower;
			if (sizeFactor > 0) {
				axis.setRange(new Range(lower, upper + (diff * sizeFactor)), true, false);

			} else {
				axis.setRange(new Range(upper - (diff * Math.abs(sizeFactor)) , upper), true, false);

			}
		}
		
		
		
	}
	
	private NumberAxis _makeYAxis(Header header) {
		String name = header.hasAdditionalMeta(Header.META_DISPLAY_NAME) ? (String) header.getAdditionalMeta(Header.META_DISPLAY_NAME) : header.getName();
		NumberAxis axis = new NumberAxis(name);
		axis.setLabelFont(Utils.getSubTitleSubFont());
		axis.setTickLabelFont(Utils.getTextFont(false));
		return axis;

	}
	
	private SamplingXYLineRenderer _makeRenderer(Header yHeader, int xDataSize) {
		SamplingXYLineRenderer render = new SamplingXYLineRenderer();
		render.setAutoPopulateSeriesShape(false);
		if (yHeader.hasAdditionalMeta(Header.META_COLOR)) {
			render.setSeriesPaint(0, (Color) yHeader.getAdditionalMeta(Header.META_COLOR), true);
			render.setSeriesFillPaint(0, (Color) yHeader.getAdditionalMeta(Header.META_COLOR), true);

		} else {
			render.setSeriesPaint(0, Color.BLACK, true);
			render.setSeriesFillPaint(0, Color.BLACK, true);

		}
		render.setSeriesVisibleInLegend(0, true, true);
		return render;
	}
	
	private XYSeriesCollection _makeSeriesCollection(Header xHeader, double[] xVals, Header yHeader, double[] yVals) {
		XYSeries newSeries = new XYSeries(yHeader.getName());
		
		for (int i = 0; i < xVals.length; i++) {
			newSeries.add(xVals[i], yVals[i]);
		}
		
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(newSeries);
		return dataset;

	}

	
	public void setSeriesVisible(Header header, boolean visible) {
		
		SamplingXYLineRenderer render = this.renderers.get(header);
		if (render != null) {
			render.setSeriesVisible(0, visible, true);
			render.setSeriesVisibleInLegend(0, true, true);

		}

	}
	
	public void removeGroupOfMarkers(String group) {
		XYPlot plot = getChart().getXYPlot();
		Set<Marker> markers = this.markers.get(group);
		if (markers != null) {
			for (Marker marker : markers) {
				plot.removeDomainMarker(marker);
			}
		}

	}
	
	public void removeAllMarkers() {
		XYPlot plot = getChart().getXYPlot();
		for (Entry<String, Set<Marker>> entry : this.markers.entrySet()) {
			for (Marker marker : entry.getValue()) {
				plot.removeDomainMarker(marker);
			}
		}
	}
	
	/**
	 * Adds a marker with a specific group, at the xValue indicated. If Label is not null, will add a label, to the right or left as indicated. Will color if Color is not null.
	 *
	 * @return the marker that was added
	 */
	public Marker addMarker(String group, double xValue, String label, Boolean right, Color color, BasicStroke stroke) {
		
		if (group == null)
			throw new IllegalArgumentException();
		
		Marker marker = new ValueMarker(xValue);
		if (label != null && !label.isBlank() && right != null) {
			marker.setLabel(label);
			if (right) {
				marker.setLabelOffset(new RectangleInsets(15, -20, 0, 0));
			} else {
				marker.setLabelOffset(new RectangleInsets(15, 20, 0, 0));

			}
		}
		
		if (color != null) {
			marker.setPaint(color);
		} else {
			marker.setPaint(Color.BLACK);
		}
		if (stroke != null) {
			marker.setStroke(stroke);
		}
		
		Set<Marker> groupMarkers = this.markers.get(group);
		if (groupMarkers != null) {
			groupMarkers.add(marker);
		} else {
			groupMarkers = new HashSet<Marker>();
			groupMarkers.add(marker);
			this.markers.put(group, groupMarkers);
		}
		this.chart.getXYPlot().addDomainMarker(marker);
		
		
		return marker;
		
	}
	
	public void addAnnotation(Object obj, double xVal1, double xVal2, Color color) {
		
		Range yRange = chart.getXYPlot().getRangeAxis().getRange();
		XYBoxAnnotation annotation = new XYBoxAnnotation(xVal1, yRange.getLowerBound(), xVal2, yRange.getUpperBound(), null, null, color);
		chart.getXYPlot().getRenderer().addAnnotation(annotation, Layer.BACKGROUND);
		this.annotations.put(obj, annotation);
		
	}
	
	public void removeAnnotation(Object obj) {
		XYBoxAnnotation annotation = this.annotations.get(obj);
		if (annotation != null) {
			chart.getXYPlot().getRenderer().removeAnnotation(annotation);
		}
	}
	
	public void removeAllAnnotations() {
		for (XYBoxAnnotation annotation: this.annotations.values()) {
			chart.getXYPlot().getRenderer().removeAnnotation(annotation);
		}

	}
	


}
