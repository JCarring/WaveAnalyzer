package com.carrington.WIA.Graph;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * A specialized ChartPanel for displaying a {@link CustomLinesChart}.
 * It provides enhanced interactivity, including mouse zooming, key-based zooming,
 * and clicking on legend items to toggle series visibility.
 */
public class CustomLinesChartPanel extends ChartPanel {
	
	private static final long serialVersionUID = -1811610213414423374L;
	
	private final CustomLinesChart chartChart;

	/**
	 * Constructs a new panel to display the given {@link CustomLinesChart}.
	 *
	 * @param chart The {@link CustomLinesChart} to be displayed in this panel.
	 */
	public CustomLinesChartPanel(CustomLinesChart chart) {
		super(chart.getChart());
		this.chartChart = chart;
		setMouseZoomable(true);
	    setMouseWheelEnabled(true);
	    setDomainZoomable(true);
	    setRangeZoomable(false);
	    setFillZoomRectangle(false);
	    setZoomOutlinePaint(new Color(0f, 0f, 0f, 0f));
	    setZoomAroundAnchor(true);
	    this.setMaximumDrawHeight(5000);
	    this.setMaximumDrawWidth(5000);
	    this.setMinimumDrawHeight(100);
	    this.setMinimumDrawWidth(100);
	    setHorizontalAxisTrace(true);
	    
	    try {
	        Field mask = ChartPanel.class.getDeclaredField("panMask");
	        mask.setAccessible(true);
	        mask.set(this, 0);
	    } catch (NoSuchFieldException e) {
	        e.printStackTrace();
	    } catch (IllegalAccessException e) {
	        e.printStackTrace();
	    }
	    
	    addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                	zoomInDomain(getWidth() / 2, 0);
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    zoomOutDomain(getWidth() / 2, 0);
                }
            }
        });
	    
	    addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                ChartEntity entity = event.getEntity();
                if (entity instanceof LegendItemEntity) {
                    LegendItemEntity legendEntity = (LegendItemEntity) entity;
                    @SuppressWarnings("rawtypes")
					Comparable seriesKey = legendEntity.getSeriesKey();
                    for (XYDataset dataset : getChart().getXYPlot().getDatasets().values()) {
                    	if (dataset.getSeriesKey(0).equals(seriesKey)) {
                            XYItemRenderer render = getChart().getXYPlot().getRendererForDataset(dataset);
                            if (render != null) {
                            	render.setSeriesPaint(0, toggleAlpha(((Color) render.getSeriesPaint(0))));
                            }

                    		
                    	}
                    }

                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
                // No action required on mouse move.
            }
        });

	    setFocusable(true);
	}
	
	/**
	 * Overrides the parent method to define the preferred size of the chart panel.
	 *
	 * @return The preferred dimensions of the panel.
	 */
	@Override
    public Dimension getPreferredSize() {
		return super.getPreferredSize();
    }
	
	
	/**
	 * Returns the underlying chart instance associated with this panel.
	 *
	 * @return The {@link CustomLinesChart} instance.
	 */
	public CustomLinesChart getLinesChart() {
		return this.chartChart;
	}
	
	/**
	 * Return the xValue at which the mouse pointer is over vertically on the graph.
	 *	<p>
	 * This does not actually confirm the xValue actually has any data.
	 *
	 * @return The domain (x-axis) value corresponding to the mouse pointer's
	 * horizontal position, or null if the pointer is not over the plot area.
	 */
	public Double getXValueFromScreenPos() {
		
		Point mousePoint = MouseInfo.getPointerInfo().getLocation();
		
		
		if (!pointIsOverChart(mousePoint)) {
			return null;
		}
		
		SwingUtilities.convertPointFromScreen(mousePoint, this); // edits in place without return
		Point2D point2d = translateScreenToJava2D(mousePoint);

		Rectangle2D plotArea = getScreenDataArea();
		XYPlot plot = (XYPlot) getChart().getPlot(); // your plot
		return plot.getDomainAxis().java2DToValue(point2d.getX(), plotArea, plot.getDomainAxisEdge());
	}
	
	/**
	 * Used to determine if the mouse pointer is over the chart's data area.
	 *
	 * @param p The point to check, in screen coordinates.
	 * @return True if the point is within the chart's screen data area, false otherwise.
	 */
	private boolean pointIsOverChart(Point p) {
		Rectangle2D chartrect = getScreenDataArea();
		Point parentUp = getLocationOnScreen();
		Rectangle newRect = new Rectangle(parentUp.x + (int) chartrect.getX(), parentUp.y + (int) chartrect.getY(),
				(int) chartrect.getWidth(), (int) chartrect.getHeight());

		return (newRect.contains(p));

	}
	
    /**
     * Toggles the alpha component of a color between 0 (fully transparent) and 255 (fully opaque).
     *
     * @param original The original {@link Color}.
     * @return A new {@link Color} instance with the toggled alpha value.
     * @throws IllegalArgumentException if the original color is null.
     */
    private static Color toggleAlpha(Color original) {
        if (original == null) {
            throw new IllegalArgumentException("Original color cannot be null.");
        }
        if (original.getAlpha() > 0) {
            return new Color(original.getRed(), original.getGreen(), original.getBlue(), 0);

        } else {
            return new Color(original.getRed(), original.getGreen(), original.getBlue(), 255);

        }
    }
    
	
}
