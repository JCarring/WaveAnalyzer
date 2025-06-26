package com.carrington.WIA;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;

import com.carrington.WIA.GUIs.BackListener;
import com.carrington.WIA.GUIs.CombowireGUI;
import com.carrington.WIA.GUIs.SeparateWireGUI;
import com.carrington.WIA.GUIs.WIAModifierGUI;
import com.carrington.WIA.GUIs.WIAStatsGUI;
import com.carrington.WIA.GUIs.Components.JCButton;

import net.miginfocom.swing.MigLayout;

/**
 * Main application frame for the WIA GUI.<br><br>
 * 
 * Creates buttons to navigate into the various functions.<br><br>
 * 
 * Implements {@link BackListener}, so that this {@link MainFrame} can be called back into visibility of one of the 
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
			Utils.showError("Unable to set the look and feel for progam. Fatal error.", null);
			e.printStackTrace();
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
		int size = basicFontBold.getSize();
		Font subtitleFontBold = Utils.getSubTitleFont();
		frame = new JFrame();
		frame.setResizable(false);
		frame.setBounds(100, 100, (int) (35 * size), (int) (25 * size));
		frame.setLocationRelativeTo(null);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel pnlTop = new JPanel();
		pnlTop.setBorder(new LineBorder(new Color(0, 0, 0)));
		pnlTop.setBackground(new Color(192, 192, 192));

		JPanel pnlMiddle = new JPanel();
		pnlMiddle.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		JPanel pnlBottom = new JPanel();
		pnlBottom.setBackground(new Color(192, 192, 192));
		pnlBottom.setBorder(new LineBorder(new Color(0, 0, 0)));
		GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(groupLayout
				.createSequentialGroup().addContainerGap()
				.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(pnlTop, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(pnlMiddle, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(pnlBottom, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(groupLayout
				.createSequentialGroup().addContainerGap()
				.addComponent(pnlTop, GroupLayout.PREFERRED_SIZE, (int) (size * 2.5), GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(pnlMiddle, GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(pnlBottom, GroupLayout.PREFERRED_SIZE, (int) (size * 3.0), GroupLayout.PREFERRED_SIZE)
				.addContainerGap()));
		pnlMiddle.setLayout(new MigLayout("al center center", "[]20[]", ""));

		JButton btnSeparateWire = new JButton(
				"<html><center>Analyze<br/>Separate Wire</center></html>");
		btnSeparateWire.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				initiateSeparateWireGUI();
			}
		});
		btnSeparateWire.setFocusPainted(false);

		JButton btnComboWire = new JButton("<html><center>Analyze<br/>Combination Wire</center></html>");
		btnComboWire.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				initiateComboWireGUI();
			}
		});
		btnComboWire.setFocusPainted(false);
		
		JButton btnStats = new JButton("<html><center>Run<br/>WIA<br/>Statistics</center></html>");
		btnStats.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				initiateStatsGUI();
			}
		});
		btnStats.setFocusPainted(false);
		
		JButton btnWIAReAnalyze = new JButton("<html><center>Re-analyze Prior<br/>WIA File</center></html>");
		btnWIAReAnalyze.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				initiateReAnalyzeGUI();
			}
		});
		btnWIAReAnalyze.setFocusPainted(false);
		
		btnComboWire.setPreferredSize(
				new Dimension(btnComboWire.getPreferredSize().width, btnSeparateWire.getPreferredSize().height));
		btnComboWire.setFont(basicFontBold);
		btnComboWire.setFocusPainted(false);
		btnComboWire.setMargin(new Insets(5, 5, 5, 5));
		btnSeparateWire.setFont(basicFontBold);
		btnSeparateWire.setFocusPainted(false);
		btnSeparateWire.setMargin(new Insets(5, 5, 5, 5));
		btnStats.setFont(basicFontBold);
		btnStats.setFocusPainted(false);
		btnStats.setMargin(new Insets(5, 5, 5, 5));
		btnWIAReAnalyze.setFont(basicFontBold);
		btnWIAReAnalyze.setFocusPainted(false);
		btnWIAReAnalyze.setMargin(new Insets(5, 5, 5, 5));
		
		pnlMiddle.add(btnSeparateWire, "cell 0 0,alignx center,aligny center");
		pnlMiddle.add(btnComboWire, "cell 1 0, alignx center, aligny center");
		pnlMiddle.add(btnStats, "cell 2 0, alignx center, aligny center");
		pnlMiddle.add(btnWIAReAnalyze, "cell 3 0, alignx center, aligny center");

		JLabel lblInstructions = new JLabel("Please select an option below.");
		lblInstructions.setFont(subtitleFontBold);
		pnlTop.add(lblInstructions);
		pnlBottom.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JButton btnQuit = new JCButton("Quit", JCButton.BUTTON_QUIT);

		btnQuit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		pnlBottom.add(btnQuit);
		frame.getContentPane().setLayout(groupLayout);
		
		Utils.setFont(Utils.getSubTitleSubFont(), btnComboWire, btnSeparateWire);

	}

	/**
	 * Initializes the GUI for separate wires, and then opens that frame
	 */
	public synchronized void initiateSeparateWireGUI() {
		this.guiSeparateWire = new SeparateWireGUI(this.frame, this);
		this.guiSeparateWire.navigateInto();
	}

	/**
	 * Initializes the GUI for combo wires, and then opens that frame
	 */
	public synchronized void initiateComboWireGUI() {
		this.guiComboWire = new CombowireGUI(this.frame, this);
		this.guiComboWire.navigateInto();
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
