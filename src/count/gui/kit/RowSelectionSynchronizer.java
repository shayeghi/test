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
package count.gui.kit;

import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 */
public class RowSelectionSynchronizer extends DefaultListSelectionModel implements ListSelectionListener
{
    public RowSelectionSynchronizer()
    {
        this.tables = new ArrayList<>();
        this.chained_event_depth = 0;
    }
    
    private final List<JTable> tables;
    
    public void linkTable(JTable tbl)
    {
        tables.add(tbl);
        tbl.getSelectionModel().addListSelectionListener(this);
    }
    
    private synchronized void forwardChangeToTables(ListSelectionEvent e)
    {
        int idx0 = this.getMinSelectionIndex();
        if (idx0 != -1)
        {
            int idx1 = this.getMaxSelectionIndex();
            int diff = (idx1>=idx0)?1:-1;

            
//            
//            
//            for (int i=idx0; i!=idx1; i+=diff)
//                if (this.isSelectedIndex(i))
//                {
//                    for (JTable tbl: tables)
//                        tbl.addRowSelectionInterval(i, i);
//                }
        }
    }
    
    private synchronized void captureTableChange(ListSelectionEvent e)
    {
    }
    
    
    @Override
    public void valueChanged(ListSelectionEvent e) 
    {
//        if (!e.getValueIsAdjusting())
//        {
//            chained_event_depth++;
//            if (chained_event_depth == 1)
//            {
//                event_origin = null;
//                forwardChangeToTables(e);
//            }
//        }
    }
    
    private int chained_event_depth;
    private JTable event_origin;
    
    
    
}
