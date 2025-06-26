package com.carrington.WIA.GUIs.Components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import com.carrington.WIA.Utils;

import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JScrollPane;
import javax.swing.JList;

public class JCPopupInfoList <T> extends JDialog {

	private static final long serialVersionUID = -8845392765818076556L;
	private final JPanel contentPanel = new JPanel();
	private JTextField txtCount1 = null;
	private JCLabel lblTitle1 = null;
	private JTextField txtCount2 = null;
	private JCLabel lblTitle2 = null;
	
	private JList<T> list1 = null;
	private JList<T> list2 = null;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		
		
		try {

			displayPopupTwoFields("Test", new String[]{"Test", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF"
					, "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF"}, "Test", new String[]{"Test", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF"
							, "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF", "AWEF"}, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public JCPopupInfoList() {
		
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			// jsut won't use the look and feel
			e.printStackTrace();
		}
		getContentPane().setLayout(new BorderLayout());
		setModal(true);
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		lblTitle1 = new JCLabel("Title", JCLabel.LABEL_SUB_SUBTITLE);
		
		JScrollPane scrollPane = new JScrollPane();
		
		JCLabel lblCount = new JCLabel("Count:", JCLabel.LABEL_TEXT_BOLD);
		
		txtCount1 = new JTextField();
		txtCount1.setColumns(10);
		txtCount1.setFocusable(false);
		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
						.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
						.addComponent(lblTitle1)
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addComponent(lblCount)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(txtCount1, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE)))
					.addContainerGap())
		);
		gl_contentPanel.setVerticalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addContainerGap()
					.addComponent(lblTitle1)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 158, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblCount)
						.addComponent(txtCount1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addContainerGap(15, Short.MAX_VALUE))
		);
		
		list1 = new JList<T>();
		list1.setEnabled(true);
		list1.setFocusable(false);
		list1.setFont(Utils.getTextFont(false));
		
		list2 = new JList<T>();
		list2.setEnabled(true);
		list2.setFocusable(false);
		list2.setFont(Utils.getTextFont(false));
		
		scrollPane.setFocusable(false);
		scrollPane.setViewportView(list1);
		contentPanel.setLayout(gl_contentPanel);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JCButton okButton = new JCButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						dispose();
						
					}
					
				});
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}

		}
		//setBounds(100, 100, 450, 150);

		setResizable(false);


	}
	
	
	
	/**
	 * Create the dialog.
	 */
	private JCPopupInfoList(boolean ags) {
		
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			// jsut won't use the look and feel
			e.printStackTrace();
		}
		getContentPane().setLayout(new BorderLayout());
		setModal(true);
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		lblTitle1 = new JCLabel("Title", JCLabel.LABEL_SUB_SUBTITLE);
		lblTitle2 = new JCLabel("Title", JCLabel.LABEL_SUB_SUBTITLE);

		JScrollPane scrollPane1 = new JScrollPane();
		JScrollPane scrollPane2 = new JScrollPane();

		JCLabel lblCount1 = new JCLabel("Count:", JCLabel.LABEL_TEXT_BOLD);
		JCLabel lblCount2 = new JCLabel("Count:", JCLabel.LABEL_TEXT_BOLD);

		txtCount1 = new JTextField();
		txtCount1.setColumns(10);
		txtCount1.setFocusable(false);
		txtCount2 = new JTextField();
		txtCount2.setColumns(10);
		txtCount2.setFocusable(false);
		
		
		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
						.addComponent(scrollPane1, GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
						.addComponent(lblTitle1)
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addComponent(lblCount1)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(txtCount1, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE))
						.addComponent(scrollPane2, GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
						.addComponent(lblTitle2)
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addComponent(lblCount2)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(txtCount2, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE)))
					.addContainerGap())
		);
		gl_contentPanel.setVerticalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addContainerGap()
					.addComponent(lblTitle1)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(scrollPane1, GroupLayout.PREFERRED_SIZE, 158, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblCount1)
						.addComponent(txtCount1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(lblTitle2)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(scrollPane2, GroupLayout.PREFERRED_SIZE, 158, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblCount2)
						.addComponent(txtCount2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addContainerGap(15, Short.MAX_VALUE))
		);
		
		list1 = new JList<T>();
		list1.setEnabled(true);
		list1.setFocusable(false);
		list1.setFont(Utils.getTextFont(false));
		
		
		list2 = new JList<T>();
		list2.setEnabled(true);
		list2.setFocusable(false);
		list2.setFont(Utils.getTextFont(false));
		
		scrollPane1.setFocusable(false);
		scrollPane1.setViewportView(list1);
		
		scrollPane2.setFocusable(false);
		scrollPane2.setViewportView(list2);
		contentPanel.setLayout(gl_contentPanel);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JCButton okButton = new JCButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						dispose();
						
					}
					
				});
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}

		}
		//setBounds(100, 100, 450, 150);

		setResizable(false);

	}
	
	public void display(String title, T[] listItems, Component parent) {
		this.list1.setListData(listItems);
		pack();
		setLocationRelativeTo(parent);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		txtCount1.setText("" + listItems.length);
		lblTitle1.setText(title);
		
		setVisible(true);
	}
	
	public void display(String title1, T[] listItems1, String title2, T[] listItems2, Component parent) {
		this.list1.setListData(listItems1);
		this.list2.setListData(listItems2);
		pack();
		setLocationRelativeTo(parent);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		txtCount1.setText("" + listItems1.length);
		txtCount2.setText("" + listItems2.length);
		lblTitle1.setText(title1);
		lblTitle2.setText(title2);

		setVisible(true);
	}
	
	public static <K> void displayPopup(String title, K[] obj, Component parent) {
		JCPopupInfoList<K> dialog = new JCPopupInfoList<K>();
		dialog.display(title, obj, parent);
	}
	
	public static <K> void displayPopupTwoFields(String title1, K[] obj1, String title2, K[] obj2, Component parent) {
		JCPopupInfoList<K> dialog = new JCPopupInfoList<K>(false);
		dialog.display(title1, obj1, title2, obj2, parent);
	}
}
