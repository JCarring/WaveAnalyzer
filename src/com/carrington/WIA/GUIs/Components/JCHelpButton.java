package com.carrington.WIA.GUIs.Components;

import javax.swing.JButton;

import com.carrington.WIA.Utils;

/**
 * Custom button which is actually a question mark icon, that can be clicked by the user to reveal a help message
 */
public class JCHelpButton extends JButton {

	private static final long serialVersionUID = -371048752409670228L;
	private String msg; 
	
	/**
	 * Constructs a new help button. Message cannot be null or empty.
	 * 
	 * @param msg the help message
	 * @throws IllegalArgumentException if message is null of blank
	 */
	public JCHelpButton(String msg) {
		super();
		if (msg == null || msg.isBlank())
			throw new IllegalArgumentException("Message cannot be blank");
		this.msg = msg;
		setIcon(Utils.IconQuestionLarger);
		setRolloverIcon(Utils.IconQuestionLargerHover);
		setContentAreaFilled(false);
		setBorderPainted(false);
		setBorder(null);
	}
	
	/**
	 * Gets the help message that would be displayed when this button is pressed
	 * 
	 * @return the String message
	 */
	public String getHelpMessage() {
		return msg;
	}
	
}
