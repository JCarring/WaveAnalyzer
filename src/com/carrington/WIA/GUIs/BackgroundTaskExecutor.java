package com.carrington.WIA.GUIs;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Consumer;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * A utility class to execute a {@link BackgroundTask} on a {@link SwingWorker}
 * thread. It simplifies the process of running a long-running task off the
 * Event Dispatch Thread (EDT) while providing progress updates to a
 * {@link JProgressBar} and handling completion via a callback.
 */
public class BackgroundTaskExecutor {

	/**
	 * Executes a given background task.
	 *
	 * @param <T>          The type of the result returned by the task.
	 * @param task         The {@link BackgroundTask} to execute.
	 * @param progressBar  The {@link JProgressBar} to update with the task's
	 *                     progress.
	 * @param onCompletion A {@link Consumer} callback that will be invoked on the
	 *                     EDT with the result of the task once it is finished.
	 */
	public static <T> void executeTask(BackgroundTask<T> task, final JProgressBar progressBar,
			final Consumer<T> onCompletion) {
		SwingWorker<T, Void> worker = new SwingWorker<T, Void>() {
			@Override
			protected T doInBackground() throws Exception {

				return task.run(new BackgroundProgressRecorder() {

					int maximum = 100;

					@Override
					public void setProgressBarProgress(int progress) {

						int percentage = (int) (((double) progress / maximum) * 100);

						setProgress(percentage); // Fires

					}

					@Override
					public void setProgressBarEnabled(final boolean enabled, final int progress,
							final int maximumProgress) {
						// ensure GUI updates run on the EDT
						maximum = maximumProgress;
						SwingUtilities.invokeLater(() -> {
							progressBar.setVisible(enabled);
							progressBar.setMaximum(100);
							progressBar.setValue(progress);
						});
					}
				});
			}

			@Override
			protected void done() {
				try {
					// get result of background task
					T result = get();
					if (onCompletion != null) {
						onCompletion.accept(result);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		};

		// update progress bar
		worker.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("progress".equals(evt.getPropertyName())) {
					progressBar.setValue((Integer) evt.getNewValue());
				}
			}
		});

		worker.execute();
	}
}
