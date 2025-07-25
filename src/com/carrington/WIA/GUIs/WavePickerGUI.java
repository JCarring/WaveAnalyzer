package com.carrington.WIA.GUIs;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
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

import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.apache.commons.io.FilenameUtils;

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
import com.carrington.WIA.IO.WIAResourceReader;
import com.carrington.WIA.IO.Header;

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
	private JTextField txtSystole;
	private JTextField txtDiastole;
	private JTextField txtFlowAvg;
	private JTextField txtPressAvg;
	private JCHelpButton btnDiameterHelp;
	private JTextField txtVesselDiameter;
	private SepWavePanel pnlGraphWIASep;
	private JPanel pnlBottomGraphs;
	private JSplitPane splitBottom;
	private NetWaveChartPanel pnlGraphWIANet;
	private PressureFlowChartPanel pnlGraphPF;
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

		setupDisplayTextFields(this.txtCVal, this.txtFlowAvg, this.txtPressAvg, this.txtSystole, this.txtDiastole);

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

		Utils.setFont(Utils.getSubTitleFont(), fileMenu, displayMenu, menuItemDisplayAccel, menuItemQuit,
				menuItemAccept, menuItemReset, menuItemSaveSettings, savingMenu, menuItemSaveImages);

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
				Utils.showInfo(btnOverallHelp.getHelpMessage(), ref.get());
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

		splitBottom = new JSplitPane();
		splitBottom.setResizeWeight(0.5);

		splitBottom.setContinuousLayout(true);
		GroupLayout gl_pnlBottomGraphs = new GroupLayout(pnlBottomGraphs);
		gl_pnlBottomGraphs.setHorizontalGroup(gl_pnlBottomGraphs.createParallelGroup(Alignment.LEADING)
				.addComponent(splitBottom, GroupLayout.DEFAULT_SIZE, 961, Short.MAX_VALUE));
		gl_pnlBottomGraphs.setVerticalGroup(gl_pnlBottomGraphs.createParallelGroup(Alignment.LEADING)
				.addComponent(splitBottom, GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE));

		pnlGraphWIANet = NetWaveChartPanel.generate(wiaData, Utils.getTextFont(false));
		pnlGraphWIANet.setBorder(new LineBorder(new Color(0, 0, 0)));
		splitBottom.setLeftComponent(pnlGraphWIANet);

		pnlGraphPF = PressureFlowChartPanel.generate(wiaData, Utils.getTextFont(false));
		pnlGraphPF.setCyclePickListener(this);
		pnlGraphPF.setBorder(new LineBorder(new Color(0, 0, 0)));

		splitBottom.setRightComponent(pnlGraphPF);

		pnlBottomGraphs.setLayout(gl_pnlBottomGraphs);
		splitBottom.setDividerLocation(0.5);
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

		txtCVal = new JTextField();
		txtFlowAvg = new JTextField();
		txtPressAvg = new JTextField();

		JLabel lblExistingWaves = new JLabel("Waves");


		btnWaveHelp = new JCHelpButton(WIAResourceReader.getContents(WIAResourceReader.HELP_WAVES));
		
		btnWaveHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Utils.showInfo(btnWaveHelp.getHelpMessage(), ref.get());
			}

		});

		JScrollPane scrWaves = new JScrollPane();

		JLabel lblPF = new JLabel("Pressure and Flow");
		btnPF = new JCHelpButton(WIAResourceReader.getContents(WIAResourceReader.HELP_WAVE_ALIGN_PF));
		btnPF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				Utils.showInfo(btnPF.getHelpMessage(), ref.get());
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
						pnlGraphPF.setSelectMode(PressureFlowChartPanel.MODE_SYS_DIAS);
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

		JLabel lblSystole = new JLabel("Systole:");

		JLabel lblDiastole = new JLabel("Diastole:");

		txtSystole = new JTextField();
		txtSystole.setColumns(10);

		txtDiastole = new JTextField();
		txtDiastole.setColumns(10);

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

		JLabel lblDiameter = new JLabel("Vessel Size");
		btnDiameterHelp = new JCHelpButton(WIAResourceReader.getContents(WIAResourceReader.HELP_VESSEL_DIAMETER));
		
		btnDiameterHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Utils.showInfo(btnDiameterHelp.getHelpMessage(), ref.get());
			}

		});
		JLabel lblDiamTxtField = new JLabel("Diameter (mm)");
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
						Utils.showError("Could not save to file. System error msg: " + e1.getMessage(), ref.get());
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
						Utils.showError("Could not save to file. System error msg: " + e1.getMessage(), ref.get());
						btnSave.setIcon(Utils.IconFail);
						e1.printStackTrace();
						return;
					}
					btnSave.setIcon(Utils.IconSuccess);
				}

			}
		});

		JLabel lblOther = new JLabel("Other");

		chSerialize = new JCheckBox("Serialize state on accept");
		chSerialize.setSelected(true);
		chSerialize.setOpaque(false);
		chSerialize.setEnabled(true);

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
				.addGroup(
						gl_pnlWIADisplay.createSequentialGroup().addContainerGap()
								.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.TRAILING, false)
										.addComponent(lblSystole, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
										.addComponent(lblDiastole, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.LEADING)
										.addComponent(txtSystole, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
												Short.MAX_VALUE)
										.addComponent(txtDiastole, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
												Short.MAX_VALUE))
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.CENTER)
										.addComponent(btnResetSystole, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
										.addComponent(btnResetDiastole, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
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
						.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addComponent(lblOther))
				.addGroup(gl_pnlWIADisplay.createSequentialGroup().addContainerGap().addComponent(chSerialize)
						.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))));

		gl_pnlWIADisplay.setVerticalGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.LEADING).addGroup(
				gl_pnlWIADisplay.createSequentialGroup().addGap(3).addComponent(lblMetrics)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(gl_pnlWIADisplay
								.createParallelGroup(Alignment.LEADING).addComponent(lblCVal).addComponent(txtCVal))
						.addGap(2)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.LEADING).addComponent(lblAvgFlow)
								.addComponent(txtFlowAvg))
						.addGap(2)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.LEADING).addComponent(lblAvgPress)
								.addComponent(txtPressAvg))
						.addPreferredGap(ComponentPlacement.RELATED)
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
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.BASELINE).addComponent(lblSystole)
								.addComponent(txtSystole, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(btnResetSystole))
						.addGap(2)
						.addGroup(gl_pnlWIADisplay.createParallelGroup(Alignment.BASELINE).addComponent(lblDiastole)
								.addComponent(txtDiastole, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(btnResetDiastole))
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
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(lblOther)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(chSerialize, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addContainerGap()
						.addPreferredGap(ComponentPlacement.UNRELATED, Short.MAX_VALUE, Short.MAX_VALUE)));

		tableWaves = WaveTable.generate(this, false);

		scrWaves.setViewportView(tableWaves);
		tableWaves.setFillsViewportHeight(true);

		Utils.setFont(Utils.getSubTitleFont(), lblMetrics, lblPF, lblDiameter, lblExistingWaves, lblOther, lblSaving);
		Utils.setFont(Utils.getTextFont(false), btnSave);
		Utils.setFont(Utils.getSubTitleSubFont(), lblSelectionMode);

		Utils.setFont(Utils.getSmallTextFont(), lblCVal, lblAvgFlow, lblAvgPress, txtCVal, txtFlowAvg, txtPressAvg,
				lblSystole, lblDiastole, txtSystole, txtDiastole, lblDiamTxtField, txtVesselDiameter, chSerialize,
				chImgIncludeFileName, chAllowWrap, chAllowWrapIgnoreEnds);

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
					Utils.showWarning(
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

		Utils.setFont(Utils.getSubTitleFont(), btnReset, btnAccept);

		pnlWIAButtons.add(btnReset);
		pnlWIAButtons.add(btnAccept);

		FlowLayout flowLayout = (FlowLayout) pnlWIAButtons.getLayout();
		flowLayout.setHgap(10);
		flowLayout.setAlignment(FlowLayout.TRAILING);
		pnlWIAButtons.setBorder(new LineBorder(new Color(0, 0, 0)));
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
		if (!Double.isNaN(wiaData.getSystoleTime())) {
			setSystole(wiaData.getSystoleTime());
		}
		if (!Double.isNaN(wiaData.getDiastoleTime())) {
			setDiastole(wiaData.getDiastoleTime());
		}
		// the panels will handle their own modifications for startup
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
			Utils.showError(e.getMessage(), this);
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
			Utils.showError("Please set a time to align in both flow and pressure graphs", pnlGraphPF);
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
			resetSystole();
			resetDiastole();
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
		this.txtSystole.setText("");
		this.txtDiastole.setText("");
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
	 * 
	 * @return true if validation successful, false otherwise
	 */
	private boolean validateFields() {
		String input = txtVesselDiameter.getText().trim();
		if (!input.isEmpty()) {
			try {
				double value = Double.parseDouble(input);

				if (value < 0) {
					Utils.showError("Vessel diameter cannot be negative!", ref.get());
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
				Utils.showError("Invalid number format for vessel diameter!", ref.get());
				return false;
			}
		}

		return true;
	}

	/**
	 * Callback method from {@link WavePickListener} to update the display when a new wave is added.
	 * @param wave The wave that was added.
	 */
	@Override
	public void updateDisplayForAddedWave(Wave wave) {

		tableWaves.addWave(wave);
	}

	/**
	 * Callback method from {@link WaveTableListener} to remove a wave from the display.
	 * @param wave The wave to be removed.
	 */
	@Override
	public void removeWave(Wave wave) {
		this.pnlGraphWIASep.removeWave(wave);

	}

	/**
	 * Callback method from {@link PFPickListener} to set the systole time in the display.
	 * @param timeSystole The time of systole.
	 */
	@Override
	public void setSystole(double timeSystole) {
		DecimalFormat df = new DecimalFormat("#.00");

		this.txtSystole.setText(df.format(timeSystole));

	}

	/**
	 * Callback method from {@link PFPickListener} to set the diastole time in the display.
	 * @param timeDiastole The time of diastole.
	 */
	@Override
	public void setDiastole(double timeDiastole) {
		DecimalFormat df = new DecimalFormat("#.00");

		this.txtDiastole.setText(df.format(timeDiastole));
	}

	/**
	 * Callback method from {@link PFPickListener} to reset the systole time in the display.
	 */
	@Override
	public void resetSystole() {
		this.txtSystole.setText("");
	}


	/**
	 * Callback method from {@link PFPickListener} to reset the diastole time in the display.
	 */
	@Override
	public void resetDiastole() {
		this.txtDiastole.setText("");
	}

	/**
	 * Callback method from {@link PFPickListener} to enable or disable the alignment button.
	 * @param ready True if the alignment can be run, false otherwise.
	 */
	@Override
	public void setReadyAlign(boolean ready) {
		Utils.setEnabled(ready, false, this.btnAlign);
	}

}
