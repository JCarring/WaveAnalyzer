package com.carrington.WIA.GUIs.Components;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.Iterator;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.carrington.WIA.Utils;
import com.carrington.WIA.IO.WIAStats;
import com.carrington.WIA.IO.WIAStats.StandardWave;
import com.carrington.WIA.IO.WIAStats.StandardWaveGrouping;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * A {@link JTable} component for displaying and managing standard wave
 * groupings.
 */
public class StandardWaveGroupTable extends JTable {

	private static final long serialVersionUID = -8024206678386190448L;
	@SuppressWarnings("unused") // it's used just not in this thread
	private final StandardWaveGroupTableListener listener;
	private final Font cellFont = new Font(Utils.getSmallTextFont().getFontName(), Font.BOLD,
			Utils.getSmallTextFont().getSize());
	private final WeakReference<JTable> ref = new WeakReference<JTable>(this);
	private WIAStats stats = null;

	/**
	 * Generates a new {@link StandardWaveGroupTable} instance.
	 *
	 * @param listener The listener to handle table events.
	 * @return A new instance of {@link StandardWaveGroupTable}
	 */
	public static StandardWaveGroupTable generate(StandardWaveGroupTableListener listener) {
		String[] cols = new String[] { "Group", "Waves", "Info", "Del" };

		return new StandardWaveGroupTable(cols, listener);

	}

	/**
	 * Constructs a table.
	 *
	 * @param cols     An array of strings for the column headers.
	 * @param listener The listener for table events.
	 */
	private StandardWaveGroupTable(String[] cols, StandardWaveGroupTableListener listener) {
		super(new CustomTableModel(cols));

		this.listener = listener;
		int[] columnWidths = new int[] { Utils.getFontParams(cellFont, cols[0])[1] * 2,
				Utils.getFontParams(cellFont, cols[1])[1] * 2, (int) (Utils.getFontParams(cellFont, cols[2])[1] * 1.5),
				(int) (Utils.getFontParams(cellFont, cols[3])[1] * 1.5),

		};

		Action delete = new AbstractAction() {
			private static final long serialVersionUID = 7684978405570273864L;

			public void actionPerformed(ActionEvent e) {

				JTable table = (JTable) e.getSource();
				int modelRow = Integer.valueOf(e.getActionCommand());

				StandardWaveGrouping delete = stats.getWaveGrouping().get(modelRow);

				((CustomTableModel) table.getModel()).removeRow(modelRow);
				stats.removeGrouping(delete);
				listener.removedWaveGroup(delete);

			}
		};

		Action info = new AbstractAction() {
			private static final long serialVersionUID = 7684978405570273864L;

			public void actionPerformed(ActionEvent e) {

				int modelRow = Integer.valueOf(e.getActionCommand());
				StandardWaveGrouping waveGroup = stats.getWaveGrouping().get(modelRow);

				JCPopupInfoList.displayPopup("Waves included in \"" + waveGroup.getName() + "\":",
						waveGroup.getStandardWaves().toArray(new StandardWave[0]), ref.get());

			}
		};

		ButtonColumn.addButtonColToTable(this, info, 2, false);
		ButtonColumn.addButtonColToTable(this, delete, 3, false);

		DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = -5377598739589939281L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				return this;
			}

		};

		getModel().addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE) {

					int row = e.getFirstRow();
					int column = e.getColumn();

					if (column != 0) {
						return;
					}

					TableModel model = (TableModel) e.getSource();

					String newName = model.getValueAt(row, column).toString();

					Iterator<StandardWaveGrouping> groupItr = stats.getWaveGrouping().iterator();
					int currRow = 0;
					while (groupItr.hasNext()) {
						if (groupItr.next().getName().equals(newName) && currRow != row) {
							currRow = -1;
							break;
						} else {
							currRow++;
						}
					}

					if (currRow == -1) {
						Utils.showMessage(Utils.ERROR, "This group already exists!", ref.get());
						model.setValueAt(stats.getWaveGrouping().get(row).getName(), row, column);
					} else {
						stats.getWaveGrouping().get(row).setName(newName);

					}

				}

			}
		});

		for (int i = 0; i < getColumnCount() - 2; i++) {
			getColumnModel().getColumn(i).setCellRenderer(r);
		}

		setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);

		getColumnModel().getColumn(1).setMinWidth(columnWidths[1]);
		getColumnModel().getColumn(1).setMaxWidth(columnWidths[1]);
		getColumnModel().getColumn(2).setMinWidth(columnWidths[2]);
		getColumnModel().getColumn(2).setMaxWidth(columnWidths[2]);
		getColumnModel().getColumn(3).setMinWidth(columnWidths[3]);
		getColumnModel().getColumn(3).setMaxWidth(columnWidths[3]);

		getTableHeader().setReorderingAllowed(false);

		getTableHeader().setResizingAllowed(false);
	}

	/**
	 * Updates the groups displayed in the table.
	 *
	 * @param wiastats The {@link WIAStats} containing the new group data.
	 */
	public void updateGroups(WIAStats wiastats) {
		if (stats != null && stats.getWaveGrouping().size() == this.getRowCount()) {
			CustomTableModel model = (CustomTableModel) getModel();
			int r = 0;

			for (StandardWaveGrouping group : this.stats.getWaveGrouping()) {

				model.setValueAt(group.getName(), r, 0);
				model.setValueAt(group.getStandardWavesString(), r, 1);
				model.setValueAt(Utils.IconQuestion, r, 2);
				model.setValueAt(Utils.IconFail, r, 3);

				r++;
			}
		} else {
			removeGroups();
			addGroups(wiastats);
		}
	}

	/**
	 * Adds groups to the table from the provided WIAStats.
	 *
	 * @param wiastats The {@link WIAStats} containing the group data.
	 */
	public void addGroups(WIAStats wiastats) {
		this.stats = wiastats;
		CustomTableModel model = (CustomTableModel) getModel();
		for (StandardWaveGrouping group : this.stats.getWaveGrouping()) {
			model.addRow(new Object[] { group.getName(), group.getStandardWavesString(), Utils.IconQuestion,
					Utils.IconFail });

		}
	}

	/**
	 * Removes all groups from the table.
	 */
	public void removeGroups() {
		CustomTableModel model = (CustomTableModel) getModel();
		model.setRowCount(0);
		this.stats = null;
	}

	/**
	 * Listener for events from the table.
	 */
	public interface StandardWaveGroupTableListener {

		/**
		 * Called when a wave group is removed from the table.
		 * 
		 * @param wave The {@link StandardWaveGrouping} that was removed.
		 */
		public void removedWaveGroup(StandardWaveGrouping wave);
	}

	/**
	 * custom table model
	 */
	private static class CustomTableModel extends DefaultTableModel {

		private static final long serialVersionUID = 5457283359118010870L;

		/**
		 * Constructs a model with specified column headers.
		 * 
		 * @param headers An array of objects for the column headers.
		 */
		public CustomTableModel(Object[] headers) {
			super(null, headers);
		}

		/**
		 * Determines if a cell is editable. In this model, only the group name column
		 * is editable.
		 * 
		 * @param row    The row of the cell.
		 * @param column The column of the cell.
		 * @return True if the cell is editable, false otherwise.
		 */
		@Override
		public boolean isCellEditable(int row, int column) {
			return column != 1;
		}

	}

}
