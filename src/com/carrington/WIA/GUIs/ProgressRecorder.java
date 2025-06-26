package com.carrington.WIA.GUIs;

public interface ProgressRecorder {
	
	public void setProgressBarEnabled(boolean enabled, int progress, int maximum);
	public void setProgressBarProgress(int progress);
}
