package com.carrington.WIA.IO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.opencsv.CSVWriter;

/**
 * An abstract utility class for saving data to files, primarily in CSV format.
 */
public abstract class Saver {

	/**
	 * Saves a 2D string array to a specified file in CSV format.
	 *
	 * @param file The file to save the data to.
	 * @param data A 2D string array representing the rows and columns.
	 * @return null on success, or an error message string on failure.
	 */
	public static String saveData(File file, String[][] data) {

		FileWriter outputfile;
		try {
			outputfile = new FileWriter(file);
			CSVWriter writer = new CSVWriter(outputfile);

			for (String[] row : data) {
				writer.writeNext(row);
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		}

		return null;

	}

	/**
	 * Saves a map of data columns to a specified file in CSV format.
	 *
	 * @param file The file to save the data to.
	 * @param data A LinkedHashMap where keys are {@link Header}s and values are double arrays of column data.
	 * @return true on success, false on failure.
	 */
	public static boolean saveData(File file, LinkedHashMap<Header, double[]> data) {

		try {
			// create FileWriter object with file as parameter

			FileWriter outputfile = new FileWriter(file);

			// create CSVWriter object filewriter object as parameter
			CSVWriter writer = new CSVWriter(outputfile);

			// adding header to csv
			String[] headers = new String[data.size()];
			int maxRow = -1;

			int counter = 0;
			for (Entry<Header, double[]> en : data.entrySet()) {
				headers[counter] = en.getKey().getName();
				counter++;

				if (maxRow == -1) {
					maxRow = en.getValue().length;
				} else {
					int thisColSize = en.getValue().length;
					if (thisColSize > maxRow) {
						maxRow = thisColSize;
					}
				}
			}
			writer.writeNext(headers);

			String[][] rows = new String[maxRow][headers.length];
			int colNum = 0;
			for (double[] array : data.values()) {
				int rowNum = 0;
				for (double d : array) {
					rows[rowNum][colNum] = String.valueOf(d);
					rowNum++;
				}
				colNum++;
			}

			for (String[] line : rows) {
				writer.writeNext(line);
			}

			// closing writer connection
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
