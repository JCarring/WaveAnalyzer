package com.carrington.WIA.GUIs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;

import com.carrington.WIA.Utils;
import com.carrington.WIA.Cardio.Beat;
import com.carrington.WIA.DataStructures.HemoData;
import com.carrington.WIA.DataStructures.WIAData;
import com.carrington.WIA.GUIs.BeatSelectorGUI.SelectionResult;
import com.carrington.WIA.GUIs.WavePickerPreviewGUI.PreviewResult;
import com.carrington.WIA.GUIs.Components.JCButton;
import com.carrington.WIA.GUIs.Configs.ComboFileConfigGUI;
import com.carrington.WIA.Graph.ComboChartSaver;
import com.carrington.WIA.Graph.PressureFlowChartPanel;
import com.carrington.WIA.IO.Header;
import com.carrington.WIA.IO.HeaderResult;
import com.carrington.WIA.IO.NamingConvention;
import com.carrington.WIA.IO.ReadResult;
import com.carrington.WIA.IO.Saver;
import com.carrington.WIA.IO.SheetDataReader;
import com.carrington.WIA.Math.Savgol;
import com.carrington.WIA.Math.DataResampler.ResampleException;
import com.carrington.WIA.Math.Savgol.SavGolSettings;

/**
 * A graphical user interface for analyzing hemodynamic data from a combination
 * pressure / flow system which is a single file containing at least time,
 * pressure, flow, and ECG data. The workflow guides the user through file
 * selection, beat selection, and wave intensity analysis.
 */
public class CombowireGUI extends JFrame implements WIACaller {

	private static final long serialVersionUID = 882079419657857245L;

	private JFrame frameToGoBackTo = null;
	private BackListener backListener = null;

	private static final int widthScaler = Utils.getMaxAppSize().width;
	private static final int heightScaler = Utils.getMaxAppSize().height;

	private static final int STATE_INIT = 0;
	private static final int STATE_BEATS = 1;
	private static final int STATE_WIA = 2;

	/**
	 * A map to associate user-friendly ensemble type names ("Trim", "Scale") with
	 * their corresponding HemoData constants.
	 */
	public static final LinkedHashMap<String, Integer> EnsembleTypeMap = new LinkedHashMap<String, Integer>();
	static {
		EnsembleTypeMap.put("Trim", HemoData.ENSEMBLE_TRIM);
		EnsembleTypeMap.put("Scale", HemoData.ENSEMBLE_SCALE);
	}

	private static final Header headerEmpty = new Header("< No R Wave Data >", 0, false);

	private final JLabel lblTopInstructions = new JLabel(
			"Select a file, then select beats and calculate wave intensity.");
	private JTextField txtFileName;
	private JTextField txtFilePath;
	private JTextField txtTime;
	private JTextField txtFlowOffset;
	private JComboBox<Header> cbRWave;
	private JComboBox<Header> cbECG;
	private JComboBox<Header> cbFlow;
	private JComboBox<Header> cbPressure;
	private JCButton btnRunBeatSel;
	private JCheckBox chUseFilesRWaves;
	private JCButton btnBrowseFile;
	{
		lblTopInstructions.setEnabled(true);
		lblTopInstructions.setFocusable(false);
		// lblTopInstructions.setLineWrap(true);
		lblTopInstructions.setBorder(null);
		lblTopInstructions.setBackground(null);
		lblTopInstructions.setForeground(Color.black);
		// lblTopInstructions.setDisabledTextColor(Color.BLACK);
	}
	private JPanel contentPane;
	private volatile File currFile = null;
	private volatile boolean currFileIsWIAReOpen = false;
	private volatile JLabel lblProcessing;
	private final WeakReference<CombowireGUI> ref = new WeakReference<CombowireGUI>(this);
	private JPanel pnlSelectFile;
	private JPanel pnlBeats;

	private volatile HemoData data = null;
	private JPanel topPanel;
	private JPanel pnlButtons;
	private JPanel pnlWIA;
	private JComboBox<String> cbEnsembleType;
	private JCButton btnSaveSelectionEnsembledBeat;
	private JCButton btnSaveIndividualBeatImages;
	private JTextField txtSampleRate;

	private JTextField txtSelectionName;
	private JTextField txtSelectionRemaining;
	private JButton btnRunWIAPreview;
	private JCButton btnRunWIA;
	private JCButton btnSaveMetrics;
	private JCButton btnNextFile;
	private JCButton btnNextSelection;
	private JLabel lblPreviewFirst;

	private final ComboFileConfigGUI config = new ComboFileConfigGUI();

	private SelectionResult selectionResult;
	private LinkedList<PreviewResult> previewResultData = null;

	/**
	 * Creates the main frame for the Combowire analysis GUI.
	 * 
	 * @param frameToGoBackTo The parent frame to return to when this frame is
	 *                        closed. Can be null.
	 * @param back            A listener to notify when the "back" action is
	 *                        performed. Can be null.
	 */
	public CombowireGUI(JFrame frameToGoBackTo, BackListener back) {
		System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");

		this.frameToGoBackTo = frameToGoBackTo;
		this.backListener = back;
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setTitle("Wave Analysis - Combined Flow and Pressure");
		contentPane = new JPanel();
		// contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});

		initTop();
		initPnlSelectRun();
		initPnlBeats();
		initBottom();
		initPnlWIA();

		JPanel middlePanel = new JPanel();
		middlePanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		GroupLayout mainContentPaneLayout = new GroupLayout(contentPane);
		mainContentPaneLayout.setHorizontalGroup(mainContentPaneLayout.createSequentialGroup().addContainerGap()
				.addGroup(mainContentPaneLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(topPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(middlePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE)
						.addComponent(pnlButtons, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE))
				.addContainerGap());
		mainContentPaneLayout.setVerticalGroup(mainContentPaneLayout.createSequentialGroup().addContainerGap()
				.addComponent(topPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(middlePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addPreferredGap(ComponentPlacement.RELATED).addComponent(pnlButtons, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addContainerGap());

		GroupLayout gl_middlePanel = new GroupLayout(middlePanel);
		gl_middlePanel
				.setHorizontalGroup(gl_middlePanel.createParallelGroup(Alignment.LEADING).addGroup(Alignment.TRAILING,
						gl_middlePanel.createSequentialGroup().addContainerGap()
								.addGroup(gl_middlePanel.createParallelGroup(Alignment.CENTER)
										.addComponent(pnlSelectFile, Alignment.LEADING, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
										.addComponent(pnlBeats, Alignment.LEADING, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
										.addComponent(pnlWIA, Alignment.LEADING, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
								.addContainerGap()));
		gl_middlePanel.setVerticalGroup(gl_middlePanel.createSequentialGroup().addContainerGap()
				.addComponent(pnlSelectFile, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(pnlBeats, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(pnlWIA, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)

				.addContainerGap());
		middlePanel.setLayout(gl_middlePanel);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		JMenu mnBeats = new JMenu("Beats");
		JMenu mnWaveAnalysis = new JMenu("Waves");
		JMenu mnHelp = new JMenu("Help");

		menuBar.add(mnFile);
		menuBar.add(mnBeats);
		menuBar.add(mnWaveAnalysis);
		menuBar.add(mnHelp);

		JMenuItem mnSelectFile = new JMenuItem("Select input file...");
		mnSelectFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnBrowseFile.doClick();
			}
		});
		mnSelectFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.META_MASK));
		mnFile.add(mnSelectFile);
		mnFile.addSeparator();
		JMenuItem mnSettings = new JMenuItem("Settings");
		mnSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				config.open(ref.get());
			}
		});
		mnSettings.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.META_MASK));
		mnFile.add(mnSettings);

		mnFile.addSeparator();

		JMenuItem mnReset = new JMenuItem("Reset");
		mnReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reset(true);

			}
		});
		mnFile.add(mnReset);
		mnReset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.META_MASK));

		JMenuItem mnBackToSTart = new JMenuItem("Back to start");
		mnBackToSTart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Utils.confirmAction("Confirm", "You will lose all progress. Sure you want to go back?",
						ref.get())) {
					navigateBack();
				}
			}
		});
		mnFile.add(mnBackToSTart);

		mnFile.addSeparator();

		JMenuItem mnQuit = new JMenuItem("Quit");
		mnQuit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				quit();
			}

		});
		mnFile.add(mnQuit);

		JMenuItem mnRunBeatSel = new JMenuItem("Run Beats Selection");
		mnRunBeatSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnRunBeatSel.doClick();
			}
		});
		mnRunBeatSel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.META_MASK));
		JMenuItem mnSaveSelections = new JMenuItem("Save Selections");
		mnSaveSelections.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnSaveSelectionEnsembledBeat.doClick();

			}
		});
		mnSaveSelections.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.META_MASK));
		JMenuItem mnSaveBeatImages = new JMenuItem("Save Beat Images");
		mnSaveBeatImages.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnSaveIndividualBeatImages.doClick();
			}
		});
		mnSaveBeatImages.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.META_MASK));
		mnBeats.add(mnRunBeatSel);
		mnBeats.addSeparator();
		mnBeats.add(mnSaveSelections);
		mnBeats.add(mnSaveBeatImages);

		JMenuItem mnRunWIA = new JMenuItem("Run WIA");
		mnRunWIA.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnRunWIA.doClick();
			}
		});
		mnRunWIA.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.META_MASK));
		JMenuItem mnNextFile = new JMenuItem("Next File");
		mnNextFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnNextFile.doClick();
			}
		});
		mnNextFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.META_MASK));
		JMenuItem mnNextSel = new JMenuItem("Next selection");
		mnNextSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnNextSelection.doClick();
			}
		});
		mnNextSel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.META_MASK));
		JMenuItem mnSaveMetrics = new JMenuItem("Save metrics");
		mnSaveMetrics.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnSaveMetrics.doClick();
			}
		});
		mnSaveMetrics.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.META_MASK));
		mnWaveAnalysis.add(mnRunWIA);
		mnWaveAnalysis.addSeparator();
		mnWaveAnalysis.add(mnSaveMetrics);
		mnWaveAnalysis.addSeparator();
		mnWaveAnalysis.add(mnNextSel);
		mnWaveAnalysis.add(mnNextFile);
		
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
					java.awt.Desktop.getDesktop().browse(new java.net.URI("https://github.com/JCarring/WaveAnalyzer/issues"));
				} catch (Exception ex) {
					Utils.showError("Could not browse internet", ref.get());
				}

			}

		});
		
		Utils.setMenuBarFont(Utils.getSubTitleFont(), getJMenuBar());


		// setSelectFileState(STATE_FILE_INIT, null, null, null);
		// setBeatsState(STATE_BEATS_INIT, null, null);
		setPanelState(STATE_INIT);
		Utils.unfocusButtons(contentPane);
		contentPane.setLayout(mainContentPaneLayout);
		setContentPane(contentPane);

		pack();
		Dimension dm = new Dimension((int) Math.max(widthScaler / 2, getMinimumSize().width),
				(int) Math.min(Math.max((3.0 / 4.0) * heightScaler, getMinimumSize().height), heightScaler));
		this.setMinimumSize(dm);
		this.setPreferredSize(dm);
		// this.setResizable(false);
		this.setLocationRelativeTo(null);
		// this.pack();

	}

	/**
	 * Initializes the top panel of the GUI, which contains instructions for the
	 * user.
	 */
	private void initTop() {
		topPanel = new JPanel();
		topPanel.setBackground(new Color(192, 192, 192));
		topPanel.setBorder(new LineBorder(new Color(0, 0, 0)));

		lblTopInstructions.setFont(Utils.getTextFont(false));

		JLabel lblInstruction = new JLabel("Instructions");
		lblInstruction.setFont(Utils.getSubTitleFont());
		GroupLayout gl_topPanel = new GroupLayout(topPanel);
		gl_topPanel.setHorizontalGroup(gl_topPanel.createSequentialGroup().addGap(3)
				.addGroup(gl_topPanel.createParallelGroup(Alignment.LEADING)
						.addComponent(lblInstruction, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addGroup(gl_topPanel.createSequentialGroup().addContainerGap().addComponent(lblTopInstructions,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)))
				.addContainerGap());
		gl_topPanel.setVerticalGroup(gl_topPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_topPanel.createSequentialGroup().addComponent(lblInstruction)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(lblTopInstructions,
								GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						.addContainerGap()));
		topPanel.setLayout(gl_topPanel);
	}

	/**
	 * Initializes the bottom panel of the GUI, which contains the main action
	 * buttons (Reset, Go Back, Quit) and a processing label.
	 */
	private void initBottom() {
		pnlButtons = new JPanel();
		pnlButtons.setBackground(new Color(192, 192, 192));
		pnlButtons.setBorder(new LineBorder(new Color(0, 0, 0)));

		pnlButtons.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));

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
		btnReset.setMnemonic('R');

		JCButton btnQuit = new JCButton("Quit", JCButton.BUTTON_QUIT);
		btnQuit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				quit();
			}
		});

		pnlButtons.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(null, btnQuit);
		lblProcessing = new JLabel("Processing...");
		lblProcessing.setFont(Utils.getTextFont(true));
		lblProcessing.setForeground(Color.RED);
		lblProcessing.setVisible(false);
		pnlButtons.add(lblProcessing);

		pnlButtons.add(btnReset);
		pnlButtons.add(btnBack);
		pnlButtons.add(btnQuit);
	}

	/**
	 * Initializes the panel for file selection and data column mapping. This panel
	 * allows the user to browse for a file and assign columns to pressure, flow,
	 * ECG, and R-wave data.
	 */
	private void initPnlSelectRun() {
		pnlSelectFile = new JPanel();
		pnlSelectFile.setBorder(new LineBorder(new Color(0, 0, 0)));

		JLabel lblSelectFile = new JLabel("Input Data");
		txtFileName = new JTextField();
		txtFileName.setColumns(10);
		txtFileName.setEditable(false);
		txtFileName.setBackground(Color.WHITE);
		txtFileName.setEnabled(true);
		txtFileName.setFocusable(false);

		txtFilePath = new JTextField();
		txtFilePath.setColumns(10);
		txtFilePath.setEditable(false);
		txtFilePath.setBackground(Color.WHITE);
		txtFilePath.setEnabled(true);
		txtFilePath.setFocusable(false);

		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setForeground(Color.GRAY);

		JLabel lblTimeField = new JLabel("Time Field:", SwingConstants.RIGHT);

		txtTime = new JTextField();
		txtTime.setEnabled(true);
		txtTime.setEditable(false);
		txtTime.setColumns(10);
		txtTime.setFocusable(false);
		txtTime.setBackground(Color.WHITE);

		JLabel lblPressure = new JLabel("Pressure:", SwingConstants.RIGHT);
		cbPressure = new JComboBox<Header>();
		JLabel lblFlow = new JLabel("Flow:", SwingConstants.RIGHT);
		cbFlow = new JComboBox<Header>();
		JLabel lblECG = new JLabel("ECG:", SwingConstants.RIGHT);
		cbECG = new JComboBox<Header>();
		JLabel lblRWave = new JLabel("RWave:", SwingConstants.RIGHT);
		cbRWave = new JComboBox<Header>();

		JLabel lblFileName = new JLabel("File Name:", SwingConstants.RIGHT);
		JLabel lblPath = new JLabel("Path:", SwingConstants.RIGHT);

		btnBrowseFile = new JCButton("Select File...", JCButton.BUTTON_STANDARD);

		GroupLayout gl_pnlSelectRun = new GroupLayout(pnlSelectFile);
		gl_pnlSelectRun.setHorizontalGroup(gl_pnlSelectRun.createSequentialGroup().addGap(4).addGroup(gl_pnlSelectRun
				.createParallelGroup(Alignment.LEADING)
				.addComponent(lblSelectFile, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addGroup(gl_pnlSelectRun.createSequentialGroup().addGap(10).addGroup(gl_pnlSelectRun
						.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_pnlSelectRun.createSequentialGroup().addComponent(btnBrowseFile,
								GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGroup(
								gl_pnlSelectRun.createSequentialGroup()
										.addGroup(gl_pnlSelectRun.createParallelGroup(Alignment.TRAILING)
												.addComponent(lblFileName, GroupLayout.PREFERRED_SIZE,
														GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
												.addComponent(lblPath, GroupLayout.PREFERRED_SIZE,
														GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(ComponentPlacement.RELATED)
										.addGroup(gl_pnlSelectRun.createParallelGroup(Alignment.LEADING)
												.addComponent(txtFileName, GroupLayout.PREFERRED_SIZE,
														GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
												.addComponent(txtFilePath, GroupLayout.PREFERRED_SIZE,
														GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(separator, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE)
						.addGroup(gl_pnlSelectRun.createParallelGroup(Alignment.TRAILING)
								.addComponent(lblECG, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(lblPressure, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(lblFlow, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(lblRWave, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(lblTimeField, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlSelectRun.createParallelGroup(Alignment.LEADING)
								.addComponent(txtTime, 240, 240, 240).addComponent(cbFlow, 240, 240, 240)
								.addComponent(cbECG, 240, 240, 240).addComponent(cbRWave, 240, 240, 240)
								.addComponent(cbPressure, 240, 240, 240))))
				.addContainerGap());
		gl_pnlSelectRun.setVerticalGroup(gl_pnlSelectRun.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_pnlSelectRun.createSequentialGroup().addGap(4).addComponent(lblSelectFile)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlSelectRun
								.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_pnlSelectRun.createSequentialGroup().addComponent(btnBrowseFile)
										.addPreferredGap(ComponentPlacement.UNRELATED).addGroup(gl_pnlSelectRun
												.createParallelGroup(Alignment.LEADING)
												.addComponent(
														txtFileName, GroupLayout.PREFERRED_SIZE,
														GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
												.addComponent(lblFileName))
										.addPreferredGap(ComponentPlacement.RELATED)
										.addGroup(gl_pnlSelectRun.createParallelGroup(Alignment.LEADING)
												.addComponent(
														txtFilePath, GroupLayout.PREFERRED_SIZE,
														GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
												.addComponent(lblPath)))
								.addGroup(
										gl_pnlSelectRun.createSequentialGroup()
												.addGroup(gl_pnlSelectRun.createParallelGroup(Alignment.BASELINE)
														.addComponent(txtTime, GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
														.addComponent(lblTimeField))
												.addPreferredGap(ComponentPlacement.RELATED)
												.addGroup(gl_pnlSelectRun.createParallelGroup(Alignment.BASELINE)
														.addComponent(cbPressure, GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
														.addComponent(lblPressure))
												.addPreferredGap(ComponentPlacement.RELATED)
												.addGroup(gl_pnlSelectRun.createParallelGroup(Alignment.BASELINE)
														.addComponent(lblFlow).addComponent(cbFlow,
																GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE))
												.addPreferredGap(ComponentPlacement.RELATED)
												.addGroup(gl_pnlSelectRun.createParallelGroup(Alignment.BASELINE)
														.addComponent(cbECG, GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
														.addComponent(lblECG))
												.addPreferredGap(ComponentPlacement.RELATED)
												.addGroup(gl_pnlSelectRun.createParallelGroup(Alignment.BASELINE)
														.addComponent(cbRWave, GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
														.addComponent(lblRWave)))
								.addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE))
						.addContainerGap()));
		pnlSelectFile.setLayout(gl_pnlSelectRun);

		btnBrowseFile.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				runFileSelection();
			}

		});

		Utils.setFont(Utils.getTextFont(true), lblFlow, lblECG, lblRWave, lblPressure, btnBrowseFile, lblTimeField,
				txtTime, cbPressure, cbFlow, cbECG, cbRWave);
		Utils.setFont(Utils.getTextFont(false), txtFilePath, txtFileName, lblFileName, lblPath);
		Utils.setFont(Utils.getSubTitleFont(), lblSelectFile);
	}

	/**
	 * Initializes the panel for beat selection configuration. This includes
	 * settings for ensembling, flow offset, resampling, and launching the beat
	 * selection process.
	 */
	private void initPnlBeats() {

		pnlBeats = new JPanel();
		pnlBeats.setBorder(new LineBorder(new Color(0, 0, 0)));

		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setBackground(new Color(192, 192, 192));
		separator.setForeground(new Color(192, 192, 192));

		btnSaveSelectionEnsembledBeat = new JCButton("Save Selections", JCButton.BUTTON_STANDARD);
		btnSaveSelectionEnsembledBeat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Saves the ensembled Beat HemoData in a separate folder for each selection
				// (treatment)
				if (selectionResult == null || selectionResult.getBeats().isEmpty()) {
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

				for (Beat beat : selectionResult.getBeats()) {
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
				if (selectionResult == null || selectionResult.getBeats().isEmpty()) {
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

				for (Beat beat : selectionResult.getBeats()) {
					String error = saveBeatImages(beat, selectionResult.getBeatImages(beat));
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

		JLabel lblBeats = new JLabel("Selections");

		chUseFilesRWaves = new JCheckBox("Use File's R Waves");
		chUseFilesRWaves.setSelected(true);
		chUseFilesRWaves.setOpaque(false);
		chUseFilesRWaves.setFocusable(false);

		JLabel lblEnsembleType = new JLabel("Ensemble type:");
		cbEnsembleType = new JComboBox<String>(EnsembleTypeMap.keySet().toArray(new String[0]));
		cbEnsembleType.setOpaque(false);
		cbEnsembleType.setEditable(false);
		cbEnsembleType.setSelectedItem(config.getEnsembleType());
		FontMetrics metrics = cbEnsembleType.getFontMetrics(cbEnsembleType.getFont());
		int cbEnsembleWidth = 0;

		for (int i = 0; i < cbEnsembleType.getItemCount(); i++) {
			int currWidth = metrics.stringWidth(cbEnsembleType.getItemAt(i));
			cbEnsembleWidth = Math.max(currWidth, cbEnsembleWidth);
		}
		cbEnsembleWidth *= 3;

		JLabel lblFlowOffset = new JLabel("Flow Offset:");
		txtFlowOffset = new JTextField();
		txtFlowOffset.setColumns(10);
		txtFlowOffset.setText(config.getFlowOffset() + "");
		JLabel lblMS = new JLabel("milliseconds");

		JLabel lblSampRate = new JLabel("Resample rate (leave blank to not resample):");
		txtSampleRate = new JTextField("");
		txtSampleRate.setText(config.getResampleString());
		int fontWidthTxtSampleRate = Utils.getFontParams(Utils.getTextFont(false), "0.00001")[1];

		btnRunBeatSel = new JCButton("Start");
		btnRunBeatSel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				runBeatSelection();
			}
		});

		int widthTxtFlowOffset = Utils.getFontParams(Utils.getTextFont(false), "1000...")[1];

		GroupLayout gl_pnlBeats = new GroupLayout(pnlBeats);
		gl_pnlBeats.setHorizontalGroup(gl_pnlBeats.createParallelGroup(Alignment.LEADING).addGroup(gl_pnlBeats
				.createSequentialGroup().addGap(4)
				.addGroup(gl_pnlBeats.createParallelGroup(Alignment.LEADING).addComponent(lblBeats).addGroup(gl_pnlBeats
						.createSequentialGroup().addContainerGap().addComponent(lblEnsembleType).addGap(3)
						.addComponent(cbEnsembleType, GroupLayout.DEFAULT_SIZE, cbEnsembleWidth, cbEnsembleWidth)
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(lblFlowOffset).addGap(3)
						.addComponent(txtFlowOffset, GroupLayout.PREFERRED_SIZE, widthTxtFlowOffset,
								GroupLayout.PREFERRED_SIZE)
						.addGap(3).addComponent(lblMS).addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(chUseFilesRWaves))
						.addGroup(gl_pnlBeats.createSequentialGroup().addContainerGap().addComponent(lblSampRate)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(txtSampleRate,
										GroupLayout.PREFERRED_SIZE, fontWidthTxtSampleRate, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_pnlBeats.createSequentialGroup().addContainerGap().addComponent(separator,
								GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
						.addGroup(gl_pnlBeats.createSequentialGroup().addContainerGap().addComponent(btnRunBeatSel)
								.addPreferredGap(ComponentPlacement.UNRELATED)
								.addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.UNRELATED)
								.addComponent(btnSaveSelectionEnsembledBeat).addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(btnSaveIndividualBeatImages)))
				.addContainerGap()));
		gl_pnlBeats.setVerticalGroup(gl_pnlBeats.createParallelGroup(Alignment.LEADING).addGroup(gl_pnlBeats
				.createSequentialGroup().addGap(4).addComponent(lblBeats).addPreferredGap(ComponentPlacement.UNRELATED)
				.addGroup(gl_pnlBeats.createParallelGroup(Alignment.CENTER).addComponent(chUseFilesRWaves)
						.addComponent(lblEnsembleType).addComponent(cbEnsembleType).addComponent(lblFlowOffset)
						.addComponent(txtFlowOffset, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(lblFlowOffset).addComponent(lblMS))
				.addPreferredGap(ComponentPlacement.UNRELATED)
				.addGroup(gl_pnlBeats
						.createParallelGroup(Alignment.CENTER).addComponent(lblSampRate).addComponent(txtSampleRate))
				.addPreferredGap(ComponentPlacement.UNRELATED)
				.addGroup(gl_pnlBeats.createParallelGroup(Alignment.CENTER)
						.addComponent(btnRunBeatSel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(btnSaveSelectionEnsembledBeat, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnSaveIndividualBeatImages, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE))
				.addContainerGap()));
		pnlBeats.setLayout(gl_pnlBeats);

		Utils.setFont(Utils.getTextFont(true), btnRunBeatSel);
		Utils.setFont(Utils.getTextFont(false), lblFlowOffset, lblMS, txtFlowOffset, chUseFilesRWaves, lblEnsembleType,
				cbEnsembleType, lblSampRate, txtSampleRate);
		Utils.setFont(Utils.getSubTitleFont(), lblBeats);
	}

	/**
	 * Initializes the panel for Wave Intensity Analysis (WIA). This panel manages
	 * previewing wave profiles, running the final analysis, saving metrics, and
	 * navigating between different selections or files.
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

		btnNextFile = new JCButton("Next File");
		btnNextFile.addActionListener(new ActionListener() {

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
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnNextFile)))
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
						.addComponent(btnNextFile))
				.addContainerGap());

		pnlWIA.setLayout(gl_pnlWIA);
		Utils.setFont(Utils.getSubTitleFont(), lblWIA);
		Utils.setFont(Utils.getTextFont(false), lblSelectionName, txtSelectionName, lblSelectionRemaining,
				txtSelectionRemaining, lblPreviewFirst);

	}

	/**
	 * Sets the state of the GUI, enabling or disabling panels and components based
	 * on the current step in the analysis workflow.
	 * 
	 * @param state The state to set, e.g., {@link #STATE_INIT},
	 *              {@link #STATE_BEATS}, {@link #STATE_WIA}.
	 */
	private void setPanelState(int state) {
		switch (state) {
		case STATE_INIT:
			Utils.setEnabledDeep(false, false, true, pnlBeats, pnlWIA);
			Utils.setEnabledDeep(true, false, true, pnlSelectFile);
			Utils.setEnabled(false, true, cbECG, cbRWave, cbFlow, cbPressure);
			txtSelectionName.setText("");
			txtSelectionRemaining.setText("");
			txtFileName.setText("");
			txtFilePath.setText("");
			txtTime.setText("");
			setButtonIconsNull();
			break;
		case STATE_BEATS:
			Utils.setEnabledDeep(false, false, true, pnlWIA);
			Utils.setEnabledDeep(true, false, true, pnlSelectFile, pnlBeats);
			Utils.setEnabled(false, false, btnBrowseFile, btnSaveSelectionEnsembledBeat, btnSaveIndividualBeatImages);
			break;
		case STATE_WIA:
			Utils.setEnabledDeep(true, false, true, pnlSelectFile, pnlBeats, pnlWIA);

			Utils.setEnabled(false, false, btnBrowseFile, cbECG, cbRWave, cbFlow, cbPressure, btnRunBeatSel,
					txtFlowOffset, txtSampleRate, chUseFilesRWaves, cbEnsembleType, btnNextFile, btnNextSelection,
					btnSaveMetrics, txtSelectionName, txtSelectionRemaining, btnRunWIA

			);

			break;

		}
	}

	/**
	 * Handles the file selection process. Prompts the user to select a data file,
	 * reads the headers, and populates the UI with the column information for
	 * mapping.
	 */
	private synchronized void runFileSelection() {

		File file = Utils.promptUserForFile("Get Data File (CSV, TXT, XLS, XLSX)", config.getLastDirectoryPath(),
				new String[] { ".csv", ".txt", ".xls", ".xlsx" });
		if (file == null)
			return;

		config.tryToSetLastDir(file);

		int numRowsIgnore;

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
		SheetDataReader dataReader = new SheetDataReader(file, numRowsIgnore);

		// Attempt to get headers;
		HeaderResult hr = dataReader.readHeaders(null);
		if (!hr.isSuccess()) {
			Utils.showError(hr.getErrors(), this);
			return;
		}

		if (hr.getHeaders().isEmpty()) {
			Utils.showError("No headers found in file", this);
			return;
		} else if (hr.getHeaders().size() < 2) {
			Utils.showError("Must have at least two headers - one domain and at least one range.", this);
			return;
		}

		ReadResult dataResult = dataReader.readData(hr.getHeaders());
		if (dataResult.getErrors() != null) {
			Utils.showError(dataResult.getErrors(), this);
			return;
		}

		this.data = dataResult.getData();

		currFile = file;
		txtFileName.setText(file.getName());
		txtFilePath.setText(file.getPath());
		txtTime.setText(data.getXHeader().toString());

		List<String> configHeaderECG = config.getColumnsECG();
		List<String> configHeaderFlow = config.getColumnsFlow();
		List<String> configHeaderPressure = config.getColumnsPressure();
		List<String> configHeaderRWave = config.getColumnsRWave();
		for (Header header : data.getYHeaders()) {
			cbECG.addItem(header);
			if (configHeaderECG.contains(header.getName())) {
				cbECG.setSelectedItem(header);
			}
			cbFlow.addItem(header);
			if (configHeaderFlow.contains(header.getName())) {
				cbFlow.setSelectedItem(header);
			}
			cbPressure.addItem(header);
			if (configHeaderPressure.contains(header.getName())) {
				cbPressure.setSelectedItem(header);
			}
			cbRWave.addItem(header);
			if (configHeaderRWave.contains(header.getName())) {
				cbRWave.setSelectedItem(header);
			}

		}
		this.cbRWave.addItem(headerEmpty);
		this.cbRWave.setRenderer(new ListCellRenderer<Header>() {

			@Override
			public Component getListCellRendererComponent(JList<? extends Header> list, Header value, int index,
					boolean isSelected, boolean cellHasFocus) {
				JLabel label = new JLabel();
				label.setOpaque(true);

				if (isSelected) {
					label.setBackground(list.getSelectionBackground());
					label.setForeground(list.getSelectionForeground());
				} else {
					label.setBackground(list.getBackground());
					label.setForeground(list.getForeground());
				}
				if (value == null) {
					label.setText("");
				} else if (!value.equals(headerEmpty)) {
					label.setText(value.toString());

				} else {
					label.setText(value.getName());
					label.setForeground(Color.RED);
					if (isSelected) {
						list.setSelectionForeground(Color.RED);
					} else {
						list.setSelectionForeground(Color.BLACK);
					}

				}
				return label;
			}

		});

		setPanelState(STATE_BEATS);

	}

	/**
	 * Gathers settings from the UI, processes the input data (resampling, offset),
	 * and launches the {@link BeatSelectorGUI} to allow the user to select beats of
	 * interest.
	 */
	private void runBeatSelection() {

		Double sampleRate = validateResampleRate();
		if (sampleRate == null) {
			return; // error occured in validating, msg was already displayed
		}

		String textFlowOffset = this.txtFlowOffset.getText();
		int flowOffset;
		if (textFlowOffset == null || textFlowOffset.isBlank()) {
			flowOffset = 0;
		} else {
			try {
				flowOffset = Integer.parseInt(textFlowOffset);
			} catch (NumberFormatException e) {
				Utils.showError("Offset must be an integer.", this);
				return;
			}
		}

		String errors = data.isValid();
		if (errors != null) {
			Utils.showError(errors, null);
			return;
		}

		Header headerFlow = (Header) cbFlow.getSelectedItem();
		headerFlow.addAdditionalMeta(Header.META_COLOR, PressureFlowChartPanel.darkerFlowLineColor);
		Header headerPressure = (Header) cbPressure.getSelectedItem();
		headerPressure.addAdditionalMeta(Header.META_COLOR, PressureFlowChartPanel.darkerPressureLineColor);
		Header headerECG = (Header) cbECG.getSelectedItem();
		headerECG.addAdditionalMeta(Header.META_COLOR, Color.DARK_GRAY);
		Header headerRWave = (Header) cbRWave.getSelectedItem();

		if (!Utils.isDistinct(headerFlow, headerPressure, headerECG, headerRWave)) {
			Utils.showError("Pressure, Flow, ECG, and RWave columns must all be unique.", this);
			this.btnRunBeatSel.setEnabled(true);
			return;
		}
		// discards all the other columns we don't need
		data.deleteYVars(headerFlow, headerPressure, headerECG, headerRWave);
		// add flags
		data.addFlags(data.getXHeader(), HemoData.UNIT_SECONDS);
		data.addFlags(headerFlow, HemoData.TYPE_FLOW, HemoData.UNIT_CMperS);
		data.addFlags(headerPressure, HemoData.TYPE_PRESSURE, HemoData.UNIT_MMHG);
		data.addFlags(headerECG, HemoData.TYPE_ECG);
		data.addFlags(headerRWave, HemoData.TYPE_R_WAVE);

		if (!Double.isNaN(sampleRate)) {

			// Resample
			double currSampleRate = HemoData.calculateAverageInterval(data.getXData());

			if (Math.abs(sampleRate - currSampleRate) > 0.0000001) {
				// Resample because they are not sampled at the specified rate
				try {
					data = data.resampleAt(sampleRate);
				} catch (ResampleException e) {
					e.printStackTrace();
					Utils.showError("Internal error. Could not resample.", this);
					return;
				}
			}
		}

		if (flowOffset != 0) {
			// apply offset
			data.applyXOffset(headerFlow, flowOffset / 1000.0);
		}

		BeatSelectorGUI beatGUI = null;

		try {
			beatGUI = new BeatSelectorGUI(data, config.getRWaveSync(), HemoData.ENSEMBLE_SCALE, this);

			// main thread hangs until finished
			beatGUI.display();

		} catch (OutOfMemoryError e) {
			// the input HemoData was too large
			Utils.showError("Memory error. Data set too large. Try trimming.", null);
			reset(false);
			return;
		} catch (IllegalArgumentException e) {
			Utils.showError("Error. " + e.getMessage(), null);
			reset(false);
		}

		selectionResult = beatGUI.getResult();
		if (selectionResult == null)
			return; // user cancelled

		setPanelState(STATE_WIA);

	}

	/**
	 * Launches the {@link WavePickerPreviewGUI} for each beat selection. This
	 * allows the user to configure filtering and alignment parameters for each
	 * selection before running the final wave intensity analysis.
	 * 
	 * @return true if all selections were previewed successfully, false otherwise.
	 */
	private boolean runWavePreview() {

		List<HemoData> beats = new ArrayList<HemoData>();
		List<PreviewResult> beatsResult = new ArrayList<PreviewResult>();

		for (Beat beat : selectionResult.getBeats()) {
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

			WavePickerPreviewGUI wavepickerGUI = new WavePickerPreviewGUI(beatCopy.getName() + " [Combo]", beatCopy, pr,
					hasPrevious, hasNext, currSav, currSavEnabled, currAllowAlignWrap, currAllowAlignWrapExcessDisc,
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

		Utils.setEnabled(true, false, txtSelectionName, btnRunWIA, btnNextSelection, btnNextFile, btnSaveMetrics,
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
		WavePickerGUI wavepickerGUI = new WavePickerGUI(pr.getWIAData().getSelectionName() + " [Combo]", pr.getWIAData(),
				config.getSaveSettingsChoices(), ref.get(), this, pr);
		wavepickerGUI.display();

		if (config.getSaveSettingsChoices().hasChanged()) {
			config.writeProperties();
			config.getSaveSettingsChoices().setChanged(false);
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
			sb.append(selectionResult.getBeats().size() - 1).append(" (");
			String comma = "";
			for (int i = 1; i < previewResultData.size(); i++) {
				sb.append(comma).append("\"").append(previewResultData.get(i).getWIAData().getSelectionName()).append("\"");
				comma = ", ";
			}
			sb.append(")");
			txtSelectionRemaining.setText(sb.toString());
		}

	}

	/**
	 * Validates the current align sample rate. Shows user error if there is an
	 * issue.
	 * 
	 * @return resample rate, {@link Double#NaN} if error, or {@code null} if not
	 *         set (and therefore should not resample)
	 */
	private Double validateResampleRate() {

		Double resampleFreq = null;
		String resampleSavTxt = txtSampleRate.getText();
		if (!resampleSavTxt.isBlank()) {
			try {
				resampleFreq = Double.parseDouble(resampleSavTxt);

				double[] range = Utils.getBounds(data.getXData());
				if (resampleFreq > (range[1] - range[0])) {
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
	 * @return {@code true} if successfully updated, {@code false} otherwise
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
	 * Prompts the user for confirmation and then quits the application.
	 */
	public void quit() {

		if (Utils.confirmAction("Confirm Quit", "Are you sure you want to quit?", this)) {
			this.setVisible(false);
			System.exit(0);
		}

	}

	/**
	 * Resets the GUI to its initial state, clearing all data and selections.
	 * 
	 * @param warn If {@code true}, prompts the user for confirmation before
	 *             resetting.
	 */
	public void reset(boolean warn) {
		setPanelState(STATE_INIT);

		selectionResult = null;
		currFile = null;
		previewResultData = null;

	}

	/**
	 * Makes this GUI visible and hides the parent frame it was called from.
	 */
	public void navigateInto() {
		this.setVisible(true);
		if (this.frameToGoBackTo != null) {
			this.frameToGoBackTo.setVisible(false);
		}
	}

	/**
	 * Hides this GUI and makes the parent frame visible, triggering the back
	 * listener.
	 */
	public void navigateBack() {

		if (this.frameToGoBackTo != null) {
			this.setVisible(false);
			this.frameToGoBackTo.setVisible(true);
			this.backListener.wentBack();
		}
	}

	private void setButtonIconsNull() {
		btnSaveSelectionEnsembledBeat.setIcon(null);
		btnSaveIndividualBeatImages.setIcon(null);
		btnSaveMetrics.setIcon(null);
		btnRunWIA.setIcon(null);
	}

	/**
	 * Saves SVG images of the individual raw beats that were part of an ensembled
	 * selection.
	 *
	 * @param beat      The ensembled {@link Beat} whose constituent parts are to be
	 *                  saved.
	 * @param svgImages A list of base64-encoded SVG strings for each constituent
	 *                  beat.
	 * @return An error message string if saving fails, otherwise null.
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
	 * Gets a data folder to store pictures of the beats and the raw beat
	 * {@link HemoData} CSV
	 * 
	 * In the original folder where the file was opened, creates a new folder
	 * WIA_[treatment_name] and returns this folder
	 */
	private File getSelectionDataFolder(String selection) {
		File folder = null;

		try {

			folder = new File(
					currFile.getParent() + File.separator + "WIA_" + Utils.stripInvalidFileNameCharacters(selection));
			folder.mkdir();

		} catch (Exception ex) {
			return null;
		}
		return folder;
	}

	/**
	 * Retrieves the currently active {@link WIAData} object from the processing
	 * queue.
	 * 
	 * @return The current {@link WIAData} object, or null if the queue is empty.
	 */
	public WIAData getCurrentData() {
		if (previewResultData == null || previewResultData.isEmpty())
			return null;

		return previewResultData.get(0).getWIAData();
	}

	/**
	 * Gets or creates the primary output folder for all WIA data related to the
	 * current file. The folder is named "WIA_Data" and is located in the same
	 * directory as the input file.
	 * 
	 * @return A {@link File} object representing the folder, or null on failure.
	 */
	public File getPrimaryDataWIAFolder() {
		File folder = null;
		try {
			if (this.currFileIsWIAReOpen) {
				folder = new File(currFile.getParent());

			} else {
				folder = new File(currFile.getParent() + File.separator + "WIA_Data");
				folder.mkdir();
			}

		} catch (Exception ex) {
			Utils.showError("Could not create folder to save data.", null);
			return null;
		}
		return folder;
	}

	/**
	 * Constructs a file path for saving WIA-related output within the primary data
	 * folder.
	 * 
	 * @param fileNameForm    The format string for the file name (e.g., "%s
	 *                        WIA.csv").
	 * @param fileNameReplace The string to insert into the format string (e.g., the
	 *                        selection name).
	 * @param ignoreExisting  If false, the user will be prompted to confirm
	 *                        overwriting an existing file.
	 * @return A {@link File} object for the new file, or null if the operation is
	 *         cancelled.
	 */
	public File getPrimaryDataWIASave(String fileNameForm, String fileNameReplace, boolean ignoreExisting) {
		if (currFile == null || fileNameForm == null) {
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
	 * Gets the save file path for the main WIA plot as an SVG image.
	 * @return The {@link File} object for the SVG image.
	 */
	@Override
	public File getWIAImageFileSVG() {
		if (getCurrentData() == null)
			return null;

		return getPrimaryDataWIASave(NamingConvention.PATHNAME_BeatSelectionsSVG, getCurrentData().getSelectionName(),
				true);
	}

	/**
	 * Gets the primary output folder where TIFF images will be saved.
	 * @return The {@link File} object for the output folder.
	 */
	@Override
	public File getWIAImageFolderTIFF() {
		return getPrimaryDataWIAFolder();
	}

	/**
	 * Gets the save file path for the wave selections plot as an SVG image.
	 * @return The {@link File} object for the SVG image.
	 */
	@Override
	public File getWIAWaveSelectionsFileSVG() {
		if (getCurrentData() == null)
			return null;

		return getPrimaryDataWIASave(NamingConvention.PATHNAME_WaveSelectionsSVG, getCurrentData().getSelectionName(),
				true);
	}

}
