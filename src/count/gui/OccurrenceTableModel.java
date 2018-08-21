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

import count.gui.kit.ColoredValueRenderer;
import count.model.OccurrenceTable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import count.model.PhyleticProfile;

/**
 * Model for family table content. Columns are the following
 * <ul> 
 * <li> 1 family name, 
 * <li> 2... <var>x</var> properties
 * <li> <var>x</var>+1 number of lineages in which it is present
 * <li> <var>x</var>+2 total number of members
 * <li> <var>x</var>+3 .. members in each lineage (if detailed profiles)
 * <li> <var>x</var>+3 profile summary
 * </ul>
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 */
public class OccurrenceTableModel extends AbstractTableModel
{
    /**
     * Instantiation of the model
     * 
     * @param data_table the underlying family table
     * @param want_detailed_profiles whether there should be a separate column for each organism, or just a 
     *          single column for profile summary
     */
    public OccurrenceTableModel(OccurrenceTable data_table, boolean want_detailed_profiles)
    {
        super();
        this.data_table = data_table;
        this.want_detailed_profiles = want_detailed_profiles;
        this.leaves = data_table.getTaxonNames();
        initDataStructures();
    }
    
    /**
     * The underlying data.
     */
    protected final OccurrenceTable data_table;
    
    private final boolean want_detailed_profiles;
    
    /**
     * Names of terminal taxa
     */
    private final String[] leaves;

    /**
     *  Number of lineages in which a famiy is present
     */
    private int[] present_leaves;
    
    /**
     * Number of genes in the family
     */
    private int[] member_count;
    
    private PhyleticProfile[] occurrence_data;

    private void initDataStructures()
    {
        int num_families = data_table.getFamilyCount();

        present_leaves=new int[num_families];
        member_count = new int[num_families];
        for(int family_idx=0; family_idx<num_families; family_idx++)
        {
            int[] pattern = data_table.getSizes(family_idx);
            present_leaves[family_idx]=0;
            member_count[family_idx]=0;
            for (int leaf_idx=0; leaf_idx<leaves.length; leaf_idx++)
            {
                member_count[family_idx]+=Math.max(0,pattern[leaf_idx]);
                if (pattern[leaf_idx]>0)
                {
                    present_leaves[family_idx]++;
                }
            }
        }
        occurrence_data = new PhyleticProfile[num_families]; // will be filled in during scrolling as necessary        
    }

    @Override
    public int getColumnCount()
    {
        int ncol = 1 // row number
                +data_table.getKnownPropertiesCount()
                +2 // lineage and member totals
                +(want_detailed_profiles?leaves.length:1); // profile
        //System.out.println("#*OTM.gCC "+ncol);
        return ncol;
    }
    
    @Override
    public int getRowCount()
    {
        return data_table.getFamilyCount();
    }
    
    /**
     * Cell content. The underlying data is queried to copy the values 
     * into a local data structure as it becomes necessary.
     * 
     * @param row_idx row index
     * @param column_idx column index
     * @return cell content value
     */
    @Override
    public Object getValueAt(int row_idx, int column_idx)
    {
        if (column_idx==0)
            return new Integer(1+row_idx);
        column_idx--;

        int num_props = data_table.getKnownPropertiesCount();
        if (column_idx<num_props)
        {
            return data_table.getFamilyProperty(row_idx, column_idx);
        } else
            column_idx -= num_props;

        if (column_idx==0)
            return new Integer(present_leaves[row_idx]);
        column_idx--;
        if (column_idx==0)
            return new Integer(member_count[row_idx]);
        column_idx--;

        if (occurrence_data[row_idx]==null)
            occurrence_data[row_idx] = new PhyleticProfile(data_table.getSizes(row_idx));
        
        Object retval = null;
        if (want_detailed_profiles)
        {
//            if (occurrence_data[row_idx][column_idx]>=0)
                retval = occurrence_data[row_idx].getValue(column_idx);
//            else
//                retval = "?";
        }
        else
            retval = occurrence_data[row_idx];
        //System.out.println("#*OTM.gVA row "+row_idx+" col "+column_idx+" ret "+retval+"\tclass "+retval.getClass());
        return retval;
    }    
    
    
    @Override
    public String getColumnName(int column_idx)
    {
        //System.out.println("#*FSTD.OTM.gCN col "+column_idx);
        if (column_idx==0)
            return "";
        column_idx--;

        int num_props = data_table.getKnownPropertiesCount();
        if (column_idx<num_props)
        {
            return data_table.getPropertyName(column_idx);
        } else
            column_idx -= num_props;

        if (column_idx==0)
            return "#lin";
        else
            column_idx--;
        if (column_idx==0)
            return "#mem";
        else
            column_idx--;
        
        if (want_detailed_profiles)
            return "\u03a6"+leaves[column_idx];
        else
            return "Profile";
    }

    /**
     * Specify column classes to use the correct comparator in sorting
     * 
     * @param column_idx index of the column
     * @return what class the colum belongs to
     */
    @Override
    public Class getColumnClass(int column_idx)
    {
        if (column_idx == 0) return Integer.class; else column_idx --;

        int num_props = data_table.getKnownPropertiesCount();
        if (column_idx<num_props)
            return String.class;
        else
            column_idx -= num_props;
        if (column_idx<2)
            return Integer.class;
        else
            column_idx -= 2;

        if (want_detailed_profiles)
            return PhyleticProfile.Entier.class;
        else
            return PhyleticProfile.class;
    }
    
    /**
     * Description for the column
     * 
     * @param column_idx
     * @return 
     */
    public String getColumnDescription(int column_idx)
    {
        if (column_idx==0)
            return "Family index";
        else column_idx--;
        int num_props = data_table.getKnownPropertiesCount();
        if (column_idx<num_props)
        {
            return data_table.getPropertyName(column_idx);
        } else
            column_idx -= num_props;

        if (column_idx==0)
            return "Number of terminal lineages in which the family has at least one member";
        column_idx--;
        if (column_idx==0)
            return "Total number of homologs, i.e., sum of family size across the terminal lineages";
        column_idx--;
        return "Profile: number of homologs at terminal node"+(want_detailed_profiles?" "+leaves[column_idx]:"s");
    }
    
    /**
     * Can be called for producing a proper tool tip.
     * 
     * @param row_idx row index by model
     * @param column_idx column index by model
     * @return a tool tip for this cell
     */
    public String getCellToolTip(int row_idx, int column_idx)
    {
        Object val = getValueAt(row_idx, column_idx);
        if (column_idx==0)
            return "Family index is "+val;
        else column_idx--;
        
        int num_props = data_table.getKnownPropertiesCount();
        if (column_idx<num_props)
        {
            return data_table.getPropertyName(column_idx)+": "+val;
        } else
            column_idx -= num_props;
        
        if (column_idx==0)
            return "Family "+data_table.getFamilyName(row_idx)+" has at least one member in "+val+" terminal lineages.";
        else
            column_idx--;
        if (column_idx==0)
            return "Family "+data_table.getFamilyName(row_idx)+" has a total of "+val+" members across the terminal lineages";
        else
            column_idx--;
        
        if (want_detailed_profiles)
        {
            PhyleticProfile.Entier ival = (PhyleticProfile.Entier)val;
            
            if (ival.isAmbiguous())
            {
                return "Nobody knows how many members the family "+data_table.getFamilyName(row_idx)+" has at terminal node "+leaves[column_idx];
            } else
            {
                int c = ival.intValue();

                return "Family "+data_table.getFamilyName(row_idx)+" has "+(c==0?"no":Integer.toString(c))
                        +" member"+(c==1?"":"s")+" at terminal node "+leaves[column_idx];
            }
        } else
        {
            StringBuilder sb = new StringBuilder("Family ");
            sb.append(data_table.getFamilyName(row_idx));
            PhyleticProfile P = (PhyleticProfile)val;
            sb.append(":");
            sb.append(P.getPatternString());

            return sb.toString();
        }
    }
    
    /**
     * Sets the renderer for profile summaries. The 
     * argument is supposed to use this table model.
     * 
     * @param tbl a JTable object in which the renderers need to be set
     */
    public void setDefaultRenderers(JTable tbl)
    {
        if (tbl.getModel() != this)
            throw new IllegalArgumentException("OccurrenceTable.setDefaultRenderer should be used with a table that has the same model.");
        tbl.setDefaultRenderer(PhyleticProfile.class, new ProfileRenderer());
    }
    
    public int getPreferredRendererWidth()
    {
        return leaves.length*4;
    }
    
    
    
    /**
     * A class for rendering phylogenetic profiles.
     */
    public class ProfileRenderer extends DefaultTableCellRenderer
    {
        public ProfileRenderer()
        {
            super();
            //this.setBackground(Color.BLACK);
            this.setOpaque(true);
        }
        private PhyleticProfile rendering_profile;
        
        /**
         * Returns the default table cell renderer.
         * 
         * @param table the JTable
         * @param profile_array a phyletic profile
         * @param isSelected true if cell is selected
         * @param hasFocus true if cell has focus
         * @param row the row of the cell to render
         * @param column the column of the cell to render
         * @return the default table cell renderer
         */
        @Override
        public Component getTableCellRendererComponent(
                            JTable table, Object profile_array,
                            boolean isSelected, boolean hasFocus,
                            int row, int column) 
        {
            //System.out.println("#*OTM.PR.gTCRC "+profile_array+"\ttype "+profile_array.getClass());
            rendering_profile = (PhyleticProfile) profile_array;
            Component C = super.getTableCellRendererComponent(table, profile_array, isSelected, hasFocus, row, column);
            setText("");
            return C;
        }
        
        @Override
        public void paintComponent(Graphics g)
        {
            int[] pattern = rendering_profile.getProfile();
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            
            Color Cs = Color.BLACK;//LookAndFeel.SINGLE_PRESENCE_COLOR;
            Color Cm = Color.BLACK;//
            Color Cmiss = Color.DARK_GRAY;
                    
            if (rendering_profile != null)
            {
                int w = super.getWidth();
                int h = super.getHeight();
                //g2.setColor(LookAndFeel.MULTI_PRESENCE_COLOR);
                //g2.drawRect(0,0,w,h);
                int max_membership = 32;
                double scale_y = h/Math.log(2.*max_membership);
                int n = pattern.length;
                double n_1 = 1.0/n;
                for (int i = 0; i<n; i++)
                {
                    double pos_left  = w*i*n_1;
                    double pos_right = pos_left+w*n_1;
                    int x_left = (int)pos_left;
                    if (x_left != pos_left) x_left++; // ceiling
                    int x_right = (int)pos_right;
                    if (x_right != pos_right)
                        x_right--;
                    
                    //System.out.println("#*OTM.PR.pC ["+i+"] "+pattern[i]+"\txl "+x_left+" xr "+x_right+"\t// "+rendering_profile.getPatternString());
                    double l0 = (pattern[i]==0?0.0:Math.log(2.*Math.min(Math.abs(pattern[i]),max_membership)));
                    if (pattern[i]!=0)
                    {
                        if (x_left<=x_right) // put a little block with height proportional to logarithm
                        {
                            if (pattern[i]==1)
                                g2.setColor(Cs);//LookAndFeel.SINGLE_PRESENCE_COLOR);
                            else if (pattern[i]<0)
                                g2.setColor(Cmiss);
                            else
                                g2.setColor(Cm);//LookAndFeel.MULTI_PRESENCE_COLOR);
                            double pos_top = scale_y*l0;
                            int y_top = (int)pos_top;
                            g2.fillRect(x_left, h-y_top, x_right-x_left+1, y_top);
                        } else
                        {
                            // nothing plotted: too thin
                        }
                    }
                    // and plot the remainder: just a vertical line for smooth transitions at the borders
                    if (pos_right>(int)pos_right) // fill in the gap between two little blocks
                    {
                        int x_border = (int)pos_right;
                        double wt0 = pos_right-(int)pos_right;
                        double wt1 = 1.-wt0;
                        int y_top = (int)(scale_y*l0);
                        Color C0 = this.getBackground();
                        if (pattern[i]==1)
                            C0 = Cs;//LookAndFeel.SINGLE_PRESENCE_COLOR;
                        else if (pattern[i]>1)
                            C0 = Cm;//LookAndFeel.MULTI_PRESENCE_COLOR;
                        else if (pattern[i]<0)
                            C0 = Cmiss;
                        Color C1 = this.getBackground();
                        if (i!=n-1)
                        {
                            // weighing with the next profile entry
                            double l1 = (pattern[i+1]==0?0.0:Math.log(2.*Math.min(Math.abs(pattern[i+1]),max_membership)));
                            y_top = (int)(scale_y*Math.max(l0,l1));// (int)(scale_y*(wt0*l0+wt1*l1));
                            if (pattern[i+1]==1)
                                C1 = Cs;//LookAndFeel.SINGLE_PRESENCE_COLOR;
                            else if (pattern[i+1]>1)
                                C1 = Cm;//LookAndFeel.MULTI_PRESENCE_COLOR;
                            else
                                C1 = Cmiss;
                        }
                        g2.setColor(ColoredValueRenderer.intermediateColor(C0, C1, wt0));
                        g2.drawLine(x_border, h-y_top, x_border, h);
                    }
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.drawLine((int)pos_left, h-1, (int)(pos_left), h-2); // a little tick
                }
                
            }
            
        }
        
        @Override
        public Dimension getMinimumSize()
        {
            return new Dimension(getPreferredRendererWidth(),20);
        }
        
    }
}
