package com.carrington.WIA.GUIs;

/**
 * Defines a task that can be executed in the background, typically on a worker thread.
 * It is designed to report progress during its execution.
 *
 * @param <T> The type of the result that will be returned when the task is complete.
 */
public interface BackgroundTask<T> {
	/**
     * The main logic of the background task. This method will be executed on a
     * background thread.
     *
     * @param progressCarrier An object that allows the task to report its progress,
     * which can be used to update a UI component like a JProgressBar.
     * @return The result of the computation.
     * @throws Exception if an error occurs during execution.
     */
	
    public T run(BackgroundProgressRecorder progressCarrier) throws Exception;
}
