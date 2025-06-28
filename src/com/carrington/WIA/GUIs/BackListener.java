package com.carrington.WIA.GUIs;

/**
 * A simple listener interface for receiving notifications when a "back" or
 * "cancel" action is performed in a GUI component.
 */
public interface BackListener {
	/**
	 * Invoked when the user initiates a "back" action, such as clicking a
	 * back button or closing a dialog without saving.
	 */
	public void wentBack();
}
