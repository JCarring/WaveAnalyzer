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

/**
 * A {@link JTable} component for displaying beat selections, including their name and beat counts.
 */
public class BeatsSelectionTable extends JTable  {

	private static final long serialVersionUID = -5811412063023398537L;
	
	@SuppressWarnings("unused") // it's used just not in this thread
	private final SelectionTableListener listener;
	private final Font cell = new Font(Utils.getSmallTextFont().getFontName(), Font.BOLD, Utils.getSmallTextFont().getSize());
	private final Color lightGray = new Color(220, 220, 220);
	
	/**
	 * Generates a new {@link BeatsSelectionTable} instance.
	 *
	 * @param listener The listener to handle table events.
	 * @return A new instance of {@link BeatsSelectionTable}
	 */
	public static BeatsSelectionTable generate(SelectionTableListener listener ) {
		String[] cols = new String[] {"Name", "# Beats Top", "# Beats Bott", "Del"};
		DefaultTableModel model = new DefaultTableModel(null, cols);
		return new BeatsSelectionTable(model, listener);
	}
	
	/**
	 * Constructs a {@link BeatsSelectionTable}
	 *
	 * @param model    The table model to use.
	 * @param listener The listener for table selection events.
	 */
	private BeatsSelectionTable(DefaultTableModel model, SelectionTableListener listener) {
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
		
		ButtonColumn.addButtonColToTable(this, delete, 3, false);
		
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

		getColumnModel().getColumn(3).setPreferredWidth(Utils.getSmallTextFont().getSize() * 3);
		
		getTableHeader().setResizingAllowed(false);

	}
	
	/**
	 * Adds a beat selection to the table.
	 *
	 * @param selection The {@link BeatSelection} to add.
	 */
	public void addSelection(BeatSelection selection) {
		DefaultTableModel model = (DefaultTableModel) getModel();


		model.addRow(new Object[] {selection, selection.getNumberBeats("Top"), selection.getNumberBeats("Bottom"), Utils.IconFail});
	}
	
	/**
	 * Removes a specific beat selection from the table.
	 *
	 * @param selection The {@link BeatSelection} to remove.
	 */
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
	
	/**
	 * Removes all beat selections from the table.
	 */
	public void removeAllBeatSelections() {
		DefaultTableModel model = (DefaultTableModel) getModel();
		model.setRowCount(0);
	}
	
	/**
	 * An interface for listening to selection events from the {@link BeatsSelectionTable}
	 */
	public interface SelectionTableListener {
		
		/**
		 * Called when a selection is removed from the table.
		 * 
		 * @param selection The {@link BeatSelection} that was removed.
		 */
		public void tableSelectionRemoved(BeatSelection selection) ;
	}
	
}
