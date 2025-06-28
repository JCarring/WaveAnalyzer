package com.carrington.WIA.IO;

/**
 * Storage for typical file naming conventions
 */
public abstract class NamingConvention {
	
	/** The file path format for the serialized WIAData object, including wave selections. */
	public static final String PATHNAME_WIASerialize = "%s serialized.wia";
	
	/** The file path format for the printable SVG image of the final WIA plot. */
	public static final String PATHNAME_WIASVG = "%s printable.svg";
	
	/** The file path format for the printable TIFF image of the final WIA plot. */
	public static final String PATHNAME_WIATIFF = "%s printable.tiff";
	
	/** The file path format for the CSV file containing the calculated WIA metrics. */
	public static final String PATHNAME_WIACSV = "%s WIA.csv";

	/** The file path format for the CSV file containing the raw data of an ensembled beat. */
	public static final String PATHNAME_BeatSelectionsCSV = "%s - data.csv";
	
	/** The file path format for the SVG image of an individual beat selection. */
	public static final String PATHNAME_BeatSelectionsSVG = "%s - beat %s.svg";

	/** The file path format for the SVG image of the wave selections on the pressure-flow loop. */
	public static final String PATHNAME_WaveSelectionsSVG = "%s wave selections.svg";
	
}
