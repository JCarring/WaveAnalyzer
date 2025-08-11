package com.carrington.WIA.GUIs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.apache.commons.io.FilenameUtils;
import org.jfree.chart.ChartPanel;

import com.carrington.WIA.Utils;
import com.carrington.WIA.Cardio.Wave;
import com.carrington.WIA.DataStructures.HemoData;
import com.carrington.WIA.DataStructures.WIAData;
import com.carrington.WIA.GUIs.WavePickerPreviewGUI.PreviewResult;
import com.carrington.WIA.GUIs.Components.JCButton;
import com.carrington.WIA.GUIs.Components.JCDimension;
import com.carrington.WIA.GUIs.Components.JCHelpButton;
import com.carrington.WIA.GUIs.Components.JCToggleButton;
import com.carrington.WIA.GUIs.Components.WaveTable;
import com.carrington.WIA.GUIs.Components.WaveTable.WaveTableListener;
import com.carrington.WIA.GUIs.Configs.WIASaveSettingsChoices;
import com.carrington.WIA.GUIs.Configs.WIASaveSettingsGUI;
import com.carrington.WIA.Graph.ComboChartSaver;
import com.carrington.WIA.Graph.NetWaveChartPanel;
import com.carrington.WIA.Graph.PressureFlowChartPanel;
import com.carrington.WIA.Graph.PressureFlowChartPanel.PFPickListener;
import com.carrington.WIA.Graph.SepWavePanel;
import com.carrington.WIA.Graph.SepWavePanel.WavePickListener;
import com.carrington.WIA.IO.Header;
import com.carrington.WIA.IO.WIAResourceReader;

/**
 * WavePickerGUI provides a user interface for selecting and processing wave
 * data in a {@link WIAData} object. It allows the user to align pressure and
 * flow data, view various metrics, and save images/selections.
 */
public class WavePickerGUI extends JDialog implements WaveTableListener, WavePickListener, PFPickListener {

	private static final long serialVersionUID = -4214441167859285576L;
	private static final Color pnlLightGray = new Color(213, 213, 213);
	private static final Color pnlDarkGray = new Color(169, 169, 169);

	/** User has accepted the selections. */
	public static final int SELECTION_OK = 0;
	/** User has cancelled the operation. */
	public static final int CANCELLED = 1;
	/** User would like to proceed to the next preview. */
	public static final int PREVIEW_NEXT = 2;
	/** User would like to proceed to the previous preview. */
	public static final int PREVIEW_LAST = 3;

	private JPanel contentPane;
	private JPanel pnlTop;
	private JPanel pnlWIA;
	private JPanel pnlWIAButtons;
	private JPanel pnlWIADisplay;
	private JButton btnAccept;
	private JButton btnReset;
	private JTextField txtCVal;
	private JCHelpButton btnWaveHelp;
	private WaveTable tableWaves;
	private JCHelpButton btnPF;
	private JButton btnAlign;
	private JButton btnResetAlign;
	private JTextField txtFlowAvg;
	private JTextField txtPressAvg;
	private JCHelpButton btnDiameterHelp;
	private JTextField txtVesselDiameter;
	private SepWavePanel pnlGraphWIASep;
	private JPanel pnlBottomGraphs;
	private JSplitPane splitBottom;
	private NetWaveChartPanel pnlGraphWIANet;
	private PressureFlowChartPanel pnlGraphPF;
	private ExpandOverlay pnlBottomRightOverlay;
	private ExpandOverlay pnlBottomLeftOverlay;
	private JCheckBox chSerialize;
	private JCheckBox chImgIncludeFileName;
	private JCToggleButton btnPFModeOff;
	private JCToggleButton btnPFModeAlignPeak;
	private JCToggleButton btnPFModeAlignManual;
	private JCheckBox chAllowWrap;
	private JCheckBox chAllowWrapIgnoreEnds;

	private WeakReference<WavePickerGUI> ref = new WeakReference<WavePickerGUI>(this);

	private WIAData wiaData = null;
	private JCHelpButton btnOverallHelp;

	private WIASaveSettingsGUI saveSettingsGUI;

	private WIACaller wiaCaller;

	private int _indexPressureAligned = -1;
	private int _indexFlowAligned = -1;

	private int status = CANCELLED;
	private JButton btnSave;

	private Component componentParent;

	/**
	 * Create the frame.
	 * 
	 * @param selectionName The name of the selection being processed.
	 * @param wiaData       The {@link WIAData} object to be visualized and
	 *                      manipulated.
	 * @param saveChoices   The choices for saving settings.
	 * @param wiaCaller     The calling interface for WIA operations.
	 * @param parent        The parent component for this dialog.
	 */
	public WavePickerGUI(String selectionName, WIAData wiaData, WIASaveSettingsChoices saveChoices, WIACaller wiaCaller,
			Component parent) {
		this(selectionName, wiaData, saveChoices, wiaCaller, parent, null);
	}

	/**
	 * Create the frame.
	 * 
	 * @param selectionName The name of the selection being processed.
	 * @param wiaData       The {@link WIAData} object to be visualized and
	 *                      manipulated.
	 * @param saveChoices   The choices for saving settings.
	 * @param wiaCaller     The calling interface for WIA operations.
	 * @param parent        The parent component for this dialog.
	 * @param pr            The result from a preview screen, if available.
	 */
	public WavePickerGUI(String selectionName, WIAData wiaData, WIASaveSettingsChoices saveChoices, WIACaller wiaCaller,
			Component parent, PreviewResult pr) {

		if (selectionName == null) {
			throw new IllegalArgumentException("Selection name cannot be null");
		} else if (wiaData == null) {
			throw new IllegalArgumentException("Wave intensity data cannot be null");
		} else if (saveChoices == null) {
			throw new IllegalArgumentException("Saving choices cannot be null");
		}

		this.saveSettingsGUI = new WIASaveSettingsGUI(saveChoices);
		this.wiaData = wiaData;
		this.wiaCaller = wiaCaller;
		this.componentParent = parent;
		setTitle("Pick Waves for Selection '" + selectionName + "' ("
				+ Utils.getShortenedFilePath(wiaData.getData().getFile().getPath(), 80) + ")");
		setModal(true);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 1240, 850); // TODO set whole screen size
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
				if (Utils.confirmAction("Confirm Quit", "Are you sure you want to quit?", rootPane)) {
					status = CANCELLED;
					discard();
				}
			}

			@Override
			public void windowOpened(WindowEvent e) {
				pnlGraphWIASep.displayExistingChoices();
				pnlGraphPF.displayExistingChoices();
			}
		});

		generateWIA(false, pr != null ? pr.isAllowWrap() : true, pr != null ? pr.isAllowWrapIgnoreEnds() : false);

		initMenuBar();
		initPnlTop(selectionName);
		initWIA();
		initPnlDisplaySetting(pr != null ? pr.isAllowWrap() : true, pr != null ? pr.isAllowWrapIgnoreEnds() : false);
		initPnlButtons();
		initKeyMaps();

		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addComponent(pnlTop, GroupLayout.DEFAULT_SIZE, 1240, Short.MAX_VALUE)
				.addGroup(Alignment.TRAILING,
						gl_contentPane.createSequentialGroup().addComponent(pnlWIA, 900, 900, Short.MAX_VALUE)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(pnlWIADisplay,
										GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE))
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
		getRootPane().setDefaultButton(btnAccept);

		setupDisplayTextFields(this.txtCVal, this.txtFlowAvg, this.txtPressAvg);

		contentPane.setLayout(gl_contentPane);
		Utils.unfocusAll(pnlWIAButtons);
		// Utils.unfocusAll(pnlWIA);
		Utils.unfocusAll(pnlWIADisplay);
		Utils.unfocusAll(pnlTop);

		txtVesselDiameter.setEditable(true);
		txtVesselDiameter.setFocusTraversalKeysEnabled(false);
		txtVesselDiameter.setFocusable(false);

		displayExistingChoices();
	}

	/**
	 * Initializes the menu bar for the GUI.
	 */
	private void initMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenu displayMenu = new JMenu("Display");
		JMenu savingMenu = new JMenu("Saving");
		menuBar.add(fileMenu);
		menuBar.add(displayMenu);
		menuBar.add(savingMenu);

		setJMenuBar(menuBar);

		JCheckBoxMenuItem menuItemDisplayAccel = new JCheckBoxMenuItem("Display separated wave accelerations");
		menuItemDisplayAccel.setSelected(true);
		displayMenu.add(menuItemDisplayAccel);
		menuItemDisplayAccel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (menuItemDisplayAccel.isSelected()) {

					ref.get().pnlGraphWIASep.enableDifferencing();
					ref.get().pnlGraphWIANet.enableDifferencing();
				} else {
					ref.get().pnlGraphWIASep.disableDifferencing();
					ref.get().pnlGraphWIANet.disableDifferencing();

				}
			}
		});
		JMenuItem menuItemShowMetrics = new JMenuItem("Show Metrics Info");
		menuItemShowMetrics.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showMetricsPopup();
			}
		});
		menuItemShowMetrics.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.META_MASK));

		fileMenu.add(menuItemShowMetrics);
		fileMenu.addSeparator();
		JMenuItem menuItemAccept = new JMenuItem("Accept");
		menuItemAccept.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnAccept.doClick();
			}
		});
		fileMenu.add(menuItemAccept);
		menuItemAccept.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.META_MASK));
		JMenuItem menuItemReset = new JMenuItem("Reset");
		menuItemReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnReset.doClick();
			}
		});

		fileMenu.add(menuItemReset);
		fileMenu.addSeparator();
		JMenuItem menuItemQuit = new JMenuItem("Quit");
		fileMenu.add(menuItemQuit);
		menuItemQuit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (Utils.confirmAction("Confirm Quit", "Are you sure you want to quit?", rootPane)) {
					discard();
				}
			}
		});

		JMenuItem menuItemSaveSettings = new JMenuItem("Save Settings");
		menuItemSaveSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveSettingsGUI.open(ref.get());
			}
		});
		JMenuItem menuItemSaveImages = new JMenuItem("Save Images");
		menuItemSaveImages.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnSave.doClick();
			}
		});
		menuItemSaveImages.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.META_MASK));
		savingMenu.add(menuItemSaveSettings);
		savingMenu.addSeparator();
		savingMenu.add(menuItemSaveImages);

		Utils.setMenuBarFont(Utils.getSubTitleSubFont(), getJMenuBar());

		// Utils.setFont(Utils.getSubTitleFont(), fileMenu, displayMenu,
		// menuItemDisplayAccel, menuItemQuit,
		// menuItemAccept, menuItemReset, menuItemSaveSettings, savingMenu,
		// menuItemSaveImages);

	}

	/**
	 * Initializes the top panel of the GUI, which displays the selection name and
	 * help button.
	 * 
	 * @param selectionName The name of the current selection.
	 */
	private void initPnlTop(String selectionName) {
		pnlTop = new JPanel();
		FlowLayout flowLayout = (FlowLayout) pnlTop.getLayout();
		flowLayout.setHgap(10);
		pnlTop.setBorder(new LineBorder(new Color(0, 0, 0)));
		pnlTop.setBackground(pnlDarkGray);

		JLabel pnlInstruction = new JLabel("Select waves for \"" + selectionName + "\"");

		btnOverallHelp = new JCHelpButton(WIAResourceReader.getContents(WIAResourceReader.HELP_WAVE_PICKER));

		btnOverallHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Utils.showMessage(Utils.INFO, btnOverallHelp.getHelpMessage(), ref.get());
			}

		});
		btnOverallHelp.setFocusable(false);
		pnlInstruction.setFont(Utils.getSubTitleFont());
		pnlTop.add(pnlInstruction);
		pnlTop.add(btnOverallHelp);

	}

	/**
	 * Initializes the main WIA panel containing the separated wave, net wave, and
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

		pnlGraphWIASep = SepWavePanel.generate(wiaData, Utils.getTextFont(false), false);
		pnlGraphWIASep.setWavePickListener(this);

		pnlGraphWIASep.setBorder(new LineBorder(new Color(0, 0, 0)));
		splitTop.setLeftComponent(pnlGraphWIASep);

		pnlBottomGraphs = new JPanel();
		splitTop.setRightComponent(pnlBottomGraphs);

		// Bottom graphs
		pnlGraphWIANet = NetWaveChartPanel.generate(wiaData, Utils.getTextFont(false));
		pnlGraphWIANet.setBorder(new LineBorder(new Color(0, 0, 0)));
		pnlGraphPF = PressureFlowChartPanel.generate(wiaData, false, Utils.getTextFont(false));
		pnlGraphPF.setBorder(new LineBorder(new Color(0, 0, 0)));
		pnlGraphPF.setCyclePickListener(this);

		final int COLLAPSE_AT_PX = 20;
		final int NORMAL_DIVIDER = 8;

		// Wrap the pressure/flow side with the edge overlay (button pointing right)
		pnlBottomRightOverlay = new ExpandOverlay(pnlGraphPF, 0, "▶", "Show net wave intensity", () -> {
			if (splitBottom.getLeftComponent() == null) {
				splitBottom.setLeftComponent(pnlBottomLeftOverlay);
			}
			splitBottom.setDividerSize(NORMAL_DIVIDER);
			splitBottom.revalidate();
			splitBottom.repaint();
			splitBottom.setDividerLocation(0.5f);
			pnlBottomRightOverlay.setButtonVisible(false);
		});
		pnlBottomRightOverlay.setButtonVisible(false);

		// Wrap the net wave intensity side with the edge overlay (button pointing left)
		pnlBottomLeftOverlay = new ExpandOverlay(pnlGraphWIANet, 1, "◀", "Show pressure and flow", () -> {
			if (splitBottom.getRightComponent() == null) {
				splitBottom.setRightComponent(pnlBottomRightOverlay);
			}
			splitBottom.setDividerSize(NORMAL_DIVIDER);
			splitBottom.revalidate();
			splitBottom.repaint();
			splitBottom.setDividerLocation(0.5f);
			pnlBottomLeftOverlay.setButtonVisible(false);
		});
		pnlBottomLeftOverlay.setButtonVisible(false);

		splitBottom = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlBottomLeftOverlay, pnlBottomRightOverlay);
		splitBottom.setContinuousLayout(true);
		splitBottom.setResizeWeight(0.5);
		splitBottom.setDividerSize(NORMAL_DIVIDER);

		GroupLayout gl_pnlBottomGraphs = new GroupLayout(pnlBottomGraphs);
		gl_pnlBottomGraphs.setHorizontalGroup(gl_pnlBottomGraphs.createParallelGroup(Alignment.LEADING)
				.addComponent(splitBottom, GroupLayout.DEFAULT_SIZE, 961, Short.MAX_VALUE));
		gl_pnlBottomGraphs.setVerticalGroup(gl_pnlBottomGraphs.createParallelGroup(Alignment.LEADING)
				.addComponent(splitBottom, GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE));

		pnlBottomGraphs.setLayout(gl_pnlBottomGraphs);

		// Collapse/expand by dragging near edges
		splitBottom.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e -> {
			int loc = splitBottom.getDividerLocation();
			int max = splitBottom.getMaximumDividerLocation();

			boolean leftCollapsed = splitBottom.getLeftComponent() == null;
			boolean rightCollapsed = splitBottom.getRightComponent() == null;

			// Collapse left if dragged near left edge
			if (loc <= COLLAPSE_AT_PX && !leftCollapsed) {
				splitBottom.setLeftComponent(null);
				splitBottom.setDividerLocation(0);
				splitBottom.setDividerSize(0); // fully hide divider
				splitBottom.revalidate();
				splitBottom.repaint();
				pnlBottomRightOverlay.setButtonVisible(true); // show in-panel pop-out control
			} else if (loc > COLLAPSE_AT_PX && leftCollapsed) {
				splitBottom.setLeftComponent(pnlBottomLeftOverlay);
				splitBottom.setDividerSize(NORMAL_DIVIDER);
				splitBottom.revalidate();
				splitBottom.repaint();
				splitBottom.setDividerLocation(0.5f);
				pnlBottomRightOverlay.setButtonVisible(false);
			}

			if ((max - loc) <= COLLAPSE_AT_PX && !rightCollapsed) {
				splitBottom.setRightComponent(null);
				splitBottom.setDividerLocation(max);
				splitBottom.setDividerSize(0); // fully hide divider
				splitBottom.revalidate();
				splitBottom.repaint();
				pnlBottomLeftOverlay.setButtonVisible(true); // show in-panel pop-out control
			} else if ((max - loc) > COLLAPSE_AT_PX && rightCollapsed) {
				splitBottom.setRightComponent(pnlBottomRightOverlay);
				splitBottom.setDividerSize(NORMAL_DIVIDER);
				splitBottom.revalidate();
				splitBottom.repaint();
				splitBottom.setDividerLocation(0.5f);
				pnlBottomLeftOverlay.setButtonVisible(false);
			}

		});

		pnlWIA.setLayout(gl_pnlWIA);
	}

	/**
	 * Initializes the right-side panel that displays metrics and settings for
	 * alignment and data manipulation.
	 * 
	 * @param allowAlignWrap                     Boolean indicating if wrap-around
	 *                                           alignment is allowed.
	 * @param allowAlignWrapExcessiveDiscordance Boolean indicating if excessive
	 *                                           discordance should be ignored
	 *                                           during wrap-around alignment.
	 */
	private void initPnlDisplaySetting(boolean allowAlignWrap, boolean allowAlignWrapExcessiveDiscordance) {
		pnlWIADisplay = new JPanel();
		pnlWIADisplay.setBackground(pnlLightGray);
		pnlWIADisplay.setBorder(new LineBorder(new Color(0, 0, 0)));

		JLabel lblMetrics = new JLabel("Metrics");

		JLabel lblCVal = new JLabel("C-value:");
		JLabel lblAvgFlow = new JLabel("Avg Flow:");
		JLabel lblAvgPress = new JLabel("Avg Pressure:");
		JLabel lblOtherValues = new JLabel("<html><u>Other...</u></html>");
		lblOtherValues.setForeground(new java.awt.Color(0, 102, 204)); // optional: link-ish color
		lblOtherValues.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

		lblOtherValues.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				showMetricsPopup();
			}
		});

		txtCVal = new JTextField();
		txtFlowAvg = new JTextField();
		txtPressAvg = new JTextField();

		JLabel lblExistingWaves = new JLabel("Waves");

		btnWaveHelp = new JCHelpButton(WIAResourceReader.getContents(WIAResourceReader.HELP_WAVES));

		btnWaveHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Utils.showMessage(Utils.INFO, btnWaveHelp.getHelpMessage(), ref.get());
			}

		});

		JScrollPane scrWaves = new JScrollPane();

		JLabel lblPF = new JLabel("Pressure and Flow");
		btnPF = new JCHelpButton(WIAResourceReader.getContents(WIAResourceReader.HELP_WAVE_ALIGN_PF));
		btnPF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				Utils.showMessage(Utils.INFO, btnPF.getHelpMessage(), ref.get());
			}
		});

		JLabel lblSelectionMode = new JLabel("Selection mode");

		btnAlign = new JCButton("Run Align", JCButton.BUTTON_SMALL);
		Utils.setEnabled(false, false, btnAlign);
		btnAlign.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				runAlignPressureFlow();

			}

		});

		btnResetAlign = new JCButton("Reset Align", JCButton.BUTTON_SMALL);

		btnResetAlign.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				resetAlignPressureFlow();

			}

		});
		// Strings separately for switch statements
		final String strModOff = " Off ";
		final String strModCycle = " Cycle ";
		final String strModAlignPeak = " Align (Peak) ";
		final String strModAlignManual = " Align (Manual) ";

		btnPFModeOff = new JCToggleButton(strModOff, JCToggleButton.BUTTON_SMALL);
		JCToggleButton btnPFModeCycle = new JCToggleButton(strModCycle, JCToggleButton.BUTTON_SMALL);
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
					case strModCycle:
						pnlGraphPF.setSelectMode(PressureFlowChartPanel.MODE_CYCLE);
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

		JSeparator sep1 = new JSeparator(JSeparator.HORIZONTAL);
		sep1.setBackground(new Color(192, 192, 192));
		sep1.setForeground(new Color(192, 192, 192));

		chAllowWrap = new JCheckBox("Wrap-around when align");
		chAllowWrap.setOpaque(false);
		chAllowWrap.setSelected(true);
		chAllowWrapIgnoreEnds = new JCheckBox("<html>Ignore excessive discordance<br> on wrap-around</html>");
		chAllowWrapIgnoreEnds.setOpaque(false);

		if (wiaData.hasOriginal()) {
			Utils.setEnabled(true, false, btnResetAlign);
			Utils.setEnabled(false, false, btnPFModeAlignManual, btnPFModeAlignPeak);
		} else {
			Utils.setEnabled(false, false, btnResetAlign);
		}

		// Add the listener to each toggle button
		btnPFModeOff.addItemListener(btnPFListener);
		btnPFModeCycle.addItemListener(btnPFListener);
		btnPFModeAlignPeak.addItemListener(btnPFListener);
		btnPFModeAlignManual.addItemListener(btnPFListener);

		ButtonGroup group = new ButtonGroup();
		group.add(btnPFModeOff);
		group.add(btnPFModeCycle);
		group.add(btnPFModeAlignPeak);
		group.add(btnPFModeAlignManual);
		btnPFModeOff.setSelected(true);

		JSeparator sep2 = new JSeparator(JSeparator.HORIZONTAL);
		sep2.setBackground(new Color(192, 192, 192));
		sep2.setForeground(new Color(192, 192, 192));

		JCButton btnResetSystole = new JCButton("Reset", JCButton.BUTTON_SMALL);
		JCButton btnResetDiastole = new JCButton("Reset", JCButton.BUTTON_SMALL);

		btnResetSystole.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pnlGraphPF.resetSystole();
			}

		});
		btnResetDiastole.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pnlGraphPF.resetDiastole();
			}

		});

		JLabel lblDiameter = new JLabel("Other");
		btnDiameterHelp = new JCHelpButton(WIAResourceReader.getContents(WIAResourceReader.HELP_VESSEL_DIAMETER));

		btnDiameterHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Utils.showMessage(Utils.INFO, btnDiameterHelp.getHelpMessage(), ref.get());
			}

		});
		JLabel lblDiamTxtField = new JLabel("Vessel Diameter (mm)");
		txtVesselDiameter = new JTextField();
		txtVesselDiameter.setEditable(true);
		txtVesselDiameter.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				txtVesselDiameter.setFocusable(true);
				txtVesselDiameter.requestFocusInWindow();
			}
		});
		pnlWIADisplay.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {
				txtVesselDiameter.setFocusable(false);
				pnlWIADisplay.requestFocusInWindow(); // Shift focus away
			}
		});

		JLabel lblSaving = new JLabel("Saving");
		btnSave = new JButton("Save Images");
		btnSave.setEnabled(true);
		chImgIncludeFileName = new JCheckBox("Include File Name");
		chImgIncludeFileName.setOpaque(false);
		chImgIncludeFileName.setSelected(true);
		chImgIncludeFileName.setEnabled(true);
		btnSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selName = wiaData.getSelectionName();

				if (wiaCaller == null)
					return;

				if (saveSettingsGUI.getChoices().getSaveSVGTIFF()) {

					File fileSVG = wiaCaller.getWIAImageFileSVG();
					File fileTIFF = wiaCaller.getWIAImageFolderTIFF();

					ComboChartSaver comboSaver = new ComboChartSaver(fileSVG, fileTIFF,
							saveSettingsGUI.getChoices().getSaveFont(), wiaData.getTime());

					JCDimension dims = saveSettingsGUI.getChoices().getSaveDimensions();
					String printPicName = null;
					if (chImgIncludeFileName.isSelected()) {
						printPicName = FilenameUtils.removeExtension(wiaData.getData().getFileName()) + " (" + selName
								+ ")";
					}

					try {
						comboSaver.saveSepWavePressFlow(printPicName, dims.getWidth(), dims.getHeight(),
								wiaData.getRawPressure(), true, wiaData.getRawFlow(), wiaData.getWIForward(),
								wiaData.getWIBackward(), wiaData.getSepFlowForwardDeriv(),
								wiaData.getSepFlowBackwardDeriv(), new double[] { 2, 1 });
					} catch (Exception e1) {
						Utils.showMessage(Utils.ERROR, "Could not save to file. System error msg: " + e1.getMessage(),
								ref.get());
						btnSave.setIcon(Utils.IconFail);
						e1.printStackTrace();
						return;
					}
					btnSave.setIcon(Utils.IconSuccess);

				}

				if (saveSettingsGUI.getChoices().getSaveSelections()) {

					File fileToSaveDisplay = wiaCaller.getWIAWaveSelectionsFileSVG();
					try {
						pnlGraphWIASep.saveChartAsSVG(fileToSaveDisplay);

					} catch (Exception e1) {
						Utils.showMessage(Utils.ERROR, "Could not save to file. System error msg: " + e1.getMessage(),
								ref.get());
						btnSave.setIcon(Utils.IconFail);
						e1.printStackTrace();
						return;
					}
					btnSave.setIcon(Utils.IconSuccess);
				}

			}
		});

		chAllowWrap.setSelected(allowAlignWrap);
		chAllowWrapIgnoreEnds.setSelected(allowAlignWrapExcessiveDiscordance);

		GroupLayout gl_pnlWIADisplay = new GroupLayout(pnlWIADisplay);

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
				.addGroup(gl_pnlWIADisplay.createSequentialGroup()
						.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(lblOtherValues, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addContainerGap())
				.addGroup(gl_pnlWIADisplay.createSequentialGroup()
						.addComponent(lblExistingWaves, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnWaveHelp,
								GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGroup(
						gl_pnlWIADisplay.createSequentialGroup().addContainerGap()
								.addComponent(scrWaves, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addContainerGap())
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addComponent(lblPF)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnPF)
						.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(lblSelectionMode))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(btnPFModeOff)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnPFModeCycle)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnPFModeAlignPeak)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnPFModeAlignManual))
				.addGroup(
						gl_pnlWIADisplay.createSequentialGroup().addContainerGap()
								.addComponent(sep1, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addContainerGap())
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(chAllowWrap))
				.addGroup(
						gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(chAllowWrapIgnoreEnds))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(btnAlign)
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(btnResetAlign))
				.addGroup(
						gl_pnlWIADisplay.createSequentialGroup().addContainerGap()
								.addComponent(sep2, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addContainerGap())

				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(btnAlign))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addComponent(lblDiameter)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnDiameterHelp)
						.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(lblDiamTxtField)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(txtVesselDiameter, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE)
						.addContainerGap())
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addComponent(lblSaving))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap()
						.addComponent(btnSave, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(chImgIncludeFileName, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))));

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
						.addGap(2).addComponent(lblOtherValues).addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.CENTER).addComponent(lblExistingWaves)
								.addComponent(btnWaveHelp))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(scrWaves, GroupLayout.PREFERRED_SIZE, 138, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlWIADisplay
								.createParallelGroup(Alignment.CENTER).addComponent(lblPF).addComponent(btnPF))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(lblSelectionMode, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.CENTER).addComponent(btnPFModeOff)
								.addComponent(btnPFModeCycle).addComponent(btnPFModeAlignPeak)
								.addComponent(btnPFModeAlignManual))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(sep1, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(chAllowWrap)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(chAllowWrapIgnoreEnds)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.CENTER).addComponent(btnAlign)
								.addComponent(btnResetAlign))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(sep2, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.CENTER).addComponent(lblDiameter)
								.addComponent(btnDiameterHelp))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.CENTER).addComponent(lblDiamTxtField)
								.addComponent(txtVesselDiameter, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(lblSaving)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.CENTER)
								.addComponent(chImgIncludeFileName).addComponent(btnSave))
						.addContainerGap()
						.addPreferredGap(ComponentPlacement.UNRELATED, Short.MAX_VALUE, Short.MAX_VALUE)));

		tableWaves = WaveTable.generate(this, false);

		scrWaves.setViewportView(tableWaves);
		tableWaves.setFillsViewportHeight(true);

		Utils.setFont(Utils.getSubTitleFont(), lblMetrics, lblPF, lblDiameter, lblExistingWaves, lblSaving);
		Utils.setFont(Utils.getTextFont(false), btnSave);
		Utils.setFont(Utils.getSubTitleSubFont(), lblSelectionMode);

		Utils.setFont(Utils.getSmallTextFont(), lblCVal, lblAvgFlow, lblAvgPress, txtCVal, txtFlowAvg, txtPressAvg,
				lblDiamTxtField, txtVesselDiameter, chImgIncludeFileName, chAllowWrap, chAllowWrapIgnoreEnds,
				lblOtherValues);

		pnlWIADisplay.setLayout(gl_pnlWIADisplay);

		updateDisplayValues();
		Utils.unfocusButtons(contentPane);

	}

	/**
	 * Initializes the bottom button panel containing the "Reset" and "Accept"
	 * buttons.
	 */
	private void initPnlButtons() {

		pnlWIAButtons = new JPanel();
		pnlWIAButtons.setBackground(pnlLightGray);

		btnReset = new JButton("Reset");
		btnReset.setBackground(new Color(255, 167, 155));
		btnReset.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (Utils.confirmAction("Confirm", "Are you sure you want to reset data?", ref.get())) {
					reset();
				}

			}

		});

		btnAccept = new JButton("Accept");
		btnAccept.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!wiaData.isUserSelectionAdequate()) {
					Utils.showMessage(Utils.WARN,
							"It is recommended you select at least two waves, and point of systole and diastole.",
							ref.get());
				}

				if (!validateFields()) {
					return;
				}

				status = SELECTION_OK;
				discard();

			}

		});
		btnAccept.setBackground(new Color(157, 249, 152));

		chSerialize = new JCheckBox("Serialize state on accept");
		chSerialize.setHorizontalTextPosition(SwingConstants.LEFT);
		chSerialize.setIconTextGap(8);
		chSerialize.setSelected(true);
		chSerialize.setOpaque(false);
		chSerialize.setEnabled(true);

		Utils.setFont(Utils.getSubTitleFont(), btnReset, btnAccept);
		Utils.setFont(Utils.getTextFont(true), chSerialize);

		pnlWIAButtons.add(chSerialize);
		pnlWIAButtons.add(btnReset);
		pnlWIAButtons.add(btnAccept);

		FlowLayout flowLayout = (FlowLayout) pnlWIAButtons.getLayout();
		flowLayout.setHgap(10);
		flowLayout.setAlignment(FlowLayout.TRAILING);
		pnlWIAButtons.setBorder(new LineBorder(new Color(0, 0, 0)));
	}

	/**
	 * Creates action mapping for specific keyboard keys
	 */
	private void initKeyMaps() {

		JRootPane pane = getRootPane();
		InputMap inputMap = pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = pane.getActionMap();

		// Keys to bind
		int[] keys = { KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_P, KeyEvent.VK_F, KeyEvent.VK_R, KeyEvent.VK_E,
				KeyEvent.VK_SPACE, KeyEvent.VK_Y, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
				KeyEvent.VK_5, KeyEvent.VK_6 };

		for (int key : keys) {
			String actionName = "pressed" + key;
			KeyStroke keyStroke = KeyStroke.getKeyStroke(key, 0, false);

			inputMap.put(keyStroke, actionName);
			actionMap.put(actionName, new AbstractAction() {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					Point mousePoint = MouseInfo.getPointerInfo().getLocation();
					KeyActionReceiver receiver = getChartPanelAtPoint(mousePoint);
					if (receiver != null) {
						receiver.keyPressed(key); // This method should exist in your KeyActionReceiver interface/class
					}
				}
			});
		}

	}

	/**
	 * Sets up specified text fields to be non-editable and non-focusable.
	 * 
	 * @param fields The text fields to configure.
	 */
	private void setupDisplayTextFields(JTextField... fields) {
		for (JTextField field : fields) {
			field.setEditable(false);
			field.setFocusable(false);
		}
	}

	/**
	 * Displays the existing wave and cycle choices from the WIAData object when the
	 * GUI is first opened.
	 */
	private void displayExistingChoices() {
		if (!this.wiaData.getWaves().isEmpty()) {
			for (Wave wave : this.wiaData.getWaves()) {
				updateDisplayForAddedWave(wave);
			}
		}

		// the panels will handle their own modifications for startup
	}

	/**
	 * Creates a popup which can be displayed, showing the available metrics
	 */
	private void showMetricsPopup() {

		wiaData.retryCalculations();

		DecimalFormat nf = new DecimalFormat("0.##");

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");

		sb.append("<b>C-value: </b>").append(nf.format(wiaData.getWaveSpeed())).append(" m/s<br>");
		sb.append("<b>Average flow: </b>").append(nf.format(wiaData.getAvgFlow(false))).append(" m/s<br>");
		sb.append("<b>Average pressure: </b>").append(nf.format(wiaData.getAvgPressure(true))).append(" mmHg<br>");
		sb.append("<b>Resistance: </b>").append(nf.format(wiaData.getResistanceOverall())).append(" mmHg/cm/s<br>");
		if (_isValidMetric(wiaData.getSystoleTime())) {
			sb.append("<b>Systole time: </b>").append(nf.format(wiaData.getSystoleTime())).append(" ms<br>");
		}
		if (_isValidMetric(wiaData.getResistanceSystole())) {
			sb.append("<b>Systole resistance: </b>").append(nf.format(wiaData.getResistanceSystole()))
					.append(" mmHg/cm/s<br>");
		}
		if (_isValidMetric(wiaData.getDiastoleTime())) {
			sb.append("<b>Diastole time: </b>").append(nf.format(wiaData.getDiastoleTime())).append(" ms<br>");
		}
		if (_isValidMetric(wiaData.getResistanceDiastole())) {
			sb.append("<b>Diastole resistance: </b>").append(nf.format(wiaData.getResistanceDiastole()))
					.append(" ms<br>");
		}

		Double cycleDuration = wiaData.getCycleDuration();
		if (_isValidMetric(cycleDuration)) {
			sb.append("<b>Cycle duration: </b>").append(nf.format(cycleDuration)).append(" ms<br>");

		}

		Double diastoleDuration = wiaData.getDiastoleDuration();
		if (_isValidMetric(diastoleDuration)) {
			sb.append("<b>Diastole duration: </b>").append(nf.format(diastoleDuration)).append(" ms<br>");

		}

		Double diastoleToFlowDuration = wiaData.getDiastoleToFlowPeakDuration();
		if (_isValidMetric(diastoleToFlowDuration)) {
			sb.append("<b>Diastole to peak flow duration: </b>").append(nf.format(diastoleToFlowDuration))
					.append(" ms<br>");

		}

		if (wiaData.getVesselDiameter() != null) {
			sb.append("<b>Vessel diameter: </b>").append(nf.format(wiaData.getVesselDiameter())).append(" mm<br>");
		}
		sb.append("</html>");

		Utils.showMessageTallFirst(Utils.INFO, sb.toString(), ref.get());

	}

	/**
	 * @param d query number
	 * @return if input {@link Double} is valid, meaning NOT null and NOT
	 *         {@link Double#NaN}
	 */
	private boolean _isValidMetric(Double d) {
		return d != null && !Double.isNaN(d);
	}

	/**
	 * Generates the WIA data based on current settings and updates the GUI.
	 * 
	 * @return {@code true} if the WIA generation was successful, {@code false}
	 *         otherwise.
	 */
	private boolean generateWIA() {
		return generateWIA(true, chAllowWrap.isSelected(), chAllowWrapIgnoreEnds.isSelected());
	}

	/**
	 * Generates WIA and updates fields and graphs
	 */
	private boolean generateWIA(boolean update, boolean allowAlignWrap, boolean allowAlignWrapExcessivelyDiscordant) {

		if (_indexFlowAligned != -1 && _indexPressureAligned != -1) {
			// the new, aligned (after +/- filtering as above) data.
			HemoData newHD = _getAlignPressureFlowAfterReFilter(wiaData, _indexFlowAligned, _indexPressureAligned,
					allowAlignWrap, allowAlignWrapExcessivelyDiscordant);
			if (newHD == null) {
				// error, already displaced
				return false;
			}
			wiaData.setNewHemoData(newHD); // also will re-analyze
		}

		if (update) {
			_applyUpdatedWIA();

		}

		return true;

	}

	/**
	 * ONLY to be called by {@link WavePickerGUI#generateWIA(boolean)} as a helper
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
			Utils.showMessage(Utils.ERROR, e.getMessage(), this);
			return null;
		}
		return hd;

	}

	/**
	 * Executes the alignment of pressure and flow data based on user selections.
	 */
	private void runAlignPressureFlow() {

		Double timeAlignFlow = pnlGraphPF.getFlowAlignTime();
		Double timeAlignPressure = pnlGraphPF.getPressureAlignTime();
		double[] xData = wiaData.getData().getXData();
		if (timeAlignFlow == null || timeAlignPressure == null) {
			Utils.showMessage(Utils.ERROR, "Please set a time to align in both flow and pressure graphs", pnlGraphPF);
			return;
		}

		int indexFlow = Utils.getClosestIndex(timeAlignFlow, xData);
		int indexPressure = Utils.getClosestIndex(timeAlignPressure, xData);

		this._indexFlowAligned = indexFlow;
		this._indexPressureAligned = indexPressure;

		boolean success = generateWIA();
		if (success) {
			Utils.setEnabled(false, false, btnAlign, btnResetAlign, btnPFModeAlignManual, btnPFModeAlignPeak);
			Utils.setEnabled(true, false, btnResetAlign);
			btnPFModeOff.doClick();
			tableWaves.removeAllWaves();
		}

	}

	/**
	 * Updated the 3 graphs after having called the
	 * {@link WavePickerPreviewGUI#generateWIA(boolean)} method
	 */
	private void _applyUpdatedWIA() {
		pnlGraphPF.resetWIAData(wiaData);
		pnlGraphWIASep.resetWIAData(wiaData);
		pnlGraphWIANet.resetWIAData(wiaData);
		updateDisplayValues();
	}

	/**
	 * display the jdialog
	 */
	public void display() {
		setLocationRelativeTo(componentParent);
		setVisible(true);
	}

	/**
	 * Returns the final status of the dialog (e.g. {@link #SELECTION_OK},
	 * {@link #CANCELLED}, {@link #PREVIEW_NEXT}, or {@link #PREVIEW_LAST})
	 * 
	 * @return The status code.
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
	 * Checks if the user has opted to serialize the WIA data upon accepting.
	 * 
	 * @return {@code true} if the serialize checkbox is selected, {@code false}
	 *         otherwise.
	 */
	public boolean serializeWIAData() {
		return this.chSerialize.isSelected();
	}

	/**
	 * Resets all user selections in the GUI, including cycle points and selected
	 * waves.
	 */
	private void reset() {
		this.pnlGraphPF.resetAllSelections();
		this.txtVesselDiameter.setText("");
		this.pnlGraphWIASep.removeAllWaves();
		this.tableWaves.removeAllWaves();

	}

	/**
	 * Resets the pressure-flow alignment to its original state.
	 */
	public void resetAlignPressureFlow() {

		pnlGraphPF.resetAlignSelections();

		wiaData.revertToOriginalHemoData();

		_indexFlowAligned = -1;
		_indexPressureAligned = -1;

		_applyUpdatedWIA();
		Utils.setEnabled(false, false, btnAlign, btnResetAlign);
		Utils.setEnabled(true, false, btnPFModeAlignManual, btnPFModeAlignPeak);

	}

	/**
	 * Updates the calculated display values (C-value, avg flow, avg pressure,
	 * vessel diameter) in their respective text fields.
	 */
	private void updateDisplayValues() {
		DecimalFormat nf = new DecimalFormat("0.##");
		if (this.wiaData != null) {
			this.txtCVal.setText(nf.format(wiaData.getWaveSpeed()) + " m/s");
			this.txtFlowAvg.setText(nf.format(wiaData.getAvgFlow(false)) + " m/s");
			this.txtPressAvg.setText(nf.format(wiaData.getAvgPressure(true)) + " mmHg");
			Double vesselDiam = wiaData.getVesselDiameter();
			if (vesselDiam != null) {
				this.txtVesselDiameter.setText(nf.format(wiaData.getVesselDiameter()));
			} else {
				this.txtVesselDiameter.setText("");
			}
		}
	}

	/**
	 * Finds the specific ChartPanel instance (which is a {@link KeyActionReceiver})
	 * at a given point within the dialog.
	 * <p>
	 * This method works by first finding the deepest visible component at the
	 * specified coordinates and then walking up the component hierarchy to check if
	 * it is, or is contained within, one of the three primary chart panels.
	 *
	 * @param p The point in the coordinate system of the WavePickerGUI dialog.
	 * @return The specific KeyActionReceiver instance at the point, or {@code null}
	 *         if the point is not over any of them.
	 */
	private KeyActionReceiver getChartPanelAtPoint(Point p) {
		
		Rectangle chartBoundsPF = pnlGraphPF.isDisplayable() ? new Rectangle(pnlGraphPF.getLocationOnScreen(), pnlGraphPF.getSize()) : null;
		Rectangle chartBoundsNet = pnlGraphWIANet.isDisplayable() ? new Rectangle(pnlGraphWIANet.getLocationOnScreen(), pnlGraphWIANet.getSize()) : null;
		Rectangle chartBoundsSep = pnlGraphWIASep.isDisplayable() ? new Rectangle(pnlGraphWIASep.getLocationOnScreen(), pnlGraphWIASep.getSize()) : null;

		if (chartBoundsPF != null && chartBoundsPF.contains(p)) {
			return pnlGraphPF;
		} else if (chartBoundsNet != null && chartBoundsNet.contains(p)) {
			return pnlGraphWIANet;
		} else if (chartBoundsSep != null && chartBoundsSep.contains(p)) {
			return pnlGraphWIASep;
		} else {
			return null;
		}
	}

	/**
	 * 
	 * @return true if validation successful, false otherwise
	 */
	private boolean validateFields() {
		String input = txtVesselDiameter.getText().trim();
		if (!input.isEmpty()) {
			try {
				double value = Double.parseDouble(input);

				if (value < 0) {
					Utils.showMessage(Utils.ERROR, "Vessel diameter cannot be negative!", ref.get());
					return false;
				} else if (value > 20) {
					boolean confirmed = Utils.confirmAction("Confirm diameter",
							"It is odd for a vessel diameter to be > 20 mm. Is this correct?", ref.get());
					if (!confirmed) {
						return false;
					}
				}
				wiaData.setVesselDiameter(value);
			} catch (NumberFormatException ex) {
				Utils.showMessage(Utils.ERROR, "Invalid number format for vessel diameter!", ref.get());
				return false;
			}
		}

		return true;
	}

	/**
	 * Callback method from {@link WavePickListener} to update the display when a
	 * new wave is added.
	 * 
	 * @param wave The wave that was added.
	 */
	@Override
	public void updateDisplayForAddedWave(Wave wave) {

		tableWaves.addWave(wave);
	}

	/**
	 * Callback method from {@link WavePickListener} to update the display when a
	 * wave is remove.
	 * 
	 * @param wave The wave that was removed.
	 */
	@Override
	public void updateDisplayForRemovedWave(Wave wave) {

		tableWaves.removeWave(wave);
	}

	/**
	 * Callback method from {@link WaveTableListener} to remove a wave from the
	 * display.
	 * 
	 * @param wave The wave to be removed.
	 */
	@Override
	public void removeWave(Wave wave) {
		this.pnlGraphWIASep.removeWave(wave);

	}

	/**
	 * Callback method from {@link PFPickListener} to enable or disable the
	 * alignment button.
	 * 
	 * @param ready True if the alignment can be run, false otherwise.
	 */
	@Override
	public void setReadyAlign(boolean ready) {
		Utils.setEnabled(ready, false, this.btnAlign);
	}

	/**
	 * Overlay that keeps a floating button exactly on the component’s outer edge.
	 */
	private static class ExpandOverlay extends JLayeredPane {
		private static final long serialVersionUID = -4428152774989371406L;
		private final ChartPanel panel;
		private final int edge;
		private final JPanel pill; // translucent background for the button
		private final JButton edgeButton;

		private final int buttonWidth = 22;
		private final int buttonHeight = 48;

		/**
		 * Creates a new overlay, containing a {@link ChartPanel}, which then has an
		 * arrow on top of it that can be used to expand another panel in
		 * {@link JSplitPane}
		 * 
		 * @param panel   the chart panel of interest
		 * @param edge    the edge in which the arrow should be placed (0 = left, 1 =
		 *                right)
		 * @param label
		 * @param tooltip
		 * @param onClick
		 */
		private ExpandOverlay(ChartPanel panel, int edge, String label, String tooltip, Runnable onClick) {
			this.panel = panel;
			this.edge = edge;

			setLayout(null); // absolute positioning
			setOpaque(false);

			// --- Edge button on a small "pill" panel ---
			edgeButton = new JButton(label);
			edgeButton.setToolTipText(tooltip);
			edgeButton.setFocusable(false);
			edgeButton.setMargin(new Insets(0, 0, 0, 0));
			edgeButton.setBorder(BorderFactory.createEmptyBorder());
			edgeButton.setContentAreaFilled(false);
			edgeButton.addActionListener(e -> onClick.run());

			pill = new JPanel(new GridBagLayout()) {
				private static final long serialVersionUID = 1643928135231180860L;

				@Override
				protected void paintComponent(Graphics g) {
					super.paintComponent(g);
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(new Color(0, 0, 0, 60));
					g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 12, 12);
					g2.dispose();
				}
			};
			pill.setOpaque(false);
			pill.add(edgeButton);
			pill.setVisible(false); // hidden until collapsed

			add(panel, JLayeredPane.DEFAULT_LAYER);
			add(pill, JLayeredPane.PALETTE_LAYER);

			pill.setSize(buttonWidth, buttonHeight);
			pill.setPreferredSize(new Dimension(buttonWidth, buttonHeight));

			// If the content’s preferred size changes, propagate it upward.
			panel.addPropertyChangeListener("preferredSize", evt -> {
				revalidate(); // ask parent layout to run again
			});
		}

		/** Show/hide the edge button overlay */
		private void setButtonVisible(boolean visible) {
			pill.setVisible(visible);
			revalidate();
			repaint();
		}

		@Override
		public Dimension getPreferredSize() {
			return panel.getPreferredSize();
		}

		@Override
		public Dimension getMinimumSize() {
			return panel.getMinimumSize();
		}

		@Override
		public Dimension getMaximumSize() {
			return panel.getMaximumSize();
		}

		@Override
		public void doLayout() {
			panel.setBounds(0, 0, getWidth(), getHeight());

			int x = (edge == 0) ? 0 : getWidth() - buttonWidth;
			int y = Math.max(0, (getHeight() - buttonHeight) / 2);
			pill.setBounds(x, y, buttonWidth, buttonHeight);
		}
	}

}
