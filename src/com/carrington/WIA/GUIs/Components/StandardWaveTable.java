package com.carrington.WIA.GUIs.Components;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.carrington.WIA.Utils;
import com.carrington.WIA.DataStructures.WIAData;
import com.carrington.WIA.IO.WIAStats;
import com.carrington.WIA.IO.WIAStats.StandardWave;

import javax.swing.*;

/**
 * A {@link JTable} component for displaying and managing standard wave data,
 * allowing for editing, deletion, and viewing information about each wave.
 */
public class StandardWaveTable extends JTable {

	private static final long serialVersionUID = -8024206678386190448L;
	private final StandardWaveTableListener listener;
	private final Font cellFont = new Font(Utils.getSmallTextFont().getFontName(), Font.BOLD,
			Utils.getSmallTextFont().getSize());
	private final WeakReference<JTable> ref = new WeakReference<JTable>(this);
	private WIAStats stats = null;

	/**
	 * Generates a new StandardWaveTable instance with specified columns.
	 *
	 * @param listener The listener to handle table events.
	 * @return A new instance of StandardWaveTable.
	 */
	public static StandardWaveTable generate(StandardWaveTableListener listener) {
		String[] cols = new String[] { "Wave", "D/P", "  #  ", "Info", "Del" };

		return new StandardWaveTable(cols, listener);

	}

	/**
	 * Constructs a StandardWaveTable.
	 *
	 * @param cols     An array of strings for the column headers.
	 * @param listener The listener for table events.
	 */
	private StandardWaveTable(String[] cols, StandardWaveTableListener listener) {
		super(new CustomTableModel(cols));

		this.listener = listener;
		int[] columnWidths = new int[] { Utils.getFontParams(cellFont, cols[0])[1],
				Utils.getFontParams(cellFont, cols[1])[1] * 2, Utils.getFontParams(cellFont, cols[2])[1] * 2,

				(int) (Utils.getFontParams(cellFont, cols[3])[1] * 1.5),
				(int) (Utils.getFontParams(cellFont, cols[4])[1] * 1.5),

		};

		Action delete = new AbstractAction() {
			private static final long serialVersionUID = 7684978405570273864L;

			public void actionPerformed(ActionEvent e) {

				JTable table = (JTable) e.getSource();
				int modelRow = Integer.valueOf(e.getActionCommand());
				StandardWave delete = stats.getWaves().get(modelRow);

				((CustomTableModel) table.getModel()).removeRow(modelRow);
				stats.removeWave(delete);

				listener.removedWave(delete);

			}
		};

		Action info = new AbstractAction() {
			private static final long serialVersionUID = 7684978405570273864L;

			public void actionPerformed(ActionEvent e) {

				int modelRow = Integer.valueOf(e.getActionCommand());
				StandardWave wave = stats.getWaves().get(modelRow);

				JCPopupInfoList.displayPopupTwoFields("Files with wave \"" + wave.getName() + "\":",
						obtainListWIADataContaining(wave), "Files without wave \"" + wave.getName() + "\":",
						obtainListWIADataNotContaining(wave), ref.get());

			}
		};

		ButtonColumn.addButtonColToTable(this, info, 3, false);
		ButtonColumn.addButtonColToTable(this, delete, 4, false);

		DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = -5377598739589939281L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				// setFont(cell);
				// setBackground(lightGray);
				// setFont(cellFont);
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

					Iterator<StandardWave> waveItr = stats.getWaves().iterator();
					int currRow = 0;
					while (waveItr.hasNext()) {
						if (waveItr.next().getName().equals(newName) && currRow != row) {
							currRow = -1;
							break;
						} else {
							currRow++;
						}
					}

					if (currRow == -1) {
						Utils.showMessage(JOptionPane.ERROR_MESSAGE, "This wave already exists!", ref.get());
						model.setValueAt(stats.getWaves().get(row).getName(), row, column);
					} else {
						stats.getWaves().get(row).setName(newName);
						listener.changedWaveName();

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
		getColumnModel().getColumn(4).setMinWidth(columnWidths[4]);
		getColumnModel().getColumn(4).setMaxWidth(columnWidths[4]);

		getTableHeader().setReorderingAllowed(false);

		getTableHeader().setResizingAllowed(false);
		setRowSelectionAllowed(true);
	}

	/**
	 * Updates the waves displayed in the table. If the number of waves is the same,
	 * it updates the existing rows. Otherwise, it clears and re-adds all waves.
	 *
	 * @param wiastats The {@link WIAStats} containing the new wave data.
	 */
	public void updateWaves(WIAStats wiastats) {
		if (stats != null && stats.getWaves().size() == this.getRowCount()) {
			CustomTableModel model = (CustomTableModel) getModel();
			int r = 0;

			for (StandardWave wave : this.stats.getWaves()) {

				model.setValueAt(wave.toString(), r, 0);
				model.setValueAt(wave.isProximal() ? "P" : "D", r, 1);
				model.setValueAt(wave.getCount(), r, 2);
				model.setValueAt(Utils.IconQuestion, r, 3);
				model.setValueAt(Utils.IconFail, r, 4);

				r++;
			}
		} else {
			removeWaves();
			addWaves(wiastats);
		}
	}

	/**
	 * Clears the table and adds new waves from the provided stats.
	 *
	 * @param wiastats The {@link WIAStats} containing the wave data to add.
	 */
	public void addWaves(WIAStats wiastats) {
		removeWaves();
		this.stats = wiastats;
		CustomTableModel model = (CustomTableModel) getModel();
		for (StandardWave wave : this.stats.getWaves()) {
			model.addRow(new Object[] { wave, wave.isProximal() ? "P" : "D", wave.getCount(), Utils.IconQuestion,
					Utils.IconFail });

		}
		calculateNewSize();
	}

	/**
	 * Removes all waves from the table and clears the associated statistics data.
	 */
	public void removeWaves() {
		CustomTableModel model = (CustomTableModel) getModel();
		model.setRowCount(0);
		this.stats = null;
	}

	/**
	 * Calculates and sets the preferred width of the first column based on the
	 * content.
	 */
	private void calculateNewSize() {
		CustomTableModel model = (CustomTableModel) getModel();
		int maxSize = 0;
		for (int i = 0; i < model.getRowCount(); i++) {
			maxSize = Math.max(maxSize, Utils.getFontParams(cellFont, model.getValueAt(i, 1).toString())[1]);
		}
		maxSize = Math.max(maxSize, getColumnModel().getColumn(1).getMaxWidth());
		getColumnModel().getColumn(1).setMinWidth(90);
		getColumnModel().getColumn(1).setMaxWidth(90);

	}

	/**
	 * Obtains a list of file paths for {@link WIAData} samples that contain the
	 * specified standard wave.
	 * 
	 * @param sw The standard wave to check for.
	 * 
	 * @return An array of strings representing the file paths.
	 */
	private String[] obtainListWIADataContaining(StandardWave sw) {
		List<String> display = new ArrayList<String>();
		for (WIAData wiaData : sw.getSamples()) {
			display.add(wiaData.getSelectionName() + ":   " + wiaData.getData().getFile().getPath());
		}
		return display.toArray(new String[0]);
	}

	/**
	 * Obtains a list of file paths for {@link WIAData} samples that do not contain
	 * the specified standard wave.
	 * 
	 * @param sw The {@link StandardWave} to check against.
	 * @return An array of strings representing the file paths.
	 */
	private String[] obtainListWIADataNotContaining(StandardWave sw) {
		List<String> display = new ArrayList<String>();
		Collection<WIAData> data = listener.getData();
		for (WIAData wiaData : data) {
			if (!sw.hasWave(wiaData)) {
				display.add(wiaData.getSelectionName() + ":   " + wiaData.getData().getFile().getPath());
			}
		}
		return display.toArray(new String[0]);
	}

	/**
	 * Retrieves the set of {@link StandardWave} objects corresponding to the
	 * selected rows in the table.
	 *
	 * @return A Set of selected {@link StandardWave} objects.
	 */
	public Set<StandardWave> getSelectedWaves() {
		Set<StandardWave> waves = new HashSet<StandardWave>();

		int[] rowsSelected = this.getSelectedRows();
		for (int i : rowsSelected) {
			waves.add((StandardWave) getModel().getValueAt(i, 0));

		}

		return waves;

	}

	/**
	 * An interface for listening to events from the {@link StandardWaveTable}
	 */
	public interface StandardWaveTableListener {

		/**
		 * Called when a wave is removed from the table.
		 * 
		 * @param wave The {@link StandardWave} that was removed.
		 */
		public void removedWave(StandardWave wave);

		/**
		 * Called when a wave's name is changed in the table.
		 */
		public void changedWaveName();

		/**
		 * Called to retrieve the collection of data.
		 * 
		 * @return A collection of {@link WIAData}
		 */
		public Collection<WIAData> getData();
	}

	/**
	 * A custom table model for the StandardWaveTable.
	 */
	private static class CustomTableModel extends DefaultTableModel {

		private static final long serialVersionUID = 5457283359118010870L;

		/**
		 * @param headers An array of objects for the column headers.
		 */
		public CustomTableModel(Object[] headers) {
			super(null, headers);

		}

		/**
		 * Determines if a cell is editable. In this model, only the first column (wave
		 * name) is editable.
		 * 
		 * @param row    The row of the cell.
		 * @param column The column of the cell.
		 * @return True if the cell is editable, false otherwise.
		 */
		@Override
		public boolean isCellEditable(int row, int column) {
			return column != 1 && column != 2;
		}

	}

}
