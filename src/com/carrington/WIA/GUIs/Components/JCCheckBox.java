package com.carrington.WIA.GUIs.Components;

import java.awt.Font;

import javax.swing.JCheckBox;

/**
 * A custom JCheckBox with a specified font and non-opaque background.
 */
public class JCCheckBox extends JCheckBox {

	private static final long serialVersionUID = -4879077130269311105L;

	/**
	 * Constructs a JCCheckBox with a given title, initial selection state, and
	 * font.
	 * 
	 * @param title           The text to be displayed for the checkbox.
	 * @param selectedDefault The initial selection state.
	 * @param font            The font to be used for the title.
	 */
	public JCCheckBox(String title, boolean selectedDefault, Font font) {
		super(title);
		setSelected(selectedDefault);
		setFont(font);
		setOpaque(false);
	}

}
