package com.carrington.WIA.GUIs;

/**
 * An interface for a background task to report progress back to the user interface.
 * This allows a long running operation on a worker thread to update a progress bar
 * on the EDT
 */
public interface BackgroundProgressRecorder {
	
	/**
	 * Enables or disables the progress bar and sets its initial value.
	 * 
	 * @param enabled  true to make the progress bar visible and active, false to hide it.
	 * @param progress The initial progress value.
	 * @param maximum  The maximum value of the progress bar.
	 */
	public void setProgressBarEnabled(boolean enabled, int progress, int maximum);
	
	/**
	 * Updates the current value of the progress bar.
	 * 
	 * @param progress The new progress value to set.
	 */
	public void setProgressBarProgress(int progress);
}
