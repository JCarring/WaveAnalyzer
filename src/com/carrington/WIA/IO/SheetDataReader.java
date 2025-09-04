package com.carrington.WIA.IO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.math.NumberUtils;

import com.carrington.WIA.Utils;
import com.carrington.WIA.DataStructures.HemoData;
import com.carrington.WIA.GUIs.BackgroundProgressRecorder;

/**
 * Reads data from various sheet-based file formats like Excel (.xls, .xlsx),
 * CSV, and tab-delimited text (.txt). It can automatically detect headers or
 * skip a specified number of lines.
 */
public class SheetDataReader {

	private File file;
	private String fileName = null;
	private int ignoreLines = -1;

	/**
	 * New sheet reader
	 * 
	 * @param file        file to read
	 * @param ignoreLines Number of lines at the top to ignore (i.e. extraneous
	 *                    information like patient ID). If -1, will try to
	 *                    auto-detect start of data.
	 */
	public SheetDataReader(File file, int ignoreLines) {
		this.file = file;
		this.fileName = file.getName();
		this.ignoreLines = ignoreLines;
	}

	/**
	 * Reads the header row from the input file. It auto-detects the file type and
	 * attempts to locate the header row if not specified.
	 *
	 * @return A {@link HeaderResult} containing the list of headers or an error
	 *         message.
	 */
	public HeaderResult readHeaders(BackgroundProgressRecorder prog) {
		String namelower = this.file.getName().toLowerCase();
		int readerType = -1;
		if (namelower.endsWith(".csv")) {
			readerType = Reader.CSV;
		} else if (namelower.endsWith(".xlsx") || namelower.endsWith(".xls")) {
			readerType = Reader.EXCEL;
		} else if (namelower.endsWith("txt")) {
			readerType = Reader.TXT_TABBED;
		} else {
			return new HeaderResult(null, "Input file did not have the extension .csv, .txt,  .xls, or .xlsx", false);
		}

		Reader reader;
		try {
			reader = new Reader(this.file, readerType, 1, this.ignoreLines, prog);
			ArrayList<Header> headers = new ArrayList<Header>();

			boolean isPrimary = true;
			boolean notAllNumbers = false;
			for (int i = 0; i < reader.getRow(0).length; i++) {
				String str = reader.getText(0, i);

				if (str.length() > 0) {

					if (!NumberUtils.isParsable(str)) {
						notAllNumbers = true;
					}
					headers.add(new Header(str, i, isPrimary));
					isPrimary = false;
				}
			}

			if (headers.isEmpty() || !notAllNumbers)
				return new HeaderResult(null, "No headers in Excel file", false);
			return new HeaderResult(headers, null, true);

		} catch (Exception e) {
			e.printStackTrace();
			return new HeaderResult(null, e.getMessage(), false);

		}

	}

	/**
	 * Reads data from the file for the specified columns.
	 *
	 * @param columns A list of {@link Header} objects indicating which columns to
	 *                read.
	 * @return A {@link ReadResult} containing the data or an error message.
	 */
	public ReadResult readData(List<Header> columns) {
		return readData(columns, null);
	}

	/**
	 * Reads data from the file for the specified columns, providing progress
	 * updates.
	 *
	 * @param columns       A list of {@link Header} objects indicating which
	 *                      columns to read.
	 * @param progDisplayer A recorder to display progress; can be null.
	 * @return A {@link ReadResult} containing the read data or an error message.
	 */
	public ReadResult readData(List<Header> columns, BackgroundProgressRecorder progDisplayer) {

		if (columns.isEmpty())
			return new ReadResult(null, "No headers supplied when trying to read sheet");

		String namelower = this.file.getName().toLowerCase();
		int readerType = -1;
		if (namelower.endsWith(".csv")) {
			readerType = Reader.CSV;
		} else if (namelower.endsWith(".xlsx") || namelower.endsWith(".xls")) {
			readerType = Reader.EXCEL;
		} else if (namelower.endsWith("txt")) {
			readerType = Reader.TXT_TABBED;
		} else {
			return new ReadResult(null, "Input file did not have the extension .csv, .txt,  .xls, or .xlsx");
		}

		Reader reader;
		try {
		
			reader = new Reader(this.file, readerType, -1, this.ignoreLines, progDisplayer);
			
			// For progress bar
			int maximum = columns.size() * reader.getRows();
	        int updateIncrement = (maximum > 100) ? maximum / 100 : 1;
	        int nextUpdate = 0;
	        int counter = 0;
	        if (progDisplayer != null) {
				progDisplayer.setProgressBarEnabled(true, 0, maximum);
			}

			
			LinkedHashMap<Header, double[]> data = new LinkedHashMap<Header, double[]>();
			boolean primary = true;
			for (int colI = 0; colI < reader.getRow(0).length; colI++) {
				String possibHeader = reader.getText(0, colI);

				if (possibHeader != null && possibHeader.length() > 0) {
					Header header = new Header(possibHeader, colI, primary);

					if (Utils.isHeaderContained(columns, header)) {
						ArrayList<Double> values = new ArrayList<Double>();
						for (int rowI = 1; rowI < reader.getRows(); rowI++) {
							// prog bar update
							if (progDisplayer != null && counter >= nextUpdate) {
								progDisplayer.setProgressBarProgress(counter);
								nextUpdate += updateIncrement;

							}
							// end prog bar update
							Double d = reader.getNumber(rowI, colI);
							if (d.isNaN()) {
								throw new IOException("Data area contained a non-numerical value.");

							} else {
								values.add(d);
							}
							counter++;
						}
						data.put(header, Utils.toArray(values));
						primary = false;

					}
				}

			}

			HemoData hd = new HemoData(file, fileName, null); // don't know name yet.
			for (Entry<Header, double[]> en : data.entrySet()) {
				if (en.getKey().isX()) {
					hd.setXData(en.getKey(), en.getValue());
				} else {

					hd.addYData(en.getKey(), en.getValue());
				}
			}

			return new ReadResult(hd, null);

		} catch (Exception e) {
			e.printStackTrace();
			return new ReadResult(null, e.getMessage());

		}

	}

}
