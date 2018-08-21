/*
 * Copyright 2016 Mikl&oacute;s Cs&#369;r&ouml;s.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package count.gui;


import java.awt.Font;
import javax.swing.JTable;

import count.gui.kit.DoubleRoundedForDisplay;
import count.model.IndexedTree;
import count.model.TreeWithRates;
import javax.swing.DefaultListSelectionModel;
/**
 * A table with its own model backed by a tree with loss-gain-duplication rates.
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 */
public class TreeRatesTable extends JTable
{
    
    public TreeRatesTable(TreeWithRates rates_model)
    {
        super(new RatesTableModel(rates_model), new javax.swing.table.DefaultTableColumnModel(), new LoggedSelectionModel());
        initSetup();
        
    }
    
    private void initSetup()
    {
        this.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        this.setColumnSelectionAllowed(false);
        this.setRowSelectionAllowed(true);
        this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.setFont(new Font("Serif",Font.PLAIN,Mien.TABLE_FONT_SIZE));
        this.createDefaultColumnsFromModel();
        this.getColumnModel().getColumn(0).setPreferredWidth(180); // first column should be wide (node name)
        this.setDefaultRenderer(DoubleRoundedForDisplay.class, new DoubleRoundedForDisplay.Renderer());
    }

    
    /**
     * {@link javax.swing.table.TableModel} for a {@link TreeWithRates} instance.
     */
    public static class RatesTableModel extends javax.swing.table.AbstractTableModel
    {
        private final TreeWithRates rates_model;
        
        public RatesTableModel(TreeWithRates rates_model)
        {
            this.rates_model = rates_model;
        }
    
        private IndexedTree getTree(){ return rates_model.getPhylogeny();}
        
        @Override
        public int getRowCount()
        {
            return getTree().getNumNodes();
        }

        @Override
        public int getColumnCount()
        {
            return 4;
        }

        @Override
        public Object getValueAt(int row, int column)
        {
            if (column == 0)
            {
                return Mien.getLongNodeName(getTree(), row);
            } else 
            {
                if (getTree().isRoot(row))
                    return "";
                else 
                {
                    if (column == 1)
                    {
                        double x = rates_model.getLossRate(row)*rates_model.getEdgeLength(row);
                        return new DoubleRoundedForDisplay(x);
                    } else if (column == 2)
                    {
                        double x = rates_model.getDuplicationRate(row)*rates_model.getEdgeLength(row);
                        return new DoubleRoundedForDisplay(x);
                    } else if (column == 3)
                    {
                        double x = rates_model.getGainRate(row)*rates_model.getEdgeLength(row);
                        return new DoubleRoundedForDisplay(x);
                    } else 
                        throw new IllegalArgumentException(".getValueAt --- column index must be 0..3 [got "+column+"]");
                }
            }
        }

        @Override
        public String getColumnName(int column)
        {
            if (column==0)
                return "Node";
            else if (column==1)
                return "Loss rate";
            else if (column == 2)
                return "Duplication rate";
            else if (column == 3)
                return "Gain rate";
            else 
                throw new IllegalArgumentException(".getColumnName --- column index must be 0..3 [got "+column+"]");

        }

        @Override
        public Class getColumnClass(int column)
        {
            if (column==0)
                return String.class;
            else return DoubleRoundedForDisplay.class;
        }
        
        
    }
    
    
    private static class LoggedSelectionModel extends DefaultListSelectionModel
    {
        LoggedSelectionModel(){ super();}
        
        @Override
        public void clearSelection()
        {
            log(".clearSelection");
            super.clearSelection();
        }
        
        @Override
        public void addSelectionInterval(int i, int j)
        {
            log(".add("+i+","+j);
            super.addSelectionInterval(i, j);
        }
        
        @Override
        public void setSelectionInterval(int i, int j)
        {
            log(".set("+i+","+j);
            super.setSelectionInterval(i, j);
        }
        
        
        private void log(String msg)
        {
            System.out.println("#*TRT.TS"+msg);
        }
    }
}
