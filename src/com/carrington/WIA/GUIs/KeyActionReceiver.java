package com.carrington.WIA.GUIs;

import java.awt.event.KeyEvent;

/**
 * Listener for key press events
 */
public interface KeyActionReceiver {
	
	/**
	 * Handle the event that a key was pressed (int returned by {@link KeyEvent})
	 * 
	 * 
	 * @param key the key that was pressed
	 */
	public void keyPressed(int key);
}
