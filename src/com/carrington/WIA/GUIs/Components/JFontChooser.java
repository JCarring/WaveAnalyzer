package com.carrington.WIA.GUIs.Components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;

import com.carrington.WIA.Utils;

/**
 * The <code>JFontChooser</code> class is a swing component for font selection.
 * This class has <code>JFileChooser</code> like APIs. The following code pops
 * up a font chooser dialog.
 * 
 **/
public class JFontChooser extends JComponent {

	private static final long serialVersionUID = -1875857422814818390L;

	/**
	 * A map to cache Font objects and their calculated dimensions for performance.
	 */
	public static final Map<String, Object[]> fonts = new HashMap<String, Object[]>();
	private static final String[] fontFamilyNames;
	static {
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		fontFamilyNames = env.getAvailableFontFamilyNames();
		for (String fontName : fontFamilyNames) {
			Font font = new Font(fontName, Font.PLAIN, 12);
			int[] fontParams = Utils.getFontParams(font, fontName);
			fonts.put(fontName, new Object[] { font, fontParams[0], fontParams[1] });
		}
	}

	/**
	 * Creates and displays a modal {@link JFontChooser} dialog.
	 */
	public static void promptForFont() {

		JFontChooser jc = new JFontChooser();
		jc.showDialog(null);

	}

	/**
	 * Return value from <code>showDialog()</code>.
	 * 
	 * @see #showDialog
	 **/
	public static final int OK_OPTION = 0;
	/**
	 * Return value from <code>showDialog()</code>.
	 * 
	 * @see #showDialog
	 **/
	public static final int CANCEL_OPTION = 1;
	/**
	 * Return value from <code>showDialog()</code>.
	 * 
	 * @see #showDialog
	 **/
	public static final int ERROR_OPTION = -1;
	private static final Font DEFAULT_SELECTED_FONT = new Font("Arial", Font.PLAIN, 12);
	private static final Font DEFAULT_FONT = Utils.getTextFont(false);
	private static final int[] FONT_STYLE_CODES = { Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD | Font.ITALIC };
	private static final String[] DEFAULT_FONT_SIZE_STRINGS = { "8", "9", "10", "11", "12", "14", "16", "18", "20",
			"22", "24", "26", "28", "36", "48", "72", };

	private int dialogResultValue = ERROR_OPTION;

	private String[] fontStyleNames = null;
	private String[] fontSizeStrings = null;
	private JTextField fontFamilyTextField = null;
	private JTextField fontStyleTextField = null;
	private JTextField fontSizeTextField = null;
	private JList<String> fontNameList = null;
	private JList<String> fontStyleList = null;
	private JList<String> fontSizeList = null;
	private JPanel fontNamePanel = null;
	private JPanel fontStylePanel = null;
	private JPanel fontSizePanel = null;
	private JPanel samplePanel = null;
	private JTextField sampleText = null;

	/**
	 * Constructs a <code>JFontChooser</code> object.
	 **/
	public JFontChooser() {
		this(DEFAULT_FONT_SIZE_STRINGS);
	}

	/**
	 * Constructs a <code>JFontChooser</code> object using the given font size
	 * array.
	 * 
	 * @param fontSizeStrings the array of font size string.
	 **/
	public JFontChooser(String[] fontSizeStrings) {
		if (fontSizeStrings == null) {
			fontSizeStrings = DEFAULT_FONT_SIZE_STRINGS;
		}
		this.fontSizeStrings = fontSizeStrings;

		JPanel selectPanel = new JPanel();
		selectPanel.setLayout(new BoxLayout(selectPanel, BoxLayout.X_AXIS));
		selectPanel.add(getFontFamilyPanel());
		selectPanel.add(getFontStylePanel());
		selectPanel.add(getFontSizePanel());

		JPanel contentsPanel = new JPanel();
		contentsPanel.setLayout(new GridLayout(2, 1));
		contentsPanel.add(selectPanel, BorderLayout.NORTH);
		contentsPanel.add(getSamplePanel(), BorderLayout.CENTER);

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.add(contentsPanel);
		this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		this.setSelectedFont(DEFAULT_SELECTED_FONT);
	}

	/**
	 * Gets the text field for font family input.
	 * 
	 * @return the font family {@link JTextField}
	 */
	private JTextField getFontFamilyTextField() {
		if (fontFamilyTextField == null) {
			fontFamilyTextField = new JTextField();
			fontFamilyTextField.addFocusListener(new TextFieldFocusHandlerForTextSelection(fontFamilyTextField));
			fontFamilyTextField.addKeyListener(new TextFieldKeyHandlerForListSelectionUpDown(getFontFamilyList()));
			fontFamilyTextField.getDocument()
					.addDocumentListener(new ListSearchTextFieldDocumentHandler(getFontFamilyList()));
			fontFamilyTextField.setFont(Utils.getTextFont(false));

		}
		return fontFamilyTextField;
	}

	/**
	 * Gets the text field for font style input.
	 * 
	 * @return the font style {@link JTextField}
	 */
	private JTextField getFontStyleTextField() {
		if (fontStyleTextField == null) {
			fontStyleTextField = new JTextField();
			fontStyleTextField.addFocusListener(new TextFieldFocusHandlerForTextSelection(fontStyleTextField));
			fontStyleTextField.addKeyListener(new TextFieldKeyHandlerForListSelectionUpDown(getFontStyleList()));
			fontStyleTextField.getDocument()
					.addDocumentListener(new ListSearchTextFieldDocumentHandler(getFontStyleList()));
			fontStyleTextField.setFont(Utils.getTextFont(false));
		}
		return fontStyleTextField;
	}

	/**
	 * Gets the text field for font size input.
	 * 
	 * @return the font size {@link JTextField}
	 */
	private JTextField getFontSizeTextField() {
		if (fontSizeTextField == null) {
			fontSizeTextField = new JTextField();
			fontSizeTextField.addFocusListener(new TextFieldFocusHandlerForTextSelection(fontSizeTextField));
			fontSizeTextField.addKeyListener(new TextFieldKeyHandlerForListSelectionUpDown(getFontSizeList()));
			fontSizeTextField.getDocument()
					.addDocumentListener(new ListSearchTextFieldDocumentHandler(getFontSizeList()));
			fontSizeTextField.setFont(DEFAULT_FONT);
		}
		return fontSizeTextField;
	}

	/**
	 * Gets the list component for font family selection.
	 * 
	 * @return the font family {@link JList}
	 */
	private JList<String> getFontFamilyList() {
		if (fontNameList == null) {
			fontNameList = new JList<String>(fontFamilyNames);
			fontNameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			fontNameList.setCellRenderer(new ListCellRenderer<String>() {

				@Override
				public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
						boolean isSelected, boolean cellHasFocus) {
					JLabel label = new JLabel(value);
					Object[] obj = fonts.get(value);
					label.setFont((Font) obj[0]);

					label.setPreferredSize(new Dimension((int) obj[2], (int) obj[1] + 2));
					label.setOpaque(true);
					if (isSelected) {
						label.setBackground(list.getSelectionBackground());
						label.setForeground(list.getSelectionForeground());
					} else {
						label.setBackground(list.getBackground());
						label.setForeground(list.getForeground());
					}

					return label;
				}

			});
			fontNameList.addListSelectionListener(new ListSelectionHandler(getFontFamilyTextField()));
			fontNameList.setSelectedIndex(0);
			fontNameList.setFont(DEFAULT_FONT);
			fontNameList.setFocusable(false);
		}
		return fontNameList;
	}

	/**
	 * Gets the list component for font style selection.
	 * 
	 * @return the font style {@link JList}
	 */
	private JList<String> getFontStyleList() {
		if (fontStyleList == null) {
			fontStyleList = new JList<String>(getFontStyleNames());
			fontStyleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			fontStyleList.addListSelectionListener(new ListSelectionHandler(getFontStyleTextField()));
			fontStyleList.setSelectedIndex(0);
			fontStyleList.setFont(DEFAULT_FONT);
			fontStyleList.setFocusable(false);
		}
		return fontStyleList;
	}

	/**
	 * Gets the list component for font size selection.
	 * 
	 * @return the font size JList.
	 */
	private JList<String> getFontSizeList() {
		if (fontSizeList == null) {
			fontSizeList = new JList<String>(this.fontSizeStrings);
			fontSizeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			fontSizeList.addListSelectionListener(new ListSelectionHandler(getFontSizeTextField()));
			fontSizeList.setSelectedIndex(0);
			fontSizeList.setFont(DEFAULT_FONT);
			fontSizeList.setFocusable(false);
		}
		return fontSizeList;
	}

	/**
	 * Get the family name of the selected font.
	 * 
	 * @return the font family of the selected font.
	 *
	 * @see #setSelectedFontFamily
	 **/
	public String getSelectedFontFamily() {
		String fontName = (String) getFontFamilyList().getSelectedValue();
		return fontName;
	}

	/**
	 * Get the style of the selected font.
	 * 
	 * @return the style of the selected font. <code>Font.PLAIN</code>,
	 *         <code>Font.BOLD</code>, <code>Font.ITALIC</code>,
	 *         <code>Font.BOLD|Font.ITALIC</code>
	 *
	 * @see java.awt.Font#PLAIN
	 * @see java.awt.Font#BOLD
	 * @see java.awt.Font#ITALIC
	 * @see #setSelectedFontStyle
	 **/
	public int getSelectedFontStyle() {
		int index = getFontStyleList().getSelectedIndex();
		return FONT_STYLE_CODES[index];
	}

	/**
	 * Get the size of the selected font.
	 * 
	 * @return the size of the selected font
	 *
	 * @see #setSelectedFontSize
	 **/
	public int getSelectedFontSize() {
		int fontSize = 1;
		String fontSizeString = getFontSizeTextField().getText();
		while (true) {
			try {
				fontSize = Integer.parseInt(fontSizeString);
				break;
			} catch (NumberFormatException e) {
				fontSizeString = (String) getFontSizeList().getSelectedValue();
				getFontSizeTextField().setText(fontSizeString);
			}
		}

		return fontSize;
	}

	/**
	 * Get the selected font.
	 * 
	 * @return the selected font
	 *
	 * @see #setSelectedFont
	 * @see java.awt.Font
	 **/
	public Font getSelectedFont() {
		Font font = new Font(getSelectedFontFamily(), getSelectedFontStyle(), getSelectedFontSize());
		return font;
	}

	/**
	 * Set the family name of the selected font.
	 * 
	 * @param name the family name of the selected font.
	 *
	 **/
	public void setSelectedFontFamily(String name) {
		for (int i = 0; i < fontFamilyNames.length; i++) {
			if (fontFamilyNames[i].toLowerCase().equals(name.toLowerCase())) {
				getFontFamilyList().setSelectedIndex(i);
				break;
			}
		}
		updateSampleFont();
	}

	/**
	 * Set the style of the selected font.
	 * 
	 * @param style the size of the selected font. <code>Font.PLAIN</code>,
	 *              <code>Font.BOLD</code>, <code>Font.ITALIC</code>, or
	 *              <code>Font.BOLD|Font.ITALIC</code>.
	 *
	 * @see java.awt.Font#PLAIN
	 * @see java.awt.Font#BOLD
	 * @see java.awt.Font#ITALIC
	 * @see #getSelectedFontStyle
	 **/
	public void setSelectedFontStyle(int style) {
		for (int i = 0; i < FONT_STYLE_CODES.length; i++) {
			if (FONT_STYLE_CODES[i] == style) {
				getFontStyleList().setSelectedIndex(i);
				break;
			}
		}
		updateSampleFont();
	}

	/**
	 * Set the size of the selected font.
	 * 
	 * @param size the size of the selected font
	 *
	 * @see #getSelectedFontSize
	 **/
	public void setSelectedFontSize(int size) {
		String sizeString = String.valueOf(size);
		for (int i = 0; i < this.fontSizeStrings.length; i++) {
			if (this.fontSizeStrings[i].equals(sizeString)) {
				getFontSizeList().setSelectedIndex(i);
				break;
			}
		}
		getFontSizeTextField().setText(sizeString);
		updateSampleFont();
	}

	/**
	 * Set the selected font.
	 * 
	 * @param font the selected font
	 *
	 * @see #getSelectedFont
	 * @see java.awt.Font
	 **/
	public void setSelectedFont(Font font) {
		setSelectedFontFamily(font.getFamily());
		setSelectedFontStyle(font.getStyle());
		setSelectedFontSize(font.getSize());
	}

	/**
	 * Show font selection dialog.
	 * 
	 * @param parent Dialog's Parent component.
	 * @return OK_OPTION, CANCEL_OPTION or ERROR_OPTION
	 *
	 * @see #OK_OPTION
	 * @see #CANCEL_OPTION
	 * @see #ERROR_OPTION
	 **/
	public int showDialog(Component parent) {
		dialogResultValue = ERROR_OPTION;
		JDialog dialog = createDialog(parent);
		dialog.setLocationRelativeTo(parent);
		dialog.setAlwaysOnTop(true);
		dialog.setModal(true);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialogResultValue = CANCEL_OPTION;
			}
		});

		dialog.setVisible(true);
		dialog.dispose();
		dialog = null;

		return dialogResultValue;
	}

	/**
	 * Handles changes in list selections, updating the corresponding text field.
	 */
	private class ListSelectionHandler implements ListSelectionListener {
		private JTextComponent textComponent;

		private ListSelectionHandler(JTextComponent textComponent) {
			this.textComponent = textComponent;
		}

		/**
		 * Called when the list selection changes. Updates the text field with the
		 * selected value.
		 * 
		 * @param e the list selection event.
		 */
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting() == false) {
				@SuppressWarnings("unchecked")
				JList<String> list = (JList<String>) e.getSource();
				String selectedValue = (String) list.getSelectedValue();

				String oldValue = textComponent.getText();
				textComponent.setText(selectedValue);
				if (!oldValue.equalsIgnoreCase(selectedValue)) {
					textComponent.selectAll();
					textComponent.requestFocus();
				}

				updateSampleFont();
			}
		}
	}

	/**
	 * A focus listener that selects all text in a text component when it gains
	 * focus.
	 */
	private class TextFieldFocusHandlerForTextSelection extends FocusAdapter {
		private JTextComponent textComponent;

		/**
		 * Constructs a {@link TextFieldFocusHandlerForTextSelection}
		 * 
		 * @param textComponent The text component to apply the focus behavior to.
		 */
		public TextFieldFocusHandlerForTextSelection(JTextComponent textComponent) {
			this.textComponent = textComponent;
		}

		public void focusGained(FocusEvent e) {
			textComponent.selectAll();
		}

		public void focusLost(FocusEvent e) {
			textComponent.select(0, 0);
			updateSampleFont();
		}
	}

	/**
	 * Handles up and down arrow key presses in a text field to change the selection
	 * in a target JList.
	 */
	private class TextFieldKeyHandlerForListSelectionUpDown extends KeyAdapter {
		private JList<String> targetList;

		public TextFieldKeyHandlerForListSelectionUpDown(JList<String> list) {
			this.targetList = list;
		}

		public void keyPressed(KeyEvent e) {
			int i = targetList.getSelectedIndex();
			switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				i = targetList.getSelectedIndex() - 1;
				if (i < 0) {
					i = 0;
				}
				targetList.setSelectedIndex(i);
				break;
			case KeyEvent.VK_DOWN:
				int listSize = targetList.getModel().getSize();
				i = targetList.getSelectedIndex() + 1;
				if (i >= listSize) {
					i = listSize - 1;
				}
				targetList.setSelectedIndex(i);
				break;
			default:
				break;
			}
		}
	}

	/**
	 * A document listener that searches a JList for content matching the text
	 * field's input.
	 */
	protected class ListSearchTextFieldDocumentHandler implements DocumentListener {
		private JList<String> targetList;

		/**
		 * Constructs a document handler.
		 * 
		 * @param targetList The {@link JList} to search within.
		 */
		public ListSearchTextFieldDocumentHandler(JList<String> targetList) {
			this.targetList = targetList;
		}

		public void insertUpdate(DocumentEvent e) {
			update(e);
		}

		public void removeUpdate(DocumentEvent e) {
			update(e);
		}

		public void changedUpdate(DocumentEvent e) {
			update(e);
		}

		/**
		 * Searches the target list for the current text and updates the list selection.
		 * 
		 * @param event the document event.
		 */
		private void update(DocumentEvent event) {
			String newValue = "";
			try {
				Document doc = event.getDocument();
				newValue = doc.getText(0, doc.getLength());
			} catch (BadLocationException e) {
				e.printStackTrace();
			}

			if (newValue.length() > 0) {
				int index = targetList.getNextMatch(newValue, 0, Position.Bias.Forward);
				if (index < 0) {
					index = 0;
				}
				targetList.ensureIndexIsVisible(index);

				String matchedName = targetList.getModel().getElementAt(index).toString();
				if (newValue.equalsIgnoreCase(matchedName)) {
					if (index != targetList.getSelectedIndex()) {
						SwingUtilities.invokeLater(new ListSelector(index));
					}
				}
			}
		}

		/**
		 * A Runnable that safely selects an index in the target list on the Event
		 * Dispatch Thread.
		 */
		public class ListSelector implements Runnable {
			private int index;

			/**
			 * Constructs a list selector
			 * 
			 * @param index the list index to select.
			 */
			public ListSelector(int index) {
				this.index = index;
			}

			public void run() {
				targetList.setSelectedIndex(this.index);
			}
		}
	}

	/**
	 * An action that is performed when the OK button is clicked, closing the dialog
	 * and setting the result to OK_OPTION.
	 */
	private class DialogOKAction extends AbstractAction {
		private static final long serialVersionUID = 4913928933897180387L;
		protected static final String ACTION_NAME = "OK";
		private JDialog dialog;

		protected DialogOKAction(JDialog dialog) {
			this.dialog = dialog;
			putValue(Action.DEFAULT, ACTION_NAME);
			putValue(Action.ACTION_COMMAND_KEY, ACTION_NAME);
			putValue(Action.NAME, (ACTION_NAME));
		}

		public void actionPerformed(ActionEvent e) {
			dialogResultValue = OK_OPTION;
			dialog.setVisible(false);
		}
	}

	/**
	 * An action that is performed when the Cancel button is clicked, closing the
	 * dialog and setting the result to CANCEL_OPTION.
	 */
	private class DialogCancelAction extends AbstractAction {
		private static final long serialVersionUID = -4636123464214561097L;
		protected static final String ACTION_NAME = "Cancel";
		private JDialog dialog;

		protected DialogCancelAction(JDialog dialog) {
			this.dialog = dialog;
			putValue(Action.DEFAULT, ACTION_NAME);
			putValue(Action.ACTION_COMMAND_KEY, ACTION_NAME);
			putValue(Action.NAME, (ACTION_NAME));
		}

		public void actionPerformed(ActionEvent e) {
			dialogResultValue = CANCEL_OPTION;
			dialog.setVisible(false);
		}
	}

	/**
	 * Creates and configures the JDialog that will contain the JFontChooser panel
	 * and buttons.
	 * 
	 * @param parent the parent component for the dialog.
	 * @return a configured JDialog.
	 */
	private JDialog createDialog(Component parent) {
		Frame frame = parent instanceof Frame ? (Frame) parent
				: (Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent);
		JDialog dialog = new JDialog(frame, ("Select Font"), true);

		Action okAction = new DialogOKAction(dialog);
		Action cancelAction = new DialogCancelAction(dialog);

		JButton okButton = new JButton(okAction);
		okButton.setFont(DEFAULT_FONT);
		JButton cancelButton = new JButton(cancelAction);
		cancelButton.setFont(DEFAULT_FONT);

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridLayout(2, 1));
		buttonsPanel.add(okButton);
		buttonsPanel.add(cancelButton);
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(25, 0, 10, 10));

		ActionMap actionMap = buttonsPanel.getActionMap();
		actionMap.put(cancelAction.getValue(Action.DEFAULT), cancelAction);
		actionMap.put(okAction.getValue(Action.DEFAULT), okAction);
		InputMap inputMap = buttonsPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), cancelAction.getValue(Action.DEFAULT));
		inputMap.put(KeyStroke.getKeyStroke("ENTER"), okAction.getValue(Action.DEFAULT));

		JPanel dialogEastPanel = new JPanel();
		dialogEastPanel.setLayout(new BorderLayout());
		dialogEastPanel.add(buttonsPanel, BorderLayout.NORTH);

		dialog.getContentPane().add(this, BorderLayout.CENTER);
		dialog.getContentPane().add(dialogEastPanel, BorderLayout.EAST);
		dialog.setBounds(0, 0, 600, 800);
		dialog.setResizable(false);
		dialog.setAlwaysOnTop(true);
		dialog.setModal(true);
		dialog.pack();
		dialog.setLocationRelativeTo(frame);
		return dialog;
	}

	/**
	 * Updates the font of the sample text field based on the current selections.
	 */
	private void updateSampleFont() {
		Font font = getSelectedFont();
		getSampleTextField().setFont(font);
	}

	/**
	 * Lazily creates and returns the panel containing the font family list and text
	 * field.
	 * 
	 * @return the font family selection {@link JPanel}
	 */
	private JPanel getFontFamilyPanel() {
		if (fontNamePanel == null) {
			fontNamePanel = new JPanel();
			fontNamePanel.setLayout(new BorderLayout());
			fontNamePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			fontNamePanel.setPreferredSize(new Dimension(180, 180));

			JScrollPane scrollPane = new JScrollPane(getFontFamilyList());
			scrollPane.getVerticalScrollBar().setFocusable(false);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.add(getFontFamilyTextField(), BorderLayout.NORTH);
			p.add(scrollPane, BorderLayout.CENTER);

			JLabel label = new JLabel(("Font Name"));
			label.setFont(Utils.getTextFont(true));
			label.setHorizontalAlignment(JLabel.LEFT);
			label.setHorizontalTextPosition(JLabel.LEFT);
			label.setLabelFor(getFontFamilyTextField());
			label.setDisplayedMnemonic('F');

			fontNamePanel.add(label, BorderLayout.NORTH);
			fontNamePanel.add(p, BorderLayout.CENTER);

		}
		return fontNamePanel;
	}

	/**
	 * Lazily creates and returns the panel containing the font style list and text
	 * field.
	 * 
	 * @return the font style selection {@link JPanel}
	 */
	private JPanel getFontStylePanel() {
		if (fontStylePanel == null) {
			fontStylePanel = new JPanel();
			fontStylePanel.setLayout(new BorderLayout());
			fontStylePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			fontStylePanel.setPreferredSize(new Dimension(140, 180));

			JScrollPane scrollPane = new JScrollPane(getFontStyleList());
			scrollPane.getVerticalScrollBar().setFocusable(false);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.add(getFontStyleTextField(), BorderLayout.NORTH);
			p.add(scrollPane, BorderLayout.CENTER);

			JLabel label = new JLabel(("Font Style"));
			label.setFont(Utils.getTextFont(true));
			label.setHorizontalAlignment(JLabel.LEFT);
			label.setHorizontalTextPosition(JLabel.LEFT);
			label.setLabelFor(getFontStyleTextField());
			label.setDisplayedMnemonic('Y');

			fontStylePanel.add(label, BorderLayout.NORTH);
			fontStylePanel.add(p, BorderLayout.CENTER);
		}
		return fontStylePanel;
	}

	/**
	 * Lazily creates and returns the panel containing the font size list and text
	 * field.
	 * 
	 * @return the font size selection {@link JPanel}
	 */
	private JPanel getFontSizePanel() {
		if (fontSizePanel == null) {
			fontSizePanel = new JPanel();
			fontSizePanel.setLayout(new BorderLayout());
			fontSizePanel.setPreferredSize(new Dimension(70, 180));
			fontSizePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

			JScrollPane scrollPane = new JScrollPane(getFontSizeList());
			scrollPane.getVerticalScrollBar().setFocusable(false);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.add(getFontSizeTextField(), BorderLayout.NORTH);
			p.add(scrollPane, BorderLayout.CENTER);

			JLabel label = new JLabel(("Font Size"));
			label.setFont(Utils.getTextFont(true));
			label.setHorizontalAlignment(JLabel.LEFT);
			label.setHorizontalTextPosition(JLabel.LEFT);
			label.setLabelFor(getFontSizeTextField());
			label.setDisplayedMnemonic('S');

			fontSizePanel.add(label, BorderLayout.NORTH);
			fontSizePanel.add(p, BorderLayout.CENTER);
		}
		return fontSizePanel;
	}

	/**
	 * Lazily creates and returns the panel that displays the sample text.
	 * 
	 * @return the sample text {@link JPanel}
	 */
	private JPanel getSamplePanel() {
		if (samplePanel == null) {
			Border titledBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), ("Sample"));
			Border empty = BorderFactory.createEmptyBorder(5, 10, 10, 10);
			Border border = BorderFactory.createCompoundBorder(titledBorder, empty);

			samplePanel = new JPanel();
			samplePanel.setLayout(new BorderLayout());
			samplePanel.setBorder(border);

			samplePanel.add(getSampleTextField(), BorderLayout.CENTER);
		}
		return samplePanel;
	}

	/**
	 * Lazily creates and returns the text field used to display the sample font.
	 * 
	 * @return the sample {@link JTextField}
	 */
	protected JTextField getSampleTextField() {
		if (sampleText == null) {
			Border lowered = BorderFactory.createLoweredBevelBorder();

			sampleText = new JTextField(("AaBbYyZz"));
			sampleText.setBorder(lowered);
			sampleText.setPreferredSize(new Dimension(300, 80));
		}
		return sampleText;
	}

	/**
	 * Lazily creates and returns an array of font style names.
	 * 
	 * @return an array of font style names.
	 */
	protected String[] getFontStyleNames() {
		if (fontStyleNames == null) {
			int i = 0;
			fontStyleNames = new String[4];
			fontStyleNames[i++] = ("Plain");
			fontStyleNames[i++] = ("Bold");
			fontStyleNames[i++] = ("Italic");
			fontStyleNames[i++] = ("BoldItalic");
		}
		return fontStyleNames;
	}
}