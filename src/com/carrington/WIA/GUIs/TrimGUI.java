package com.carrington.WIA.GUIs;

import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;

import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleInsets;

import com.carrington.WIA.Graph.CustomLinesChartPanel;
import com.carrington.WIA.Utils;
import com.carrington.WIA.DataStructures.HemoData;
import com.carrington.WIA.GUIs.Components.JCButton;
import com.carrington.WIA.Graph.CustomLinesChart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionEvent;

/**
 * A dialog for trimming a {@link HemoData} object by selecting start and end
 * points on a chart.
 */
public class TrimGUI extends JDialog {

	private static final long serialVersionUID = -5175211099134912625L;
	private JPanel contentPane;
	private JTextField txtRight;
	private JTextField txtLeft;
	private double trimXLeft = Double.NaN;
	private Marker trimXMarkerLeft = null;
	private double trimXRight = Double.NaN;
	private Marker trimXMarkerRight = null;
	private final double[] validRangeX;
	private final HemoData data;
	private final String chartTitle;

	private CustomLinesChartPanel chartPanel = null;

	/**
	 * Create the frame.
	 * 
	 */
	public TrimGUI(String chartTitle, HemoData data, int[] trimIndices) {
		super();
		setModal(true);

		this.data = data;
		this.chartTitle = chartTitle;
		double[] xValues = data.getXData();
		this.validRangeX = new double[] { xValues[0], xValues[xValues.length - 1] };
		if (trimIndices != null && (trimIndices.length != 2 || !isWithinBounds(data, trimIndices[0])
				|| !isWithinBounds(data, trimIndices[1]))) {
			throw new IllegalArgumentException("Trim indices out of bounds... likely coding error");
		}

		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			// jsut won't use the look and feel
			e.printStackTrace();
		}

		Font normalText = Utils.getTextFont(false);
		Font normalTextBold = Utils.getTextFont(true);

		int size = normalTextBold.getSize();

		setTitle("Trim Graph");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		int width = Utils.getMaxAppSize().width / 2;
		int height = Utils.getMaxAppSize().height / 2;
		setBounds(100, 100, Math.max(600, width), Math.max(600, height));
		setMinimumSize(new Dimension(width, height));
		setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);

		JPanel pnlInstr = new JPanel();
		pnlInstr.setBackground(new Color(192, 192, 192));
		pnlInstr.setBorder(new LineBorder(new Color(0, 0, 0)));

		_createChartPanel();
		JPanel pnlGraph = this.chartPanel;
		pnlGraph.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "trimLeft");
		pnlGraph.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0, false), "trimLeft");
		pnlGraph.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0, false), "trimLeft");

		pnlGraph.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "trimRight");
		pnlGraph.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_2, 0, false), "trimRight");
		pnlGraph.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "trimRight");

		pnlGraph.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, false), "trimReset");

		pnlGraph.getActionMap().put("trimReset", new AbstractAction() {
			private static final long serialVersionUID = -7703504349811078217L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setTrimRight(Double.NaN);
				setTrimLeft(Double.NaN);

			}
		});

		pnlGraph.getActionMap().put("trimRight", new AbstractAction() {

			private static final long serialVersionUID = -5072632055650935107L;

			@Override
			public void actionPerformed(ActionEvent e) {

				Point mousePoint = MouseInfo.getPointerInfo().getLocation();
				if (!pointIsOverChart(mousePoint)) {
					return;
				}
				double xVal = getXValueFromScreenPos(mousePoint);

				if (_validateTrim(xVal, true)) {
					setTrimRight(xVal);

				}

			}
		});

		pnlGraph.getActionMap().put("trimLeft", new AbstractAction() {

			private static final long serialVersionUID = -4346817373303671458L;

			@Override
			public void actionPerformed(ActionEvent e) {

				Point mousePoint = MouseInfo.getPointerInfo().getLocation();
				if (!pointIsOverChart(mousePoint)) {
					return;
				}
				double xVal = getXValueFromScreenPos(mousePoint);

				if (_validateTrim(xVal, false)) {

					setTrimLeft(xVal);
				}

			}
		});
		pnlGraph.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		JPanel pnlButtons = new JPanel();
		pnlButtons.setBackground(new Color(213, 213, 213));
		pnlButtons.setBorder(new LineBorder(new Color(0, 0, 0)));
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)

				.addComponent(pnlButtons, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addComponent(pnlGraph, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addComponent(pnlInstr, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
		gl_contentPane.setVerticalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
						.addComponent(pnlInstr, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(pnlGraph, GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(pnlButtons,
								GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)) // set
		// fixed
		);

		JLabel lblInstruction = new JLabel("Trim the file below.");
		pnlInstr.add(lblInstruction);

		JCButton btnCancel = new JCButton("Cancel", JCButton.BUTTON_QUIT);

		ActionListener quitAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
				// this is a JDialog, so code calling it will hang. When they return they can
				// get values as needed.

			}
		};
		btnCancel.addActionListener(quitAction);

		JCButton btnOK = new JCButton("Accept", JCButton.BUTTON_ACCEPT);
		btnOK.addActionListener(quitAction);

		this.getRootPane().setDefaultButton(btnOK);

		JLabel lblTrimVals = new JLabel("Trim values:");
		lblTrimVals.setFont(normalTextBold);

		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setBackground(new Color(192, 192, 192));
		separator.setForeground(new Color(192, 192, 192));

		JLabel lblLeft = new JLabel("Left");

		JLabel lblRight = new JLabel("Right");

		JTextArea lblSelInstruction = new JTextArea(
				"Press \"1\" on graph to select the left trim. Press \"2\" to select the right trim. Press \"R\" to reset both.");
		lblSelInstruction.setFont(normalText);
		lblSelInstruction.setLineWrap(true);
		lblSelInstruction.setWrapStyleWord(true);
		lblSelInstruction.setOpaque(false);
		JScrollPane jscr = new JScrollPane();
		jscr.setViewportView(lblSelInstruction);
		jscr.setBorder(BorderFactory.createEmptyBorder());
		jscr.setOpaque(false);
		jscr.getViewport().setOpaque(false);

		txtRight = new JTextField();
		txtRight.setColumns(10);
		txtRight.setFont(normalText);

		txtLeft = new JTextField();
		txtLeft.setColumns(10);
		txtLeft.setFont(normalText);

		JCButton btnResetLeft = new JCButton("Reset", JCButton.BUTTON_STANDARD);
		btnResetLeft.setFont(normalText);
		btnResetLeft.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setTrimLeft(Double.NaN);
			}
		});
		JCButton btnResetRight = new JCButton("Reset", JCButton.BUTTON_STANDARD);
		btnResetRight.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setTrimRight(Double.NaN);
			}
		});
		btnResetRight.setFont(normalText);
		GroupLayout gl_panel_2 = new GroupLayout(pnlButtons);
		gl_panel_2.setHorizontalGroup(gl_panel_2.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_2.createSequentialGroup().addGap(3).addComponent(lblTrimVals,
						GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
				.addGroup(gl_panel_2.createSequentialGroup().addContainerGap().addGroup(gl_panel_2
						.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_2.createSequentialGroup().addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_panel_2.createParallelGroup(Alignment.LEADING).addComponent(lblRight)
										.addComponent(lblLeft))
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_panel_2.createParallelGroup(Alignment.LEADING)
										.addGroup(gl_panel_2.createSequentialGroup()
												.addComponent(txtLeft, size * 10, size * 10, size * 10)
												.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnResetLeft))
										.addGroup(gl_panel_2.createSequentialGroup()
												.addComponent(txtRight, size * 10, size * 10, size * 10)
												.addPreferredGap(ComponentPlacement.RELATED)
												.addComponent(btnResetRight)))
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(jscr,
										GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(gl_panel_2.createParallelGroup(Alignment.TRAILING)
								.addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(btnOK, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addContainerGap()));
		gl_panel_2.setVerticalGroup(gl_panel_2.createParallelGroup(Alignment.LEADING).addGroup(Alignment.TRAILING,
				gl_panel_2.createSequentialGroup().addGap(3).addComponent(lblTrimVals)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_panel_2
								.createParallelGroup(Alignment.LEADING).addGroup(gl_panel_2.createSequentialGroup() // Trim
																													// boxes
										.addGroup(
												gl_panel_2.createParallelGroup(Alignment.BASELINE).addComponent(lblLeft)
														.addComponent(txtLeft, GroupLayout.PREFERRED_SIZE,
																GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
														.addComponent(btnResetLeft))
										.addPreferredGap(ComponentPlacement.UNRELATED)
										.addGroup(gl_panel_2.createParallelGroup(Alignment.BASELINE)
												.addComponent(lblRight)
												.addComponent(txtRight, GroupLayout.PREFERRED_SIZE,
														GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
												.addComponent(btnResetRight)))
								.addComponent(jscr)
								.addComponent(separator, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addGroup(Alignment.TRAILING,
										gl_panel_2.createSequentialGroup().addComponent(btnCancel)
												.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnOK)))
						.addContainerGap()));
		pnlButtons.setLayout(gl_panel_2);
		contentPane.setLayout(gl_contentPane);

		_setupTxtFields();

		Utils.setFont(Utils.getSubTitleFont(), lblInstruction);

		Utils.setFont(Utils.getTextFont(false), lblSelInstruction, lblLeft, lblRight);

		if (trimIndices != null) {
			if (isSet(trimIndices[0])) {
				setTrimLeft(data.getXData()[trimIndices[0]]);
			}
			if (isSet(trimIndices[1])) {
				setTrimRight(data.getXData()[trimIndices[1]]);
			}
		}

		chartPanel.requestFocusInWindow();

	}

	/**
	 * Retrieves the start and end indices for the trim based on user selections.
	 * 
	 * @return An array of two integers: the left trim index and the right trim
	 *         index. Returns -1 for an index if it was not set.
	 */
	public int[] getTrimIndices() {

		double[] xVals = this.data.getXData();

		int[] indices = new int[2];
		if (!Double.isNaN(this.trimXLeft)) {

			double distance = Math.abs(xVals[0] - trimXLeft);
			int idx = 0;
			for (int c = 1; c < xVals.length; c++) {
				double cdistance = Math.abs(xVals[c] - trimXLeft);
				if (cdistance < distance) {
					idx = c;
					distance = cdistance;
				}
			}
			indices[0] = idx;
		} else {
			indices[0] = -1;
		}

		if (!Double.isNaN(trimXRight)) {
			double distance = Math.abs(xVals[0] - trimXRight);
			int idx = 0;
			for (int c = 1; c < xVals.length; c++) {
				double cdistance = Math.abs(xVals[c] - trimXRight);
				if (cdistance < distance) {
					idx = c;
					distance = cdistance;
				}
			}
			indices[1] = idx;
		} else {
			indices[1] = -1;
		}

		if (indices[0] < -1) { // remember -1 means not selected
			indices[0] = 0;
		}
		if (indices[1] >= xVals.length) {
			indices[1] = xVals.length;
		}

		return indices;

	}

	/**
	 * Checks if a given screen point is within the chart's data area.
	 * 
	 * @param p The point on the screen.
	 * @return {@code true} if the point is over the chart, {@code false} otherwise.
	 */
	private boolean pointIsOverChart(Point p) {
		Rectangle2D chartrect = this.chartPanel.getScreenDataArea();
		Point parentUp = chartPanel.getLocationOnScreen();
		Rectangle newRect = new Rectangle(parentUp.x + (int) chartrect.getX(), parentUp.y + (int) chartrect.getY(),
				(int) chartrect.getWidth(), (int) chartrect.getHeight());

		return (newRect.contains(p));

	}

	/**
	 * Creates and initializes the chart panel.
	 * 
	 * @throws IllegalStateException if the chart panel has already been created.
	 */
	private void _createChartPanel() {
		if (this.chartPanel != null)
			throw new IllegalStateException("Chart panel already created");

		this.chartPanel = new CustomLinesChartPanel(new CustomLinesChart(this.chartTitle, this.data));
		this.chartPanel.setFocusable(true);
	}

	/**
	 * Translates a screen coordinate to an x-value on the chart's domain axis.
	 * 
	 * @param point The screen point to translate.
	 * @return The corresponding x-value on the chart.
	 */
	private double getXValueFromScreenPos(Point point) {
		SwingUtilities.convertPointFromScreen(point, chartPanel); // edits in place without return
		Point2D point2d = chartPanel.translateScreenToJava2D(point);

		Rectangle2D plotArea = chartPanel.getScreenDataArea();
		XYPlot plot = (XYPlot) chartPanel.getChart().getPlot(); // your plot
		return plot.getDomainAxis().java2DToValue(point2d.getX(), plotArea, plot.getDomainAxisEdge());
	}

	/**
	 * Validate that the trim is in an OK position
	 */
	private boolean _validateTrim(double trim, boolean right) {

		if (trim <= this.validRangeX[0] || trim >= this.validRangeX[1]) {
			Utils.showError("Invalid trim. Must be withinin data range.", this);

			return false;
		}
		if (right) {

			double minValidRight = (Double.isNaN(this.trimXLeft) ? this.validRangeX[0] : trimXLeft);

			if (trim > minValidRight) {

				return true;
			} else {
				Utils.showError("Invalid trim. Right trim must be further to RIGHT than left trim.", this);
				return false;
			}
		} else {
			double maxValidLeft = (Double.isNaN(this.trimXRight) ? this.validRangeX[1] : trimXRight);

			if (trim < maxValidLeft) {
				return true;
			} else {
				Utils.showError("Invalid trim. Left trim must be further to LEFT than right trim.", this);

				return false;
			}
		}

	}

	/**
	 * Assumes the trim has been VAILDATED
	 */
	private void setTrimLeft(double trimLeft) {
		XYPlot plot = (XYPlot) this.chartPanel.getChart().getPlot();

		if (this.trimXMarkerLeft != null) {
			plot.removeDomainMarker(this.trimXMarkerLeft);
		}

		if (Double.isNaN(trimLeft)) {
			this.txtLeft.setText("");
			this.trimXLeft = Double.NaN;
			this.trimXMarkerLeft = null;

		} else {
			ValueMarker marker = new ValueMarker(trimLeft);
			marker.setLabel("Left");
			marker.setLabelOffset(new RectangleInsets(15, -20, 0, 0));
			marker.setLabelFont(Utils.getTextFont(false));
			marker.setPaint(Color.BLACK);
			marker.setStroke(new BasicStroke(2f));
			plot.addDomainMarker(marker);
			this.trimXMarkerLeft = marker;
			this.txtLeft.setText(getRoundedDecimalPart(trimLeft)); // remove decimal.
			this.trimXLeft = trimLeft;
		}

	}

	/**
	 * Assumes the trim has been VAILDATED
	 */
	private void setTrimRight(double trimRight) {
		XYPlot plot = (XYPlot) this.chartPanel.getChart().getPlot();

		if (this.trimXMarkerRight != null) {
			plot.removeDomainMarker(this.trimXMarkerRight);
		}

		if (Double.isNaN(trimRight)) {
			this.txtRight.setText("");
			this.trimXRight = Double.NaN;
			this.trimXMarkerRight = null;
		} else {
			ValueMarker marker = new ValueMarker(trimRight);
			marker.setLabel("Right");
			marker.setLabelOffset(new RectangleInsets(15, 20, 0, 0));
			marker.setLabelFont(Utils.getTextFont(false));
			marker.setPaint(Color.BLACK);
			marker.setStroke(new BasicStroke(2f));
			plot.addDomainMarker(marker);
			this.trimXMarkerRight = marker;
			this.txtRight.setText(getRoundedDecimalPart(trimRight)); // remove decimal.
			this.trimXRight = trimRight;
		}

	}

	/**
	 * Initializes the text boxes to display trims, should not need to be edited.
	 */
	private void _setupTxtFields() {
		this.txtLeft.setEditable(false);
		this.txtLeft.setFocusable(true);
		this.txtRight.setEditable(false);
		this.txtRight.setFocusable(true);

	}

	/**
	 * Checks if an index is set (i.e., not -1).
	 * 
	 * @param xValueIndex The index to check.
	 * @return {@code true} if the index is not -1, {@code false} otherwise.
	 */
	private boolean isSet(Integer xValueIndex) {
		return xValueIndex != -1;
	}

	/**
	 * Checks if a given index is within the bounds of the {@link HemoData} x-axis data.
	 * @param hd The {@link HemoData} object.
	 * @param xValueIndex The index to check.
	 * @return {@code true} if the index is within bounds, {@code false} otherwise.
	 */
	private boolean isWithinBounds(HemoData hd, Integer xValueIndex) {
		return xValueIndex >= -1 && xValueIndex < hd.getXData().length;
	}

	/**
	 * 
	 * @param input the number to prune
	 * @return number with only 1 digit after (to the right of) the decimal point
	 */
	private static String getRoundedDecimalPart(double input) {
		// Round the number to one decimal place
		double roundedValue = Math.round(input * 10) / 10.0;

		// Return as a string
		return String.valueOf(roundedValue);
	}

}
