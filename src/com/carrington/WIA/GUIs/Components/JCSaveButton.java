package com.carrington.WIA.GUIs.Components;

import javax.swing.JButton;

import com.carrington.WIA.Utils;

/**
 * Custom button which is actually a save icon, that can be clicked by the user to save a file
 */
public class JCSaveButton extends JButton {

	private static final long serialVersionUID = -371048752409670228L;
	
	/**
	 * Constructs a new save button. Tool tip cannot be null or empty.
	 * 
	 * @param toolTipMessage the tooltip message shown whenhovering
	 * @throws IllegalArgumentException if message is null of blank
	 */
	public JCSaveButton(String toolTipMessage) {
		super();
		if (toolTipMessage == null || toolTipMessage.isBlank())
			throw new IllegalArgumentException("Tooltip message cannot be blank");
		setIcon(Utils.IconSave);
		setRolloverIcon(Utils.IconSaveHover);
		setToolTipText(toolTipMessage);
		setContentAreaFilled(false);
		setBorderPainted(false);
		setBorder(null);
		
	}
	
	
}
