package com.carrington.WIA.IO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.carrington.WIA.Utils;

/**
 * A utility class to read text content from files that are embedded as
 * resources within the application's JAR file.
 */
@SuppressWarnings("javadoc")
public abstract class WIAResourceReader {
	
	private static final String PATH_PREFIX = "/resources/textfiles/";
	
	public static final String HELP_ALIGN_TIME = "alignbytimehelp.html";
	public static final String HELP_VESSEL_DIAMETER = "diameterhelp.html";
	public static final String HELP_MAIN = "mainframehelp.html";
	public static final String HELP_RESAMPLE = "resamplehelp.html";
	public static final String HELP_SELECT_BEATS = "selectbeatshelp.html";
	public static final String HELP_TRIM = "trimhelp.html";
	public static final String HELP_WAVE_ALIGN_PF = "wavealignpfhelp.html";
	public static final String HELP_WAVE_PICKER = "wavepickerpanelhelp.html";
	public static final String HELP_WAVES = "waveshelp.html";

	/**
	 * Reads and returns the content of the specified resource file
	 *
	 * @return The file content as a single string with line separators removed, or
	 *         an empty string on error.
	 */
	public static String getContents(String fileName) {
		
	
		
	    try (InputStream in = WIAResourceReader.class.getResourceAsStream(PATH_PREFIX + fileName);
	            ByteArrayOutputStream out = new ByteArrayOutputStream()) {

	           byte[] buffer = new byte[8192];
	           int len;
	           while ((len = in.read(buffer)) != -1) {
	               out.write(buffer, 0, len);
	           }

	           return new String(out.toByteArray(), StandardCharsets.UTF_8)
	                   .replaceAll(System.lineSeparator(), "");

	       } catch (IOException e) {
	           Utils.showMessage(Utils.ERROR, "Resource error: " + fileName + " - contact developer", null);
	           e.printStackTrace();
	           return "Error";
	       }
		
	}
	
	
	

}
