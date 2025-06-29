package com.carrington.WIA.GUIs.Components;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.JButton;

import com.carrington.WIA.Utils;

/**
 * A custom {@link JButton} with predefined styles for consistent look and feel
 * across the application.
 */
public class JCButton extends JButton {

	private static final long serialVersionUID = 703129182463975013L;
	private static final Insets btnInsetsStandard = new Insets(1, 10, 1, 10);
	private static final Insets btnInsetsSmall = new Insets(0, 0, 0, 0);

	private static final Insets btnInsetsLarger = new Insets(2, 10, 2, 10);
	private static final Color colorLightBlue = new Color(155, 233, 255);
	private static final Color colorLightRed = new Color(255, 167, 155);
	private static final Color colorLightGreen = new Color(157, 249, 152);

	/** A green button for 'accept' or 'confirm' actions. */
	public static final int BUTTON_ACCEPT = 1;
	/** A red button for 'go back' actions. */
	public static final int BUTTON_GO_BACK = 2;
	/** A red button for 'quit' or 'cancel' actions. */
	public static final int BUTTON_QUIT = 3;
	/** A standard, default-styled button. */
	public static final int BUTTON_STANDARD = 4;
	/** A blue button for 'reset' actions. */
	public static final int BUTTON_RESET = 5;
	/** A smaller button with minimal padding. */
	public static final int BUTTON_SMALL = 6;
	/** A green button for 'run' or 'start' actions. */
	public static final int BUTTON_RUN = 7;

	/**
	 * Constructs a new {@link JCButton} with the specified text and a default
	 * style.
	 * 
	 * @param name The text to display on the button.
	 */
	public JCButton(String name) {
		this(name, BUTTON_STANDARD);
	}

	/**
	 * Constructs a new {@link JCButton} with specified text and button type.
	 * 
	 * @param name The text to display on the button.
	 * @param buttonType The style type of the button (e.g., {@link #BUTTON_ACCEPT}, {@link #BUTTON_QUIT}).
	 */
	public JCButton(String name, int buttonType) {
		super(name);

		switch (buttonType) {
		case BUTTON_ACCEPT:
			Utils.setFont(Utils.getSubTitleFont(), this);
			setMargin(btnInsetsLarger);
			setBackground(colorLightGreen);
			break;
		case BUTTON_RUN:
			Utils.setFont(Utils.getSubTitleSubFont(), this);
			setMargin(btnInsetsStandard);
			setBackground(colorLightGreen);
			break;
		case BUTTON_GO_BACK:
		case BUTTON_QUIT:
			Utils.setFont(Utils.getSubTitleFont(), this);
			setMargin(btnInsetsLarger);
			setBackground(colorLightRed);
			break;
		case BUTTON_STANDARD:
			Utils.setFont(Utils.getTextFont(true), this);
			setMargin(btnInsetsStandard);
			break;
		case BUTTON_RESET:
			setBackground(colorLightBlue);
			Utils.setFont(Utils.getSubTitleFont(), this);
			setMargin(btnInsetsLarger);
			break;
		case BUTTON_SMALL:
			setMargin(btnInsetsSmall);
			Utils.setFont(Utils.getSmallTextFont(), this);

			break;
		}

		setFocusable(false);

	}

}
