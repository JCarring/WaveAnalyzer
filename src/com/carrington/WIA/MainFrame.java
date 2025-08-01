package com.carrington.WIA;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Year;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;

import com.carrington.WIA.GUIs.BackListener;
import com.carrington.WIA.GUIs.CombowireGUI;
import com.carrington.WIA.GUIs.SeparateWireGUI;
import com.carrington.WIA.GUIs.WIAModifierGUI;
import com.carrington.WIA.GUIs.WIAStatsGUI;
import com.carrington.WIA.GUIs.Components.JCButton;
import com.carrington.WIA.GUIs.Components.JCHelpButton;
import com.carrington.WIA.IO.WIAResourceReader;

import net.miginfocom.swing.MigLayout;

/**
 * Main application frame for the WIA GUI.<br>
 * <br>
 * 
 * Creates buttons to navigate into the various functions.<br>
 * <br>
 * 
 * Implements {@link BackListener}, so that this {@link MainFrame} can be called
 * back into visibility if one of the program functions cancels.
 */
public class MainFrame implements BackListener {

	/** The main frame object */
	private JFrame frame;
	private volatile SeparateWireGUI guiSeparateWire = null;
	private volatile CombowireGUI guiComboWire = null;
	private volatile WIAStatsGUI guiStats = null;
	private volatile WIAModifierGUI guiModifyWIA = null;

	/**
	 * Launch the application.
	 * 
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			Utils.showMessage(Utils.ERROR, "<html>Unable to set the look and feel for progam. Fatal error.<br><br></html>" + e.getMessage(), null);
			e.printStackTrace();
			return;
		}
				
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {

					MainFrame window = new MainFrame();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainFrame() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		Font basicFontBold = Utils.getTextFont(true);
		Font subtitleFontBold = Utils.getSubTitleFont();
		frame = new JFrame();
		// frame.setResizable(false);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel pnlInstructionAnalyze = new JPanel();
		pnlInstructionAnalyze.setBackground(new Color(192, 192, 192));

		JPanel pnlAnalyze = new JPanel();
		pnlAnalyze.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		JPanel pnlInstructionOther = new JPanel();
		pnlInstructionOther.setBackground(new Color(192, 192, 192));

		JPanel pnlOtherActions = new JPanel();
		pnlOtherActions.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		JPanel pnlBottom = new JPanel();
		pnlBottom.setBackground(new Color(192, 192, 192));

		GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup().addContainerGap()
						.addGroup(groupLayout.createParallelGroup(Alignment.CENTER).addComponent(pnlInstructionAnalyze)
								.addComponent(pnlAnalyze).addComponent(pnlInstructionOther)
								.addComponent(pnlOtherActions).addComponent(pnlBottom))
						.addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup().addContainerGap()
						.addComponent(pnlInstructionAnalyze, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(pnlAnalyze)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(pnlInstructionOther, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(pnlOtherActions)
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(pnlBottom,
								GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						.addContainerGap()));

		// Set layouts for the new button panels
		pnlAnalyze.setLayout(new MigLayout("insets 10, al center center"));
		pnlOtherActions.setLayout(new MigLayout("insets 10, al center center"));

		// --- Analysis Buttons ---
		JButton btnSeparateWire = new JButton(
				"<html><body style='white-space: nowrap'><center><font color='red'>Separate</font> flow and pressure<br>sources (i.e. Doppler FloWire)</center></body></html>");
		btnSeparateWire.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				initiateSeparateWireGUI();
			}
		});
		btnSeparateWire.setFocusPainted(false);

		JButton btnComboWire = new JButton(
				"<html><body style='white-space: nowrap'><center><font color='red'>Same</font> flow and pressure<br>sources (i.e. ComboWire)</center></body></html>");

		btnComboWire.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				initiateComboWireGUI();
			}
		});
		btnComboWire.setFocusPainted(false);

		JButton btnWIAReAnalyze = new JButton(
				"<html><body style='white-space: nowrap'><center><font color='red'>Re-analyze</font> prior wave<br>profile</center></body></html>");
		btnWIAReAnalyze.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				initiateReAnalyzeGUI();
			}
		});
		btnWIAReAnalyze.setFocusPainted(false);

		// --- Other Action Buttons ---
		JButton btnStats = new JButton(
				"<html><body style='white-space: nowrap'><center>Run statistics</center></body></html>");
		btnStats.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				initiateStatsGUI();
			}
		});
		btnStats.setFocusPainted(false);

		// Set button properties

		btnComboWire.setPreferredSize(
				new Dimension(btnComboWire.getPreferredSize().width, btnSeparateWire.getPreferredSize().height));
		btnComboWire.setFont(basicFontBold);
		btnComboWire.setMargin(new Insets(5, 5, 5, 5));
		btnSeparateWire.setFont(basicFontBold);
		btnSeparateWire.setMargin(new Insets(5, 5, 5, 5));
		btnStats.setFont(basicFontBold);
		btnStats.setMargin(new Insets(5, 5, 5, 5));
		btnWIAReAnalyze.setFont(basicFontBold);
		btnWIAReAnalyze.setMargin(new Insets(5, 5, 5, 5));

		// Add buttons to their respective panels

		JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
		sep.setForeground(Color.GRAY);
		sep.setBackground(Color.GRAY);
		JPanel thickSep = new JPanel();
		thickSep.setBackground(Color.GRAY); // or whatever color

		pnlAnalyze.add(btnSeparateWire, "sgx"); // sgx is a size group for same width
		pnlAnalyze.add(btnComboWire, "sgx, gapleft 10, wrap"); // wrap to end the row

		pnlAnalyze.add(thickSep, "growx, height 2!, span, gaptop 5, gapbottom 5, wrap");

		pnlAnalyze.add(btnWIAReAnalyze, "span, align center");

		// Add components to the other actions panel
		pnlOtherActions.add(btnStats, "alignx center, aligny center");

		// --- Top Panel Content ---
		JLabel lblInstructions = new JLabel("Run wave intensity analysis");
		lblInstructions.setFont(subtitleFontBold);

		JCHelpButton btnHelp = new JCHelpButton(WIAResourceReader.getContents(WIAResourceReader.HELP_MAIN));
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Utils.showMessage(Utils.INFO, btnHelp.getHelpMessage(), frame);
			}
		});

		pnlInstructionAnalyze.add(lblInstructions);
		pnlInstructionAnalyze.add(btnHelp);

		// --- Separator Panel Content ---
		JLabel lblSeparator = new JLabel("Statistics");
		lblSeparator.setFont(subtitleFontBold);
		pnlInstructionOther.add(lblSeparator);

		// --- Bottom Panel Content ---
		pnlBottom.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 5));

		JButton btnQuit = new JCButton("Exit", JCButton.BUTTON_QUIT);
		btnQuit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
				System.exit(0);
			}
		});
		
		JLabel lblCopy = new JLabel("<html>&copy; " + Year.now().getValue() + " Justin Carrington, MD  |  <a href=\"\">GitHub</a></html>");
		lblCopy.setCursor(new Cursor(Cursor.HAND_CURSOR));
		lblCopy.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(new URI("https://github.com/JCarring/WaveAnalyzer"));
				} catch (IOException | URISyntaxException ex) {
					Utils.showMessage(Utils.ERROR, "Could not open", frame);
				}
			}
		});
		
		pnlBottom.add(lblCopy);
		pnlBottom.add(btnQuit);
		frame.getContentPane().setLayout(groupLayout);

		// Set fonts for all components
		Utils.setFont(Utils.getSubTitleSubFont(), lblInstructions, lblSeparator);
		Utils.setFont(Utils.getTextFont(true), btnComboWire, btnSeparateWire, btnStats, btnWIAReAnalyze);
		Utils.setFont(Utils.getTextFont(false), lblCopy);
		
		frame.pack();
		frame.setSize(frame.getMinimumSize());
		frame.setLocationRelativeTo(null);

	}

	/**
	 * Initializes the GUI for separate wires, and then opens that frame
	 */
	public synchronized void initiateSeparateWireGUI() {
		
		try {
			this.guiSeparateWire = new SeparateWireGUI(this.frame, this);
			this.guiSeparateWire.navigateInto();
		} catch (IOException e) {
			Utils.showMessage(Utils.ERROR, "<html>Error opening:<br><br>" + e.getMessage() + "</html>", frame);
		}
		
	}

	/**
	 * Initializes the GUI for combo wires, and then opens that frame
	 */
	public synchronized void initiateComboWireGUI() {
		try {
			this.guiComboWire = new CombowireGUI(this.frame, this);
			this.guiComboWire.navigateInto();
		} catch (IOException e) {
			Utils.showMessage(Utils.ERROR, "<html>Error opening:<br><br>" + e.getMessage() + "</html>", frame);
		}
	}

	/**
	 * Initializes the GUI for stats, and then opens that frame
	 */
	public synchronized void initiateStatsGUI() {
		this.guiStats = new WIAStatsGUI(this.frame, this);
		this.guiStats.navigateInto();
	}

	/**
	 * Initializes the GUI for combo wires, and then opens that frame
	 */
	public synchronized void initiateReAnalyzeGUI() {
		this.guiModifyWIA = new WIAModifierGUI(this.frame, this);
		this.guiModifyWIA.navigateInto();
	}

	/**
	 * Is called when one of the frames opened by this {@link MainFrame} is closed
	 * by user.
	 */
	@Override
	public synchronized void wentBack() {
		if (guiSeparateWire != null) {
			guiSeparateWire.dispose();
			guiSeparateWire = null;
		}

		if (guiComboWire != null) {
			guiComboWire.dispose();
			guiComboWire = null;
		}

		if (guiStats != null) {
			guiStats.dispose();
			guiStats = null;
		}

		if (guiModifyWIA != null) {
			guiModifyWIA.dispose();
			guiModifyWIA = null;
		}
		frame.setVisible(true);

	}
}
