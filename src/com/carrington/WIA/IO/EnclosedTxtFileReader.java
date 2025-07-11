package com.carrington.WIA.IO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A utility class to read text content from files that are embedded as
 * resources within the application's JAR file.
 */
public class EnclosedTxtFileReader {

	/**
	 * Reads and returns the content of the 'waveshelp.html' resource file.
	 *
	 * @return The file content as a single string with line separators removed, or
	 *         an empty string on error.
	 */
	public static String getWavesHelp() {

		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/waveshelp.html").readAllBytes();

			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {

			e.printStackTrace();
			return "";
		}

	}
	
	/**
	 * Reads and returns the content of the 'mainframehelp.html' resource file.
	 *
	 * @return The file content as a single string with line separators removed, or
	 *         an empty string on error.
	 */
	public static String getMainFrameHelp() {

		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/mainframehelp.html").readAllBytes();

			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {

			e.printStackTrace();
			return "";
		}

	}
	
	/**
	 * Reads and returns the content of the 'wavealignpfhelp.html' resource file.
	 *
	 * @return The file content as a single string with line separators removed, or
	 *         an empty string on error.
	 */
	public static String getWavesAlignPFHelp() {

		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/wavealignpfhelp.html").readAllBytes();

			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {

			e.printStackTrace();
			return "";
		}

	}

	/**
	 * Reads and returns the content of the 'diameterhelp.html' resource file.
	 *
	 * @return The file content as a single string with line separators removed, or
	 *         an empty string on error.
	 */
	public static String getDiameterHelp() {

		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/diameterhelp.html")
					.readAllBytes();

			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {

			e.printStackTrace();
			return "";
		}

	}

	/**
	 * Reads and returns the content of the 'wavepickerpanelhelp.html' resource file.
	 *
	 * @return The file content as a single string with line separators removed, or
	 *         an empty string on error.
	 */
	public static String getWavePanelHelp() {
		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/wavepickerpanelhelp.html")
					.readAllBytes();

			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {

			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Reads and returns the content of the 'selectbeatshelp.html' resource file.
	 *
	 * @return The file content as a single string with line separators removed, or
	 *         an empty string on error.
	 */
	public static String getSelectBeatPanelHelp() {
		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/selectbeatshelp.html")
					.readAllBytes();

			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {

			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Reads and returns the content of the 'alignbytimehelp.html' resource file.
	 *
	 * @return The file content as a single string with line separators removed, or
	 *         an empty string on error.
	 */
	public static String getAlignByTimeHelp() {
		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/alignbytimehelp.html")
					.readAllBytes();

			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {

			e.printStackTrace();
			return "";
		}
	}
}
