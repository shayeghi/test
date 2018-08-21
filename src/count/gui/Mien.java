/*
 * Copyright 2014 Miklos Csuros (csurosm@gmail.com).
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
import count.model.IndexedTree;
import java.awt.Color;
import java.awt.Insets;
import java.io.File;
import java.util.Random;

/**
 *
 * Constants and convenience methods for eye candy. 
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 */
public class Mien 
{
    /**
     * Color for {@link count.gui.TreePanel}.
     */
    public static Color TREE_UNSELECTED_LEAF_COLOR = Color.getHSBColor((float)(92.0/360.0), 0.82f, 0.64f); // Fern
    /**
     * Color for {@link count.gui.TreePanel}.
     */
    public static Color TREE_UNSELECTED_NODE_COLOR = Color.BLACK; //Color.getHSBColor((float)(138.0/360.0), 0.79f, 0.48f); // Pine
    /**
     * Color for {@link count.gui.TreePanel}.
     */
    public static Color TREE_SELECTED_LEAF_COLOR = Color.getHSBColor((float)(92.0/360.0), 0.82f, 0.64f);  // Fern
    /**
     * Color for {@link count.gui.TreePanel}.
     */
    public static Color TREE_SELECTED_NODE_COLOR = Color.getHSBColor((float)(138.0/360.0), 0.79f, 0.48f); // Pine
    /**
     * Color for {@link count.gui.TreePanel}.
     */
    public static Color TREE_EDGE_COLOR = Color.getHSBColor((float)(28.0/360.0), 0.62f, 0.36f); // Raw Sienna
    /**
     * Size of the nodes in {@link count.gui.TreePanel}.
     */
    public static int TREE_POINT_SIZE = 8;
    
    public static Color AREA_SELECTION_COLOR = new Color(210, 240, 255, 128);
    
    public static double TREE_MAGNIFICATION_MIN = 0.1;
    public static double TREE_MAGNIFICATION_MAX = 10.;

    /**
     * Thick edge width for {@link count.gui.TreePanel}
     */
    public static int TREE_THICK_EDGE = 3;

    public static final Color TREE_NODE_LABEL_BACKGROUND = new Color(180,180,180,180);
    
    
    public static final Color WORKTAB_TREES_COLOR = new Color(24,117,52);
    public static final Color WORKTAB_DATA_COLOR = new Color(108,24,76);
    public static final Color WORKTAB_RATES_COLOR = new Color(250,78,25);

    /**
     * Default white space amount around the tree. 
     */
    public static int getTreeDefaultPadding(int tree_point_size){return (5*tree_point_size)/8;}
    
    public static Insets getTreeDefaultInsets(int tree_point_size)
    { 
        int pad = getTreeDefaultPadding(tree_point_size);
        return new Insets(pad, pad, pad, pad);
    }
    
    public static int getTreeBBoxSperation(int tree_point_size){ return (1*tree_point_size)/2;}
    
    public static int getTreeNormalFontSize(int tree_point_size){ return (3*tree_point_size)/2;}
    
    public static int getTreeDefaultStem(int tree_point_size){ return 0;}
    
    public static int FONT_SIZE_MIN = 7; 
    public static int FONT_SIZE_MAX = 32;
    
    public static int TABLE_FONT_SIZE = 14;
//    public static int TREE_PANEL_FONT_SIZE = 12;

    public static final Color SMOKY_BACKGROUND = new Color(120,120,200,50);

    public static final Color WARNING_COLOR = new Color(255, 255, 192); // Banana
    
    
    public static int GAMMA_PLOT_WIDTH = 320;
    public static int GAMMA_PLOT_HEIGHT = 60;
    public static int GAMMA_PLOT_PARTITIONS = 8;
    
    public static final int DISTRIBUTION_PLOT_WIDTH = 40;
    public static final int DISTRIBUTION_PLOT_HEIGHT = 100;
    public static final int DISTRIBUTION_PLOT_RANGE = 6;
    public static final Color DISTRIBUTION_PLOT_BACKGROUND = new Color(204, 255, 102); // Honeydew // new Color(255,204,102); // Canteloupe

    public static final int RATE_TREE_ROOT_DISTRIBUTION_WIDTH = 120;
    public static final int  RATE_TREE_ROOT_DISTRIBUTION_HEIGHT = 40;
    
    public static final int TREE_LEGEND_INSET = 4;
    public static final int TREE_LEGEND_BAR_HEIGHT = 80;
    
    public static final double RATE_TREE_SHORT_EDGE = 0.05;

    public static final Color RATES_LOSS_COLOR = new Color(255,128,0); // Tangerine
    public static final Color RATES_GAIN_COLOR = new Color(64,128,0); // Fern
    public static final Color RATES_DUPLICATION_COLOR = new Color(104,118,231); // Evening Blue
    public static final Color RATES_EDGELENGTH_COLOR =  Color.DARK_GRAY;
;
    
    public static final Color RATE_TREE_EDGE_COLOR =  new Color(180,180,180);
    
    public static final int RATE_VARIATION_PLOT_WIDTH = 320;
    public static final int RATE_VARIATION_PLOT_HEIGHT = 60;
    public static final int RATE_VARIATION_PADDING = 5;

    
    public static final Color TREE_PANEL_SELECTED_NODE_INFO_BACKGROUND = new Color(255,220,220,240);
    
    
    public static final Color MULTI_PRESENCE_COLOR = new Color(128,0,255);//Color.RED;
    public static final Color ABSENCE_COLOR  = Color.WHITE;
    public static final Color SINGLE_PRESENCE_COLOR = ColoredValueRenderer.intermediateColor(ABSENCE_COLOR, MULTI_PRESENCE_COLOR, 0.5); 
    
    
    /**
     * Constructs a short name for the node.
     * 
     * @param tree the tree topology with a fixed node traversal order
     * @param idx index of tree node (into the traversal array)
     * @return leaf name if leaf, or a positive integer for non-leaf nodes
     */
    public static String getShortNodeName(IndexedTree tree, int idx)
    {
        if (tree.isLeaf(idx))
            return tree.getName(idx);
        else
            return Integer.toString(idx-tree.getNumLeaves()+1);
    }
    
    /**
     * Constructs a long name for the node.
     * 
     * @param tree the tree topology with a fixed node traversal order
     * @param idx index of tree node (into the traversal array)
     * @return leaf name if leaf, or String of style "int [name]" for non-leaf nodes
     */
    public static String getLongNodeName(IndexedTree tree, int idx)
    {
        String short_name = getShortNodeName(tree, idx);
        if (tree.isLeaf(idx))
            return short_name;
        else
            return short_name+" ["+tree.getName(idx)+"]";
    }    
    
    public static String chopFileExtension(String s)
    {
        if (s==null) return null;
        int last_dir = s.lastIndexOf(File.separatorChar);
        int first_dot = s.indexOf('.', last_dir+1); // +1 ok if not found
        return s.substring(last_dir+1, first_dot==-1?s.length():first_dot);
    }

    /**
     * Rounds to the closest unit at the given precision.
     * 
     * @param x the value to be rounded
     * @param exact_values if not null, then (msd, exponent) pair is put there (must be of length &ge;2)
     * @return 
     */    
    public static double roundToMostSignificantDigit(double x, int[] exact_values)
    {
        return roundToMostSignificantDigit(x, exact_values, false);
    }
    /**
     * Rounds a double to one significant digit (called msd). 
     * 
     * @param x the value to be rounded
     * @param exact_values if not null, then (msd, exponent) pair is put there (must be of length &ge;2)
     * @param round_to_zero whether we round towards 0 (conversion to int), or to the closest unit
     * @return the rounded value
     */
    public static double roundToMostSignificantDigit(double x, int[] exact_values, boolean round_to_zero)
    {
        
        if (x==0.0)
        {
            if (exact_values!=null)
            {
                exact_values[0]=0;
                exact_values[1]=0;
            }
            return x;
        } 
        int sign = 1;
        if (x<0.0)
        {
            sign = -1;
            x=-x;
        }
        
        double z = Math.log10(x/(round_to_zero?1.0:0.95));
        int exponent = (int)z;
        if (z<0.) exponent--;
        
        
        double pow10 = Math.pow(10.0, exponent);
        int msd = (int)(x/pow10+(round_to_zero?0.0:0.5));
        msd = sign * msd;
        if (exact_values != null)
        {
            exact_values[0] = msd;
            exact_values[1] = exponent;
        }
        double v = pow10 * msd;
        //System.out.println("#*RMD.rTMSD x "+x+"\t v"+v+"\t"+msd+"E"+exponent);
        
        return v;
    }
    
    public static String[] random_identifiers 
            = { "Alfa", 
                "Bravo", 
                "Charlie", 
                "Delta", 
                "Echo", 
                "Fox", 
                "Golf", 
                "Hotel", 
                "India", 
                "Juliet", 
                "Kilo", 
                "Lima", 
                "Mike", 
                "Nancy", 
                "Oscar", 
                "Papa", 
                "Quebec", 
                "Romeo", 
                "Sierra", 
                "Tango", 
                "Uncle", 
                "Victor", 
                "Whisky", 
                "Xray", 
                "Yankee", 
                "Zulu"};
//            = {"Anna",
//                "Bob",
//                "Chelsea",
//                "Doug",
//                "Ella",
//                "Frank",
//                "Gina",
//                "Hugo",
//                "Ivy",
//                "Jules",
//                "Kate",
//                "Louis",
//                "Mimi",
//                "Nimrod",
//                "Olga",
//                "Peter",
//                "Queen",
//                "Roland",
//                "Susan",
//                "Timmy",
//                "Ulrike",
//                "Vince",
//                "William",
//                "Xena",
//                "Yannick"
//            };
    public static String anyIdentifier()
    {
        Random RND = new Random();
        return random_identifiers[RND.nextInt(random_identifiers.length)];
    }
    
    
    public static String createIdentifier(int idx)
    {
        if (idx<random_identifiers.length)
            return random_identifiers[idx];
        else 
            return "Z"+Integer.toString(idx);
    }
    
    public static Color OK_COLOR = Color.getHSBColor(0.25f, 0.2f, 1f);
    public static Color CANCEL_COLOR = Color.getHSBColor(0f, 0.2f, 1f);
 
}
