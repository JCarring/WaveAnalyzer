package com.carrington.WIA.Graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.carrington.WIA.Utils;
import com.carrington.WIA.DataStructures.HemoData;
import com.carrington.WIA.IO.Header;

/**
 * Manages the creation and customization of a JFreeChart instance for displaying
 * hemodynamic data. Handles the setup of datasets, renderers,
 * axes, and annotations for a multi line plot.
 */
public class CustomLinesChart {
	
	/** A predefined thick, solid stroke for lines. */
	private static final BasicStroke strokeThickSolid = /*new BasicStroke(1f);*/
			new BasicStroke(2f);
	
	/** A standard set of colors to cycle through for different data series. */
	private static final Color[] GRAPH_COLOR_STANDARD_SET = new Color[] { Color.GREEN.darker(), Color.BLUE.brighter(),
			Color.ORANGE, Color.CYAN };
	private volatile int standardGraphColorIndex = 0;
	
	private String chartTitle;
	

	private JFreeChart chart = null;
	private Map<Header, SamplingXYLineRenderer> renderers = new HashMap<Header, SamplingXYLineRenderer>();
		
	/**
	 * Constructs a CustomLinesChart with a title and data.
	 *
	 * @param chartTitle The title of the chart.
	 * @param data The hemodynamic data to plot.
	 */
	public CustomLinesChart(String chartTitle, HemoData data) {
		
		this(chartTitle, data, null, null, null);

	}
	
	
	/**
	 * Constructs a CustomLinesChart with options for downsampling and ignoring headers.
	 *
	 * @param chartTitle The title of the chart.
	 * @param data The hemodynamic data to plot.
	 * @param downsample A map specifying downsampling factors for specific headers.
	 * @param headersToIgnore A collection of {@link Header} to exclude from the plot.
	 */
	public CustomLinesChart(String chartTitle, HemoData data, LinkedHashMap<Header, Double> downsample, Collection<Header> headersToIgnore) {
		this(chartTitle, data, downsample, null, null);
	}

	
	/**
	 * Constructs a CustomLinesChart with full customization options.
	 *
	 * @param chartTitle The title of the chart.
	 * @param data The hemodynamic data to plot.
	 * @param downsample A map specifying downsampling factors for specific y-axes.
	 * @param colors A map specifying custom colors for specific headers.
	 * @param headersToIgnore A collection of {@link Header} to exclude from the plot.
	 * @throws IllegalArgumentException if the provided HemoData is invalid.
	 */
	public CustomLinesChart(String chartTitle, HemoData data, LinkedHashMap<Header, Double> downsample, Map<Header, Color> colors, Collection<Header> headersToIgnore) {
		
		String errors = data.isValid();
		if (errors != null)
			throw new IllegalArgumentException(errors);
		
		this.chartTitle = chartTitle;

		// xHeader will not be null as it already passed the validation above.
		_createDataset(data, downsample, colors, headersToIgnore);

	}

	/**
	 * Returns the underlying {@link JFreeChart} object.
	 *
	 * @return The {@link JFreeChart} instance.
	 */
	public JFreeChart getChart() {
		return this.chart;
	}

	/**
	 * Initializes the {@link XYPlot} and creates all necessary datasets, renderers, and axes.
	 *
	 * @param data The hemodynamic data to plot.
	 * @param downSample A map specifying downsampling factors for y-axes.
	 * @param colors A map of custom colors for specific data series.
	 * @param headersToIgnore A collection of data series to exclude from the chart.
	 */
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
	
	/**
	 * Adjusts the range of a number axis based on a size factor.
	 *
	 * @param axis The axis to adjust.
	 * @param sizeFactor The factor by which to expand or contract the axis range.
	 * If null or zero, auto-range is enabled.
	 */
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
	
	/**
	 * Helper to create and configure a {@link NumberAxis} for a Y-axis.
	 *
	 * @param header The data {@link Header} associated with this axis.
	 * @return A configured {@link NumberAxis}.
	 */
	private NumberAxis _makeYAxis(Header header) {
		String name = header.hasAdditionalMeta(Header.META_DISPLAY_NAME) ? (String) header.getAdditionalMeta(Header.META_DISPLAY_NAME) : header.getName();
		NumberAxis axis = new NumberAxis(name);
		axis.setLabelFont(Utils.getSubTitleSubFont());
		axis.setTickLabelFont(Utils.getTextFont(false));
		return axis;

	}
	
	/**
	 * Helper that creates and configures a line renderer for a data series.
	 *
	 * @param yHeader The {@link Header} for the Y-data series.
	 * @param xDataSize The size of the dataset, used for potential optimizations.
	 * @return A configured {@link SamplingXYLineRenderer}.
	 */
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
	
	/**
	 * Creates an {@link XYSeriesCollection} from X and Y data arrays.
	 *
	 * @param xHeader The header for the X-axis data.
	 * @param xVals The array of X-values.
	 * @param yHeader The header for the Y-axis data.
	 * @param yVals The array of Y-values.
	 * @return An {@link XYSeriesCollection} containing a single series.
	 */
	private XYSeriesCollection _makeSeriesCollection(Header xHeader, double[] xVals, Header yHeader, double[] yVals) {
		XYSeries newSeries = new XYSeries(yHeader.getName());
		
		for (int i = 0; i < xVals.length; i++) {
			newSeries.add(xVals[i], yVals[i]);
		}
		
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(newSeries);
		return dataset;

	}

	/**
	 * Sets the visibility of a data series on the chart.
	 *
	 * @param header The {@link Header} identifying the data series.
	 * @param visible True to make the series visible, false to hide it.
	 */
	public void setSeriesVisible(Header header, boolean visible) {
		
		SamplingXYLineRenderer render = this.renderers.get(header);
		if (render != null) {
			render.setSeriesVisible(0, visible, true);
			render.setSeriesVisibleInLegend(0, true, true);

		}

	}
	


}
