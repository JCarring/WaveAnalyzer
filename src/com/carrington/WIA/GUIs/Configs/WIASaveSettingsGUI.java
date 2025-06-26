package com.carrington.WIA.GUIs.Configs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.carrington.WIA.Utils;
import com.carrington.WIA.GUIs.Components.JCDimension;
import com.carrington.WIA.GUIs.Components.JFontChooser;

public class WIASaveSettingsGUI extends JDialog {

	private static final long serialVersionUID = 3466535735939549617L;
	private final JPanel contentPanel = new JPanel();
	private JTextField txtHeightSave;
	private JTextField txtWidthSave;
	private JCheckBox chSaveSVGTIFF;
	private JCheckBox chSaveSelections;
	
	public WIASaveSettingsChoices settings;

	public Font tentativeFont = null;
	private JTextField txtCurrFont;
	
	private WeakReference<WIASaveSettingsGUI> ref = new WeakReference<WIASaveSettingsGUI>(this);

	
	
	/**
	 * Create the dialog.
	 */
	public WIASaveSettingsGUI(WIASaveSettingsChoices settings) {
		this.settings = settings;
		setBounds(100, 100, 550, 500);
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);

		JLabel lblSaveSettings = new JLabel("Save Settings");

		chSaveSVGTIFF = new JCheckBox(
				"<html><b>Save SVG & TIFF</b> (preserves pixels, convertible to all common file formats at future point)</html>");

		JLabel lblHeight2 = new JLabel("Height:");

		txtHeightSave = new JTextField();
		txtHeightSave.setColumns(10);

		JLabel lblPx3 = new JLabel("px");

		JLabel lblWidth2 = new JLabel("Width:");

		txtWidthSave = new JTextField();
		txtWidthSave.setColumns(10);

		JLabel lblPx4 = new JLabel("px");

		chSaveSelections = new JCheckBox(
				"<html><b>Save Image of Selections</b> (uses current display dimensions)</html>");

		JLabel lblFontSettings = new JLabel("Font Settings");

		JButton btnPickFont = new JButton("Pick font");
		btnPickFont.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setAlwaysOnTop(false);
				JFontChooser chooser = new JFontChooser();
				int choice = chooser.showDialog(ref.get());
				
				if (choice == JFontChooser.OK_OPTION) {
					
					Font font = chooser.getSelectedFont();

					String weightString = "Plain";
					
					switch (font.getStyle()) {
					case Font.PLAIN:
						weightString = "Plain";
						break;
					case Font.BOLD:
						weightString = "Bold";
						break;
					case Font.ITALIC:
						weightString = "Italic";
						break;
					}
					txtCurrFont.setText(font.getFamily() + ", " + weightString + ", size " + font.getSize());
					tentativeFont = font;
					settings.setChanged(true);
				}
				setAlwaysOnTop(true);

				
			}
		});
		

		JLabel lblFontWarning = new JLabel("Font sizes in 10-20 range are less likely to distort the graphs");
		lblFontWarning.setForeground(Color.RED);
		
		JLabel lblImgSize = new JLabel("Size Settings");
		
		txtCurrFont = new JTextField();
		txtCurrFont.setColumns(10);
		txtCurrFont.setEditable(false);
		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
						.addComponent(lblSaveSettings)
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addGap(6)
							.addComponent(chSaveSVGTIFF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addContainerGap()
							.addComponent(lblFontSettings)
							.addGap(134))
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addContainerGap()
							.addComponent(chSaveSelections, GroupLayout.PREFERRED_SIZE, 561, Short.MAX_VALUE))
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addGap(20)
							.addComponent(lblHeight2)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(txtHeightSave, GroupLayout.PREFERRED_SIZE, 44, GroupLayout.PREFERRED_SIZE)
							.addComponent(lblPx3)
							.addGap(30)
							.addComponent(lblWidth2)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(txtWidthSave, GroupLayout.PREFERRED_SIZE, 44, GroupLayout.PREFERRED_SIZE)
							.addComponent(lblPx4))
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addContainerGap()
							.addComponent(lblImgSize))
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addContainerGap()
							.addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING, false)
								.addGroup(gl_contentPanel.createSequentialGroup()
									.addGap(6)
									.addComponent(lblFontWarning))
								.addGroup(gl_contentPanel.createSequentialGroup()
									.addComponent(btnPickFont)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(txtCurrFont)))))
					.addContainerGap())
		);
		gl_contentPanel.setVerticalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addComponent(lblSaveSettings)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(chSaveSVGTIFF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
					
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(chSaveSelections)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(lblImgSize)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblHeight2)
						.addComponent(txtHeightSave, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblPx3)
						.addComponent(lblWidth2)
						.addComponent(txtWidthSave, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblPx4))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(lblFontSettings)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(btnPickFont)
						.addComponent(txtCurrFont, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(lblFontWarning)
					.addGap(27))
		);
		contentPanel.setLayout(gl_contentPanel);
		
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton btnOK = new JButton("OK");
				btnOK.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						if (!validateAndStoreChoices()) {
							return;
						}
						
						close();
					}
					
				});
				buttonPane.add(btnOK);
				getRootPane().setDefaultButton(btnOK);
			}
			{
				JButton btnCancel = new JButton("Cancel");
				btnCancel.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						close();
					}
					
				});
				buttonPane.add(btnCancel);
			}
		
		
		lblSaveSettings.setFont(Utils.getSubTitleFont());
		Utils.removePaintFocus(contentPanel);
		Utils.setFont(Utils.getSmallTextFont(), lblFontWarning);
		Utils.setFont(Utils.getSubTitleSubFont(), lblFontSettings, lblImgSize);
		Utils.setFont(Utils.getTextFont(false), lblPx3, lblPx4, txtWidthSave, txtHeightSave, lblWidth2, lblHeight2,
				txtCurrFont, chSaveSVGTIFF, chSaveSelections);
		contentPanel.setSize(contentPanel.getPreferredSize());
		setSize(new Dimension(contentPanel.getPreferredSize().width, this.getPreferredSize().height + buttonPane.getPreferredSize().height));
		//setBounds(100, 100, this.getPreferredSize().width, this.getPreferredSize().height);
		setResizable(false);
		setAlwaysOnTop(true);

	}
	
	public void open(Component relative) {

		setLocationRelativeTo(relative);
		setModal(true);
		displayDefaults();
		setVisible(true);
	}
	
	public void close() {
		setVisible(false);
	}
	
	public void displayDefaults() {
		this.chSaveSVGTIFF.setSelected(settings.getSaveSVGTIFF());
		this.chSaveSelections.setSelected(settings.getSaveSelections());

		this.txtHeightSave.setText(settings.getSaveDimensions().getHeight() + "");
		this.txtWidthSave.setText(settings.getSaveDimensions().getWidth() + "");
		
		
		Font saveFont = settings.getSaveFont();
		String weightString = "Plain";
		
		switch (saveFont.getStyle()) {
		case Font.PLAIN:
			weightString = "Plain";
			break;
		case Font.BOLD:
			weightString = "Bold";
			break;
		case Font.ITALIC:
			weightString = "Italic";
			break;
		}
		this.txtCurrFont.setText(saveFont.getFamily() + ", " + weightString + ", size " + saveFont.getSize());
		

		
	}
	
	public WIASaveSettingsChoices getChoices() {
		return this.settings;
	}
	
	private boolean validateAndStoreChoices() {
		
		Integer saveWidth = parsePixelString(this.txtWidthSave.getText());
		Integer saveHeight = parsePixelString(this.txtHeightSave.getText());
		
		if (saveWidth == null || saveHeight == null) {
			Utils.showError("Error with pixel selection. Must be a positive integer, between 4 and 2000", this);
			return false;
		}
		settings.setChanged(true);
		settings.setSaveDimensions(new JCDimension(saveHeight, saveWidth));
		settings.setSaveSelections(chSaveSelections.isSelected());
		settings.setSaveSVGTIFF(chSaveSVGTIFF.isSelected());

		if (this.tentativeFont != null) {
			settings.setSaveFont(tentativeFont);
		}
		
		return true;
		
	}
	
	private Integer parsePixelString(String input) {
		if (input == null || input.isBlank()) {
			return null;
		}
		
		int num = -1;
		try {
			num = Integer.parseInt(input);
		} catch (Exception e) {
			return null;
		}
		
		if (num > 2000 || num < 4) {
			return null;
		}
		
		return num;
	}
	
	
	
}
