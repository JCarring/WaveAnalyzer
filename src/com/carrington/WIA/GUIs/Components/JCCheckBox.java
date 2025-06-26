package com.carrington.WIA.GUIs.Components;

import java.awt.Font;

import javax.swing.JCheckBox;

public class JCCheckBox extends JCheckBox {
	
	private static final long serialVersionUID = -4879077130269311105L;

	public JCCheckBox(String title, boolean selectedDefault, Font font) {
		super(title);
		setSelected(selectedDefault);
		setFont(font);
		setOpaque(false);
	}

}
