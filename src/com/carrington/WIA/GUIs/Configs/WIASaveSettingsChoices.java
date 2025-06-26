package com.carrington.WIA.GUIs.Configs;

import java.awt.Font;
import java.util.Properties;

import com.carrington.WIA.GUIs.Components.JCDimension;

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

	public WIASaveSettingsChoices(Properties properties) {
		this.saveSVGTIFF = Boolean.parseBoolean(properties.getProperty(keySaveSettingSVGTIFF, "true"));
		this.saveSelections = Boolean.parseBoolean(properties.getProperty(keySaveSettingsDisplay, "true"));
		this.dimSave = new JCDimension(Integer.parseInt(properties.getProperty(keySaveSettingHeight, "800")),
				Integer.parseInt(properties.getProperty(keySaveSettingWidth, "600")));

		this.saveFont = new Font(properties.getProperty(keySaveSettingsFontFamily, "Arial"),
				Integer.parseInt(properties.getProperty(keySaveSettingsFontStyle, Font.PLAIN + "")),
				Integer.parseInt(properties.getProperty(keySaveSettingsFontSize, "12")));
	}

	public void writeProperties(Properties properties) {
		properties.setProperty(keySaveSettingSVGTIFF, Boolean.toString(saveSVGTIFF));
		properties.setProperty(keySaveSettingsDisplay, Boolean.toString(saveSelections));
		properties.setProperty(keySaveSettingHeight, String.valueOf(dimSave.getHeight()));
		properties.setProperty(keySaveSettingWidth, String.valueOf(dimSave.getWidth()));
		properties.setProperty(keySaveSettingsFontFamily, saveFont.getFamily());
		properties.setProperty(keySaveSettingsFontStyle, saveFont.getStyle() + "");
		properties.setProperty(keySaveSettingsFontSize, saveFont.getSize() + "");
	}
	
	public boolean hasChanged() {
		return hasChanged;
	}
	
	public void setChanged(boolean changed) {
		this.hasChanged = changed;
	}
	
	public boolean getSaveSVGTIFF() {
		return saveSVGTIFF;
	}
	
	public boolean getSaveSelections() {
		return saveSelections;
	}
	
	public void setSaveSVGTIFF(boolean save) {
		saveSVGTIFF = save;
	}
	
	public void setSaveSelections(boolean save) {
		saveSelections = save;
	}
	
	public JCDimension getSaveDimensions() {
		return dimSave;
	}
	
	public void setSaveDimensions(JCDimension dimSave) {
		this.dimSave = dimSave;
	}
	
	public Font getSaveFont() {
		return saveFont;
	}
	
	public void setSaveFont(Font saveFont) {
		this.saveFont = saveFont;
	}

}
