package com.carrington.WIA.GUIs.Components;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

/**
 * A utility class to globally track the pressed state of the Shift and Command
 * (Meta) keys.
 */
public class KeyChecker {

	private static volatile boolean shiftPressed = false;
	private static volatile boolean cmdPressed = false;

	static {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

			@Override
			public boolean dispatchKeyEvent(KeyEvent ke) {
				synchronized (KeyChecker.class) {
					switch (ke.getID()) {
					case KeyEvent.KEY_PRESSED:

						if (ke.getKeyCode() == KeyEvent.VK_SHIFT) {
							shiftPressed = true;
						} else if (ke.getKeyCode() == KeyEvent.VK_META) {
							cmdPressed = true;
						}
						break;

					case KeyEvent.KEY_RELEASED:
						if (ke.getKeyCode() == KeyEvent.VK_SHIFT) {
							shiftPressed = false;
						} else if (ke.getKeyCode() == KeyEvent.VK_META) {
							cmdPressed = false;
						}
						break;
					}
					return false;
				}
			}
		});
	}

	/**
	 * Checks if the Shift key is currently pressed.
	 * 
	 * @return true if Shift is pressed, false otherwise.
	 */
	public static boolean isShiftPressed() {
		synchronized (KeyChecker.class) {
			return shiftPressed;
		}
	}

	/**
	 * Checks if the Command (Meta) key is currently pressed.
	 * 
	 * @return true if Command/Meta is pressed, false otherwise.
	 */
	public static boolean isControlPressed() {
		synchronized (KeyChecker.class) {
			return cmdPressed;
		}
	}
}
