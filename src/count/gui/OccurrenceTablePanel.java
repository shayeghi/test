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

import count.gui.kit.FrozenColumnsTable;
import count.model.OccurrenceTable;
import count.io.DataFile;
import count.model.PhyleticProfile;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Swing component (JPanel) for displaying an OccurrenceTable.
 *
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 */
public class OccurrenceTablePanel 
        extends Browser.PrimaryItem // which is a JPanel
        implements ListSelectionListener
{
    private final DataFile<OccurrenceTable> data;

    /**
     *  Initializes this display with the given OccurrenceTable file.
     * 
     *  @param T the underlying data file
     */
    public OccurrenceTablePanel(DataFile<OccurrenceTable> T)
    {
        this.data = T;
        initComponents();
    }
    
    /**
     * Table for displaying the data set;
     */
    private JTable table;
    
    /**
     * Scroll pane for holding the data set
     */
    private FrozenColumnsTable<OccurrenceTableModel> table_scroll;
    

    /**
     * A label about what families are selected: placed at the bottom.
     */
    private JLabel selected_rows_information;

    /**
     * A semaphore variable for managing the displayed
     * information about the current family selection.
     * Families can be selected by usual table selection,
     * or by a double-click in a cell. In the latter case,
     * the information can be set by the selectSimilarFamilies()
     * method. The default information is displayed by a ListSelectionListener
     * for the family table. This variable is set to false in
     * order to disable the update by the selection listener
     * when the family selection is done by a computation.
     *
     */
    protected boolean update_selected_rows_information = true;

    
    /**
     * Initialization of components within this display.
     */
    private void initComponents()
    {
        setBackground(Color.WHITE);
        OccurrenceTableModel M = new OccurrenceTableModel(data.getContent(), true);
        table_scroll = new FamilyScroll(M);
        
        table = table_scroll.getDataTable();
        table.setFont(new Font("Serif",Font.PLAIN,Mien.TABLE_FONT_SIZE));
        
        M.setDefaultRenderers(table);
        
        // row header data_table
        JTable row_header =table_scroll.getHeaderTable();
        row_header.setFont(table.getFont());

        table_scroll.getViewport().setBackground(getBackground());
        
        Font tp_font_rm = table.getFont().deriveFont(0.8f);
        Font tp_font_it = tp_font_rm.deriveFont(Font.ITALIC);

        selected_rows_information = new JLabel(":");
        selected_rows_information.setFont(tp_font_it.deriveFont(Mien.TABLE_FONT_SIZE*0.8f));
        selected_rows_information.setOpaque(false);
        selected_rows_information.setLabelFor(table);
        selected_rows_information.setMaximumSize(new Dimension(520,30)); // not even a constant setup for this
        selected_rows_information.setMinimumSize(selected_rows_information.getMaximumSize());
        selected_rows_information.setPreferredSize(selected_rows_information.getMaximumSize());
        
        table.getSelectionModel().addListSelectionListener(this);
        table.setRowSelectionInterval(0,0);

        this.setLayout(new BorderLayout());
        this.add(table_scroll, BorderLayout.CENTER);
        this.add(selected_rows_information, BorderLayout.SOUTH);
    }

    private void displaySelectionInfo(String text)
    {
        if (selected_rows_information != null)
            selected_rows_information.setText(text);
    }

    /**
     * We are listening to selection changes in the family table.
     * When selection changes, the selection inormation in the bottom
     * bar is updated.
     *
     * @param e a list selection event: the info is updated only if the event is not adjusting
     */
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (!e.getValueIsAdjusting())
        {
            if (update_selected_rows_information)
            {

                int num_selected = table.getSelectedRowCount();


                String selection_info_text = "";
                if (num_selected == 0)
                    selection_info_text = "No rows selected.";
                else
                {
                    if (num_selected==1)
                        selection_info_text = "One row selected";
                    else
                        selection_info_text = Integer.toString(num_selected)+
                                (num_selected==table.getRowCount()?" (all)":"")
                                +" rows selected";
                }

                displaySelectionInfo(selection_info_text);
            }
            else
                update_selected_rows_information = true; // next time
        }
    }
    
    /**
     * Gives the name of the underlying data file (will be shown in the tree)
     * 
     * @return name of the underlying file or String for "noname" if associated file is null
     */
    @Override
    public String toString()
    {
        if (data.getFile()==null)
        {
            return "[noname]";
        } else
            return Mien.chopFileExtension(data.getFile().getName());
    }
    
    private class FamilyScroll extends FrozenColumnsTable<OccurrenceTableModel>
    {
        private FamilyScroll(OccurrenceTableModel M)
        {
            super(M,2);
            FamilyScroll.this.initComponents();
        }

        private void initComponents()
        {
//            MouseListener listener = new MouseAdapter()
//            {
//                @Override
//                public void mouseClicked(MouseEvent event)
//                {
//                    if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount()==2)
//                    {
//                        Point click_xy = event.getPoint();
//                        int displayed_column = data_table.columnAtPoint(click_xy);
//                        int col = data_table.convertColumnIndexToModel(displayed_column);
//
//
//                        System.out.println("#* "+getClass().getName()+".mouseClicked "+displayed_column+"/"+col+"\t// "+event);
//                    }
//                }
//            };
//            this.getDataTable().getTableHeader().addMouseListener(listener);
        }

        @Override
        protected int getPreferredColumnWidth(int idx)
        {
            if (idx == 1 ) // family name
                return 120;
            else
                return super.getPreferredColumnWidth(idx);
        }

        @Override
        public String getRowName(int family_idx)
        {
            return data.getContent().getFamilyName(family_idx);
        }

        @Override
        protected String getHeaderToolTip(int column_idx)
        {
            String tt = model.getColumnDescription(column_idx);

            return tt +
                    "; click to sort rows, drag to rearrange columns, or double-click to split the table";
        }

        @Override
        protected String getCellToolTip(int row, int col)
        {
            return model.getCellToolTip(row, col);
        }

        
        @Override 
        protected void selectByReference(int family_idx, int column_idx)        
        {
            Class column_class = model.getColumnClass(column_idx);
            if (column_class == PhyleticProfile.Entier.class)
            {
                Object cell_value = model.getValueAt(family_idx, column_idx);
                PhyleticProfile.Entier iValue = (PhyleticProfile.Entier) cell_value;
                if (iValue.isAmbiguous())
                    return;
            }
            super.selectByReference(family_idx, column_idx);
        }
        
        @Override
        public int selectSimilarFamilies(int family_idx, int col, String command)
        {
            if (command == null)
                return 0;
            else
            {
                int num_selected = super.selectSimilarFamilies(family_idx, col, command);
                String info = "";
                if (num_selected == 1)
                {
                    info = "One row";
                } else
                {
                    info = Integer.toString(num_selected)+" rows";
                }
                info += " selected with "+model.getColumnName(col);
                if (command.equals("eq"))
                    info += "=";
                else if (command.equals("le"))
                    info += "\u2264";
                else if (command.equals("ge"))
                    info += "\u2265";
                info += model.getValueAt(family_idx, col);
                displaySelectionInfo(info);
                update_selected_rows_information = false;

                return num_selected;
            }
        }
    }


    
}
