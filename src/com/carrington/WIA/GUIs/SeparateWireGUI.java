package com.carrington.WIA.GUIs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FilenameUtils;

import com.carrington.WIA.Utils;
import com.carrington.WIA.Cardio.Beat;
import com.carrington.WIA.DataStructures.HemoData;
import com.carrington.WIA.DataStructures.WIAData;
import com.carrington.WIA.GUIs.AlignerGUI.AlignResult;
import com.carrington.WIA.GUIs.SheetOptionsSelectionGUI.OptionSelections;
import com.carrington.WIA.GUIs.WavePickerPreviewGUI.PreviewResult;
import com.carrington.WIA.GUIs.Components.JCButton;
import com.carrington.WIA.GUIs.Components.JCHelpButton;
import com.carrington.WIA.GUIs.Components.JCSaveButton;
import com.carrington.WIA.GUIs.Configs.SepFileConfigGUI;
import com.carrington.WIA.Graph.ComboChartSaver;
import com.carrington.WIA.IO.Header;
import com.carrington.WIA.IO.HeaderResult;
import com.carrington.WIA.IO.NamingConvention;
import com.carrington.WIA.IO.ReadResult;
import com.carrington.WIA.IO.Saver;
import com.carrington.WIA.IO.SheetDataReader;
import com.carrington.WIA.IO.WIAResourceReader;
import com.carrington.WIA.Math.DataResampler;
import com.carrington.WIA.Math.DataResampler.ResampleException;
import com.carrington.WIA.Math.Savgol;
import com.carrington.WIA.Math.Savgol.SavGolSettings;

/**
 * A graphical user interface that provides a step-by-step workflow for
 * analyzing data from two separate wire files. The process includes file
 * selection, resampling, trimming, alignment of the two datasets, beat
 * selection, and finally, wave intensity analysis.
 */
public class SeparateWireGUI extends JFrame implements WIACaller {

	private static final long serialVersionUID = 2125209267251911177L;
	// Constant used for setting the panel state
	private static final int STATE_INIT = 0;
	private static final int STATE_FILE_ONE_SELECTED = 1;
	private static final int STATE_RESAMPLE = 2;
	private static final int STATE_TRIM = 3;
	private static final int STATE_ALIGN = 4;
	private static final int STATE_WIA = 5;

	private static int fontWidth = Utils.getFontParams(Utils.getTextFont(false), "0.00001")[1];

	// Fields for file 1 selection panel
	private JPanel pnlFileOne;
	private JCButton btnSelectFile2;
	private JCButton btnSelectFile1;
	private JTextField txtFile1;

	// Fields for file 2 selection panel
	private JPanel pnlFileTwo;
	private JList<Header> listColsFile1;
	private JTextField txtFile2;
	private JList<Header> listColsFile2;

	// Fields for Resample panel
	private JPanel pnlResample;
	private JTextField txtResampFreq;
	private JCButton btnStartResamp;
	private JCSaveButton btnSaveResampled;
	private final Border borderDefaultResamp = new JTextField().getBorder();

	// Fields for Trim panel
	private JPanel pnlTrim;
	private JCButton btnTrim1;
	private JCSaveButton btnSaveTrim1;
	private JCButton btnTrim2;
	private JCSaveButton btnSaveTrim2;
	private JCButton btnAcceptTrims;

	// Fields for Align panel
	private JPanel pnlAlign;
	private JCheckBox chPreAlignFilter;
	private JComboBox<String> cbAlignEnsembleType;
	/**
	 * A map to associate user-friendly ensemble type names ("Trim", "Scale") with
	 * their corresponding HemoData constants.
	 */
	public static final LinkedHashMap<String, Integer> EnsembleTypeMap = new LinkedHashMap<String, Integer>();
	static {
		EnsembleTypeMap.put("Trim", HemoData.ENSEMBLE_TRIM);
		EnsembleTypeMap.put("Scale", HemoData.ENSEMBLE_SCALE);
	}
	private JTextField txtSavWindow;
	private JTextField txtSavPolynomialOrder;
	private JTextField txtSavSampleRate;
	private JCButton btnRunAlignment;
	private JCButton btnSaveSelectionEnsembledBeat;
	private JCButton btnSaveIndividualBeatImages;

	// Fields for WIA panel
	private JPanel pnlWIA;
	private JTextField txtSelectionName;
	private JTextField txtSelectionRemaining;
	private JButton btnRunWIAPreview;
	private JCButton btnRunWIA;
	private JCButton btnSaveMetrics;
	private JCButton btnNextFiles;
	private JCButton btnNextSelection;

	private JPanel contentPane;
	private volatile JFrame frameToGoBackTo;
	private BackListener backListener;
	private WeakReference<SeparateWireGUI> ref = new WeakReference<SeparateWireGUI>(this);

	private volatile JProgressBar progressBar;

	// Need to be reset the reset() method
	private SepFileConfigGUI config;
	private RASData dataManager = new RASData();
	private boolean isReopen = false;
	private AlignResult alignResult = null;
	private LinkedList<PreviewResult> previewResultData = null;

	private JLabel lblPreviewFirst;

	/**
	 * Constructs a new GUI to select file, resample, trim, and then align data.
	 * 
	 * @param frameToGoBackTo Frame to navigate back to if this is closed. If null,
	 *                        program will exit.
	 * @param closeListener   When navigating back, will call this listener. If
	 *                        null, program will exit.
	 * 
	 * @throws IOException if could not interact with configuration file
	 */
	public SeparateWireGUI(JFrame frameToGoBackTo, BackListener closeListener) throws IOException {

		this();
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.backListener = closeListener;
		this.frameToGoBackTo = frameToGoBackTo;
	}

	/**
	 * Makes this GUI visible and makes the caller invisible
	 */
	public synchronized void navigateInto() {
		this.setVisible(true);
		if (this.frameToGoBackTo != null) {
			this.frameToGoBackTo.setVisible(false);
		}
	}

	/**
	 * Makes this GUI invisible amd makes the caller visible.
	 */
	public synchronized void navigateBack() {

		if (this.frameToGoBackTo != null) {
			this.setVisible(false);
			this.frameToGoBackTo.setVisible(true);
			this.backListener.wentBack();
		}
	}

	/**
	 * Quits the program WITHOUT going back to the calling frame (if set)
	 */
	public void quit() {

		if (Utils.confirmAction("Confirm Quit", "Are you sure you want to quit?", this)) {
			this.setVisible(false);
			System.exit(0);
		}

	}

	/**
	 * Resets the entire GUI to its initial state, clearing all loaded data and
	 * progress.
	 * 
	 * @param warn If {@code true}, prompts the user with a confirmation dialog
	 *             before resetting.
	 */
	public void reset(boolean warn) {
		if (warn) {
			if (!Utils.confirmAction("Confirm Reset", "Are you sure you want to reset to next files?", this)) {
				return;

			}
		}
		setPanelState(STATE_INIT);
		dataManager = new RASData();
		alignResult = null;
		previewResultData = null;

		isReopen = false;

	}

	/**
	 * Creates the main GUI frame and initializes all its components.
	 * 
	 * @throws IOException if could not interact with configuration file
	 */
	public SeparateWireGUI() throws IOException {

		this.config = new SepFileConfigGUI();

		

		setTitle("Wave Analysis - Separate Flow and Pressure");

		int size = Utils.getTextFont(false).getSize();
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		int frameWidth = (int) (Utils.getMaxAppSize().width * (3.0 / 5.0));

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});
		UIManager.put("Menu.font", Utils.getSubTitleFont());

		UIManager.put("MenuBar.font", Utils.getSubTitleFont());
		UIManager.put("MenuItem.font", Utils.getSubTitleFont());

		JMenuBar menu = new JMenuBar();
		setJMenuBar(menu);
		// menu.setPreferredSize(new Dimension(menu.getPreferredSize().width, size *
		// 2));

		JMenu mnFile = new JMenu("File");
		JMenu mnHelp = new JMenu("Help");
		menu.add(mnFile);
		menu.add(mnHelp);

		// File
		JMenuItem mnItemSettings = new JMenuItem("Settings");
		mnFile.add(mnItemSettings);

		mnFile.addSeparator();

		JMenuItem mnItemQuit = new JMenuItem("Quit");
		mnFile.add(mnItemQuit);

		mnItemQuit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				quit();

			}

		});

		mnItemSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				config.open(ref.get());

			}

		});

		// Help
		JMenuItem mnItemGithub = new JMenuItem("Github");
		mnHelp.add(mnItemGithub);

		JMenuItem mnItemReport = new JMenuItem("Report issue");
		mnHelp.add(mnItemReport);

		mnItemGithub.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					java.awt.Desktop.getDesktop().browse(new java.net.URI("https://github.com/JCarring/WaveAnalyzer"));
				} catch (Exception ex) {
					Utils.showError("Could not browse internet", ref.get());
				}

			}

		});

		mnItemReport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					java.awt.Desktop.getDesktop()
							.browse(new java.net.URI("https://github.com/JCarring/WaveAnalyzer/issues"));
				} catch (Exception ex) {
					Utils.showError("Could not browse internet", ref.get());
				}

			}

		});

		Utils.setMenuBarFont(Utils.getSubTitleSubFont(), getJMenuBar());

		contentPane = new JPanel();

		setContentPane(contentPane);

		JPanel topPanel = new JPanel();
		topPanel.setBackground(new Color(192, 192, 192));
		topPanel.setBorder(new LineBorder(new Color(0, 0, 0)));

		JPanel middlePanel = new JPanel();
		middlePanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		JPanel bottomPanel = new JPanel();
		bottomPanel.setBackground(new Color(192, 192, 192));
		bottomPanel.setBorder(new LineBorder(new Color(0, 0, 0)));

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width / 2;

		GroupLayout mainContentPaneLayout = new GroupLayout(contentPane);
		mainContentPaneLayout.setHorizontalGroup(mainContentPaneLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(mainContentPaneLayout.createSequentialGroup().addContainerGap()
						.addGroup(mainContentPaneLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(topPanel, width, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
								.addComponent(middlePanel, width, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
								.addComponent(bottomPanel, width, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
						.addContainerGap()));
		mainContentPaneLayout.setVerticalGroup(mainContentPaneLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(mainContentPaneLayout.createSequentialGroup().addContainerGap()
						.addComponent(topPanel, GroupLayout.PREFERRED_SIZE, (int) (size * 2.5),
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(middlePanel, GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(bottomPanel, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						.addContainerGap()));

		// Initialize all the panels
		initPnlFile1(frameWidth, size);
		initPnlFile2(frameWidth, size);
		initPnlResample(frameWidth, size);
		initPnlTrim();
		initPnlAlign();
		initPnlWIA();

		GroupLayout gl_middlePanel = new GroupLayout(middlePanel);
		gl_middlePanel.setHorizontalGroup(gl_middlePanel.createParallelGroup(Alignment.TRAILING).addGroup(gl_middlePanel
				.createSequentialGroup().addContainerGap()
				.addGroup(gl_middlePanel.createParallelGroup(Alignment.TRAILING)
						.addComponent(pnlWIA, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(pnlAlign, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE)
						.addComponent(pnlFileOne, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(pnlFileTwo, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addGroup(gl_middlePanel.createSequentialGroup()
								.addComponent(pnlResample, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(pnlTrim,
										GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)))
				.addContainerGap()));
		gl_middlePanel
				.setVerticalGroup(
						gl_middlePanel.createParallelGroup(Alignment.LEADING)
								.addGroup(
										gl_middlePanel.createSequentialGroup().addContainerGap()
												.addComponent(pnlFileOne, GroupLayout.PREFERRED_SIZE,
														GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
												.addPreferredGap(ComponentPlacement.RELATED)
												.addComponent(pnlFileTwo, GroupLayout.PREFERRED_SIZE,
														GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
												.addPreferredGap(ComponentPlacement.RELATED)
												.addGroup(gl_middlePanel.createParallelGroup(Alignment.LEADING, false)
														.addComponent(pnlTrim).addComponent(pnlResample))
												.addPreferredGap(ComponentPlacement.RELATED)
												.addComponent(pnlAlign, GroupLayout.PREFERRED_SIZE,
														GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
												.addPreferredGap(ComponentPlacement.RELATED)
												.addComponent(pnlWIA, GroupLayout.PREFERRED_SIZE,
														GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
												.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		middlePanel.setLayout(gl_middlePanel);
		contentPane.setLayout(mainContentPaneLayout);

		JLabel lblInstructions = new JLabel("Proceed with the steps below.");
		topPanel.add(lblInstructions);

		bottomPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		GroupLayout bottomLayout = new GroupLayout(bottomPanel);

		// bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));

		Utils.setFont(Utils.getSubTitleFont(), lblInstructions);

		JCButton btnBack = new JCButton("Main Menu", JCButton.BUTTON_GO_BACK);
		btnBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Utils.confirmAction("Confirm", "You will lose all progress. Sure you want to go back?",
						ref.get())) {
					navigateBack();
				}
			}
		});

		JCButton btnReset = new JCButton("Reset", JCButton.BUTTON_RESET);
		btnReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reset(true);

			}
		});

		JCButton btnQuit = new JCButton("Quit", JCButton.BUTTON_QUIT);
		btnQuit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				quit();
			}

		});
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(50);
		progressBar.setVisible(false);
		progressBar.setStringPainted(true);

		bottomLayout.setHonorsVisibility(false);

		bottomLayout.setHorizontalGroup(bottomLayout.createSequentialGroup().addContainerGap()
				.addComponent(progressBar, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addPreferredGap(ComponentPlacement.UNRELATED)
				.addComponent(btnReset, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(btnBack, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnQuit, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addContainerGap()

		);
		// Create the vertical group.
		// Both components are aligned on their baseline (or centered) vertically.
		bottomLayout.setVerticalGroup(bottomLayout.createSequentialGroup().addGap(3)
				.addGroup(bottomLayout.createParallelGroup(Alignment.CENTER)
						.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(btnReset, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(btnBack, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(btnQuit, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE))
				.addGap(3));

		bottomPanel.setLayout(bottomLayout);
		pack();
		setLocationRelativeTo(null);

		setPanelState(STATE_INIT);

	}

	/**
	 * Initializes the panel for selecting the first data file.
	 * 
	 * @param frameWidth The target width for layout calculations.
	 * @param size       A font-based size for layout calculations.
	 */
	private void initPnlFile1(int frameWidth, int size) {
		pnlFileOne = new JPanel();
		pnlFileOne.setBorder(new LineBorder(new Color(0, 0, 0)));

		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setBackground(new Color(192, 192, 192));
		separator.setForeground(new Color(192, 192, 192));

		JPanel pnlSelectFileOne = new JPanel();

		JPanel pnlFileColDisplayOne = new JPanel();

		int height = Utils.getFontParams(Utils.getTextFont(false), "test")[0] * 4;
		GroupLayout gl_pnlFileOne_1 = new GroupLayout(pnlFileOne);
		gl_pnlFileOne_1.setHorizontalGroup(gl_pnlFileOne_1.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_pnlFileOne_1.createSequentialGroup().addContainerGap()
						.addComponent(pnlSelectFileOne, GroupLayout.DEFAULT_SIZE, frameWidth / 4,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(separator, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(pnlFileColDisplayOne, GroupLayout.DEFAULT_SIZE, frameWidth / 2, Short.MAX_VALUE)
						.addContainerGap()));
		gl_pnlFileOne_1.setVerticalGroup(gl_pnlFileOne_1.createParallelGroup(Alignment.LEADING).addGroup(gl_pnlFileOne_1
				.createSequentialGroup().addContainerGap()
				.addGroup(gl_pnlFileOne_1.createParallelGroup(Alignment.LEADING)
						.addComponent(pnlSelectFileOne, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(separator, GroupLayout.PREFERRED_SIZE, height, Short.MAX_VALUE).addComponent(
								pnlFileColDisplayOne, GroupLayout.PREFERRED_SIZE, height, GroupLayout.PREFERRED_SIZE))
				.addContainerGap())

		);
		pnlFileColDisplayOne.setLayout(new BoxLayout(pnlFileColDisplayOne, BoxLayout.X_AXIS));

		Component strut1 = Box.createHorizontalStrut(5);
		pnlFileColDisplayOne.add(strut1);

		JLabel lblColumnList1 = new JLabel("Columns:");
		lblColumnList1.setVerticalAlignment(SwingConstants.TOP);
		lblColumnList1.setMaximumSize(new Dimension(lblColumnList1.getPreferredSize().width, Short.MAX_VALUE));

		pnlFileColDisplayOne.add(lblColumnList1);

		Component strut2 = Box.createHorizontalStrut(5);
		pnlFileColDisplayOne.add(strut2);

		JScrollPane scrollPane = new JScrollPane();

		pnlFileColDisplayOne.add(scrollPane);

		listColsFile1 = new JList<Header>();
		_setColumnList(listColsFile1, null);
		scrollPane.setViewportView(listColsFile1);

		Component horizontalGlue = Box.createHorizontalGlue();
		pnlFileColDisplayOne.add(horizontalGlue);
		pnlSelectFileOne.setLayout(new BoxLayout(pnlSelectFileOne, BoxLayout.Y_AXIS));

		JPanel pnlSelect1 = new JPanel();
		pnlSelect1.setLayout(new BoxLayout(pnlSelect1, BoxLayout.X_AXIS));
		btnSelectFile1 = new JCButton("Select File 1");
		btnSelectFile1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				runFileSelection(true);

			}
		});
		btnSelectFile1.setAlignmentX(Component.CENTER_ALIGNMENT);
		JCHelpButton btnSelectFile1Help = new JCHelpButton(
				"Select file with extension \".csv\", \".txt\", \".xls\", or \".xlsx\". Should contain one column with X time stamps, column with either pressure or flow, and column with something to align (i.e. ECG trace)",
				ref.get());
		pnlSelect1.add(btnSelectFile1);
		pnlSelect1.add(Box.createHorizontalStrut(5));
		pnlSelect1.add(btnSelectFile1Help);
		pnlSelect1.setAlignmentX(Component.LEFT_ALIGNMENT);
		pnlSelectFileOne.add(pnlSelect1);
		pnlSelectFileOne.add(Box.createVerticalStrut(5));
		txtFile1 = new JTextField();
		txtFile1.setColumns(10);
		_setFileName(txtFile1, null, null);
		pnlSelectFileOne.add(txtFile1);

		Utils.setFont(Utils.getTextFont(false), txtFile1, lblColumnList1);

		pnlFileOne.setLayout(gl_pnlFileOne_1);

	}

	/**
	 * Initializes the panel for selecting the second data file.
	 * 
	 * @param frameWidth The target width for layout calculations.
	 * @param size       A font-based size for layout calculations.
	 */
	private void initPnlFile2(int frameWidth, int size) {
		pnlFileTwo = new JPanel();
		pnlFileTwo.setBorder(new LineBorder(new Color(0, 0, 0)));

		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setBackground(new Color(192, 192, 192));
		separator.setForeground(new Color(192, 192, 192));

		JPanel pnlSelectFileTwo = new JPanel();

		JPanel pnlFileColDisplayTwo = new JPanel();

		int height = Utils.getFontParams(Utils.getTextFont(false), "test")[0] * 4;

		GroupLayout gl_pnlFileOne_2 = new GroupLayout(pnlFileTwo);
		gl_pnlFileOne_2.setHorizontalGroup(gl_pnlFileOne_2.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_pnlFileOne_2.createSequentialGroup().addContainerGap()
						.addComponent(pnlSelectFileTwo, GroupLayout.DEFAULT_SIZE, frameWidth / 4,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(separator, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(pnlFileColDisplayTwo, GroupLayout.DEFAULT_SIZE, frameWidth / 2, Short.MAX_VALUE)
						.addContainerGap()));
		gl_pnlFileOne_2.setVerticalGroup(gl_pnlFileOne_2.createParallelGroup(Alignment.LEADING).addGroup(gl_pnlFileOne_2
				.createSequentialGroup().addContainerGap()
				.addGroup(gl_pnlFileOne_2.createParallelGroup(Alignment.LEADING)
						.addComponent(pnlSelectFileTwo, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(separator, GroupLayout.PREFERRED_SIZE, height, Short.MAX_VALUE).addComponent(
								pnlFileColDisplayTwo, GroupLayout.PREFERRED_SIZE, height, GroupLayout.PREFERRED_SIZE))
				.addContainerGap())

		);
		pnlFileColDisplayTwo.setLayout(new BoxLayout(pnlFileColDisplayTwo, BoxLayout.X_AXIS));

		Component strut1 = Box.createHorizontalStrut(5);
		pnlFileColDisplayTwo.add(strut1);

		JLabel lblColumnList2 = new JLabel("Columns:");
		lblColumnList2.setVerticalAlignment(SwingConstants.TOP);
		lblColumnList2.setMaximumSize(new Dimension(lblColumnList2.getPreferredSize().width, Short.MAX_VALUE));

		pnlFileColDisplayTwo.add(lblColumnList2);

		Component strut2 = Box.createHorizontalStrut(5);
		pnlFileColDisplayTwo.add(strut2);

		JScrollPane scrollPane = new JScrollPane();
		pnlFileColDisplayTwo.add(scrollPane);

		listColsFile2 = new JList<Header>();
		_setColumnList(listColsFile2, null);
		scrollPane.setViewportView(listColsFile2);

		Component horizontalGlue = Box.createHorizontalGlue();
		pnlFileColDisplayTwo.add(horizontalGlue);
		pnlSelectFileTwo.setLayout(new BoxLayout(pnlSelectFileTwo, BoxLayout.Y_AXIS));

		JPanel pnlSelect2 = new JPanel();
		pnlSelect2.setLayout(new BoxLayout(pnlSelect2, BoxLayout.X_AXIS));
		btnSelectFile2 = new JCButton("Select File 2");
		btnSelectFile2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runFileSelection(false);
			}
		});
		btnSelectFile2.setAlignmentX(Component.CENTER_ALIGNMENT);
		JCHelpButton btnSelectFile2Help = new JCHelpButton(
				"Select file with extension \".csv\", \".txt\", \".xls\", or \".xlsx\". Should contain one column with X time stamps, column with either pressure or flow, and column with something to align (i.e. ECG trace)",
				ref.get());
		pnlSelect2.add(btnSelectFile2);
		pnlSelect2.add(Box.createHorizontalStrut(5));
		pnlSelect2.add(btnSelectFile2Help);
		pnlSelect2.setAlignmentX(Component.LEFT_ALIGNMENT);
		pnlSelectFileTwo.add(pnlSelect2);
		pnlSelectFileTwo.add(Box.createVerticalStrut(5));

		txtFile2 = new JTextField();
		_setFileName(txtFile2, null, null);
		pnlSelectFileTwo.add(txtFile2);
		txtFile2.setColumns(10);

		Utils.setFont(Utils.getTextFont(false), txtFile2, lblColumnList2);

		pnlFileTwo.setLayout(gl_pnlFileOne_2);
	}

	/**
	 * Initializes the panel for resampling the data.
	 * 
	 * @param frameWidth The target width for layout calculations.
	 * @param size       A font-based size for layout calculations.
	 */
	private void initPnlResample(int frameWidth, int size) {

		pnlResample = new JPanel();
		pnlResample.setBorder(new LineBorder(new Color(0, 0, 0)));

		JLabel lblResamp = new JLabel("Resample");

		JLabel lblResampFreq = new JLabel("Frequency");

		btnStartResamp = new JCButton("Start");
		btnStartResamp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String resampleFreq = validateResamplePanel();
				if (resampleFreq == null)
					return;

				double[][] domains = dataManager.getDomains();
				double resampleRate;
				try {
					resampleRate = DataResampler.calculateReSampleFrequency(resampleFreq, domains[0], domains[1]);
				} catch (ResampleException e1) {
					Utils.showError(e1.getMessage(), ref.get());
					return;
				}
				btnStartResamp.setEnabled(false);
				BackgroundTaskExecutor.executeTask((BackgroundProgressRecorder progress) -> {
					try {
						HemoData resampled1 = dataManager.data1.resampleAt(resampleRate, progress);
						HemoData resampled2 = dataManager.data2.resampleAt(resampleRate, progress);
						return new HemoData[] { resampled1, resampled2 };
					} catch (ResampleException e1) {
						return null;
					}

				}, progressBar, resampledHD -> {

					if (resampledHD == null) {
						btnStartResamp.setEnabled(true);
						Utils.showError("Error occurred while resampling.", ref.get());
						return;
					}

					dataManager.resampled1 = resampledHD[0];
					dataManager.resampled2 = resampledHD[1];
					setPanelState(STATE_TRIM);

				});

			}
		});

		txtResampFreq = new JTextField();
		txtResampFreq.setColumns(10);
		txtResampFreq.getDocument().addDocumentListener(new DocumentListener() {
			void validateField() {
				if (!txtResampFreq.isEnabled()) {
					txtResampFreq.setBorder(borderDefaultResamp);
					return;
				}
				boolean isValid = validateResamplePanelSilent(); // New method below
				txtResampFreq.setBorder(isValid ? borderDefaultResamp : BorderFactory.createLineBorder(Color.RED, 2));
			}

			public void insertUpdate(DocumentEvent e) {
				validateField();
			}

			public void removeUpdate(DocumentEvent e) {
				validateField();
			}

			public void changedUpdate(DocumentEvent e) {
				validateField();
			}
		});
		txtResampFreq.addPropertyChangeListener("enabled", evt -> {
			if (!(Boolean) evt.getNewValue()) {
				txtResampFreq.setBorder(borderDefaultResamp);
			} else {
				// On re-enable, re-validate
				boolean isValid = validateResamplePanelSilent();
				txtResampFreq.setBorder(isValid ? borderDefaultResamp : BorderFactory.createLineBorder(Color.RED, 2));
			}
		});

		JCHelpButton btnHelp = new JCHelpButton(WIAResourceReader.getContents(WIAResourceReader.HELP_RESAMPLE),
				ref.get());
		btnSaveResampled = new JCSaveButton("Save resampled file (optional)");
		btnSaveResampled.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (dataManager.resampled1 != null && dataManager.resampled2 != null) {

					File file1 = Utils.promptUserForFileName("Save resampled " + dataManager.resampled1.getName(),
							config.getLastDirectoryPath(),
							FilenameUtils.removeExtension(dataManager.resampled1.getName()) + " resampled.csv", ".csv");
					if (file1 == null) {
						// user cancelled
						return;
					} else if (!Utils.hasOkayExtension(file1, ".csv")) {
						// filed HAD an extension and it was not .csv
						Utils.showError("File must be saved as .csv", ref.get());
						return;
					}
					file1 = Utils.appendExtensionIfDoesNotHaveExt(file1, ".csv");

					File file2 = Utils.promptUserForFileName("Save resampled " + dataManager.resampled2.getName(),
							config.getLastDirectoryPath(),
							FilenameUtils.removeExtension(dataManager.resampled2.getName()) + " resampled.csv", ".csv");
					if (file2 == null) {
						// user cancelled
						return;
					} else if (!Utils.hasOkayExtension(file2, ".csv")) {
						// filed HAD an extension and it was not .csv
						Utils.showError("File must be saved as .csv", ref.get());
						return;
					}
					file2 = Utils.appendExtensionIfDoesNotHaveExt(file2, ".csv");

					String errors1 = Saver.saveData(file1, HemoData.toSaveableStringArray(dataManager.resampled1));
					String errors2 = Saver.saveData(file2, HemoData.toSaveableStringArray(dataManager.resampled2));
					if (errors1 != null || errors2 != null) {
						Utils.showError("There was an error in saving the resampled data: "
								+ (errors1 == null ? errors2 : errors1), ref.get());
						return;
					}
				}
			}
		});

		GroupLayout gl_pnlResample = new GroupLayout(pnlResample);
		gl_pnlResample.setHorizontalGroup(gl_pnlResample.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_pnlResample.createSequentialGroup().addGap(4)
						.addGroup(gl_pnlResample.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_pnlResample.createSequentialGroup().addComponent(lblResamp)
										.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnHelp)
										.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addComponent(btnSaveResampled))
								.addGroup(gl_pnlResample.createSequentialGroup().addContainerGap()
										.addComponent(lblResampFreq).addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(txtResampFreq, fontWidth, GroupLayout.PREFERRED_SIZE,
												Short.MAX_VALUE)
										.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnStartResamp)))
						.addContainerGap()));
		gl_pnlResample.setVerticalGroup(gl_pnlResample.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_pnlResample.createSequentialGroup().addGap(4)
						.addGroup(gl_pnlResample.createParallelGroup(Alignment.CENTER).addComponent(lblResamp)
								.addComponent(btnHelp).addComponent(btnSaveResampled))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlResample.createParallelGroup(Alignment.BASELINE).addComponent(lblResampFreq)
								.addComponent(txtResampFreq).addComponent(btnStartResamp))
						.addContainerGap()));
		pnlResample.setLayout(gl_pnlResample);

		Utils.setFont(Utils.getSubTitleFont(), lblResamp);
		Utils.setFont(Utils.getTextFont(false), lblResampFreq, txtResampFreq);
	}

	/**
	 * Initializes the panel for trimming the data from the beginning or end.
	 */
	private void initPnlTrim() {

		pnlTrim = new JPanel();
		pnlTrim.setBorder(new LineBorder(new Color(0, 0, 0)));

		JLabel lblTrim = new JLabel("Trim");

		btnTrim1 = new JCButton("Trim File 1");
		btnTrim1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runTrimSelection(true);
			}
		});

		JSeparator trimSep = new JSeparator();
		trimSep.setOrientation(SwingConstants.VERTICAL);
		trimSep.setBackground(new Color(192, 192, 192));
		trimSep.setForeground(new Color(192, 192, 192));

		btnTrim2 = new JCButton("Trim File 2");
		btnTrim2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runTrimSelection(false);
			}
		});

		btnAcceptTrims = new JCButton("Accept");
		btnAcceptTrims.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				setPanelState(STATE_ALIGN);
			}
		});

		JSeparator trimSepNext = new JSeparator();
		trimSepNext.setOrientation(SwingConstants.VERTICAL);
		trimSepNext.setBackground(new Color(192, 192, 192));
		trimSepNext.setForeground(new Color(192, 192, 192));

		JCHelpButton btnTrimHelp = new JCHelpButton(WIAResourceReader.getContents(WIAResourceReader.HELP_TRIM),
				ref.get());

		btnSaveTrim1 = new JCSaveButton("Save file 1 (optional)");
		btnSaveTrim2 = new JCSaveButton("Save file 2 (optional)");
		btnSaveTrim1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File file1 = Utils.promptUserForFileName("Save trim file 1", config.getLastDirectoryPath(),
						FilenameUtils.removeExtension(dataManager.resampled1.getName()) + " trimmed.csv", ".csv");
				if (file1 == null) {
					// user cancelled
					return;
				} else if (!Utils.hasOkayExtension(file1, ".csv")) {
					// filed HAD an extension and it was not .csv
					Utils.showError("File must be saved as .csv", ref.get());
					return;
				}
				file1 = Utils.appendExtensionIfDoesNotHaveExt(file1, ".csv");

				String errors1 = Saver.saveData(file1, HemoData.toSaveableStringArray(
						RASData.applyTrim(dataManager.trimIndices1, dataManager.resampled1, true)));

				if (errors1 != null) {
					Utils.showError("There was an error in saving the resampled data: " + errors1, ref.get());
					return;
				}
			}
		});
		btnSaveTrim2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				File file2 = Utils.promptUserForFileName("Save trim file 2", config.getLastDirectoryPath(),
						FilenameUtils.removeExtension(dataManager.resampled2.getName()) + " trimmed.csv", ".csv");
				if (file2 == null) {
					// user cancelled
					return;
				} else if (!Utils.hasOkayExtension(file2, ".csv")) {
					// filed HAD an extension and it was not .csv
					Utils.showError("File must be saved as .csv", ref.get());
					return;
				}
				file2 = Utils.appendExtensionIfDoesNotHaveExt(file2, ".csv");
				String errors2 = Saver.saveData(file2, HemoData.toSaveableStringArray(
						RASData.applyTrim(dataManager.trimIndices2, dataManager.resampled2, true)));

				if (errors2 != null) {
					Utils.showError("There was an error in saving the resampled data: " + errors2, ref.get());
					return;
				}
			}
		});

		GroupLayout gl_pnlTrim = new GroupLayout(pnlTrim);
		gl_pnlTrim.setHorizontalGroup(gl_pnlTrim.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_pnlTrim.createSequentialGroup().addGroup(gl_pnlTrim.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_pnlTrim.createSequentialGroup().addGap(4).addComponent(lblTrim)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnTrimHelp))
						.addGroup(gl_pnlTrim.createSequentialGroup().addContainerGap().addComponent(btnTrim1)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnSaveTrim1)
								.addPreferredGap(ComponentPlacement.UNRELATED)
								.addComponent(trimSep, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(btnTrim2)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnSaveTrim2)
								.addPreferredGap(ComponentPlacement.UNRELATED)
								.addComponent(trimSepNext, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(btnAcceptTrims)))
						.addContainerGap()));
		gl_pnlTrim
				.setVerticalGroup(
						gl_pnlTrim.createParallelGroup(Alignment.LEADING)
								.addGroup(Alignment.TRAILING,
										gl_pnlTrim.createSequentialGroup().addGap(4)
												.addGroup(gl_pnlTrim.createParallelGroup(Alignment.CENTER)
														.addComponent(lblTrim).addComponent(btnTrimHelp))
												.addPreferredGap(ComponentPlacement.RELATED)
												.addGroup(gl_pnlTrim.createParallelGroup(Alignment.CENTER)
														.addComponent(btnTrim1).addComponent(btnSaveTrim1)
														.addComponent(trimSep, GroupLayout.PREFERRED_SIZE,
																GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
														.addComponent(btnTrim2).addComponent(btnSaveTrim2)
														.addComponent(trimSepNext, GroupLayout.PREFERRED_SIZE,
																GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
														.addComponent(btnAcceptTrims))
												.addContainerGap()));
		pnlTrim.setLayout(gl_pnlTrim);

		Utils.setFont(Utils.getSubTitleFont(), lblTrim);

	}

	/**
	 * Initializes the panel for aligning the two datasets and selecting beats.
	 */
	private void initPnlAlign() {
		pnlAlign = new JPanel();
		pnlAlign.setBorder(new LineBorder(new Color(0, 0, 0)));

		JLabel lblAlign = new JLabel("Alignment / Selections");

		JLabel lblWindow = new JLabel("Window:");
		JLabel lblPolyOrder = new JLabel("Polynomial order:");
		JLabel lblSampRate = new JLabel("Resample rate (leave blank to not resample):");
		txtSavWindow = new JTextField("");
		txtSavWindow.setColumns(10);
		txtSavPolynomialOrder = new JTextField("");
		txtSavPolynomialOrder.setColumns(10);
		txtSavSampleRate = new JTextField("");
		txtSavPolynomialOrder.setColumns(10);

		chPreAlignFilter = new JCheckBox("Filter before align (Savitsky-Golay).");
		chPreAlignFilter.setOpaque(false);
		chPreAlignFilter.setSelected(true);
		chPreAlignFilter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean selected = chPreAlignFilter.isSelected();
				Utils.setEnabled(selected, false, txtSavWindow, txtSavPolynomialOrder, lblPolyOrder, lblWindow);
			}
		});

		JLabel lblEnsembleType = new JLabel("Ensemble type:");
		cbAlignEnsembleType = new JComboBox<String>(EnsembleTypeMap.keySet().toArray(new String[0]));
		cbAlignEnsembleType.setOpaque(false);
		cbAlignEnsembleType.setEditable(false);
		cbAlignEnsembleType.setSelectedItem(config.getEnsembleType());
		FontMetrics metrics = cbAlignEnsembleType.getFontMetrics(cbAlignEnsembleType.getFont());
		int cbEnsembleWidth = 0;

		for (int i = 0; i < cbAlignEnsembleType.getItemCount(); i++) {
			int currWidth = metrics.stringWidth(cbAlignEnsembleType.getItemAt(i));
			cbEnsembleWidth = Math.max(currWidth, cbEnsembleWidth);
		}
		cbEnsembleWidth *= 3;

		btnRunAlignment = new JCButton("Start");
		btnRunAlignment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				runAlignmentSelections();

			}
		});

		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setBackground(new Color(192, 192, 192));
		separator.setForeground(new Color(192, 192, 192));

		btnSaveSelectionEnsembledBeat = new JCButton("Save Selections", JCButton.BUTTON_STANDARD);
		btnSaveSelectionEnsembledBeat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Saves the ensembled Beat HemoData in a separate folder for each selection
				// (treatment)
				if (alignResult == null || alignResult.getBeats().isEmpty()) {
					Utils.showError("No selections to save!", ref.get());
				}

				if (ref.get().btnSaveSelectionEnsembledBeat.getIcon() == Utils.IconSuccess) {
					// Already successfully saved. Confirm overwrite.
					if (!Utils.confirmAction("Confirm possible overwrite",
							"You have already saved the selections. This may overwrite data. "
									+ "Are you sure you want to continue?",
							ref.get())) {
						return;
					}
				}

				for (Beat beat : alignResult.getBeats()) {
					String error = saveEnsembledBeatData(beat);
					if (error != null) {
						Utils.showError(error, ref.get());
						btnSaveSelectionEnsembledBeat.setIcon(Utils.IconFail);
						// Don't keep trying to save, same error is likely going to occur
						return;
					}
				}
				btnSaveSelectionEnsembledBeat.setIcon(Utils.IconSuccess);

			}

		});

		btnSaveIndividualBeatImages = new JCButton("Save Images of Beats", JCButton.BUTTON_STANDARD);
		btnSaveIndividualBeatImages.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				// Saves the ensembled Beat HemoData in a separate folder for each selection
				// (treatment)
				if (alignResult == null || alignResult.getBeats().isEmpty()) {
					Utils.showError("No selections to save!", ref.get());
				}

				if (ref.get().btnSaveIndividualBeatImages.getIcon() == Utils.IconSuccess) {
					// Already successfully saved. Confirm overwrite.
					if (!Utils.confirmAction("Confirm possible overwrite",
							"You have already saved the selections. This may overwrite data. "
									+ "Are you sure you want to continue?",
							ref.get())) {
						return;
					}
				}

				for (Beat beat : alignResult.getBeats()) {
					String error = saveBeatImages(beat, alignResult.getBeatImages(beat));
					if (error != null) {
						Utils.showError(error, ref.get());
						btnSaveIndividualBeatImages.setIcon(Utils.IconFail);
						// Don't keep trying to save, same error is likely going to occur
						return;
					}
				}
				btnSaveIndividualBeatImages.setIcon(Utils.IconSuccess);
			}

		});

		GroupLayout gl_panel = new GroupLayout(pnlAlign);
		gl_panel.setHorizontalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(gl_panel
				.createSequentialGroup().addGap(4)
				.addGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(gl_panel.createSequentialGroup()
						.addContainerGap().addComponent(chPreAlignFilter).addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(lblWindow).addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(txtSavWindow, GroupLayout.PREFERRED_SIZE, fontWidth, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(lblPolyOrder)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(txtSavPolynomialOrder,
								GroupLayout.PREFERRED_SIZE, fontWidth, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_panel.createSequentialGroup().addContainerGap().addComponent(lblEnsembleType)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(cbAlignEnsembleType,
										GroupLayout.DEFAULT_SIZE, cbEnsembleWidth, cbEnsembleWidth))
						.addGroup(gl_panel.createSequentialGroup().addContainerGap().addComponent(lblSampRate)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(txtSavSampleRate,
										GroupLayout.PREFERRED_SIZE, fontWidth, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_panel.createSequentialGroup().addContainerGap().addComponent(btnRunAlignment)
								.addPreferredGap(ComponentPlacement.UNRELATED)
								.addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.UNRELATED)
								.addComponent(btnSaveSelectionEnsembledBeat).addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(btnSaveIndividualBeatImages))
						.addComponent(lblAlign))
				.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		gl_panel.setVerticalGroup(gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup().addGap(4).addComponent(lblAlign)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(gl_panel.createParallelGroup(Alignment.CENTER, false).addComponent(chPreAlignFilter)
								.addComponent(lblWindow)
								.addComponent(txtSavWindow, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(lblPolyOrder)
								.addComponent(txtSavPolynomialOrder, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_panel.createParallelGroup(Alignment.CENTER, false).addComponent(lblSampRate)
								.addComponent(txtSavSampleRate, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_panel.createParallelGroup(Alignment.CENTER, false).addComponent(lblEnsembleType)
								.addComponent(cbAlignEnsembleType))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(gl_panel.createParallelGroup(Alignment.LEADING, false)
								.addComponent(btnRunAlignment, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(btnSaveSelectionEnsembledBeat, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(btnSaveIndividualBeatImages, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE))
						.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		pnlAlign.setLayout(gl_panel);

		Utils.setFont(Utils.getSubTitleFont(), lblAlign);
		Utils.setFont(Utils.getTextFont(false), chPreAlignFilter, txtSavPolynomialOrder, txtSavWindow, txtSavSampleRate,
				lblWindow, lblPolyOrder, lblSampRate, lblEnsembleType, cbAlignEnsembleType);

	}

	/**
	 * Initializes the panel for performing Wave Intensity Analysis (WIA).
	 */
	private void initPnlWIA() {
		pnlWIA = new JPanel();
		pnlWIA.setBorder(new LineBorder(new Color(0, 0, 0)));
		GroupLayout gl_pnlWIA = new GroupLayout(pnlWIA);

		btnRunWIAPreview = new JCButton("Preview Wave Profile");
		lblPreviewFirst = new JLabel("All selections must be previewed first.");
		lblPreviewFirst.setForeground(Color.RED);

		btnRunWIAPreview.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runWavePreview();
			}

		});

		btnSaveMetrics = new JCButton("Save Metrics");
		btnSaveMetrics.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!validateCurrentSelectionName()) {
					return; // error already shown
				}
				WIAData wiaData = getCurrentData();
				String selName = wiaData.getSelectionName();
				String[][] data = wiaData.toCSV(selName);

				File fileToSave = getPrimaryDataWIASave(NamingConvention.PATHNAME_WIACSV, selName, true);
				if (fileToSave == null)
					return;
				String errors = Saver.saveData(fileToSave, data);
				if (errors != null) {
					Utils.showError("Error occurred while saving: " + errors, ref.get());
					btnSaveMetrics.setIcon(Utils.IconFail);
				} else {
					btnSaveMetrics.setIcon(Utils.IconSuccess);
				}
			}
		});

		btnRunWIA = new JCButton("Run Wave Analysis");
		btnRunWIA.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				runNextWIASelection();
			}

		});

		btnNextSelection = new JCButton("Next Selection");
		btnNextSelection.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// Check if user saved previous data
				if (ref.get().btnSaveMetrics.getIcon() != Utils.IconSuccess) {
					if (!Utils.confirmAction("Confirm Next",
							"You have NOT saved the data. Are you sure you want to go to the next selection?",
							ref.get())) {
						return;
					}
				} else if (ref.get().btnSaveIndividualBeatImages.getIcon() != Utils.IconSuccess) {
					if (!Utils.confirmAction("Confirm Next",
							"You have NOT saved images of beats. If you go to the next selection, and then decide to save them, the images "
									+ "for this past selection will not be saved.",
							ref.get())) {
						return;
					}
				} else if (ref.get().btnSaveSelectionEnsembledBeat.getIcon() != Utils.IconSuccess) {
					if (!Utils.confirmAction("Confirm Next",
							"You have NOT saved the raw ensembled beat data. If you go to the next selection, and then decide to save them later, the raw data "
									+ "for this past selection will not be saved.",
							ref.get())) {
						return;
					}
				}
				prepareNextWIASelection(true);
			}

		});

		btnNextFiles = new JCButton("Next File");
		btnNextFiles.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Check if user saved previous data
				if (ref.get().btnSaveMetrics.getIcon() != Utils.IconSuccess) {
					if (!Utils.confirmAction("Confirm Next",
							"You have NOT saved the data. Are you sure you want to go to the next file?", ref.get())) {
						return;
					}
				} else if (ref.get().btnSaveIndividualBeatImages.getIcon() != Utils.IconSuccess) {
					if (!Utils.confirmAction("Confirm Next",
							"You have NOT saved images of beats. If you go to the next file they will be lost. Sure you want to proceed?",
							ref.get())) {
						return;
					}
				} else if (ref.get().btnSaveSelectionEnsembledBeat.getIcon() != Utils.IconSuccess) {
					if (!Utils.confirmAction("Confirm Next",
							"You have NOT saved the raw ensembled beat data. If you go to the next file it will be lost. Sure you want to proceed?",
							ref.get())) {
						return;
					}
				}
				if (Utils.confirmAction("Confirm Next",
						"Are you sure you would like to proceed to the next file? This will skip any selections "
								+ "that you have not analyzed yet.",
						ref.get())) {
					reset(false);
				}

			}

		});

		JLabel lblSelectionRemaining = new JLabel("Selections remaining:");
		txtSelectionRemaining = new JTextField("");
		txtSelectionRemaining.setColumns(10);
		txtSelectionRemaining.setEditable(false);

		JLabel lblSelectionName = new JLabel("Selection name:");
		txtSelectionName = new JTextField("");

		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setForeground(Color.GRAY);
		JSeparator separator2 = new JSeparator();
		separator2.setOrientation(SwingConstants.HORIZONTAL);
		separator2.setForeground(Color.GRAY);
		JSeparator separator3 = new JSeparator();
		separator3.setOrientation(SwingConstants.HORIZONTAL);
		separator3.setForeground(Color.GRAY);

		JLabel lblWIA = new JLabel("Wave Intensity Analysis");

		gl_pnlWIA.setHorizontalGroup(gl_pnlWIA.createSequentialGroup().addGap(4)
				.addGroup(gl_pnlWIA.createParallelGroup(Alignment.LEADING).addComponent(lblWIA)
						.addGroup(gl_pnlWIA.createSequentialGroup().addContainerGap().addComponent(btnRunWIAPreview)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(lblPreviewFirst))
						.addGroup(gl_pnlWIA.createSequentialGroup().addContainerGap().addComponent(separator3))
						.addGroup(gl_pnlWIA.createSequentialGroup().addContainerGap().addComponent(lblSelectionName)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(txtSelectionName,
										GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
						.addGroup(gl_pnlWIA.createSequentialGroup().addContainerGap().addComponent(btnRunWIA)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnSaveMetrics))
						.addGroup(gl_pnlWIA.createSequentialGroup().addContainerGap().addComponent(separator2,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
						.addGroup(gl_pnlWIA.createSequentialGroup().addContainerGap()
								.addComponent(lblSelectionRemaining).addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(txtSelectionRemaining, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
										Short.MAX_VALUE)
								.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(btnNextSelection)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnNextFiles)))
				.addContainerGap(10, GroupLayout.PREFERRED_SIZE));

		gl_pnlWIA.setVerticalGroup(gl_pnlWIA.createSequentialGroup().addGap(4).addComponent(lblWIA)
				.addPreferredGap(ComponentPlacement.UNRELATED)
				.addGroup(gl_pnlWIA.createParallelGroup(Alignment.CENTER).addComponent(btnRunWIAPreview).addComponent(
						lblPreviewFirst, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED).addComponent(separator3)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_pnlWIA.createParallelGroup(Alignment.CENTER).addComponent(lblSelectionName).addComponent(
						txtSelectionName, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_pnlWIA.createParallelGroup(Alignment.CENTER).addComponent(btnRunWIA)
						.addComponent(btnSaveMetrics))
				.addPreferredGap(ComponentPlacement.RELATED).addComponent(separator2)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_pnlWIA.createParallelGroup(Alignment.CENTER).addComponent(lblSelectionRemaining)
						.addComponent(txtSelectionRemaining, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(btnNextSelection)
						.addComponent(separator, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(btnNextFiles))
				.addContainerGap());

		pnlWIA.setLayout(gl_pnlWIA);
		Utils.setFont(Utils.getSubTitleFont(), lblWIA);
		Utils.setFont(Utils.getTextFont(false), lblSelectionName, txtSelectionName, lblSelectionRemaining,
				txtSelectionRemaining, lblPreviewFirst);

	}

	/**
	 * Sets the state of the GUI, enabling and disabling panels according to the
	 * current step in the workflow (e.g., file selection, trimming, alignment).
	 * 
	 * @param state The integer constant representing the current state.
	 */
	private void setPanelState(int state) {
		switch (state) {
		case STATE_INIT:
			Utils.setEnabledDeep(false, true, true, pnlResample, pnlTrim, pnlFileTwo);
			Utils.setEnabledDeep(false, false, true, pnlAlign, pnlWIA);
			Utils.setEnabledDeep(true, false, true, pnlFileOne);
			_setColumnList(listColsFile1, null);
			_setColumnList(listColsFile2, null);
			_setFileName(txtFile1, null, null);
			_setFileName(txtFile2, null, null);
			txtSelectionName.setText("");
			txtSelectionRemaining.setText("");
			_nullifyButtonIcons();
			break;
		case STATE_FILE_ONE_SELECTED:
			Utils.setEnabledDeep(false, true, true, pnlResample, pnlTrim);
			Utils.setEnabledDeep(false, false, true, pnlAlign, pnlWIA); // leave the filter parameters present
			Utils.setEnabledDeep(true, false, true, pnlFileOne, pnlFileTwo);
			Utils.setEnabled(false, false, btnSelectFile1);
			_setColumnList(listColsFile2, null);
			_setFileName(txtFile2, null, null);
			break;
		case STATE_RESAMPLE:

			Utils.setEnabledDeep(false, true, true, pnlTrim);
			Utils.setEnabledDeep(false, false, true, pnlAlign, pnlWIA);
			Utils.setEnabledDeep(true, false, true, pnlFileOne, pnlFileTwo, pnlResample);
			Utils.setEnabled(false, false, btnSelectFile1, btnSelectFile2, btnSaveResampled);
			this.txtResampFreq.setText(config.getResampleString());
			break;
		case STATE_TRIM:
			txtResampFreq.setBorder(borderDefaultResamp);
			Utils.setEnabledDeep(false, false, true, pnlAlign, pnlWIA);
			Utils.setEnabledDeep(true, false, true, pnlFileOne, pnlFileTwo, pnlResample, pnlTrim);
			Utils.setEnabled(false, false, btnSelectFile1, btnSelectFile2, btnStartResamp, txtResampFreq, btnSaveTrim1,
					btnSaveTrim2);
			Utils.setEnabled(true, false, btnSaveResampled);
			break;
		case STATE_ALIGN:
			Utils.setEnabledDeep(false, false, true, pnlWIA);
			Utils.setEnabledDeep(true, false, true, pnlFileOne, pnlFileTwo, pnlResample, pnlTrim, pnlAlign);
			Utils.setEnabled(false, false, btnSelectFile1, btnSelectFile2, btnStartResamp, txtResampFreq,
					btnSaveResampled, btnSaveTrim1, btnSaveTrim2, btnAcceptTrims, btnTrim1, btnTrim2,
					btnSaveSelectionEnsembledBeat, btnSaveIndividualBeatImages);
			cbAlignEnsembleType.setSelectedItem(config.getEnsembleType());
			chPreAlignFilter.setSelected(config.isPreBeatSelectionFilterEnabled());
			txtSavWindow.setText(config.getPreBeatSelectionFilterWindowString());
			txtSavPolynomialOrder.setText(config.getPreBeatSelectionFilterPolyString());
			txtSavSampleRate.setText(config.getPreBeatSelectionResampleRateString());
			txtSavSampleRate.setEnabled(true);
			Utils.setEnabled(config.isPreBeatSelectionFilterEnabled(), false, txtSavWindow, txtSavPolynomialOrder);

			dataManager.applyTrims(false);
			break;
		case STATE_WIA:
			Utils.setEnabledDeep(true, false, true, pnlFileOne, pnlFileTwo, pnlResample, pnlTrim, pnlAlign, pnlWIA);
			Utils.setEnabled(false, false, btnSelectFile1, btnSelectFile2, btnStartResamp, txtResampFreq,
					btnSaveResampled, btnSaveTrim1, btnSaveTrim2, btnAcceptTrims, btnTrim1, btnTrim2, btnRunAlignment,
					chPreAlignFilter, txtSavPolynomialOrder, txtSavSampleRate, txtSavWindow, cbAlignEnsembleType,
					txtSelectionName, btnRunWIA, btnNextSelection, btnNextFiles, btnSaveMetrics, txtSelectionRemaining);
			break;
		}
	}

	/**
	 * Sets the progress bar. If enabled, need to provide progress level and the
	 * maximum.
	 * 
	 * @param enabled  whether to enabled the progress bar
	 * @param progress the current progress level, must be <= maximum
	 * @param maximum  maximum progress level
	 */
	public void setProgressBarEnabled(boolean enabled, int progress, int maximum) {
		if (!enabled) {
			progressBar.setVisible(false);
		} else {
			progressBar.setVisible(true);
			if (progress < 0 || maximum <= 0 || progress > maximum)
				throw new IllegalArgumentException(
						"When enabled progress bar, progress must be >= 0 and maximum > 0, with progress <= maximum");

			progressBar.setMaximum(maximum);
			progressBar.setValue(progress);
		}
	}

	/**
	 * Sets the progress bar progress
	 * 
	 * @param progress the current progress level, must be <= maximum
	 */
	public void setProgressBarProgress(int progress) {
		if (progress > progressBar.getMaximum()) {
			progressBar.setValue(progressBar.getValue());
		} else {

			progressBar.setValue(progress);
		}
	}

	/**
	 * Resets the icons of the various save buttons to null, clearing any success or
	 * failure indicators.
	 */
	private void _nullifyButtonIcons() {
		btnSaveSelectionEnsembledBeat.setIcon(null);
		btnSaveIndividualBeatImages.setIcon(null);
		btnSaveMetrics.setIcon(null);
		btnRunWIA.setIcon(null);
	}

	/**
	 * Manages the process of selecting a data file, reading its headers, letting
	 * the user configure options, and reading the data in a background thread.
	 * 
	 * @param isFileOne True if selecting the first file, false for the second file.
	 */
	private void runFileSelection(boolean isFileOne) {

		JList<Header> list = isFileOne ? this.listColsFile1 : this.listColsFile2;
		JTextField text = isFileOne ? this.txtFile1 : this.txtFile2;
		JButton btnSelectFile = isFileOne ? btnSelectFile1 : btnSelectFile2;

		final File file = Utils.promptUserForFile("Get Data File (CSV, TXT, XLS, XLSX)", config.getLastDirectoryPath(),
				new String[] { ".csv", ".txt", ".xls", ".xlsx" });
		if (file == null)
			return;

		else if (!isFileOne && dataManager.data1.getFile().getPath().equals(file.getPath())) {
			Utils.showError("Cannot select the same file twice.", this);
			return;
		}

		try {
			config.tryToSetLastDir(file);
		} catch (IOException e) {
			// fail silently, just don't save
			e.printStackTrace();
		}

		int numRowsIgnore = -1;

		if (config.getAutoHeader()) {
			numRowsIgnore = -1; // specifies auto determine
		} else {
			Integer numRowsIgnoreObj = Utils.promptIntegerNumber(
					"Some files have data in the first few rows we want to ignore.\n\nNumber of rows to ignore:", this);

			if (numRowsIgnoreObj == null) {
				Utils.showError("Invalid number of rows to ignore.", this);
				return;
			} else {
				numRowsIgnore = Math.max(-1, numRowsIgnoreObj);
			}
		}

		// disable button. Will be further handled by the async process
		btnSelectFile.setEnabled(false);
		SheetDataReader dataReader = new SheetDataReader(file, numRowsIgnore);

		BackgroundTaskExecutor.executeTask((BackgroundProgressRecorder progress) -> {

			HeaderResult hr = dataReader.readHeaders(progress);

			if (!hr.isSuccess()) {
				Utils.showError(hr.getErrors(), this);
				return null;
			}
			if (hr.getHeaders().isEmpty()) {
				Utils.showError("No headers found in file", this);
				return null;
			} else if (hr.getHeaders().size() < 2) {
				Utils.showError("Must have at least two headers - one domain and at least one range.", this);
				return null;
			}

			// Have user select name, headers to pulls, and the header for alignment
			// We will take EKG to to be the default
			Header defaultHeader = null;
			List<Header> excludes = new ArrayList<Header>();
			for (Header header : hr.getHeaders()) {
				if (config.getColumnsAlign().contains(header.getName())) {
					defaultHeader = header;
				} else if (config.getColumnsExclude().contains(header.getName())) {
					excludes.add(header);
				}

			}

			SheetOptionsSelectionGUI optionsDialog = new SheetOptionsSelectionGUI(
					FilenameUtils.removeExtension(file.getName()), hr.getHeaders(), excludes, defaultHeader, this);
			optionsDialog.setVisible(true);
			// Code will hang until it is closed

			OptionSelections options = optionsDialog.getOptionSelections();
			if (options == null)
				return null;

			ReadResult rr = dataReader.readData(options.selectedHeaders, progress);

			if (rr.getErrors() != null) {
				Utils.showError(rr.getErrors(), ref.get());
				return null;
			}

			return new AsyncFileSelectionResult(rr, options);
		}, progressBar, result -> {

			setProgressBarEnabled(false, -1, -1);

			if (result == null) {
				// There was some sort of error, already handled
				btnSelectFile.setEnabled(true);
				return;
			} else {
				_setColumnList(list, result.options().selectedHeaders);
				_setFileName(text, result.options().name + " (" + file.getName() + ")", file);
				HemoData hd = result.read().getData();
				OptionSelections op = result.options();

				hd.setName(op.name);
				hd.addFlags(op.headerForAlign, HemoData.OTHER_ALIGN);
				op.headerForAlign.addAdditionalMeta(Header.META_ALIGN, null);
				op.headerForAlign.addAdditionalMeta(Header.META_COLOR, Color.RED);

				if (isFileOne) {
					dataManager.data1 = hd;
					setPanelState(STATE_FILE_ONE_SELECTED);
				} else {
					dataManager.data2 = hd;
					setPanelState(STATE_RESAMPLE);
				}

			}

		});

	}

	/**
	 * Launches the {@link TrimGUI} to allow the user to visually trim the start and
	 * end of the resampled data for one of the files.
	 * 
	 * @param isFile1 {@code true} to trim the first file's data, {@code false} for
	 *                the second.
	 */
	private void runTrimSelection(boolean isFile1) {
		if (!areBothFilesSelected()) {
			Utils.showError("Data not yet resampled.", this);
			return;
		}

		TrimGUI trimGUI;

		if (isFile1) {
			HemoData justECG = dataManager.resampled1.blankCopy("Just ECG");
			trimGUI = new TrimGUI(dataManager.resampled1.getName(), justECG, dataManager.trimIndices1);
		} else {
			HemoData justECG = dataManager.resampled2.blankCopy("Just ECG");

			trimGUI = new TrimGUI(dataManager.resampled2.getName(), justECG, dataManager.trimIndices2);
		}

		trimGUI.setVisible(true);
		// code will hang until returns because it is a JDialog

		int[] trimIndices = trimGUI.getTrimIndices();
		if (isFile1) {
			dataManager.trimIndices1 = trimIndices;
			if (trimIndices != null && (trimIndices[0] != -1 || trimIndices[1] != -1)) {
				// changed
				Utils.setEnabled(true, false, btnSaveTrim1);
			} else {
				Utils.setEnabled(false, false, btnSaveTrim1);
			}
		} else {
			dataManager.trimIndices2 = trimIndices;
			if (trimIndices != null && (trimIndices[0] != -1 || trimIndices[1] != -1)) {
				// changed
				Utils.setEnabled(true, false, btnSaveTrim2);
			} else {
				Utils.setEnabled(false, false, btnSaveTrim2);
			}
		}

	}

	/**
	 * Gathers filtering/resampling settings, applies them to the data, and then
	 * launches the {@link AlignerGUI} for the user to align the two datasets and
	 * make beat selections.
	 */
	private void runAlignmentSelections() {

		if (chPreAlignFilter.isEnabled()) {
			// gets disabled after the first filter run so that filter does not keep getting
			// run
			Double sampleRate = validAlignResampleRate();
			if (sampleRate == null) {
				return; // error occured in validating, msg was already displayed
			}
			SavGolSettings filterParams = null;
			if (chPreAlignFilter.isSelected()) {
				try {
					filterParams = Savgol.generateSettings(txtSavWindow.getText(), txtSavPolynomialOrder.getText());
				} catch (Exception e) {
					Utils.showError(e.getMessage(), this);
					return;
				}
			}

			// resample rate chosen
			if (!Double.isNaN(sampleRate)) {

				double currSampleRate = HemoData.calculateAverageInterval(dataManager.resampled1.getXData());
				if (Math.abs(sampleRate - currSampleRate) > 0.0000001) {
					// Resample because they are not sampled at the specified rate
					try {
						dataManager.resampled1 = dataManager.resampled1.resampleAt(sampleRate);
						dataManager.resampled2 = dataManager.resampled2.resampleAt(sampleRate);
					} catch (ResampleException e) {
						e.printStackTrace();
						Utils.showError("Internal error. Could not resample.", this);
						return;
					}
				}
			}

			// filter settings chosen
			if (filterParams != null) {
				Savgol sav = new Savgol(filterParams.window, filterParams.polyOrder);
				for (Header header : new ArrayList<Header>(dataManager.resampled1.getYHeaders())) {
					if (dataManager.resampled1.hasFlag(header, HemoData.OTHER_ALIGN)) {
						continue;
					} else {
						double[] data = dataManager.resampled1.getYData(header);
						dataManager.resampled1.applyFilter(header, sav.filter(data));
					}

				}
				for (Header header : new ArrayList<Header>(dataManager.resampled2.getYHeaders())) {
					if (dataManager.resampled2.hasFlag(header, HemoData.OTHER_ALIGN)) {
						continue;
					} else {
						double[] data = dataManager.resampled2.getYData(header);
						dataManager.resampled2.applyFilter(header, sav.filter(data));
					}
				}

			}

		}

		Utils.setEnabled(false, false, txtSavWindow, txtSavPolynomialOrder, chPreAlignFilter, txtResampFreq,
				cbAlignEnsembleType);

		// TODO: make smarter
		dataManager.resampled1.addFlags(dataManager.resampled1.getXHeader(), HemoData.UNIT_SECONDS);
		dataManager.resampled2.addFlags(dataManager.resampled2.getXHeader(), HemoData.UNIT_SECONDS);

		AlignerGUI alignGUI = null;
		try {
			alignGUI = new AlignerGUI(dataManager.resampled1, dataManager.resampled2,
					EnsembleTypeMap.get((String) cbAlignEnsembleType.getSelectedItem()), this);
			alignGUI.display();
		} catch (OutOfMemoryError e) {
			// the input HemoData was too large
			Utils.showError("Memory error. Data set too large. Try trimming.", null);
			reset(false);
			return;
		} catch (IllegalArgumentException e) {
			Utils.showError("Error. " + e.getMessage(), null);
			reset(false);
		}

		// hangs until finished

		alignResult = alignGUI.getResult();
		if (alignResult == null)
			return; // user cancelled

		setPanelState(STATE_WIA);

	}

	/**
	 * Runs WIA analysis preview using the filter parameters
	 */
	private boolean runWavePreview() {

		List<HemoData> beats = new ArrayList<HemoData>();
		List<PreviewResult> beatsResult = new ArrayList<PreviewResult>();

		for (Beat beat : alignResult.getBeats()) {
			beats.add(beat.getData());
			beatsResult.add(null);

		}

		final SavGolSettings defSav = Savgol.generateSettings(config.getPreWIAFilterWindowString(),
				config.getPreWIAFilterPolyString());
		final boolean defSavEnabled = config.isPreWIAFilterEnabled();
		final boolean defAllowAlignWrap = true;
		final boolean defAllowAlignWrapExcessDisc = false;
		SavGolSettings currSav = defSav.copy();
		boolean currSavEnabled = config.isPreWIAFilterEnabled();
		boolean currAllowAlignWrap = true;
		boolean currAllowAlignWrapExcessDisc = false;

		int currSelectionIndex = 0;
		boolean maintain = true;

		int status = WavePickerGUI.PREVIEW_NEXT;

		while (status == WavePickerPreviewGUI.PREVIEW_NEXT || status == WavePickerPreviewGUI.PREVIEW_LAST) {
			HemoData beatCopy = beats.get(currSelectionIndex);
			boolean hasNext = currSelectionIndex < beats.size() - 1;
			boolean hasPrevious = currSelectionIndex > 0;
			PreviewResult pr = beatsResult.get(currSelectionIndex);
			if (pr != null) {
				currAllowAlignWrap = pr.isAllowWrap();
				currAllowAlignWrapExcessDisc = pr.isAllowWrapIgnoreEnds();
				currSavEnabled = pr.isFilterEnabled();
				currSav = pr.getSettings();
			} else if (!maintain) {

				currAllowAlignWrap = defAllowAlignWrap;
				currAllowAlignWrapExcessDisc = defAllowAlignWrapExcessDisc;
				currSavEnabled = defSavEnabled;
				currSav = defSav;

			}

			WavePickerPreviewGUI wavepickerGUI = new WavePickerPreviewGUI(beatCopy.getName() + " [Separate]", beatCopy,
					pr, hasPrevious, hasNext, currSav, currSavEnabled, currAllowAlignWrap, currAllowAlignWrapExcessDisc,
					maintain, this);
			wavepickerGUI.display();
			// hangs

			status = wavepickerGUI.getStatus();
			PreviewResult prCurr = wavepickerGUI.getPreviewResult();

			beatsResult.set(currSelectionIndex, prCurr);

			switch (status) {
			case WavePickerPreviewGUI.PREVIEW_NEXT:
				currSelectionIndex++;
				currSelectionIndex = Math.min(currSelectionIndex, beats.size() - 1); // safe guard to not go past last
																						// index, but shouldn't happen
				break;
			case WavePickerPreviewGUI.PREVIEW_LAST:
				currSelectionIndex--;
				currSelectionIndex = Math.max(currSelectionIndex, 0); // safe guard to not go to negative index, but
																		// shouldn't happen
				break;
			case WavePickerPreviewGUI.DONE:
				// the for loop will exit
				break;
			}

			maintain = wavepickerGUI.getMaintainSetting();

			if (maintain) {
				currAllowAlignWrap = prCurr.isAllowWrap();
				currAllowAlignWrapExcessDisc = prCurr.isAllowWrapIgnoreEnds();
				currSavEnabled = prCurr.isFilterEnabled();
				currSav = prCurr.getSettings();
			}

		}

		if (beatsResult.stream().anyMatch(Objects::isNull)) {
			StringBuilder sb = new StringBuilder();
			sb.append("You did not preview ");
			String comma = "";
			for (int i = 0; i < beats.size(); i++) {
				if (beatsResult.get(i) == null) {
					sb.append(comma).append("'").append(beats.get(i).getName()).append("'");
					comma = ", ";
				}
			}
			Utils.showError(sb.toString(), this);
			previewResultData = null;

			return false;
		}
		previewResultData = new LinkedList<PreviewResult>();
		for (int i = 0; i < beatsResult.size(); i++) {
			previewResultData.add(beatsResult.get(i));
		}

		prepareNextWIASelection(false);

		return true;
	}

	/**
	 * Prepares the WIA panel for the next available selection. It updates the UI
	 * with the new selection's name and enables the appropriate controls.
	 * 
	 * @param remove If {@code true}, the currently processed selection is removed
	 *               from the queue.
	 */
	private void prepareNextWIASelection(boolean remove) {

		Utils.setEnabled(true, false, txtSelectionName, btnRunWIA, btnNextSelection, btnNextFiles, btnSaveMetrics,
				txtSelectionRemaining);

		btnRunWIAPreview.setEnabled(false);

		if (remove && !previewResultData.isEmpty()) {
			previewResultData.remove(0);
		}

		// All previews have been completed
		if (previewResultData.isEmpty()) {
			reset(false);
			return;
		}

		PreviewResult pr = previewResultData.get(0);

		txtSelectionName.setText(pr.getWIAData().getSelectionName());

		_updateNumSelectionsLeftWIA();
		btnRunWIA.setEnabled(true);
		btnRunWIA.setIcon(null);
		btnSaveMetrics.setEnabled(false);
		btnSaveMetrics.setIcon(null);

	}

	/**
	 * Runs the final wave intensity analysis for the current selection by launching
	 * the {@link WavePickerGUI}. After the user defines waves, it calculates
	 * metrics and enables saving options.
	 */
	private void runNextWIASelection() {

		if (!validateCurrentSelectionName()) {
			return; // error already shown
		}

		btnRunWIAPreview.setEnabled(false);

		PreviewResult pr = previewResultData.get(0);
		WavePickerGUI wavepickerGUI = new WavePickerGUI(pr.getWIAData().getSelectionName() + " [Separate]",
				pr.getWIAData(), config.getSaveSettingsChoices(), ref.get(), this, pr);
		wavepickerGUI.display();

		if (config.getSaveSettingsChoices().hasChanged()) {
			try {
				config.writeProperties();
				config.getSaveSettingsChoices().setChanged(false);
			} catch (IOException e) {
				// fail silently, just don't save
				e.printStackTrace();
			}

		}

		if (wavepickerGUI.getStatus() == WavePickerGUI.CANCELLED) {
			return;
		}

		btnRunWIA.setIcon(Utils.IconSuccess);

		if (wavepickerGUI.serializeWIAData()) {
			try {

				File fileToSave = getPrimaryDataWIASave(NamingConvention.PATHNAME_WIASerialize,
						pr.getWIAData().getSelectionName(), true);
				if (fileToSave != null) {
					WIAData.serialize(pr.getWIAData(), fileToSave);
				}

			} catch (Exception ex) {
				Utils.showError(
						"Unable to save the current WIA data state. This may be due to lack of permissions to save in the current "
								+ "directory. You will not be able to re-edit wave selections at a later point. System error msg: "
								+ ex.getMessage(),
						ref.get());
			}
		}

		// Waves and systole / diastole were set. NOW need to run calculations
		pr.getWIAData().calculateWavePeaksAndSum();
		pr.getWIAData().calculateResistance();

		// Allow saving
		btnSaveMetrics.setEnabled(true);

	}

	/**
	 * Set the number in the field {@link SeparateWireGUI#txtSelectionRemaining}
	 * which indicates the number of selections (i.e. treatments) remaining.
	 * 
	 * Has the format <b>[number] ([selection_1], [selection_2], ...)</b> <br>
	 * 
	 * @param beatsLeft The beats which are remaining, including the currently
	 *                  evaluating beat (must be at index 0)
	 */
	private void _updateNumSelectionsLeftWIA() {

		if (previewResultData == null || previewResultData.size() <= 1) {
			txtSelectionRemaining.setText("None");
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(alignResult.getBeats().size() - 1).append(" (");
			String comma = "";
			for (int i = 1; i < previewResultData.size(); i++) {
				sb.append(comma).append("\"").append(previewResultData.get(i).getWIAData().getSelectionName())
						.append("\"");
				comma = ", ";
			}
			sb.append(")");
			txtSelectionRemaining.setText(sb.toString());
		}

	}

	/**
	 * @return true if the {@link RASData} manager has {@link HemoData} loaded for
	 *         both files.
	 */
	public boolean areBothFilesSelected() {
		return (this.dataManager.data1 != null && this.dataManager.data2 != null);
	}

	/**
	 * Partial validation of user input of resample frequency in real time, i.e.
	 * text can be converted to a number, and the number is > 0
	 * 
	 * @return {@code true} if valid, {@code false} otherwise.
	 */
	private boolean validateResamplePanelSilent() {
		try {
			String text = txtResampFreq.getText().trim();
			double freq = Double.parseDouble(text);
			return freq > 0;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Validates the user input for the resample frequency in the resample panel.
	 * 
	 * @return A string representation of the valid frequency, or null if the input
	 *         is invalid.
	 */
	private String validateResamplePanel() {

		Double freq = null;
		String text = this.txtResampFreq.getText();
		String errors = null;
		if (text == null || text.isBlank()) {
			errors = "Enter a number.";
		} else {

			try {
				freq = Double.valueOf(text);

				double[] range1 = Utils.getBounds(dataManager.data1.getXData());
				double[] range2 = Utils.getBounds(dataManager.data2.getXData());

				if (range1 == null || range2 == null) {
					freq = null;
					errors = "Internal error";
				} else {
					double minRange = Math.max(range1[1] - range1[0], range2[1] - range2[0]);
					if (freq > minRange) {
						freq = null;
						errors = "Resample frequency too large";
					}
				}
			} catch (Exception e) {
				freq = null;
				errors = "Plesae enter a decimal number only.";
			}

		}

		if (errors != null) {
			Utils.showError(errors, this);
		}
		if (freq != null)
			return text.trim();
		else
			return null;

	}

	/**
	 * Validates the resample rate entered in the alignment panel.
	 * 
	 * @return The resample rate as a {@link Double}. Returns {@link Double#NaN} if
	 *         blank (no resampling), or {@code null} if there is a validation
	 *         error.
	 */
	private Double validAlignResampleRate() {

		Double resampleFreq = null;
		String resampleSavTxt = txtSavSampleRate.getText();
		if (!resampleSavTxt.isBlank()) {
			try {
				resampleFreq = Double.parseDouble(resampleSavTxt);

				double[] range1 = Utils.getBounds(dataManager.resampled1.getXData());
				double[] range2 = Utils.getBounds(dataManager.resampled2.getXData());
				double minRange = Math.max(range1[1] - range1[0], range2[1] - range2[0]);
				if (resampleFreq > minRange) {
					Utils.showError("Resample frequency too large", this);
					return null;
				} else if (resampleFreq <= 0) {
					Utils.showError("Resample frequency must be positive", this);
					return null;
				}
			} catch (NumberFormatException e) {
				Utils.showError("Resample frequency must be a (decimal) number", this);
				return null;
			}

			return resampleFreq;
		} else {

			return Double.NaN;
		}

	}

	/**
	 * Evaluates the {@link SeparateWireGUI#txtSelectionName} text. If it is empty,
	 * then it displays an error to the user and replaces the textfield with the
	 * current / previous selection's name
	 * 
	 * @return true if successfully updated, false otherwise
	 */
	private boolean validateCurrentSelectionName() {
		WIAData data = previewResultData.isEmpty() ? null : previewResultData.get(0).getWIAData();

		if (data == null || !txtSelectionName.isEnabled()) {
			return false;
		}

		String text = txtSelectionName.getText();
		if (text.isBlank()) {
			Utils.showError("The Selection Name cannot be blank!", this);
			txtSelectionName.setText(data.getSelectionName());
			return false;
		}
		data.setSelectionName(text);
		return true;

	}

	/**
	 * Utility method to set the file name in the text box which displays the
	 * current file selection name
	 */
	private void _setFileName(JTextField txtFile, String name, File file) {
		txtFile.setFocusable(true);
		txtFile.setEditable(false);
		txtFile.setOpaque(true);
		if (name == null) {
			txtFile.setText("");
			txtFile.setEnabled(false);
			txtFile.setBackground(Color.LIGHT_GRAY);
			txtFile.setDisabledTextColor(Color.DARK_GRAY);

		} else {
			txtFile.setBackground(Color.WHITE);
			txtFile.setForeground(Color.BLACK);

			txtFile.setText(name);
			txtFile.setToolTipText(file.getPath());
			txtFile.setEnabled(true);

		}
	}

	/**
	 * Utility method to set the headers for the currently selected file. Will
	 * display X data in blue, and have an [align] tag after the column used for
	 * alignment
	 */
	private void _setColumnList(JList<Header> jlist, List<Header> headers) {
		jlist.setDragEnabled(false);
		jlist.setFocusable(false);
		jlist.setCellRenderer(new ListCellRenderer<Header>() {

			@Override
			public Component getListCellRendererComponent(JList<? extends Header> list, Header value, int index,
					boolean isSelected, boolean cellHasFocus) {

				String append = value.hasAdditionalMeta(Header.META_ALIGN) ? " [ALIGN]" : "";
				JLabel label = new JLabel(value.getName() + " (Col " + value.getCol() + ")" + append);
				if (value.isX()) {
					label.setForeground(Color.BLUE);
				}

				return label;
			}

		});
		if (headers == null) {
			jlist.setListData(new Header[0]);
			jlist.setEnabled(false);
			jlist.setBackground(Color.LIGHT_GRAY);

			return;
		} else {
			jlist.setEnabled(true);
			jlist.setBackground(Color.WHITE);
			jlist.setListData(headers.toArray(new Header[0]));

		}
	}

	/**
	 * Saves images of the {@link Beat} selections.
	 * 
	 * @param beat      The beat of interest
	 * @param svgImages Images, in the format of an SVG string
	 * @return error String, otherwise null
	 */
	private String saveBeatImages(Beat beat, List<String> svgImages) {

		String selectionName = beat.getData().getName();
		if (selectionName == null || selectionName.isBlank())
			return "Blank name for beat, invalid.";

		File folder = getSelectionDataFolder(selectionName);
		if (folder == null) {
			return "Could not create folder to save data.";
		}

		int counter = 1;
		for (String str : svgImages) {
			try {

				ComboChartSaver.saveSVGString(str, new File(folder.getPath() + File.separator
						+ String.format(NamingConvention.PATHNAME_BeatSelectionsSVG, selectionName, counter)));
			} catch (IOException e) {
				return "Could not save Beat SVG.";
			}
			counter++;
		}
		return null;
	}

	/**
	 * Saves {@link Beat} selections
	 * 
	 * @param beat the {@link Beat} of interest
	 * @return error String, otherwise null
	 */
	private String saveEnsembledBeatData(Beat beat) {

		String selectionName = beat.getData().getName();
		if (selectionName == null || selectionName.isBlank())
			return "Blank name for beat, invalid.";

		File folder = getSelectionDataFolder(selectionName);
		if (folder == null) {
			return "Could not create folder to save data.";
		}

		String errors = beat.getData().saveToSheet(new File(folder.getPath() + File.separator
				+ String.format(NamingConvention.PATHNAME_BeatSelectionsCSV, selectionName)));

		return errors;
	}

	/**
	 * Gets the {@link WIAData} object for the current selection being processed.
	 * 
	 * @return The current {@link WIAData}, or null if none is available.
	 */
	public WIAData getCurrentData() {
		if (previewResultData == null || previewResultData.isEmpty())
			return null;

		return previewResultData.get(0).getWIAData();
	}

	/**
	 * Gets the file path for the SVG image of the final WIA plot.
	 * 
	 * @return A {@link File} object for the SVG file.
	 */
	@Override
	public File getWIAImageFileSVG() {
		if (getCurrentData() == null)
			return null;

		return getPrimaryDataWIASave(NamingConvention.PATHNAME_WIASVG, getCurrentData().getSelectionName(), true);
	}

	/**
	 * Gets the primary folder where WIA-related TIFF images are stored.
	 * 
	 * @return A {@link File} object representing the folder.
	 */
	@Override
	public File getWIAImageFolderTIFF() {
		return getPrimaryDataWIAFolder();
	}

	/**
	 * Gets the file path for the SVG image of the wave selections view.
	 * 
	 * @return A {@link File} object for the SVG file.
	 */
	@Override
	public File getWIAWaveSelectionsFileSVG() {
		if (getCurrentData() == null)
			return null;

		return getPrimaryDataWIASave(NamingConvention.PATHNAME_WaveSelectionsSVG, getCurrentData().getSelectionName(),
				true);
	}

	/**
	 * Constructs a file path for saving WIA-related output within a specific
	 * folder.
	 * 
	 * @param fileNameForm    The format string for the file name (e.g., "%s
	 *                        WIA.csv").
	 * @param fileNameReplace The string to substitute into the file name format.
	 * @param ignoreExisting  If false, prompts the user to confirm overwriting an
	 *                        existing file.
	 * @return A {@link File} object for saving, or null if the user cancels.
	 */
	private File getPrimaryDataWIASave(String fileNameForm, String fileNameReplace, boolean ignoreExisting) {
		if (dataManager.data1 == null) {
			Utils.showError("No file to save to.", null);
			return null;
		}

		if (fileNameReplace != null) {
			fileNameForm = String.format(fileNameForm, fileNameReplace);
		}

		File folder = getPrimaryDataWIAFolder();
		if (folder == null) {
			return null;
		}

		File file = new File(folder + File.separator + fileNameForm);

		if (!ignoreExisting) {
			if (file.exists()) {
				boolean okay = Utils.confirmAction("Confirm Overwrite", "A file by the name \"" + file.getName()
						+ "\" already exists, would you " + "like to overwrite it?", null);
				if (!okay) {
					return null;
				}
			}
		}

		return file;
	}

	/**
	 * Gets a data folder to store pictures of the beats and the raw beat
	 * {@link HemoData} CSV
	 * 
	 * In the original folder where the file was opened, creates a new folder
	 * WIA_[treatment_name] and returns this folder
	 */
	private File getSelectionDataFolder(String selection) {
		File folder = null;

		try {

			folder = new File(dataManager.resampled1.getFile().getParent() + File.separator + "WIA_"
					+ Utils.stripInvalidFileNameCharacters(selection));
			folder.mkdir();

		} catch (Exception ex) {
			return null;
		}
		return folder;
	}

	/**
	 * Gets or creates the main output folder ("WIA_Data") for all analysis results.
	 * 
	 * @return A {@link File} object representing the folder, or null on failure.
	 */
	private File getPrimaryDataWIAFolder() {

		if (getCurrentData() == null)
			return null;

		File folder = null;
		try {
			if (isReopen) {
				folder = new File(getCurrentData().getSerializeFileSource().getParent());

			} else {
				folder = new File(dataManager.resampled1.getFile().getParent() + File.separator + "WIA_Data");
				folder.mkdir();
			}

		} catch (Exception ex) {
			Utils.showError("Could not create folder to save data.", this);
			return null;
		}
		return folder;
	}

	private record AsyncFileSelectionResult(ReadResult read, OptionSelections options) {
	}

	/**
	 * A private inner class used as a data container to hold all the hemodynamic
	 * data throughout the different stages of the analysis process (Raw, Resampled,
	 * Trimmed).
	 */
	private static class RASData {

		private volatile HemoData data1 = null;
		private volatile HemoData data2 = null;
		private volatile HemoData resampled1 = null;
		private volatile HemoData resampled2 = null;

		private int[] trimIndices1 = null;
		private int[] trimIndices2 = null;

		/**
		 * Gets the X-axis (domain) data from both raw data files.
		 * 
		 * @return A 2D array where each sub-array is the X-data for one of the files.
		 */
		private double[][] getDomains() {

			return new double[][] { data1.getXData(), data2.getXData() };
		}

		/**
		 * Applies the stored trim indices to the resampled data.
		 * 
		 * @param onCopy If true, applies the trim to a copy of the data; otherwise,
		 *               modifies the data in-place.
		 */
		private void applyTrims(boolean onCopy) {
			applyTrim(trimIndices1, resampled1, onCopy);
			applyTrim(trimIndices2, resampled2, onCopy);
		}

		/**
		 * Checks if both datasets have been trimmed.
		 * 
		 * @return true if trim indices have been set for both files.
		 */
		@SuppressWarnings("unused")
		private boolean areBothTrimmed() {
			return trimIndices1 != null && trimIndices2 != null;
		}

		/**
		 * Applies trim indices to a {@link HemoData} object.
		 * 
		 * @param trimIndices An array of two integers: the start and end indices for
		 *                    the trim.
		 * @param data        The {@link HemoData} to trim.
		 * @param onCopy      If true, the trim is applied to a copy; otherwise, it's
		 *                    done in-place.
		 * @return The trimmed {@link HemoData} object.
		 */
		private static HemoData applyTrim(int[] trimIndices, HemoData data, boolean onCopy) {

			HemoData hd = onCopy ? data.copy() : data;
			if (trimIndices == null)
				return hd;

			int[] trimIndicesFinal = new int[] { trimIndices[0], trimIndices[1] };

			if (trimIndicesFinal[0] == -1) {
				trimIndicesFinal[0] = 0;
			}

			if (trimIndicesFinal[1] == -1) {
				trimIndicesFinal[1] = hd.getXData().length - 1;
			}
			hd.trimByIndex(trimIndicesFinal[0], trimIndicesFinal[1]);
			return hd;
		}

	}

}
