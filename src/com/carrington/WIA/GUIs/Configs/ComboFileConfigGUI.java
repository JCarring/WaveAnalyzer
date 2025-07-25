package com.carrington.WIA.GUIs.Configs;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import org.apache.commons.lang3.math.NumberUtils;

import com.carrington.WIA.Utils;
import com.carrington.WIA.GUIs.CombowireGUI;
import com.carrington.WIA.Math.Savgol;

/**
 * A {@link JDialog} window for managing configuration settings for the
 * Combowire analysis process. It provides UI for setting general options, run
 * configurations like flow offset and column mapping, and filter parameters.
 */
public class ComboFileConfigGUI extends JDialog {

	private static final long serialVersionUID = 5137417115882742282L;
	private static final String configFileDefaultPath = "/resources/configs/config-combo-default.properties";
	private static final File configFile = new File("./config_combo.properties");
	private static final String keyFlowOffset = "flow_offset";
	private static final String keyAutoHeader = "auto_set_header";
	private static final String keyAutoName = "auto_set_name";
	private static final String keyAutoSaveDir = "auto_save_in_start_directory";
	private static final String keyPressureCols = "pressure_columns";
	private static final String keyFlowCols = "flow_columns";
	private static final String keyECGCols = "ECG_columns";
	private static final String keyRWaveCols = "RWave_columns";

	private static final String keyLastDirectory = "last_dir";
	private static final String keySnapToR = "snap_to_R";

	private static final String keyBeatsResample = "resample";
	private static final String keyEnsembleType = "ensemble_type";
	private static final String keyWIAFilt = "WIA_filter_enabled";
	private static final String keyWIAWindow = "WIA_filter_window";
	private static final String keyWIAPoly = "WIA_filter_polyorder";

	private String opLastDir = ""; // not settable by the user

	// initialize these to their respective default values
	private int opFlowOffset = 0;
	private boolean opAutoHeader = true;
	private boolean opAutoName = true;
	private boolean opAutoSaveDir = false;
	private List<String> opColPressure = new ArrayList<String>();
	private List<String> opColFlow = new ArrayList<String>();
	private List<String> opColECG = new ArrayList<String>();
	private List<String> opColRWave = new ArrayList<String>();

	private String opResampleRate = "";
	private String opEnsembleType = CombowireGUI.EnsembleTypeMap.keySet().stream().findFirst().orElse("Trim");
	private boolean opPreWIAFilterEnable = false;
	private String opPreWIAFilterWindow = "";
	private String opPreWIAFilterPoly = "";

	private boolean opSnapToR = false;

	private JPanel contentPane;
	private JTextField txtOffset;
	private JCheckBox chAutoHeader;
	private JCheckBox chAutoName;
	private JCheckBox chAutoSaveDir;
	private JTextArea txtColumnsPressure;
	private JTextArea txtColumnsFlow;
	private JTextArea txtColumnsECG;
	private JTextArea txtColumnsRWave;
	private JCheckBox chPreWIAFilter;
	private JTextField txtPreWIAWindow;
	private JTextField txtPreWIAPoly;
	private JTextField txtResampleRate;
	private JComboBox<String> cbEnsembleType;

	private WIASaveSettingsChoices wiaSettings;

	private JCheckBox chSaveSettingsFile;

	private final WeakReference<ComboFileConfigGUI> ref = new WeakReference<ComboFileConfigGUI>(this);

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ComboFileConfigGUI frame = new ComboFileConfigGUI();
					frame.open(null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Creates the configuration settings frame for Combowire analysis. Initializes
	 * all UI components and loads existing properties from the configuration file.
	 * 
	 * @throws IOException if errors with I/O with the configuration file
	 */
	public ComboFileConfigGUI() throws IOException {

		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		Font titleFont = Utils.getTitleFont();
		Font subtitleFont = Utils.getSubTitleFont();
		Font normalBold = Utils.getTextFont(true);
		Font normalPlain = Utils.getTextFont(false);
		int size = normalPlain.getSize();

		setTitle("Settings");
		setModal(true);
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setBounds(100, 100, 800, 400);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);

		JLabel lblTitleSettings = new JLabel("Settings");
		lblTitleSettings.setFont(titleFont);

		JPanel pnlMain = new JPanel();
		pnlMain.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		JButton btnAccept = new JButton("Accept");
		btnAccept.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String errors = validateDisplayValues();
				if (errors != null) {
					Utils.showError(errors, ref.get());
					return;
				}
				recordDisplayValues();

				if (chSaveSettingsFile.isSelected()) {
					try {
						writeProperties();
					} catch (IOException e1) {
						Utils.showError("<html>There was an error saving the " + configFile + " file:<br><br>"
								+ e1.getMessage() + "</html>", ref.get());
						e1.printStackTrace();
					}
				}
				close();

			}
		});

		getRootPane().setDefaultButton(btnAccept);
		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				close();
				setDisplayValues();
			}
		});

		chSaveSettingsFile = new JCheckBox("Save these settings to file");
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addComponent(lblTitleSettings)
				.addGroup(gl_contentPane.createSequentialGroup()
						.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
								.addComponent(pnlMain, GroupLayout.DEFAULT_SIZE, 784, Short.MAX_VALUE)
								.addGroup(gl_contentPane.createSequentialGroup().addContainerGap()
										.addComponent(chSaveSettingsFile).addPreferredGap(ComponentPlacement.UNRELATED)
										.addComponent(btnCancel).addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(btnAccept).addContainerGap()))
						.addPreferredGap(ComponentPlacement.UNRELATED)));
		gl_contentPane.setVerticalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup().addComponent(lblTitleSettings)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(pnlMain, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE).addComponent(btnAccept)
								.addComponent(btnCancel).addComponent(chSaveSettingsFile))
						.addContainerGap()));

		JLabel lblGeneral = new JLabel("General");
		lblGeneral.setFont(subtitleFont);

		JLabel lblRunConfig = new JLabel("Run Configuration");
		lblRunConfig.setFont(subtitleFont);

		chAutoHeader = new JCheckBox("Auto-detect headers");
		chAutoName = new JCheckBox("Auto name files");
		chAutoSaveDir = new JCheckBox("Auto Save to directory");

		chAutoHeader.setFont(normalPlain);
		chAutoName.setFont(normalPlain);
		chAutoSaveDir.setFont(normalPlain);

		JLabel lblOffset = new JLabel("Flow Offset:");
		lblOffset.setFont(normalBold);

		txtOffset = new JTextField();
		txtOffset.setColumns(10);
		txtOffset.setFont(normalPlain);

		JLabel lblColPressure = new JLabel("Pressure Columns");
		JLabel lblColFlow = new JLabel("Flow Columns");
		JLabel lblColECG = new JLabel("ECG Columns");
		JLabel lblColRWave = new JLabel("R Wave Columns");

		JScrollPane scrColP = new JScrollPane();
		JScrollPane scrColF = new JScrollPane();
		JScrollPane scrColE = new JScrollPane();
		JScrollPane scrColR = new JScrollPane();

		JLabel lblResample = new JLabel("Resample rate:");
		lblResample.setFont(normalBold);

		txtResampleRate = new JTextField();
		txtResampleRate.setColumns(10);
		txtResampleRate.setFont(normalPlain);

		JLabel lblEnsembletype = new JLabel("Ensemble type:");
		cbEnsembleType = new JComboBox<String>();
		for (Entry<String, Integer> entry : CombowireGUI.EnsembleTypeMap.entrySet()) {
			cbEnsembleType.addItem(entry.getKey());
		}
		FontMetrics metrics = cbEnsembleType.getFontMetrics(cbEnsembleType.getFont());
		int cbEnsembleWidth = 0;

		for (int i = 0; i < cbEnsembleType.getItemCount(); i++) {
			int currWidth = metrics.stringWidth(cbEnsembleType.getItemAt(i));
			cbEnsembleWidth = Math.max(currWidth, cbEnsembleWidth);
		}
		cbEnsembleWidth *= 3; // double the width for padding

		JLabel lblPreWIAFilter = new JLabel("Pre-WIA SG Filter:");
		chPreWIAFilter = new JCheckBox("Enabled");
		JLabel lblPreWIAWindow = new JLabel("Window:");
		txtPreWIAWindow = new JTextField("");
		JLabel lblPreWIAPolyOrder = new JLabel("Polynomial order:");
		txtPreWIAPoly = new JTextField("");

		Utils.setFont(normalBold, lblColPressure, lblColFlow, lblColECG, lblColRWave);
		Utils.setFont(Utils.getTextFont(true), lblPreWIAFilter, lblEnsembletype);
		Utils.setFont(Utils.getTextFont(false), lblPreWIAWindow, lblPreWIAPolyOrder, chPreWIAFilter, txtPreWIAPoly,
				txtPreWIAWindow);

		int width = Utils.getFontParams(Utils.getTextFont(false), "0.0000001")[1];

		GroupLayout gl_panel = new GroupLayout(pnlMain);
		gl_panel.setHorizontalGroup(gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup().addContainerGap().addGroup(gl_panel
						.createParallelGroup(Alignment.LEADING).addComponent(lblGeneral).addComponent(lblRunConfig)
						.addGroup(gl_panel.createSequentialGroup().addGap(10).addComponent(chAutoHeader)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(chAutoName)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(chAutoSaveDir))
						.addGroup(gl_panel.createSequentialGroup().addGap(10)
								.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
										.addGroup(gl_panel.createSequentialGroup().addComponent(
												lblOffset).addPreferredGap(ComponentPlacement.RELATED).addComponent(
														txtOffset, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
														GroupLayout.PREFERRED_SIZE))
										.addGroup(gl_panel.createSequentialGroup()
												.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
														.addComponent(scrColP, GroupLayout.PREFERRED_SIZE, size * 15,
																GroupLayout.PREFERRED_SIZE)
														.addComponent(lblColPressure))
												.addPreferredGap(ComponentPlacement.UNRELATED)
												.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
														.addComponent(lblColFlow).addComponent(scrColF,
																GroupLayout.PREFERRED_SIZE, size * 15,
																GroupLayout.PREFERRED_SIZE))
												.addPreferredGap(ComponentPlacement.UNRELATED)
												.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
														.addComponent(lblColECG).addComponent(scrColE,
																GroupLayout.PREFERRED_SIZE, size * 15,
																GroupLayout.PREFERRED_SIZE))
												.addPreferredGap(ComponentPlacement.UNRELATED)
												.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
														.addComponent(lblColRWave).addComponent(scrColR,
																GroupLayout.PREFERRED_SIZE, size * 15,
																GroupLayout.PREFERRED_SIZE))

										)))

						.addGroup(gl_panel.createSequentialGroup().addGap(10).addComponent(lblResample)
								.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(txtResampleRate,
										GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_panel.createSequentialGroup().addGap(10).addComponent(lblEnsembletype)
								.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(cbEnsembleType,
										GroupLayout.DEFAULT_SIZE, cbEnsembleWidth, cbEnsembleWidth))
						.addGroup(gl_panel.createSequentialGroup().addGap(10).addComponent(lblPreWIAFilter)
								.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(chPreWIAFilter)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(lblPreWIAWindow)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(txtPreWIAWindow, GroupLayout.PREFERRED_SIZE, width,
										GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(lblPreWIAPolyOrder)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(txtPreWIAPoly,
										GroupLayout.PREFERRED_SIZE, width, GroupLayout.PREFERRED_SIZE)

						))

						.addContainerGap(278, Short.MAX_VALUE)));
		gl_panel.setVerticalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(gl_panel
				.createSequentialGroup().addContainerGap().addComponent(lblGeneral)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE).addComponent(chAutoHeader)
						.addComponent(chAutoName).addComponent(chAutoSaveDir))
				.addPreferredGap(ComponentPlacement.RELATED).addComponent(lblRunConfig)
				.addPreferredGap(ComponentPlacement.UNRELATED)
				.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE).addComponent(lblOffset).addComponent(
						txtOffset, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
						.addGroup(gl_panel.createSequentialGroup().addComponent(lblColPressure).addComponent(scrColP,
								GroupLayout.PREFERRED_SIZE, size * 10, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_panel.createSequentialGroup().addComponent(lblColFlow).addComponent(scrColF,
								GroupLayout.PREFERRED_SIZE, size * 10, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_panel.createSequentialGroup().addComponent(lblColECG).addComponent(scrColE,
								GroupLayout.PREFERRED_SIZE, size * 10, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_panel.createSequentialGroup().addComponent(lblColRWave).addComponent(scrColR,
								GroupLayout.PREFERRED_SIZE, size * 10, GroupLayout.PREFERRED_SIZE)))
				.addPreferredGap(ComponentPlacement.UNRELATED)
				.addGroup(gl_panel
						.createParallelGroup(Alignment.CENTER).addComponent(lblResample).addComponent(txtResampleRate))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_panel.createParallelGroup(Alignment.CENTER).addComponent(lblEnsembletype)
						.addComponent(cbEnsembleType))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_panel.createParallelGroup(Alignment.CENTER).addComponent(lblPreWIAFilter)
						.addComponent(chPreWIAFilter).addComponent(lblPreWIAWindow).addComponent(lblPreWIAPolyOrder)
						.addComponent(txtPreWIAWindow).addComponent(txtPreWIAPoly))
				.addContainerGap(55, Short.MAX_VALUE)));

		txtColumnsFlow = new JTextArea();
		scrColF.setViewportView(txtColumnsFlow);

		txtColumnsPressure = new JTextArea();
		scrColP.setViewportView(txtColumnsPressure);

		txtColumnsECG = new JTextArea();
		scrColE.setViewportView(txtColumnsECG);

		txtColumnsRWave = new JTextArea();
		scrColR.setViewportView(txtColumnsRWave);

		pnlMain.setLayout(gl_panel);
		contentPane.setLayout(gl_contentPane);
		setBounds(100, 100, this.getPreferredSize().width, this.getPreferredSize().height);
		setResizable(false);
		setLocationRelativeTo(null);

		readProperties();
		setDisplayValues();

	}

	/**
	 * Opens the settings dialog, making it visible and modal.
	 *
	 * @param component The {@link Component} to which the dialog should be
	 *                  positioned relative. Can be null.
	 */
	public void open(Component component) {
		setLocationRelativeTo(component);
		setModal(true);
		setDisplayValues();
		setVisible(true);
	}

	/**
	 * Closes the dialog by setting its visibility to false. Does not dispose of it.
	 */
	public void close() {
		setVisible(false);
	}

	/**
	 * Populates the GUI fields with the current option values stored in the
	 * instance variables.
	 */
	public void setDisplayValues() {

		this.chAutoHeader.setSelected(this.opAutoHeader);
		this.chAutoName.setSelected(this.opAutoName);
		this.chAutoSaveDir.setSelected(this.opAutoSaveDir);
		this.txtOffset.setText(String.valueOf(this.opFlowOffset));
		this.txtColumnsPressure.setText(listToDisplayString(this.opColPressure));
		this.txtColumnsFlow.setText(listToDisplayString(this.opColFlow));
		this.txtColumnsECG.setText(listToDisplayString(this.opColECG));
		this.txtColumnsRWave.setText(listToDisplayString(this.opColRWave));
		this.txtResampleRate.setText(this.opResampleRate);
		this.cbEnsembleType.setSelectedItem(this.opEnsembleType);
		this.chPreWIAFilter.setSelected(this.opPreWIAFilterEnable);
		this.txtPreWIAWindow.setText(this.opPreWIAFilterWindow);
		this.txtPreWIAPoly.setText(this.opPreWIAFilterPoly);

	}

	/**
	 * Validates the data entered into the GUI fields. Checks for a valid flow
	 * offset, unique column names across types, and valid Savitzky-Golay filter
	 * parameters.
	 *
	 * @return A {@code String} containing an error message if validation fails, or
	 *         {@code null} if all values are valid.
	 */
	public String validateDisplayValues() {

		try {
			Integer.parseInt(this.txtOffset.getText());
		} catch (Exception e) {
			return "Offset must be an integer";
		}

		if (this.txtColumnsPressure.getText().contains(",")) {
			return "Column name cannot contain a comma.";
		}

		if (this.txtColumnsFlow.getText().contains(",")) {
			return "Column name cannot contain a comma.";
		}

		if (this.txtColumnsECG.getText().contains(",")) {
			return "Column name cannot contain a comma.";
		}

		if (this.txtColumnsRWave.getText().contains(",")) {
			return "Column name cannot contain a comma.";
		}
		List<String> inputColPressure = recordStringDisplay(this.txtColumnsPressure.getText());
		List<String> inputColFlow = recordStringDisplay(this.txtColumnsFlow.getText());
		List<String> inputColECG = recordStringDisplay(this.txtColumnsECG.getText());
		List<String> inputColRWave = recordStringDisplay(this.txtColumnsRWave.getText());

		if (!Utils.disjoint(inputColPressure, inputColFlow, inputColECG, inputColRWave)) {
			return "Column names must be unique to each type";
		}

		if (!this.txtResampleRate.getText().isEmpty() && !NumberUtils.isCreatable(this.txtResampleRate.getText())) {
			return "Resample rate must be a number.";
		}

		if (this.txtPreWIAWindow.getText().isEmpty()) {
			return "Pre-WIA filter window value must be a number.";
		}

		if (this.txtPreWIAPoly.getText().isEmpty()) {
			return "Pre-WIA filter polynomial order must be a number.";
		}

		try {
			Savgol.generateSettings(txtPreWIAWindow.getText(), txtPreWIAPoly.getText());
		} catch (Exception e) {
			return e.getMessage();
		}

		return null;

	}

	/**
	 * Records the values from the GUI components into their corresponding instance
	 * variables.
	 */
	public void recordDisplayValues() {
		this.opFlowOffset = Integer.parseInt(this.txtOffset.getText());
		this.opAutoHeader = this.chAutoHeader.isSelected();
		this.opAutoName = this.chAutoName.isSelected();
		this.opAutoSaveDir = this.chAutoSaveDir.isSelected();
		this.opColPressure = recordStringDisplay(this.txtColumnsPressure.getText().trim());
		this.opColFlow = recordStringDisplay(this.txtColumnsFlow.getText().trim());
		this.opColECG = recordStringDisplay(this.txtColumnsECG.getText().trim());
		this.opColRWave = recordStringDisplay(this.txtColumnsRWave.getText().trim());
		this.opResampleRate = this.txtResampleRate.getText();
		this.opEnsembleType = (String) this.cbEnsembleType.getSelectedItem();
		this.opPreWIAFilterEnable = this.chPreWIAFilter.isSelected();
		this.opPreWIAFilterWindow = this.txtPreWIAWindow.getText();
		this.opPreWIAFilterPoly = this.txtPreWIAPoly.getText();

	}

	/**
	 * Reads configuration settings from the "config_WIA.properties" file and loads
	 * them into the instance variables. If the file does not exist, default values
	 * are retained.
	 * 
	 * @throws IOException if errors with I/O with the configuration file
	 */
	public void readProperties() throws IOException {

		if (!configFile.exists()) {
			try (InputStream defaultStream = getClass().getResourceAsStream(configFileDefaultPath)) {
				if (defaultStream == null) {
					throw new IOException("Default configuration file not found in resources.");
				}

				try (FileOutputStream out = new FileOutputStream(configFile)) {
					byte[] buffer = new byte[1024];
					int bytesRead;
					while ((bytesRead = defaultStream.read(buffer)) != -1) {
						out.write(buffer, 0, bytesRead);
					}
				}
			} 
		}

		Properties prop = new Properties();
		try (FileInputStream in = new FileInputStream(configFile)) {

			prop.load(in);

			chSaveSettingsFile.setSelected(true);

			String flowOffset = prop.getProperty(keyFlowOffset, "0");
			String autoHeader = prop.getProperty(keyAutoHeader, "true");
			String autoName = prop.getProperty(keyAutoName, "true");
			String autoSaveDir = prop.getProperty(keyAutoSaveDir, "false");
			String colPressure = prop.getProperty(keyPressureCols, "[]");
			String colFlow = prop.getProperty(keyFlowCols, "[]");
			String colECG = prop.getProperty(keyECGCols, "[]");
			String colRWave = prop.getProperty(keyRWaveCols, "[]");
			String resampleRate = prop.getProperty(keyBeatsResample, "0.001");
			String ensembleType = prop.getProperty(keyEnsembleType,
					CombowireGUI.EnsembleTypeMap.keySet().stream().findFirst().orElse("Trim"));
			String WIAFiltEnable = prop.getProperty(keyWIAFilt, "true");
			String WIAFiltWindow = prop.getProperty(keyWIAWindow, "51");
			String WIAFiltPoly = prop.getProperty(keyWIAPoly, "3");

			if (NumberUtils.isCreatable(flowOffset)) {
				this.opFlowOffset = Integer.parseInt(flowOffset);
			}

			if (autoName.toLowerCase().equals("true") || autoName.toLowerCase().equals("false")) {
				this.opAutoName = Boolean.valueOf(autoName);
			}

			if (autoHeader.toLowerCase().equals("true") || autoHeader.toLowerCase().equals("false")) {
				this.opAutoHeader = Boolean.valueOf(autoHeader);
			}

			if (autoSaveDir.toLowerCase().equals("true") || autoSaveDir.toLowerCase().equals("false")) {
				this.opAutoSaveDir = Boolean.valueOf(autoSaveDir);
			}

			for (String col : parseStringList(colPressure)) {
				this.opColPressure.add(col);
			}

			for (String col : parseStringList(colFlow)) {
				this.opColFlow.add(col);
			}

			for (String col : parseStringList(colECG)) {
				this.opColECG.add(col);
			}

			for (String col : parseStringList(colRWave)) {
				this.opColRWave.add(col);
			}

			if (NumberUtils.isCreatable(resampleRate)) {
				this.opResampleRate = resampleRate;
			}

			if (CombowireGUI.EnsembleTypeMap.containsKey(ensembleType)) {
				opEnsembleType = ensembleType;
			}

			if (WIAFiltEnable.toLowerCase().equals("true") || WIAFiltEnable.toLowerCase().equals("false")) {
				this.opPreWIAFilterEnable = Boolean.valueOf(WIAFiltEnable);
			}

			if (NumberUtils.isCreatable(WIAFiltWindow)) {
				this.opPreWIAFilterWindow = WIAFiltWindow;
			}

			if (NumberUtils.isCreatable(WIAFiltPoly)) {
				this.opPreWIAFilterPoly = WIAFiltPoly;
			}

			// non user settable
			this.opLastDir = prop.getProperty(keyLastDirectory, "");
			if (prop.getProperty(keySnapToR, "false").toLowerCase().equals("true")) {
				this.opSnapToR = true;
			} else {
				this.opSnapToR = false;
			}

			this.wiaSettings = new WIASaveSettingsChoices(prop);

		} catch (IOException e) {
			// should be found since we made sure it exists
			e.printStackTrace();
			return;
		}

	}

	/**
	 * Writes the current configuration settings from the instance variables to the
	 * config file.
	 * 
	 * @throws IOException if unable to write and save properties file to system
	 */
	public void writeProperties() throws IOException {
		Properties prop = new Properties();
		prop.setProperty(keyFlowOffset, String.valueOf(opFlowOffset));
		prop.setProperty(keyAutoHeader, Boolean.toString(opAutoHeader));
		prop.setProperty(keyAutoName, Boolean.toString(opAutoName));
		prop.setProperty(keyAutoSaveDir, Boolean.toString(opAutoSaveDir));
		prop.setProperty(keyPressureCols, listToSaveString(opColPressure));
		prop.setProperty(keyFlowCols, listToSaveString(opColFlow));
		prop.setProperty(keyECGCols, listToSaveString(opColECG));
		prop.setProperty(keyRWaveCols, listToSaveString(opColRWave));
		prop.setProperty(keyLastDirectory, opLastDir);
		prop.setProperty(keySnapToR, Boolean.toString(opSnapToR));
		prop.setProperty(keyBeatsResample, opResampleRate);
		prop.setProperty(keyEnsembleType, opEnsembleType);
		prop.setProperty(keyWIAFilt, Boolean.toString(opPreWIAFilterEnable));
		prop.setProperty(keyWIAWindow, opPreWIAFilterWindow);
		prop.setProperty(keyWIAPoly, opPreWIAFilterPoly);

		if (this.wiaSettings != null) {
			this.wiaSettings.writeProperties(prop);
		}

		try (FileOutputStream out = new FileOutputStream(configFile)) {

			prop.store(out, "Configuration properties");

		} // propagate exceptions to caller
	}

	/**
	 * Parses a string representation of a list (e.g., "[item1,item2]") into a
	 * {@code List<String>}.
	 *
	 * @param str The string to parse.
	 * @return A {@code List<String>} containing the parsed items.
	 */
	public List<String> parseStringList(String str) {
		List<String> list = new ArrayList<String>();
		try {
			String[] lines = str.substring(str.indexOf("[") + 1, str.lastIndexOf("]")).split(",");

			if (lines == null)
				return list;

			for (String line : lines) {
				list.add(line);
			}
		} catch (Exception e) {

		}
		return list;
	}

	/**
	 * Converts a multi-line string from a JTextArea into a list of non-blank
	 * strings.
	 *
	 * @param str The string from the display component, with lines separated by the
	 *            system's line separator.
	 * @return A {@code List<String>} where each element is a line from the input
	 *         string.
	 */
	public List<String> recordStringDisplay(String str) {
		List<String> list = new ArrayList<String>();
		String[] split = str.split(System.getProperty("line.separator"));
		for (String s : split) {
			if (!s.isBlank()) {
				list.add(s);
			}
		}
		return list;
	}

	/**
	 * Converts a list of strings into a comma-separated string enclosed in square
	 * brackets, suitable for saving in a properties file.
	 *
	 * @param list The list of strings to convert.
	 * @return A formatted string representation of the list.
	 */
	public String listToSaveString(List<String> list) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");

		boolean firstItem = true;
		for (String item : list) {
			if (item.contains(","))
				continue;

			if (firstItem)
				sb.append(item);
			else
				sb.append(",").append(item);

			firstItem = false;
		}

		sb.append("]");
		return sb.toString();
	}

	/**
	 * Converts a list of strings into a single string where each item is on a new
	 * line, for display in a JTextArea.
	 *
	 * @param list The list of strings to convert.
	 * @return A multi-line string representation of the list.
	 */
	public String listToDisplayString(List<String> list) {
		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;
		for (String s : list) {
			if (isFirst)
				sb.append(s);
			else
				sb.append(System.getProperty("line.separator")).append(s);

			isFirst = false;
		}
		return sb.toString();

	}

	/**
	 * Gets the path of the last directory accessed by the user.
	 *
	 * @return The last used directory path as a string.
	 */
	public String getLastDirectoryPath() {
		return this.opLastDir;
	}

	/**
	 * Sets and saves the path of the last accessed directory based on a given file.
	 *
	 * @param file The file or directory from which to derive the path.
	 * @throws IOException if unable to write and save properties file to system
	 */
	public void tryToSetLastDir(File file) throws IOException {
		if (!file.exists())
			return;

		if (file.isDirectory()) {
			opLastDir = file.getPath();
		} else {
			opLastDir = file.getParentFile().getPath();
		}
		writeProperties();
	}

	/**
	 * Gets the R-Wave synchronization setting.
	 *
	 * @return {@code true} if snap to R-Wave is enabled, {@code false} otherwise.
	 */
	public boolean getRWaveSync() {
		return this.opSnapToR;
	}

	/**
	 * Sets and saves the R-Wave synchronization preference.
	 *
	 * @param sync The new synchronization state.
	 * @throws IOException if unable to write and save properties file to system
	 * 
	 */
	public void tryToSetRWaveSync(boolean sync) throws IOException {
		this.opSnapToR = sync;
		writeProperties();
	}

	/**
	 * Gets the configured flow offset value.
	 *
	 * @return The flow offset as an integer.
	 */
	public int getFlowOffset() {
		return opFlowOffset;
	}

	/**
	 * Gets the status of the "Auto-detect headers" checkbox.
	 *
	 * @return {@code true} if auto-detect headers is selected, {@code false}
	 *         otherwise.
	 */
	public boolean getAutoHeader() {
		return chAutoHeader.isSelected();
	}

	/**
	 * Gets the status of the "Auto name files" checkbox.
	 *
	 * @return {@code true} if auto-name files is selected, {@code false} otherwise.
	 */
	public boolean getAutoName() {
		return chAutoName.isSelected();
	}

	/**
	 * Gets the status of the "Auto Save to directory" checkbox.
	 *
	 * @return {@code true} if auto-save is selected, {@code false} otherwise.
	 */
	public boolean getAutoSaveInDirectory() {
		return chAutoSaveDir.isSelected();
	}

	/**
	 * Gets the list of column names designated as pressure columns.
	 *
	 * @return A list of strings representing the pressure columns.
	 */
	public List<String> getColumnsPressure() {
		return this.opColPressure;
	}

	/**
	 * Gets the list of column names designated as flow columns.
	 *
	 * @return A list of strings representing the flow columns.
	 */
	public List<String> getColumnsFlow() {
		return this.opColFlow;
	}

	/**
	 * Gets the list of column names designated as ECG columns.
	 *
	 * @return A list of strings representing the ECG columns.
	 */
	public List<String> getColumnsECG() {
		return this.opColECG;
	}

	/**
	 * Gets the list of column names designated as R-Wave columns.
	 *
	 * @return A list of strings representing the R-Wave columns.
	 */
	public List<String> getColumnsRWave() {
		return this.opColRWave;
	}

	/**
	 * Gets the object containing choices for how to save WIA results.
	 *
	 * @return The {@code WIASaveSettingsChoices} object.
	 */
	public WIASaveSettingsChoices getSaveSettingsChoices() {
		return this.wiaSettings;
	}

	/**
	 * Gets the resample rate from the corresponding text field.
	 *
	 * @return The resample rate as a string.
	 */
	public String getResampleString() {
		return txtResampleRate.getText();
	}

	/**
	 * Checks if the pre-WIA Savitzky-Golay filter is enabled.
	 *
	 * @return {@code true} if the filter is enabled, {@code false} otherwise.
	 */
	public boolean isPreWIAFilterEnabled() {
		return opPreWIAFilterEnable;
	}

	/**
	 * Gets the window size for the pre-WIA Savitzky-Golay filter.
	 *
	 * @return The window size as a string.
	 */
	public String getPreWIAFilterWindowString() {
		return opPreWIAFilterWindow;
	}

	/**
	 * Gets the polynomial order for the pre-WIA Savitzky-Golay filter.
	 *
	 * @return The polynomial order as a string.
	 */
	public String getPreWIAFilterPolyString() {
		return opPreWIAFilterPoly;
	}

	/**
	 * Gets the selected ensemble type for waveform analysis.
	 *
	 * @return The ensemble type as a string.
	 */
	public String getEnsembleType() {
		return opEnsembleType;
	}

}
