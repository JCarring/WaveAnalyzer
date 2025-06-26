package com.carrington.WIA.GUIs.Components;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

import com.carrington.WIA.Utils;

public class JCLabel extends JLabel{
	
	private static final long serialVersionUID = -4030824285826399857L;

	public static final int LABEL_SUBTITLE = 1;
	public static final int LABEL_TEXT_BOLD = 2;
	public static final int LABEL_TEXT_PLAIN = 3;
	public static final int LABEL_SUB_SUBTITLE = 4;
	public static final int LABEL_SMALL = 5;


	
	public JCLabel(String label, int type) {
		super(label);
		
		switch (type) {
		case LABEL_SUBTITLE:
			setFont(Utils.getSubTitleFont());
			this.setBorder(new EmptyBorder(0, 3, 0 , 3));
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
