package com.carrington.WIA.GUIs.Components;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
		if (msg == null || msg.length() == 0)
			throw new IllegalArgumentException("Message cannot be blank");
		this.msg = msg;
		setIcon(Utils.IconQuestionLarger);
		setRolloverIcon(Utils.IconQuestionLargerHover);
		setContentAreaFilled(false);
		setBorderPainted(false);
		setBorder(null);
		
	}
	
	/**
	 * Constructs a new help button. Message cannot be null or empty.
	 * 
	 * @param msg the help message
	 * @param comp component for placement of popup
	 * @throws IllegalArgumentException if message is null of blank
	 */
	public JCHelpButton(String msg, final Component comp) {
		this(msg);
		
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Utils.showMessage(Utils.INFO, msg, comp);
			}
		});
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
