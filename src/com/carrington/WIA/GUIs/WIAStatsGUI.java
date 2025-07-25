package com.carrington.WIA.GUIs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Set;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.carrington.WIA.Utils;
import com.carrington.WIA.DataStructures.WIAData;
import com.carrington.WIA.GUIs.Components.JCButton;
import com.carrington.WIA.GUIs.Components.JCLabel;
import com.carrington.WIA.GUIs.Components.StandardWaveGroupTable;
import com.carrington.WIA.GUIs.Components.StandardWaveGroupTable.StandardWaveGroupTableListener;
import com.carrington.WIA.GUIs.Components.StandardWaveTable;
import com.carrington.WIA.GUIs.Components.StandardWaveTable.StandardWaveTableListener;
import com.carrington.WIA.GUIs.Components.WIAFileSelectionTable;
import com.carrington.WIA.GUIs.Components.WIAFileSelectionTable.WIATableListener;
import com.carrington.WIA.GUIs.Components.WIATxNameTable;
import com.carrington.WIA.GUIs.Configs.ComboFileConfigGUI;
import com.carrington.WIA.IO.Saver;
import com.carrington.WIA.IO.WIAStats;
import com.carrington.WIA.IO.WIAStats.StandardWave;
import com.carrington.WIA.IO.WIAStats.StandardWaveGrouping;
import com.carrington.WIA.stats.StatisticalException;

/**
 * A graphical user interface for performing statistical analysis on a
 * collection of Wave Intensity Analysis (.wia) files. It allows users to load a
 * folder of WIA files, manage wave definitions, group waves, and run and save
 * statistical comparisons.
 */
public class WIAStatsGUI extends JFrame
		implements StandardWaveTableListener, StandardWaveGroupTableListener, WIATableListener {

	private static final long serialVersionUID = 9222582868347296174L;

	// GUI FIELDS
	private JPanel contentPane;

	private BackListener backListener = null;
	private JFrame frameToGoBackTo = null;

	private WeakReference<WIAStatsGUI> ref = new WeakReference<WIAStatsGUI>(this);
	private JPanel pnlInput;
	private JPanel pnlOutput;
	private JPanel pnlBottom;
	private JPanel pnlMiddle;
	private JPanel pnlTop;
	private JCButton btnBrowse;

	private WIAFileSelectionTable tableWIASelections;
	private StandardWaveTable tableStandardWave;
	private StandardWaveGroupTable tableStandardWaveGroups;
	private WIATxNameTable tableWIATxNames;
	private volatile JCButton btnRunStats;
	private volatile JCButton btnSaveStats;
	private volatile JCButton btnSaveCompiledData;
	private JCButton btnGroup;

	private static final int STATE_INIT = 0;
	private static final int STATE_LOADED = 1;
	private static final int STATE_STATS_RUN = 2;

	// Configuration (i.e. used for determining default file search path
	private final ComboFileConfigGUI config = new ComboFileConfigGUI();
	// Object with all of the statistics stored
	private WIAStats wiastat;

	/**
	 * Constructs the WIA Statistics GUI.
	 *
	 * @param frameToGoBackTo The parent frame to which this GUI will return.
	 * @param closeListener   The listener to be notified when the "back" action is
	 *                        performed.
	 */
	public WIAStatsGUI(JFrame frameToGoBackTo, BackListener closeListener) {
		this();
		this.backListener = closeListener;
		this.frameToGoBackTo = frameToGoBackTo;
	}

	/**
	 * Create the frame.
	 */
	public WIAStatsGUI() {
		System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {

				quit();
			}
		});
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);

		_initPnlTop();
		_initPnlMiddle();
		_initPnlBottom();

		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(gl_contentPane.createSequentialGroup()
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addComponent(pnlTop, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(pnlMiddle, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE)
						.addComponent(pnlBottom, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE))
				.addPreferredGap(ComponentPlacement.RELATED));
		gl_contentPane.setVerticalGroup(gl_contentPane.createSequentialGroup()
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(pnlTop, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(pnlMiddle, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addPreferredGap(ComponentPlacement.RELATED).addComponent(pnlBottom, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED));

		contentPane.setLayout(gl_contentPane);

		setLocationRelativeTo(null);

		Utils.unfocusAll(contentPane);
		Utils.unfocusAll(this);

		setPanelState(STATE_INIT);
	}

	/**
	 * Makes this GUI visible and hides the parent frame it was called from.
	 */
	public synchronized void navigateInto() {
		pack();
		setLocationRelativeTo(null);

		setVisible(true);
		// pack(); // TODO:
		// setBounds(100,100,850,800);

		if (frameToGoBackTo != null) {
			frameToGoBackTo.setVisible(false);
		}
	}

	/**
	 * Go back to the frame which called this frame to be opened
	 */
	public synchronized void navigateBack() {

		if (frameToGoBackTo != null) {
			setVisible(false);
			frameToGoBackTo.setVisible(true);
			backListener.wentBack();
		}
	}

	/**
	 * Exit the program entirely, NOT navigating back to the prior frame (as in
	 * {@link #navigateBack()})
	 */
	public void quit() {

		if (Utils.confirmAction("Confirm Quit", "Are you sure you want to exit?", ref.get())) {
			this.setVisible(false);
			System.exit(0);
		}

	}

	/**
	 * Set the current panel state.
	 * 
	 * @param state One of {@link #STATE_INIT}, {@link #STATE_LOADED},
	 *              {@link #STATE_STATS_RUN}.
	 */
	private synchronized void setPanelState(int state) {
		switch (state) {
		case STATE_INIT:
			this.wiastat = null;
			tableWIASelections.removeAllWIAData();
			tableWIATxNames.removeData();
			tableStandardWave.removeWaves();
			tableStandardWaveGroups.removeGroups();
			Utils.setEnabledDeep(false, true, false, pnlOutput);
			Utils.setEnabled(true, false, btnBrowse);
			Utils.setEnabled(false, true, btnGroup);
			break;
		case STATE_LOADED:
			Utils.setEnabledDeep(true, true, false, pnlOutput);
			Utils.setEnabled(false, false, btnBrowse, btnSaveStats);
			btnRunStats.setIcon(null);
			btnSaveCompiledData.setIcon(null);
			btnSaveStats.setIcon(null);
			Utils.setEnabled(true, false, btnGroup);
			break;
		case STATE_STATS_RUN:
			Utils.setEnabledDeep(true, false, false, pnlOutput);
			Utils.setEnabled(false, false, btnBrowse);
			Utils.setEnabled(true, false, btnGroup, btnSaveCompiledData, btnSaveStats);
			btnRunStats.setIcon(Utils.IconSuccess);
			btnSaveCompiledData.setIcon(null);
			btnSaveStats.setIcon(null);
			break;
		}
	}

	/**
	 * Prompts the user to select a directory, then loads all .wia files from that
	 * directory into the {@link WIAStats} object for analysis.
	 */
	private synchronized void runFileSelection() {

		File file = Utils.promptUserForDirectory("Select Folder", config.getLastDirectoryPath());
		if (file == null)
			return;

		config.tryToSetLastDir(file);

		wiastat = new WIAStats("test");
		String errors = wiastat.loadFiles(file, true);
		if (errors != null) {
			Utils.showError(errors, this);
			return;
		}
		setPanelState(STATE_LOADED);

		tableWIASelections.addWIAData(wiastat);
		tableWIATxNames.addTreatments(wiastat);
		tableStandardWave.addWaves(wiastat);

		// config.tryToSetLastDir(file);

	}

	/**
	 * Prompts the user for a file name and saves the calculated statistics to an Excel (.xlsx) file.
	 */
	private synchronized void runStatsSave() {

		File fileToSave = Utils.promptUserForFileName("Save Stats", config.getLastDirectoryPath(), "stats.xlsx",
				".xlsx");
		if (fileToSave == null) {
			return;
		} else if (!Utils.hasOkayExtension(fileToSave, ".xlsx")) {
			// filed HAD an extension and it was not .csv
			Utils.showError("File must be saved as .csv", ref.get());
			return;
		}
		fileToSave = Utils.appendExtensionIfDoesNotHaveExt(fileToSave, ".xlsx");
		
		try {
			wiastat.save(fileToSave);
		} catch (IOException e) {
			Utils.showError("<html>Could not save file.<br><br>Error msg: " + e.getMessage() + "</html>", ref.get());
		}
	}

	/**
	 * Prompts the user for a file name and saves the compiled, aggregated data from all
	 * loaded .wia files into a single CSV file.
	 */
	private synchronized void runDataSave() {
		String[][] dataSaveableArray = wiastat.getDataArray();
		File fileToSav = Utils.promptUserForFileName("Save Data", config.getLastDirectoryPath(), "compiled data.csv",
				".csv");
		if (fileToSav == null || !Utils.hasOkayExtension(fileToSav, ".csv")) {
			// user cancelled
			return;
		} else if (!Utils.hasOkayExtension(fileToSav, ".csv")) {
			// filed HAD an extension and it was not .csv
			Utils.showError("File must be saved as .csv", ref.get());
			return;
		}
		fileToSav = Utils.appendExtensionIfDoesNotHaveExt(fileToSav, ".csv");

		Saver.saveData(fileToSav, dataSaveableArray);
	}

	@Override
	public void removedWaveGroup(StandardWaveGrouping waveGrouping) {
		tableStandardWaveGroups.updateGroups(wiastat);
	}

	/**
	 * Called when user removes a wave in the wave table
	 */
	@Override
	public void removedWave(StandardWave wave) {
		tableStandardWaveGroups.updateGroups(wiastat);

	}

	/**
	 * called when a user changes a wave name in the wave table
	 */
	@Override
	public void changedWaveName() {
		tableStandardWaveGroups.updateGroups(wiastat);

	}

	/**
	 * Callback method invoked when a WIA data file is removed from the selection table.
	 * This triggers updates to the treatment names, standard waves, and wave groups tables.
	 */
	@Override
	public void wiaDataRemoved() {
		tableWIATxNames.updateTreatments(wiastat);
		tableStandardWave.updateWaves(wiastat);
		tableStandardWaveGroups.updateGroups(wiastat);

	}

	/**
	 * Callback method invoked when a treatment name is changed for a WIA data file.
	 * This triggers an update of the treatment names table.
	 */
	@Override
	public void wiaDataTxNameChanged() {
		tableWIATxNames.updateTreatments(wiastat);

	}

	/**
	 * Provides access to the collection of {@link WIAData} objects loaded for analysis.
	 * @return A collection of the loaded WIA data.
	 */
	@Override
	public Collection<WIAData> getData() {
		return wiastat.getData();
	}

	///////////////////////////////////////////
	// Helper methods for building GUI
	///////////////////////////////////////////

	/**
	 * Helper method for building GUI
	 */
	private void _initPnlTop() {

		pnlTop = new JPanel();
		pnlTop.setBackground(new Color(192, 192, 192));
		pnlTop.setBorder(new LineBorder(new Color(0, 0, 0)));
		JCLabel lblTopInstruc = new JCLabel("Select wave intensity analysis file (.wia) to analyze",
				JCLabel.LABEL_SUBTITLE);
		pnlTop.add(lblTopInstruc);
	}

	/**
	 * Helper method for building GUI
	 */
	private void _initPnlMiddle() {
		pnlMiddle = new JPanel();
		pnlMiddle.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		_initPnlInput();
		_initPnlOutput();

		GroupLayout gl_pnlMiddle = new GroupLayout(pnlMiddle);
		gl_pnlMiddle
				.setHorizontalGroup(gl_pnlMiddle.createSequentialGroup().addContainerGap()
						.addGroup(gl_pnlMiddle.createParallelGroup()
								.addComponent(pnlInput, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addComponent(pnlOutput, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE))
						.addContainerGap());
		gl_pnlMiddle.setVerticalGroup(gl_pnlMiddle.createParallelGroup(Alignment.LEADING).addGroup(gl_pnlMiddle
				.createSequentialGroup().addContainerGap()
				.addComponent(pnlInput, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(pnlOutput, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		pnlMiddle.setLayout(gl_pnlMiddle);

	}

	/**
	 * Helper method for building GUI
	 */
	public void _initPnlBottom() {
		pnlBottom = new JPanel();
		FlowLayout flowLayout = (FlowLayout) pnlBottom.getLayout();
		flowLayout.setAlignment(FlowLayout.TRAILING);
		pnlBottom.setBackground(new Color(192, 192, 192));
		pnlBottom.setBorder(new LineBorder(new Color(0, 0, 0)));

		JCButton btnReset = new JCButton("Reset", JCButton.BUTTON_RESET);
		btnReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Utils.confirmAction("Confirm Reset", "Are you sure you want to reset?", ref.get())) {
					setPanelState(STATE_INIT);
				}
			}
		});

		JCButton btnBack = new JCButton("Main Menu", JCButton.BUTTON_GO_BACK);
		btnBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Utils.confirmAction("Confirm", "You will lose all progress. Sure you want to go back?",
						ref.get())) {
					navigateBack();
				}
			}
		});

		JCButton btnQuit = new JCButton("Quit", JCButton.BUTTON_QUIT);
		btnQuit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				quit();
			}
		});

		pnlBottom.add(btnReset);
		pnlBottom.add(btnBack);
		pnlBottom.add(btnQuit);
	}

	/**
	 * Helper method for building GUI
	 */
	private void _initPnlInput() {

		pnlInput = new JPanel();
		pnlInput.setBackground(Utils.colorPnlEnabled);
		pnlInput.setBorder(new LineBorder(Color.BLACK));

		JCLabel lblInput = new JCLabel("Input", JCLabel.LABEL_SUBTITLE);
		JCLabel lblDirections = new JCLabel("Select a folder which contains \".wia\" files", JCLabel.LABEL_TEXT_BOLD);
		JCLabel lblWaves = new JCLabel("Waves:", JCLabel.LABEL_TEXT_BOLD);
		btnGroup = new JCButton("Group Selected", JCButton.BUTTON_SMALL);
		btnGroup.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				btnGroup.setEnabled(false);

				Set<StandardWave> wavesSelected = tableStandardWave.getSelectedWaves();

				if (!wavesSelected.isEmpty()) {
					String groupName = Utils.promptTextInput("Name of wave:", ref.get());
					if (groupName != null) {
						if (!wiastat.containsGroup(groupName)) {
							wiastat.addWaveGrouping(groupName, wavesSelected);
							tableStandardWaveGroups.updateGroups(wiastat);
							tableStandardWave.clearSelection();
						} else {
							Utils.showError("Group \"" + groupName + "\" already exists", ref.get());
						}
					}
				}

				btnGroup.setEnabled(true);
			}

		});
		JCLabel lblGroupedWaves = new JCLabel("Wave Groups:", JCLabel.LABEL_TEXT_BOLD);

		JCLabel lblSelections = new JCLabel("Selections", JCLabel.LABEL_TEXT_BOLD);
		JScrollPane scrFiles = new JScrollPane();
		tableWIASelections = WIAFileSelectionTable.generate(this, scrFiles);
		scrFiles.setViewportView(tableWIASelections);
		tableWIASelections.setFillsViewportHeight(true);
		tableWIASelections.setPreferredScrollableViewportSize(
				new Dimension(tableWIASelections.getPreferredSize().width, tableWIASelections.getRowHeight() * 10));

		JCLabel lblTxNames = new JCLabel("Tx Names", JCLabel.LABEL_TEXT_BOLD);
		JScrollPane scrTxtNames = new JScrollPane();
		tableWIATxNames = WIATxNameTable.generate();
		scrTxtNames.setViewportView(tableWIATxNames);
		tableWIATxNames.setFillsViewportHeight(true);
		tableWIATxNames.setPreferredScrollableViewportSize(
				new Dimension(tableWIATxNames.getPreferredSize().width, tableWIATxNames.getRowHeight() * 5));

		JScrollPane scrWaves = new JScrollPane();
		tableStandardWave = StandardWaveTable.generate(this);
		scrWaves.setViewportView(tableStandardWave);
		tableStandardWave.setFillsViewportHeight(true);
		tableStandardWave.setPreferredScrollableViewportSize(
				new Dimension(tableStandardWave.getPreferredSize().width, tableStandardWave.getRowHeight() * 8));

		JScrollPane scrWaveGroups = new JScrollPane();
		tableStandardWaveGroups = StandardWaveGroupTable.generate(this);
		scrWaveGroups.setViewportView(tableStandardWaveGroups);
		tableStandardWaveGroups.setFillsViewportHeight(true);
		tableStandardWaveGroups.setPreferredScrollableViewportSize(new Dimension(
				tableStandardWaveGroups.getPreferredSize().width, tableStandardWaveGroups.getRowHeight() * 3));

		btnBrowse = new JCButton("Browse...");
		btnBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runFileSelection();
			}
		});
		JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
		JSeparator sep2 = new JSeparator(SwingConstants.HORIZONTAL);
		sep.setForeground(Color.GRAY);
		sep2.setForeground(Color.GRAY);

		GroupLayout gl_pnlIn = new GroupLayout(pnlInput);
		gl_pnlIn.setHorizontalGroup(gl_pnlIn.createParallelGroup(Alignment.LEADING)
				.addComponent(lblInput, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE)
				.addGroup(gl_pnlIn.createSequentialGroup().addContainerGap().addGroup(gl_pnlIn
						.createParallelGroup(Alignment.LEADING, false).addComponent(lblDirections)
						.addComponent(btnBrowse)
						.addComponent(sep2, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addGroup(gl_pnlIn.createSequentialGroup()
								.addComponent(lblWaves, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addComponent(btnGroup))
						.addComponent(scrWaves).addComponent(lblGroupedWaves).addComponent(scrWaveGroups))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(
								sep, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlIn.createParallelGroup(Alignment.LEADING)
								.addComponent(scrFiles, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addComponent(lblSelections).addComponent(scrTxtNames, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
								.addComponent(lblTxNames))
						.addContainerGap()));
		gl_pnlIn.setVerticalGroup(gl_pnlIn.createParallelGroup(Alignment.LEADING).addGroup(gl_pnlIn
				.createSequentialGroup()
				.addComponent(
						lblInput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_pnlIn.createParallelGroup(Alignment.LEADING)
						.addComponent(sep, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addGroup(gl_pnlIn.createSequentialGroup().addComponent(lblSelections)
								.addComponent(scrFiles, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(lblTxNames)
								.addComponent(scrTxtNames, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addGroup(
								gl_pnlIn.createSequentialGroup()
										.addComponent(lblDirections, GroupLayout.PREFERRED_SIZE,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.UNRELATED)
										.addComponent(btnBrowse, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.UNRELATED)
										.addComponent(sep2, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addGroup(gl_pnlIn.createParallelGroup(Alignment.CENTER).addComponent(lblWaves)
												.addComponent(btnGroup))
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(scrWaves, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED).addComponent(lblGroupedWaves)
										.addComponent(scrWaveGroups, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)))
				.addContainerGap()));
		pnlInput.setLayout(gl_pnlIn);

	}

	/**
	 * Helper method for building GUI
	 */
	private void _initPnlOutput() {
		pnlOutput = new JPanel();
		pnlOutput.setBackground(Utils.colorPnlEnabled);
		pnlOutput.setBorder(new LineBorder(Color.BLACK));

		JCLabel lblOut = new JCLabel("Output", JCLabel.LABEL_SUBTITLE);
		btnRunStats = new JCButton("Run Stats", JCButton.BUTTON_RUN);
		btnSaveCompiledData = new JCButton("Save Compiled Data", JCButton.BUTTON_STANDARD);
		btnSaveStats = new JCButton("Save Stats", JCButton.BUTTON_STANDARD);

		JSeparator jsep = new JSeparator(SwingConstants.VERTICAL);
		JSeparator jsepH = new JSeparator(SwingConstants.HORIZONTAL);
		jsep.setForeground(Color.GRAY);
		jsepH.setForeground(Color.GRAY);

		btnRunStats.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (wiastat != null) {
					try {
						wiastat.runStats();
					} catch (StatisticalException e1) {
						btnRunStats.setIcon(Utils.IconFail);
						e1.printStackTrace();
						Utils.showError("Unable to perform stats: " + e1.getMessage(), null);
						return;
					}

					setPanelState(STATE_STATS_RUN);
				}

			}

		});

		btnSaveStats.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				runStatsSave();

			}

		});

		btnSaveCompiledData.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				runDataSave();

			}

		});

		GroupLayout gl_pnlOut = new GroupLayout(pnlOutput);
		gl_pnlOut.setHorizontalGroup(gl_pnlOut.createParallelGroup(Alignment.LEADING).addComponent(lblOut)
				.addGroup(gl_pnlOut.createSequentialGroup().addContainerGap().addComponent(btnSaveCompiledData)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(jsep, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(btnRunStats)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnSaveStats).addContainerGap())

		);
		gl_pnlOut
				.setVerticalGroup(
						gl_pnlOut.createSequentialGroup().addComponent(lblOut)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_pnlOut.createParallelGroup(Alignment.LEADING)
										.addComponent(btnSaveCompiledData).addComponent(btnRunStats).addComponent(jsep,
												GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
										.addComponent(btnSaveStats)

								).addContainerGap());

		pnlOutput.setLayout(gl_pnlOut);

	}

}
