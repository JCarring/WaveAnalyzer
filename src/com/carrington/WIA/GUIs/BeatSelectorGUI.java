package com.carrington.WIA.GUIs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
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
import javax.swing.JSlider;
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
import com.carrington.WIA.GUIs.Components.BeatsSelectionTableCombo;
import com.carrington.WIA.GUIs.Components.BeatsSelectionTableCombo.SelectionTableListener;
import com.carrington.WIA.GUIs.Components.JCButton;
import com.carrington.WIA.GUIs.Components.JCLabel;
import com.carrington.WIA.Graph.AlignChartPanel;
import com.carrington.WIA.Graph.BeatsChartPanel;
import com.carrington.WIA.Graph.BeatsChartPanel.BeatsChartPanelListener;
import com.carrington.WIA.IO.Header;
import com.carrington.WIA.IO.HeaderResult;
import com.carrington.WIA.IO.SheetDataReader;
import com.carrington.WIA.Math.FlowUnit;
import com.carrington.WIA.Math.PressureUnit;

public class BeatSelectorGUI extends JDialog implements SelectionTableListener,BeatsChartPanelListener {

	private static final long serialVersionUID = -8183475955290511152L;
	private final JPanel contentPanel = new JPanel();

	private static final Color pnlTopColor = new Color(169, 169, 169);
	private static final Color pnlOtherColor = new Color(213, 213, 213);
	public static final Color purple = new Color(161, 0, 132, 255);


	private final WeakReference<BeatSelectorGUI> ref = new WeakReference<BeatSelectorGUI>(this);
	private BeatsChartPanel pnlDisplay;
	private JTextField txtBeats;
	private BeatsSelectionTableCombo table;
	private JComboBox<String> cbTraces;
	private JCheckBox chAutoR;
	private JCheckBox chAutoDetect;
	private JCheckBox chAutoBeat;
	private JComboBox<PressureUnit> cbUnitsPressure;
	private JComboBox<FlowUnit> cbUnitsFlow;
	private JCButton btnAddSel;
	private JCButton btnResetCurrSel;
	private JSlider sliderOverlap;
	
	private final Header headerPressure;
	private final Header headerFlow;
	
	private Component componentRelative;
	
	/**
	 * One of {@link HemoData#ENSEMBLE_TRIM} or {@link HemoData#ENSEMBLE_SCALE}
	 */
	@SuppressWarnings("unused")
	private final int trimSelection;
	private SelectionResult alignResult;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {

					File fileInput = new File("/Users/justincarrington/Documents/School/Residency/Research/WIA Project/ComboWire Comparison/Alexander combo 13118093/Combo/Combo_13118093.txt");
					// File fileSave = new
					// File("/Users/justincarrington/Downloads/AA/EnsembledOuptut.csv");

					SheetDataReader dataFandP = new SheetDataReader(fileInput, 1);
					HeaderResult hr = dataFandP.readHeaders();

					Header time = hr.headers.get(0);
					Header pressure = hr.headers.get(2);
					Header flow = hr.headers.get(4);
					Header ecg = hr.headers.get(3);
					Header rWave = hr.headers.get(6);

					List<Header> headersToPull = new ArrayList<Header>();
					headersToPull.add(time);
					headersToPull.add(pressure);
					headersToPull.add(flow);
					headersToPull.add(ecg);
					headersToPull.add(rWave);

					HemoData hdData = dataFandP.readData(headersToPull, null).data;
					hdData.addFlags(pressure, HemoData.TYPE_PRESSURE, HemoData.UNIT_MMHG); // will succeed or will
																								// throw an error
					hdData.addFlags(flow, HemoData.TYPE_FLOW, HemoData.UNIT_CMperS);
					hdData.addFlags(ecg, HemoData.TYPE_ECG);
					hdData.addFlags(rWave, HemoData.TYPE_R_WAVE);


					BeatSelectorGUI frame = new BeatSelectorGUI(hdData, true, HemoData.ENSEMBLE_TRIM, null);
					frame.setVisible(true);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the dialog.
	 * 
	 * @throws OutOfMemoryError if the {@link HemoData} is too large - may need to trim
	 * @throws IllegalArgumentException if there is not a flow and pressure trace contained in the hemo data
	 */
	@SuppressWarnings("serial")
	public BeatSelectorGUI(HemoData data, boolean syncRWave, int ensembleType, Component parent) throws OutOfMemoryError, IllegalArgumentException {
		
		this.componentRelative = parent;
		this.trimSelection = ensembleType;
		
		if (data == null)
			throw new IllegalArgumentException("Null inputs");
		
		if (data.isValid() != null) {
			throw new IllegalStateException("Invalid state of input data");
		}

		if (data.getHeaderByFlag(HemoData.TYPE_PRESSURE).isEmpty() || data.getHeaderByFlag(HemoData.TYPE_ECG).isEmpty()
				|| data.getHeaderByFlag(HemoData.TYPE_FLOW).isEmpty()) {
			throw new IllegalArgumentException(
					"For beat selection, pressure, flow, and ECG are the minimum requirements.");
		}
		
		headerPressure = data.getHeaderByFlag(HemoData.TYPE_PRESSURE).get(0);
		headerFlow = data.getHeaderByFlag(HemoData.TYPE_FLOW).get(0);

		// make sure we have flow and pressure traces
		
		setModal(true);
		setTitle("Pick Selections");
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
		pnlDisplay = new BeatsChartPanel(data, this, true, true, true, 100);

		pnlDisplay.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		JPanel pnlSelections = new JPanel();
		pnlSelections.setBackground(pnlOtherColor);
		pnlSelections.setBorder(new LineBorder(new Color(0, 0, 0)));

		JPanel pnlBottom = new JPanel();
		pnlBottom.setBackground(pnlOtherColor);
		pnlBottom.setBorder(new LineBorder(new Color(0, 0, 0)));
		FlowLayout flowLayout = (FlowLayout) pnlBottom.getLayout();
		flowLayout.setAlignment(FlowLayout.TRAILING);

		JLabel lblDisplay = new JLabel("Display");
		cbTraces = new JComboBox<String>(BeatsChartPanel.DISPLAY_TRACE_OPTIONS.toArray(new String[0]));
		pnlDisplay.showVisibleTraces(BeatsChartPanel.DISPLAY_TRACE_ALL);
		cbTraces.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				pnlDisplay.showVisibleTraces(cbTraces.getSelectedItem().toString());

			}

		});

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


		chAutoR = new JCheckBox("Snap to R");
		chAutoR.setOpaque(false);
		chAutoR.setFocusable(false);
		chAutoR.setSelected(syncRWave);
		chAutoR.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean selected = chAutoR.isSelected();
				pnlDisplay.setAutoSnapToR(selected);

				if (!selected) {
					chAutoBeat.setEnabled(false);
					chAutoDetect.setEnabled(false);
					pnlDisplay.setAutoBeat(false);
					pnlDisplay.setAutoDetectRWave(false);
				} else {
					chAutoBeat.setEnabled(true);
					pnlDisplay.setAutoBeat(chAutoBeat.isSelected());
					if (data.containsHeaderByFlag(HemoData.TYPE_R_WAVE)) {
						chAutoDetect.setEnabled(true);
						pnlDisplay.setAutoDetectRWave(chAutoDetect.isSelected());
					}

				}
			}
		});
		pnlDisplay.setAutoSnapToR(chAutoR.isSelected());

		chAutoBeat = new JCheckBox("Auto Beat");
		chAutoBeat.setOpaque(false);
		chAutoBeat.setFocusable(false);
		chAutoBeat.setSelected(true);
		chAutoBeat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pnlDisplay.setAutoBeat(chAutoBeat.isSelected());
			}
		});
		pnlDisplay.setAutoSnapToR(chAutoBeat.isSelected());
		
		chAutoDetect = new JCheckBox("Auto Detect");
		chAutoDetect.setToolTipText("If selected, detect R waves, otherwise use file.");
		chAutoDetect.setOpaque(false);
		chAutoDetect.setFocusable(false);
		chAutoDetect.setSelected(true);
		chAutoDetect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pnlDisplay.setAutoDetectRWave(chAutoDetect.isSelected());
			}
		});
		pnlDisplay.setAutoDetectRWave(chAutoDetect.isSelected());
		if (!data.containsHeaderByFlag(HemoData.TYPE_R_WAVE)) {
			chAutoDetect.setEnabled(false);
			chAutoDetect.setSelected(true);
			pnlDisplay.setAutoDetectRWave(true);
		} else {
			pnlDisplay.setAutoDetectRWave(chAutoDetect.isSelected());

		}

		JCButton btnFitTrace = new JCButton("Fit Trace (F)", JCButton.BUTTON_STANDARD);
		btnFitTrace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pnlDisplay.refresh();
			}
		});
		
		JCLabel lblPFOverlap = new JCLabel("Pressure / flow overlap:", JCLabel.LABEL_TEXT_PLAIN);
		
		sliderOverlap = new JSlider(0, 100, 100);
		sliderOverlap.setOpaque(false);
		sliderOverlap.setMajorTickSpacing(50);
		sliderOverlap.setPaintTicks(true);
		sliderOverlap.setPaintLabels(true);

		sliderOverlap.addChangeListener(e -> {
			if (!sliderOverlap.getValueIsAdjusting()) {
				int value = sliderOverlap.getValue();
				pnlDisplay.setOverlap(value);
			}
		});

        // Label table with custom markers
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JCLabel("None", JCLabel.LABEL_SMALL));
        labelTable.put(100, new JCLabel("Full", JCLabel.LABEL_SMALL));
        sliderOverlap.setLabelTable(labelTable);

		JLabel lblDataTypes = new JLabel("Units");

		JLabel lblPressure = new JLabel("Pressure:");
		cbUnitsPressure = new JComboBox<PressureUnit>();
		cbUnitsPressure.setEditable(false);
		cbUnitsPressure.addItem(PressureUnit.NEITHER);
		cbUnitsPressure.addItem(PressureUnit.MMHG);
		cbUnitsPressure.addItem(PressureUnit.PASCALS);
		cbUnitsPressure.setSelectedIndex(0);
		PressureUnit pressureUnit = Utils.determinePressureUnit(data.getYData(headerPressure));
		if (pressureUnit != null) {
			cbUnitsPressure.setSelectedIndex(Utils.getJComboBoxItemIndex(cbUnitsPressure, pressureUnit));
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

		
		
		JLabel lblFlow = new JLabel("Flow:");
		cbUnitsFlow = new JComboBox<FlowUnit>();
		cbUnitsFlow.setEditable(false);
		cbUnitsFlow.addItem(FlowUnit.NEITHER);
		cbUnitsFlow.addItem(FlowUnit.MPS);
		cbUnitsFlow.addItem(FlowUnit.CPS);
		cbUnitsFlow.setSelectedIndex(0);
		FlowUnit flowUnit = Utils.determineFlowUnit(data.getYData(headerFlow));
		if (flowUnit != null) {
			cbUnitsFlow.setSelectedIndex(Utils.getJComboBoxItemIndex(cbUnitsFlow, flowUnit));
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


		JLabel lblSelections = new JLabel("Selections");

		JLabel lblSelTime = new JLabel("# Beats:");

		txtBeats = new JTextField();
		txtBeats.setColumns(10);
		txtBeats.setFocusable(false);
		txtBeats.setEditable(false);


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
				setNumBeats(0);
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
				.addComponent(lblDataTypes, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
						Short.MAX_VALUE)
				.addGroup(Alignment.LEADING, gl_pnlSelections.createSequentialGroup().addGap(5)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING)
								.addComponent(lblPressure)
								.addComponent(lblFlow))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING)
								.addComponent(cbUnitsPressure, width2, width2, Short.MAX_VALUE)
								.addComponent(cbUnitsFlow, width2, width2, Short.MAX_VALUE)))	
				.addComponent(lblSelections, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
						Short.MAX_VALUE)
				.addGroup(Alignment.LEADING,
						gl_pnlSelections.createSequentialGroup().addGap(5)
								.addGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING)
										.addComponent(lblSelTime))
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING)
										.addComponent(txtBeats, GroupLayout.DEFAULT_SIZE, width, width)))
				.addGroup(Alignment.LEADING,
						gl_pnlSelections.createSequentialGroup().addGap(5).addComponent(btnAddSel)
								.addComponent(dummyLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
										Short.MAX_VALUE)
								.addComponent(btnResetCurrSel))
				.addGroup(Alignment.LEADING, gl_pnlSelections.createSequentialGroup().addGap(5)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING).addComponent(chShowAlignLines)
								.addComponent(cbTraces).addComponent(chAutoR)))
				.addGroup(Alignment.LEADING, gl_pnlSelections.createSequentialGroup().addGap(15)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING)
								.addComponent(chAutoBeat).addComponent(chAutoDetect)))
				.addGroup(Alignment.LEADING, gl_pnlSelections.createSequentialGroup().addGap(5).addComponent(btnFitTrace))
				.addGroup(gl_pnlSelections.createSequentialGroup().addGap(5).addComponent(scrSelections,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
				.addGroup(Alignment.LEADING, gl_pnlSelections.createSequentialGroup().addGap(5).addComponent(lblPFOverlap))
				.addGroup(Alignment.LEADING, gl_pnlSelections.createSequentialGroup().addGap(15).addComponent(sliderOverlap).addGap(15)))
				.addContainerGap());
		gl_pnlSelections.setVerticalGroup(gl_pnlSelections.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_pnlSelections.createSequentialGroup().addGap(5).addComponent(lblDisplay)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(cbTraces, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(chShowAlignLines)
						.addComponent(chAutoR)
						.addComponent(chAutoDetect)
						.addComponent(chAutoBeat)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(lblPFOverlap)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(sliderOverlap)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(btnFitTrace)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(lblDataTypes)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblPressure)
								.addComponent(cbUnitsPressure, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblFlow)
								.addComponent(cbUnitsFlow, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
										GroupLayout.PREFERRED_SIZE))
						
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(lblSelections)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.BASELINE).addComponent(lblSelTime)
								.addComponent(txtBeats, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_pnlSelections.createParallelGroup(Alignment.BASELINE).addComponent(btnAddSel)
								.addComponent(dummyLabel).addComponent(btnResetCurrSel))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(scrSelections, GroupLayout.PREFERRED_SIZE,
								Utils.getFontParams(Utils.getTextFont(false), null)[0] * 8, GroupLayout.PREFERRED_SIZE)
						.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		table = BeatsSelectionTableCombo.generate(this);
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

				
				PressureUnit pressureUnits = (PressureUnit) cbUnitsPressure.getSelectedItem();
				FlowUnit flowUnits = (FlowUnit) cbUnitsFlow.getSelectedItem();
				
				
				if (pressureUnits == PressureUnit.NEITHER || flowUnits == FlowUnit.NEITHER) {
					if (Utils.confirmAction("Confirm", "You need to select the pressure and trace units. Sure you want to exit?",
							ref.get())) {
						alignResult = null;
						discard();
						return;
					} else {
						return;
					}
				}
				
				// Make sure that all the Beats within BeatSelection have updated flags also
				for (BeatSelection selection : getBeatSelections()) {
					List<Beat> allBeats = selection.getBeats();
					for (Beat beat : allBeats) {
						if (beat.getData().hasHeader(headerPressure)) {
							beat.getData().addFlags(headerPressure, pressureUnits == PressureUnit.MMHG ? HemoData.UNIT_MMHG : HemoData.UNIT_PASCAL);

						}
						if (beat.getData().hasHeader(headerFlow)) {
							beat.getData().addFlags(headerFlow, flowUnits == FlowUnit.MPS ? HemoData.UNIT_MperS : HemoData.UNIT_CMperS);

						}
					}
					
				}
				
				alignResult = new SelectionResult(getBeatSelections(), ensembleType);

				discard();

			}
		});
		pnlBottom.add(btnAccept);

		JLabel lblTopInstruction = new JLabel("Hover over graph to make selections.");
		pnlTop.add(lblTopInstruction);

		JButton btnHelp = new JButton();
		btnHelp.setIcon(Utils.IconQuestionLarger);
		btnHelp.setRolloverIcon(Utils.IconQuestionLargerHover);
		btnHelp.setContentAreaFilled(false);
		btnHelp.setBorderPainted(false);
		btnHelp.setBorder(null);


		pnlTop.add(btnHelp);
		Utils.setFont(Utils.getSmallTextFont(), chAutoBeat, chAutoDetect);
		Utils.setFont(Utils.getTextFont(false), chShowAlignLines, cbTraces, chAutoR,
				lblSelTime, lblPressure, lblFlow, cbUnitsFlow, cbUnitsPressure);
		Utils.setFont(Utils.getSubTitleFont(), lblTopInstruction, lblDisplay, lblDataTypes, lblSelections);

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
		pnlDisplay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false),
				"panLeft");
		pnlDisplay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "panRight");
		pnlDisplay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false),
				"addSel");
		
		
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
		pnlDisplay.getActionMap().put("panRight", new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				if (Utils.doesTextFieldHaveFocus(ref.get())) {
					return;
				}
				pnlDisplay.panByPercent(0.05);;
			}
		});
		pnlDisplay.getActionMap().put("panLeft", new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				if (Utils.doesTextFieldHaveFocus(ref.get())) {
					return;
				}
				pnlDisplay.panByPercent(-0.05);;
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
		

		pnlDisplay.grabFocus();
		pnlDisplay.setFocusCycleRoot(true);

		setNumBeats(0);
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
		pnlDisplay.setFocusable(true);
		pnlDisplay.grabFocus();

		this.setSize(getMinimumSize());
		setLocationRelativeTo(null);

	}



	/**
	 * display the jdialog
	 */
	public void display() {
		setLocationRelativeTo(componentRelative);
		setVisible(true);
	}

	/**
	 * Exit the jdialog and return to the calling frame
	 */
	public void discard() {
		setVisible(false);
		dispose();
	}
	
	/**
	 * 
	 * @return if R wave sync is selected
	 */
	public boolean getRWaveSync() {
		return chAutoR.isSelected();
	}
	
	/**
	 * @return results from running this alignment, or null if user cancelled before adequate input was obtained
	 */
	public SelectionResult getResult() {
		return this.alignResult;
	}

	public Set<BeatSelection> getBeatSelections() {
		return pnlDisplay.getAllSelections();
	}

	public void addSelection() {
		BeatSelection bs = pnlDisplay.attemptBeatGrouping();
		if (bs != null) {
			table.addSelection(bs);
			setNumBeats(0);
		}
	}
	
	/**
	 * Performed if key press is triggered on the {@link AlignChartPanel}
	 */
	@Override 
	public void triggerAddSelection() {
		addSelection();
	}
	

	@Override
	public void setNumBeats(int numberOfBeats) {
		this.txtBeats.setText(numberOfBeats + "");

		
	}
	
	/**
	 * Called if a {@link BeatSelection} is removed from the selection table. Then makes sure
	 * that the appropriate annotations on the {@link AlignChartPanel} are removed.
	 */
	@Override
	public void tableSelectionRemoved(BeatSelection selection) {
		pnlDisplay.removeBeatsSelection(selection);
	}

	/**
	 * Parses a user-entered time value into seconds. Integer number of seconds is returned.
	 */
	public static int parseTimeToSeconds(String time) {
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
	
	public static class SelectionResult {
				
		/**
		 * Name of the beat selection can be acquired from the {@link Beat}
		 */
		private Map<Beat, List<String>> ensembledBeats = new LinkedHashMap<Beat, List<String>>();
		
		private SelectionResult(Set<BeatSelection> beatSelections, int ensembleType) {
			
			// Go through each selection
			for (BeatSelection beatSelection : beatSelections) {
				List<Beat> allBeats = beatSelection.getBeats();
				Beat ensembledBeat = Beat.ensembleFlowPressure(allBeats, ensembleType, beatSelection.getName());
				
				List<String> beatScreenImages = beatSelection.getBeatImages();

				
				ensembledBeats.put(ensembledBeat, beatScreenImages);
				
			}
			
		}
		
		public List<Beat> getBeats() {
			return new ArrayList<Beat>(ensembledBeats.keySet());
		}
		
		public List<String> getBeatImages(Beat beat) {
			return ensembledBeats.get(beat);
		}
		
		public void removeBeat(Beat beat) {
			ensembledBeats.remove(beat);
		}
		
		
	}
	


}