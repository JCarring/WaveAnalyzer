package com.carrington.WIA.GUIs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.carrington.WIA.Utils;
import com.carrington.WIA.Cardio.Wave;
import com.carrington.WIA.DataStructures.HemoData;
import com.carrington.WIA.DataStructures.WIAData;
import com.carrington.WIA.GUIs.Components.JCHelpButton;
import com.carrington.WIA.GUIs.Components.JCLabel;
import com.carrington.WIA.GUIs.Components.JCToggleButton;
import com.carrington.WIA.Graph.NetWaveChartPanel;
import com.carrington.WIA.Graph.PressureFlowChartPanel;
import com.carrington.WIA.Graph.PressureFlowChartPanel.PFPickListener;
import com.carrington.WIA.Graph.SepWavePanel;
import com.carrington.WIA.Graph.SepWavePanel.WavePickListener;
import com.carrington.WIA.IO.Header;
import com.carrington.WIA.IO.WIAResourceReader;
import com.carrington.WIA.Math.Savgol;
import com.carrington.WIA.Math.Savgol.SavGolSettings;

/**
 * A dialog for previewing wave intensity analysis on a single beat of
 * hemodynamic data. It allows for data filtering and alignment before final
 * processing.
 */
public class WavePickerPreviewGUI extends JDialog implements PFPickListener, WavePickListener {

	private static final long serialVersionUID = -4214441167859285576L;
	private static final Color pnlLightGray = new Color(213, 213, 213);
	private static final Color pnlDarkGray = new Color(169, 169, 169);
	/** Status code indicating done with previewing */
	public static final int DONE = 0;
	/** Status code indicating to preview next */
	public static final int PREVIEW_NEXT = 1;
	/** Status code indicating to preview previous */
	public static final int PREVIEW_LAST = 2;

	private JPanel contentPane;
	private JPanel pnlTop;
	private JPanel pnlWIA;
	private JPanel pnlWIAButtons;
	private JPanel pnlWIADisplay;
	private JButton previewBtnNext;
	private JButton previewBtnPrev;
	private JButton previewBtnOK;
	private JTextField txtCVal;
	private JTextField txtFlowAvg;
	private JTextField txtPressAvg;
	private JCHelpButton btnPF;
	private JButton btnAlign;
	private JButton btnResetAlign;
	private JTextField txtSavWindow;
	private JTextField txtSavPolynomialOrder;
	private JCheckBox chFilter;
	private JCheckBox chMaintainFilterSettings;
	private JButton btnReFilter;
	private SepWavePanel pnlGraphWIASep;
	private NetWaveChartPanel pnlGraphWIANet;
	private PressureFlowChartPanel pnlGraphPF;
	private JCToggleButton btnPFModeOff;
	private JCToggleButton btnPFModeAlignPeak;
	private JCToggleButton btnPFModeAlignManual;
	private JCheckBox chAllowWrap;
	private JCheckBox chAllowWrapIgnoreEnds;
	private JCheckBox chMeasureWaveIntensity;
	private JTextField txtWaveIntensity;

	// Old HemoData, pre-filtered. Stored so that re-filtering can occur
	private HemoData data = null;
	private WIAData wiaDataPreview = null;
	private boolean filterEnabled;
	private SavGolSettings filterSettings;
	private int _indexPressureAligned = -1;
	private int _indexFlowAligned = -1;

	private int status = DONE;

	private final boolean hasPreviousPreview;
	private final boolean hasNextPreview;

	private PreviewResult previewResult = null;

	private final Component compForPosition;

	/**
	 * Creates the preview frame.
	 *
	 * @param selectionName        The name of the current data selection.
	 * @param data                 The hemodynamic data to be previewed.
	 * @param pr                   The result from a previous preview, if any.
	 * @param hasPreviousPreview   True if a "previous" preview option should be
	 *                             available.
	 * @param hasNextPreview       True if a "next" preview option should be
	 *                             available.
	 * @param filterSettings       The initial settings for the Savitzky-Golay
	 *                             filter.
	 * @param filterEnabled        True if the filter should be enabled by default.
	 * @param allowAlignWrap       True if wrap-around alignment is permitted.
	 * @param allowWrapDiscordance True if excessive discordance during wrap-around
	 *                             should be ignored.
	 * @param maintainSettings     True if settings should be maintained for the
	 *                             next preview.
	 * @param relative             The component to position this dialog relative
	 *                             to.
	 */
	public WavePickerPreviewGUI(String selectionName, HemoData data, PreviewResult pr, boolean hasPreviousPreview,
			boolean hasNextPreview, SavGolSettings filterSettings, boolean filterEnabled, boolean allowAlignWrap,
			boolean allowWrapDiscordance, boolean maintainSettings, Component relative) {

		this.data = data;
		this.hasPreviousPreview = hasPreviousPreview;
		this.hasNextPreview = hasNextPreview;
		this.compForPosition = relative;
		if (pr != null) {
			this.filterSettings = pr.filterSettings;
			this.filterEnabled = filterEnabled;
			this._indexFlowAligned = pr.indexFlowAlign;
			this._indexPressureAligned = pr.indexPressureAlign;
		} else {
			this.filterEnabled = filterEnabled;
			this.filterSettings = filterSettings;
			this._indexFlowAligned = -1;
			this._indexPressureAligned = -1;
		}

		setTitle("[PREVIEW] Waves for Selection '" + selectionName + "' ("
				+ Utils.getShortenedFilePath(data.getFile().getPath(), 80) + ")");

		setModal(true);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 1240, 850);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);

		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				status = DONE;
				createPreviewResult();
				discard();
			}

			@Override
			public void windowOpened(WindowEvent e) {
				pnlGraphWIASep.displayExistingChoices();
				pnlGraphPF.displayExistingChoices();
			}
		});

		generateWIA(this.filterEnabled, false, allowAlignWrap, allowWrapDiscordance);

		initPnlTop(selectionName);
		initWIA();
		initPnlDisplaySetting(allowAlignWrap, allowWrapDiscordance, maintainSettings);
		initPnlButtons();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		pnlWIA.setPreferredSize(pnlWIA.getPreferredSize());
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addComponent(pnlTop, GroupLayout.DEFAULT_SIZE, 1240, Short.MAX_VALUE)
				.addGroup(Alignment.TRAILING, gl_contentPane.createSequentialGroup()
						.addComponent(pnlWIA, screenSize.width / 4, screenSize.width / 2, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(pnlWIADisplay,
								GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
				.addComponent(pnlWIAButtons, GroupLayout.DEFAULT_SIZE, 1240, Short.MAX_VALUE));
		gl_contentPane.setVerticalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING).addGroup(gl_contentPane
				.createSequentialGroup()
				.addComponent(
						pnlTop, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addComponent(pnlWIA, GroupLayout.DEFAULT_SIZE, 725, Short.MAX_VALUE)
						.addComponent(pnlWIADisplay, GroupLayout.DEFAULT_SIZE, 725, Short.MAX_VALUE))
				.addPreferredGap(ComponentPlacement.RELATED).addComponent(pnlWIAButtons, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)));

		setupDisplayTextFields(this.txtCVal, this.txtFlowAvg, this.txtPressAvg);

		contentPane.setLayout(gl_contentPane);
		Utils.unfocusAll(pnlWIAButtons);
		// Utils.unfocusAll(pnlWIA);
		Utils.unfocusAll(pnlWIADisplay);
		Utils.unfocusAll(pnlTop);

	}

	/**
	 * Initializes the top panel of the GUI, displaying the selection name.
	 * 
	 * @param selectionName The name of the selection being previewed.
	 */
	private void initPnlTop(String selectionName) {
		pnlTop = new JPanel();
		FlowLayout flowLayout = (FlowLayout) pnlTop.getLayout();
		flowLayout.setHgap(10);
		pnlTop.setBorder(new LineBorder(new Color(0, 0, 0)));
		pnlTop.setBackground(pnlDarkGray);

		JLabel pnlInstruction = new JLabel("<html><span style='background-color: yellow;'>Preview wave profile for \""
				+ selectionName + "\"</span></html>");

		pnlInstruction.setFont(Utils.getSubTitleFont());
		pnlTop.add(pnlInstruction);

	}

	/**
	 * Initializes the main WIA panel containing the separated wave and
	 * pressure-flow graphs.
	 */
	private void initWIA() {

		pnlWIA = new JPanel();
		pnlWIA.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		JSplitPane splitTop = new JSplitPane();
		splitTop.setResizeWeight(0.5);
		splitTop.setContinuousLayout(true);
		splitTop.setOrientation(JSplitPane.VERTICAL_SPLIT);
		GroupLayout gl_pnlWIA = new GroupLayout(pnlWIA);
		gl_pnlWIA.setHorizontalGroup(gl_pnlWIA.createParallelGroup(Alignment.LEADING).addComponent(splitTop,
				GroupLayout.DEFAULT_SIZE, 965, Short.MAX_VALUE));
		gl_pnlWIA.setVerticalGroup(gl_pnlWIA.createParallelGroup(Alignment.LEADING).addComponent(splitTop,
				GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE));
		pnlGraphWIASep = SepWavePanel.generate(wiaDataPreview, Utils.getTextFont(false), true);

		pnlGraphWIASep.setBorder(new LineBorder(new Color(0, 0, 0)));
		pnlGraphWIASep.setWavePickListener(this);

		splitTop.setLeftComponent(pnlGraphWIASep);

		pnlGraphPF = PressureFlowChartPanel.generate(wiaDataPreview, Utils.getTextFont(false));
		pnlGraphPF.setCyclePickListener(this);
		pnlGraphPF.setBorder(new LineBorder(new Color(0, 0, 0)));

		splitTop.setRightComponent(pnlGraphPF);

		pnlGraphWIANet = NetWaveChartPanel.generate(wiaDataPreview, Utils.getTextFont(false));
		pnlGraphWIANet.setBorder(new LineBorder(new Color(0, 0, 0)));

		pnlWIA.setLayout(gl_pnlWIA);
	}

	/**
	 * Initializes the right-side display and settings panel.
	 * 
	 * @param allowAlignWrap                     If true, allows wrap-around
	 *                                           alignment.
	 * @param allowAlignWrapExcessiveDiscordance If true, ignores excessive
	 *                                           discordance on wrap-around.
	 * @param maintain                           If true, maintains settings for the
	 *                                           next preview.
	 */
	private void initPnlDisplaySetting(boolean allowAlignWrap, boolean allowAlignWrapExcessiveDiscordance,
			boolean maintain) {
		pnlWIADisplay = new JPanel();
		pnlWIADisplay.setBackground(pnlLightGray);
		pnlWIADisplay.setBorder(new LineBorder(new Color(0, 0, 0)));

		JLabel lblMetrics = new JLabel("Metrics");

		JLabel lblCVal = new JLabel("C-value:");
		JLabel lblAvgFlow = new JLabel("Avg Flow:");
		JLabel lblAvgPress = new JLabel("Avg Pressure:");

		txtCVal = new JTextField();
		txtFlowAvg = new JTextField();
		txtPressAvg = new JTextField();

		JLabel lblWaveIntensity = new JLabel("Wave Intensity");
		chMeasureWaveIntensity = new JCheckBox("Test Measure Wave Intensity");
		chMeasureWaveIntensity.setSelected(true);
		pnlGraphWIASep.setTrace(true);
		chMeasureWaveIntensity.addActionListener(e -> {
			txtWaveIntensity.setText(null);
			pnlGraphWIASep.setTrace(chMeasureWaveIntensity.isSelected());
		});
		chMeasureWaveIntensity.setOpaque(false);
		JLabel lblCurrSelection = new JLabel("Current intensity:");
		txtWaveIntensity = new JTextField("");
		txtWaveIntensity.setEditable(false);
		txtWaveIntensity.setFocusable(false);

		JLabel lblPF = new JLabel("Pressure and Flow");
		btnPF = new JCHelpButton(WIAResourceReader.getContents(WIAResourceReader.HELP_WAVE_ALIGN_PF));
		btnPF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				Utils.showMessage(JOptionPane.INFORMATION_MESSAGE, btnPF.getHelpMessage(), compForPosition);
			}
		});
	

		JLabel lblSelectionMode = new JLabel("Selection mode");

		btnAlign = new JButton("Run Align");
		btnAlign.setEnabled(false);
		btnAlign.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				runAlignPressureFlow();

			}

		});

		btnResetAlign = new JButton("Reset");

		btnResetAlign.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				resetAlignPressureFlow();

			}

		});
		// Strings separately for switch statements
		final String strModOff = " Off ";
		final String strModAlignPeak = " Align (Peak) ";
		final String strModAlignManual = " Align (Manual) ";

		btnPFModeOff = new JCToggleButton(strModOff, JCToggleButton.BUTTON_SMALL);
		btnPFModeAlignPeak = new JCToggleButton(strModAlignPeak, JCToggleButton.BUTTON_SMALL);
		btnPFModeAlignManual = new JCToggleButton(strModAlignManual, JCToggleButton.BUTTON_SMALL);
		ItemListener btnPFListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				// Check if the button is being selected
				if (e.getStateChange() == ItemEvent.SELECTED) {
					switch (((JToggleButton) e.getSource()).getText()) {
					case strModOff:
						pnlGraphPF.setSelectMode(PressureFlowChartPanel.MODE_NONE);
						break;
					case strModAlignPeak:
						pnlGraphPF.setSelectMode(PressureFlowChartPanel.MODE_ALIGN_PEAK);
						break;
					case strModAlignManual:
						pnlGraphPF.setSelectMode(PressureFlowChartPanel.MODE_ALIGN_MANUAL);
						break;
					}
				}
			}
		};

		if (wiaDataPreview != null && wiaDataPreview.hasOriginal()) {
			btnResetAlign.setEnabled(true);
			btnPFModeAlignManual.setEnabled(false);
			btnPFModeAlignPeak.setEnabled(false);
		} else {
			btnResetAlign.setEnabled(false);
		}

		chAllowWrap = new JCheckBox("Wrap-around when align");
		chAllowWrap.setOpaque(false);
		chAllowWrap.setSelected(true);
		chAllowWrapIgnoreEnds = new JCheckBox("<html>Ignore excessive discordance<br> on wrap-around</html>");
		chAllowWrapIgnoreEnds.setOpaque(false);

		// Add the listener to each toggle button
		btnPFModeOff.addItemListener(btnPFListener);
		btnPFModeAlignPeak.addItemListener(btnPFListener);
		btnPFModeAlignManual.addItemListener(btnPFListener);

		ButtonGroup group = new ButtonGroup();
		group.add(btnPFModeOff);
		group.add(btnPFModeAlignPeak);
		group.add(btnPFModeAlignManual);
		btnPFModeOff.setSelected(true);

		JLabel lblFilter = new JLabel("Filter");
		JLabel lblWindow = new JLabel("Window size:");
		JLabel lblPolyOrder = new JLabel("Polynomial order:");
		chFilter = new JCheckBox("Apply Savitsy-Golay filter");
		chFilter.setSelected(this.filterEnabled);
		chFilter.setOpaque(false);
		chFilter.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					filterEnabled = true;
				} else if (e.getStateChange() == ItemEvent.DESELECTED) {
					filterEnabled = false;
				}

				Utils.setEnabled(filterEnabled, false, btnReFilter);
				generateWIA();
			}
		});

		chMaintainFilterSettings = new JCheckBox("Maintain filter settings");
		chMaintainFilterSettings.setToolTipText("Maintain current settings to the next wave intensity preview");
		chMaintainFilterSettings.setSelected(maintain);
		chMaintainFilterSettings.setOpaque(false);

		txtSavWindow = new JTextField(filterSettings.window + "");
		txtSavPolynomialOrder = new JTextField(filterSettings.polyOrder + "");
		btnReFilter = new JButton("Re-Filter");
		Utils.setEnabled(this.filterEnabled, false, btnReFilter);
		btnReFilter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!filterEnabled)
					return;

				if (validateFilterSettings()) {
					generateWIA();
				}

			}

		});

		txtSavWindow.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				txtSavWindow.setFocusable(true);
				txtSavWindow.requestFocusInWindow();
			}
		});
		txtSavPolynomialOrder.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {

				txtSavPolynomialOrder.setFocusable(true);
				txtSavPolynomialOrder.requestFocusInWindow();
			}
		});
		pnlWIA.addMouseListener(new MouseAdapter() {

			public void mouseEntered(MouseEvent e) {
				txtSavWindow.setFocusable(false);
				txtSavPolynomialOrder.setFocusable(false);
				pnlWIA.requestFocusInWindow(); // Shift focus away
			}
		});
		pnlWIADisplay.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {
				txtSavWindow.setFocusable(false);
				txtSavPolynomialOrder.setFocusable(false);
				pnlWIA.requestFocusInWindow(); // Shift focus away
			}
		});

		chAllowWrap.setSelected(allowAlignWrap);
		chAllowWrapIgnoreEnds.setSelected(allowAlignWrapExcessiveDiscordance);

		GroupLayout gl_pnlWIADisplay = new GroupLayout(pnlWIADisplay);

		JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
		sep.setBackground(new Color(192, 192, 192));
		sep.setForeground(new Color(192, 192, 192));

		gl_pnlWIADisplay.setHorizontalGroup(gl_pnlWIADisplay.createSequentialGroup().addGap(5).addGroup(gl_pnlWIADisplay
				.createParallelGroup()
				.addComponent(lblMetrics, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap()
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.TRAILING, false)
								.addComponent(lblCVal, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(lblAvgFlow, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(lblAvgPress, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.LEADING)
								.addComponent(txtCVal, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addComponent(txtFlowAvg, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addComponent(txtPressAvg, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE))
						.addContainerGap())
				.addComponent(lblWaveIntensity, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addGroup(
						gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(chMeasureWaveIntensity))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(lblCurrSelection)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(txtWaveIntensity, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE)
						.addContainerGap())
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addComponent(lblPF)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnPF)
						.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(lblSelectionMode))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(btnPFModeOff)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnPFModeAlignPeak)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnPFModeAlignManual)
						.addContainerGap())
				.addGroup(
						gl_pnlWIADisplay.createSequentialGroup().addContainerGap()
								.addComponent(sep, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addContainerGap())
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(chAllowWrap))
				.addGroup(
						gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(chAllowWrapIgnoreEnds))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(btnAlign)
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(btnResetAlign))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(lblFilter))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(chFilter))
				.addGroup(
						gl_pnlWIADisplay.createSequentialGroup().addContainerGap()
								.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.TRAILING, false)
										.addComponent(lblWindow, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addComponent(lblPolyOrder, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.LEADING)
										.addComponent(txtSavWindow, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
										.addComponent(txtSavPolynomialOrder, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
								.addContainerGap())
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(btnReFilter))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap()
						.addComponent(chMaintainFilterSettings))));

		gl_pnlWIADisplay.setVerticalGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addGap(3).addComponent(lblMetrics)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(gl_pnlWIADisplay
								.createParallelGroup(Alignment.LEADING).addComponent(lblCVal).addComponent(txtCVal))
						.addGap(2)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.LEADING).addComponent(lblAvgFlow)
								.addComponent(txtFlowAvg))
						.addGap(2)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.LEADING).addComponent(lblAvgPress)
								.addComponent(txtPressAvg))
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(lblWaveIntensity)
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(chMeasureWaveIntensity)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.CENTER).addComponent(lblCurrSelection)
								.addComponent(txtWaveIntensity))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(gl_pnlWIADisplay
								.createParallelGroup(Alignment.CENTER).addComponent(lblPF).addComponent(btnPF))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(lblSelectionMode, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.CENTER).addComponent(btnPFModeOff)
								.addComponent(btnPFModeAlignPeak).addComponent(btnPFModeAlignManual))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(sep, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(chAllowWrap)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(chAllowWrapIgnoreEnds)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.CENTER).addComponent(btnAlign)
								.addComponent(btnResetAlign))
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(lblFilter)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(chFilter)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.CENTER).addComponent(lblWindow)
								.addComponent(txtSavWindow, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.CENTER).addComponent(lblPolyOrder)
								.addComponent(txtSavPolynomialOrder, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(btnReFilter)
						.addPreferredGap(ComponentPlacement.UNRELATED, Short.MAX_VALUE, Short.MAX_VALUE)
						.addComponent(chMaintainFilterSettings).addContainerGap()));

		Utils.setFont(Utils.getSubTitleFont(), lblMetrics, lblPF, lblFilter, lblWaveIntensity);
		Utils.setFont(Utils.getTextFont(false), btnAlign, btnResetAlign, btnReFilter, chMaintainFilterSettings);
		Utils.setFont(Utils.getSubTitleSubFont(), lblSelectionMode);

		Utils.setFont(Utils.getSmallTextFont(), lblCVal, lblAvgFlow, lblAvgPress, txtCVal, txtFlowAvg, txtPressAvg,
				chFilter, txtSavPolynomialOrder, txtSavPolynomialOrder, lblPolyOrder, lblWindow, chAllowWrap,
				chAllowWrapIgnoreEnds, lblCurrSelection, txtWaveIntensity, chMeasureWaveIntensity);

		pnlWIADisplay.setLayout(gl_pnlWIADisplay);
		storeDisplayValues();
		Utils.unfocusButtons(contentPane);

	}

	/**
	 * Initializes the bottom button panel with "Done", "Next Preview", and "Last
	 * Preview" buttons.
	 */
	public void initPnlButtons() {

		pnlWIAButtons = new JPanel();
		pnlWIAButtons.setBackground(pnlLightGray);

		previewBtnOK = new JButton("Done");
		previewBtnOK.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				status = DONE;
				createPreviewResult();
				discard();

			}

		});
		previewBtnOK.setBackground(new Color(255, 255, 150));

		previewBtnNext = new JButton("Next Preview");
		previewBtnNext.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				status = PREVIEW_NEXT;
				createPreviewResult();
				discard();

			}

		});
		previewBtnNext.setBackground(new Color(235, 235, 235));

		previewBtnPrev = new JButton("Last Preview");
		previewBtnPrev.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				status = PREVIEW_LAST;
				createPreviewResult();
				discard();

			}

		});
		previewBtnPrev.setBackground(new Color(235, 235, 235));

		Utils.setFont(Utils.getSubTitleFont(), previewBtnOK);

		JCLabel lblWarn = new JCLabel("Preview only. No changes will be made to the underlying WIA data.",
				JCLabel.LABEL_TEXT_BOLD);
		lblWarn.setForeground(Color.RED);
		pnlWIAButtons.add(lblWarn);

		previewBtnPrev.setEnabled(hasPreviousPreview);
		previewBtnNext.setEnabled(hasNextPreview);
		pnlWIAButtons.add(previewBtnPrev);
		pnlWIAButtons.add(previewBtnNext);
		pnlWIAButtons.add(previewBtnOK);

		FlowLayout flowLayout = (FlowLayout) pnlWIAButtons.getLayout();
		flowLayout.setHgap(10);
		flowLayout.setAlignment(FlowLayout.TRAILING);
		pnlWIAButtons.setBorder(new LineBorder(new Color(0, 0, 0)));
	}

	/**
	 * Configures an array of JTextFields to be non-editable and non-focusable.
	 * 
	 * @param fields The text fields to set up.
	 */
	private void setupDisplayTextFields(JTextField... fields) {
		for (JTextField field : fields) {
			field.setEditable(false);
			field.setFocusable(false);
		}
	}

	/**
	 * Generates WIA data based on the current filter and alignment settings.
	 * 
	 * @return {@code true} if generation is successful, {@code false} otherwise.
	 */
	private boolean generateWIA() {
		return generateWIA(chFilter.isSelected(), true, chAllowWrap.isSelected(), chAllowWrapIgnoreEnds.isSelected());
	}

	/**
	 * Generates WIA and updates fields and graphs
	 */
	private boolean generateWIA(boolean filter, boolean update, boolean allowAlignWrap,
			boolean allowAlignWrapExcessivelyDiscordant) {

		HemoData dataCopy = data.copy();
		if (filter) {

			Savgol savGol = new Savgol(filterSettings.window, filterSettings.polyOrder);

			for (Header header : new ArrayList<Header>(dataCopy.getYHeaders())) {
				if (dataCopy.hasFlag(header, HemoData.OTHER_ALIGN)) {
					continue;
				} else if (dataCopy.hasFlag(header, HemoData.TYPE_FLOW)
						|| dataCopy.hasFlag(header, HemoData.TYPE_PRESSURE)) {
					double[] dataY = dataCopy.getYData(header);
					dataCopy.applyFilter(header, savGol.filter(dataY));
				}

			}

		}

		dataCopy.convertXUnits(HemoData.UNIT_MILLISECONDS);

		WIAData temp = new WIAData(dataCopy.getName(), dataCopy);

		if (_indexFlowAligned != -1 && _indexPressureAligned != -1) {
			// the new, aligned (after +/- filtering as above) data.
			HemoData newHD = _getAlignPressureFlowAfterReFilter(temp, _indexFlowAligned, _indexPressureAligned,
					allowAlignWrap, allowAlignWrapExcessivelyDiscordant);
			if (newHD == null) {
				// error, already displaced
				wiaDataPreview = temp;
				return false;
			}
			temp.setNewHemoData(newHD); // also will re-analyze
		}

		wiaDataPreview = temp;

		if (update) {
			_applyUpdatedWIA();

		}

		return true;

	}

	/**
	 * Updated the 3 graphs after having called the
	 * {@link WavePickerPreviewGUI#generateWIA(boolean)} method
	 */
	private void _applyUpdatedWIA() {
		pnlGraphPF.resetWIAData(wiaDataPreview);
		pnlGraphWIASep.resetWIAData(wiaDataPreview);
		pnlGraphWIANet.resetWIAData(wiaDataPreview);
		storeDisplayValues();
	}

	/**
	 * Validates the current WIA inputs for filtering
	 * 
	 * @return true if validate, false if otherwise (error message already
	 *         displayed). Stores in private variable if valid.
	 */
	private boolean validateFilterSettings() {

		if (!chFilter.isSelected())
			return true;

		try {
			SavGolSettings settings = Savgol.generateSettings(txtSavWindow.getText().trim(),
					txtSavPolynomialOrder.getText().trim());
			filterSettings = settings;
		} catch (Exception e) {
			Utils.showMessage(JOptionPane.ERROR_MESSAGE, e.getMessage(), this);
			return false;
		}

		return true;

	}

	/**
	 * Sets the displayed wave intensity value.
	 * 
	 * @param value The wave intensity value to display. Formats the value with
	 *              units.
	 */
	private void setWaveIntensity(Double value) {
		if (value == null || Double.isNaN(value))
			txtWaveIntensity.setText("");
		else {
			String formatted = _formatToOneDecimal(value) + " W m\u207B\u00B2 s\u207B\u00B2";
			txtWaveIntensity.setText(formatted);

		}
	}

	/**
	 * Resets the pressure and flow alignment selections and reverts to the original
	 * data.
	 */
	private void resetAlignPressureFlow() {

		pnlGraphPF.resetAlignSelections();

		wiaDataPreview.revertToOriginalHemoData();

		_indexFlowAligned = -1;
		_indexPressureAligned = -1;

		_applyUpdatedWIA();
		btnAlign.setEnabled(false);
		btnResetAlign.setEnabled(false);
		btnPFModeAlignManual.setEnabled(true);
		btnPFModeAlignPeak.setEnabled(true);

	}

	/**
	 * ONLY to be called by {@link WavePickerPreviewGUI#generateWIA(boolean)} as a
	 * helper
	 */
	private HemoData _getAlignPressureFlowAfterReFilter(WIAData tempData, int indexFlow, int indexPressure,
			boolean allowAlignWrap, boolean allowAlignWrapExcessivelyDiscordant) {
		Header headerFlow = tempData.getData().getHeaderByFlag(HemoData.TYPE_FLOW).get(0);
		Header headerPressure = tempData.getData().getHeaderByFlag(HemoData.TYPE_PRESSURE).get(0);
		Header headerlower;
		Header headerhigher;
		int indexLower;
		int indexHigher;
		if (indexFlow < indexPressure) {
			headerlower = headerFlow;
			headerhigher = headerPressure;
			indexLower = indexFlow;
			indexHigher = indexPressure;

		} else {
			headerlower = headerPressure;
			headerhigher = headerFlow;
			indexLower = indexPressure;
			indexHigher = indexFlow;
		}

		HemoData hd;

		try {
			hd = tempData.getData().copyWithYAlignment(headerlower, headerhigher, indexLower, indexHigher,
					allowAlignWrap, allowAlignWrapExcessivelyDiscordant);
		} catch (Exception e) {
			Utils.showMessage(JOptionPane.ERROR_MESSAGE, e.getMessage(), this);
			return null;
		}
		return hd;

	}

	/**
	 * Executes the alignment of pressure and flow data based on user selections in
	 * the preview.
	 */
	private void runAlignPressureFlow() {

		// check if valid
		Double timeAlignFlow = pnlGraphPF.getFlowAlignTime();
		Double timeAlignPressure = pnlGraphPF.getPressureAlignTime();
		double[] xData = wiaDataPreview.getData().getXData();
		if (timeAlignFlow == null || timeAlignPressure == null) {
			Utils.showMessage(JOptionPane.ERROR_MESSAGE, "Please set a time to align in both flow and pressure graphs", pnlGraphPF);
			return;
		}

		int indexFlow = Utils.getClosestIndex(timeAlignFlow, xData);
		int indexPressure = Utils.getClosestIndex(timeAlignPressure, xData);

		this._indexFlowAligned = indexFlow;
		this._indexPressureAligned = indexPressure;

		boolean success = generateWIA();
		if (success) {
			btnAlign.setEnabled(false);
			btnResetAlign.setEnabled(true);
			btnPFModeOff.doClick();
			btnPFModeAlignManual.setEnabled(false);
			btnPFModeAlignPeak.setEnabled(false);
		}

	}

	/**
	 * @return the current Savitsky Golay filter settings
	 */
	public SavGolSettings getFilterSettings() {
		validateFilterSettings();
		return filterSettings;
	}

	/**
	 * @return true if should carry over filter settings (i.e. settings changed in
	 *         this preview, and should be kept that way in the next preview)
	 */
	public boolean getMaintainFilterSettings() {
		return chMaintainFilterSettings.isSelected();
	}

	/**
	 * display the dialog
	 */
	public void display() {
		setMinimumSize(getMinimumSize());
		setLocationRelativeTo(compForPosition);
		setVisible(true);
	}

	/**
	 * @return the status after this GUI has closed. One of
	 */
	public int getStatus() {
		return this.status;
	}

	/**
	 * Exit the jdialog and return to the calling frame
	 */
	private void discard() {
		setVisible(false);
		dispose();
	}

	/**
	 * Creates a {@link PreviewResult} object from the current state of the GUI.
	 */
	private void createPreviewResult() {
		this.previewResult = new PreviewResult(filterSettings.copy(), filterEnabled, wiaDataPreview,
				chMaintainFilterSettings.isSelected(), chAllowWrap.isSelected(), chAllowWrapIgnoreEnds.isSelected(),
				_indexPressureAligned, _indexFlowAligned);
	}

	/**
	 * Gets the result of the preview operation.
	 * 
	 * @return The {@link PreviewResult} object containing the final state of the
	 *         preview.
	 */
	public PreviewResult getPreviewResult() {
		return this.previewResult;
	}

	/**
	 * Gets the user's choice on whether to maintain settings for the next preview.
	 * 
	 * @return True if the "maintain settings" checkbox is selected, false
	 *         otherwise.
	 */
	public boolean getMaintainSetting() {
		return this.chMaintainFilterSettings.isSelected();
	}

	/**
	 * Updates the display fields with calculated metrics from the WIA data.
	 */
	private void storeDisplayValues() {
		DecimalFormat nf = new DecimalFormat("0.##");
		if (wiaDataPreview != null) {
			txtCVal.setText(nf.format(wiaDataPreview.getWaveSpeed()) + " m/s");
			txtFlowAvg.setText(nf.format(wiaDataPreview.getAvgFlow(false)) + " m/s");
			txtPressAvg.setText(nf.format(wiaDataPreview.getAvgPressure(true)) + " mmHg");
		}
	}

	/**
	 * Enables or disables the "Run Align" button based on whether alignment points
	 * are selected.
	 * 
	 * @param ready {@code true} to enable the button, {@code false} to disable it.
	 */
	@Override
	public void setReadyAlign(boolean ready) {
		this.btnAlign.setEnabled(ready);
	}

	/**
	 * A container for the results of a wave-picking preview, including filter
	 * settings and alignment data.
	 */
	public static class PreviewResult {
		private final SavGolSettings filterSettings;
		private final boolean filterEnabled;
		private final WIAData wiaData;
		private final boolean allowWrap;
		private final boolean allowWrapIgnoreEnds;
		private final int indexPressureAlign;
		private final int indexFlowAlign;

		/**
		 * Constructs a PreviewResult object to store the state of the preview session.
		 *
		 * @param filterSettings      The Savitzky-Golay filter settings used.
		 * @param filterEnabled       Whether the filter was enabled.
		 * @param wiaData             The resulting WIAData after adjustments.
		 * @param settingsPersist     Not used in current implementation.
		 * @param allowWrap           Whether wrap-around alignment was allowed.
		 * @param allowWrapIgnoreEnds Whether ignoring excessive discordance was
		 *                            allowed.
		 * @param pressureAlign       The alignment index for pressure.
		 * @param flowAlign           The alignment index for flow.
		 */
		private PreviewResult(SavGolSettings filterSettings, boolean filterEnabled, WIAData wiaData,
				boolean settingsPersist, boolean allowWrap, boolean allowWrapIgnoreEnds, int pressureAlign,
				int flowAlign) {
			this.filterSettings = filterSettings;
			this.filterEnabled = filterEnabled;
			this.wiaData = wiaData;
			this.allowWrap = allowWrap;
			this.allowWrapIgnoreEnds = allowWrapIgnoreEnds;
			this.indexPressureAlign = pressureAlign;
			this.indexFlowAlign = flowAlign;
		}

		/**
		 * Gets the Savitzky-Golay filter settings.
		 * 
		 * @return The SavGolSettings object.
		 */
		public SavGolSettings getSettings() {
			return this.filterSettings;
		}

		/**
		 * Checks if the filter was enabled.
		 * 
		 * @return True if the filter was enabled.
		 */
		public boolean isFilterEnabled() {
			return this.filterEnabled;
		}

		/**
		 * Checks if wrap-around alignment was allowed.
		 * 
		 * @return True if wrap-around was allowed.
		 */
		public boolean isAllowWrap() {
			return this.allowWrap;
		}

		/**
		 * Checks if ignoring excessive discordance during wrap-around was allowed.
		 * 
		 * @return True if ignoring excessive discordance was allowed.
		 */
		public boolean isAllowWrapIgnoreEnds() {
			return this.allowWrapIgnoreEnds;
		}

		/**
		 * Gets the resulting WIA data from the preview.
		 * 
		 * @return The WIAData object.
		 */
		public WIAData getWIAData() {
			return this.wiaData;
		}

		/**
		 * Gets the alignment index for the pressure data.
		 * 
		 * @return The pressure alignment index.
		 */
		public int getIndexPressureAlign() {
			return this.indexPressureAlign;
		}

		/**
		 * Gets the alignment index for the flow data.
		 * 
		 * @return The flow alignment index.
		 */
		public int getIndexFlowAlign() {
			return this.indexFlowAlign;
		}
	}

	@Override
	public void updateDisplayForAddedWave(Wave wave) {
		if (wave == null || Double.isNaN(wave.getCumulativeIntensity())) {
			setWaveIntensity(null);
		} else {
			setWaveIntensity(wave.getCumulativeIntensity());

		}

	}

	private static String _formatToOneDecimal(double number) {
		return String.format("%.1f", number);
	}

}
