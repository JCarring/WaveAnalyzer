package com.carrington.WIA.Graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.AttributedString;

import org.apache.batik.apps.rasterizer.DestinationType;
import org.apache.batik.apps.rasterizer.SVGConverter;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.carrington.WIA.Utils;

public class ComboChartSaver {
	
	private static final Color noneColor = new Color(0, 0, 0, 0);
	private static final Color solidGrayColor = new Color(115, 115, 115);
	
	private final File fileSVG;
	private final File fileTIFF;

	private final Font font;
	private final int fontSize;
	private final float tickStroke;
	private final BasicStroke strokeDotted;
			
	private final BasicStroke strokeThickSolid;
	private final NumberAxis sharedDomain;
	private final double[] time;
	
	public static void main(String[] args) throws Exception {
		
		double[] time = new double[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 };
		double[] pressure = new double[] { 60, 65, 75, 88, 105, 115, 120, 125, 124, 118, 113, 108, 104, 98, 90, 84, 76, 68, 64, 60 };
		double[] flow = new double[] { 0.05, 0.08, 0.1, 0.14, 0.17, 0.2, 0.23, 0.26, 0.28, 0.26, 0.23, 0.2, 0.17, 0.14, 0.1, 0.08,
				0.09, 0.07, 0.05, 0.04 };
		double[] waveForwardLarge = new double[] { 0, 15, 30, 35, 30, 15, 5, 0, 0, 5, 10, 15, 20, 15, 10, 5, 0, 0, 0, 0 };
		double[] waveBackwardLarge = new double[] { 0, 0, -20, -40, -80, -85, -75, -40, -20, -10, -10, -10, -5, -10, -20, -10,
				-10, -5, 0, 0 };
		double[] waveForwardAccel = new double[] {1, 1, 1, 1, 1, -1, -1, -1, -1, -1, 1, 1, 1, 1, 1, -1, -1, -1, -1, -1};


		double[] waveBackwardAccel = new double[] { -1, -1, -1, -1, 1, 1, -1, -1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
				1 };
		for (int i = 0; i < time.length; i++) {
			time[i] = BigDecimal.valueOf(time[i]).multiply(BigDecimal.valueOf(50)).doubleValue();
		}
		for (int i = 0; i < waveForwardLarge.length; i++) {
			waveForwardLarge[i] = BigDecimal.valueOf(waveForwardLarge[i]).multiply(BigDecimal.valueOf(10000)).doubleValue();
			waveBackwardLarge[i] = BigDecimal.valueOf(waveBackwardLarge[i]).multiply(BigDecimal.valueOf(10000)).doubleValue();

		}
		
		Font font = new Font("Aptos", Font.PLAIN, 12);
		
		ComboChartSaver saver = new ComboChartSaver(new File("testingss.svg"), new File("Testingstif.tiff"), font, time);
		saver.saveSepWavePressFlow("test", 400, 800, pressure, false, flow, waveForwardLarge, waveBackwardLarge, waveForwardAccel, waveBackwardAccel, new double[] {2, 1});
	}
	
	
	public ComboChartSaver(File fileSVG, File fileTIFF, Font font, double[] time) {
		if (fileSVG == null || fileTIFF == null || font == null || time == null) {
			throw new IllegalArgumentException("Invalid chart save options");
		}
		this.fileSVG = fileSVG;
		this.fileTIFF = fileTIFF;
		this.font = font;
		fontSize = font.getSize();
		tickStroke = Math.max(fontSize / 8.0f, 1.5f);
		strokeDotted = new BasicStroke(
				tickStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
		        1.0f, new float[] {6f, 6f}, 0.0f
		    );
		strokeThickSolid = new BasicStroke(tickStroke);
		this.time = time;
		

		sharedDomain = new AutoRangeLimitedValueAxis("Time (ms)");
		sharedDomain.setAutoRange(true);
		sharedDomain.setAutoRangeIncludesZero(false);

		sharedDomain.setLabelFont(font);
		sharedDomain.setTickLabelFont(font);
		sharedDomain.setAutoTickUnitSelection(false);
		sharedDomain.setTickUnit(new NumberTickUnit(Utils.findOptimalTickInterval(time[0], time[time.length - 1], false)));
		sharedDomain.setTickMarkStroke(new BasicStroke(tickStroke));
		sharedDomain.setTickMarkInsideLength(fontSize / 2);
		sharedDomain.setTickMarksVisible(true);
		sharedDomain.setTickMarkPaint(Color.BLACK);
		sharedDomain.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
		sharedDomain.setAxisLineStroke(new BasicStroke(tickStroke));
		sharedDomain.setAxisLinePaint(Color.BLACK);
		sharedDomain.setRange(new Range(time[0], time[time.length - 1]));

		
	}
	
	public static void saveSepWavePressFlowMultiple() {
		
	}
	
	public void saveSepWavePressFlow(String name, int width, int height, double[] pressure, boolean pascals, double[] flow, 
			double[] waveForward, double[] waveBackward, double[] forwardAccel, double[] backwardAccel, double[] weights) throws Exception {
		

		Object[] scaled = Utils.scaleToScientific(waveForward, waveBackward);
		double[] waveForwardSmall = (double[]) scaled[0];
		double[] waveBackwardSmall = (double[]) scaled[1];
		int numScientific = (int) scaled[2];
		
		double maxRange = Utils.max(waveForwardSmall);
		double minRange = Utils.min(waveBackwardSmall);
		System.out.println(minRange);
		System.out.println(maxRange);
		System.out.println(Utils.findOptimalTickInterval(minRange, maxRange, false));

		NumberTickUnit tickUnit = new NumberTickUnit(Utils.findOptimalTickInterval(minRange, maxRange, false));
		
		XYPlot plotPF = createPFPlot(pressure, pascals, flow);
		XYPlot plotFor = createSepWavePlot(waveForwardSmall, forwardAccel, numScientific, tickUnit);
		XYPlot plotBack = createSepWavePlot(waveBackwardSmall, backwardAccel, numScientific, tickUnit);
		
		double rangeNeg = Math.abs(plotBack.getRangeAxis().getLowerBound()) * 1.05;
		double rangePos = Math.abs(plotFor.getRangeAxis().getUpperBound()) * 1.05;

		double scaleNeg = weights[0] * 500d;
		double scalePos = weights[0] * 500d;
		double scalePF = weights[1] * 500d;
		if (rangeNeg > rangePos) {
			
			// pos = 80
			// neg = 40
			scalePos = scalePos * (rangePos / rangeNeg);
		} else {
			scaleNeg = scaleNeg * (rangeNeg / rangePos);

		}
		
		CombinedDomainXYPlot comboplot = new CombinedDomainXYPlot(sharedDomain);
		comboplot.setDomainAxis(sharedDomain);
		
		comboplot.add(plotFor, (int) scalePos);
		comboplot.add(plotBack, (int) scaleNeg);
		
		comboplot.add(plotPF, (int) scalePF);

		comboplot.setDomainAxis(sharedDomain);
		comboplot.mapDatasetToDomainAxis(0, 0); // TODO is this needed
		comboplot.mapDatasetToDomainAxis(1, 0);
		comboplot.mapDatasetToDomainAxis(2, 0);
		comboplot.mapDatasetToDomainAxis(3, 0);
		comboplot.mapDatasetToDomainAxis(4, 0);
		comboplot.mapDatasetToDomainAxis(5, 0);


		comboplot.setGap(Utils.getFontParams(font, null)[0] * 2);

		comboplot.setDomainPannable(true);
		comboplot.setRangePannable(false);
		comboplot.setDomainTickBandPaint(Color.BLACK);
		
		String title = null;
		if (name != null) {
			title = "Sep WIA " + name;
		} else {
			title = "Separated Wave Intensity";
		}
		JFreeChart jch = new JFreeChart(title, comboplot);

		jch.setBorderVisible(false);
		jch.setBorderPaint(new Color(0,0,0,0));
		jch.setBackgroundPaint(Color.WHITE);
		
		ChartPanel cp = new ChartPanel(jch);
		
		// to ensure no distortion with scaling
		cp.setMaximumDrawHeight(5000);
		cp.setMaximumDrawWidth(5000);
		cp.setMinimumDrawHeight(80);
		cp.setMinimumDrawWidth(80);
		
//		JDialog frame = new JDialog();
//		frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
//		frame.setContentPane(cp);
//		frame.setBounds(100, 100, 800, 600);
//		frame.setVisible(true);
//		frame.pack();
		
		saveAsSVG(jch, fileSVG, width, height);
		
		SVGConverter convert = new SVGConverter();
		convert.setSources(new String[] {fileSVG.getPath()});
		convert.setDst(fileTIFF);
		//convert.setHeight(400);
		//convert.setWidth(400);
		convert.setDestinationType(DestinationType.TIFF);
		convert.execute();
		convert.setPixelUnitToMillimeter(25.4f / 72f);
	}
	
	

	public XYPlot createSepWavePlot(double[] waveSepSmall, double[] waveAccel, int numScientific,
			NumberTickUnit sharedTickUnit) {

		XYPlot plotSepWave = new XYPlot();
		
		XYSeries waveSeries = new XYSeries("Sep Wave");
		XYSeries waveFillSeries = new XYSeries("Sep Wave Fill");


		for (int i = 0; i < waveSepSmall.length; i++) {
			waveSeries.add(time[i], waveSepSmall[i]);
		}

		for (int i = 0; i < waveAccel.length; i++) {
			if (waveAccel[i] > 0) {
				if (i > 0 && waveAccel[i - 1] <= 0) {
					waveFillSeries.add(time[i] - 0.0001,  waveSepSmall[i]);
				}
				waveFillSeries.add(time[i], 0);

			} else {
				if (i > 0 && waveAccel[i - 1] > 0) {
					waveFillSeries.add(time[i-1] + 0.001, waveSepSmall[i-1]);
				}
				waveFillSeries.add(time[i], waveSepSmall[i]);

			}
		}		
		
		// dataset
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(waveSeries);
		dataset.addSeries(waveFillSeries);
		plotSepWave.setDataset(0, dataset);
		
		// renderer
		XYDifferenceRenderer renderer = new XYDifferenceRenderer(solidGrayColor, solidGrayColor, false);
		renderer.setSeriesPaint(0, solidGrayColor);
		renderer.setSeriesStroke(0, strokeThickSolid, false);
		renderer.setSeriesVisibleInLegend(0, false);
		renderer.setSeriesPaint(1, noneColor);
		renderer.setSeriesVisible(1, false);
		plotSepWave.setRenderer(0, renderer);
		
		// range axis
		NumberAxis rangeAxisSep = new NumberAxis("Wave Intensity");
		rangeAxisSep.setLabelFont(font);
		rangeAxisSep.setAutoRange(true);
		rangeAxisSep.setAutoRangeIncludesZero(true);
		String s = "Wave Intensity (W m-2 s-2)";
		if (numScientific > 0) {
			s = s + "   x10" + numScientific;
		}
		AttributedString ast = new AttributedString(s);
		ast.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, 19, 21);
		ast.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, 23, 25);
		if (numScientific > 0) {
			ast.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, 32, 33);
			ast.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_EXTRABOLD, 29, 33);
			ast.addAttribute(TextAttribute.FOREGROUND, Color.GRAY, 29, 33);

		}
		ast.addAttribute(TextAttribute.SIZE, font.getSize());
		ast.addAttribute(TextAttribute.FAMILY, font.getFamily());
	
		rangeAxisSep.setAttributedLabel(ast);
		rangeAxisSep.setTickLabelFont(font);
		rangeAxisSep.setAutoTickUnitSelection(false);
		rangeAxisSep.setTickUnit(sharedTickUnit);
		rangeAxisSep.setTickMarkStroke(strokeThickSolid);
		rangeAxisSep.setTickMarkInsideLength(fontSize / 2);
		rangeAxisSep.setTickMarkPaint(Color.BLACK);
		rangeAxisSep.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
		rangeAxisSep.setAxisLineStroke(strokeThickSolid);
		rangeAxisSep.setAxisLinePaint(Color.BLACK);
		rangeAxisSep.setAutoRangeIncludesZero(true);
		rangeAxisSep.setAutoRangeStickyZero(true);

		ValueMarker marker = new ValueMarker(0);  // position is the value on the axis
		marker.setPaint(Color.black);
		marker.setStroke(new BasicStroke(5f));
		plotSepWave.addRangeMarker(marker);

		
		plotSepWave.setRangeAxis(rangeAxisSep);
		plotSepWave.mapDatasetToRangeAxis(0, 0);
		plotSepWave.mapDatasetToRangeAxis(1, 0); // try to map bother datasets to the same range
		
		
		// housekeeping
		plotSepWave.setOutlineVisible(false);
		plotSepWave.setDomainGridlinesVisible(false);
		plotSepWave.setRangeGridlinesVisible(false);
		plotSepWave.setRangeZeroBaselineVisible(true);
		plotSepWave.setRangeZeroBaselinePaint(new Color(0, 0,0, 255));
		plotSepWave.setRangeZeroBaselineStroke(new BasicStroke(tickStroke * 2));
		plotSepWave.setForegroundAlpha(1);
		plotSepWave.setBackgroundAlpha(0);
		
		
		return plotSepWave;
		
	}
	
	
	public XYPlot createPFPlot(double[] pressure, boolean pascals, double[] flow) {

		XYPlot plotPF = new XYPlot();

		if (pascals) {
			pressure = Utils.convertPascalsToMMHG(pressure);
		}
		XYSeries pressureSeries = new XYSeries("Pressure");
		XYSeries flowSeries = new XYSeries("Velocity");
		
		

		for (int i = 0; i < time.length; i++) {
			pressureSeries.add(time[i], pressure[i]);
			flowSeries.add(time[i], flow[i]);
		}
		
		XYSeriesCollection datasetPressure = new XYSeriesCollection();
		XYSeriesCollection datasetFlow = new XYSeriesCollection();
		datasetPressure.addSeries(pressureSeries);
		datasetFlow.addSeries(flowSeries);


		plotPF.setDataset(0, datasetPressure);
		plotPF.setDataset(1, datasetFlow);
		XYLineAndShapeRenderer rendererPressure = new XYLineAndShapeRenderer();
		XYLineAndShapeRenderer rendererFlow = new XYLineAndShapeRenderer();


		// pressure
		rendererPressure.setSeriesPaint(0, Color.BLUE);
		rendererPressure.setSeriesStroke(0, strokeThickSolid, false);
		rendererPressure.setAutoPopulateSeriesShape(false);
		rendererPressure.setSeriesShapesVisible(0, false);

		//flow
		rendererFlow.setSeriesPaint(0, Color.RED);
		rendererFlow.setSeriesStroke(0, strokeDotted, false);
		rendererFlow.setAutoPopulateSeriesShape(false);
		rendererFlow.setSeriesShapesVisible(0, false);
		rendererFlow.setDrawSeriesLineAsPath(true);

		plotPF.setRenderer(0, rendererPressure);
		plotPF.setRenderer(1, rendererFlow);

		plotPF.setOutlineVisible(false);

		NumberAxis rangeAxisPressure = new NumberAxis("BP (mm Hg)\nTest");
		rangeAxisPressure.setLabelFont(font);
		rangeAxisPressure.setAutoRange(true);
		rangeAxisPressure.setAutoRangeIncludesZero(false);
		rangeAxisPressure.setAutoTickUnitSelection(true);
		rangeAxisPressure.setTickMarkStroke(strokeThickSolid);
		rangeAxisPressure.setTickMarkInsideLength(fontSize / 2);
		
		NumberAxis rangeAxisFlow = new NumberAxis("Velocity (m s-1)");
		rangeAxisFlow.setLabelFont(font);
		rangeAxisFlow.setAutoRange(true);
		rangeAxisFlow.setAutoRangeIncludesZero(false);
		rangeAxisFlow.setAutoTickUnitSelection(true);
		rangeAxisFlow.setTickMarkStroke(strokeThickSolid);
		rangeAxisFlow.setTickMarkInsideLength(fontSize / 2);

		AttributedString as = new AttributedString("Velocity (m s-1)");
		as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, 13, 15);
		as.addAttribute(TextAttribute.SIZE, font.getSize());
		as.addAttribute(TextAttribute.FAMILY, font.getFamily());
		rangeAxisFlow.setAttributedLabel(as);

		rangeAxisPressure.setTickLabelFont(font);
		rangeAxisFlow.setTickLabelFont(font);
		rangeAxisPressure.setTickMarkPaint(Color.BLACK);
		rangeAxisFlow.setTickMarkPaint(Color.BLACK);
		rangeAxisPressure.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
		rangeAxisFlow.setTickMarkOutsideLength(Math.round(-1.0f * (tickStroke / 2.0f)));
		rangeAxisPressure.setAxisLineStroke(strokeThickSolid);
		rangeAxisFlow.setAxisLineStroke(strokeThickSolid);
		rangeAxisPressure.setAxisLinePaint(Color.BLACK);
		rangeAxisFlow.setAxisLinePaint(Color.BLACK);
		

		plotPF.setRangeAxis(0, rangeAxisPressure);
		plotPF.setRangeAxis(1, rangeAxisFlow);
		plotPF.mapDatasetToRangeAxis(0, 0);
		plotPF.mapDatasetToRangeAxis(1, 1);
		plotPF.setDomainGridlinesVisible(false);
		plotPF.setRangeGridlinesVisible(false);
		plotPF.setForegroundAlpha(1);
		plotPF.setBackgroundAlpha(0);
		
		return plotPF;
		
	}
	



	/**
	 * Generates an SVG string, which can later be saved to filed by using the {@link #saveSVGString(String, File)} method
	 * 
	 * @param chart  The chart to save
	 * @param width	 Width
	 * @param height Height
	 * @return SVG string
	 * @throws Exception if there was an error (numerous possibilities)
	 */
	public static String getSaveAsSVGString(JFreeChart chart, int width, int height) throws Exception {
		return generateSVG(chart, width, height);
	}
	
	/**
	 * Saves an SVG string
	 * 
	 * @param svgString the string value returned by {@link #getSaveAsSVGString(JFreeChart, int, int)}
	 * @param file	the {@link File} to save to
	 * @throws IOException if there was an error in saving to file.
	 */
	public static void saveSVGString(String svgString, File file) throws IOException{
		BufferedWriter writer = null;
        IOException originalException = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n");
            writer.write(svgString + "\n");
            writer.flush();
        } catch (IOException e) {
            originalException = e;
        } finally {
        	if (writer != null) {
        		writer.close();
        	}
        	writer.close();
        }
        if (originalException != null) {
        	throw originalException;
        }
	}
	
	public static void saveAsSVG(JFreeChart chart, File file, int width, int height) throws Exception {
            // use reflection to get the SVG string
            String svg = generateSVG(chart, width, height);
            BufferedWriter writer = null;
            Exception originalException = null;
            try {
                writer = new BufferedWriter(new FileWriter(file));
                writer.write("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n");
                writer.write(svg + "\n");
                writer.flush();
            } catch (Exception e) {
                originalException = e;
            } finally {
            	if (writer != null) {
            		writer.close();
            	}
            	writer.close();
            }
            if (originalException != null) {
            	throw originalException;
            }
    }
	
	private static String generateSVG(JFreeChart chart, int width, int height) throws Exception {
        Graphics2D g2 = createSVGGraphics2D(width, height);
        if (g2 == null) {
            throw new IllegalStateException("JFreeSVG library is not present.");
        }
        // we suppress shadow generation, because SVG is a vector format and
        // the shadow effect is applied via bitmap effects...
        g2.setRenderingHint(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, true);
        String svg = null;
        Rectangle2D drawArea = new Rectangle2D.Double(0, 0, width, height);
        chart.draw(g2, drawArea);
        Method m = g2.getClass().getMethod("getSVGElement");
        svg = (String) m.invoke(g2);
        return svg;
    }
	
	private static Graphics2D createSVGGraphics2D(int width, int height) throws ClassNotFoundException, NoSuchMethodException, SecurityException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?> svgGraphics2d = Class.forName("org.jfree.graphics2d.svg.SVGGraphics2D");
		Constructor<?> ctor = svgGraphics2d.getConstructor(int.class, int.class);
		return (Graphics2D) ctor.newInstance(width, height);

	}

}
