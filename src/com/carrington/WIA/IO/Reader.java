package com.carrington.WIA.IO;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.carrington.WIA.Utils;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class Reader {

	public static final int EXCEL = 1;
	public static final int CSV = 2;
	public static final int TXT_TABBED = 3;

	public static final int NO_SKIP = -1;

	private final File file;

	private String[][] mainData = null;

	private int skipLines;

	/**
	 * 
	 * @param file					The file to read from
	 * @param type					the type of file, one of {@link #EXCEL}, {@link #CSV}, or {@link #TXT_TABBED}
	 * @param numberOfLinesToRead Number lines to read, i.e. for only getting
	 *                            headers so we don't need to read whole file
	 * @param skipLines           Number of lines to skip or zero; If -1, will
	 *                            auto-determine start of data, subtract one, and
	 *                            that will be considered the start of the file.
	 * @throws IOException if there was an error with reading
	 */
	public Reader(File file, int type, int numberOfLinesToRead, int skipLines) throws IOException {
		this.file = file;

		if (skipLines >= 0) {
			this.skipLines = skipLines;
		} else {
			this.skipLines = -1; // convert any negative number to -1
		}
		switch (type) {
		case EXCEL:
			_initExcel(numberOfLinesToRead);
			break;
		case CSV:
			_initCSV(numberOfLinesToRead);
			break;
		case TXT_TABBED:
			_initTXT(numberOfLinesToRead);
			break;
		default:
			throw new IllegalArgumentException();
		}

		if (this.skipLines == -1) {
			int numLinesSkip = _determineMeaningfulStart(this.mainData);
			this.skipLines = numLinesSkip >= 0 ? numLinesSkip : 0;
		}

		_trimDataList();

	}

	public int getRows() {
		return this.mainData.length - this.skipLines;
	}

	public String[] getRow(int row) {
		return this.mainData[row + this.skipLines];
	}

	public String getText(int row, int column) {
		row = row + this.skipLines;
		if (row < this.mainData.length) {
			if (column < this.mainData[row].length) {
				return mainData[row][column];

			}
		}
		return null;
	}

	/**
	 * Retrieves a number from the requested row and column.
	 * 
	 * @param row    Row of interest
	 * @param column Column of interest
	 * @return the number in the specified location, or 0 if blank, or
	 *         {@link Double#NaN} if something else is contain (i.e. text, formula,
	 *         etc)
	 */
	public Double getNumber(int row, int column) {
		row = row + this.skipLines;
		if (row < this.mainData.length) {
			if (column < this.mainData[row].length) {

				String value = mainData[row][column];
				if (value.isBlank())
					value = "0";

				try {
					return Double.parseDouble(value);
				} catch (Exception e) {
					if (Utils.isATimeStamp(value)) {
						return 0d;
					} else {
						return Double.NaN;
					}
				}

			}
		}
		return null;
	}

	public void _initExcel(int numLines) throws IOException {
		if (this.mainData == null) {
			Workbook workbook = WorkbookFactory.create(file);
			if (workbook == null)
				throw new IOException("Could not obtain Excel workbook");
			Sheet sheet = workbook.getSheetAt(0);
			if (sheet == null)
				throw new IOException("Could not obtain Excel sheet");

			Iterator<Row> rowItr = sheet.rowIterator();

			try {
				List<String[]> data = new ArrayList<String[]>();
				if (numLines <= 0) {

					while (rowItr.hasNext()) {
						Row row = rowItr.next();
						Iterator<Cell> cells = row.cellIterator();
						ArrayList<String> str = new ArrayList<String>();
						while (cells.hasNext()) {
							str.add(cells.next().getStringCellValue());
						}

						data.add(str.toArray(new String[0]));
					}

				} else {

					int linesToRead = (numLines + (skipLines > 0 ? skipLines : 0));
					while (linesToRead > 0) {
						if (!rowItr.hasNext()) {
							throw new IOException("Not enough rows");
						}
						Row row = rowItr.next();
						Iterator<Cell> cells = row.cellIterator();
						ArrayList<String> str = new ArrayList<String>();
						while (cells.hasNext()) {
							str.add(cells.next().getStringCellValue());
						}

						data.add(str.toArray(new String[0]));
						linesToRead--;

					}
				}

				this.mainData = data.toArray(new String[0][]);

			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException("Error reading excel - maybe it contained formulas.");
			}

			if (this.mainData == null || (this.mainData.length <= skipLines)) {
				throw new IOException("Not enough data in Excel sheet");

			}

		}
	}

	private void _initCSV(int numLines) throws IOException {

		if (this.mainData == null) {
			CSVReader reader;
			try {

				reader = new CSVReader(new FileReader(file));

			} catch (Exception e) {
				throw new IOException(
						"Could not read CSV file. May it has been deleted, moved, or you do not have permission. "
								+ e.getMessage());
			}

			if (numLines <= 0) {
				try {

					this.mainData = reader.readAll().toArray(new String[0][]);
					reader.close();
				} catch (Exception e) {

					throw new IOException("Could not open the CSV file.");

				}
			} else {

				List<String[]> rawData = new ArrayList<String[]>();
				int linesToRead = (numLines + (skipLines >= 0 ? skipLines : 0));

				while (linesToRead > 0) {

					String[] s;
					try {
						s = reader.readNext();

					} catch (CsvValidationException | IOException e) {
						e.printStackTrace();
						throw new IOException("Not enough rows or could not read.");
					}

					if (s == null) {
						throw new IOException("Not enough rows");
					} else {

						rawData.add(s);
					}

					linesToRead--;
				}
				this.mainData = rawData.toArray(new String[0][]);

			}

			if (this.mainData == null || (this.mainData.length <= skipLines)) {
				throw new IOException("Not enough data in CSV sheet");

			}

		}
	}

	@SuppressWarnings("resource")
	private void _initTXT(int numLines) throws IOException {

		if (this.mainData == null) {
			List<String[]> lines = new ArrayList<String[]>();
			Scanner scanner;
			try {
				scanner = new Scanner(this.file);

			} catch (Exception e) {
				throw new IOException("Could not read TXT file.");
			}
			if (numLines <= 0) {

				while (scanner.hasNext()) {
					String line = scanner.nextLine();
					if (line == null)
						continue;
					lines.add(line.split("\\t"));
				}

			} else {

				int linesToRead = (numLines + (skipLines >= 0 ? skipLines : 10000)); // auto look for headers,
																						// realistcally they will not be
																						// 10000 down...

				while (linesToRead > 0) {
					if (!scanner.hasNext()) {
						if (skipLines >= 0) {
							throw new IOException("Not enough rows");

						} else {
							throw new IOException("No headers found");

						}
					}
					try {
						String[] line = scanner.nextLine().split("\\t");
						lines.add(line);
						int start = _determineMeaningfulStart(lines);
						if (start != -1) {
							break;
						}
					} catch (Exception e) {
						e.printStackTrace();
						throw new IOException("Not enough rows or could not read.");
					}

					linesToRead--;
				}
			}
			scanner.close();

			this.mainData = lines.toArray(new String[0][]);

			if (this.mainData == null || (this.mainData.length <= skipLines)) {
				throw new IOException("Not enough data in TXT sheet");

			}

		}

	}

	public static int _determineMeaningfulStart(List<String[]> data) {
		return _determineMeaningfulStart(data.toArray(new String[0][]));
	}

	private static int _determineMeaningfulStart(String[][] data) {

		int firstLineOfDataIndex = -1;

		int row = 0;
		for (String[] str : data) {
			// determine if all of the values in this row are numbers.

			boolean allNumbers = true;
			for (String cell : str) {
				if (cell == null) {
					allNumbers = false;
					break;
				} else if (cell.isBlank()) {
					cell = "0";
				}
				if (!NumberUtils.isCreatable(cell) && !Utils.isATimeStamp(cell)) {
					allNumbers = false;
					break;
				}
			}

			if (allNumbers) {
				if (firstLineOfDataIndex != -1) {
					// basically found two rows of data.
					break;
				} else {
					// found first line, make SURE that the next line is also a row of data.
					firstLineOfDataIndex = row;
				}
			}
			row++;
		}

		// Now need to confirm that the row right before the row of data is a header
		// row.

		if (firstLineOfDataIndex < 1) {
			// no row was found, or the row was row 0 meaning no headers which is invalid.
			return -1;
		} else {
			int indexOfHeaders = firstLineOfDataIndex - 1;

			if (_meaningfulSize(data[indexOfHeaders]) < data[firstLineOfDataIndex].length) {
				return -1; // could not determine header location
			} else {
				String[] headers = data[indexOfHeaders];
				for (String header : headers) {
					if (header == null || header.isBlank()) {
						// where there were supposed to be headers, there was a blank cell. Invalid.
						return -1;
					}
				}

			}

			return indexOfHeaders;
		}

	}

	private void _trimDataList() {
		int uniformSize = 0;
		for (int i = skipLines; i < mainData.length; i++) {
			uniformSize = Math.max(uniformSize, _meaningfulSize(mainData[i]));
		}

		for (int i = skipLines; i < mainData.length; i++) {
			mainData[i] = _uniformEndRemoveNulls(mainData[i], uniformSize);
		}

	}

	public static int _meaningfulSize(String[] array) {

		int meaningfulSize = array.length;
		for (int i = array.length - 1; i > 0; i--) {
			if (array[i] == null || array[i].isBlank()) {
				meaningfulSize--;
			}
		}
		return meaningfulSize;
	}

	private static String[] _uniformEndRemoveNulls(String[] list, int targetSize) {

		String[] trimmedArray = Arrays.copyOfRange(list, 0, targetSize);
		for (int i = 0; i < trimmedArray.length; i++) {
			if (trimmedArray[i] == null) {
				trimmedArray[i] = "";
			}
		}
		return trimmedArray;
	}

}
