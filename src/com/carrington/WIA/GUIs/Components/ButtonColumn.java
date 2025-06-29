package com.carrington.WIA.GUIs.Components;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * The ButtonColumn class provides a renderer and an editor that looks like a
 * JButton. The renderer and editor will then be used for a specified column in
 * the table. The TableModel will contain the String to be displayed on the
 * button.
 *
 * The button can be invoked by a mouse click or by pressing the space bar when
 * the cell has focus. Optionaly a mnemonic can be set to invoke the button.
 * When the button is invoked the provided Action is invoked. The source of the
 * Action will be the table. The action command will contain the model row
 * number of the button that was clicked.
 *
 */
public class ButtonColumn extends AbstractCellEditor
		implements TableCellRenderer, TableCellEditor, ActionListener, MouseListener {
	private static final long serialVersionUID = 5649050218078700431L;
	private JTable table;
	private Action action;
	private int mnemonic;
	private Border originalBorder;
	private Border focusBorder;

	private JButton renderButton;
	private JButton editButton;
	private Object editorValue;
	private boolean isButtonColumnEditor;

	private final boolean isPreviewOnly;

	/**
	 * Create the ButtonColumn to be used as a renderer and editor. The renderer and
	 * editor will automatically be installed on the TableColumn of the specified
	 * column.
	 *
	 * @param table         the table containing the button renderer/editor
	 * @param action        the Action to be invoked when the button is invoked
	 * @param column        the column to which the button renderer/editor is added
	 * @param isPreviewOnly true if the button should be disabled for preview
	 * 
	 */
	public static void addButtonColToTable(JTable table, Action action, int column, boolean isPreviewOnly) {
		ButtonColumn bc = new ButtonColumn(table, action, column, isPreviewOnly);
		TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(column).setCellRenderer(bc);
		columnModel.getColumn(column).setCellEditor(bc);
		table.addMouseListener(bc);

	}

	/**
	 * Constructs a ButtonColumn and sets it as the renderer and editor for a
	 * specific column in the table.
	 *
	 * @param table         the table containing the button renderer/editor
	 * @param action        the {@link Action} to be invoked when the button is
	 *                      invoked
	 * @param column        the column to which the button renderer/editor is added
	 * @param isPreviewOnly true if the button should be disabled for preview
	 */
	private ButtonColumn(JTable table, Action action, int column, boolean isPreviewOnly) {
		this.table = table;
		this.action = action;
		this.isPreviewOnly = isPreviewOnly;

		renderButton = new JButton();
		editButton = new JButton();
		editButton.setFocusPainted(false);
		editButton.addActionListener(this);
		originalBorder = editButton.getBorder();
		setFocusBorder(new LineBorder(Color.BLUE));

	}

	/**
	 * Get foreground color of the button when the cell has focus
	 *
	 * @return the foreground color
	 */
	public Border getFocusBorder() {
		return focusBorder;
	}

	/**
	 * The foreground color of the button when the cell has focus
	 *
	 * @param focusBorder the foreground color
	 */
	public void setFocusBorder(Border focusBorder) {
		this.focusBorder = focusBorder;
		editButton.setBorder(focusBorder);
	}

	/**
	 * Gets the mnemonic to activate the button when the cell has focus
	 *
	 * @return the mnemonic
	 */
	public int getMnemonic() {
		return mnemonic;
	}

	/**
	 * The mnemonic to activate the button when the cell has focus
	 *
	 * @param mnemonic the mnemonic
	 */
	public void setMnemonic(int mnemonic) {
		this.mnemonic = mnemonic;
		renderButton.setMnemonic(mnemonic);
		editButton.setMnemonic(mnemonic);

	}

	/**
	 * Returns the component that should be used as the editor for the cell.
	 *
	 * @param table      the JTable that is asking the editor to edit; can be null
	 * @param value      the value of the cell to be edited; it is up to the editor
	 *                   to display whatever is appropriate
	 * @param isSelected true if the cell is to be rendered with highlighting
	 * @param row        the row of the cell being edited
	 * @param column     the column of the cell being edited
	 * @return the component for editing
	 */
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if (value == null) {
			editButton.setText("");
			editButton.setIcon(null);
		} else if (value instanceof Icon) {
			editButton.setText("");
			editButton.setIcon((Icon) value);
		} else {
			editButton.setText(value.toString());
			editButton.setIcon(null);
		}

		this.editorValue = value;
		return editButton;
	}

	/**
	 * Returns the value contained in the editor.
	 * 
	 * @return the value contained in the editor
	 */
	@Override
	public Object getCellEditorValue() {
		return editorValue;
	}

	/**
	 * Returns the component used for drawing the cell. This method is used to
	 * configure the renderer appropriately before drawing.
	 *
	 * @param table      the <code>JTable</code> that is asking the renderer to
	 *                   draw; can be <code>null</code>
	 * @param value      the value of the cell to be rendered.
	 * @param isSelected true if the cell is to be rendered with the selection
	 *                   highlighted; otherwise false
	 * @param hasFocus   if true, render cell appropriately.
	 * @param row        the row index of the cell being drawn.
	 * @param column     the column index of the cell being drawn.
	 * @return the component used for drawing the cell.
	 */
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		if (isSelected) {
			renderButton.setForeground(table.getSelectionForeground());
			renderButton.setBackground(table.getSelectionBackground());
		} else {
			renderButton.setForeground(table.getForeground());
			renderButton.setBackground(UIManager.getColor("Button.background"));
		}

		if (hasFocus) {
			renderButton.setBorder(focusBorder);
		} else {
			renderButton.setBorder(originalBorder);
		}

		if (value == null) {
			renderButton.setText("");
			renderButton.setIcon(null);
		} else if (value instanceof Icon) {
			renderButton.setText("");
			renderButton.setIcon((Icon) value);
		} else {
			renderButton.setText(value.toString());
			renderButton.setIcon(null);
		}

		renderButton.setEnabled(isPreviewOnly ? false : true);

		return renderButton;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int row = table.convertRowIndexToModel(table.getEditingRow());
		fireEditingStopped();

		ActionEvent event = new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "" + row);
		action.actionPerformed(event);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (table.isEditing() && table.getCellEditor() == this)
			isButtonColumnEditor = true;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (isButtonColumnEditor && table.isEditing())
			table.getCellEditor().stopCellEditing();

		isButtonColumnEditor = false;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}
}
