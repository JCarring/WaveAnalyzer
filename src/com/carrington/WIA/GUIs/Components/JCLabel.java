package com.carrington.WIA.GUIs.Components;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

import com.carrington.WIA.Utils;

/**
 * A custom {@link JLabel} with predefined font styles for consistent typography
 * across the application.
 */
public class JCLabel extends JLabel {

	private static final long serialVersionUID = -4030824285826399857L;

	/** Style for a main subtitle. */
	public static final int LABEL_SUBTITLE = 1;
	/** Style for standard bold text. */
	public static final int LABEL_TEXT_BOLD = 2;
	/** Style for standard plain text. */
	public static final int LABEL_TEXT_PLAIN = 3;
	/** Style for a smaller, secondary subtitle. */
	public static final int LABEL_SUB_SUBTITLE = 4;
	/** Style for small informational text. */
	public static final int LABEL_SMALL = 5;

	/**
	 * Constructs a new {@link JCLabel} with specified text and style type.
	 * 
	 * @param label The text to be displayed by the label.
	 * @param type  The style type for the label (e.g., {@link #LABEL_SUBTITLE},
	 *              {@link #LABEL_TEXT_BOLD}).
	 */
	public JCLabel(String label, int type) {
		super(label);

		switch (type) {
		case LABEL_SUBTITLE:
			setFont(Utils.getSubTitleFont());
			this.setBorder(new EmptyBorder(0, 3, 0, 3));
			break;
		case LABEL_TEXT_BOLD:
			setFont(Utils.getTextFont(true));
			break;
		case LABEL_TEXT_PLAIN:
			setFont(Utils.getTextFont(false));
			break;
		case LABEL_SUB_SUBTITLE:
			setFont(Utils.getSubTitleSubFont());
			break;
		case LABEL_SMALL:
			setFont(Utils.getSmallTextFont());
			break;
		}
	}

}
