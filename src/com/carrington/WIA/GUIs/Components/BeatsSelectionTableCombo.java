package com.carrington.WIA.GUIs.Components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.carrington.WIA.Utils;
import com.carrington.WIA.Cardio.BeatSelection;

public class BeatsSelectionTableCombo extends JTable  {

	private static final long serialVersionUID = -5811412063023398537L;
	
	//private ButtonColumn buttonCol; // TODO may need to re-instate this
	@SuppressWarnings("unused") // it's used just not in this thread
	private final SelectionTableListener listener;
	private final Font cell = new Font(Utils.getSmallTextFont().getFontName(), Font.BOLD, Utils.getSmallTextFont().getSize());
	private final Color lightGray = new Color(220, 220, 220);
	
	public static BeatsSelectionTableCombo generate(SelectionTableListener listener ) {
		String[] cols = new String[] {"Name", "# Beats", "Del"};
		DefaultTableModel model = new DefaultTableModel(null, cols);
		return new BeatsSelectionTableCombo(model, listener);
	}
	
	private BeatsSelectionTableCombo(DefaultTableModel model, SelectionTableListener listener) {
		super(model);
		
		this.listener = listener;
		
		Action delete = new AbstractAction()
		{
			private static final long serialVersionUID = 7684978405570273864L;

			public void actionPerformed(ActionEvent e)
			{

				JTable table = (JTable)e.getSource();
				int modelRow = Integer.valueOf( e.getActionCommand() );
				BeatSelection delete = (BeatSelection) table.getModel().getValueAt(modelRow, 0);
				
				((DefaultTableModel)table.getModel()).removeRow(modelRow);
				
				listener.tableSelectionRemoved(delete);

			}
		};
		
		ButtonColumn.addButtonColToTable(this, delete, 2, false);
		
		DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
		    private static final long serialVersionUID = -5377598739589939281L;

		    @Override
		    public Component getTableCellRendererComponent(JTable table,
		            Object value, boolean isSelected, boolean hasFocus,
		            int row, int column) {
		        super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
		                row, column);
		        //setFont(cell);
		        setBackground(lightGray);
		        setFont(cell);
		        return this;
		    }

		};
		
		
		
		for (int i = 0; i < getColumnCount() - 1; i++) {
			getColumnModel().getColumn(i).setCellRenderer(r);
		}
		
		
		getTableHeader().setReorderingAllowed(false);

		getColumnModel().getColumn(2).setPreferredWidth(Utils.getSmallTextFont().getSize() * 3);
		
		getTableHeader().setResizingAllowed(false);

	}
	
	

	public void addSelection(BeatSelection selection) {
		DefaultTableModel model = (DefaultTableModel) getModel();


		model.addRow(new Object[] {selection, selection.getNumberBeats("Combo"), Utils.IconFail});
	}
	
	public void removeSelection(BeatSelection selection) {
		DefaultTableModel model = (DefaultTableModel) getModel();
		for (int row = 0; row < model.getRowCount(); row++) {
			BeatSelection qSel = (BeatSelection) model.getValueAt(row, 0);
			if (qSel.equals(selection)) {
				model.removeRow(row);
				break;
			}
		}
	}
	
	public void removeAllBeatSelections() {
		DefaultTableModel model = (DefaultTableModel) getModel();
		model.setRowCount(0);
	}
	
	public interface SelectionTableListener {
		
		public void tableSelectionRemoved(BeatSelection selection) ;
	}
	
}
