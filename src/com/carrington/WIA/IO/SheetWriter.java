package com.carrington.WIA.IO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.carrington.WIA.Utils;

/**
 * Utility class for writing data to an Excel (.xlsx) spreadsheet using Apache
 * POI. It handles the creation of workbooks, sheets, cells, and applying
 * various styles.
 */
public class SheetWriter {

	private XSSFWorkbook workbook = null;
	private XSSFSheet sheet = null;
	private int nextRowNum = 0;

	/** Font style for main text. */
	public static final int FONT_MAIN = 0;
	/** Font style for bold text. */
	public static final int FONT_BOLD = 1;

	/** Font style for green-colored text (RGB 25, 107, 36). */
	public static final int FONT_GREEN = 2;
	/** Font style for red-colored text (RGB 255, 0, 0). */
	private static final int FONT_RED = 3;
	/** Font style for large title text. */
	public static final int FONT_TITLE_LARGE = 4;

	private final CellStyle styleMain;
	private final CellStyle styleMainBold;
	private final CellStyle styleGreen;
	private final CellStyle styleRed;
	private final CellStyle styleTitle;

	/**
	 * Constructs a new writer, initializing the workbook and predefined cell styles
	 * for fonts, colors, and patterns.
	 */
	public SheetWriter() {

		workbook = new XSSFWorkbook();

		styleMainBold = workbook.createCellStyle();
		XSSFFont fontMainBold = workbook.createFont();
		fontMainBold.setBold(true);
		fontMainBold.setColor(IndexedColors.BLACK.getIndex());
		fontMainBold.setFontHeight(12);
		fontMainBold.setFontName("Arial");
		styleMainBold.setFont(fontMainBold);
		styleMainBold.setFillPattern(FillPatternType.NO_FILL);
		styleMainBold.setFillForegroundColor(IndexedColors.WHITE.getIndex());

		styleMain = workbook.createCellStyle();
		XSSFFont fontMain = workbook.createFont();
		fontMain.setBold(false);
		fontMain.setColor(IndexedColors.BLACK.getIndex());
		fontMain.setFontHeight(12);
		fontMain.setFontName("Arial");
		styleMain.setFont(fontMain);
		styleMain.setFillPattern(FillPatternType.NO_FILL);
		styleMain.setFillForegroundColor(IndexedColors.WHITE.getIndex());

		styleGreen = workbook.createCellStyle();
		XSSFFont fontGreen = workbook.createFont();
		fontGreen.setBold(true);
		fontGreen
				.setColor(new XSSFColor(new byte[] { (byte) 25, (byte) 107, (byte) 36 }, new DefaultIndexedColorMap()));
		fontGreen.setFontHeight(12);
		fontGreen.setFontName("Arial");

		styleGreen.setFont(fontGreen);
		styleGreen.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		styleGreen.setFillForegroundColor(
				new XSSFColor(new byte[] { (byte) 193, (byte) 240, (byte) 200 }, new DefaultIndexedColorMap()));

		styleRed = workbook.createCellStyle();
		XSSFFont fontRed = workbook.createFont();
		fontRed.setBold(true);
		fontRed.setColor(new XSSFColor(new byte[] { (byte) 255, (byte) 0, (byte) 0 }, new DefaultIndexedColorMap()));

		fontRed.setFontHeight(12);
		fontRed.setFontName("Arial");
		styleRed.setFont(fontRed);
		styleRed.setFillPattern(FillPatternType.NO_FILL);
		styleMainBold.setFillForegroundColor(IndexedColors.WHITE.getIndex());

		styleTitle = workbook.createCellStyle();
		XSSFFont fontTitle = workbook.createFont();
		fontTitle.setBold(true);
		fontTitle.setFontHeight(16);
		fontTitle.setColor(new XSSFColor(new byte[] { (byte) 16, (byte) 72, (byte) 97 }, new DefaultIndexedColorMap()));
		fontTitle.setFontName("Arial");
		styleTitle.setFont(fontTitle);
		styleTitle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		styleTitle.setFillForegroundColor(
				new XSSFColor(new byte[] { (byte) 192, (byte) 230, (byte) 245 }, new DefaultIndexedColorMap()));
	}

	/**
	 * Creates a new sheet in the workbook with the specified name and resets the
	 * row counter
	 *
	 * @param name The name for the new sheet
	 */
	public void newSheet(String name) {
		if (sheet != null) {
			int numColumns = getNumColumns();
			if (numColumns > 0) {
				// sheet.autoSizeColumn(0);
			}
		}
		sheet = workbook.createSheet(name);
		nextRowNum = 0;
	}

	/**
	 * Gets the currently active sheet
	 *
	 * @return The current {@link XSSFSheet} instance
	 */
	public XSSFSheet getCurrentSheet() {
		return this.sheet;
	}

	/**
	 * Sets the widths for multiple columns based on a map of column indices to
	 * width values
	 *
	 * @param widths A map where keys are column indices and values are the desired
	 *               widths (in units of 1/256th of a character width)
	 */
	public void setCurrentWidths(Map<Integer, Integer> widths) {
		if (this.sheet != null) {
			for (Entry<Integer, Integer> en : widths.entrySet()) {
				sheet.setColumnWidth(en.getKey(), en.getValue() * 256);
			}
		}
	}

	/**
	 * Sets a uniform width for a specified range of columns.
	 *
	 * @param colStart The starting column index (inclusive)
	 * @param colEnd   The ending column index (inclusive)
	 * @param width    The width to apply to the columns (in units of 1/256th of a
	 *                 character width)
	 */
	public void setCurrentWidths(int colStart, int colEnd, int width) {
		if (this.sheet != null) {
			for (int i = colStart; i <= colEnd; i++) {
				sheet.setColumnWidth(i, width * 256);

			}
		}
	}

	/**
	 * Saves the workbook content to the specified file.
	 *
	 * @param file The file to save the workbook to. Must have a '.xlsx' extension.
	 * @throws IOException              if any error with writing
	 * @throws IllegalArgumentException if no file extension '.xlsx'
	 */
	public void saveFile(File file) throws IOException {

		int numColumns = getNumColumns();
		if (numColumns > 0) {
			// sheet.autoSizeColumn(0);
		}

		if (!Utils.hasOkayExtension(file, ".xlsx")) {
			throw new IllegalArgumentException("Only '.xlsx' extension allowed, but trid to save " + file.getName());
		}
		FileOutputStream out = new FileOutputStream(file);
		workbook.write(out);

		out.close();

	}

	/**
	 * Writes a single row of data to the sheet with default styling. Does not
	 * actually do any saving. That must be done with {@link #saveFile(File)}
	 *
	 * @param data An array of objects to be written as a row.
	 */
	public void writeData(Object[] data) {

		XSSFRow row = sheet.createRow(nextRowNum);
		if (data == null || data.length == 0)
			return;

		for (int i = 0; i < data.length; i++) {
			Object ob = data[i];
			XSSFCell cell = null;
			if (ob instanceof Double) {
				cell = row.createCell(i, CellType.NUMERIC);
				cell.setCellValue((Double) ob);

			} else if (ob instanceof Boolean) {
				cell = row.createCell(i, CellType.BOOLEAN);
				cell.setCellValue((Boolean) ob);

			} else if (ob instanceof String) {

				cell = row.createCell(i, CellType.STRING);
				cell.setCellValue((String) ob);

			} else {

				cell = row.createCell(i);
				cell.setCellValue(ob.toString());
			}

			cell.setCellStyle(styleMain);

		}
		nextRowNum++;

	}

	/**
	 * Writes a single row of data to the sheet with specified cell formatting. Does
	 * not actually do any saving. That must be done with {@link #saveFile(File)}
	 *
	 * @param data          An array of objects to be written as a row.
	 * @param fontSelection An array of integers corresponding to font styles for
	 *                      each cell.
	 */
	public void writeData(Object[] data, int[] fontSelection) {

		XSSFRow row = sheet.createRow(nextRowNum);

		if (data == null || data.length == 0)
			return;

		for (int i = 0; i < data.length; i++) {
			Object ob = data[i];
			XSSFCell cell = null;
			if (ob instanceof Double) {
				cell = row.createCell(i, CellType.NUMERIC);
				cell.setCellValue((Double) ob);

			} else if (ob instanceof Boolean) {
				cell = row.createCell(i, CellType.BOOLEAN);
				cell.setCellValue((Boolean) ob);

			} else if (ob instanceof String) {

				cell = row.createCell(i, CellType.STRING);
				cell.setCellValue((String) ob);

			} else {

				cell = row.createCell(i);
				cell.setCellValue(ob.toString());
			}

			if (i < fontSelection.length) {
				switch (fontSelection[i]) {
				case FONT_GREEN:
					cell.setCellStyle(styleGreen);
					break;
				case FONT_RED:
					cell.setCellStyle(styleRed);
					break;
				case FONT_TITLE_LARGE:
					cell.setCellStyle(styleTitle);
					break;
				case FONT_BOLD:
					cell.setCellStyle(styleMainBold);
					break;
				default:
					cell.setCellStyle(styleMain);
					break;
				}
			}

		}
		nextRowNum++;

	}

	/**
	 * Writes a multiple row of data to the sheet with default cell formatting. Does
	 * not actually do any saving. That must be done with {@link #saveFile(File)}
	 *
	 * @param rows An array of Object[] (rows) to be written.
	 */
	public void writeData(Object[][] rows) {

		for (Object[] row : rows) {
			writeData(row);
		}

	}

	/**
	 * Writes a single merged row with formatting. The entire row will be merged
	 * into one cell spanning from column 0 to mergeToColumn.
	 *
	 * @param value         the value to write in the merged cell
	 * @param mergeToColumn the last column index to merge (0-based, inclusive)
	 * @param fontSelection the formatting selection; one of
	 *                      {@link SheetWriter#FONT_MAIN},
	 *                      {@link SheetWriter#FONT_BOLD},
	 *                      {@link SheetWriter#FONT_GREEN},
	 *                      {@link SheetWriter#FONT_RED},
	 *                      {@link SheetWriter#FONT_TITLE_LARGE}
	 */
	public void writeMergedRow(Object value, int mergeToColumn, int fontSelection) {
		XSSFRow row = sheet.createRow(nextRowNum);
		XSSFCell cell = row.createCell(0);

		if (value instanceof Double) {
			cell.setCellValue((Double) value);
		} else if (value instanceof Boolean) {
			cell.setCellValue((Boolean) value);
		} else if (value instanceof String) {
			cell.setCellValue((String) value);
		} else {
			cell.setCellValue(value.toString());
		}

		// Apply cell style based on the provided fontSelection
		switch (fontSelection) {
		case FONT_GREEN:
			cell.setCellStyle(styleGreen);
			break;
		case FONT_RED:
			cell.setCellStyle(styleRed);
			break;
		case FONT_TITLE_LARGE:
			cell.setCellStyle(styleTitle);
			break;
		case FONT_BOLD:
			cell.setCellStyle(styleMainBold);
			break;
		default:
			cell.setCellStyle(styleMain);
			break;
		}

		// Merge cells from column 0 to mergeToColumn (inclusive)
		sheet.addMergedRegion(new CellRangeAddress(nextRowNum, nextRowNum, 0, mergeToColumn));
		nextRowNum++;
	}

	/**
	 * Writes multiple rows of data to the sheet with specified cell formatting.
	 * Does not actually do any saving. That must be done with
	 * {@link #saveFile(File)}
	 *
	 * @param rows       A 2D array of objects to write.
	 * @param formatting A 2D array of integers specifying the font style for each
	 *                   cell.
	 */
	public void writeData(Object[][] rows, int[][] formatting) {

		for (int i = 0; i < rows.length; i++) {
			int[] formattingForRow = i < formatting.length ? formatting[i] : new int[0];
			writeData(rows[i], formattingForRow);
		}

	}

	/**
	 * Calculates the maximum number of columns in the current sheet. This is a more
	 * time intense process than would be expected, because maximum column number
	 * isn't stored or retrievable from {@link XSSFSheet} by default. This method
	 * iterates over every row to find the max
	 *
	 * @return The number of columns as an integer.
	 */
	private int getNumColumns() {
		int colNumber = -1;
		Iterator<Row> rowItr = sheet.rowIterator();
		while (rowItr.hasNext()) {
			colNumber = Math.max(colNumber, rowItr.next().getLastCellNum());

		}

		return colNumber;
	}

}
