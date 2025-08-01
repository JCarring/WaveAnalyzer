package com.carrington.WIA.Graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.lang.reflect.Field;
import java.text.AttributedString;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.carrington.WIA.Utils;
import com.carrington.WIA.DataStructures.WIAData;
import com.carrington.WIA.GUIs.KeyActionReceiver;

/**
 * A specialized {@link ChartPanel} designed to display net wave intensity data from a
 * {@link WIAData} object. It encapsulates a {@link NetWaveChart} and provides
 * methods for its creation and manipulation.
 */
public class NetWaveChartPanel extends ChartPanel implements KeyActionReceiver {

	private static final long serialVersionUID = 5158158439696012597L;

	private NetWaveChart customChart;
	
	private final Font fontCustom;

	 
	/**
	 * Factory method to create a {@link NetWaveChartPanel} instance.
	 *
	 * @param wiaData The {@link WIAData} to plot.
	 * @param font The font to use for chart text and labels.
	 * @return A new instance of {@link NetWaveChartPanel}.
	 * @throws IllegalArgumentException if wave data is null.
	 */
	public static NetWaveChartPanel generate(WIAData wiaData, Font font) {
		if (wiaData == null) {
			throw new IllegalArgumentException("WIA data cannot be null.");
		}

		return new NetWaveChartPanel(NetWaveChart.generate(wiaData, font), font);
	}

	/**
	 * Private constructor to create the chart panel.
	 * Instantiation should be done via the static {@link #generate(WIAData, Font)} method.
	 *
	 * @param chart The custom {@link NetWaveChart} to display.
	 * @param font The font to be used in the chart.
	 */
	private NetWaveChartPanel(NetWaveChart chart, Font font) {
		super(chart);

		this.customChart = chart;
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

	}

	/**
	 * Enables the difference renderer, which fills the area between the wave
	 * intensity line and the zero baseline.
	 */
	public void enableDifferencing() {
		customChart.enableDifferencing();
	}

	/**
	 * Disables the difference renderer, showing only the wave intensity line.
	 */
	public void disableDifferencing() {
		customChart.disableDifferencing();
	}

	/**
	 * 
	 * This method updates the internal WIAData reference, generates a new chart
	 * based on the new data, updates the ChartPanel, and repaints the panel.
	 *
	 * @param newData the new WIAData instance to display.
	 */
	public void resetWIAData(WIAData newData) {
		if (newData == null) {
			throw new IllegalArgumentException("New WIAData file cannot be null.");
		}

		// Generate a new chart using the new data.
		JFreeChart newChart = NetWaveChart.generate(newData, fontCustom);
		// Update the ChartPanel and the custom chart reference.
		setChart(newChart);
		this.customChart = (NetWaveChart) newChart;

		// Repaint the panel to reflect the new data.
		repaint();
	}
	
	@Override
	public void keyPressed(int key) {
		// Does not handle any key events
	}

	/**
	 * A custom {@link JFreeChart} class specifically for displaying Net Wave Intensity.
	 * It handles the creation of the plot, renderers, and axes, and allows
	 * for toggling between a standard line renderer and a difference renderer.
	 */
	public static class NetWaveChart extends JFreeChart {

		private static final long serialVersionUID = -1372038490696834353L;
		private static final Color noneColor = new Color(0, 0, 0, 0);
		private static final Color solidGrayColor = new Color(115, 115, 115);

		private XYDifferenceRenderer rendererDiff = null;
		private XYLineAndShapeRenderer rendererNormal = null;

		/**
		 * Constructor for {@link NetWaveChart}.
		 * Instantiation should be done via the static {@link #generate(WIAData, Font)} method.
		 *
		 * @param plot The fully configured {@link XYPlot} for the chart.
		 * @param font The base font for chart elements.
		 */
		private static NetWaveChart generate(WIAData data, Font font) {
			return new NetWaveChart(_createPlot(font, data), font);

		}

		
		private NetWaveChart(Plot plot, Font font) {
			super("Net Wave Intensity", new Font(font.getFamily(), Font.BOLD, (int) (font.getSize() * 1.2)), plot,
					true);
			removeLegend();
			setBorderVisible(false);
			setBorderPaint(new Color(0, 0, 0, 0));
			getXYPlot().setDomainPannable(true);
			getXYPlot().setRangePannable(false);

			this.rendererDiff = (XYDifferenceRenderer) getXYPlot().getRenderer(0);

			rendererNormal = new XYLineAndShapeRenderer();

			rendererNormal.setSeriesPaint(0, solidGrayColor);
			rendererNormal.setSeriesStroke(0, rendererDiff.getSeriesStroke(0), false);
			rendererNormal.setSeriesPaint(1, noneColor);
			rendererNormal.setSeriesVisible(1, false);
			rendererNormal.setAutoPopulateSeriesShape(false);
			rendererNormal.setSeriesShapesVisible(0, false);
			rendererNormal.setSeriesShapesVisible(1, false);

		}

		/**
		 * Disables the difference renderer and applies a normal line renderer.
		 */
		public void disableDifferencing() {
			getXYPlot().setRenderer(0, rendererNormal, true);

		}

		/**
		 * Enables the difference renderer to show filled areas.
		 */
		public void enableDifferencing() {
			getXYPlot().setRenderer(0, rendererDiff, true);
		}


		/**
		 * Returns this chart instance.
		 *
		 * @return the current {@link NetWaveChart} object.
		 */
		public NetWaveChart getChart() {
			return this;
		}

		/**
		 * Creates and configures the {@link XYPlot} for the net wave intensity chart.
		 *
		 * @param textFont The font to use for labels and titles.
		 * @param wiaData The data to be plotted.
		 * @return A fully configured {@link XYPlot}.
		 */
		private static XYPlot _createPlot(Font textFont, WIAData wiaData) {

			XYPlot plot = new XYPlot();

			int textFontSize = textFont.getSize();
			float tickStroke = Math.max(textFontSize / 8.0f, 1.5f);

			double[] time = wiaData.getTime();
			double[] flowDeriv = wiaData.getFlowDeriv();
			Object[] scaled = Utils.scaleToScientific(wiaData.getNetWaveIntensity());
			double[] waveNet = (double[]) scaled[0];
			int numScientific = (int) scaled[1];

			// set domain
			NumberAxis domainAxis = new AutoRangeLimitedValueAxis("Time (ms)");
			domainAxis.setAutoRange(true);
			domainAxis.setAutoRangeIncludesZero(false);

			domainAxis.setLabelFont(textFont);
			domainAxis.setTickLabelFont(textFont);
			domainAxis.setAutoTickUnitSelection(false);
			domainAxis.setTickUnit(new NumberTickUnit(Utils.findOptimalTickInterval(time[0], time[time.length - 1], false)));
			domainAxis.setTickMarkStroke(new BasicStroke(tickStroke));
			domainAxis.setTickMarkInsideLength(textFontSize / 2);
			domainAxis.setTickMarkPaint(Color.BLACK);
			domainAxis.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
			domainAxis.setAxisLineStroke(new BasicStroke(tickStroke));
			domainAxis.setAxisLinePaint(Color.BLACK);

			plot.setDomainAxis(domainAxis);

			XYSeries netSeries = new XYSeries("Net Wave");
			XYSeries netSeriesFill = new XYSeries("Net Wave Fill");

			for (int i = 0; i < waveNet.length; i++) {
				netSeries.add(time[i], waveNet[i]);
			}

			for (int i = 0; i < flowDeriv.length; i++) {
				if (flowDeriv[i] > 0) {

					if (i > 0 && flowDeriv[i - 1] <= 0) {
						netSeriesFill.add(time[i] - 0.0001, waveNet[i]);

					}
					netSeriesFill.add(time[i], 0);

				} else {
					if (i > 0 && flowDeriv[i - 1] > 0) {
						netSeriesFill.add(time[i - 1] + 0.001, waveNet[i - 1]);

					}
					netSeriesFill.add(time[i], waveNet[i]);

				}
			}

			XYSeriesCollection dataset = new XYSeriesCollection();
			dataset.addSeries(netSeries);
			dataset.addSeries(netSeriesFill);

			plot.setDataset(0, dataset);

			XYDifferenceRenderer renderer1 = new XYDifferenceRenderer(solidGrayColor, solidGrayColor, false);
			renderer1.setSeriesPaint(0, solidGrayColor);
			renderer1.setSeriesStroke(0, new BasicStroke(tickStroke), false);

			renderer1.setSeriesPaint(1, noneColor);
			renderer1.setSeriesVisible(1, false);

			plot.setRenderer(0, renderer1);
			plot.setOutlineVisible(false);

			NumberAxis rangeAxis = new NumberAxis("Wave Intensity");
			rangeAxis.setLabelFont(textFont);
			rangeAxis.setAutoRange(true);
			rangeAxis.setAutoRangeIncludesZero(true);

			String s = "Net Wave Intensity (W m-2 s-2)";
			if (numScientific > 0) {
				s = s + "   x10" + numScientific;
			}
			AttributedString as = new AttributedString(s);
			as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, 23, 25);
			as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, 27, 29);
			if (numScientific > 0) {
				as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, 36, 37);
				as.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_EXTRABOLD, 33, 37);
				as.addAttribute(TextAttribute.FOREGROUND, Color.GRAY, 33, 37);
			}

			as.addAttribute(TextAttribute.SIZE, textFont.getSize());
			as.addAttribute(TextAttribute.FAMILY, textFont.getFamily());

			rangeAxis.setAttributedLabel(as);
			rangeAxis.setLabel(s);
			rangeAxis.setTickLabelFont(textFont);
			rangeAxis.setAutoTickUnitSelection(false);
			double min = Utils.min(waveNet);
			double max = Utils.max(waveNet);
			rangeAxis.setTickUnit(new NumberTickUnit(Utils.findOptimalTickInterval(min, max, false)));
			rangeAxis.setTickMarkStroke(new BasicStroke(tickStroke));
			rangeAxis.setTickMarkInsideLength(textFontSize / 2);
			rangeAxis.setTickMarkPaint(Color.BLACK);
			rangeAxis.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
			rangeAxis.setAxisLineStroke(new BasicStroke(tickStroke));
			rangeAxis.setAxisLinePaint(Color.BLACK);

			ValueMarker marker = new ValueMarker(0); // position is the value on the axis
			marker.setPaint(Color.black);
			marker.setStroke(new BasicStroke(1.5f));

			plot.setRangeAxis(rangeAxis);
			plot.mapDatasetToRangeAxis(0, 0);
			plot.setDomainGridlinesVisible(false);
			plot.setRangeGridlinesVisible(false);
			plot.addRangeMarker(marker);
			plot.setDomainCrosshairPaint(Color.BLACK);

			return plot;

		}

	}

	

}
