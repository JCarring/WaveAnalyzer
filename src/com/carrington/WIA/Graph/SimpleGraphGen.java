package com.carrington.WIA.Graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.swing.JDialog;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.carrington.WIA.Utils;
import com.carrington.WIA.IO.Header;

public class SimpleGraphGen {
	
	
	/**
	 * Generates a simple graph
	 */
	public static void generateGraph(String title, Header xHeader, double[] xVals, LinkedHashMap<Header, double[]> yAxes, Integer spline) {
		generateGraph(title, xHeader, xVals, yAxes, spline, false);
	}
	
	public static void generateGraph(String title, Header xHeader, double[] xVals, LinkedHashMap<Header, double[]> yAxes, Integer spline, boolean modal) {
		
	    XYPlot plot = new XYPlot();
		
		int counter = 0;
		for (Entry<Header, double[]> yAxis : yAxes.entrySet() ) {
			XYSeries series = new XYSeries(yAxis.getKey().getName());
			
			for (int i = 0; i < yAxis.getValue().length; i++) {
				series.add(xVals[i], yAxis.getValue()[i]);
			}
			
			XYSeriesCollection dataset = new XYSeriesCollection();
			dataset.addSeries(series);
			plot.setDataset(counter, dataset);
			XYItemRenderer render = null;
			if (spline == null) {
				render = new SamplingXYLineRenderer();
			} else {
				render = new XYSplineRenderer(spline);
				
			}
			render.setSeriesStroke(0, new BasicStroke(3f), false);
			plot.setRenderer(counter, render);

			if (yAxis.getKey().hasAdditionalMeta(Header.META_COLOR)) {
				render.setSeriesPaint(0, (Color) yAxis.getKey().getAdditionalMeta(Header.META_COLOR));
			}
			
		    plot.setRangeAxis(counter, new NumberAxis(yAxis.getKey().getName()));
		    plot.mapDatasetToRangeAxis(counter, counter);


			counter++;
		}
		
		NumberAxis domainAxis = new AutoRangeLimitedValueAxis(xHeader.getName());

		domainAxis.setAutoRange(true);
		domainAxis.setAutoRangeIncludesZero(false);

		domainAxis.setLabelFont(Utils.getSubTitleFont());
		domainAxis.setTickLabelFont(Utils.getTextFont(false));

		plot.setDomainAxis(domainAxis);
		
	    JFreeChart chart = new JFreeChart(title, null, plot, true);
	    
	    chart.getXYPlot().setDomainPannable(true);
	    chart.getXYPlot().setRangePannable(false);
	    
	    chart.setBackgroundPaint(Color.WHITE);
	    ChartPanel chartPanel = new ChartPanel(chart);
	    try {
	        Field mask = ChartPanel.class.getDeclaredField("panMask");
	        mask.setAccessible(true);
	        mask.set(chartPanel, 0);
	    } catch (NoSuchFieldException e) {
	        e.printStackTrace();
	    } catch (IllegalAccessException e) {
	        e.printStackTrace();
	    }
	    
	    chartPanel.setMouseZoomable(true);
	    chartPanel.setMouseWheelEnabled(true);
	    chartPanel.setDomainZoomable(true);
	    chartPanel.setRangeZoomable(false);
	    ///setPreferredSize(new Dimension(1680, 1100));
	    chartPanel.setZoomTriggerDistance(Integer.MAX_VALUE);
	    chartPanel.setFillZoomRectangle(false);
	    chartPanel.setZoomOutlinePaint(new Color(0f, 0f, 0f, 0f));
	    chartPanel.setZoomAroundAnchor(true);
		JDialog frame = new JDialog();
		if (modal) {
			frame.setModal(true);
			//frame.setAlwaysOnTop(true);
		}
		frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		frame.setContentPane(chartPanel);
		frame.setVisible(true);
		frame.pack();
		
	}
	
	public static void generateGraphOneDataset(String title, Header yHeader, Header xHeader, double[] xVals, LinkedHashMap<Header, double[]> yAxes, Integer spline) {
		
	    XYPlot plot = new XYPlot();
		XYSeriesCollection dataset = new XYSeriesCollection();
		XYLineAndShapeRenderer render = spline != null ? new XYSplineRenderer(spline) : new XYLineAndShapeRenderer();
		render.setAutoPopulateSeriesShape(false);
		int counter = 0;
		for (Entry<Header, double[]> yAxis : yAxes.entrySet() ) {
			XYSeries series = new XYSeries(yAxis.getKey().getName());
			
			for (int i = 0; i < yAxis.getValue().length; i++) {
				series.add(xVals[i], yAxis.getValue()[i]);
			}
			
			dataset.addSeries(series);
			render.setSeriesShapesVisible(counter, false);
			render.setSeriesStroke(counter, new BasicStroke(3f), false);
			if (yAxis.getKey().hasAdditionalMeta(Header.META_COLOR)) {
				render.setSeriesPaint(counter, (Color) yAxis.getKey().getAdditionalMeta(Header.META_COLOR));

			}

			counter++;

		}


		plot.setRenderer(render);
		
		plot.setDataset(dataset);
		plot.setRangeAxis(new NumberAxis(yHeader.getName()));
		plot.mapDatasetToRangeAxis(0, 0);
		
		NumberAxis domainAxis = new AutoRangeLimitedValueAxis(xHeader.getName());

		domainAxis.setAutoRange(true);
		domainAxis.setAutoRangeIncludesZero(false);

		domainAxis.setLabelFont(Utils.getSubTitleFont());
		domainAxis.setTickLabelFont(Utils.getTextFont(false));

		plot.setDomainAxis(domainAxis);
		plot.setDomainPannable(true);
		plot.setRangePannable(false);
		
	    JFreeChart chart = new JFreeChart(title, null, plot, true);
	    chart.getXYPlot().setDomainPannable(true);
	    chart.getXYPlot().setRangePannable(false);
	    
	    chart.setBackgroundPaint(Color.WHITE);
	    ChartPanel chartPanel = new ChartPanel(chart);
	    chartPanel.setPreferredSize(new Dimension(1000, 800));
	    chartPanel.setMouseZoomable(true);
	    chartPanel.setMouseWheelEnabled(true);
	    chartPanel.setDomainZoomable(true);
	    chartPanel.setRangeZoomable(false);
	    ///setPreferredSize(new Dimension(1680, 1100));
	    chartPanel.setZoomTriggerDistance(Integer.MAX_VALUE);
	    chartPanel.setFillZoomRectangle(false);
	    chartPanel.setZoomOutlinePaint(new Color(0f, 0f, 0f, 0f));
	    chartPanel.setZoomAroundAnchor(true);
	    
		JDialog frame = new JDialog();
		frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		frame.setContentPane(chartPanel);
		frame.setPreferredSize(new Dimension(1000, 800));
		frame.setVisible(true);
		frame.pack();
		
	}
	
}
