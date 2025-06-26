package com.carrington.WIA.GUIs;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.apache.commons.lang3.ArrayUtils;

import com.carrington.WIA.Utils;
import com.carrington.WIA.IO.Header;

import java.awt.Color;
import java.awt.Component;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JComboBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class SheetOptionsSelectionGUI extends JDialog {

	private static final long serialVersionUID = -7538705833329832549L;
	private final JPanel contentPanel = new JPanel();
	private JTextField txtName;
	private JComboBox<Header> cbSelectCategory;
	private JList<Header> listCategories;
	private OptionSelections options = null;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			Header header1 = new Header("time", 0, true);
			Header header2 = new Header("pressure", 1, false);
			Header header3 = new Header("EKG", 2, false);

			SheetOptionsSelectionGUI dialog = new SheetOptionsSelectionGUI("Testing",
					Arrays.asList(header1, header2, header3), new ArrayList<Header>(), header3, null);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public SheetOptionsSelectionGUI(String name, List<Header> headers, List<Header> headersToExclude,
			Header defaultForAlign, Component parent) {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.setModalityType(ModalityType.APPLICATION_MODAL); // may need to edit
		Font smallTextFont = Utils.getTextFont(true);
		Font smallTextFontPlain = Utils.getTextFont(false);

		setTitle("Dataset Properties");
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel pnlTop = new JPanel();
		pnlTop.setBorder(new LineBorder(new Color(0, 0, 0)));
		JLabel lblName = new JLabel("Series Name:");
		lblName.setFont(smallTextFont);
		txtName = new JTextField();
		txtName.setColumns(10);
		txtName.setFont(smallTextFontPlain);
		if (name != null) {
			txtName.setText(name);
		}
		JLabel lblCat = new JLabel("<html><b>Select Categories:<br></b><i>(Ctrl/Shift to select<br>multiple.)</html>");
		lblCat.setFont(smallTextFontPlain);

		JScrollPane scrollPane = new JScrollPane();

		JLabel lblCatAlign = new JLabel("<html>Selection category<br> used for aligning:</html>");
		lblCatAlign.setFont(smallTextFont);

		lblCatAlign.setFont(smallTextFont);

		cbSelectCategory = new JComboBox<Header>();
		for (Header header : headers) {
			cbSelectCategory.addItem(header);
		}
		cbSelectCategory.setEditable(false);
		cbSelectCategory.setSelectedItem(defaultForAlign);

		GroupLayout gl_pnlTop = new GroupLayout(pnlTop);
		gl_pnlTop.setHorizontalGroup(gl_pnlTop.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_pnlTop.createSequentialGroup().addContainerGap()
						.addGroup(gl_pnlTop.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_pnlTop.createSequentialGroup()
										.addComponent(lblName, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED).addComponent(txtName,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
								.addGroup(gl_pnlTop.createSequentialGroup()
										.addComponent(lblCat, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED).addComponent(scrollPane,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
								.addGroup(gl_pnlTop.createSequentialGroup()
										.addComponent(lblCatAlign, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED).addComponent(cbSelectCategory,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)))
						.addContainerGap()));
		gl_pnlTop.setVerticalGroup(gl_pnlTop.createParallelGroup(Alignment.LEADING).addGroup(gl_pnlTop
				.createSequentialGroup().addContainerGap()
				.addGroup(gl_pnlTop.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblName, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(txtName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.UNRELATED)
				.addGroup(gl_pnlTop.createParallelGroup(Alignment.BASELINE).addComponent(lblCat).addComponent(
						scrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
				.addPreferredGap(ComponentPlacement.UNRELATED)
				.addGroup(gl_pnlTop.createParallelGroup(Alignment.TRAILING)
						.addComponent(lblCatAlign, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(cbSelectCategory, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE))
				.addContainerGap()));

		listCategories = new JList<Header>();
		listCategories.setEnabled(true);
		listCategories.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		List<Integer> indices = new ArrayList<Integer>();
		for (int i = 0; i < headers.size(); i++) {
			if (!headersToExclude.contains(headers.get(i))) {
				indices.add(i);
			}
		}
		listCategories.setListData(headers.toArray(new Header[0]));
		listCategories.setSelectedIndices(ArrayUtils.toPrimitive(indices.toArray(new Integer[0])));
		listCategories.setCellRenderer(new ListCellRenderer<Header>() {

			@Override
			public Component getListCellRendererComponent(JList<? extends Header> list, Header value, int index,
					boolean isSelected, boolean cellHasFocus) {
				JLabel label = new JLabel(value.toString());
				label.setOpaque(true);
				label.setFont(smallTextFontPlain);

				if (isSelected) {
					// label.setBackground(Color.red);
					label.setBackground(list.getSelectionBackground());
					label.setForeground(list.getSelectionForeground());
				} else {
					label.setBackground(list.getBackground());
					label.setForeground(list.getForeground());
				}
				if (value.isPrimaryX()) {
					label.setForeground(Color.BLUE);
				}
				return label;
			}

		});

		listCategories.setFocusable(true);
		scrollPane.setFocusable(true);
		scrollPane.setViewportView(listCategories);
		pnlTop.setLayout(gl_pnlTop);

		JPanel pnlButton = new JPanel();
		// buttonPane.setLayout(new GroupL(FlowLayout.RIGHT));

		pnlButton.setBorder(new EmptyBorder(0, 5, 5, 5));

		JLabel lblError = new JLabel("Error (needs name, ≥ 2 columns of which Align and x-axis should be one.)");
		lblError.setFont(smallTextFontPlain);
		lblError.setForeground(new Color(0,0,0,0));
		JButton btnOK = new JButton("OK");
		btnOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!validateSelections()) {
					lblError.setForeground(Color.RED);
				} else {
					dispose();

				}
			}
		});

		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		btnCancel.setFont(smallTextFont);
		btnOK.setFont(smallTextFont);
		btnOK.setActionCommand("OK");

		GroupLayout gl_pnlButton = new GroupLayout(pnlButton);
		gl_pnlButton.setHorizontalGroup(gl_pnlButton.createSequentialGroup().addContainerGap().addComponent(lblError)
				.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(btnCancel)
				.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnOK).addContainerGap());
		gl_pnlButton.setVerticalGroup(gl_pnlButton.createParallelGroup(Alignment.CENTER).addComponent(lblError)
				.addComponent(btnCancel).addComponent(btnOK));


		getRootPane().setDefaultButton(btnOK);
		
		GroupLayout gl_main = new GroupLayout(getContentPane());
		gl_main.setVerticalGroup(gl_main.createSequentialGroup().addContainerGap()
				.addComponent(pnlTop, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(pnlButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addContainerGap());
		gl_main.setHorizontalGroup(gl_main.createSequentialGroup()
				.addContainerGap()
				.addGroup(gl_main.createParallelGroup(Alignment.CENTER)
						.addComponent(pnlTop)
						.addComponent(pnlButton))
				.addContainerGap());
		getContentPane().setLayout(gl_main);
		
		pack();
		setMinimumSize(getMinimumSize());

		setLocationRelativeTo(parent);
		
		SwingUtilities.invokeLater(() -> {
		    txtName.requestFocusInWindow();
		    txtName.selectAll();
		});

	}

	public OptionSelections getOptionSelections() {
		return this.options;
	}

	private boolean validateSelections() {

		String name = this.txtName.getText();
		if (name == null || name.isBlank())
			return false;

		List<Header> selectedHeaders = new ArrayList<Header>(this.listCategories.getSelectedValuesList());
		if (selectedHeaders.size() < 2) {
			return false;
		} else {
			boolean xPrimaryFound = false;
			for (Header header : selectedHeaders) {
				if (header.isPrimaryX()) {
					xPrimaryFound = true;
					break;
				}
			}
			if (!xPrimaryFound)
				return false;
		}

		Object alignHeaderObj = this.cbSelectCategory.getSelectedItem();
		if (alignHeaderObj == null)
			return false;
		Header alignHeader = (Header) alignHeaderObj;

		if (!selectedHeaders.contains(alignHeader)) {
			return false;
		}
		this.options = new OptionSelections(name, selectedHeaders, alignHeader);
		return true;

	}

	class OptionSelections {

		public final String name;
		public final List<Header> selectedHeaders;
		public final Header headerForAlign;

		private OptionSelections(String name, List<Header> selectedHeaders, Header headerForAlign) {
			this.name = name;
			this.selectedHeaders = selectedHeaders;
			this.headerForAlign = headerForAlign;
		}

	}

}
