package com.carrington.WIA.GUIs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.carrington.WIA.Utils;
import com.carrington.WIA.Cardio.Beat;
import com.carrington.WIA.Cardio.BeatSelection;
import com.carrington.WIA.DataStructures.HemoData;
import com.carrington.WIA.GUIs.Components.BeatsSelectionTable;
import com.carrington.WIA.GUIs.Components.BeatsSelectionTable.SelectionTableListener;
import com.carrington.WIA.GUIs.Components.JCButton;
import com.carrington.WIA.Graph.AlignChartPanel;
import com.carrington.WIA.Graph.AlignChartPanel.AlignChartPanelListener;
import com.carrington.WIA.IO.Header;
import com.carrington.WIA.Math.FlowUnit;
import com.carrington.WIA.Math.PressureUnit;

/**
 * GUI for aligning two files. The user can visually align traces, select
 * corresponding heartbeats from each file, and then generate an ensembled
 * result from these selections. This is crucial for comparing hemodynamic data
 * (i.e. pressure/flow) recorded with different equipment.
 */
public class AlignerGUI extends JDialog implements SelectionTableListener, AlignChartPanelListener {

	private static final long serialVersionUID = -8183475955290511152L;
	private final JPanel contentPanel = new JPanel();

	private static final Color pnlTopColor = new Color(169, 169, 169);
	private static final Color pnlOtherColor = new Color(213, 213, 213);

	private static final String displayTraceAll = "All traces (Q)";
	private static final String displayTraceEKG = "EKG trace only (W)";
	private static final String displayTraceNonEKG = "All traces except EKG (E)";

	private final WeakReference<AlignerGUI> ref = new WeakReference<AlignerGUI>(this);
	private AlignChartPanel pnlDisplay;
	private JTextField txtTimeSync1;
	private JTextField txtTimeSync2;
	private JTextField txtBeatsTop;
	private JTextField txtBeatsBott;
	private BeatsSelectionTable table;
	private JComboBox<String> cbTraces;
	private JCheckBox chLockSamples;
	private JCheckBox chAutoBeat;
	private JCheckBox chAutoR;
	private JComboBox<Trace> cbTypePressure;
	private JComboBox<PressureUnit> cbUnitsPressure;
	private JComboBox<Trace> cbTypeFlow;
	private JComboBox<FlowUnit> cbUnitsFlow;
	private JCButton btnSetSync;
	private JCButton btnAddSel;
	private JCButton btnResetCurrSel;

	/**
	 * One of {@link HemoData#ENSEMBLE_TRIM} or {@link HemoData#ENSEMBLE_SCALE}.
	 * This determines the ensembling method.
	 */
	@SuppressWarnings("unused")
	private final int trimSelection;

	/**
	 * Stores the final result of the alignment process, containing ensembled beats.
	 * It is null until the user accepts the selections.
	 */
	private AlignResult alignResult;

	private Component componentParent;

	/**
	 * Create the dialog. * @param hd1 The first hemodynamic data object to align.
	 * 
	 * @param hd2          The second hemodynamic data object to align.
	 * @param ensembleType The type of ensembling to perform, either
	 *                     {@link HemoData#ENSEMBLE_TRIM} or
	 *                     {@link HemoData#ENSEMBLE_SCALE}.
	 * @param parent       The parent component, used for positioning the dialog.
	 * 
	 * @throws OutOfMemoryError         if the {@link HemoData} is too large - may
	 *                                  need to trim
	 * @throws IllegalArgumentException if there is not a flow and pressure trace
	 *                                  contained in the hemo data
	 */
	@SuppressWarnings("serial")
	public AlignerGUI(HemoData hd1, HemoData hd2, int ensembleType, Component parent)
			throws OutOfMemoryError, IllegalArgumentException {

		this.trimSelection = ensembleType;
		this.componentParent = parent;

		// make sure we have flow and pressure traces
		List<Trace> tracesPressure = getTracesPressure(hd1, hd2);
		List<Trace> tracesFlow = getTracesFlow(hd1, hd2);
		if (tracesPressure.size() < 2 || tracesPressure.size() < 2) {
			throw new IllegalArgumentException(
					"Less than 2 traces, but must be at least 2, one for pressure and one for flow!");
		}

		setModal(true);
		setTitle("Pick selections");
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);

		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				discard();
			}
		});

		JPanel pnlTop = new JPanel();
		FlowLayout pnlTopLayout = (FlowLayout) pnlTop.getLayout();
		pnlTopLayout.setHgap(10);

		pnlTop.setBorder(new LineBorder(new Color(0, 0, 0)));
		pnlTop.setBackground(pnlTopColor);

		// pnlDisplay =AlignChartPanel.createDummy(this); // TODO revert
		pnlDisplay = new AlignChartPanel(hd1, hd2, this, true, true);

		pnlDisplay.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		JPanel pnlSelections = new JPanel();
		pnlSelections.setBackground(pnlOtherColor);
		pnlSelections.setBorder(new LineBorder(new Color(0, 0, 0)));

		JPanel pnlBottom = new JPanel();
		pnlBottom.setBackground(pnlOtherColor);
		pnlBottom.setBorder(new LineBorder(new Color(0, 0, 0)));
		FlowLayout flowLayout = (FlowLayout) pnlBottom.getLayout();
		flowLayout.setAlignment(FlowLayout.TRAILING);

		JPanel pnlTopGraph = new JPanel();
		pnlTopGraph.setOpaque(false);

		JPanel pnlBottomGraph = new JPanel();
		pnlBottomGraph.setOpaque(false);

		JLabel lblDisplay = new JLabel("Display");

		cbTraces = new JComboBox<String>(new String[] { displayTraceAll, displayTraceEKG, displayTraceNonEKG });
		pnlDisplay.showVisibleTraces(1);
		cbTraces.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String option = cbTraces.getSelectedItem().toString();

				switch (option) {
				case displayTraceAll:
					pnlDisplay.showVisibleTraces(1);
					break;
				case displayTraceEKG:
					pnlDisplay.showVisibleTraces(2);

					break;
				case displayTraceNonEKG:
					pnlDisplay.showVisibleTraces(3);
					break;
				default:
					break;
				}

			}

		});

		JLabel lblSync = new JLabel("Center at Time Point");

		JButton btnSyncHelp = new JButton();

		JCheckBox chShowAlignLines = new JCheckBox("Alignment Lines");
		chShowAlignLines.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pnlDisplay.setAlignLines(chShowAlignLines.isSelected());
			}
		});
		chShowAlignLines.setOpaque(false);
		chShowAlignLines.setFocusable(false);
		chShowAlignLines.setSelected(false);
		pnlDisplay.setAlignLines(false);

		chLockSamples = new JCheckBox("Lock Alignment");
		chLockSamples.setOpaque(false);
		chLockSamples.setFocusable(false);
		chLockSamples.setSelected(false);
		chLockSamples.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (chLockSamples.isSelected()) {
					txtTimeSync1.setText("");
					txtTimeSync2.setText("");

				}
				txtTimeSync1.setEnabled(!chLockSamples.isSelected());
				txtTimeSync2.setEnabled(!chLockSamples.isSelected());
				btnSetSync.setEnabled(!chLockSamples.isSelected());

				pnlDisplay.setLocked(chLockSamples.isSelected());

			}

		});

		chAutoBeat = new JCheckBox("Auto Beat");
		chAutoBeat.setOpaque(false);
		chAutoBeat.setFocusable(false);
		chAutoBeat.setSelected(true);
		chAutoBeat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pnlDisplay.setAutoBeat(chAutoBeat.isSelected());
			}
		});

		chAutoR = new JCheckBox("Snap to R");
		chAutoR.setOpaque(false);
		chAutoR.setFocusable(false);
		chAutoR.setSelected(true);
		chAutoR.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pnlDisplay.setAutoSnapToR(chAutoR.isSelected());
			}
		});
		pnlDisplay.setAutoSnapToR(chAutoR.isSelected());

		JCButton btnFitTrace = new JCButton("Fit Trace (F)", JCButton.BUTTON_STANDARD);
		btnFitTrace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pnlDisplay.refresh();
			}
		});

		JLabel lblDataTypes = new JLabel("Data Types");

		JLabel lblPressure = new JLabel("Pressure tracing:");
		cbTypePressure = new JComboBox<Trace>();
		cbTypePressure.setEditable(false);
		cbTypePressure.addItem(new Trace("Select trace..."));
		cbTypePressure.setSelectedIndex(0);
		int selectIndex = -1;
		int counter = 1;
		for (Trace trace : tracesPressure) {
			if (trace.selectedDefault && selectIndex == -1) {
				selectIndex = counter;
			}
			counter++;
			cbTypePressure.addItem(trace);
		}
		if (selectIndex != -1) {
			cbTypePressure.setSelectedIndex(selectIndex);

		}
		cbTypePressure.setRenderer(new DefaultListCellRenderer() {

			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				if (index == 0 && !isSelected) {
					component.setForeground(Color.GRAY);
				} else {
					component.setForeground(Color.BLACK);
				}

				return component;
			}
		});
		cbUnitsPressure = new JComboBox<PressureUnit>();
		cbUnitsPressure.setEditable(false);
		cbUnitsPressure.addItem(PressureUnit.NEITHER);
		cbUnitsPressure.addItem(PressureUnit.MMHG);
		cbUnitsPressure.addItem(PressureUnit.PASCALS);
		cbUnitsPressure.setSelectedIndex(0);
		Trace selTrace = (Trace) cbTypePressure.getSelectedItem();
		if (selTrace != null && selTrace.hd != null) {
			PressureUnit pressureUnit = Utils.determinePressureUnit(selTrace.hd.getYData(selTrace.header));
			if (pressureUnit != null) {
				cbUnitsPressure.setSelectedIndex(Utils.getJComboBoxItemIndex(cbUnitsPressure, pressureUnit));
			}
		}
		cbUnitsPressure.setRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 6355491917846478726L;

			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				if (index == 0 && !isSelected) {
					component.setForeground(Color.GRAY);
				} else {
					component.setForeground(Color.BLACK);
				}

				return component;
			}
		});

		JLabel lblFlow = new JLabel("Flow tracing:");
		cbTypeFlow = new JComboBox<Trace>();
		cbTypeFlow.setEditable(false);
		cbTypeFlow.addItem(new Trace("Select trace..."));
		cbTypeFlow.setSelectedIndex(0);
		selectIndex = -1;
		counter = 1;
		for (Trace trace : tracesFlow) {
			if (trace.selectedDefault && selectIndex == -1) {
				selectIndex = counter;
			}
			counter++;
			cbTypeFlow.addItem(trace);
		}
		if (selectIndex != -1) {
			cbTypeFlow.setSelectedIndex(selectIndex);

		}
		cbTypeFlow.setRenderer(new DefaultListCellRenderer() {

			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				if (index == 0 && !isSelected) {
					component.setForeground(Color.GRAY);
				} else {
					component.setForeground(Color.BLACK);
				}

				return component;
			}
		});
		cbUnitsFlow = new JComboBox<FlowUnit>();
		cbUnitsFlow.setEditable(false);
		cbUnitsFlow.addItem(FlowUnit.NEITHER);
		cbUnitsFlow.addItem(FlowUnit.MPS);
		cbUnitsFlow.addItem(FlowUnit.CPS);
		cbUnitsFlow.setSelectedIndex(0);
		selTrace = (Trace) cbTypeFlow.getSelectedItem();
		if (selTrace != null && selTrace.hd != null) {
			FlowUnit flowUnit = Utils.determineFlowUnit(selTrace.hd.getYData(selTrace.header));
			if (flowUnit != null) {
				cbUnitsFlow.setSelectedIndex(Utils.getJComboBoxItemIndex(cbUnitsFlow, flowUnit));
			}
		}
		cbUnitsFlow.setRenderer(new DefaultListCellRenderer() {

			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				if (index == 0 && !isSelected) {
					component.setForeground(Color.GRAY);
				} else {
					component.setForeground(Color.BLACK);
				}

				return component;
			}
		});

		JLabel lblTimeSamp1 = new JLabel("Time sample 1:");
		JLabel lblTimeSamp2 = new JLabel("Time sample 2:");
		txtTimeSync1 = new JTextField();
		txtTimeSync1.setColumns(10);
		txtTimeSync2 = new JTextField();
		txtTimeSync2.setColumns(10);

		btnSetSync = new JCButton("Set", JCButton.BUTTON_STANDARD);
		btnSetSync.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String time1 = txtTimeSync1.getText();
				String time2 = txtTimeSync2.getText();
				Double time1d;
				Double time2d;
				if (!time1.isBlank()) {
					if (time1.contains(":")) {
						time1 = parseTimeToSeconds(time1) + "";
					}
					try {
						time1d = Double.parseDouble(time1);
					} catch (NumberFormatException ex) {
						Utils.showError("One of the times was not a valid number", ref.get());
						return;
					}
				} else {
					time1d = null;
				}
				if (!time2.isBlank()) {
					if (time2.contains(":")) {
						time2 = parseTimeToSeconds(time1) + "";
					}
					try {
						time2d = Double.parseDouble(time2);
					} catch (NumberFormatException ex) {
						Utils.showError("One of the times was not a valid number", ref.get());
						return;
					}
				} else {
					time2d = null;
				}

				boolean result = pnlDisplay.setTimeAlignment(time1d, time2d);
				if (!result) {
					Utils.showError("One of the times is outside of valid range.", ref.get());

				} else {
					ref.get().requestFocusInWindow();
				}
			}
		});

		JLabel lblSelections = new JLabel("Selections");

		JLabel lblSelTime1 = new JLabel("# Beats Top:");

		JLabel lblSelTime2 = new JLabel("# Beats Bottom:");

		txtBeatsTop = new JTextField();
		txtBeatsTop.setColumns(10);
		txtBeatsTop.setFocusable(false);
		txtBeatsTop.setEditable(false);

		txtBeatsBott = new JTextField();
		txtBeatsBott.setColumns(10);
		txtBeatsBott.setFocusable(false);
		txtBeatsBott.setEditable(false);

		btnAddSel = new JCButton("Add (A)", JCButton.BUTTON_STANDARD);
		btnAddSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addSelection();
			}
		});
		btnAddSel.setEnabled(true);

		btnResetCurrSel = new JCButton("Reset (R)", JCButton.BUTTON_STANDARD);
		btnResetCurrSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pnlDisplay.clearCurrentSelection();
				setNumBeatsTop(0);
				setNumBeatsBottom(0);
			}
		});
		btnResetCurrSel.setEnabled(true);
		JLabel dummyLabel = new JLabel("");

		JScrollPane scrSelections = new JScrollPane();
		scrSelections.setEnabled(false);

		int width = Utils.getFontParams(Utils.getTextFont(false), "1000")[1];
		int width2 = Utils.getFontParams(Utils.getTextFont(false), "Select trace... eee")[1];
		GroupLayout gl_pnlSelections = new GroupLayout(pnlSelections);
		gl_pnlSelections.setHorizontalGroup(gl_pnlSelections.createSequentialGroup().addGap(5).addGroup(gl_pnlSelections
				.createParallelGroup(Alignment.LEADING)
				.addComponent(lblDisplay, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
						Short.MAX_VALUE)
				.addGroup(gl_pnlSelections.createSequentialGroup()
						.addComponent(lblSync, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnSyncHelp))
				.addGroup(Alignment.LEADING, gl_pnlSelections.createSequentialGroup().addGap(5)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING).addComponent(lblTimeSamp1)
								.addComponent(lblTimeSamp2))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING)
								.addComponent(txtTimeSync1, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addComponent(txtTimeSync2, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addComponent(btnSetSync, Alignment.TRAILING)))
				.addComponent(lblDataTypes, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
						Short.MAX_VALUE)
				.addGroup(Alignment.LEADING, gl_pnlSelections.createSequentialGroup().addGap(5)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING).addComponent(lblPressure)
								.addGroup(gl_pnlSelections.createSequentialGroup()
										.addComponent(cbTypePressure, width2, width2, Short.MAX_VALUE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(cbUnitsPressure, width2, width2, Short.MAX_VALUE))
								.addComponent(lblFlow)
								.addGroup(gl_pnlSelections.createSequentialGroup()
										.addComponent(cbTypeFlow, width2, width2, Short.MAX_VALUE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(cbUnitsFlow, width2, width2, Short.MAX_VALUE))))
				.addComponent(lblSelections, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
						Short.MAX_VALUE)
				.addGroup(Alignment.LEADING,
						gl_pnlSelections.createSequentialGroup().addGap(5)
								.addGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING)
										.addComponent(lblSelTime1).addComponent(lblSelTime2))
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING)
										.addComponent(txtBeatsTop, GroupLayout.DEFAULT_SIZE, width, width)
										.addComponent(txtBeatsBott, GroupLayout.DEFAULT_SIZE, width, width)))
				.addGroup(Alignment.LEADING,
						gl_pnlSelections.createSequentialGroup().addGap(5).addComponent(btnAddSel)
								.addComponent(dummyLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addComponent(btnResetCurrSel))
				.addGroup(Alignment.LEADING, gl_pnlSelections.createSequentialGroup().addGap(5)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING).addComponent(chShowAlignLines)
								.addComponent(cbTraces).addComponent(chLockSamples).addComponent(chAutoBeat)
								.addComponent(chAutoR).addComponent(btnFitTrace)))
				.addGroup(gl_pnlSelections.createSequentialGroup().addGap(5).addComponent(scrSelections,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)))
				.addContainerGap());
		gl_pnlSelections.setVerticalGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_pnlSelections.createSequentialGroup().addGap(5).addComponent(lblDisplay)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(cbTraces, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(chShowAlignLines).addComponent(chLockSamples).addComponent(chAutoBeat)
						.addComponent(chAutoR).addComponent(btnFitTrace).addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(lblDataTypes).addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(lblPressure)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.BASELINE)
								.addComponent(cbTypePressure, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(cbUnitsPressure, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(lblFlow)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.BASELINE)
								.addComponent(cbTypeFlow, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(cbUnitsFlow, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(gl_pnlSelections
								.createParallelGroup(Alignment.CENTER).addComponent(lblSync).addComponent(btnSyncHelp))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.BASELINE).addComponent(lblTimeSamp1)
								.addComponent(txtTimeSync1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.BASELINE).addComponent(lblTimeSamp2)
								.addComponent(txtTimeSync2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnSetSync)
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(lblSelections)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.BASELINE).addComponent(lblSelTime1)
								.addComponent(txtBeatsTop, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.BASELINE).addComponent(lblSelTime2)
								.addComponent(txtBeatsBott, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.BASELINE).addComponent(btnAddSel)
								.addComponent(dummyLabel).addComponent(btnResetCurrSel))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(scrSelections, GroupLayout.PREFERRED_SIZE,
								Utils.getFontParams(Utils.getTextFont(false), null)[0] * 8, GroupLayout.PREFERRED_SIZE)
						.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		table = BeatsSelectionTable.generate(this);
		table.setFillsViewportHeight(true);
		scrSelections.setViewportView(table);

		pnlSelections.setLayout(gl_pnlSelections);

		JCButton btnExit = new JCButton("Exit", JCButton.BUTTON_QUIT);
		btnExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Utils.confirmAction("Confirm",
						"If you exit now, you will lose all alignment and selection. Are you SURE?", ref.get())) {
					discard();
				}
			}
		});
		pnlBottom.add(btnExit);

		JCButton btnAccept = new JCButton("Accept", JCButton.BUTTON_ACCEPT);
		btnAccept.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (pnlDisplay.getNumberOfSelections() == 0) {
					if (Utils.confirmAction("Confirm", "You need to make a selection. Sure you want to exit?",
							ref.get())) {
						discard();
						alignResult = null;
						return;
					} else {
						return;
					}
				}

				Trace pressureTrace = (Trace) cbTypePressure.getSelectedItem();
				PressureUnit pressureUnits = (PressureUnit) cbUnitsPressure.getSelectedItem();
				Trace flowTrace = (Trace) cbTypeFlow.getSelectedItem();
				FlowUnit flowUnits = (FlowUnit) cbUnitsFlow.getSelectedItem();
				if (pressureTrace.hd == null || flowTrace.hd == null) {
					if (Utils.confirmAction("Confirm",
							"You need to select the pressure and trace. Sure you want to exit?", ref.get())) {
						alignResult = null;
						discard();
						return;
					} else {
						return;
					}
				}

				if (pressureUnits == PressureUnit.NEITHER || flowUnits == FlowUnit.NEITHER) {
					if (Utils.confirmAction("Confirm",
							"You need to select the pressure and trace units. Sure you want to exit?", ref.get())) {
						alignResult = null;
						discard();
						return;
					} else {
						return;
					}
				}
				pressureTrace.hd.addFlags(pressureTrace.header, HemoData.TYPE_PRESSURE);
				pressureTrace.hd.addFlags(pressureTrace.header,
						pressureUnits == PressureUnit.MMHG ? HemoData.UNIT_MMHG : HemoData.UNIT_PASCAL);
				flowTrace.hd.addFlags(flowTrace.header, HemoData.TYPE_FLOW);
				flowTrace.hd.addFlags(flowTrace.header,
						flowUnits == FlowUnit.MPS ? HemoData.UNIT_MperS : HemoData.UNIT_CMperS);

				// Make sure that all the Beats within BeatSelection have updated flags also
				for (BeatSelection selection : getBeatSelections()) {
					List<Beat> allBeats = selection.getBeats();
					for (Beat beat : allBeats) {
						if (beat.getData().hasHeader(pressureTrace.header)) {
							beat.getData().addFlags(pressureTrace.header, HemoData.TYPE_PRESSURE);
							beat.getData().addFlags(pressureTrace.header,
									pressureUnits == PressureUnit.MMHG ? HemoData.UNIT_MMHG : HemoData.UNIT_PASCAL);

						}
						if (beat.getData().hasHeader(flowTrace.header)) {
							beat.getData().addFlags(flowTrace.header, HemoData.TYPE_FLOW);
							beat.getData().addFlags(flowTrace.header,
									flowUnits == FlowUnit.MPS ? HemoData.UNIT_MperS : HemoData.UNIT_CMperS);

						}
					}

				}

				alignResult = new AlignResult(getBeatSelections(), ensembleType);

				discard();

			}
		});
		pnlBottom.add(btnAccept);

		JLabel lblTopInstruction = new JLabel("Make selections of interest.");
		pnlTop.add(lblTopInstruction);

		JButton btnHelp = new JButton();
		btnHelp.setIcon(Utils.IconQuestionLarger);
		btnHelp.setRolloverIcon(Utils.IconQuestionLargerHover);
		btnHelp.setContentAreaFilled(false);
		btnHelp.setBorderPainted(false);
		btnHelp.setBorder(null);

		btnSyncHelp.setIcon(Utils.IconQuestionLarger);
		btnSyncHelp.setRolloverIcon(Utils.IconQuestionLargerHover);
		btnSyncHelp.setContentAreaFilled(false);
		btnSyncHelp.setBorderPainted(false);
		btnSyncHelp.setBorder(null);

		pnlTop.add(btnHelp);

		Utils.setFont(Utils.getTextFont(false), chShowAlignLines, cbTraces, chLockSamples, chAutoBeat, chAutoR,
				txtTimeSync2, txtTimeSync1, lblTimeSamp1, lblTimeSamp2, lblSelTime1, lblSelTime2, txtBeatsTop,
				txtBeatsBott, lblPressure, lblFlow, cbTypeFlow, cbTypePressure, cbUnitsFlow, cbUnitsPressure);
		Utils.setFont(Utils.getTextFont(true), btnSyncHelp);
		Utils.setFont(Utils.getSubTitleFont(), lblTopInstruction, lblDisplay, lblDataTypes, lblSync, lblSelections);

		Utils.unfocusButtons(contentPanel);
		Utils.unfocusAll(pnlBottom);
		Utils.unfocusAll(pnlTop);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// Request focus on the frame, causing the JTextField to lose focus
				ref.get().requestFocusInWindow();
			}
		});

		setPreferredSize(new Dimension(1200, 800));

		pnlDisplay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false),
				"zoomIn");
		pnlDisplay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "zoomOut");
		pnlDisplay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false),
				"addSel");
		pnlDisplay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0, false),
				"traceAll");
		pnlDisplay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false),
				"traceEKG");
		pnlDisplay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0, false),
				"traceNonEKG");

		pnlDisplay.getActionMap().put("zoomIn", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (Utils.doesTextFieldHaveFocus(ref.get())) {
					return;
				}
				pnlDisplay.zoomIn();

			}
		});
		pnlDisplay.getActionMap().put("zoomOut", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (Utils.doesTextFieldHaveFocus(ref.get())) {
					return;
				}
				pnlDisplay.zoomOut();
			}
		});

		pnlDisplay.getActionMap().put("addSel", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (Utils.doesTextFieldHaveFocus(ref.get())) {
					return;
				}
				addSelection();
			}
		});
		pnlDisplay.getActionMap().put("traceAll", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (Utils.doesTextFieldHaveFocus(ref.get())) {
					return;
				}
				cbTraces.setSelectedItem(displayTraceAll);
			}
		});
		pnlDisplay.getActionMap().put("traceEKG", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (Utils.doesTextFieldHaveFocus(ref.get())) {
					return;
				}
				cbTraces.setSelectedItem(displayTraceEKG);
			}
		});
		pnlDisplay.getActionMap().put("traceNonEKG", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (Utils.doesTextFieldHaveFocus(ref.get())) {
					return;
				}
				cbTraces.setSelectedItem(displayTraceNonEKG);
			}
		});

		pnlDisplay.grabFocus();
		pnlDisplay.setFocusCycleRoot(true);

		setNumBeatsTop(0);
		setNumBeatsBottom(0);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_contentPanel.createSequentialGroup().addGap(3).addGroup(gl_contentPanel
						.createParallelGroup(Alignment.TRAILING)
						.addComponent(pnlTop, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addGroup(gl_contentPanel.createSequentialGroup()
								.addComponent(pnlDisplay, screenSize.width / 2, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(pnlSelections,
										pnlSelections.getMinimumSize().width, GroupLayout.DEFAULT_SIZE,
										pnlSelections.getMinimumSize().width))
						.addComponent(pnlBottom, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE))
						.addGap(3)));
		gl_contentPanel.setVerticalGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING).addGroup(gl_contentPanel
				.createSequentialGroup().addGap(3)
				.addComponent(
						pnlTop, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
						.addComponent(pnlSelections, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE)
						.addComponent(pnlDisplay, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
								Short.MAX_VALUE))
				.addPreferredGap(ComponentPlacement.RELATED).addComponent(pnlBottom, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addGap(3)));
		contentPanel.setLayout(gl_contentPanel);

		pack();

		Utils.unfocusAll(this);
		txtTimeSync1.setFocusable(true);
		txtTimeSync2.setFocusable(true);
		pnlDisplay.setFocusable(true);
		pnlDisplay.grabFocus();

		this.setSize(getMinimumSize());
		setLocationRelativeTo(null);

	}

	/**
	 * Extracts and identifies potential pressure traces from two HemoData objects.
	 *
	 * @param hd1 The first {@link HemoData} object.
	 * @param hd2 The second {@link HemoData} object.
	 * @return A list of {@link Trace} objects identified as potential pressure
	 *         traces.
	 */
	private List<Trace> getTracesPressure(HemoData hd1, HemoData hd2) {

		List<Trace> traces = new LinkedList<Trace>();
		for (Header header : hd1.getYHeaders()) {
			Trace trace = new Trace(hd1, header);
			if (trace.isLikelyPressure()) {
				trace.selectedDefault = true;
			}
			traces.add(trace);
		}
		for (Header header : hd2.getYHeaders()) {
			Trace trace = new Trace(hd2, header);
			if (trace.isLikelyPressure()) {
				trace.selectedDefault = true;
			}
			traces.add(trace);
		}

		if (!traces.stream().anyMatch(trace -> trace.selectedDefault)) {
			HemoData hdMatch = null;
			if (isLikelyPressureString(hd1.getName())) {
				hdMatch = hd1;
			} else if (isLikelyPressureString(hd2.getName())) {
				hdMatch = hd2;
			}

			if (hdMatch != null) {
				Trace match = null;
				for (Trace trace : traces) {
					if (trace.hd.equals(hdMatch) && !trace.isAlign) {
						if (match == null) {
							match = trace;
						} else {
							match = null;
							break;
						}
					}
				}

				if (match != null) {
					match.selectedDefault = true;
				}
			}
		}

		return traces;

	}

	/**
	 * Extracts and identifies potential flow traces from two {@link HemoData}
	 * objects.
	 *
	 * @param hd1 The first {@link HemoData} object.
	 * @param hd2 The second {@link HemoData} object.
	 * @return A list of {@link Trace} objects identified as potential flow traces.
	 */
	private List<Trace> getTracesFlow(HemoData hd1, HemoData hd2) {

		List<Trace> traces = new LinkedList<Trace>();
		for (Header header : hd1.getYHeaders()) {
			Trace trace = new Trace(hd1, header);
			if (trace.isLikelyFlow()) {
				trace.selectedDefault = true;
			}
			traces.add(trace);
		}
		for (Header header : hd2.getYHeaders()) {
			Trace trace = new Trace(hd2, header);
			if (trace.isLikelyFlow()) {
				trace.selectedDefault = true;
			}
			traces.add(trace);
		}

		if (!traces.stream().anyMatch(trace -> trace.selectedDefault)) {
			HemoData hdMatch = null;
			if (isLikelyFlowString(hd1.getName())) {
				hdMatch = hd1;
			} else if (isLikelyFlowString(hd1.getName())) {
				hdMatch = hd2;
			}

			if (hdMatch != null) {
				Trace match = null;
				for (Trace trace : traces) {
					if (trace.hd.equals(hdMatch) && !trace.isAlign) {
						if (match == null) {
							match = trace;
						} else {
							match = null;
							break;
						}
					}
				}

				if (match != null) {
					match.selectedDefault = true;
				}
			}
		}

		return traces;

	}

	/**
	 * Checks if a given string suggests it represents a pressure measurement.
	 *
	 * @param input The string to check (e.g., a header or file name).
	 * @return {@code true} if the string contains "pressure" or "press",
	 *         case-insensitive.
	 */
	private boolean isLikelyPressureString(String input) {

		return (input.toLowerCase().contains("pressure") || input.toLowerCase().contains("press"));
	}

	/**
	 * Checks if a given string suggests it represents a flow or velocity
	 * measurement.
	 *
	 * @param input The string to check (e.g., a header or file name).
	 * @return {@code true} if the string contains "flow" or "velocity",
	 *         case-insensitive.
	 */
	private boolean isLikelyFlowString(String input) {

		return (input.toLowerCase().contains("flow") || input.toLowerCase().contains("velocity"));
	}

	/**
	 * Makes the dialog visible and positions it relative to parent component (if
	 * provided at time of creation of this GUI)
	 */
	public void display() {
		setLocationRelativeTo(componentParent);
		setVisible(true);
	}

	/**
	 * Exit the jdialog and return to the calling frame
	 */
	private void discard() {
		setVisible(false);
		dispose();
	}

	/**
	 * Retrieves the results of the alignment process.
	 * 
	 * @return An {@link AlignResult} object containing the ensembled beats, or
	 *         {@code null} if the user cancelled the operation or did not provide
	 *         adequate input.
	 */
	public AlignResult getResult() {
		return this.alignResult;
	}

	/**
	 * Gets all beat selections made by the user in the alignment panel.
	 *
	 * @return A set of {@link BeatSelection} objects.
	 */
	public Set<BeatSelection> getBeatSelections() {
		return pnlDisplay.getAllSelections();
	}

	/**
	 * Attempts to group the currently selected beats in the top and bottom graphs
	 * into a single {@link BeatSelection}. If successful, the new selection is
	 * added to the selections table and the current beat counts are reset.
	 */
	public void addSelection() {
		BeatSelection bs = pnlDisplay.attemptBeatGrouping();
		if (bs != null) {
			table.addSelection(bs);
			setNumBeatsTop(0);
			setNumBeatsBottom(0);
		}
	}

	/**
	 * Performed if key press is triggered on the {@link AlignChartPanel}. This is a
	 * callback method that invokes the general {@link #addSelection()} method.
	 */
	@Override
	public void triggerAddSelection() {
		addSelection();
	}

	/**
	 * Callback method to programmatically click the 'Lock Alignment' checkbox.
	 */
	@Override
	public void triggerLockToggle() {
		chLockSamples.doClick();

	}

	/**
	 * Sets the displayed number of currently selected beats in the top graph. This
	 * method is a callback from the {@link AlignChartPanel}.
	 * 
	 * @param numberOfBeats The number of beats to display.
	 */
	@Override
	public void setNumBeatsTop(int numberOfBeats) {
		this.txtBeatsTop.setText(numberOfBeats + "");

	}

	/**
	 * Sets the displayed number of currently selected beats in the bottom graph.
	 * This method is a callback from the {@link AlignChartPanel}.
	 * 
	 * @param numberOfBeats The number of beats to display.
	 */
	@Override
	public void setNumBeatsBottom(int numberOfBeats) {
		this.txtBeatsBott.setText(numberOfBeats + "");
	}

	/**
	 * Called if a {@link BeatSelection} is removed from the selection table. Then
	 * makes sure that the appropriate annotations on the {@link AlignChartPanel}
	 * are removed.
	 */
	@Override
	public void tableSelectionRemoved(BeatSelection selection) {
		pnlDisplay.removeBeatsSelection(selection);
	}

	/**
	 * Parses a user-entered time value into seconds. Integer number of seconds is
	 * returned.
	 */
	private static int parseTimeToSeconds(String time) {
		Pattern pattern = Pattern.compile("^(?:(\\d+):)?(\\d{1,2}):(\\d{1,2})$");
		Matcher matcher = pattern.matcher(time);

		if (matcher.matches()) {
			int hours = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
			int minutes = Integer.parseInt(matcher.group(2));
			int seconds = Integer.parseInt(matcher.group(3));

			return hours * 3600 + minutes * 60 + seconds;
		} else {
			throw new IllegalArgumentException("Invalid time format. Expected HH:MM:SS or MM:SS.");
		}
	}

	/**
	 * A container for the results of the alignment process. It holds the ensembled
	 * beats created from the user's selections and provides access to associated
	 * data like screenshots.
	 */
	public static class AlignResult {

		/**
		 * Map where each key is an ensembled {@link Beat} and the value is a list of
		 * base64-encoded strings representing screenshots of the original beat
		 * selections. Name of the beat selection can be acquired from the {@link Beat}.
		 */
		private Map<Beat, List<String>> ensembledBeats = new LinkedHashMap<Beat, List<String>>();

		/**
		 * Create an alignment result. It processes the user's beat selections to
		 * generate the final ensembled beats.
		 *
		 * @param beatSelections The set of beat selections made by the user.
		 * @param ensembleType   The ensembling method to use (e.g.,
		 *                       {@link HemoData#ENSEMBLE_TRIM}).
		 */
		private AlignResult(Set<BeatSelection> beatSelections, int ensembleType) {

			// Go through each selection
			for (BeatSelection beatSelection : beatSelections) {
				List<Beat> allBeats = beatSelection.getBeats();
				Beat ensembledBeat = Beat.ensembleFlowPressure(allBeats, ensembleType, beatSelection.getName());

				List<String> beatScreenImages = beatSelection.getBeatImages();

				ensembledBeats.put(ensembledBeat, beatScreenImages);

			}

		}

		/**
		 * Gets the list of final, ensembled beats.
		 *
		 * @return A list of {@link Beat} objects.
		 */
		public List<Beat> getBeats() {
			return new ArrayList<Beat>(ensembledBeats.keySet());
		}

		/**
		 * Gets the screenshots associated with the selection that created a given
		 * ensembled beat.
		 *
		 * @param beat The ensembled {@link Beat} for which to retrieve images.
		 * @return A list of base64-encoded image strings, or null if not found.
		 */
		public List<String> getBeatImages(Beat beat) {
			return ensembledBeats.get(beat);
		}

		/**
		 * Removes an ensembled beat and its associated images from the result.
		 *
		 * @param beat The {@link Beat} to remove.
		 */
		public void removeBeat(Beat beat) {
			ensembledBeats.remove(beat);
		}

	}

	/**
	 * A helper class representing a single data trace from a {@link HemoData} file.
	 * It bundles the data source, the specific header for the trace, and metadata
	 * like whether it's an alignment trace or a default selection.
	 */
	public static class Trace {

		/** The source {@link HemoData} object for this trace. */
		private final HemoData hd;
		/** The specific {@link Header} defining this trace within the {@link HemoData} object. */
		private final Header header;
		/** Flag indicating if this is an alignment trace (e.g., EKG). */
		private final boolean isAlign;
		/** Flag indicating if this trace should be selected by default in the UI. */
		private boolean selectedDefault = false;

		/** The display name for this trace, shown in UI components. */
		private final String name;

		/**
		 * Constructs a {@link Trace} from {@link HemoData} object and a specific {@link Header}.
		 *
		 * @param hd The source {@link HemoData}.
		 * @param header The {@link Header} that identifies this trace.
		 */
		private Trace(HemoData hd, Header header) {
			this.hd = hd;
			this.header = header;
			this.isAlign = hd.hasFlag(header, HemoData.OTHER_ALIGN);
			name = "\"" + hd.getFileName() + "\": " + header.getName();
		}

		/**
		 * Constructs a dummy Trace object with only a name. Used for placeholder
		 * items in JComboBox controls, such as "Select trace...".
		 *
		 * @param name The display name for the dummy trace.
		 */
		private Trace(String name) {
			hd = null;
			header = null;
			isAlign = false;
			this.name = name;
		}

		/**
		 * Determines if the trace is likely a pressure measurement based on its name
		 * based on its {@link Header} name.
		 *
		 * @return {@code true} if the name contains "pressure" or "press".
		 */
		private boolean isLikelyPressure() {

			String headerName = header.getName();
			return (headerName.toLowerCase().contains("pressure") || headerName.toLowerCase().contains("press"));

		}

		/**
		 * Determines if the trace is likely a flow measurement based on its name
		 * based on its {@link Header} name.
		 *
		 * @return {@code true} if the name contains "flow", "velocity", or "doppler"
		 */
		private boolean isLikelyFlow() {

			String headerName = header.getName();
			return (headerName.toLowerCase().contains("flow") || headerName.toLowerCase().contains("velocity")
					|| headerName.toLowerCase().contains("doppler"));

		}

		/**
		 * Provides the string representation of the trace, which is its display name.
		 *
		 * @return The name of the trace.
		 */
		public String toString() {
			return name;
		}

	}

}