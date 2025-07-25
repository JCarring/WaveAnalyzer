package com.carrington.WIA.IO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.carrington.WIA.Utils;
import com.carrington.WIA.GUIs.BackgroundProgressRecorder;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * A versatile file reader designed to parse data from Excel (.xls, .xlsx), CSV,
 * and tab-delimited text (.txt) files. It can automatically determine where the
 * data starts or skip a specified number of header lines.
 */
public class Reader {

	/** Constant representing an Excel file type. */
	public static final int EXCEL = 1;
	/** Constant representing a CSV file type. */
	public static final int CSV = 2;
	/** Constant representing a tab-delimited text file type. */
	public static final int TXT_TABBED = 3;
	/**
	 * Constant indicating that no lines should be skipped (auto-detection will be
	 * used).
	 */
	public static final int NO_SKIP = -1;

	private final File file;

	private String[][] mainData = null;

	private int skipLines;

	/**
	 * 
	 * @param file                The file to read from
	 * @param type                the type of file, one of {@link #EXCEL},
	 *                            {@link #CSV}, or {@link #TXT_TABBED}
	 * @param numberOfLinesToRead Number lines to read, i.e. for only getting
	 *                            headers so we don't need to read whole file
	 * @param skipLines           Number of lines to skip or zero; If -1, will
	 *                            auto-determine start of data, subtract one, and
	 *                            that will be considered the start of the file.
	 * @throws IOException if there was an error with reading
	 */
	public Reader(File file, int type, int numberOfLinesToRead, int skipLines, BackgroundProgressRecorder prog) throws IOException {
		this.file = file;

		
		if (skipLines >= 0) {
			this.skipLines = skipLines;
		} else {
			this.skipLines = -1; // convert any negative number to -1
		}
		
		switch (type) {
		case EXCEL:
			_initExcel(numberOfLinesToRead, prog);
			break;
		case CSV:
			_initCSV(numberOfLinesToRead);
			break;
		case TXT_TABBED:
			_initTXT(numberOfLinesToRead);
			break;
		default:
			prog.setProgressBarEnabled(false, -1, -1);
			throw new IllegalArgumentException();
		}

		if (this.skipLines == -1) {
			int numLinesSkip = _determineMeaningfulStart(this.mainData);
			this.skipLines = numLinesSkip >= 0 ? numLinesSkip : 0;
		}

		_trimDataList();
		if (prog != null) {
			prog.setProgressBarEnabled(false, -1, -1);
		}

	}

	/**
	 * Gets the total number of data rows (after skipping initial lines).
	 *
	 * @return The number of rows.
	 */
	public int getRows() {
		return this.mainData.length - this.skipLines;
	}

	/**
	 * Gets a specific row of data as an array of strings.
	 *
	 * @param row The index of the row to retrieve (0-based, relative to the start
	 *            of data).
	 * @return A string array representing the row, or null if out of bounds.
	 */
	public String[] getRow(int row) {
		return this.mainData[row + this.skipLines];
	}

	/**
	 * Gets the text value of a specific cell.
	 *
	 * @param row    The row index (0-based, relative to the start of data).
	 * @param column The column index (0-based).
	 * @return The string value of the cell, or null if out of bounds.
	 */
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

	/**
	 * Initializes the reader by parsing an Excel file.
	 *
	 * @param numLines The number of lines to read; if -1, reads the entire sheet.
	 * @throws IOException if the file cannot be read or parsed.
	 */
	private void _initExcel(int numLines, BackgroundProgressRecorder prog) throws IOException {

		if (mainData != null)
			return;
		
		if (prog != null) {
			prog.setProgressBarEnabled(true, 1, 4);
		}
		
		Workbook workbook = WorkbookFactory.create(file);
		if (workbook == null)
			throw new IOException("Could not obtain Excel workbook");

		prog.setProgressBarProgress(2);

		Sheet sheet = workbook.getSheetAt(0);
		if (sheet == null)
			throw new IOException("Could not obtain Excel sheet");

		FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
		Iterator<Row> rowItr = sheet.rowIterator();

		try {

			prog.setProgressBarProgress(2);

			List<String[]> data = new ArrayList<String[]>();
			if (numLines <= 0) {

				while (rowItr.hasNext()) {
					Row row = rowItr.next();
					Iterator<Cell> cells = row.cellIterator();
					ArrayList<String> str = new ArrayList<String>();
					while (cells.hasNext()) {
						str.add(_getCellValueAsString(cells.next(), evaluator));
					}

					data.add(str.toArray(new String[0]));
				}

			} else {

				int linesToRead = numLines + Math.max(0, skipLines);

				while (linesToRead > 0) {
					if (!rowItr.hasNext()) {
						throw new IOException("Not enough rows");
					}
					Row row = rowItr.next();
					Iterator<Cell> cells = row.cellIterator();
					ArrayList<String> str = new ArrayList<String>();
					while (cells.hasNext()) {
						str.add(_getCellValueAsString(cells.next(), evaluator));
					}

					data.add(str.toArray(new String[0]));
					linesToRead--;

				}
			}
			prog.setProgressBarProgress(3);

			this.mainData = data.toArray(new String[0][]);

		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException("Error reading excel - maybe it contained formulas.");
		} finally {
			if (workbook != null)
				workbook.close();
		}

		if (this.mainData == null || (this.mainData.length <= skipLines)) {
			throw new IOException("Not enough data in Excel sheet");

		}
		prog.setProgressBarProgress(4);



	}

	/**
	 * Initializes the reader by parsing a CSV file.
	 *
	 * @param numLines The number of lines to read; if -1, reads the entire file.
	 * @throws IOException if the file cannot be read or parsed.
	 */
	private void _initCSV(int numLines) throws IOException {

		if (this.mainData != null)
			return;

		try (CSVReader reader = new CSVReader(new FileReader(file))) {

			if (numLines <= 0) {
				// Read all of the lines in the files
				try {

					this.mainData = reader.readAll().toArray(new String[0][]);
				} catch (Exception e) {
					throw new IOException("Could not open the CSV file.");
				} finally {
					reader.close();
				}
			} else {
				// Read only specified numbers of lines
				List<String[]> rawData = new ArrayList<String[]>();
				int linesToRead = numLines + Math.max(0, skipLines);

				for (int i = 0; i < linesToRead; i++) {
					String[] row;
					try {
						row = reader.readNext();
						if (row == null) {
							throw new IOException("Reached end of file before reading " + linesToRead + " rows.");
						}
						rawData.add(row);
					} catch (CsvValidationException | IOException e) {
						throw new IOException("Error reading row " + (i + 1) + ": " + e.getMessage(), e);
					}
				}

				this.mainData = rawData.toArray(new String[0][]);

			}

			if (this.mainData == null || (this.mainData.length <= skipLines)) {
				throw new IOException("Not enough data in CSV sheet");
			}

		} catch (FileNotFoundException e) {
			throw new IOException(
					"Could not read CSV file. May it has been deleted, moved, or you do not have permission. "
							+ e.getMessage());
		}

	}

	/**
	 * Initializes the reader by parsing a tab-delimited text file.
	 *
	 * @param numLines The number of lines to read; if -1, reads the entire file.
	 * @throws IOException if the file cannot be read or parsed.
	 */
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

	/**
	 * Determines the starting row of meaningful data by finding the first row of
	 * numeric values that is preceded by a header row.
	 *
	 * @param data A list of string arrays representing the raw file data.
	 * @return The 0-based index of the header row, or -1 if not found.
	 */
	private static int _determineMeaningfulStart(List<String[]> data) {
		return _determineMeaningfulStart(data.toArray(new String[0][]));
	}

	/**
	 * Determines the starting row of meaningful data by finding the first row of
	 * numeric values that is preceded by a header row.
	 *
	 * @param data A 2D string array representing the raw file data.
	 * @return The 0-based index of the header row, or -1 if not found.
	 */
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

	/**
	 * Trims the loaded data to have a uniform number of columns based on the
	 * longest meaningful row and removes trailing nulls.
	 */
	private void _trimDataList() {
		int uniformSize = 0;
		for (int i = skipLines; i < mainData.length; i++) {
			uniformSize = Math.max(uniformSize, _meaningfulSize(mainData[i]));
		}

		for (int i = skipLines; i < mainData.length; i++) {
			mainData[i] = _uniformEndRemoveNulls(mainData[i], uniformSize);
		}

	}

	/**
	 * Calculates the "meaningful" size of an array by counting non-blank elements
	 * from the end.
	 *
	 * @param array The string array to measure.
	 * @return The number of meaningful elements.
	 */
	private static int _meaningfulSize(String[] array) {

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

	/**
	 * Safely converts any cell's content to a String.
	 * 
	 * @param cell      The cell to read from.
	 * @param evaluator The workbook's formula evaluator.
	 * @return The string representation of the cell's value.
	 */
	private String _getCellValueAsString(Cell cell, FormulaEvaluator evaluator) {
		if (cell == null) {
			return "";
		}

		// Use the evaluator on formula cells
		CellType cellType = cell.getCellType();
		if (cellType == CellType.FORMULA) {
			cellType = evaluator.evaluateInCell(cell).getCellType();
		}

		switch (cellType) {
		case STRING:
			return cell.getStringCellValue();
		case NUMERIC:
			// Check if the cell is a date format
			if (DateUtil.isCellDateFormatted(cell)) {
				return cell.getDateCellValue().toString(); // Or use a SimpleDateFormat
			} else {
				return Double.toString(cell.getNumericCellValue());
			}
		case BOOLEAN:
			return Boolean.toString(cell.getBooleanCellValue());
		case BLANK:
			return "";
		default:
			return ""; // Or handle other types like ERROR if needed
		}
	}

}
