package com.carrington.WIA.IO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A utility class to read text content from files that are embedded as
 * resources within the application's JAR file.
 */
public class EnclosedTxtFileReader {

	/**
	 * Reads and returns the content of the 'waveshelp.txt' resource file.
	 *
	 * @return The file content as a single string with line separators removed, or
	 *         an empty string on error.
	 */
	public static String getWavesHelp() {

		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/waveshelp.txt").readAllBytes();

			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {

			e.printStackTrace();
			return "";
		}

	}

	/**
	 * Reads and returns the content of the 'diameterhelp.txt' resource file.
	 *
	 * @return The file content as a single string with line separators removed, or
	 *         an empty string on error.
	 */
	public static String getDiameterHelp() {

		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/diameterhelp.txt")
					.readAllBytes();

			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {

			e.printStackTrace();
			return "";
		}

	}

	/**
	 * Reads and returns the content of the 'wavepickerpanelhelp.txt' resource file.
	 *
	 * @return The file content as a single string with line separators removed, or
	 *         an empty string on error.
	 */
	public static String getWavePanelHelp() {
		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/wavepickerpanelhelp.txt")
					.readAllBytes();

			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {

			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Reads and returns the content of the 'selectbeatshelp.txt' resource file.
	 *
	 * @return The file content as a single string with line separators removed, or
	 *         an empty string on error.
	 */
	public static String getSelectBeatPanelHelp() {
		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/selectbeatshelp.txt")
					.readAllBytes();

			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {

			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Reads and returns the content of the 'alignbytimehelp.txt' resource file.
	 *
	 * @return The file content as a single string with line separators removed, or
	 *         an empty string on error.
	 */
	public static String getAlignByTimeHelp() {
		try {
			byte[] bytes = EnclosedTxtFileReader.class.getResourceAsStream("/textfiles/alignbytimehelp.txt")
					.readAllBytes();

			return new String(bytes, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");

		} catch (IOException e) {

			e.printStackTrace();
			return "";
		}
	}
}
