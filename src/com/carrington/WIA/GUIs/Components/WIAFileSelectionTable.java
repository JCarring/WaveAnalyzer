package com.carrington.WIA.GUIs.Components;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.carrington.WIA.Utils;
import com.carrington.WIA.DataStructures.WIAData;
import com.carrington.WIA.IO.WIAStats;

import javax.swing.*;

/**
 * A {@link JTable} component for displaying and managing selected WIA (Waveform
 * Information Analysis) files.
 */
public class WIAFileSelectionTable extends JTable {

	private static final long serialVersionUID = 5102361285897241470L;
	@SuppressWarnings("unused") // it's used just not in this thread
	private final WIATableListener listener;
	private final Font cellFont = new Font(Utils.getSmallTextFont().getFontName(), Font.BOLD,
			Utils.getSmallTextFont().getSize());
	private WIAStats stats;
	private WeakReference<Component> ref = null;

	/**
	 * Generates a new table instance.
	 *
	 * @param listener             The listener to handle table events.
	 * @param parentForPositioning The parent component used for positioning
	 *                             dialogs.
	 * @return A new instance of {@link WIAFileSelectionTable}
	 */
	public static WIAFileSelectionTable generate(WIATableListener listener, Component parentForPositioning) {
		String[] cols = new String[] { "Original File", "Treatment", "WIA File Path", "Del" };

		DefaultTableModel model = new DefaultTableModel(null, cols) {

			private static final long serialVersionUID = -7143568837846607098L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 1 || column == 3;
			}
		};
		return new WIAFileSelectionTable(model, listener, parentForPositioning);

	}

	/**
	 * Constructs a table.
	 *
	 * @param model                The table model to use.
	 * @param listener             The listener for table events.
	 * @param parentForPositioning The parent component for positioning dialogs.
	 */
	private WIAFileSelectionTable(DefaultTableModel model, WIATableListener listener, Component parentForPositioning) {
		super(model);
		ref = new WeakReference<Component>(parentForPositioning != null ? parentForPositioning : this);

		List<Integer> widths = new ArrayList<Integer>();
		for (int i = 0; i < model.getColumnCount(); i++) {
			widths.add(Utils.getFontParams(cellFont, model.getColumnName(i))[1] * 2);
		}
		int[] columnWidths = Utils.toPrimitiveInteger(widths);
		this.listener = listener;

		Action delete = new AbstractAction() {
			private static final long serialVersionUID = 7684978405570273864L;

			public void actionPerformed(ActionEvent e) {

				JTable table = (JTable) e.getSource();
				int modelRow = Integer.valueOf(e.getActionCommand());
				WIAData delete = (WIAData) table.getModel().getValueAt(modelRow, 0);

				((DefaultTableModel) table.getModel()).removeRow(modelRow);
				stats.removeData(delete);

				if (listener != null) {
					listener.wiaDataRemoved();
				}

			}
		};

		ButtonColumn.addButtonColToTable(this, delete, 3, false);

		DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = -5377598739589939281L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			}

		};

		for (int i = 0; i < getColumnCount() - 1; i++) {
			getColumnModel().getColumn(i).setCellRenderer(r);
		}

		getModel().addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE) {
					int row = e.getFirstRow();
					int column = e.getColumn();

					if (column != 1) {
						return;
					}

					TableModel model = (TableModel) e.getSource();
					WIAData data = (WIAData) model.getValueAt(row, 0);
					String newName = model.getValueAt(row, column).toString();
					if (newName.isEmpty()) {
						Utils.showError("Treatment name cannot be empty!", ref.get());
						model.setValueAt(data.getSelectionName(), row, column);

					}

					stats.setStandardTreatmentName(newName, data);

					if (listener != null) {
						listener.wiaDataTxNameChanged();
					}

				}

			}
		});

		getTableHeader().setReorderingAllowed(false);
		setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);

		getColumnModel().getColumn(0).setMinWidth(columnWidths[0]);
		getColumnModel().getColumn(0).setMaxWidth(columnWidths[0]);
		getColumnModel().getColumn(1).setMinWidth(columnWidths[1]);
		getColumnModel().getColumn(1).setMaxWidth(columnWidths[1]);
		getColumnModel().getColumn(3).setMinWidth(columnWidths[3]);
		getColumnModel().getColumn(3).setMaxWidth(columnWidths[3]);
		getTableHeader().setResizingAllowed(false);
	}

	/**
	 * Adds WIA data to the table from the provided WIAStats.
	 *
	 * @param wiaStats The {@link WIAStats} containing the data to add.
	 */
	public void addWIAData(WIAStats wiaStats) {
		removeAllWIAData();
		this.stats = wiaStats;
		for (WIAData wiaData : wiaStats.getData()) {
			DefaultTableModel model = (DefaultTableModel) getModel();
			model.addRow(new Object[] { wiaData, wiaData.getSelectionName(), wiaData.getSerializeFileSource().getPath(),
					Utils.IconFail });
		}

		calculateNewSize();
	}

	/**
	 * Removes all WIA data from the table.
	 */
	public void removeAllWIAData() {
		this.stats = null;
		DefaultTableModel model = (DefaultTableModel) getModel();
		model.setRowCount(0);
	}

	/**
	 * Calculates and sets the preferred width of columns based on their content.
	 */
	private void calculateNewSize() {
		DefaultTableModel model = (DefaultTableModel) getModel();
		int maxSize = 0;
		for (int i = 0; i < model.getRowCount(); i++) {
			maxSize = Math.max(maxSize, Utils.getFontParams(cellFont, model.getValueAt(i, 0).toString())[1]);
		}
		maxSize = Math.max(maxSize, getColumnModel().getColumn(0).getMaxWidth());
		getColumnModel().getColumn(0).setMinWidth(maxSize);
		getColumnModel().getColumn(0).setMaxWidth(maxSize);

		maxSize = 0;
		for (int i = 0; i < model.getRowCount(); i++) {
			maxSize = Math.max(maxSize, Utils.getFontParams(cellFont, model.getValueAt(i, 1).toString())[1]);
		}
		maxSize = Math.max(maxSize, getColumnModel().getColumn(1).getMaxWidth());
		getColumnModel().getColumn(1).setMinWidth(maxSize);
		getColumnModel().getColumn(1).setMaxWidth(maxSize);
	}

	/**
	 * An interface for listening to events from the {@link WIAFileSelectionTable}
	 */
	public interface WIATableListener {

		/**
		 * Called when WIA data is removed from the table.
		 */
		public void wiaDataRemoved();

		/**
		 * Called when the treatment name of a WIA data entry is changed.
		 */
		public void wiaDataTxNameChanged();

	}

}
