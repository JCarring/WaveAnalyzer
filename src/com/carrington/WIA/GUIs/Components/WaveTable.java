package com.carrington.WIA.GUIs.Components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.carrington.WIA.Utils;
import com.carrington.WIA.Cardio.Wave;

import javax.swing.*;

public class WaveTable extends JTable{
	
	private static final long serialVersionUID = 5102361285897241470L;
	//private ButtonColumn buttonCol; // TODO may need to re-instate this
	@SuppressWarnings("unused") // it's used just not in this thread
	private final WaveTableListener listener;
	private final Font cell = new Font(Utils.getSmallTextFont().getFontName(), Font.BOLD, Utils.getSmallTextFont().getSize());
	private final Color lightGray = new Color(220, 220, 220);
	
	
	public static WaveTable generate(WaveTableListener listener, boolean isPreviewOnly) {
		String[] cols = new String[] {"Wave", "Intensity", "Del"};
		
		DefaultTableModel model = new DefaultTableModel(null, cols);
		return new WaveTable(model, listener, isPreviewOnly);

	}
	
	private WaveTable(DefaultTableModel model, WaveTableListener listener, boolean isPreviewOnly) {
		super(model);
		
		this.listener = listener;

		Action delete = new AbstractAction()
		{
			private static final long serialVersionUID = 7684978405570273864L;

			public void actionPerformed(ActionEvent e)
			{

				JTable table = (JTable)e.getSource();
				int modelRow = Integer.valueOf( e.getActionCommand() );
				Wave delete = (Wave) table.getModel().getValueAt(modelRow, 0);
				
				((DefaultTableModel)table.getModel()).removeRow(modelRow);
				
				listener.removeWave(delete);

			}
		};
		
		ButtonColumn.addButtonColToTable(this, delete, 2, isPreviewOnly);
		
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
		getColumnModel().getColumn(0).setPreferredWidth(Utils.getSmallTextFont().getSize() * 5);
		getColumnModel().getColumn(2).setPreferredWidth(Utils.getSmallTextFont().getSize() * 3);
		
		getTableHeader().setResizingAllowed(false);
	}
	
	public void addWave(Wave wave) {
		DefaultTableModel model = (DefaultTableModel) getModel();
		String value = null;
		if (Double.isNaN(wave.getCumulativeIntensity())) {
			value = "";
		} else {
			value = _formatToOneDecimal(wave.getCumulativeIntensity());
		}
		model.addRow(new Object[] {wave, value, Utils.IconFail});
	}
	

	private static String _formatToOneDecimal(double number) {
	    return String.format("%.1f", number);
	}
	
	public void removeWave(Wave wave) {
		DefaultTableModel model = (DefaultTableModel) getModel();
		for (int row = 0; row < model.getRowCount(); row++) {
			Wave qWave = (Wave) model.getValueAt(row, 0);
			if (qWave.equals(wave)) {
				model.removeRow(row);
				break;
			}
		}
	}
	
	public void removeAllWaves() {
		DefaultTableModel model = (DefaultTableModel) getModel();
		model.setRowCount(0);
	}
	
	public interface WaveTableListener {
		
		public void removeWave(Wave wave);
	}
	
}

