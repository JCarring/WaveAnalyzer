package com.carrington.WIA.GUIs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.ref.WeakReference;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;

import com.carrington.WIA.Utils;
import com.carrington.WIA.DataStructures.WIAData;
import com.carrington.WIA.GUIs.Components.JCButton;
import com.carrington.WIA.GUIs.Components.JCLabel;
import com.carrington.WIA.GUIs.Configs.ComboFileConfigGUI;
import com.carrington.WIA.IO.Saver;

public class WIAModifierGUI extends JFrame implements WIACaller {

	private static final long serialVersionUID = -989119673787391595L;
	private JPanel contentPane;
	private BackListener backListener = null;
	private volatile JFrame frameToGoBackTo = null;
	private WeakReference<WIAModifierGUI> ref = new WeakReference<WIAModifierGUI>(this);

	private JPanel pnlFileSelect;
	private JPanel pnlFileDetails;
	private JPanel middlePanel;
	private JTextField txtFileName;

	private JScrollPane scrFileName;
	private JCButton btnRunWIA;

	private final ComboFileConfigGUI config = new ComboFileConfigGUI();
	private volatile WIAData wiaData = null;
	private volatile File currFile = null;
	private JCButton btnSaveMetrics;

	public WIAModifierGUI(JFrame frameToGoBackTo, BackListener backListener) {
		System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");

		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.backListener = backListener;
		this.frameToGoBackTo = frameToGoBackTo;

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setTitle("Modify WIA");
		contentPane = new JPanel();
		setContentPane(contentPane);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});

		// Top
		JPanel topPanel = new JPanel();
		topPanel.setBackground(new Color(192, 192, 192));
		topPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		JLabel lblInstructions = new JLabel("Modify WIA (.wia file) below");
		topPanel.add(lblInstructions);

		// Middle
		initPnlMain();

		// Bottom
		JPanel bottomPanel = new JPanel();
		bottomPanel.setBackground(new Color(192, 192, 192));
		bottomPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));

		JCButton btnBack = new JCButton("Go Back", JCButton.BUTTON_GO_BACK);
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
		bottomPanel.add(btnReset);
		bottomPanel.add(btnBack);
		bottomPanel.add(btnQuit);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width / 2;
		int size = Utils.getTextFont(false).getSize();

		GroupLayout mainContentPaneLayout = new GroupLayout(contentPane);
		mainContentPaneLayout.setHorizontalGroup(mainContentPaneLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(mainContentPaneLayout.createSequentialGroup().addContainerGap()
						.addGroup(mainContentPaneLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(topPanel, width, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
								.addComponent(middlePanel, width, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
								.addComponent(bottomPanel, width, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
						.addContainerGap()));
		mainContentPaneLayout.setVerticalGroup(mainContentPaneLayout.createSequentialGroup().addContainerGap()
				.addComponent(topPanel, GroupLayout.PREFERRED_SIZE, (int) (size * 2.5), GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(middlePanel, GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(bottomPanel, GroupLayout.PREFERRED_SIZE, (int) (size * 3.0), GroupLayout.PREFERRED_SIZE)
				.addContainerGap());

		contentPane.setLayout(mainContentPaneLayout);
		Utils.setFont(Utils.getSubTitleFont(), lblInstructions);

		pack();
		setLocationRelativeTo(null);
		setMinimumSize(getPreferredSize());
	}

	private synchronized void initPnlMain() {
		middlePanel = new JPanel();
		middlePanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		pnlFileDetails = new JPanel();
		pnlFileDetails.setBorder(new LineBorder(new Color(0, 0, 0)));
		initMiddleFileDetails();

		pnlFileSelect = new JPanel();
		pnlFileSelect.setBorder(new LineBorder(new Color(0, 0, 0)));
		initMiddleFileSelect();

		GroupLayout gl_pnlMain = new GroupLayout(middlePanel);
		gl_pnlMain.setVerticalGroup(gl_pnlMain.createSequentialGroup().addContainerGap()
				.addComponent(pnlFileSelect, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.UNRELATED)
				.addComponent(pnlFileDetails, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addContainerGap());
		gl_pnlMain.setHorizontalGroup(gl_pnlMain.createSequentialGroup().addContainerGap()
				.addGroup(gl_pnlMain.createParallelGroup(Alignment.CENTER)
						.addComponent(pnlFileSelect, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE)
						.addComponent(pnlFileDetails, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE))
				.addContainerGap());

		updateWIAFileDisplay();
		middlePanel.setLayout(gl_pnlMain);
	}

	private void initMiddleFileSelect() {

		JCLabel titleSelect = new JCLabel("Input", JCLabel.LABEL_SUBTITLE);
		JCButton btnSelect = new JCButton("Browse...", JCButton.BUTTON_STANDARD);
		btnSelect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				runFileOpen();
			}

		});
		btnSelect.setMnemonic('B');
		btnSelect.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.META_DOWN_MASK), "buttonBrowse");

		// Map the keystroke to an action
		btnSelect.getActionMap().put("buttonBrowse", new AbstractAction() {
			private static final long serialVersionUID = 1620869276363160741L;
			@Override
			public void actionPerformed(ActionEvent e) {
				btnSelect.doClick();
			}
		});
		txtFileName = new JTextField("");
		txtFileName.setBackground(Color.WHITE);
		txtFileName.setEditable(false);
		txtFileName.setColumns(10);
		txtFileName.setMargin(new Insets(2, 2, 2, 2));

		GroupLayout gl_pnlMiddleFileSelect = new GroupLayout(pnlFileSelect);
		scrFileName = new JScrollPane();
		scrFileName.setViewportView(txtFileName);
		scrFileName.getHorizontalScrollBar().setValue(scrFileName.getHorizontalScrollBar().getMaximum());
		gl_pnlMiddleFileSelect
				.setVerticalGroup(gl_pnlMiddleFileSelect.createSequentialGroup().addGap(3).addComponent(titleSelect)
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(btnSelect)
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(scrFileName,
								GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						.addContainerGap());
		gl_pnlMiddleFileSelect.setHorizontalGroup(gl_pnlMiddleFileSelect.createParallelGroup()
				.addGroup(gl_pnlMiddleFileSelect.createSequentialGroup().addGap(3).addComponent(titleSelect))
				.addGroup(gl_pnlMiddleFileSelect.createSequentialGroup().addContainerGap()
						.addGroup(gl_pnlMiddleFileSelect.createParallelGroup().addComponent(btnSelect).addComponent(
								scrFileName, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
						.addContainerGap()));

		pnlFileSelect.setLayout(gl_pnlMiddleFileSelect);
	}

	private void initMiddleFileDetails() {

		JCLabel titleDetails = new JCLabel("Details", JCLabel.LABEL_SUBTITLE);
		btnRunWIA = new JCButton("Run Wave Analysis");
		btnRunWIA.setEnabled(false);
		btnRunWIA.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				runWaveSelection();
			}

		});
		btnRunWIA.setMnemonic('R');
		btnRunWIA.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.META_DOWN_MASK), "buttonRunWIA");

		// Map the keystroke to an action
		btnRunWIA.getActionMap().put("buttonRunWIA", new AbstractAction() {
			private static final long serialVersionUID = -7785103215338161128L;
			@Override
			public void actionPerformed(ActionEvent e) {
				btnRunWIA.doClick();
			}
		});

		JSeparator sepVert = new JSeparator(JSeparator.VERTICAL);

		btnSaveMetrics = new JCButton("Save Metrics", JCButton.BUTTON_STANDARD);
		btnSaveMetrics.setEnabled(false);
		btnSaveMetrics.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String selName = wiaData.getSelectionName();
				String[][] data = wiaData.toCSV(selName);

				File fileToSave = getPrimaryDataWIASave(SeparateWireGUI.pathNameWIACSV, selName, true);
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
		btnSaveMetrics.setMnemonic('S');
		btnSaveMetrics.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.META_DOWN_MASK), "buttonSaveMetrics");

		// Map the keystroke to an action
		btnSaveMetrics.getActionMap().put("buttonSaveMetrics", new AbstractAction() {
			private static final long serialVersionUID = -2945487034037281330L;
			@Override
			public void actionPerformed(ActionEvent e) {
				btnSaveMetrics.doClick();
			}
		});

		GroupLayout gl_pnlMiddleFileDetails = new GroupLayout(pnlFileDetails);

		JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
		sep.setForeground(Color.GRAY);

		gl_pnlMiddleFileDetails.setVerticalGroup(gl_pnlMiddleFileDetails.createSequentialGroup().addGap(3)
				.addComponent(titleDetails).addPreferredGap(ComponentPlacement.UNRELATED)
				.addGroup(gl_pnlMiddleFileDetails.createParallelGroup(Alignment.CENTER).addComponent(btnRunWIA)
						.addComponent(btnSaveMetrics).addComponent(sepVert))
				.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(sep)
				.addPreferredGap(ComponentPlacement.UNRELATED).addContainerGap());
		gl_pnlMiddleFileDetails.setHorizontalGroup(gl_pnlMiddleFileDetails.createParallelGroup()
				.addGroup(gl_pnlMiddleFileDetails.createSequentialGroup().addGap(3).addComponent(titleDetails))
				.addGroup(gl_pnlMiddleFileDetails.createSequentialGroup().addContainerGap()
						.addGroup(gl_pnlMiddleFileDetails.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_pnlMiddleFileDetails.createSequentialGroup().addComponent(btnRunWIA)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(sepVert, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnSaveMetrics,
												GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE))
								.addComponent(sep, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE))
						.addContainerGap()));

		pnlFileDetails.setLayout(gl_pnlMiddleFileDetails);
	}

	private void runFileOpen() {

		reset(false);
		File file = Utils.promptUserForFile("Get WIA file (.wia)", config.getLastDirectoryPath(),
				new String[] { ".wia" });
		if (file == null)
			return;
		WIAData data = null;
		try {
			data = WIAData.deserialize(file);
			data.retryCalculations();
		} catch (Exception e) {
			Utils.showError(
					"Could not open file " + file.getName() + " due to an error. System error msg: " + e.getMessage(),
					this);
			return;
		}
		this.wiaData = data;
		this.currFile = file;

		updateWIAFileDisplay();
		config.tryToSetLastDir(file);
		txtFileName.setText(file.getPath());
		scrFileName.getHorizontalScrollBar().setValue(scrFileName.getHorizontalScrollBar().getMaximum());

	}

	private void runWaveSelection() {
		WavePickerGUI wavepickerGUI = new WavePickerGUI(wiaData.getSelectionName(), wiaData, config.getSaveSettingsChoices(), ref.get(), this);
		wavepickerGUI.display();
		// Hangs

		if (config.getSaveSettingsChoices().hasChanged()) {
			config.writeProperties();
			config.getSaveSettingsChoices().setChanged(false);
		}
		
		if (wavepickerGUI.getStatus() == WavePickerGUI.CANCELLED) {
			return;
		}
		
		if (wavepickerGUI.serializeWIAData()) {
			try {

				File fileToSave = getPrimaryDataWIASave(CombowireGUI.pathNameWIASerialize, wiaData.getSelectionName(),
						true);
				if (fileToSave != null) {
					WIAData.serialize(wiaData, fileToSave);
				}

			} catch (Exception ex) {
				Utils.showError(
						"Unable to save the current WIA data state. This may be due to lack of permissions to save in the current "
								+ "directory. You will not be able to re-edit wave selections at a later point. System error msg: "
								+ ex.getMessage(),
						ref.get());
			}
		}

		wiaData.calculateWavePeaksAndSum();
		wiaData.calculateResistance();
	}

	public void updateWIAFileDisplay() {
		if (wiaData == null) {
			btnRunWIA.setEnabled(false);
			btnSaveMetrics.setEnabled(false);
		} else {
			btnRunWIA.setEnabled(true);
			btnSaveMetrics.setEnabled(true);
		}
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

	public void reset(boolean warn) {
		if (warn) {
			if (!Utils.confirmAction("Confirm Reset", "Are you sure you want to reset to next files?", this)) {
				return;

			}
		}

		wiaData = null;
		txtFileName.setText("");
		currFile = null;
		updateWIAFileDisplay();
		btnSaveMetrics.setIcon(null);
		// TODO
	}

	public File getPrimaryDataWIAFolder() {
		return new File(currFile.getParent());
	}

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

	@Override
	public File getWIAImageFileSVG() {
		return getPrimaryDataWIASave(CombowireGUI.pathNameWIASVG, wiaData.getSelectionName(), true);
	}

	@Override
	public File getWIAImageFolderTIFF() {
		return getPrimaryDataWIAFolder();
	}

	@Override
	public File getWIAWaveSelectionsFileSVG() {
		return getPrimaryDataWIASave(CombowireGUI.pathNameWaveSelectionsSVG, wiaData.getSelectionName(), true);
	}

}
