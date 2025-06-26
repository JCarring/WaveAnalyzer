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

public class ComboFileConfigGUI extends JDialog {

	private static final long serialVersionUID = 5137417115882742282L;
	private static final File configFile = new File("./config_WIA.properties");
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
	 * Create the frame.
	 */
	public ComboFileConfigGUI() {
//		try {
//			String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
//			File file = new File(path);
//			
//			//File jarFile = new File(path);
//			//String jarDir = jarFile.getParentFile().getAbsolutePath();
//			Utils.showInfo("No error: " + file.getPath(), null);
//			Utils.showInfo("No error: " + file.getCanonicalPath(), null);
//			Utils.showInfo("No error: " + file.getAbsolutePath(), null);
//
//
//		} catch (Exception e) {
//			Utils.showInfo("Error: " + e, null);
//
//		}

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
					writeProperties();
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
		Utils.setFont(Utils.getTextFont(false), lblPreWIAWindow, lblPreWIAPolyOrder, chPreWIAFilter,
				txtPreWIAPoly, txtPreWIAWindow);

		
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
										GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
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
				.addGroup(gl_panel.createParallelGroup(Alignment.CENTER).addComponent(lblResample)
						.addComponent(txtResampleRate))
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

	public void open(Component component) {
		setLocationRelativeTo(component);
		setModal(true);
		setDisplayValues();
		setVisible(true);
	}

	public void close() {
		setVisible(false);
	}

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

	public void readProperties() {
		if (configFile.exists()) {
			Properties prop = new Properties();
			FileInputStream in;
			try {
				// String file =
				// getClass().getProtectionDomain().getCodeSource().getLocation().getPath() +
				// File.separator + configFile;
				// in = new FileInputStream(file);
				in = new FileInputStream(configFile);

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
	}

	public void writeProperties() {
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

		try {
			// String file =
			// getClass().getProtectionDomain().getCodeSource().getLocation().getPath() +
			// File.separator + configFile;
			// prop.store(new FileOutputStream(file), "Configuration WIA properties");
			prop.store(new FileOutputStream(configFile), "Configuration WIA properties");

		} catch (Exception e) {
			Utils.showError("Error " + e.getMessage(), this);
			e.printStackTrace();
		}

	}

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

	public String getLastDirectoryPath() {
		return this.opLastDir;
	}

	public void tryToSetLastDir(File file) {
		if (!file.exists())
			return;

		if (file.isDirectory()) {
			opLastDir = file.getPath();
		} else {
			opLastDir = file.getParentFile().getPath();
		}
		writeProperties();
	}

	public boolean getRWaveSync() {
		return this.opSnapToR;
	}

	public void tryToSetRWaveSync(boolean sync) {
		this.opSnapToR = sync;
		writeProperties();
	}

	public int getFlowOffset() {
		return opFlowOffset;
	}

	public boolean getAutoHeader() {
		return chAutoHeader.isSelected();
	}

	public boolean getAutoName() {
		return chAutoName.isSelected();
	}

	public boolean getAutoSaveInDirectory() {
		return chAutoSaveDir.isSelected();
	}

	public List<String> getColumnsPressure() {
		return this.opColPressure;
	}

	public List<String> getColumnsFlow() {
		return this.opColFlow;
	}

	public List<String> getColumnsECG() {
		return this.opColECG;
	}

	public List<String> getColumnsRWave() {
		return this.opColRWave;
	}

	public WIASaveSettingsChoices getSaveSettingsChoices() {
		return this.wiaSettings;
	}

	public String getResampleString() {
		return txtResampleRate.getText();
	}
	
	public boolean isPreWIAFilterEnabled() {
		return opPreWIAFilterEnable;
	}

	public String getPreWIAFilterWindowString() {
		return opPreWIAFilterWindow;
	}

	public String getPreWIAFilterPolyString() {
		return opPreWIAFilterPoly;
	}

	public String getEnsembleType() {
		return opEnsembleType;
	}

}
