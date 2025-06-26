package com.carrington.WIA.GUIs.Components;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.carrington.WIA.Utils;
import com.carrington.WIA.IO.WIAStats;
import com.carrington.WIA.IO.WIAStats.StandardTreatment;
import com.carrington.WIA.IO.WIAStats.StandardTreatmentType;

public class WIATxNameTable extends JTable {

	private static final long serialVersionUID = -8024206678386190448L;
	private final Font cellFont = new Font(Utils.getSmallTextFont().getFontName(), Font.BOLD,
			Utils.getSmallTextFont().getSize());
	private final WeakReference<JTable> ref = new WeakReference<JTable>(this);
	private WIAStats stats;
	
	public static WIATxNameTable generate() {
		String[] cols = new String[] { "Treatment", "  #  ", "Type", "Info"};


		return new WIATxNameTable(cols);

	}

	private WIATxNameTable(String[] cols) {
		super(new MyTableModel(cols));

		int[] columnWidths = new int[] { Utils.getFontParams(cellFont, cols[0])[1],
				Utils.getFontParams(cellFont, cols[1])[1] * 2,
				(int) (Utils.getFontParams(cellFont, cols[2])[1] * 2),
				(int) (Utils.getFontParams(cellFont, cols[3])[1] * 1.5),


		};
		
		int maxTypeStringWidth = 0;
		for (StandardTreatmentType type : StandardTreatmentType.values()) {
			int width = Utils.getFontParams(cellFont, type.getDisplayName())[1];
			maxTypeStringWidth = Math.max(maxTypeStringWidth, width);
		}
		maxTypeStringWidth*=1.5;
		columnWidths[2] = maxTypeStringWidth;

		DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = -5377598739589939281L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {

				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			}

		};
		
		DefaultTableCellRenderer rType = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = -5377598739589939281L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {

				Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				comp.setForeground(((StandardTreatmentType) value).getColor());
				return comp;

			}

		};

		Action info = new AbstractAction() {
			private static final long serialVersionUID = 7684978405570273864L;

			public void actionPerformed(ActionEvent e) {

				int modelRow = Integer.valueOf(e.getActionCommand());
				StandardTreatment tx = stats.getTreatments().get(modelRow);

				JCPopupInfoList.displayPopup("Files with treatment \"" + tx.getName() + "\":",
						tx.getSampleNames().toArray(new String[0]), ref.get());

			}
		};

		ButtonColumn.addButtonColToTable(this, info, 3, false);
		TableColumn typeColumn = getColumnModel().getColumn(2);
		JComboBox<StandardTreatmentType> txDropdown = new JComboBox<StandardTreatmentType>();
		for (StandardTreatmentType type : StandardTreatmentType.values()) {
			txDropdown.addItem(type);
		}
		
		DefaultCellEditor editor = new DefaultCellEditor(txDropdown);
		typeColumn.setCellEditor(editor);
		
		

		getColumnModel().getColumn(0).setCellRenderer(r);
		getColumnModel().getColumn(1).setCellRenderer(r);
		getColumnModel().getColumn(2).setCellRenderer(rType);


		getModel().addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE) {
					int row = e.getFirstRow();
					int column = e.getColumn();
					
					if (column != 2) {
						return;
					}
					
					TableModel model = (TableModel) e.getSource();
					StandardTreatmentType txType = (StandardTreatmentType) model.getValueAt(row, column);
					stats.getTreatments().get(row).setTreatmentType(txType);

				}

			}
		});

		setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);

		getColumnModel().getColumn(1).setMinWidth(columnWidths[1]);
		getColumnModel().getColumn(1).setMaxWidth(columnWidths[1]);
		getColumnModel().getColumn(2).setMinWidth(columnWidths[2]);
		getColumnModel().getColumn(2).setMaxWidth(columnWidths[2]);
		getColumnModel().getColumn(3).setMinWidth(columnWidths[3]);
		getColumnModel().getColumn(3).setMaxWidth(columnWidths[3]);


		getTableHeader().setReorderingAllowed(false);
		getTableHeader().setResizingAllowed(false);
		setRowSelectionAllowed(false);
	}

	public void updateTreatments(WIAStats wiastats) {
		if (stats != null && stats.getTreatments().size() == this.getRowCount()) {
			MyTableModel model = (MyTableModel) getModel();
			int r = 0;

			for (StandardTreatment treatment : this.stats.getTreatments()) {
				
				model.setValueAt(treatment.getName(), r, 0);
				model.setValueAt(treatment.getCount(), r, 1);
				model.setValueAt(treatment.getTreatmentType(), r, 2);
				model.setValueAt(Utils.IconQuestion, r, 3);
				
				r++;
			}
		} else {
			removeData();
			addTreatments(wiastats);
		}
	}

	
	public void addTreatments(WIAStats wiastats) {
		
		removeData();
		this.stats = wiastats;
		MyTableModel model = (MyTableModel) getModel();
		
		for (StandardTreatment treatment : wiastats.getTreatments()) {
			model.addRow(new Object[] { treatment.getName(), treatment.getCount(), treatment.getTreatmentType(), Utils.IconQuestion});

		}
		
	}

	public void removeData() {
		this.stats = null;
		MyTableModel model = (MyTableModel) getModel();
		model.setRowCount(0);
	}


	
	
	private static class MyTableModel extends DefaultTableModel {

		private static final long serialVersionUID = 5457283359118010870L;
		
		public MyTableModel(Object[] headers) {
			super(null, headers);
			
		}
		
		@Override
		public boolean isCellEditable(int row, int column) {
			return (column > 1);
		}
		
	}

}




