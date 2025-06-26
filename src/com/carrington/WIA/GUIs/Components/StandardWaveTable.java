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

public class StandardWaveTable extends JTable {

	private static final long serialVersionUID = -8024206678386190448L;
	@SuppressWarnings("unused") // it's used just not in this thread
	private final StandardWaveTableListener listener;
	private final Font cellFont = new Font(Utils.getSmallTextFont().getFontName(), Font.BOLD,
			Utils.getSmallTextFont().getSize());
	private final WeakReference<JTable> ref = new WeakReference<JTable>(this);
	private WIAStats stats = null;
	
	public static StandardWaveTable generate(StandardWaveTableListener listener) {
		String[] cols = new String[] { "Wave", "D/P", "  #  ", "Info", "Del" };


		return new StandardWaveTable(cols, listener);

	}

	private StandardWaveTable(String[] cols, StandardWaveTableListener listener) {
		super(new MyTableModel(cols));

		this.listener = listener;
		int[] columnWidths = new int[] { Utils.getFontParams(cellFont, cols[0])[1],
				Utils.getFontParams(cellFont, cols[1])[1] * 2,
				Utils.getFontParams(cellFont, cols[2])[1] * 2,

				(int) (Utils.getFontParams(cellFont, cols[3])[1] * 1.5),
				(int) (Utils.getFontParams(cellFont, cols[4])[1] * 1.5),

		};

		Action delete = new AbstractAction() {
			private static final long serialVersionUID = 7684978405570273864L;

			public void actionPerformed(ActionEvent e) {

				JTable table = (JTable) e.getSource();
				int modelRow = Integer.valueOf(e.getActionCommand());
				StandardWave delete = stats.getWaves().get(modelRow);

				((MyTableModel) table.getModel()).removeRow(modelRow);
				stats.removeWave(delete);

				listener.removedWave(delete);

			}
		};

		Action info = new AbstractAction() {
			private static final long serialVersionUID = 7684978405570273864L;

			public void actionPerformed(ActionEvent e) {

				int modelRow = Integer.valueOf(e.getActionCommand());
				StandardWave wave = stats.getWaves().get(modelRow);

				JCPopupInfoList.displayPopupTwoFields("Files with wave \"" + wave.getName() + "\":", obtainListWIADataContaining(wave), 
						"Files without wave \"" + wave.getName() + "\":", obtainListWIADataNotContaining(wave), 
						ref.get());

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
				//setBackground(lightGray);
				//setFont(cellFont);
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
				        Utils.showError("This wave already exists!", ref.get());
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
	
	
	public void updateWaves(WIAStats wiastats) {
		if (stats != null && stats.getWaves().size() == this.getRowCount()) {
			MyTableModel model = (MyTableModel) getModel();
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

	public void addWaves(WIAStats wiastats) {
		removeWaves();
		this.stats = wiastats;
		MyTableModel model = (MyTableModel) getModel();
		for (StandardWave wave : this.stats.getWaves()) {
			model.addRow(new Object[] { wave, wave.isProximal() ? "P" : "D", wave.getCount(), Utils.IconQuestion, Utils.IconFail });

		}
		calculateNewSize();
	}

	public void removeWaves() {
		MyTableModel model = (MyTableModel) getModel();
		model.setRowCount(0);
		this.stats = null;
	}

	private void calculateNewSize() {
		MyTableModel model = (MyTableModel) getModel();
		int maxSize = 0;
		for (int i = 0; i < model.getRowCount(); i++) {
			maxSize = Math.max(maxSize, Utils.getFontParams(cellFont, model.getValueAt(i, 1).toString())[1]);
		}
		maxSize = Math.max(maxSize, getColumnModel().getColumn(1).getMaxWidth());
		getColumnModel().getColumn(1).setMinWidth(90);
		getColumnModel().getColumn(1).setMaxWidth(90);
		

	}
	
	private String[] obtainListWIADataContaining(StandardWave sw) {
		List<String> display = new ArrayList<String>();
		for (WIAData wiaData : sw.getSamples()) {
			display.add(wiaData.getSelectionName() + ":   " + wiaData.getData().getFile().getPath());
		}
		return display.toArray(new String[0]);
	}
	
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
	
	public Set<StandardWave> getSelectedWaves() {
		Set<StandardWave> waves = new HashSet<StandardWave>();
		
		int[] rowsSelected = this.getSelectedRows();
		for (int i : rowsSelected) {
			waves.add((StandardWave) getModel().getValueAt(i, 0));

		}

		return waves;
		
	}
	
	

	public interface StandardWaveTableListener {

		public void removedWave(StandardWave wave);
		public void changedWaveName();
		public Collection<WIAData> getData();
	}
	
	private static class MyTableModel extends DefaultTableModel {

		private static final long serialVersionUID = 5457283359118010870L;
		
		public MyTableModel(Object[] headers) {
			super(null, headers);
			
		}
		
		@Override
		public boolean isCellEditable(int row, int column) {
			return column != 1 && column != 2;
		}
		
	}

}




