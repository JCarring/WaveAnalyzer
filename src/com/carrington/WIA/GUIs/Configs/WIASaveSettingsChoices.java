package com.carrington.WIA.GUIs.Configs;

import java.awt.Font;
import java.util.Properties;

import com.carrington.WIA.GUIs.Components.JCDimension;

/**
 * A data object that encapsulates all user-configurable settings related to
 * saving Waveform Intensity Analysis (WIA) outputs. This includes image
 * dimensions, font settings, and file format choices.
 */
public class WIASaveSettingsChoices {

	private static final String keySaveSettingSVGTIFF = "save_svgtiff";
	private static final String keySaveSettingsDisplay = "save_wave_selections";
	private static final String keySaveSettingsFontFamily = "save_font_family";
	private static final String keySaveSettingsFontStyle = "save_font_style";
	private static final String keySaveSettingsFontSize = "save_font_size";
	private static final String keySaveSettingWidth = "save_width";
	private static final String keySaveSettingHeight = "save_height";

	private JCDimension dimSave = null;
	private Font saveFont = null;
	private boolean saveSVGTIFF = true;
	private boolean saveSelections = true;

	private boolean hasChanged = false;

	/**
	 * Constructs a {@link WIASaveSettingsChoices} object and initializes its values from a
	 * given {@link Properties} object.
	 *
	 * @param properties the saved settings.
	 */
	public WIASaveSettingsChoices(Properties properties) {
		this.saveSVGTIFF = Boolean.parseBoolean(properties.getProperty(keySaveSettingSVGTIFF, "true"));
		this.saveSelections = Boolean.parseBoolean(properties.getProperty(keySaveSettingsDisplay, "true"));
		this.dimSave = new JCDimension(Integer.parseInt(properties.getProperty(keySaveSettingHeight, "800")),
				Integer.parseInt(properties.getProperty(keySaveSettingWidth, "600")));

		this.saveFont = new Font(properties.getProperty(keySaveSettingsFontFamily, "Arial"),
				Integer.parseInt(properties.getProperty(keySaveSettingsFontStyle, Font.PLAIN + "")),
				Integer.parseInt(properties.getProperty(keySaveSettingsFontSize, "12")));
	}

	/**
	 * Writes the current settings stored in this object to a
	 * {@link Properties} object.
	 *
	 * @param properties The {@link Properties} object to which the settings will
	 * be saved.
	 */
	public void writeProperties(Properties properties) {
		properties.setProperty(keySaveSettingSVGTIFF, Boolean.toString(saveSVGTIFF));
		properties.setProperty(keySaveSettingsDisplay, Boolean.toString(saveSelections));
		properties.setProperty(keySaveSettingHeight, String.valueOf(dimSave.getHeight()));
		properties.setProperty(keySaveSettingWidth, String.valueOf(dimSave.getWidth()));
		properties.setProperty(keySaveSettingsFontFamily, saveFont.getFamily());
		properties.setProperty(keySaveSettingsFontStyle, saveFont.getStyle() + "");
		properties.setProperty(keySaveSettingsFontSize, saveFont.getSize() + "");
	}
	
	/**
	 * Checks if the settings have been modified by the user.
	 *
	 * @return {@code true} if settings have changed, {@code false} otherwise.
	 */
	public boolean hasChanged() {
		return hasChanged;
	}
	
	/**
	 * Sets the changed status of the settings.
	 *
	 * @param changed The new changed status.
	 */
	public void setChanged(boolean changed) {
		this.hasChanged = changed;
	}
	
	/**
	 * Gets the setting for saving in SVG and TIFF formats.
	 *
	 * @return {@code true} if saving as SVG/TIFF is enabled, {@code false}
	 * otherwise.
	 */
	public boolean getSaveSVGTIFF() {
		return saveSVGTIFF;
	}
	
	/**
	 * Gets the setting for saving an image of the wave selections.
	 *
	 * @return {@code true} if saving wave selections is enabled, {@code false}
	 * otherwise.
	 */
	public boolean getSaveSelections() {
		return saveSelections;
	}
	
	/**
	 * Sets the option for saving in SVG and TIFF formats.
	 *
	 * @param save The desired setting.
	 */
	public void setSaveSVGTIFF(boolean save) {
		saveSVGTIFF = save;
	}
	
	/**
	 * Sets the option for saving an image of the wave selections.
	 *
	 * @param save The desired setting.
	 */
	public void setSaveSelections(boolean save) {
		saveSelections = save;
	}
	
	/**
	 * Gets the dimensions for saved images.
	 *
	 * @return A {@link JCDimension} object containing the height and width.
	 */
	public JCDimension getSaveDimensions() {
		return dimSave;
	}
	
	/**
	 * Sets the dimensions for saved images.
	 *
	 * @param dimSave A {@link JCDimension} object specifying the height and
	 * width.
	 */
	public void setSaveDimensions(JCDimension dimSave) {
		this.dimSave = dimSave;
	}
	
	/**
	 * Gets the font used for text in saved images.
	 *
	 * @return The {@link Font} object.
	 */
	public Font getSaveFont() {
		return saveFont;
	}
	
	/**
	 * Sets the font to be used for text in saved images.
	 *
	 * @param saveFont The {@link Font} object to set.
	 */
	public void setSaveFont(Font saveFont) {
		this.saveFont = saveFont;
	}

}
