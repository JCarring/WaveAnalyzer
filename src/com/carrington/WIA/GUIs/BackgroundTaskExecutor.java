package com.carrington.WIA.GUIs;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Consumer;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;



// Interface for a background task.
interface BackgroundTask<T> {
    T run(ProgressRecorder progressCarrier) throws Exception;
}

// Updated helper class that also accepts a completion callback.
public class BackgroundTaskExecutor {
    public static <T> void executeTask(BackgroundTask<T> task, final JProgressBar progressBar, final Consumer<T> onCompletion) {
        SwingWorker<T, Void> worker = new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                // Pass a ProgressBarCarrier that calls SwingWorker's setProgress().
            	
                return task.run(new ProgressRecorder() {
                	
                	int maximum = 100;
                    @Override
                    public void setProgressBarProgress(int progress) {
                    	
                    	 int percentage = (int) (((double) progress / maximum) * 100);

                         setProgress(percentage); // Fires
                         
                    }
                    @Override
                    public void setProgressBarEnabled(final boolean enabled, final int progress, final int maximumProgress) {
                        // Ensure GUI updates run on the EDT.
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
                    // Retrieve the result of the background task.
                    T result = get();
                    if (onCompletion != null) {
                        onCompletion.accept(result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Optionally, pass a null result or handle errors separately.
                }

            }
        };

        // Listen for progress updates and update the progress bar.
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
