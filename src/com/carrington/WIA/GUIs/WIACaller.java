package com.carrington.WIA.GUIs;

import java.io.File;

/**
 * An interface for GUI components that initiate Wave Intensity Analysis (WIA)
 * and need to provide file paths for saving various analysis outputs, such as
 * images and data files. Implementing this interface allows a callee (like a
 * WIA processing window) to request standardized save locations from its
 * caller.
 */
public interface WIACaller {

	/**
	 * Gets the destination {@link File} for saving the main WIA plot as an SVG image.
	 *
	 * @return A {@link File} object representing the target SVG file path.
	 */
	public File getWIAImageFileSVG();

	/**
	 * Gets the destination folder {@link File} where TIFF images should be saved.
	 *
	 * @return A {@link File} object representing the target directory.
	 */
	public File getWIAImageFolderTIFF();

	/**
	 * Gets the destination {@link File} for saving the wave selections plot as an SVG image.
	 *
	 * @return A {@link File} object representing the target SVG file path for the wave selections.
	 */
	public File getWIAWaveSelectionsFileSVG();

}
