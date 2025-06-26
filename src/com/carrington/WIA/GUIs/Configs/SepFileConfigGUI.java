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
import com.carrington.WIA.GUIs.SeparateWireGUI;
import com.carrington.WIA.Math.Savgol;

public class SepFileConfigGUI extends JDialog {

	private static final long serialVersionUID = 5137417115882742282L;
	private static final File configFile = new File("./config.properties");
	private static final String keyResample = "resample";
	private static final String keyAutoHeader = "auto_set_header";
	private static final String keyAutoName = "auto_set_name";
	private static final String keyAutoSaveDir = "auto_save_in_start_directory";
	private static final String keyColumnsExclude = "exclude_columns";
	private static final String keyColumnsAlign = "align_columns";
	private static final String keyLastDirectory = "last_dir";
	private static final String keyBeatsFilt = "pre_beat_selection_filter_enabled";
	private static final String keyBeatsWindow = "pre_beat_selection_filter_window";
	private static final String keyBeatsPoly = "pre_beat_selection_filter_polyorder";
	private static final String keyBeatsResample = "pre_beat_selection_filter_resample";
	private static final String keyEnsembleType = "ensemble_type";
	private static final String keyWIAFilt = "WIA_filter_enabled";
	private static final String keyWIAWindow = "WIA_filter_window";
	private static final String keyWIAPoly = "WIA_filter_polyorder";
	
	// initialize these to their respective default values
	private String opResample = "";
	private boolean opAutoHeader = true;
	private boolean opAutoName = true;
	private boolean opAutoSaveDir = false;
	private boolean opPreAlignFilterEnable = false;
	private String opPreAlignFilterWindow = "";
	private String opPreAlignFilterPoly = "";
	private String opPreAlignFilterResample = "";
	private String opEnsembleType = SeparateWireGUI.EnsembleTypeMap.keySet().stream()
            .findFirst().orElse("Trim");
	private boolean opPreWIAFilterEnable = false;
	private String opPreWIAFilterWindow = "";
	private String opPreWIAFilterPoly = "";

	private List<String> opColExclude = new ArrayList<String>();
	private List<String> opColAlign = new ArrayList<String>();
	
	private String opLastDir = ""; // not settable by the user


	private JPanel contentPane;
	private JTextField txtResample;
	private JCheckBox chAutoHeader;
	private JCheckBox chAutoName;
	private JCheckBox chAutoSaveDir;
	private JCheckBox chUseFileRWave;
	private JTextArea txtColumnsExclude;
	private JTextArea txtColumnsAlign;
	
	private JCheckBox chPreAlignFilter;
	private JCheckBox chPreWIAFilter;
	private JTextField txtPreSelectWindow;
	private JTextField txtPreWIAWindow;
	private JTextField txtPreSelectPoly;
	private JTextField txtPreWIAPoly;
	private JTextField txtPreSelectResamp;
	private JComboBox<String> cbEnsembleType;

	
	private JCheckBox chSaveSettingsFile;
	
	private WeakReference<SepFileConfigGUI> ref = new WeakReference<SepFileConfigGUI>(this);
	
	private WIASaveSettingsChoices wiaSettings;


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SepFileConfigGUI frame = new SepFileConfigGUI();
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
	public SepFileConfigGUI() {
		
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
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addComponent(lblTitleSettings)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
						.addComponent(pnlMain, GroupLayout.DEFAULT_SIZE, 784, Short.MAX_VALUE)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addContainerGap()
							.addComponent(chSaveSettingsFile)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(btnCancel)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(btnAccept)
							.addContainerGap()))
					.addPreferredGap(ComponentPlacement.UNRELATED))
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addComponent(lblTitleSettings)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(pnlMain, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(btnAccept)
						.addComponent(btnCancel)
						.addComponent(chSaveSettingsFile))
					.addContainerGap())
		);
		
		JLabel lblGeneral = new JLabel("General");
		lblGeneral.setFont(subtitleFont);
		
		JLabel lblRunConfig = new JLabel("Run Configuration");
		lblRunConfig.setFont(subtitleFont);
		
		chAutoHeader = new JCheckBox("Auto-detect headers");
		chAutoName = new JCheckBox("Auto name files");
		chAutoSaveDir = new JCheckBox("Auto Save to directory");
		chUseFileRWave = new JCheckBox("Use Input File's R Waves");

		chAutoHeader.setFont(normalPlain);
		chAutoName.setFont(normalPlain);
		chAutoSaveDir.setFont(normalPlain);
		chUseFileRWave.setFont(subtitleFont);

		JLabel lblResample = new JLabel("Resample rate:");
		lblResample.setFont(normalBold);
		
		txtResample = new JTextField();
		txtResample.setColumns(10);
		txtResample.setFont(normalPlain);
		
		JLabel lblExclude = new JLabel("Exclude columns");
		lblExclude.setFont(normalBold);
		
		JScrollPane scrExclude = new JScrollPane();
		
		JScrollPane scrAlign = new JScrollPane();
		
		JLabel lblPreAlignFilter = new JLabel("Pre-Align SG Filter:");
		chPreAlignFilter = new JCheckBox("Enabled");

		JLabel lblPreAlignWindow = new JLabel("Window:");
		txtPreSelectWindow = new JTextField("");
		JLabel lblPreAlignPolyOrder = new JLabel("Polynomial order:");
		txtPreSelectPoly = new JTextField("");
		JLabel lblPreAlignResampRate = new JLabel("Resample rate:");
		txtPreSelectResamp = new JTextField("");
		
		JLabel lblEnsembletype = new JLabel("Ensemble type:");
		cbEnsembleType = new JComboBox<String>();
		for (Entry<String, Integer> entry : SeparateWireGUI.EnsembleTypeMap.entrySet()) {
			cbEnsembleType.addItem(entry.getKey());
		}
		FontMetrics metrics = cbEnsembleType.getFontMetrics(cbEnsembleType.getFont());
        int cbEnsembleWidth = 0;

        for (int i = 0; i < cbEnsembleType.getItemCount(); i++) {
            int currWidth = metrics.stringWidth(cbEnsembleType.getItemAt(i));
            cbEnsembleWidth = Math.max(currWidth, cbEnsembleWidth);
        }
        cbEnsembleWidth*=3; // double the width for padding


		JLabel lblPreWIAFilter = new JLabel("Pre-WIA SG Filter:");
		chPreWIAFilter = new JCheckBox("Enabled");
		JLabel lblPreWIAWindow = new JLabel("Window:");
		txtPreWIAWindow = new JTextField("");
		JLabel lblPreWIAPolyOrder = new JLabel("Polynomial order:");
		txtPreWIAPoly = new JTextField("");

		int width = Utils.getFontParams(Utils.getTextFont(false), "0.0000001")[1];
		
		
		JLabel lblColumnAlign = new JLabel("Columns for Alignment");
		lblColumnAlign.setFont(normalBold);
		GroupLayout gl_panel = new GroupLayout(pnlMain);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addComponent(lblGeneral)
						.addComponent(lblRunConfig)
						.addGroup(gl_panel.createSequentialGroup()
							.addGap(10)
							.addComponent(chAutoHeader)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(chAutoName)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(chAutoSaveDir))
						.addGroup(gl_panel.createSequentialGroup()
							.addGap(10)
							.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_panel.createSequentialGroup()
									.addComponent(lblResample)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(txtResample, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
								.addGroup(gl_panel.createSequentialGroup()
									.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
										.addComponent(scrExclude, GroupLayout.PREFERRED_SIZE, size * 15, GroupLayout.PREFERRED_SIZE)
										.addComponent(lblExclude))
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
										.addComponent(lblColumnAlign)
										.addComponent(scrAlign, GroupLayout.PREFERRED_SIZE, size * 15, GroupLayout.PREFERRED_SIZE)))))
						.addGroup(gl_panel.createSequentialGroup()
								.addGap(10)
								.addComponent(lblPreAlignFilter)
								.addPreferredGap(ComponentPlacement.UNRELATED)
								.addComponent(chPreAlignFilter)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(lblPreAlignWindow)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(txtPreSelectWindow, GroupLayout.PREFERRED_SIZE, width, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(lblPreAlignPolyOrder)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(txtPreSelectPoly, GroupLayout.PREFERRED_SIZE, width, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(lblPreAlignResampRate)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(txtPreSelectResamp, GroupLayout.PREFERRED_SIZE, width, GroupLayout.PREFERRED_SIZE)
								)
						.addGroup(gl_panel.createSequentialGroup()
								.addGap(10)
								.addComponent(lblEnsembletype)
								.addPreferredGap(ComponentPlacement.UNRELATED)
								.addComponent(cbEnsembleType, GroupLayout.DEFAULT_SIZE, cbEnsembleWidth, cbEnsembleWidth)
								)
						.addGroup(gl_panel.createSequentialGroup()
								.addGap(10)
								.addComponent(lblPreWIAFilter)
								.addPreferredGap(ComponentPlacement.UNRELATED)
								.addComponent(chPreWIAFilter)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(lblPreWIAWindow)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(txtPreWIAWindow, GroupLayout.PREFERRED_SIZE, width, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(lblPreWIAPolyOrder)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(txtPreWIAPoly, GroupLayout.PREFERRED_SIZE, width, GroupLayout.PREFERRED_SIZE)

								))
					.addContainerGap(278, Short.MAX_VALUE))
		);
		gl_panel.setVerticalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addContainerGap()
					.addComponent(lblGeneral)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
						.addComponent(chAutoHeader)
						.addComponent(chAutoName)
						.addComponent(chAutoSaveDir))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(lblRunConfig)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblResample)
						.addComponent(txtResample, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
						.addGroup(gl_panel.createSequentialGroup()
								.addComponent(lblExclude)
								.addComponent(scrExclude, GroupLayout.PREFERRED_SIZE, size * 10, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_panel.createSequentialGroup()
								.addComponent(lblColumnAlign)
								.addComponent(scrAlign, GroupLayout.PREFERRED_SIZE, size * 10, GroupLayout.PREFERRED_SIZE)))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel.createParallelGroup(Alignment.CENTER)
						.addComponent(lblPreAlignFilter)
						.addComponent(chPreAlignFilter)
						.addComponent(lblPreAlignWindow)
						.addComponent(lblPreAlignPolyOrder)
						.addComponent(lblPreAlignResampRate)
						.addComponent(txtPreSelectWindow)
						.addComponent(txtPreSelectPoly)
						.addComponent(txtPreSelectResamp))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel.createParallelGroup(Alignment.CENTER)
						.addComponent(lblEnsembletype)
						.addComponent(cbEnsembleType))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel.createParallelGroup(Alignment.CENTER)
						.addComponent(lblPreWIAFilter)
						.addComponent(chPreWIAFilter)
						.addComponent(lblPreWIAWindow)
						.addComponent(lblPreWIAPolyOrder)
						.addComponent(txtPreWIAWindow)
						.addComponent(txtPreWIAPoly))
					.addContainerGap(55, Short.MAX_VALUE))
		);
		
		txtColumnsAlign = new JTextArea();
		scrAlign.setViewportView(txtColumnsAlign);
		
		Utils.setFont(Utils.getTextFont(true), lblPreAlignFilter, lblPreWIAFilter);
		Utils.setFont(Utils.getTextFont(false), lblPreAlignWindow, lblPreAlignPolyOrder, lblPreAlignResampRate, lblEnsembletype,
				lblPreWIAWindow, lblPreWIAPolyOrder, txtPreSelectWindow, txtPreSelectPoly, txtPreSelectResamp, txtPreWIAWindow,
				txtPreWIAPoly, chPreAlignFilter, cbEnsembleType, chPreWIAFilter);

		txtColumnsExclude = new JTextArea();
		scrExclude.setViewportView(txtColumnsExclude);
		pnlMain.setLayout(gl_panel);
		contentPane.setLayout(gl_contentPane);
		setBounds(100, 100, this.getPreferredSize().width, this.getPreferredSize().height);
		setResizable(false);
		setLocationRelativeTo(null);
		
		readProperties();
		setDisplayValues();

	}
	
	public void open(Component parent) {
		setLocationRelativeTo(parent);
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
		this.txtResample.setText(this.opResample);
		this.txtColumnsExclude.setText(listToDisplayString(this.opColExclude));
		this.txtColumnsAlign.setText(listToDisplayString(this.opColAlign));
		this.chPreAlignFilter.setSelected(this.opPreAlignFilterEnable);
		this.txtPreSelectWindow.setText(this.opPreAlignFilterWindow);
		this.txtPreSelectPoly.setText(this.opPreAlignFilterPoly);
		this.txtPreSelectResamp.setText(this.opPreAlignFilterResample);
		this.chPreWIAFilter.setSelected(this.opPreWIAFilterEnable);
		this.txtPreWIAWindow.setText(this.opPreWIAFilterWindow);
		this.txtPreWIAPoly.setText(this.opPreWIAFilterPoly);
		this.cbEnsembleType.setSelectedItem(this.opEnsembleType);
	}
	
	public String validateDisplayValues() {
		if (!this.txtResample.getText().isEmpty()) {
			if (!NumberUtils.isCreatable(this.txtResample.getText())) {
				return "Resample value must be a number.";
			}
		}

		
		if (this.txtColumnsExclude.getText().contains(",")) {
			return "Column name cannot contain a comma.";
		}
		
		if (this.txtColumnsAlign.getText().contains(",")) {
			return "Column name cannot contain a comma.";
		}
		
		String[] inputColExclude = this.txtColumnsExclude.getText().split(System.getProperty("line.separator"));
		String[] inputColAlign = this.txtColumnsAlign.getText().split(System.getProperty("line.separator"));
		
		for (String s1 : inputColExclude) {
			for (String s2 : inputColAlign) {
				if (s1.equalsIgnoreCase(s2) && !s1.isBlank() && !s2.isBlank()) {
					return "Cannot exclude column " + s1 + " AND align based on it";
				}
			}
		}

		
		if (this.txtPreSelectWindow.getText().isEmpty()) {
				return "Pre-beat selection filter window value must be a number.";
		}
		
		if (this.txtPreSelectPoly.getText().isEmpty()) {
				return "Pre-beat selection filter polynomial order must be a number.";
		}
		
		if (!this.txtPreSelectResamp.getText().isEmpty() && !NumberUtils.isCreatable(this.txtPreSelectResamp.getText())) {
			return "Resample rate must be a number.";
		}
		
		if (this.txtPreWIAWindow.getText().isEmpty()) {
				return "Pre-WIA filter window value must be a number.";
		}
		
		if (this.txtPreWIAPoly.getText().isEmpty()) {
				return "Pre-WIA filter polynomial order must be a number.";
		}
		
		try {
			Savgol.generateSettings(txtPreSelectWindow.getText(), txtPreSelectPoly.getText());
		} catch (Exception e) {
			return e.getMessage();
		}
		
		try {
			Savgol.generateSettings(txtPreWIAWindow.getText(), txtPreWIAPoly.getText());
		} catch (Exception e) {
			return e.getMessage();
		}
		
		
		return null;
		
	}
	
	
	public void recordDisplayValues() {
		this.opResample = txtResample.getText();

		this.opAutoHeader = this.chAutoHeader.isSelected();
		this.opAutoName = this.chAutoName.isSelected();
		this.opAutoSaveDir = this.chAutoSaveDir.isSelected();
		this.opColAlign = recordStringDisplay(this.txtColumnsAlign.getText());
		this.opColExclude = recordStringDisplay(this.txtColumnsExclude.getText());
		
		this.opPreAlignFilterEnable = this.chPreAlignFilter.isSelected();
		this.opPreAlignFilterWindow = this.txtPreSelectWindow.getText();
		this.opPreAlignFilterPoly = this.txtPreSelectPoly.getText();
		this.opPreAlignFilterResample = this.txtPreSelectResamp.getText();
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
				in = new FileInputStream(configFile);
				prop.load(in);
				
				chSaveSettingsFile.setSelected(true);
				
				String resample = prop.getProperty(keyResample, "");
				String autoHeader = prop.getProperty(keyAutoHeader, "true");
				String autoName = prop.getProperty(keyAutoName, "true");
				String autoSaveDir = prop.getProperty(keyAutoSaveDir, "false");
				String colExclude = prop.getProperty(keyColumnsExclude, "[]");
				String colAlign = prop.getProperty(keyColumnsAlign, "[]");
				String beatsFiltEnable = prop.getProperty(keyBeatsFilt, "false");
				String beatsFiltWindow = prop.getProperty(keyBeatsWindow, "51");
				String beatsFiltPoly = prop.getProperty(keyBeatsPoly, "3");
				String beatsFiltResamp = prop.getProperty(keyBeatsResample, "0.001");
				String ensembleType = prop.getProperty(keyEnsembleType, SeparateWireGUI.EnsembleTypeMap.keySet().stream()
		                .findFirst().orElse("Trim"));
				String WIAFiltEnable = prop.getProperty(keyWIAFilt, "true");
				String WIAFiltWindow = prop.getProperty(keyWIAWindow, "51");
				String WIAFiltPoly = prop.getProperty(keyWIAPoly, "3");
				
				if (NumberUtils.isCreatable(resample)) {
					this.opResample = resample;
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
				
				if (beatsFiltEnable.toLowerCase().equals("true") || beatsFiltEnable.toLowerCase().equals("false")) {
					this.opPreAlignFilterEnable = Boolean.valueOf(beatsFiltEnable);
				}
				
				if (NumberUtils.isCreatable(beatsFiltWindow)) {
					this.opPreAlignFilterWindow = beatsFiltWindow;
				}
				
				if (NumberUtils.isCreatable(beatsFiltPoly)) {
					this.opPreAlignFilterPoly = beatsFiltPoly;
				}
				
				if (NumberUtils.isCreatable(beatsFiltResamp)) {
					this.opPreAlignFilterResample = beatsFiltResamp;
				}
				
				if (SeparateWireGUI.EnsembleTypeMap.containsKey(ensembleType)) {
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
				

				for (String col : parseStringList(colExclude)) {
					this.opColExclude.add(col);
				}
				
				for (String col : parseStringList(colAlign)) {
					this.opColAlign.add(col);
				}
				
				this.opLastDir = prop.getProperty(keyLastDirectory, "");
				
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
		prop.setProperty(keyResample, opResample);
		
		prop.setProperty(keyAutoHeader, Boolean.toString(opAutoHeader));
		prop.setProperty(keyAutoName, Boolean.toString(opAutoName));
		prop.setProperty(keyAutoSaveDir, Boolean.toString(opAutoSaveDir));
		prop.setProperty(keyColumnsExclude, listToSaveString(opColExclude));
		prop.setProperty(keyColumnsAlign, listToSaveString(opColAlign));
		prop.setProperty(keyLastDirectory, opLastDir);
		prop.setProperty(keyBeatsFilt, Boolean.toString(opPreAlignFilterEnable));
		prop.setProperty(keyBeatsWindow, opPreAlignFilterWindow);
		prop.setProperty(keyBeatsPoly, opPreAlignFilterPoly);
		prop.setProperty(keyBeatsResample, opPreAlignFilterResample);
		prop.setProperty(keyEnsembleType, opEnsembleType);
		prop.setProperty(keyWIAFilt, Boolean.toString(opPreWIAFilterEnable));
		prop.setProperty(keyWIAWindow, opPreWIAFilterWindow);
		prop.setProperty(keyWIAPoly, opPreWIAFilterPoly);
		
		if (this.wiaSettings != null) {
			this.wiaSettings.writeProperties(prop);
		}


		try {
			prop.store(new FileOutputStream(configFile), "Configuration properties");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public List<String> parseStringList(String str) {
		List<String> list = new ArrayList<String>();
		try {
			String[] lines = str.substring(str.indexOf("[") + 1, str.lastIndexOf("]")).split(",");
			
			if (lines == null) return list;
			
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
	
	public String getResampleString() {
		return txtResample.getText();
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
	
	public List<String> getColumnsExclude() {
		return this.opColExclude;
	}
	
	public List<String> getColumnsAlign() {
		return this.opColAlign;
	}
	
	public WIASaveSettingsChoices getSaveSettingsChoices() {
		return this.wiaSettings;
	}

	public boolean isPreBeatSelectionFilterEnabled() {
		return opPreAlignFilterEnable;
	}

	public String getPreBeatSelectionFilterWindowString() {
		return opPreAlignFilterWindow;
	}

	public String getPreBeatSelectionFilterPolyString() {
		return opPreAlignFilterPoly;
	}

	public String getPreBeatSelectionResampleRateString() {
		return opPreAlignFilterResample;
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
