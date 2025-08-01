package com.carrington.WIA;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.MenuElement;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.carrington.WIA.DataStructures.HemoData;
import com.carrington.WIA.IO.Header;
import com.carrington.WIA.Math.FlowUnit;
import com.carrington.WIA.Math.PressureUnit;

/**
 * A collection of static utility methods used throughout the WIA application.
 */
public abstract class Utils {

	///////////////////////////////////////////////////////
	//
	// VARIABLES
	//
	///////////////////////////////////////////////////////

	// Standard icons to be used within buttons
	/** Icon for success (green check) */
	public static final ImageIcon IconSuccess;
	/** Icon for fail (red X) */
	public static final ImageIcon IconFail;
	/** Icon for success (blue file preview button icon) */
	public static final ImageIcon IconPreview;
	/** Icon for success (blue file preview button icon) */
	public static final ImageIcon IconPreviewHover;

	// Standard icon for be used for standalone question mark buttons which are used
	// to provide user help

	/** Question icon */
	public static final ImageIcon IconQuestion;
	/** Question icon, to be displayed when hovering */
	public static final ImageIcon IconQuestionHover;
	/** Question icon, larger */
	public static final ImageIcon IconQuestionLarger;
	/** Question icon, larger, to be displayed when hovering */
	public static final ImageIcon IconQuestionLargerHover;

	// Standard icon for save button

	/** Save icon */
	public static final ImageIcon IconSave;
	/** Save icon, to be displayed when hovering */
	public static final ImageIcon IconSaveHover;

	// Standard fonts used by this program which are scaled to the size of the
	// user's screen

	/** Arial, bold, size 24 pt */
	private static final Font fontTitle = new Font("Arial", Font.BOLD, 24);

	/** Arial, bold, size 18 pt */
	private static final Font fontSubtitle = new Font("Arial", Font.BOLD, 18);

	/** Arial, plain, size 18 pt */
	private static final Font fontSubtitlePlain = new Font("Arial", Font.PLAIN, 18);

	/** Arial, bold, size 14 pt */
	private static final Font fontSubtitleSub = new Font("Arial", Font.BOLD, 16);

	/** Arial, plain, size 14 */
	private static final Font fontNormalPlain = new Font("Arial", Font.PLAIN, 14);

	/** Arial, bold, size 14 */
	private static final Font fontNormalBold = new Font("Arial", Font.BOLD, 14);

	/** Arial, plain, size 10 */
	private static final Font fontSmall = new Font("Arial", Font.PLAIN, 10);

	static {

		ImageIcon iconS = null;
		ImageIcon iconF = null;
		ImageIcon iconP = null;
		ImageIcon iconPH = null;
		ImageIcon iconQ = null;
		ImageIcon iconQL = null;
		ImageIcon iconQH = null;
		ImageIcon iconQLH = null;
		ImageIcon iconSave = null;
		ImageIcon iconSaveH = null;

		try {
			BufferedImage successIconRaw = ImageIO
					.read(Utils.class.getResourceAsStream("/resources/images/success.png"));
			BufferedImage failIconRaw = ImageIO.read(Utils.class.getResourceAsStream("/resources/images/fail.png"));
			BufferedImage previewIconRaw = ImageIO
					.read(Utils.class.getResourceAsStream("/resources/images/preview.png"));
			BufferedImage previewHoverIconRaw = ImageIO
					.read(Utils.class.getResourceAsStream("/resources/images/preview_hover.png"));

			BufferedImage qIconRaw = ImageIO.read(Utils.class.getResourceAsStream("/resources/images/question.png"));
			BufferedImage qIconRawH = ImageIO
					.read(Utils.class.getResourceAsStream("/resources/images/question_light.png"));
			BufferedImage saveIconRaw = ImageIO.read(Utils.class.getResourceAsStream("/resources/images/save.png"));
			BufferedImage saveIconRawH = ImageIO
					.read(Utils.class.getResourceAsStream("/resources/images/save_hover.png"));

			iconS = new ImageIcon(successIconRaw.getScaledInstance(15, 15, Image.SCALE_SMOOTH));
			iconF = new ImageIcon(failIconRaw.getScaledInstance(15, 15, Image.SCALE_SMOOTH));
			iconP = new ImageIcon(previewIconRaw.getScaledInstance(20, 20, Image.SCALE_SMOOTH));
			iconPH = new ImageIcon(previewHoverIconRaw.getScaledInstance(20, 20, Image.SCALE_SMOOTH));
			iconQ = new ImageIcon(qIconRaw.getScaledInstance(12, 12, Image.SCALE_SMOOTH));
			iconQL = new ImageIcon(
					qIconRaw.getScaledInstance(fontSubtitle.getSize(), fontSubtitle.getSize(), Image.SCALE_SMOOTH));
			iconQH = new ImageIcon(qIconRawH.getScaledInstance(12, 12, Image.SCALE_SMOOTH));
			iconQLH = new ImageIcon(
					qIconRawH.getScaledInstance(fontSubtitle.getSize(), fontSubtitle.getSize(), Image.SCALE_SMOOTH));
			iconQL = new ImageIcon(
					qIconRaw.getScaledInstance(fontSubtitle.getSize(), fontSubtitle.getSize(), Image.SCALE_SMOOTH));

			iconSave = new ImageIcon(saveIconRaw);
			iconSaveH = new ImageIcon(saveIconRawH);

		} catch (Exception e) {
			/** won't happen **/
		}

		IconSuccess = iconS;
		IconFail = iconF;
		IconQuestion = iconQ;
		IconQuestionLarger = iconQL;
		IconQuestionHover = iconQH;
		IconQuestionLargerHover = iconQLH;
		IconPreview = iconP;
		IconPreviewHover = iconPH;
		IconSave = iconSave;
		IconSaveHover = iconSaveH;

	}

	/**
	 * Default value for an enabled panel (RGB = 220, 220, 220)
	 */
	public static final Color colorPnlEnabled = new Color(220, 220, 220);

	/**
	 * Default value for an disabled panel, {@link Color#LIGHT_GRAY}
	 */
	public static final Color colorPnlDisabled = Color.LIGHT_GRAY;

	/** A custom purple color used for UI elements. */
	public static final Color colorPurple = new Color(161, 0, 132, 255);
	/** A custom darker purple color used for UI elements. */
	public static final Color colorPurpleDarker = colorPurple.darker();

	/**
	 * Variable used in {@link #showMessage(int, String, Component)} to denote
	 * informational message
	 */
	public static final int INFO = JOptionPane.INFORMATION_MESSAGE;

	/**
	 * Variable used in {@link #showMessage(int, String, Component)} to denote
	 * warning message
	 */
	public static final int WARN = JOptionPane.WARNING_MESSAGE;

	/**
	 * Variable used in {@link #showMessage(int, String, Component)} to denote error
	 * message
	 */
	public static final int ERROR = JOptionPane.ERROR_MESSAGE;

	///////////////////////////////////////////////////////
	//
	// METHODS
	//
	///////////////////////////////////////////////////////

	/**
	 * @return Arial, bold, screen height / 50 pixel size (minimum 24)
	 */
	public static Font getTitleFont() {
		return fontTitle;
	}

	/**
	 * @return Arial, bold, screen height / 75 pixel size (minimum 18)
	 */
	public static Font getSubTitleFont() {
		return fontSubtitle;
	}

	/**
	 * @return Arial, bold, screen height / 83 pixel size (minimum 14)
	 */
	public static Font getSubTitleSubFont() {
		return fontSubtitleSub;
	}

	/**
	 * @return subtitle font, but plain
	 */
	public static Font getSubTitleFontPlain() {
		return fontSubtitlePlain;
	}

	/**
	 * @return Arial, bold depending on params, screen height / 90 pixel size
	 *         (minimum 14)
	 */
	public static Font getTextFont(boolean bold) {
		return bold ? fontNormalBold : fontNormalPlain;
	}

	/**
	 * @return Arial, bold depending on params, screen height / 110 pixel size
	 *         (minimum 12)
	 */
	public static Font getSmallTextFont() {
		return fontSmall;
	}

	/**
	 * Returns an int array with two elements. For element is the rise. Second
	 * element is the width if a string entry was provided.
	 */
	public static int[] getFontParams(Font font, String entry) {
		Canvas c = new Canvas();
		FontMetrics fm = c.getFontMetrics(font);

		int rise = fm.getHeight();
		int width = entry != null ? fm.stringWidth(entry) : -1;
		return new int[] { rise, width };
	}

	/**
	 * @return maximum realistic app size considering menu bars, etc
	 */
	public static Rectangle getMaxAppSize() {
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
	}

	/**
	 * @param list of Double
	 * @return primitive array, from list of Double object
	 */
	public static double[] toArray(List<Double> list) {
		return ArrayUtils.toPrimitive(list.toArray(new Double[0]));
	}

	/**
	 * Checks if there is a key within the specified map, which matches the
	 * specified query in a case insensitive manner.
	 * 
	 * @param <E>   the type parameter for the value of the map
	 * @param map   The map of interest
	 * @param query Value to look for
	 * @return true if found
	 */
	public static <E> E getCaseInsensitive(Map<String, E> map, String query) {
		if (query == null) {
			return map.get(null);
		}
		for (Entry<String, E> mapEn : map.entrySet()) {
			if (query.equalsIgnoreCase(mapEn.getKey())) {
				return mapEn.getValue();
			}
		}
		return null;
	}

	/**
	 * Shows a properly sized and positioned popup message to the user with the
	 * specified type and message
	 * 
	 * @param type   The type of message, one of
	 *               {@link Utils#INFO},
	 *               {@link Utils#WARN}, or {@link Utils#ERROR}
	 * @param msg    The message to display; can utilized HTML
	 * @param parent component used to positioning this popup
	 * @throws IllegalArgumentException if type is not valid
	 */
	public static void showMessage(int type, String msg, Component parent) throws IllegalArgumentException {

		String title;
		switch (type) {
		case Utils.INFO:
			title = "Info";
			break;
		case Utils.ERROR:
			title = "Error";
			break;
		case Utils.WARN:
			title = "Warning";
			break;
		default:
			throw new IllegalArgumentException("Invalid type of message");
		}

		Font font = fontNormalPlain;
		int padding = 10;

		// Usable screen area (excludes dock/taskbar/menu bar)
		Rectangle usableBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int screenW = usableBounds.width;
		int screenH = usableBounds.height;

		int baseMaxWidth = screenW / 3;
		int baseMaxHeight = screenH / 3;

		int absoluteMaxWidth = (int) (screenW * 0.9);
		int absoluteMaxHeight = (int) (screenH * 0.9);

		// Use JEditorPane for HTML rendering

		Color bg = UIManager.getColor("OptionPane.background");
		if (bg == null)
			bg = UIManager.getColor("Panel.background"); // fallback

		JEditorPane editorPane = new JEditorPane("text/html", msg);
		editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		editorPane.setFont(font); // may not affect HTML unless styled in the HTML itself
		editorPane.setEditable(false);
		editorPane.setOpaque(true);
		editorPane.setBackground(bg); // match OptionPane
		editorPane.setForeground(UIManager.getColor("OptionPane.foreground"));

		// Initial width guess based on plain text version
		// Start with no wrapping to get minimum width required for a single line
		// Force layout to calculate preferred size for one-line fit
		editorPane.setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
		Dimension singleLineSize = editorPane.getPreferredSize();
		int minWidth = Math.min(singleLineSize.width + padding, absoluteMaxWidth);

		// Try to see if wrapping occurs at minWidth
		editorPane.setSize(minWidth, Integer.MAX_VALUE);
		Dimension minSize = editorPane.getPreferredSize();
		boolean wraps = minSize.height > singleLineSize.height;

		int width = minWidth;
		int height = minSize.height + padding;

		if (wraps) {
			double scaleFactor = 1.0;
			final double step = 0.1;

			while (true) {
				int currentMaxWidth = Math.min((int) (baseMaxWidth * scaleFactor), absoluteMaxWidth);
				int currentMaxHeight = Math.min((int) (baseMaxHeight * scaleFactor), absoluteMaxHeight);

				editorPane.setSize(currentMaxWidth, Integer.MAX_VALUE);
				Dimension prefSize = editorPane.getPreferredSize();
				height = prefSize.height + padding;

				if (height <= currentMaxHeight) {
					width = currentMaxWidth;
					break;
				}

				if (currentMaxWidth >= absoluteMaxWidth && currentMaxHeight >= absoluteMaxHeight) {
					width = currentMaxWidth;
					height = currentMaxHeight;
					break;
				}

				scaleFactor += step;
			}
		}

		// add small buffer to avoid scrollbar from off-by-1 sizing
		int buffer = 5;

		JScrollPane scrollPane = new JScrollPane(editorPane);
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBackground(bg); // match option pane

		// add buffer to width/height to preempt scrollbars
		scrollPane.setPreferredSize(new Dimension(width + buffer, height + buffer));

		JOptionPane.showMessageDialog(parent, scrollPane, title, type);
	}

	/**
	 * Shows a properly sized and positioned popup message to the user with the
	 * specified type and message. It expands vertically first.
	 * 
	 * @param type   The type of message, one of
	 *               {@link Utils#INFO},
	 *               {@link Utils#WARN}, or {@link Utils#ERROR}
	 * @param msg    The message to display; can utilized HTML
	 * @param parent component used to positioning this popup
	 * @throws IllegalArgumentException if type is not valid
	 */
	public static void showMessageTallFirst(int type, String msg, Component parent) throws IllegalArgumentException {
		String title;
		switch (type) {
		case Utils.INFO:
			title = "Info";
			break;
		case ERROR:
			title = "Error";
			break;
		case Utils.WARN:
			title = "Warning";
			break;
		default:
			throw new IllegalArgumentException("Invalid type of message");
		}

		Font font = fontNormalPlain; // reuse your font
		int padding = 10;
		int buffer = 5; // small buffer to avoid off-by-1 scrollbars

		// Usable screen area (excludes dock/taskbar/menu bar)
		Rectangle usableBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int screenW = usableBounds.width;
		int screenH = usableBounds.height;

		// Reasonable cap (you can tweak these)
		int absoluteMaxWidth = (int) (screenW * 0.90);
		int absoluteMaxHeight = (int) (screenH * 0.90);

		// Prepare HTML viewer
		Color bg = UIManager.getColor("OptionPane.background");
		if (bg == null)
			bg = UIManager.getColor("Panel.background");

		JEditorPane editorPane = new JEditorPane("text/html", msg);
		editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		editorPane.setFont(font); // may not affect HTML unless styled inside HTML
		editorPane.setEditable(false);
		editorPane.setOpaque(true);
		editorPane.setBackground(bg);
		editorPane.setForeground(UIManager.getColor("OptionPane.foreground"));

		// ---- Step 1: compute intrinsic (no-wrap) preferred width of the HTML ----
		// Ask the HTML view how wide it *wants* to be if not constrained.
		// We do this via the root View spans.
		editorPane.addNotify(); // ensure UI installed so getUI() returns a valid one
		javax.swing.plaf.TextUI ui = editorPane.getUI();
		float intrinsicWidth;
		try {
			javax.swing.text.View root = ui.getRootView(editorPane);
			// Size to "infinite" width so the view computes its natural span.
			root.setSize(Float.MAX_VALUE, 0f);
			intrinsicWidth = root.getPreferredSpan(javax.swing.text.View.X_AXIS);
			if (Float.isNaN(intrinsicWidth) || intrinsicWidth <= 0f) {
				// Fallback: compute from preferred size when unconstrained
				editorPane.setSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
				intrinsicWidth = editorPane.getPreferredSize().width;
			}
		} catch (Exception ex) {
			// Robust fallback path
			editorPane.setSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
			intrinsicWidth = editorPane.getPreferredSize().width;
		}

		int targetWidth = Math.min((int) Math.ceil(intrinsicWidth) + padding, absoluteMaxWidth);

		// ---- Step 2: lay out the editor at that width and measure height ----
		editorPane.setSize(new Dimension(targetWidth, Integer.MAX_VALUE));
		Dimension pref = editorPane.getPreferredSize();
		int targetHeight = Math.min(pref.height + padding, absoluteMaxHeight);

		// ---- Step 3: put in a scroll pane: vertical as needed, horizontal never ----
		JScrollPane scrollPane = new JScrollPane(editorPane);
		scrollPane.setBorder(null);
		scrollPane.getViewport().setBackground(bg);
		scrollPane.setBackground(bg);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(targetWidth + buffer, targetHeight + buffer));

		JOptionPane.showMessageDialog(parent, scrollPane, title, type);
	}

	/**
	 * Shows an error message and then quits the program after the user selects OK
	 * 
	 * @param parent component to position this message within
	 */
	public static void showErrorAndQuit(String msg, Component parent) {
		JOptionPane.showMessageDialog(parent, msg, "Error", Utils.ERROR);
		System.exit(0);

	}

	/**
	 * Requests an integer inut from the user
	 * 
	 * @param msg    the prompt
	 * @param parent the component to position dialog within
	 * @return integer selected, or null if it was not an integer or was blank
	 */
	public static Integer promptIntegerNumber(String msg, Component parent) {
		String text = JOptionPane.showInputDialog(parent, msg, "Enter Number", Utils.INFO);
		if (text == null || text.isBlank())
			return null;
		try {
			return Integer.parseInt(text);
		} catch (Exception e) {
			return null;
		}

	}

	/**
	 * Prompts for text. Returns null if canceled or there was no input.
	 */
	public static String promptTextInput(String msg, Component parent) {
		String text = JOptionPane.showInputDialog(parent, msg, "Enter Number", Utils.INFO);
		if (text == null || text.isBlank())
			return null;
		else
			return text;

	}

	/**
	 * Prints data to console. This can also be directly performed from
	 * {@link HemoData} as well
	 * 
	 * @param data        the data or interest (keys of data {@link Header}, values
	 *                    are array of data points)
	 * @param colsToLimit maximum number of columns to process
	 */
	public static void printDataToConsole(LinkedHashMap<Header, ArrayList<Double>> data, int colsToLimit) {
		if (data == null) {
			System.out.println("no data");
			return;
		}
		int counter = 1;
		for (Entry<Header, ArrayList<Double>> en : data.entrySet()) {
			if (colsToLimit > 0) {
				if (counter > colsToLimit)
					break;
			}

			System.out.print(en.getKey() + " " + en.getValue().size() + " : ");
			for (Double d : en.getValue()) {
				System.out.print(d + ", ");
			}
			System.out.println();
			counter++;
		}
	}

	/**
	 * Converts Object array to primitive array
	 */
	public static boolean[] toPrimitive(Boolean[] d) {
		return ArrayUtils.toPrimitive(d);
	}

	/**
	 * Converts List to primitive array
	 */
	public static boolean[] toPrimitiveBoolean(List<Boolean> d) {
		return ArrayUtils.toPrimitive(d.toArray(new Boolean[0]));
	}

	/**
	 * Converts Object array to primitive array
	 */
	public static double[] toPrimitive(Double[] d) {
		return ArrayUtils.toPrimitive(d);
	}

	/**
	 * Converts List to primitive array
	 */
	public static double[] toPrimitiveDouble(List<Double> d) {
		return ArrayUtils.toPrimitive(d.toArray(new Double[0]));
	}

	/**
	 * Converts Object array to primitive array
	 */
	public static int[] toPrimitive(Integer[] i) {
		return ArrayUtils.toPrimitive(i);
	}

	/**
	 * Converts List to primitive array
	 */
	public static int[] toPrimitiveInteger(List<Integer> i) {
		return ArrayUtils.toPrimitive(i.toArray(new Integer[0]));
	}

	/**
	 * counts the number of true elements in the passed array
	 */
	public static int countTrue(boolean[] b) {
		int count = 0;
		for (int i = 0; i < b.length; i++) {
			if (b[i])
				count++;
		}
		return count;
	}

	/**
	 * Request selection from list of items.
	 * 
	 * @param <E>          Type of item
	 * @param options      Items for selection
	 * @param msg          Message prompt
	 * @param defaultIndex Index of item to select by default, or -1 for no
	 *                     selection
	 * @param parent       The component to place this dialog within
	 * @return The object selected, or null if nothing selected
	 */
	@SuppressWarnings("unchecked")
	public static <E> E promptSelection(List<E> options, String msg, int defaultIndex, Component parent) {
		Object[] obOptions = options.toArray();
		Object ob = JOptionPane.showInputDialog(parent, msg, "Select Column", JOptionPane.PLAIN_MESSAGE, null,
				obOptions, defaultIndex > -1 && defaultIndex < obOptions.length ? obOptions[defaultIndex] : null);
		if (ob == null)
			return null;
		else
			return (E) ob;
	}

	/**
	 * Request selection from list of items.
	 * 
	 * @param <E>          Type of item
	 * @param options      Items for selection
	 * @param msg          Message prompt
	 * @param defaultIndex Index of item to select by default, or -1 for no
	 *                     selection
	 * @param parent       The component to place this dialog within
	 * @return The object selected, or null if nothing selected
	 */
	@SuppressWarnings("unchecked")
	public static <E> E promptSelection(E[] options, String msg, int defaultIndex, Component parent) {
		Object ob = JOptionPane.showInputDialog(parent, msg, "Select Column", JOptionPane.PLAIN_MESSAGE, null, options,
				defaultIndex > -1 && defaultIndex < options.length ? options[defaultIndex] : null);
		if (ob == null)
			return null;
		else
			return (E) ob;
	}

	/**
	 * Request that user select OK or CANCEL.
	 * 
	 * @param title  title of the dialog
	 * @param msg    message to the user
	 * @param parent component to display this prompt within
	 * @return true if selected OK, false otherwise
	 */
	public static boolean confirmAction(String title, String msg, Component parent) {

		int result = JOptionPane.showConfirmDialog(parent, msg, title, JOptionPane.OK_CANCEL_OPTION,
				Utils.WARN);

		if (result == JOptionPane.OK_OPTION) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Start directory can be null - in that case will default to user's home folder
	 * 
	 * @return file if selected and is valid, otherwise null
	 */
	public static File promptUserForFile(String title, String startDirectory, String... extensions) {

		FileDialog fd = new FileDialog(new Frame(), title, FileDialog.LOAD);

		File startDir = getFileFromString(startDirectory);
		if (startDir == null) {
			fd.setDirectory(System.getProperty("user.home"));

		} else {
			fd.setDirectory(startDir.getPath());
		}
		fd.setFilenameFilter(new CustomFilter(extensions));
		fd.setMultipleMode(false);
		fd.setVisible(true);

		File[] file = fd.getFiles();

		if (file == null || file.length == 0)
			return null;
		else {
			return file[0];
		}

	}

	/**
	 * Start directory can be null - in that case will default to user's home folder
	 * 
	 * @return file if selected and is valid, otherwise null
	 */
	public static File promptUserForDirectory(String title, String startDirectory) {

		JFileChooser jfc = null;

		File startDir = getFileFromString(startDirectory);
		if (startDir == null) {
			jfc = new JFileChooser();
		} else {
			jfc = new JFileChooser(startDir, FileSystemView.getFileSystemView());
		}
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.showOpenDialog(null);

		File file = jfc.getSelectedFile();

		if (file == null)
			return null;
		else {
			return file;
		}

	}

	/**
	 * Checks if file has one of the listed extensions.
	 * 
	 * @param file       File to check
	 * @param extensions Extensions to check, in format ".extension"
	 * @return true if okay extension, false otherwise
	 */
	public static boolean hasOkayExtension(File file, String... extensions) {
		String name = file.getName();
		if (!name.contains("."))
			return true;

		String thisExt = name.substring(name.lastIndexOf("."), name.length());
		boolean okay = false;
		for (String str : extensions) {
			if (thisExt.equalsIgnoreCase(str)) {
				okay = true;
				break;
			}
		}
		return okay;
	}

	/**
	 * Start directory can be null - in that case will default to user's home folder
	 * 
	 * @return file if selected and is valid, otherwise null
	 */
	public static File promptUserForFileName(String title, String startDirectory, String fileName,
			String... extensions) {

		FileDialog fd = new FileDialog(new Frame(), title, FileDialog.SAVE);

		File startDir = getFileFromString(startDirectory);
		if (startDir == null) {
			fd.setDirectory(System.getProperty("user.home"));

		} else {
			fd.setDirectory(startDir.getPath());
		}
		fd.setFilenameFilter(new CustomFilter(extensions));
		if (fileName != null) {
			fd.setFile(fileName);
		}
		fd.setMultipleMode(false);
		fd.setVisible(true);

		File[] file = fd.getFiles();

		if (file == null || file.length == 0)
			return null;
		else {
			return file[0];
		}
	}

	/**
	 * Removes file name extension (i.e. "test.txt" returns as "test")
	 * 
	 * @param fileName Name of file
	 * @return file name without extension
	 */
	public static String removeExtension(String fileName) {
		return FilenameUtils.removeExtension(fileName);
	}

	/**
	 * Adds the specified extension if it does not already exist.
	 * 
	 * @param file      file to check
	 * @param extension extension, does not need to include the preceding period
	 *                  (however it can)
	 * @return new {@link File} with the correct extension
	 */
	public static File appendExtensionIfDoesNotHaveExt(File file, String extension) {
		if (file.getName().contains("."))
			return file;

		if (!extension.startsWith("."))
			extension = "." + extension;

		file = new File(file.getPath() + extension);
		return file;
	}

	/**
	 * Given a string, will remove trailing zeros. If the string is not a decimal
	 * number, then this will simply return what was passed in.
	 */
	public static String removeTrailingZeros(String string) {

		return string.indexOf(".") < 0 ? string : string.replaceAll("0*$", "").replaceAll("\\.$", "");

	}

	/**
	 * Returns the range of the list of Double. The returned array has size 2, with
	 * the first index (0) being the minimum of the range and second index (1) being
	 * the maximum of the range
	 * 
	 * @param list the list to check
	 * @return range as described above
	 */
	public static double[] getBounds(List<Double> list) {
		if (list == null)
			return null;

		double min = Double.NaN;
		double max = Double.NaN;

		for (Double d : list) {
			if (Double.isNaN(min) || Double.isNaN(max)) {
				min = d;
				max = d;
			} else {
				if (d < min) {
					min = d;
				} else if (d > max) {
					max = d;
				}
			}
		}

		return new double[] { min, max };

	}

	/**
	 * Finds the minimum and maximum values across one or more double arrays.
	 *
	 * @param arrays A variable number of double arrays to search.
	 * @return A double array of size 2, where index 0 is the overall minimum and
	 *         index 1 is the overall maximum.
	 */
	public static double[] getBounds(double[]... arrays) {
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;

		for (double[] array : arrays) {

			for (int i = 0; i < array.length; i++) {
				if (array[i] > max) {
					max = array[i];
				}

				if (array[i] < min) {
					min = array[i];
				}
			}

		}

		return new double[] { min, max };
	}

	/**
	 * Finds the bounds (min and max) of the array with the smallest difference
	 * between max and min.
	 *
	 * @param arrays A variable number of double arrays to search.
	 * @return A double array of size 2, where index 0 is the min and index 1 is the
	 *         max of the array with the smallest range.
	 */
	public static double[] getMinimumBounds(double[]... arrays) {
		double[] result = null;
		double minRange = Double.MAX_VALUE;

		for (double[] array : arrays) {
			if (array.length == 0)
				continue;

			double min = Double.MAX_VALUE;
			double max = -Double.MAX_VALUE;

			for (double v : array) {
				if (v < min)
					min = v;
				if (v > max)
					max = v;
			}

			double range = max - min;
			if (range < minRange) {
				minRange = range;
				result = new double[] { min, max };
			}
		}

		return result;
	}

	/**
	 * Calculates the total absolute range (max - min) across one or more double
	 * arrays.
	 *
	 * @param arrays A variable number of double arrays to process.
	 * @return The absolute difference between the overall maximum and minimum
	 *         values.
	 */
	public static double getRange(double[]... arrays) {
		double[] bounds = getBounds(arrays);
		return bounds[1] - bounds[0];
	}

	/**
	 * Calculates the minimum range (max - min) across one or more double arrays.
	 *
	 * @param arrays A variable number of double arrays to process.
	 * @return The smallest difference between maximum and minimum in one of the
	 *         arrays
	 */
	public static double getMinimumRange(double[]... arrays) {
		double[] bounds = getMinimumBounds(arrays);
		return bounds[1] - bounds[0];
	}

	/**
	 * Finds the maximum value within all the passed arrays
	 * 
	 * @param arrays arrays to check
	 * @return max value
	 */
	public static double max(double[]... arrays) {

		double max = Double.NEGATIVE_INFINITY;

		for (double[] array : arrays) {
			for (int i = 0; i < array.length; i++) {
				if (array[i] > max) {
					max = array[i];
				}
			}
		}

		return max;
	}

	/**
	 * Finds the minimum value within all the passed arrays
	 * 
	 * @param arrays arrays to check
	 * @return min value
	 */
	public static double min(double[]... arrays) {

		double min = Double.POSITIVE_INFINITY;

		for (double[] array : arrays) {
			for (int i = 0; i < array.length; i++) {
				if (array[i] < min) {
					min = array[i];
				}
			}
		}
		return min;

	}

	/**
	 * Finds the minimum or maximum value in an array between the given indices.
	 *
	 * @param arr     the array of integers
	 * @param low     the starting index (inclusive)
	 * @param high    the ending index (inclusive)
	 * @param findMin if true, the method returns the minimum; if false, the maximum
	 * @return the minimum or maximum value in the specified range
	 * @throws IllegalArgumentException if the array is null, empty, or the bounds
	 *                                  are invalid
	 */
	public static double findMinMax(double[] arr, int low, int high, boolean findMin) {
		if (arr == null || arr.length == 0) {
			throw new IllegalArgumentException("Array must not be null or empty.");
		}
		if (low < 0 || high >= arr.length || low > high) {
			throw new IllegalArgumentException("Invalid bounds specified.");
		}

		double result = arr[low];
		for (int i = low + 1; i <= high; i++) {
			if (findMin) {
				if (arr[i] < result) {
					result = arr[i];
				}
			} else {
				if (arr[i] > result) {
					result = arr[i];
				}
			}
		}
		return result;
	}

	/**
	 * Finds the index of the minimum or maximum value in an array between the given
	 * indices.
	 *
	 * @param arr     the array of doubles
	 * @param low     the starting index (inclusive)
	 * @param high    the ending index (inclusive)
	 * @param findMin if true, returns the index of the minimum value; if false,
	 *                returns the index of the maximum value
	 * @return the index of the minimum or maximum value in the specified range
	 * @throws IllegalArgumentException if the array is null, empty, or the bounds
	 *                                  are invalid
	 */
	public static int findMinMaxIndex(double[] arr, int low, int high, boolean findMin) {
		if (arr == null || arr.length == 0) {
			throw new IllegalArgumentException("Array must not be null or empty.");
		}
		if (low < 0 || high >= arr.length || low > high) {
			throw new IllegalArgumentException("Invalid bounds specified.");
		}

		int resultIndex = low;
		for (int i = low + 1; i <= high; i++) {
			if (findMin) {
				if (arr[i] < arr[resultIndex]) {
					resultIndex = i;
				}
			} else {
				if (arr[i] > arr[resultIndex]) {
					resultIndex = i;
				}
			}
		}
		return resultIndex;
	}

	/**
	 * Finds the absolute maximum Y value for the specified input. Returns an array
	 * specifying this value as well as the X value associated with it.
	 * 
	 * @param x input of X values
	 * @param y input of Y values
	 * @return array of size two, [<x value of max Y>,<the max Y>]
	 */
	public static double[] absoluteMax(double[] x, double[] y) {

		double min = Double.POSITIVE_INFINITY;
		double minTime = Double.NaN;
		double max = Double.NEGATIVE_INFINITY;
		double maxTime = Double.NaN;

		for (int i = 0; i < y.length; i++) {
			if (y[i] < min) {
				min = y[i];
				minTime = x[i];
			}

			if (y[i] > max) {
				max = y[i];
				maxTime = x[i];
			}

		}

		if (Math.abs(min) > Math.abs(max)) {
			return new double[] { minTime, min };
		} else {
			return new double[] { maxTime, max };
		}

	}

	/**
	 * A custom file name filter for use with {@link FileDialog} to filter by
	 * extension.
	 */
	private static class CustomFilter implements FilenameFilter {

		/** list of extensions permited by this filter */
		private String[] extensions = null;

		/**
		 * Creates a new filter.
		 * 
		 * @param extensions The allowed extensions, e.g., ".txt", ".csv".
		 */
		public CustomFilter(String[] extensions) {
			this.extensions = extensions;
			if (extensions != null) {
				for (int i = 0; i < extensions.length; i++) {
					extensions[i] = extensions[i].toLowerCase();
				}
			}
		}

		/**
		 * Determines whether a given file should be accepted. Looks at the extension of
		 * the file name.
		 *
		 * @param dir  the directory in which the file was found
		 * @param name the name of the file
		 * @return true if the file is accepted; false otherwise
		 */
		public boolean accept(File dir, String name) {
			if (extensions == null) {
				return true;
			} else {
				String nameLower = name.toLowerCase();
				for (String extension : extensions) {
					if (nameLower.endsWith(extension))
						return true;
				}
			}
			return false;
		}
	}

	/**
	 * @return copy of array in pascals (input in mmHg)
	 */
	public static double[] convertToPascals(double[] data) {
		double[] copyOfArray = Arrays.copyOf(data, data.length);

		BigDecimal pascalConversionFactor = divide(101325d, 760d);
		for (int i = 0; i < data.length; i++) {

			copyOfArray[i] = multiply(BigDecimal.valueOf(copyOfArray[i]), pascalConversionFactor).doubleValue();
		}
		return copyOfArray;
	}

	/**
	 * @return cvalue in Pascals
	 */
	public static double convertToPascals(double value) {

		BigDecimal pascalConversionFactor = divide(101325d, 760d);

		return multiply(BigDecimal.valueOf(value), pascalConversionFactor).doubleValue();

	}

	/**
	 * @return copy of array in mmHg converted from Pascals
	 */
	public static double[] convertPascalsToMMHG(double[] data) {
		double[] copyOfArray = Arrays.copyOf(data, data.length);
		BigDecimal pascalConversionFactor = divide(101325d, 760d);
		for (int i = 0; i < data.length; i++) {
			copyOfArray[i] = divide(BigDecimal.valueOf(copyOfArray[i]), pascalConversionFactor).doubleValue();
		}
		return copyOfArray;
	}

	/**
	 * Converts the input value from pascals to mmHg
	 */
	public static double convertPascalsToMMHG(double value) {
		return divide(BigDecimal.valueOf(value), divide(101324d, 760d)).doubleValue();
	}

	/**
	 * @return a copy of the input array with each element divided by specified
	 *         value
	 */
	public static double[] multiplyArray(double[] data, double value) {
		double[] copyOfArray = Arrays.copyOf(data, data.length);
		for (int i = 0; i < copyOfArray.length; i++) {
			copyOfArray[i] = multiply(copyOfArray[i], value).doubleValue();
		}
		return copyOfArray;
	}

	/**
	 * @return a copy of the input array with each element multiplied by specified
	 *         value
	 */
	public static double[] divideArray(double[] data, double value) {
		double[] copyOfArray = Arrays.copyOf(data, data.length);

		for (int i = 0; i < copyOfArray.length; i++) {
			copyOfArray[i] = divide(copyOfArray[i], value).doubleValue();
		}
		return copyOfArray;
	}

	/**
	 * @param array input, whose values must be ascending (i.e. time)
	 * @return new array, shifted to zero (first index of array will be value of
	 *         zero).
	 */
	public static double[] shiftToZero(double[] array) {
		if (array[0] < 0) {
			return Arrays.copyOf(array, array.length);

		}

		BigDecimal subtractor = BigDecimal.valueOf(array[0]);

		double[] zerodArray = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			zerodArray[i] = BigDecimal.valueOf(array[i]).subtract(subtractor).doubleValue();
		}
		return zerodArray;
	}

	/**
	 * Returns a String with all elements of the specified list separate by a comma
	 * and space. It calls the {@code toString()} method for each object in the list
	 * 
	 * @param <E>  type of list
	 * @param list elements to concatenate into a string
	 * @return concatenated string
	 */
	public static <E> String listToString(List<E> list) {

		StringBuilder sb = new StringBuilder();
		String comma = "";
		for (E item : list) {
			sb.append(comma).append(item.toString());
			comma = ", ";
		}

		return sb.toString();
	}

	/**
	 * Converts the initial part of a boolean array to its string representation.
	 * 
	 * @param array          The source array.
	 * @param numberToResult The number of elements from the start of the array to
	 *                       include.
	 * @return A string representation of the subarray.
	 */
	public static String getStringFromArray(boolean[] array, int numberToResult) {
		return Arrays.toString(ArrayUtils.subarray(array, 0, numberToResult));
	}

	/**
	 * Converts the initial part of a double array to its string representation.
	 * 
	 * @param array          The source array.
	 * @param numberToResult The number of elements from the start of the array to
	 *                       include.
	 * @return A string representation of the subarray.
	 */
	public static String getStringFromArray(double[] array, int numberToResult) {
		return Arrays.toString(ArrayUtils.subarray(array, 0, numberToResult));
	}

	/**
	 * Converts the final part of a double array to its string representation.
	 * 
	 * @param array          The source array.
	 * @param numberToResult The number of elements from the end of the array to
	 *                       include.
	 * @return A string representation of the subarray.
	 */
	public static String getStringFromTerminalArray(double[] array, int numberToResult) {
		return Arrays.toString(ArrayUtils.subarray(array, array.length - numberToResult, array.length));
	}

	/**
	 * Sets the font for a variable number of components.
	 * 
	 * @param font  The {@link Font} to apply.
	 * @param comps The {@link Component}s to modify.
	 */
	public static void setFont(Font font, Component... comps) {
		for (Component comp : comps) {
			comp.setFont(font);
		}
	}

	/**
	 * Recursively sets the font for all components within a {@link JMenuBar}.
	 * 
	 * @param font    The {@link Font} to apply.
	 * @param menuBar The {@link JMenuBar} whose items should be updated.
	 */
	public static void setMenuBarFont(Font font, JMenuBar menuBar) {
		for (int i = 0; i < menuBar.getMenuCount(); i++) {
			JMenu menu = menuBar.getMenu(i);
			if (menu != null) {
				menu.setFont(font);
				setMenuFontRecursive(menu, font);
			}
		}
	}

	/**
	 * Recursively applies the given {@link Font} to the specified
	 * {@link MenuElement} and all of its sub-elements.
	 *
	 * @param element The {@link MenuElement} to update.
	 * @param font    The {@link Font} to apply.
	 */
	private static void setMenuFontRecursive(MenuElement element, Font font) {
		Component comp = element.getComponent();
		if (comp != null) {
			comp.setFont(font);
		}
		for (MenuElement subElement : element.getSubElements()) {
			setMenuFontRecursive(subElement, font);
		}
	}

	/**
	 * Transposes a 2D integer matrix.
	 * 
	 * @param m The matrix to transpose.
	 * @return The transposed matrix.
	 */
	public static int[][] getTransposedMatrix(int[][] m) {
		int[][] temp = new int[m[0].length][m.length];
		for (int i = 0; i < m.length; i++)
			for (int j = 0; j < m[0].length; j++)
				temp[j][i] = m[i][j];
		return temp;
	}

	/**
	 * @param color input
	 * @return new color that is darker x2. It creates a new color with opacity of
	 *         255.
	 */
	public static Color getDarkerColor(Color color) {
		Color col = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
		col = col.darker().darker();
		/*
		 * for (int i = 0; i < 50; i++) { col = col.darker(); }
		 */
		return col;
	}

	/**
	 * Checks if each list is entirely unique
	 * 
	 * @param <K>   type of the list
	 * @param lists lists which to test
	 * @return true if all objects within the lists are contained only within one
	 *         list, otherwise false.
	 */
	@SafeVarargs
	public static <K> boolean disjoint(List<K>... lists) {

		for (int i = 0; i < lists.length; i++) {

			List<K> queryList = lists[i];
			for (int j = 0; j < lists.length; j++) {

				if (i == j)
					continue;

				List<K> targetList = lists[j];

				for (K element : queryList) {
					if (targetList.contains(element)) {
						return false;
					}
				}

			}

		}

		return true;
	}

	/**
	 * Finds the closest index for the specified integer within the passed integer
	 * array, assuming that the passed integer array is ascending (it does NOT check
	 * for this)
	 * 
	 * @param query  the integer to look for
	 * @param source the array
	 * @return index of the nearest element to the passed query
	 */
	public static int getClosestIndex(int query, int[] source) {
		int min = Integer.MAX_VALUE;
		int closest = query;

		for (int v : source) {
			final int diff = Math.abs(v - query);

			if (diff < min) {
				min = diff;
				closest = v;
			}
		}

		return closest;
	}

	/**
	 * Finds the closest index for the specified double within the passed double
	 * array, assuming that the passed double array is ascending (this method does
	 * not verify that).
	 *
	 * @param query  the double value to search for
	 * @param source the array of double values
	 * @return the index of the nearest element to the passed query
	 */
	public static int getClosestIndex(double query, double[] source) {
		double minDiff = Double.MAX_VALUE;
		int closestIndex = -1;
		for (int i = 0; i < source.length; i++) {
			double diff = Math.abs(source[i] - query);
			if (diff < minDiff) {
				minDiff = diff;
				closestIndex = i;
			}
		}
		return closestIndex;
	}

	/**
	 * 
	 * @param source source array
	 * @param value  value to find
	 * @return the index of the array whose value if closest to the argument value
	 */
	public static int getClosestIndex(int[] source, int value) {

		int bestIndex = 0;

		int bestDistance = value - source[0];

		for (int i = 1; i < source.length; i++) {
			int qDistance = Math.abs(value - source[i]);
			if (qDistance < bestDistance) {
				bestIndex = i;
				bestDistance = qDistance;
			}
		}

		return bestIndex;

	}

	/**
	 * 
	 * @param source source array
	 * @param value  value to find
	 * @return the value within the array, closest to the argument value
	 */
	public static double getClosestValue(double[] source, double value) {

		return source[getClosestIndex(value, source)];

	}

	/**
	 * Converts a string path to a File object if it represents an existing
	 * directory.
	 * 
	 * @param input The path string.
	 * @return A {@link File} object if the path is a valid, existing directory;
	 *         otherwise, null.
	 */
	private static File getFileFromString(String input) {
		if (input == null || input.isBlank())
			return null;

		File dir = new File(input);
		if (dir.exists() && dir.isDirectory()) {
			return dir;
		} else {
			return null;
		}
	}

	/**
	 * Checks if all provided objects are distinct from each other.
	 * 
	 * @param obj A variable number of objects to compare.
	 * @return {@code true} if no two objects are equal, {@code false} otherwise.
	 */
	public static boolean isDistinct(Object... obj) {
		HashSet<Object> objects = new HashSet<Object>(Arrays.asList(obj));
		return objects.size() == obj.length;
	}

	/**
	 * Recursively traverses a container and its sub-containers to disable the
	 * focus-painted state on all JButtons and JCheckBoxes.
	 * 
	 * @param comp The container to process.
	 */
	public static void removePaintFocus(Container comp) {
		Component[] comps = comp.getComponents();
		for (Component theComp : comps) {

			if (theComp instanceof JButton) {
				((JButton) theComp).setFocusPainted(false);
			} else if (theComp instanceof JCheckBox) {
				((JCheckBox) theComp).setFocusPainted(false);
			} else if (theComp instanceof Container) {
				removePaintFocus((Container) theComp);
			}
		}
	}

	/**
	 * Unfocus all buttons within the supplied container, recursively applying to
	 * all sub-containers
	 * 
	 * @param comp the container to process
	 */
	public static void unfocusButtons(Container comp) {

		Component[] comps = comp.getComponents();
		for (Component theComp : comps) {

			if (theComp instanceof JButton) {
				((JButton) theComp).setFocusable(false);
			} else if (theComp instanceof JComboBox) {
				theComp.setFocusable(false);
			} else if (theComp instanceof Container) {
				unfocusButtons((Container) theComp);
			}
		}
	}

	/**
	 * Unfocus all components within the supplied container, recursively applying to
	 * all sub-containers
	 * 
	 * @param comp the container to process
	 */
	public static void unfocusAll(Container comp) {

		Component[] comps = comp.getComponents();
		for (Component theComp : comps) {
			theComp.setFocusable(false);
			if (theComp instanceof Container) {
				unfocusAll((Container) theComp);
			}
		}
	}

	/**
	 * Calculates the nearest power of ten that is greater than or equal to the
	 * input value. For example, upToNearestTen(85) returns 100, and
	 * upToNearestTen(1200) returns 10,000.
	 *
	 * @param input The input value.
	 * @return The nearest power of ten as a long.
	 */
	public static long upToNearestTen(double input) {
		BigInteger bi = BigInteger.valueOf(10).pow((int) Math.ceil(log10(BigDecimal.valueOf(input)).doubleValue()));

		return bi.longValue();
	}

	/**
	 * Calculates the base-10 logarithm of a BigDecimal with high precision.
	 *
	 * @param b The {@link BigDecimal} to calculate the logarithm of.
	 * @return The base-10 logarithm as a {@link BigDecimal}.
	 * @throws ArithmeticException if the input number is negative or zero.
	 */
	public static BigDecimal log10(BigDecimal b) {
		final int SCALE = 18;
		final int NUM_OF_DIGITS = SCALE + 2;

		MathContext mc = new MathContext(NUM_OF_DIGITS, RoundingMode.HALF_EVEN);
		// special conditions:
		// log(-x) -> exception
		// log(1) == 0 exactly;
		// log of a number lessthan one = -log(1/x)
		if (b.signum() <= 0) {
			throw new ArithmeticException("log of a negative number! (or zero)");
		} else if (b.compareTo(BigDecimal.ONE) == 0) {
			return BigDecimal.ZERO;
		} else if (b.compareTo(BigDecimal.ONE) < 0) {
			return (log10((BigDecimal.ONE).divide(b, mc))).negate();
		}

		StringBuilder sb = new StringBuilder();
		// number of digits on the left of the decimal point
		int leftDigits = b.precision() - b.scale();

		// so, the first digits of the log10 are:
		sb.append(leftDigits - 1).append(".");

		// this is the algorithm outlined in the webpage
		int n = 0;
		while (n < NUM_OF_DIGITS) {
			b = (b.movePointLeft(leftDigits - 1)).pow(10, mc);
			leftDigits = b.precision() - b.scale();
			sb.append(leftDigits - 1);
			n++;
		}
		BigDecimal ans = new BigDecimal(sb.toString());

		// Round the number to the correct number of decimal places.
		ans = ans.round(new MathContext(ans.precision() - ans.scale() + SCALE, RoundingMode.HALF_EVEN));
		return ans;
	}

	/**
	 * Divides two BigDecimal numbers with a fixed precision
	 * {@link MathContext#DECIMAL64}.
	 *
	 * @param number  The number to be divided.
	 * @param divisor The divisor.
	 * @return The result of the division as a {@link BigDecimal}.
	 */
	public static BigDecimal divide(BigDecimal number, BigDecimal divisor) {
		return number.divide(divisor, MathContext.DECIMAL64);
	}

	/**
	 * Divides two double numbers using BigDecimal for precision and a fixed context
	 * {@link MathContext#DECIMAL64}.
	 *
	 * @param number  The number to be divided.
	 * @param divisor The divisor.
	 * @return The result of the division as a {@link BigDecimal}.
	 */
	public static BigDecimal divide(double number, double divisor) {
		return BigDecimal.valueOf(number).divide(BigDecimal.valueOf(divisor), MathContext.DECIMAL64);
	}

	/**
	 * Multiplies two BigDecimal numbers with a fixed precision
	 * {@link MathContext#DECIMAL64}.
	 *
	 * @param number     The number to be multiplied.
	 * @param multiplier The multiplier.
	 * @return The result of the multiplication as a {@link BigDecimal}.
	 */
	public static BigDecimal multiply(BigDecimal number, BigDecimal multiplier) {
		return number.multiply(multiplier, MathContext.DECIMAL64);
	}

	/**
	 * Multiplies two double numbers using BigDecimal for precision and a fixed
	 * context {@link MathContext#DECIMAL64}.
	 *
	 * @param number     The number to be multiplied.
	 * @param multiplier The multiplier.
	 * @return The result of the multiplication as a {@link BigDecimal}.
	 */
	public static BigDecimal multiply(double number, double multiplier) {
		return BigDecimal.valueOf(number).multiply(BigDecimal.valueOf(multiplier), MathContext.DECIMAL64);
	}

	/**
	 * Finds a good tick distance. Prefers integers. Won't produce more than 11
	 * ticks or less than 4. Prefers multiples of 10, 5, and 2 in that order.
	 * 
	 * @param min minimum value of the range
	 * @param max maximum value of the range
	 * @return interval between ticks
	 */
	public static double findOptimalTickInterval(double min, double max, boolean lessUnits) {
		double range = max - min;
		double magnitude = Math.pow(10, Math.floor(Math.log10(range)));
		double[] candidates = new double[] { magnitude * 0.1, magnitude * 0.2, magnitude * 0.5, // for smaller ranges
				magnitude, magnitude * 2, magnitude * 5, magnitude * 10 // for larger ranges
		};

		// First check integer multiples
		for (double candidate : candidates) {

			if (Math.floor(candidate) == candidate) {
				int negativeTicks = (int) Math.floor(-min / candidate);
				int positiveTicks = (int) Math.floor(max / candidate);
				int ticks = negativeTicks + positiveTicks + 1; // ensure 0 is included
				if (lessUnits) {
					if (ticks >= 3 && ticks <= 8) {
						return candidate;
					}
				} else {
					if (ticks >= 4 && ticks <= 11) {
						return candidate;
					}
				}

			}
		}
		// Check all candidates if integer multiples don't fit the criteria
		for (double candidate : candidates) {
			int negativeTicks = (int) Math.floor(-min / candidate);
			int positiveTicks = (int) Math.floor(max / candidate);
			int ticks = negativeTicks + positiveTicks + 1; // ensure 0 is included
			if (lessUnits) {
				if (ticks >= 3 && ticks <= 8) {
					return candidate;
				}
			} else {
				if (ticks >= 4 && ticks <= 11) {
					return candidate;
				}
			}

		}
		return candidates[candidates.length - 1]; // Return the least optimal if none found
	}

	/**
	 * Recursively looks through the passed {@link Container}s and sets enabled as
	 * follows:<br>
	 * <br>
	 * 
	 * 
	 * <ul>
	 * <li>{@link JButton}: sets enabled, sets to default background for enabled, or
	 * light gray for disabled</li>
	 * <li>{@link JTextField}: sets enabled. If enabled, default border and white
	 * background. Otherwise light gray line border and light gray background.</li>
	 * <li>{@link JComboBox}: sets enabled. If clear, then all items are
	 * removed</li>
	 * <li>{@link JLabel}: if enabled, black text. IF disabled then light gray. Only
	 * if changeLabels is set to true</li>
	 * <li>{@link JCheckBox}: if enabled, black text. IF disabled then dark gray
	 * text</li>
	 * <li>{@link JScrollPane}: if enabled, white background, otherwise light
	 * gray</li>
	 * </ul>
	 * 
	 * @param enabled      true if should enable
	 * @param clear        Removes items from {@link JComboBox}, clears text of
	 *                     {@link JTextField}
	 * @param changeLabels true if should apply to {@link JLabel}
	 * @param containers   list of {@link Container}s
	 */
	public static void setEnabledDeep(boolean enabled, boolean clear, boolean changeLabels, Container... containers) {

		for (Container cont : containers) {
			Component[] comps = cont.getComponents();
			if (cont instanceof JPanel) {
				((JPanel) cont).setBackground(enabled ? colorPnlEnabled : colorPnlDisabled);
			}
			for (Component theComp : comps) {

				if (theComp instanceof JButton) {
					theComp.setEnabled(enabled);

					setBackgroundForComp(enabled ? new JButton().getBackground() : Color.LIGHT_GRAY, (JButton) theComp);
				} else if (theComp instanceof JToggleButton) {
					theComp.setEnabled(enabled);
					setBackgroundForComp(enabled ? new JToggleButton().getBackground() : Color.LIGHT_GRAY,
							(JToggleButton) theComp);
				} else if (theComp instanceof JTextField) {
					JTextField textField = (JTextField) theComp;
					textField.setEnabled(enabled);
					textField.setBorder(
							enabled ? new JTextField().getBorder() : new LineBorder(Color.LIGHT_GRAY.darker()));
					if (clear) {
						textField.setText("");
					}
					setBackgroundForComp(enabled ? Color.WHITE : Color.LIGHT_GRAY, textField);
				} else if (theComp instanceof JComboBox) {
					@SuppressWarnings("rawtypes")
					JComboBox comboBox = (JComboBox) theComp;
					comboBox.setEnabled(enabled);
					if (clear) {
						comboBox.removeAllItems();
					}

				} else if (theComp instanceof JLabel && changeLabels) {
					JLabel label = (JLabel) theComp;
					label.setForeground(enabled ? Color.BLACK : Color.LIGHT_GRAY.darker());
				} else if (theComp instanceof JCheckBox) {
					theComp.setEnabled(enabled);
					theComp.setForeground(enabled ? Color.black : Color.DARK_GRAY);
				} else if (theComp instanceof JScrollPane) {
					theComp.setBackground(enabled ? Color.white : Color.LIGHT_GRAY);
					((JScrollPane) theComp).getViewport().setBackground(Color.LIGHT_GRAY);
				}

				else if (theComp instanceof Container) {
					setEnabledDeep(enabled, clear, changeLabels, (Container) theComp);
				}
			}
		}

	}

	/**
	 * Sets the supplied components as enabled or disabled, clears if needed. See
	 * further documentation in
	 * {@link #setEnabledDeep(boolean, boolean, boolean, Container...)}
	 * 
	 * @param enabled    whether to enable the specified {@link Component}s
	 * @param clear      if true, elements which are JTextField or JComboBox will be
	 *                   cleared
	 * @param components components to set enabled / disabled
	 */
	public static void setEnabled(boolean enabled, boolean clear, JComponent... components) {
		for (Component comp : components) {
			if (comp instanceof JButton) {
				comp.setEnabled(enabled);
				setBackgroundForComp(enabled ? new JButton().getBackground() : Color.LIGHT_GRAY, (JButton) comp);
			} else if (comp instanceof JToggleButton) {
				comp.setEnabled(enabled);
				setBackgroundForComp(enabled ? new JToggleButton().getBackground() : Color.LIGHT_GRAY,
						(JToggleButton) comp);
			} else if (comp instanceof JTextField) {
				JTextField textField = (JTextField) comp;
				textField.setEnabled(enabled);
				if (clear) {
					textField.setText("");
				}
				setBackgroundForComp(enabled ? Color.WHITE : Color.LIGHT_GRAY, textField);
			} else if (comp instanceof JComboBox) {
				@SuppressWarnings("rawtypes")
				JComboBox comboBox = (JComboBox) comp;
				comboBox.setEnabled(enabled);
				if (clear) {
					comboBox.removeAllItems();
				}

			} else if (comp instanceof JLabel) {
				comp.setForeground(enabled ? Color.black : Color.DARK_GRAY);
			} else if (comp instanceof JCheckBox) {
				comp.setEnabled(enabled);
				comp.setForeground(enabled ? Color.black : Color.DARK_GRAY);
			} else if (comp instanceof JScrollPane) {
				comp.setBackground(enabled ? Color.white : Color.DARK_GRAY);
			} else if (comp instanceof JPanel) {
				comp.setBackground(enabled ? colorPnlEnabled : colorPnlDisabled);
			}
		}
	}

	/**
	 * Sets the background color for the specified components.
	 */
	private static void setBackgroundForComp(Color color, JComponent... components) {
		for (Component comp : components) {
			comp.setBackground(color);
		}
	}

	/**
	 * Checks if any {@link JTextField} within the specified {@link Container} and
	 * its children currently has focus.
	 * 
	 * @param container the {@link Container} to recursively check
	 * @return true if a {@link JTextField} has focus
	 */
	public static boolean doesTextFieldHaveFocus(Container container) {

		boolean found = false;

		for (Component theComp : container.getComponents()) {

			if (theComp instanceof JTextField) {
				JTextField textField = (JTextField) theComp;
				if (textField.hasFocus()) {
					found = true;
					break;
				}
			} else if (theComp instanceof Container) {
				found = doesTextFieldHaveFocus((Container) theComp);
				if (found)
					break;
			}
		}

		return found;

	}

	/**
	 * Converts input numbers into scientific notation if any number exceeds 1000.
	 * 
	 * @param numbers The series of numbers.
	 * @return An object array containing the modified numbers in scientific
	 *         notation and the power of the scientific notation.
	 */
	public static Object[] scaleToScientific2(double... numbers) {

		// Check if any value exceeds 1000 in absolute value
		boolean needsScientificNotation = false;
		double maxValue = 0;

		for (double num : numbers) {
			if (Math.abs(num) > 1000) {
				needsScientificNotation = true;
			}
			maxValue = Math.max(maxValue, Math.abs(num));
		}

		// If the range exceeds 1000, convert to scientific notation
		if (needsScientificNotation) {
			// Find the power of 10 for scientific notation
			int power = (int) Math.floor(Math.log10(maxValue));

			// Find the scaling factor to convert the numbers
			double scale = Math.pow(10, power);

			// Create a new array to hold the numbers in scientific notation
			double[] scaledNumbers = new double[numbers.length];
			for (int i = 0; i < numbers.length; i++) {
				scaledNumbers[i] = numbers[i] / scale;
			}

			// Return the modified array in scientific notation and the power
			return new Object[] { scaledNumbers, power };
		} else {
			// If no value exceeds 1000, return the numbers unchanged with power 0
			return new Object[] { numbers, 0 };
		}
	}

	/**
	 * Scales the input numbers by the provided power of ten.
	 *
	 * @param numbers The array of numbers to convert.
	 * @param power   The exponent for scientific notation (e.g., 3 means divide by
	 *                10^3).
	 * @return A new array of numbers scaled accordingly.
	 */
	public static double[] scaleToScientific(double[] numbers, int power) {
		double scale = Math.pow(10, power);
		double[] result = new double[numbers.length];

		for (int i = 0; i < numbers.length; i++) {
			result[i] = numbers[i] / scale;
		}

		return result;
	}

	/**
	 * Modifies the input arrays to show in scientific notation.
	 * 
	 * It only converts to scientific notation if either the absolute max / min of
	 * either of the arrays exceeds 1000. The reason for this method is to make sure
	 * that both arrays are converted or both arrays aren't.
	 * 
	 * @param array1 The forward wave intensity
	 * @param array2 The backward wave intensity
	 * @return array in format [<scaled array 1>,<scaled array 2>,<integer multiple
	 *         for scientific notation>]
	 */
	public static Object[] scaleToScientific(double[] array1, double[] array2) {

		double[] rangeArray = Utils.getBounds(array1, array2);
		double range = Math.abs(rangeArray[0]) + Math.abs(rangeArray[1]);

		double[] modifiedYForward = Arrays.copyOf(array1, array1.length);
		double[] modifiedYBackward = Arrays.copyOf(array2, array2.length);

		if (Math.abs(rangeArray[0]) > 1000 || Math.abs(rangeArray[1]) > 1000) {
			double multiples = BigDecimal.valueOf(range).divide(BigDecimal.valueOf(10.0)).doubleValue();
			BigDecimal divisor = BigDecimal.valueOf(Utils.upToNearestTen(multiples));

			if (BigDecimal.valueOf(range).divide(divisor).doubleValue() < 5) {
				// with the adjusted tick distance, there would be less than 5 ticks. Retry with
				// a higher value
				multiples = BigDecimal.valueOf(range).divide(BigDecimal.valueOf(100.0)).doubleValue();
				divisor = BigDecimal.valueOf(Utils.upToNearestTen(multiples));

			}

			for (int i = 0; i < array1.length; i++) {
				modifiedYForward[i] = BigDecimal.valueOf(modifiedYForward[i]).divide(divisor).doubleValue();
				modifiedYBackward[i] = BigDecimal.valueOf(modifiedYBackward[i]).divide(divisor).doubleValue();
			}

			String divisorString = String.valueOf(divisor.toPlainString());
			int numZeros = divisorString.length() - divisorString.replaceAll("0", "").length();

			return new Object[] { modifiedYForward, modifiedYBackward, numZeros };
		} else {
			return new Object[] { modifiedYForward, modifiedYBackward, 0 };
		}

	}

	/**
	 * Modifies the input arrays to show in scientific notation. First two elements
	 * of array are the initial scaled arrays. Third element is the integer multiple
	 * of 10 used in the scientific notation.
	 * 
	 * It only converts to scientific notation if either the absolute max / min of
	 * either of the arrays exceeds 1000.
	 * 
	 */
	public static Object[] scaleToScientific(double[] array) {

		double[] rangeArray = Utils.getBounds(array);
		double range = Math.abs(rangeArray[0]) + Math.abs(rangeArray[1]);

		double[] modified = Arrays.copyOf(array, array.length);

		if (Math.abs(rangeArray[0]) > 1000 || Math.abs(rangeArray[1]) > 1000) {
			double multiples = BigDecimal.valueOf(range).divide(BigDecimal.valueOf(10.0)).doubleValue();
			BigDecimal divisor = BigDecimal.valueOf(Utils.upToNearestTen(multiples));

			if (BigDecimal.valueOf(range).divide(divisor).doubleValue() < 4) {
				// with the adjusted tick distance, there would be less than 5 ticks. Retry with
				// a higher value
				multiples = BigDecimal.valueOf(range).divide(BigDecimal.valueOf(100.0)).doubleValue();
				divisor = BigDecimal.valueOf(Utils.upToNearestTen(multiples));

			}

			for (int i = 0; i < array.length; i++) {
				modified[i] = BigDecimal.valueOf(modified[i]).divide(divisor).doubleValue();
			}

			String divisorString = String.valueOf(divisor.toPlainString());
			int numZeros = divisorString.length() - divisorString.replaceAll("0", "").length();

			return new Object[] { modified, numZeros };
		} else {
			return new Object[] { modified, 0 };
		}

	}

	/**
	 * Shortens a file path to a specified maximum length by inserting "..." in the
	 * middle. Prioritizes keeping more of the end than the beginning and attempts
	 * to split around slashes.
	 * 
	 * @param filePath  The original file path to shorten.
	 * @param maxLength The maximum allowed length for the shortened file path.
	 * @return The shortened file path with "..." inserted if truncation is needed.
	 */
	public static String getShortenedFilePath(String filePath, int maxLength) {
		if (filePath == null || filePath.length() <= maxLength) {
			return filePath;
		}

		String[] parts = filePath.split("/");
		int leftIndex = 0, rightIndex = parts.length - 1;

		// Always retain the last segment
		String shortenedPath = parts[rightIndex];
		int remainingLength = maxLength - shortenedPath.length() - 3; // 3 for "..."

		// Keep adding segments from the right until we are near the limit
		while (rightIndex > 0 && remainingLength > 0) {
			rightIndex--;
			String nextSegment = parts[rightIndex];
			if (remainingLength - nextSegment.length() - 1 > 0) { // -1 for slash
				shortenedPath = nextSegment + "/" + shortenedPath;
				remainingLength -= nextSegment.length() + 1;
			} else {
				break;
			}
		}

		// If there's space, add some from the left
		String prefix = "";
		while (leftIndex < rightIndex && remainingLength > 0) {
			String nextSegment = parts[leftIndex];
			if (remainingLength - nextSegment.length() - 1 > 0) {
				prefix += nextSegment + "/";
				remainingLength -= nextSegment.length() + 1;
				leftIndex++;
			} else {
				break;
			}
		}

		return prefix + "..." + "/" + shortenedPath;
	}

	/**
	 * Computes the area under curve (integral) on the specified double array
	 * 
	 * @param timeInterval time interval between sequential elements in the supplied
	 *                     array
	 * @param y            array of values
	 * @return the total area under the curve (integral)
	 */
	public static double getAreaUnderCurve(BigDecimal timeInterval, double[] y) {
		BigDecimal two = BigDecimal.valueOf(2.0);
		BigDecimal[] auc = new BigDecimal[y.length - 1];
		for (int i = 0; i < y.length - 1; i++) {

			if (y[i] >= 0 && y[i + 1] >= 0) {

				BigDecimal yBDMax = BigDecimal.valueOf(Math.max(y[i], y[i + 1]));
				BigDecimal yBDMin = BigDecimal.valueOf(Math.min(y[i], y[i + 1]));
				auc[i] = yBDMin.multiply(timeInterval, MathContext.DECIMAL128)
						.add(yBDMax.subtract(yBDMin, MathContext.DECIMAL128)
								.multiply(timeInterval, MathContext.DECIMAL128).divide(two, MathContext.DECIMAL128),
								MathContext.DECIMAL128);
			} else if (y[i] <= 0 && y[i + 1] <= 0) {
				BigDecimal yBDMax = BigDecimal.valueOf(Math.max(Math.abs(y[i]), Math.abs(y[i + 1])));
				BigDecimal yBDMin = BigDecimal.valueOf(Math.min(Math.abs(y[i]), Math.abs(y[i + 1])));
				auc[i] = yBDMin.multiply(timeInterval, MathContext.DECIMAL128)
						.add(yBDMax.subtract(yBDMin, MathContext.DECIMAL128)
								.multiply(timeInterval, MathContext.DECIMAL128).divide(two, MathContext.DECIMAL128),
								MathContext.DECIMAL128)
						.negate();
			} else {
				BigDecimal bd0 = new BigDecimal(y[i]);
				BigDecimal bd1 = new BigDecimal(y[i + 1]);

				BigDecimal absSum = bd0.abs().add(bd1.abs());
				BigDecimal propTimeIntervalPos = null;
				BigDecimal propTimeIntervalNeg = null;
				if (absSum.compareTo(BigDecimal.ZERO) == 0) {
					propTimeIntervalPos = propTimeIntervalNeg = timeInterval.divide(two, MathContext.DECIMAL128);
				} else {
					propTimeIntervalPos = timeInterval.multiply(bd0.abs().divide(absSum, MathContext.DECIMAL128),
							MathContext.DECIMAL128);
					propTimeIntervalNeg = timeInterval.multiply(bd1.abs().divide(absSum, MathContext.DECIMAL128),
							MathContext.DECIMAL128);
				}

				auc[i] = bd0.multiply(propTimeIntervalPos, MathContext.DECIMAL128).divide(two, MathContext.DECIMAL128)
						.add(bd1.multiply(propTimeIntervalNeg, MathContext.DECIMAL128).divide(two,
								MathContext.DECIMAL128));

				// signs are opposite.

			}

		}

		BigDecimal total = BigDecimal.ZERO;

		for (BigDecimal value : auc) {
			total = total.add(value, MathContext.DECIMAL128);
		}

		return total.doubleValue();

	}

	/**
	 * Determines if the passed input is likely a time stamp
	 * 
	 * @return true if time stamp
	 */
	public static boolean isATimeStamp(String str) {
		return str.matches("[0-9:]*");
	}

	/**
	 * Gets the nearest multiple above the specified query value
	 * 
	 * @param query    value to find multiple above
	 * @param multiple the multiple to look for
	 * @return the nearest multiple
	 */
	public static double getNearestMultipleAbove(double query, double multiple) {

		BigDecimal multBD = BigDecimal.valueOf(multiple);
		int numDecimals = getNumberOfDecimalPlaces(multBD);
		BigDecimal multiplier = BigDecimal.TEN.pow(numDecimals, MathContext.DECIMAL128);
		BigDecimal intMultBD = multBD.multiply(multiplier, MathContext.DECIMAL128);

		BigDecimal divided = BigDecimal.valueOf(query).multiply(multiplier, MathContext.DECIMAL128)
				.divide(intMultBD, MathContext.DECIMAL128).setScale(0, RoundingMode.UP)
				.multiply(intMultBD, MathContext.DECIMAL128).divide(multiplier, MathContext.DECIMAL128);
		return divided.doubleValue();
	}

	/**
	 * Returns number of decimals for a {@link BigDecimal}
	 * 
	 * @param bigDecimal the number of interest
	 * @return numbers after the decimal. For instance return value for 1.23 would
	 *         be 2.
	 */
	public static int getNumberOfDecimalPlaces(BigDecimal bigDecimal) {
		String string = bigDecimal.stripTrailingZeros().toPlainString();
		int index = string.indexOf(".");
		return index < 0 ? 0 : string.length() - index - 1;
	}

	/**
	 * This function returns the input array after reversing the order of the
	 * elements in it.
	 * 
	 * @param arr Array to be reversed
	 * @return double[] Reversed array
	 */
	public static double[] reverse(double[] arr) {
		double[] inv = new double[arr.length];
		for (int i = 0; i < inv.length; i++) {
			inv[i] = arr[arr.length - 1 - i];
		}
		return inv;
	}

	/**
	 * Converts a 1D double array into a 2D array where the first column is the
	 * original index and the second column is the value.
	 *
	 * @param input The input 1D array.
	 * @return The resulting 2D array.
	 */
	private static double[][] conv2d(double[] input) {
		double[][] out = new double[input.length][2];
		for (int i = 0; i < input.length; i++) {
			out[i][0] = i;
			out[i][1] = input[i];
		}

		return out;
	}

	/**
	 * Converts a 1D array to a 2D array (containing original index and value) and
	 * sorts it in descending order based on the value.
	 *
	 * @param input The input 1D array.
	 * @return A new 2D array sorted in descending order of values. Each inner array
	 *         is [index, value].
	 */
	public static double[][] convertToDescending(double[] input) {

		double[][] out = conv2d(input);
		Arrays.sort(out, Comparator.comparingDouble(o -> o[1]));
		ArrayUtils.reverse(out);
		return out;

	}

	/**
	 * Finds the index of the maximum value within a specified range of an array. If
	 * start or end are not specified ({@code null}), then it will look from the
	 * start or end of the input array, respectively.
	 *
	 * @param target The array to search within.
	 * @param start  The starting index of the search range (inclusive).
	 * @param end    The ending index of the search range (inclusive).
	 * @return The index of the maximum value found in the specified range.
	 */
	public static int getIndexOfMax(double[] target, Integer start, Integer end) {

		if (start == null) {
			start = 0;
		}
		if (end == null) {
			end = target.length - 1;
		}
		int index = 0;
		double max = Double.MIN_VALUE;
		for (int i = start; i <= end; i++) {
			if (target[i] > max) {
				max = target[i];
				index = i;
			}
		}

		return index;
	}

	/**
	 * Checks if a list of {@link Header} objects contains a given header. The check
	 * is based on a case-insensitive match of the header name and an exact match of
	 * the column number.
	 *
	 * @param list   The list of headers to search.
	 * @param header The header to find.
	 * @return {@code true} if a matching header is found, {@code false} otherwise.
	 */
	public static boolean isHeaderContained(List<Header> list, Header header) {
		for (Header qHeader : list) {
			if (header.getName().equalsIgnoreCase(qHeader.getName()) && header.getCol() == qHeader.getCol()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds the specified number of zeros before and after the array
	 */
	public static double[] padWithZeros(double[] array, int numBefore, int numAfter) {
		ArrayList<Double> dubs = new ArrayList<Double>();
		for (int i = 0; i < numBefore; i++) {
			dubs.add(0d);
		}

		for (int i = 0; i < array.length; i++) {
			dubs.add(array[i]);
		}

		for (int i = 0; i < numAfter; i++) {
			dubs.add(0d);
		}

		return Utils.toPrimitiveDouble(dubs);
	}

	/**
	 * Checks whether the input String is a valid file name
	 *
	 * @return true if input is not black, and is alphanumeric.
	 */
	public static boolean isValidFileName(String input) {
		if (input == null || input.isBlank())
			return false;

		return StringUtils.isAlphanumericSpace(input);
	}

	/**
	 * Checks if file exists and can be read.
	 * 
	 * @param file the file to check
	 * @return error String if cannot read, otherwise null
	 */
	public static String checkCanReadFile(File file) {
		if (!file.exists() || !file.canRead()) {
			return "Cannot read file " + file.getPath()
					+ " -- it may be deleted, moved, or you do not have permission.";
		} else {
			return null;
		}
	}

	/**
	 * Determines the pressure unit, given the inputs which are expected to be blood
	 * pressure readings. Will return one of {@link PressureUnit}. If cannot
	 * determine, returns {@link PressureUnit#NEITHER}
	 * 
	 * @param bloodPressureValues values to check
	 * @return {@link PressureUnit} for the values
	 */
	public static PressureUnit determinePressureUnit(double[] bloodPressureValues) {
		if (bloodPressureValues == null || bloodPressureValues.length == 0) {
			return PressureUnit.NEITHER;
		}

		int mmHgCount = 0;
		int pascalCount = 0;
		int validCount = 0;

		for (double value : bloodPressureValues) {
			if (value == 0) {
				continue; // Ignore zero values
			}
			validCount++;
			if (isLikelyMmHg(value)) {
				mmHgCount++;
			} else if (isLikelyPascal(value)) {
				pascalCount++;
			}
		}

		if (validCount == 0) {
			return PressureUnit.NEITHER;
		}

		if (mmHgCount > validCount / 2) {
			return PressureUnit.MMHG;
		} else if (pascalCount > validCount / 2) {
			return PressureUnit.PASCALS;
		} else {
			return PressureUnit.NEITHER;
		}
	}

	/**
	 * Heuristically determines if a value is likely to be a blood pressure reading
	 * in mmHg.
	 *
	 * @param value The value to check.
	 * @return {@code true} if the value is within a typical physiological range for
	 *         mmHg.
	 */
	private static boolean isLikelyMmHg(double value) {
		// Blood pressure in mmHg typically ranges between 60 and 180 mmHg
		return value >= 10 && value <= 200;
	}

	/**
	 * Heuristically determines if a value is likely to be a blood pressure reading
	 * in Pascals.
	 *
	 * @param value The value to check.
	 * @return {@code true} if the value is within a typical physiological range for
	 *         Pascals.
	 */
	private static boolean isLikelyPascal(double value) {
		// Blood pressure in Pascals typically ranges between 8000 and 24000 Pascals
		return value >= 8000 && value <= 24000;
	}

	/**
	 * Determines the flow unit, given the inputs which are expected to be coronary
	 * flow readings. Will return one of {@link FlowUnit}. If cannot determine,
	 * returns {@link FlowUnit#NEITHER}
	 * 
	 * @param flowVelocityValues values to check
	 * @return {@link FlowUnit} for the values
	 */
	public static FlowUnit determineFlowUnit(double[] flowVelocityValues) {
		if (flowVelocityValues == null || flowVelocityValues.length == 0) {
			return FlowUnit.NEITHER;
		}

		int mpsCount = 0;
		int cpsCount = 0;
		int validCount = 0;

		for (double value : flowVelocityValues) {
			if (value == 0) {
				continue; // Ignore zero values
			}
			validCount++;
			if (isLikelyMPS(value)) {
				mpsCount++;
			} else if (isLikelyCPS(value)) {
				cpsCount++;
			}
		}

		if (validCount == 0) {
			return FlowUnit.NEITHER;
		}

		if (mpsCount > validCount / 2) {
			return FlowUnit.MPS;
		} else if (cpsCount > validCount / 2) {
			return FlowUnit.CPS;
		} else {
			return FlowUnit.NEITHER;
		}
	}

	/**
	 * Determines if passed value is likely meters per second, specifically for
	 * coronary flow, where the velocity when measured in m/s is typically in the
	 * range 0.1 to 1.5 m/s
	 * 
	 * @return true if m/s
	 */
	private static boolean isLikelyMPS(double value) {
		// Coronary artery flow velocities in m/s typically range from 0.1 to 1.5 m/s
		return value >= 0.05 && value <= 1.5;
	}

	/**
	 * Determines if passed value is likely centimeters per second, specifically for
	 * coronary flow, where the velocity when measured in m/s is typically in the
	 * range 10 to 150 cm/s
	 * 
	 * @return true if cm/s
	 */
	private static boolean isLikelyCPS(double value) {
		// Coronary artery flow velocities in cm/s typically range from 10 to 150 cm/s
		return value >= 5 && value <= 300;
	}

	/**
	 * Removes \/:*?"<>|
	 */
	public static String stripInvalidFileNameCharacters(String input) {
		// Define a regular expression for invalid characters
		String invalidChars = "[\\\\/:*?\"<>|]";

		// Replace invalid characters with an empty string
		return input.replaceAll(invalidChars, "");
	}

	/**
	 * Gets the index of a specific item within a JComboBox.
	 *
	 * @param <E>      The type of items in the JComboBox.
	 * @param comboBox The JComboBox to search.
	 * @param item     The item to find.
	 * @return The index of the first occurrence of the item, or -1 if the item is
	 *         not found.
	 */
	public static <E> int getJComboBoxItemIndex(JComboBox<E> comboBox, E item) {
		for (int i = 0; i < comboBox.getItemCount(); i++) {
			if (comboBox.getItemAt(i).equals(item)) {
				return i;
			}
		}
		return -1; // Item not found
	}

}
