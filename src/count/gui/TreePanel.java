/*
 * Copyright 2014 Miklos Csuros (csuros@iro.umontreal.ca).
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

import java.awt.Color;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Set;


import count.gui.kit.BoxIcon;
import count.gui.kit.DiamondIcon;
import count.gui.kit.DrawString;
import count.gui.kit.IndexedPoint;
import count.gui.kit.PointIcon;
import count.gui.kit.PointSetPanel;
import count.gui.kit.SpinnerMagnifierModel;
import count.model.IndexedTree;
import count.model.IndexedTreeTraversal;
import count.io.DataFile;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Path2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 * JPanel capable of displaying a phylogenetic tree.
 * Each tree node has an assigned IndexedPoint, which is used
 * to map points selected on the screen to the nodes. The indexing of the tree
 * nodes here, used for the display/selection purposes, is independent
 * from the TreeNode objects own getId().
 *
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 * @since November 14, 2007, 12:17 AM
 */
public class TreePanel extends PointSetPanel 
{
    /**
     * Slant for the cladogram drawing.
     */
    private static final double CLADOGRAM_SLANT = 0.75;
    /**
     * Layout styles. 
     * May be (slanted) cladogram, phenogram (=rectangular cladogram) or (rectangular) phylogram. 
     * In a cladogram and a phenogram, all leaves are at the same level. In a phylogam, displayed edge lengths 
     * are informative. 
     */
    public enum LayoutStyle {CLADOGRAM, PHENOGRAM, PHYLOGRAM, PITCHFORK};

    /**
     * Layout style used with this tree.
     */
    private LayoutStyle layout_style;
    
    /**
     * The tree that is displayed here. 
     */
    private final IndexedTree tree; 
    private final DataFile<IndexedTree> tree_data;
    
    private final IndexedPoint[] displayed_node_locations;
    private final IndexedPoint[] node_locations;
//    private final boolean[] selected;
    private final int[] subtree_sizes;
    private Dimension user_preferred;
    
    /**
     * Dimensions of the tree space.
     */    
    private final double[] tree_size=new double[2];
        
    /**
     * Indexing constant for {@link #tree_size}.
     */
    private static final int IDX_DEPTH = 0;
    /**
     * Indexing constant for {@link #tree_size}.
     */
    private static final int IDX_WIDTH = 1;
    
    
    private final NodeLabel[] displayed_node_labels;
    
    /**
     * Quick lookup of styles for selected/unselected nodes. 
     * This is a two-element array of selected/unselected styles.
     */
    private final PointIcon[][] displayed_node_icons;
    
    /**
     * Constant for accessing {@link #displayed_node_icons}.
     */
    private static final int IDX_UNSELECTED= 0;
    /**
     * Constant for accessing {@link #displayed_node_icons}.
     */
    private static final int IDX_SELECTED  = 1;
    
    private final int tree_point_size = Mien.TREE_POINT_SIZE;
    private int tree_thick_edge = Mien.TREE_THICK_EDGE;
    
    /**
     * Padding around the tree. 
     */
    private Insets padding;
    
    private int root_stem_length;
    
    private int bounding_box_separation;
    
    
    /**
     * Bounding boxes for node-specific information. 
     */
    private Rectangle2D[] node_label_bounding_box;
    
    /**
     * Bounding boxes for edge-specific information. 
     */
    private Rectangle2D[] edge_label_bounding_box;
    
    private boolean valid_bounding_boxes;

    /**
     * Font size for displaying node names.
     */
    private int label_font_size;
    
    /**
     * Magnification on the display. 
     */
    private double magnification = 1.0;
    
    /**
     * Property change fired when magnification changes. 
     */
    public static final String MAGNIFICATION_PROPERTY = "magnification";
    

    /** 
     * Font size for 100% magnification.  
     */
    private int normal_label_font_size;
    
//    /**
//     * Management of selection listeners: single selection only.
//     */
//    private final ListSelectionModel node_selection;
    

//    public TreePanel(DataFile<IndexedTree> tree_data)
//    {
//        this.tree = tree_data.getContent();
//    }
    public TreePanel(DataFile<IndexedTree> tree_data)
    {
        this(tree_data, (tree_data.getContent().hasLength()?LayoutStyle.PHYLOGRAM:LayoutStyle.PITCHFORK), ListSelectionModel.SINGLE_SELECTION);
    }
    
    
    /**
     * Instantiation.
     * 
     * 
     * @param tree_data Tree to be displayed. Edge lengths will be accessed through the {@link IndexedTree#getLength(int) } interface. 
     * @param layout_style
     * @param selection_mode one of the mode constants from {@link javax.swing.ListSelectionModel}
     */
    public TreePanel(DataFile<IndexedTree> tree_data, LayoutStyle layout_style, int selection_mode)
    {
        super(selection_mode, true, true); // no area selection
        this.tree_data = tree_data;
        this.tree = tree_data.getContent();
        
//        this.node_selection = new javax.swing.DefaultListSelectionModel();
//        node_selection.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        this.layout_style = layout_style;
        int num_nodes = tree.getNumNodes();
        node_locations = new IndexedPoint[num_nodes];
        subtree_sizes = IndexedTreeTraversal.getSubtreeSizes(tree);
        initializeNodeLocations();

        displayed_node_locations = new IndexedPoint[num_nodes];
        Set<IndexedPoint> point_set = this.getPointSet();
        for (int i=0; i<num_nodes; i++)
        {
            displayed_node_locations[i] = new IndexedPoint(i);
            point_set.add( displayed_node_locations[i]);
        }

//        selected = new boolean[num_nodes];
        displayed_node_icons=new PointIcon[num_nodes][2];
        displayed_node_labels = new NodeLabel[num_nodes];
        
        setNormalFontSize(Mien.getTreeNormalFontSize(tree_point_size));
        
        int pad = 2*Mien.getTreeDefaultPadding(tree_point_size);
        
        setPadding(new Insets(pad, pad, pad, pad+10*label_font_size));
        setRootStem(Mien.getTreeDefaultStem(tree_point_size));
        setBoundingBoxSeparation(Mien.getTreeBBoxSperation(tree_point_size));
        
        initializeGraphicalElements();
        initializeBoundingBoxes();
    }

//    /**
//     * Sets the associated file 
//     * @param F file that will be associated with this tree 
//     */
//    public final void setAssociatedFile(File F)
//    {
//        this.phylo_path = F;
//    }
//    
//    /**
//     * Returns the associated file
//     * @return whatever file was set most recently by {@link #setAssociatedFile(java.io.File) }
//     */
//    public final File getAssociatedFile()
//    {
//        return phylo_path;
//    }
    
    
    public final DataFile<IndexedTree> getData(){ return tree_data;}
    
    public final LayoutStyle getTreeLayoutStyle(){ return layout_style;}
    
    protected final IndexedPoint getDisplayedNodeLocation(int nidx)
    {
        return displayed_node_locations[nidx];
    }
    
    protected final NodeLabel getDisplayedNodeLabel(int nidx)
    {
        return displayed_node_labels[nidx];
    }
    
    /**
     * Padding around the tree in the panel. 
     * 
     * @param I the new padding values
     */
    protected final void setPadding(Insets I)
    {
        this.padding = I;
    }
    
    /**
     * Sets the length of the little stem leading to the root.
     * 
     * @param len stem length in pixels
     */
    protected final void setRootStem(int len)
    {
        root_stem_length = len;
    }
    
    protected final int getTreePointSize()
    {
        return tree_point_size;
    }
    
    /**
     * Each edge has a bounding box (BB) for its label. The BB is assumed 
     * to cover at least the edge thickness. The BB is positioned relative to the 
     * node. 
     * 
     * @param node_idx the index for the node to which the edge goes (even root is ok)
     * @param R bounding rectangle
     */
    protected final void setEdgeLabelBoundingBox(int node_idx, Rectangle2D R)
    {
        edge_label_bounding_box[node_idx] = R;
    }
    
    /**
     * Each node has a bounding box (BB) for its label. The BB is assumed 
     * to cover at least the symbol used to plot the node. The BB is positioned relative to the 
     * node.
     * 
     * @param node_idx index for the node
     * @param R the bounding box for the node 
     */    
    protected final void setNodeLabelBoundingBox(int node_idx, Rectangle2D R)
    {
        this.node_label_bounding_box[node_idx]=R;
    }
    
    /**
     * Sets the node's display style. 
     * 
     * @param node_idx node index
     * @param icon_selected selected icon (if null, it is ignored)
     * @param icon_unselected uncelected icon (ignored if null)
     */
    protected final void setNodeDisplayIcon(int node_idx, PointIcon icon_selected, PointIcon icon_unselected)
    {
        if (icon_selected!=null) displayed_node_icons[node_idx][IDX_SELECTED] = icon_selected;
        if (icon_unselected != null) displayed_node_icons[node_idx][IDX_UNSELECTED]=icon_unselected;
    }

    /**
     * Separation between bounding boxes for nodes. 
     * @param d ne value
     */
    protected final void setBoundingBoxSeparation(int d)
    {
        this.bounding_box_separation = d;
    }
    
    protected final void setNormalFontSize(int d)
    {
        this.normal_label_font_size = d;
        setMagnification(magnification);
    }
    

    /**
     * Sets the new magnification value. 
     * Setting this value updates the font size {@link #label_font_size} and calls
     * {@link #repaint() }. Also fires a property change (named {@link #MAGNIFICATION_PROPERTY}). 
     * 
     * @param mag desired scale (positive, normal size for 1.0)
     */
    public void setMagnification(double mag)
    {
        double old_mag = this.magnification;
        this.magnification = mag;
        label_font_size = (int)(normal_label_font_size * Math.sqrt(magnification)+0.5);
        //label_font_size = Math.max(label_font_size, Mien.FONT_SIZE_MIN);
        label_font_size = Math.min(label_font_size, Mien.FONT_SIZE_MAX);
        this.firePropertyChange(MAGNIFICATION_PROPERTY, old_mag, mag);
        setValidBoundingBoxes(false);
        repaint();
    }
    
    protected final double getMagnification()
    {
        return this.magnification;
    }
    
    protected final void setValidBoundingBoxes(boolean b)
    {
        this.valid_bounding_boxes = b;
    }
    
    /**
     * Font size used at this magnificatione level. 
     * @return 
     */
    protected final int getLabelFontSize()
    {
        return this.label_font_size;
    }
    
    /**
     * Font size used with 100% magnification.
     * 
     * @return 
     */
    protected final int getNormalFontSize()
    {
        return this.normal_label_font_size;
    }
    
    /**
     * Painting the tree. Calls 
     * {@link #calculateDisplayedNodeLocations()}, 
     * {@link #plotEdges(Graphics)}, 
     * {@link #plotNodes(Graphics)} and 
     * {@link #plotNodeNames(Graphics)}, in this order.
     * 
     * @param g graphics context
     */
    @Override
    protected void paintComponent(Graphics g) 
    {
        super.paintComponent(g);
        
//        System.out.println("#*TP.pC "+g);
        
        if (!valid_bounding_boxes)
        {
            this.computeNodeLabelBoundingBoxes(g);
            this.computeEdgeLabelBoundingBoxes(g);
            setValidBoundingBoxes(true);
            revalidate(); // and repaint after
        } 
        calculateDisplayedNodeLocations();
        plotEdges(g);
        
        plotNodes(g);
        plotNodeNames(g);
        
        
    }    
    
    /**
     * Sets a user-preferred size, which overrules the locally computed values.
     * Reset to default by calling with null argument.
     * @param size preferred size
     */
    @Override
    public void setPreferredSize(Dimension size)
    {
        this.user_preferred = size;
        //System.out.println("#**ISD.sPS "+size);
    }    
    
    /**
     * Computes the preferred size of this panel, unless 
     * it was set previously via setPreferredSize()
     * 
     * @return the preferred size
     */
    @Override
    public Dimension getPreferredSize()
    {
//        System.out.println("#*TP.gPS ");
        
        if (user_preferred==null)
        {
            // figure out minimum stretch for X coordinates
            double min_x_stretch = 0.0;
            int num_leaves = tree.getNumLeaves();
            for (int leaf_idx=1; leaf_idx<num_leaves; leaf_idx++)
            {
                final Rectangle2D leaf_BB = node_label_bounding_box[leaf_idx-1];
                double previous_leaf_ends = leaf_BB.getX()+leaf_BB.getWidth();
                final Rectangle2D edge_BB = edge_label_bounding_box[leaf_idx-1];
                double previous_edge_ends = edge_BB.getX()+edge_BB.getWidth();
                double previous_ends = Math.max(previous_leaf_ends, previous_edge_ends);

                double current_leaf_starts = node_label_bounding_box[leaf_idx].getX();
                double current_edge_starts = edge_label_bounding_box[leaf_idx].getX();
                double current_starts = Math.min(current_leaf_starts, current_edge_starts);

                double current_min_stretch = previous_ends - current_starts;
                if (current_min_stretch>min_x_stretch)
                    min_x_stretch = current_min_stretch;
            }
            min_x_stretch += bounding_box_separation;
            
            //min_x_stretch *= magnification;
            
            final int last_leaf_idx = num_leaves -1;
            double minimum_width 
                    = padding.left 
                    - node_label_bounding_box[0].getX()
                    + min_x_stretch * (num_leaves-1.0)
                    + node_label_bounding_box[last_leaf_idx].getX()
                    + node_label_bounding_box[last_leaf_idx].getWidth()
                    + padding.right;
            
            int num_nodes = tree.getNumNodes();
            double min_y_stretch = 0.0;
            for (int node_idx=0; node_idx<num_nodes-1; // skip root at index num_nodes-1
                    node_idx++) 
            {
                int parent_idx = tree.getParentIndex(node_idx);
                Rectangle2D cR = node_label_bounding_box[node_idx];
                Rectangle2D eR = edge_label_bounding_box[node_idx];
                Rectangle2D pR = node_label_bounding_box[parent_idx];
                        
                double eh = eR.getHeight();
                double current_stretch = (cR.getY()+cR.getHeight()+eh-pR.getY());

                int x = (eh==0.?0:1); // counting how many non-empty bounding boxes need to be separated here
                if (eh!=0. && pR.getHeight()!=0.)
                {
                    x++;
                }
                current_stretch += x * bounding_box_separation;

                double diff_y = node_locations[node_idx].getY()-node_locations[parent_idx].getY();
                current_stretch /= diff_y;
                        
                if (current_stretch > min_y_stretch)
                    min_y_stretch = current_stretch;
            }
            
            min_y_stretch *= getMagnification();
            
            double farthest_label = 0.0;
            {
                for (int node_idx=0; node_idx<num_nodes; node_idx++)
                {
                    Rectangle2D cR = node_label_bounding_box[node_idx];
                        
                    double label_y = node_locations[node_idx].getY()*min_y_stretch-cR.getY();//+cR.getWidth();
                    if (label_y > farthest_label)
                        farthest_label = label_y;
                }
            }
            double minimum_height = padding.top + root_stem_length + padding.bottom 
                    + farthest_label;

            double reasonable_height = 300*magnification;
            //int w = (int)(Math.max(minimum_height,reasonable_height)/minimum_height*minimum_width);
            int h = (int) Math.max(minimum_height, reasonable_height);
            
            int w = (int)Math.max(minimum_width,12*num_leaves*magnification);
            //int h = (int)minimum_height;
            
//            System.out.println("#*TP.gPS "+w+"*"+h+"\txs "+min_x_stretch+"\tys "+min_y_stretch+"\tsep "+bounding_box_separation+"\tfarthest "+farthest_label);
            
            int tiny = 2*(padding.top + root_stem_length + padding.bottom);
            return new Dimension(Math.max(w,tiny),Math.max(h,tiny)); // refuse to draw something too small, no matter the magnification
        } else
            return user_preferred;
    }
    
    /**
     * Transformation from {@link #node_locations} to {@link #displayed_node_locations}.
     * 
     * @return affine transformation to find display coordinates
     */
    protected AffineTransform getDisplayTransform()
    {
//        int w=getWidth();
//        int h=getHeight();
//        double x_stretch = (w-2.*tree_padding-label_font_size*20)/tree_size[IDX_WIDTH];
//        double y_stretch = (h-2*tree_padding-4*label_font_size)/tree_size[IDX_DEPTH];
//        AffineTransform plot_transform=new AffineTransform();        
//        plot_transform.setToIdentity();
//
//        plot_transform.translate(tree_padding+10*label_font_size, h-tree_padding-2*label_font_size);
//        plot_transform.scale(x_stretch,-y_stretch);
//        return plot_transform;
        
        int w=getWidth();
        int h=getHeight();
        
        
        
        int num_leaves = tree.getNumLeaves();
        int last_leaf_idx = num_leaves-1;
        double x_stretch = (w-
                     padding.left 
                    + node_label_bounding_box[0].getX()
                    - node_label_bounding_box[last_leaf_idx].getX()
                    - node_label_bounding_box[last_leaf_idx].getWidth()
                    - padding.right)/(num_leaves-1.0);

        int root_idx = tree.getNumNodes()-1;
        
        double stem = Math.max(root_stem_length, node_label_bounding_box[root_idx].getY()+node_label_bounding_box[root_idx].getHeight());
            
        double y_stretch = (h-padding.top-padding.bottom
                    - stem
                    + node_label_bounding_box[0].getY())/tree_size[IDX_DEPTH];
        AffineTransform plot_transform=new AffineTransform();        
        plot_transform.setToIdentity();

        plot_transform.translate(padding.left-node_label_bounding_box[0].getX(), h-padding.bottom-stem);
        plot_transform.scale(x_stretch,-y_stretch);
        return plot_transform;
    }
    
    /**
     * 
     * @param g2
     * @param node_idx parent node 
     */
    protected void plotEdgesToChildren(Graphics2D g2, int node_idx)
    {
        if (layout_style == LayoutStyle.PITCHFORK)
        {
            IndexedPoint parent_pt = displayed_node_locations[node_idx];
            double parent_y = parent_pt.getY();
            int cidx=0; 
            int child = tree.getChildIndex(node_idx, cidx);
            double min_diff = Math.abs(parent_y-displayed_node_locations[child].getY());
            int min_child = child;
            cidx++;
            while (cidx<tree.getNumChildren(node_idx))
            {
                child = tree.getChildIndex(node_idx, cidx);
                double diff = Math.abs(parent_y-displayed_node_locations[child].getY());
                if (diff<min_diff)
                {
                    min_diff = diff;
                    min_child = child;
                }
                cidx++;
            }
            double ref_Y = displayed_node_locations[min_child].getY();

            do
            {
                --cidx;
                child = tree.getChildIndex(node_idx, cidx);
                drawCurvedLine(g2, displayed_node_locations[child], parent_pt, ref_Y);
                //System.err.println("#*TP.pE "+node_idx+"->"+child+"\t"+parent_pt+"\tchl "+displayed_node_locations[child]+"\t// "+ref_Y+"\t// "+Mien.getLongNodeName(tree, node_idx)+"; "+Mien.getLongNodeName(tree, child));
            } while (cidx>0);
        } else if (layout_style == LayoutStyle.CLADOGRAM)
        {
            for (int cidx=0; cidx<tree.getNumChildren(node_idx); cidx++)
            {
                int edge_idx = tree.getChildIndex(node_idx, cidx);
                drawThickLine(g2,displayed_node_locations[edge_idx],displayed_node_locations[node_idx]);
            }
        } else if (layout_style == LayoutStyle.PHENOGRAM || layout_style == LayoutStyle.PHYLOGRAM)
        {
            for (int cidx=0; cidx<tree.getNumChildren(node_idx); cidx++)
            {
                int edge_idx = tree.getChildIndex(node_idx, cidx);
                drawBentLine(g2,displayed_node_locations[edge_idx],displayed_node_locations[node_idx]);
            }
            
        }        
    }
    
    /**
    * Draws the edges of the tree.
    * 
    * @param g graphics context
    */
    protected void plotEdges(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Mien.TREE_EDGE_COLOR); 
        int num_edges = tree.getNumEdges();

        assert (tree.getNode(num_edges).isRoot()); // skipping that one
        
        
        if (layout_style == LayoutStyle.PITCHFORK)
        {
            for (int node_idx=tree.getNumLeaves(); node_idx<tree.getNumNodes(); node_idx++ )
            {
                assert (!tree.isLeaf(node_idx));
                IndexedPoint parent_pt = displayed_node_locations[node_idx];
                double parent_y = parent_pt.getY();
                int cidx=0; 
                int child = tree.getChildIndex(node_idx, cidx);
                double min_diff = Math.abs(parent_y-displayed_node_locations[child].getY());
                int min_child = child;
                cidx++;
                while (cidx<tree.getNumChildren(node_idx))
                {
                    child = tree.getChildIndex(node_idx, cidx);
                    double diff = Math.abs(parent_y-displayed_node_locations[child].getY());
                    if (diff<min_diff)
                    {
                        min_diff = diff;
                        min_child = child;
                    }
                    cidx++;
                }
                double ref_Y = displayed_node_locations[min_child].getY();
                
                do
                {
                    --cidx;
                    child = tree.getChildIndex(node_idx, cidx);
                    drawCurvedLine(g2, displayed_node_locations[child], parent_pt, ref_Y);
                    //System.err.println("#*TP.pE "+node_idx+"->"+child+"\t"+parent_pt+"\tchl "+displayed_node_locations[child]+"\t// "+ref_Y+"\t// "+Mien.getLongNodeName(tree, node_idx)+"; "+Mien.getLongNodeName(tree, child));
                } while (cidx>0);
            }
        } else if (layout_style == LayoutStyle.CLADOGRAM)
        {
            for (int edge_idx=0; edge_idx<num_edges; edge_idx++)
            {
                IndexedPoint parent_pt = displayed_node_locations[tree.getParentIndex(edge_idx)];
                drawThickLine(g2,displayed_node_locations[edge_idx],parent_pt);
            }
        } else if (layout_style == LayoutStyle.PHENOGRAM || layout_style == LayoutStyle.PHYLOGRAM)
        {
            for (int edge_idx=0; edge_idx<num_edges; edge_idx++)
            {
                IndexedPoint parent_pt = displayed_node_locations[tree.getParentIndex(edge_idx)];
                drawBentLine(g2,displayed_node_locations[edge_idx],parent_pt);
            }
            
        }
    }
    
    protected void plotNode(Graphics g, int node_idx)
    {
        int nx = (int)displayed_node_locations[node_idx].getX();
        int ny = (int)displayed_node_locations[node_idx].getY();
        if (this.isSelected(node_idx))
            displayed_node_icons[node_idx][IDX_SELECTED].paint(g, nx, ny);
        else if (label_font_size>=Mien.FONT_SIZE_MIN)
            displayed_node_icons[node_idx][IDX_UNSELECTED].paint(g, nx, ny);
    }

    /**
    * Draws the tree nodes. 
    * @param g graphics context
    */
    protected void plotNodes(Graphics g)
    {
        int num_nodes = tree.getNumNodes();
        for (int node_idx=0; node_idx<num_nodes; node_idx++)
        {
            plotNode(g, node_idx);
//            int nx = (int)displayed_node_locations[node_idx].getX();
//            int ny = (int)displayed_node_locations[node_idx].getY();
//            if (this.isSelected(node_idx))
//                displayed_node_icons[node_idx][IDX_SELECTED].paint(g, nx, ny);
//            else if (label_font_size>=Mien.FONT_SIZE_MIN)
//                displayed_node_icons[node_idx][IDX_UNSELECTED].paint(g, nx, ny);
        }
    }
    
//    protected void plotNodeLabel(Graphics g, int nidx)
//    {
////        FontMetrics label_fm = g.getFontMetrics();
////        Color old_color = g.getColor();
//        
//        if (tree.isLeaf(nidx))
//        {
//            String label_text = tree.getName(nidx);
//            this.drawNodeLabelCentered(g, label_text, getDisplayedNodeLocation(nidx), 0, -getTreePointSize()/2-3);
////            
////            int w = label_fm.stringWidth(label_text);
////                //int h = label_font_size+2;
////
////            int x = (int)displayed_node_locations[nidx].x-w/2;
////            int y = (int)displayed_node_locations[nidx].y-tree_point_size/2-3;
////            g.setColor(old_color);
////            g.drawString(label_text, x, y);              
////                    g.translate((int)displayed_node_locations[nidx].x,(int)displayed_node_locations[nidx].y);
////                    Rectangle2D R = node_label_bounding_box[nidx];
////                    ((Graphics2D)g).draw(R);
////                    g.translate(-(int)displayed_node_locations[nidx].x,-(int)displayed_node_locations[nidx].y);
////                    System.out.println("#*TP.pNN "+nidx+"\t"+displayed_node_locations[nidx]+"\t// "+tree.getNode(nidx)+"\t// "+node_label_bounding_box[nidx]);
//        } else
//        {
////            int h = label_font_size+2;
////            int x = (int)displayed_node_locations[nidx].x+tree_point_size/2+5;
////            int y = (int)displayed_node_locations[nidx].y+label_font_size/2-2;
//
//            int inner_node_idx = nidx-tree.getNumLeaves()+1;
//            String label_text = Integer.toString(inner_node_idx);
//            String full_name = tree.getName(nidx);
//            
//            this.drawNodeLabelTrimmed(g, label_text, full_name, getDisplayedNodeLocation(nidx), getTreePointSize()/2+5, getLabelFontSize()/2-2);
////            
////
////            if (full_name!=null && !full_name.equals(""))
////            {
////                for (int j=full_name.length(); j>=0; j--)
////                {
////                    String dn = full_name.substring(0,j);
////                    String cand_label = label_text+" ["+dn;
////                    if (j<full_name.length())
////                        cand_label = cand_label+"...";
////                    cand_label = cand_label+"]";
////                    int w = label_fm.stringWidth(cand_label)+8;
////                    Rectangle covered_by_label = new Rectangle(x-1, y-h+3,w,h);
////                    int num_nodes_covered = this.getNumCoveredPoints(covered_by_label);
////                    if (j==0 || num_nodes_covered==0)
////                    {
////                        label_text = cand_label;
////                        break;
////                    }
////                }
////                int w = label_fm.stringWidth(label_text)+2;
////                Rectangle covered_by_label = new Rectangle(x-1, y-h+3,w,h);
////                g.setColor(Mien.TREE_NODE_LABEL_BACKGROUND);//new Color(174,249,63,50)); //new Color(24,24,200,80)); //
////                g.fillRoundRect(covered_by_label.x,covered_by_label.y,covered_by_label.width, covered_by_label.height, 10, 10);
////                g.setColor(Mien.TREE_UNSELECTED_NODE_COLOR);
////                g.drawString(label_text, x, y);
////                //g.translate((int)displayed_node_location[display_idx].x,(int)displayed_node_location[display_idx].y);
////                //Rectangle2D R = node_label_bounding_box[nidx];
////                //System.out.println("#**WSC.pNN "+nidx+"\t"+R+"\t//"+N);
////                //((Graphics2D)g).draw(R);
////                //g.translate(-(int)displayed_node_location[display_idx].x,-(int)displayed_node_location[display_idx].y);
//            } // not a leaf 
////            g.setColor(old_color);
////        }
//    }
    
    /**
     * Displays all the node names. 
     * @param g graphics context
     */
    protected void plotNodeNames(Graphics g)
    {
        
        Font leaf_font = new Font("Serif", Font.PLAIN, label_font_size);
        Font inner_font = new Font("Serif", Font.ITALIC, label_font_size);
        
        Graphics2D g2 = (Graphics2D) g.create();
        
//        Color label_background = new Color(180,180,180,180);
//        Color old_color = g.getColor();

//        int inner_node_idx = 1;
        for (int nidx=0; nidx<tree.getNumNodes(); nidx++)
        {
            if (!this.isSelected(nidx))
            {
                Font label_font=(tree.isLeaf(nidx)?leaf_font:inner_font);
                g2.setFont(label_font);
                displayed_node_labels[nidx].draw(g2);
            }
        }
        if (!this.getSelectionModel().isSelectionEmpty())
        {
            float selected_size = Math.max(label_font_size, normal_label_font_size);
            leaf_font = leaf_font.deriveFont(selected_size).deriveFont(leaf_font.getStyle() + Font.BOLD);
            inner_font = inner_font.deriveFont(selected_size).deriveFont(inner_font.getStyle() + Font.BOLD);

            for (int nidx=0; nidx<tree.getNumNodes(); nidx++)
                if (this.isSelected(nidx))
                {
                    Font label_font=(tree.isLeaf(nidx)?leaf_font:inner_font);
                    g2.setFont(label_font);
                    displayed_node_labels[nidx].draw(g2);
                }
        }
//            
//            
//            
//            plotNodeLabel(g, nidx);
////            FontMetrics label_fm = g.getFontMetrics(label_font);
////            if (tree.isLeaf(nidx))
////            {
////                String label_text = tree.getName(nidx);
////                int w = label_fm.stringWidth(label_text);
////                    //int h = label_font_size+2;
////                    
////                int x = (int)displayed_node_locations[nidx].x-w/2;
////                int y = (int)displayed_node_locations[nidx].y-tree_point_size/2-3;
////                g.setFont(label_font);
////                g.setColor(old_color);
////                g.drawString(label_text, x, y);              
//////                    g.translate((int)displayed_node_locations[nidx].x,(int)displayed_node_locations[nidx].y);
//////                    Rectangle2D R = node_label_bounding_box[nidx];
//////                    ((Graphics2D)g).draw(R);
//////                    g.translate(-(int)displayed_node_locations[nidx].x,-(int)displayed_node_locations[nidx].y);
//////                    System.out.println("#*TP.pNN "+nidx+"\t"+displayed_node_locations[nidx]+"\t// "+tree.getNode(nidx)+"\t// "+node_label_bounding_box[nidx]);
////            } else
////            {
////                int h = label_font_size+2;
////                int x = (int)displayed_node_locations[nidx].x+tree_point_size/2+5;
////                int y = (int)displayed_node_locations[nidx].y+label_font_size/2-2;
////                String label_text = Integer.toString(inner_node_idx);
////
////                inner_node_idx++;
////                
////                String full_name = tree.getName(nidx);
////                    
////                if (full_name!=null && !full_name.equals(""))
////                {
////                    for (int j=full_name.length(); j>=0; j--)
////                    {
////                        String dn = full_name.substring(0,j);
////                        String cand_label = label_text+" ["+dn;
////                        if (j<full_name.length())
////                            cand_label = cand_label+"...";
////                        cand_label = cand_label+"]";
////                        int w = label_fm.stringWidth(cand_label)+8;
////                        Rectangle covered_by_label = new Rectangle(x-1, y-h+3,w,h);
////                        int num_nodes_covered = this.getNumCoveredPoints(covered_by_label);
////                        if (j==0 || num_nodes_covered==0)
////                        {
////                            label_text = cand_label;
////                            break;
////                        }
////                    }
////                    int w = label_fm.stringWidth(label_text)+2;
////                    Rectangle covered_by_label = new Rectangle(x-1, y-h+3,w,h);
////                    g.setColor(label_background);//new Color(174,249,63,50)); //new Color(24,24,200,80)); //
////                    g.fillRoundRect(covered_by_label.x,covered_by_label.y,covered_by_label.width, covered_by_label.height, 10, 10);
////                    g.setFont(label_font);
////                    g.setColor(Mien.TREE_UNSELECTED_NODE_COLOR);
////                    g.drawString(label_text, x, y);
////                    //g.translate((int)displayed_node_location[display_idx].x,(int)displayed_node_location[display_idx].y);
////                    //Rectangle2D R = node_label_bounding_box[nidx];
////                    //System.out.println("#**WSC.pNN "+nidx+"\t"+R+"\t//"+N);
////                    //((Graphics2D)g).draw(R);
////                    //g.translate(-(int)displayed_node_location[display_idx].x,-(int)displayed_node_location[display_idx].y);
////                } // not a leaf 
////                g.setColor(old_color);
////            }
    }
        
    /**
     * Line drawing for edges (straight thick lines).
     * 
     * @param g graphics context
     * @param p1 one endpoint
     * @param p2 other endpoint
     */
    private static void drawThickLine(Graphics g, Point2D p1, Point2D p2)
    {
        final int x1 = (int)p1.getX();
        final int y1 = (int)p1.getY();
        final int x2 = (int)p2.getX();
        final int y2 = (int)p2.getY();
        final int x_offset_min = -Mien.TREE_THICK_EDGE/2;
        final int x_offset_max = Mien.TREE_THICK_EDGE + x_offset_min;
        for (int dx=x_offset_min; dx<x_offset_max; dx++)
            g.drawLine(x1+dx, y1, x2+dx, y2);
//        g.drawLine((int)p1.getX(),(int)p1.getY(),(int)p2.getX(),(int)p2.getY());
//        g.drawLine((int)p1.getX()-1, (int)p1.getY(),(int)p2.getX()-1,(int)p2.getY());
//        g.drawLine((int)p1.getX()+1, (int)p1.getY(),(int)p2.getX()+1,(int)p2.getY());
    }

    private static final double CURVE_CONTROL1 = 1.0;
    private static final double CURVE_CONTROL2 = 0.0;
    
    private static void drawCurvedLine(Graphics2D g2, Point2D p_to, Point2D p_from, double mid_Y)
    {
        double y1 = p_from.getY();
        double x1 = p_from.getX();
        double y2 = p_to.getY();
        double x2 = p_to.getX();
        
        double bx1 = x1;
        double by1 = y1*(1.-CURVE_CONTROL1)+mid_Y*CURVE_CONTROL1;
        double bx2 = x2;
        double by2 = y1*(1.-CURVE_CONTROL2)+y2*CURVE_CONTROL2;
        
        Path2D link = new Path2D.Double();
        link.moveTo(x1, y1);
        link.curveTo(bx1, by1, bx2, by2, x2, y2);
        g2.draw(link);
    }
    
    /**
     * Line drawing for edges (bent lines).
     * 
     * @param g graphics context
     * @param p1 one endpoint
     * @param p2 other endpoint
     */
    private static void drawBentLine(Graphics g, Point2D p1, Point2D p2)
    {
        g.drawLine((int)p1.getX(),(int)p1.getY(),(int)p1.getX(),(int)p2.getY());
        g.drawLine((int)p1.getX(),(int)p2.getY(),(int)p2.getX(),(int)p2.getY());
    }

//    /**
//     * Called by the superclass when multiple nodes are selected by the mouse.
//     * The normal behavior is that all nodes in the subtrees are set to <em>selected</em>.
//     *
//     * @param L list of points, may be null
//     * @param extend_selection whether current selection should be kept
//     */
//    @Override
//    protected void selectPoints(Set<IndexedPoint> L, boolean extend_selection) 
//    {
//        node_selection.setValueIsAdjusting(true);
//        
//        if (!extend_selection)
//        {
//            Arrays.fill(selected, false);
//            node_selection.clearSelection();
//        }
//        
//        if (L!=null)
//        {
//            L.stream().forEach((P) -> {
//                selectPoint(P.getIndex());
//            });
//        }
//        node_selection.setValueIsAdjusting(false);
//        repaint();
//    }
    
//    /**
//     * Called by the superclass when a single tree node is selected by the mouse.
//     * The normal behavior is that all nodes in the subtree are set to "selected".
//     */
//    @Override
//    protected void selectPoint(IndexedPoint P, int num_mouse_clicks, boolean extend_selection) 
//    {
//        node_selection.setValueIsAdjusting(true);
//        if (!extend_selection)
//        {
//            Arrays.fill(selected, false);
//            node_selection.clearSelection();
//        }
//        selectPoint(P.getIndex());
//        node_selection.setValueIsAdjusting(false);
//        repaint();
//    }
//    
//    /**
//     * Sets selection bit for given point, and registers with the ListSelectionModel.
//     * 
//     * @param point_index 
//     */
//    protected final void selectPoint(int point_index)
//    {
//        System.out.println("#*TP.sP "+point_index);
//
//        selected[point_index]=true;
//        node_selection.setSelectionInterval(point_index, point_index);
//        
//        
//    }

//    /**
//     * Sets the nodes in the subtree to "selected".
//     * @param point_index index for the point at the subtree root
//     */
//    private void selectSubtree(int point_index)
//    {
//        selectPoint(point_index);
////        selected[point_index]=true;
////        node_selection.setSelectionInterval(point_index, point_index);
//        
//        for (int cidx=0; cidx<tree.getNumChildren(point_index); cidx++)
//        {
//            selectSubtree(tree.getChildIndex(point_index, cidx));
//        }
//    }
    
    
//    /**
//     * Sets all tree nodes to <em>unselected</em> and repaints the tree
//     */
//    @Override
//    protected void removeSelection() 
//    {
//        System.out.println("#*TP.rS");
//        Arrays.fill(selected, false);
//        node_selection.clearSelection();
//        repaint();
//    }
//    
    
//    public void addListSelectionListener(ListSelectionListener listener)
//    {
//        node_selection.addListSelectionListener(listener);
//    }
//    
//    public void removeListSelectionListener(ListSelectionListener listener)
//    {
//        node_selection.removeListSelectionListener(listener);
//    }
    
//    /**
//     * Links row selection in a table with node selection in the tree panel. 
//     * 
//     * @param tbl 
//     */
//    public void linkNodeAndRowSelection(JTable tbl)
//    {
//        tbl.setSelectionModel(this.getSelectionModel());
////        class LinkFromTable implements ListSelectionListener
////        {
////            private final JTable table;
////            
////            private int call_counter=0;
////            
////            LinkFromTable(JTable tbl)
////            {
////                this.table = tbl;
////            }
////
////            @Override
////            public void valueChanged(ListSelectionEvent e) 
////            {
////                int call_idx = call_counter++;
////                for (int i=0; i<call_idx; i++) System.out.print("\t");
////                System.out.println("#**TP.lNARS.LFT.vC START/"+call_idx+" "+e+"\t"+table.getSelectedRow()+"\tnsadj "+node_selection.getValueIsAdjusting());
////                
////                if (!e.getValueIsAdjusting() && !node_selection.getValueIsAdjusting())
////                {
////                    node_selection.setValueIsAdjusting(true);
////                    
////                    removeSelection();
////                    if (table.getSelectedRowCount()>0)
////                    {
////                        int node_idx =table.getSelectedRow();
////                        
////                        int h = 100;
////                        int w = h+h;
////                        
////                        double x0 = Math.max(0.0, displayed_node_locations[node_idx].getX()-0.5*w);
////                        double x1 = Math.min(getWidth(), x0+w);
////                        x0 = x1-w;
////                        double y0 = Math.max(0.0, displayed_node_locations[node_idx].getY()-0.5*h);
////                        double y1 = Math.min(getHeight(), y0+h);
////                        y0 = y1-h;
////                        Rectangle bbox = new Rectangle((int)x0, (int)y0, w, h);
////                        TreePanel.this.scrollRectToVisible(bbox);
////                        
////                        selected[node_idx]=true;
////                        node_selection.setSelectionInterval(node_idx, node_idx);
////                    }
////                    TreePanel.this.repaint();
////                    node_selection.setValueIsAdjusting(false);
////                }
////                for (int i=0; i<call_idx; i++) System.out.print("\t");
////                System.out.println("#**TP.lNARS.LFT.vC DONE/"+call_idx+" "+e+"\t"+table.getSelectedRow()+"\tnsadj "+node_selection.getValueIsAdjusting());
////            }
////        }        
////        
////        class LinkToTable implements ListSelectionListener
////        {
////            private final JTable table;
////            private int call_counter=0;
////            
////            public LinkToTable(JTable table)
////            {
////                this.table = table;
////            }
////
////            @Override
////            public void valueChanged(ListSelectionEvent e) 
////            {
////                int call_idx = call_counter++;
////                for (int i=0; i<call_idx; i++) System.out.print("\t");
////                System.out.println("#**TP.lNARS.LTT.vC START/"+call_idx+" "+e+"\t"+table.getSelectedRow()+"\tnsadj "+node_selection.getValueIsAdjusting());
////                
////                if (!node_selection.getValueIsAdjusting())
////                {
////                    ListSelectionModel selection_model = table.getSelectionModel();
////                    selection_model.setValueIsAdjusting(true);
////                    selection_model.clearSelection();
////                    Set<IndexedPoint> selected_points = TreePanel.this.getCoveredPoints();
////                    if (selected_points != null)
////                        for (IndexedPoint P:selected_points)
////                        {
////                            selection_model.addSelectionInterval(P.getIndex(), P.getIndex());
////                        }
////                    table.scrollRectToVisible(table.getCellRect(selection_model.getLeadSelectionIndex(), 0, true));
////                    selection_model.setValueIsAdjusting(false);
////                }
////                for (int i=0; i<call_idx; i++) System.out.print("\t");
////                System.out.println("#**TP.lNARS.LTT.vC DONE/"+call_idx+" "+e+"\t"+table.getSelectedRow()+"\tnsadj "+node_selection.getValueIsAdjusting());
////            }
////        }        
////        
////        this.addListSelectionListener(new LinkToTable(tbl));
////        tbl.getSelectionModel().addListSelectionListener(new LinkFromTable(tbl));
//    }
//    
    
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (!e.getValueIsAdjusting())
        {
            int idx = getSelectionModel().getLeadSelectionIndex();
            if (idx != -1)
            {
                double nx = displayed_node_locations[idx].getX();
                double ny = displayed_node_locations[idx].getY();
                int r = (int)(getNodeRadius()+0.5);
                Rectangle nRect = new Rectangle((int)(nx-r),(int)(ny-r),2*r,7*r);
                scrollRectToVisible(nRect);
            }
        }
        super.valueChanged(e);
    }
    

    public int getNodeRadius()
    {
        int font_size = getLabelFontSize();
        int r = Math.max(7*font_size, 7*font_size*font_size/getNormalFontSize())/5;
        return r;
    }
    
    
    /**
    * Calculates displayed_node_location[] using {@link #getDisplayTransform() }.
    */
    protected void calculateDisplayedNodeLocations()
    {
//        System.out.println("#*TP.cDN ");
        AffineTransform plot_transform=getDisplayTransform();
        plot_transform.transform(node_locations,0,displayed_node_locations,0,node_locations.length);
    }
    
    private void initializeGraphicalElements()
    {
        setSelectionAreaColor(Mien.AREA_SELECTION_COLOR);
        setCloseRadius(tree_point_size);
        PointIcon leaf_icon=new BoxIcon(tree_point_size,true);
        leaf_icon.setDrawColor(Mien.TREE_UNSELECTED_LEAF_COLOR);
        leaf_icon.setFillColor(getBackground());
        PointIcon selected_leaf_icon=new BoxIcon(tree_point_size,true);
        selected_leaf_icon.setDrawColor(Mien.TREE_SELECTED_LEAF_COLOR);
        selected_leaf_icon.setFillColor(Mien.TREE_SELECTED_LEAF_COLOR);
        PointIcon node_icon=new DiamondIcon(tree_point_size,true);
        node_icon.setDrawColor(Mien.TREE_UNSELECTED_NODE_COLOR);
        node_icon.setFillColor(getBackground());
        PointIcon selected_node_icon=new DiamondIcon(tree_point_size,true);
        selected_node_icon.setDrawColor(Mien.TREE_SELECTED_NODE_COLOR);
        selected_node_icon.setFillColor(Mien.TREE_SELECTED_NODE_COLOR);

        int num_nodes = tree.getNumNodes();
        for (int node_idx=0; node_idx<num_nodes; node_idx++)
        {
            if (tree.isLeaf(node_idx))
            {
                displayed_node_icons[node_idx][IDX_SELECTED]=selected_leaf_icon;
                displayed_node_icons[node_idx][IDX_UNSELECTED]=leaf_icon;
                setNodeLabel(node_idx,tree.getName(node_idx), 0, -getTreePointSize()/2-3, 0.0, 0.5f);
            } else 
            {
                displayed_node_icons[node_idx][IDX_SELECTED]=selected_node_icon;
                displayed_node_icons[node_idx][IDX_UNSELECTED]=node_icon;
                int inner_node_idx = node_idx-tree.getNumLeaves()+1;
                String label_text = Integer.toString(inner_node_idx);
                String full_name = tree.getName(node_idx);
                NodeLabel label = setNodeLabel(node_idx, label_text, full_name, getTreePointSize()/2+5, getLabelFontSize()/2-2, 0.0, 0f);
                label.setLabelBackgroundFill(Mien.TREE_NODE_LABEL_BACKGROUND);
            }
        } // for all nodes
        
        this.setBackground(Color.WHITE);
    }
    
    /**
     * Calculates the placement of nodes ({@link #node_locations}) in a generic space. 
     * Called only once. 
     */
    protected final void initializeNodeLocations()
    {
        int num_leaves = tree.getNumLeaves();
        for (int leaf_idx=0; leaf_idx<num_leaves; leaf_idx++)
        {
            assert (tree.getNode(leaf_idx).isLeaf());
            
            node_locations[leaf_idx]=new IndexedPoint(leaf_idx, leaf_idx, 0.0);
        }
        int num_nodes = tree.getNumNodes();
        for (int node_idx=num_leaves; node_idx<num_nodes; node_idx++)
        {
            assert (!tree.getNode(node_idx).isLeaf());
            switch (layout_style)
            {
                case CLADOGRAM: node_locations[node_idx]=placeNodeCladogram(node_idx); break;
                case PHENOGRAM: 
                case PITCHFORK:     
                    node_locations[node_idx]=placeNodePhenogram(node_idx); break;
                case PHYLOGRAM: node_locations[node_idx]=placeNodePhylogram(node_idx); break;
                
            }
        }
        
        // set bounding box
        tree_size[IDX_DEPTH]=tree_size[IDX_WIDTH]=0.0;
        for (int node_idx=0; node_idx<num_nodes; node_idx++)
        {
            tree_size[IDX_DEPTH] = Math.max(tree_size[IDX_DEPTH],node_locations[node_idx].getY());
            tree_size[IDX_WIDTH] = Math.max(tree_size[IDX_WIDTH],node_locations[node_idx].getX());
        }
        
        // actually, there is no need to check all nodes, we know which ones define the bounding box
        assert (num_leaves == 0 || tree_size[IDX_WIDTH]==num_leaves-1);
        assert (num_nodes==0 || tree_size[IDX_DEPTH]==node_locations[num_nodes-1].getY());
        
        // reposition the Y cordinates so that root is at Y=0 and children are at Y>0 
        switch(layout_style)
        {
            case CLADOGRAM:
            case PHENOGRAM:
            case PITCHFORK:
                for (int node_idx=0; node_idx<num_nodes; node_idx++)
                {
                    double ny = node_locations[node_idx].getY();
                    double nx = node_locations[node_idx].getX();
                    node_locations[node_idx].setLocation(nx, tree_size[IDX_DEPTH]-ny);
                }
                break;
            case PHYLOGRAM:
                int node_idx = num_nodes-1;
                node_locations[node_idx].setLocation(node_locations[node_idx].getX(), 0.0);
                while(node_idx>0)
                {
                    node_idx--;
                    int Pidx = tree.getParentIndex(node_idx);
                    double nx = node_locations[node_idx].getX();
                    double ny = node_locations[Pidx].getY()+getDisplayEdgeLength(node_idx);
                    node_locations[node_idx].setLocation(nx, ny);
                }
        }       
    }
    
    /**
     * Allocates the bounding box arrays, and initializes the bounding boxes. 
     */
    private void initializeBoundingBoxes()
    {
        System.out.println("#*TP.iBB");
        int num_nodes = tree.getNumNodes();
        node_label_bounding_box = new Rectangle2D[num_nodes];
        edge_label_bounding_box = new Rectangle2D[num_nodes];
        
        computeNodeLabelBoundingBoxes(null);
        computeEdgeLabelBoundingBoxes(null);
        
        this.valid_bounding_boxes = false;
    }
    
    /**
     * Initial implementation works for null argument
     * 
     * @param g if null, employ default bounding box
     */
    protected void computeNodeLabelBoundingBoxes(Graphics g)
    {
//        System.out.println("#*TP.cNLB "+g);
        if (g==null)
        {
            int num_nodes = tree.getNumNodes();
            for (int node_idx=0; node_idx<num_nodes; node_idx++)
            { // we do not know the font metrics
                setNodeLabelBoundingBox(node_idx,new Rectangle2D.Double(-tree_point_size/2, -tree_point_size/2, tree_point_size, tree_point_size));
            }
        } else
        {
            Graphics2D g2 = (Graphics2D)g.create();
            
            Font leaf_font = new Font("Serif", Font.PLAIN, label_font_size);
            Font inner_font = new Font("Serif", Font.ITALIC, label_font_size);
            for (int node_idx=0; node_idx<tree.getNumNodes(); node_idx++)
            {
                if (tree.isLeaf(node_idx))
                {
                    g2.setFont(leaf_font);
                    Rectangle2D R = displayed_node_labels[node_idx].getBoundingBox(g2);
//                            DrawString.getBoundingBoxForRotatedString(g2, leaf_fm, tree.getName(node_idx), 0, -tree_point_size/2-3, 0.0, 0.5f);
                    R.add(new Rectangle2D.Double(-tree_point_size,-tree_point_size,tree_point_size*2, tree_point_size*2)); // space for the little squares/diamonds
                    setNodeLabelBoundingBox(node_idx, R);
                } else
                {
                    //String label = (getMagnification()<1.0?" ":"mmmmm");
                    g2.setFont(inner_font);
                    Rectangle2D R = displayed_node_labels[node_idx].getBoundingBox(g2);
//                            DrawString.getBoundingBoxForRotatedString(g2, inner_fm, label, tree_point_size/2+5, label_font_size/2-2, 0.0, 0.f);
                    R.setRect(R.getX()-1.0, R.getY()-2.0, R.getWidth()+2, R.getHeight()+2);
                    R.add(new Rectangle2D.Double(-tree_point_size,-tree_point_size,2*tree_point_size, 2*tree_point_size)); // space for the little squares/diamonds
                    setNodeLabelBoundingBox(node_idx,R);
                }
            }            
        }
    }
    
    /**
     * Initial implementation works for null argument
     * 
     * @param g  if null, employ default values
     */
    protected void computeEdgeLabelBoundingBoxes(Graphics g)
    {
        int num_edges = tree.getNumEdges();
        for (int edge_idx=0; edge_idx<num_edges; edge_idx++)
        {
            setEdgeLabelBoundingBox(edge_idx, new Rectangle2D.Double(-(Mien.TREE_THICK_EDGE/2), 0, Mien.TREE_THICK_EDGE, 0));
        }
    }
    
    /**
     * Tree layout in cladogram style.
     * 
     * X cordinates are placed proportionally by subtree size.
     * Y coordinate of bifurcating inner nodes is set
     * so that all branches have the same slant.
     */
    private IndexedPoint placeNodeCladogram(int node_idx)
    {
        double nx = 0.0;
        double ny = 0.0;
        int num_children = tree.getNumChildren(node_idx);
        if (num_children==2)
        {
            int Cidx_left = tree.getChildIndex(node_idx, 0);
            double cx_left = node_locations[Cidx_left].getX();
            double cy_left = node_locations[Cidx_left].getY();
            int Cidx_right = tree.getChildIndex(node_idx, 1);
            double cx_right = node_locations[Cidx_right].getX();
            double cy_right = node_locations[Cidx_right].getY();
                    
            double xdiff = Math.abs(cx_left-cx_right);
            double ydiff = Math.abs(cy_left-cy_right);
                    
            double h = 0.5*(xdiff-CLADOGRAM_SLANT*ydiff)/CLADOGRAM_SLANT;
                    
            ny = Math.max(cy_left,cy_right)+h;
            nx = cx_left + (ny-cy_left)*CLADOGRAM_SLANT;
        } else
        {
            for (int child_idx=0; child_idx<num_children; child_idx++)
            {
                int Cidx = tree.getChildIndex(node_idx, child_idx);
                double cx = node_locations[Cidx].getX();
                double cy = node_locations[Cidx].getY();
                nx += subtree_sizes[Cidx]*cx;
                ny = Math.max(cy+1.0, ny);
//                ny = Math.max(cy+tree.getLength(Cidx), ny);
            }
            nx /= subtree_sizes[node_idx]-1.0;
        }
        return new IndexedPoint(node_idx,nx,ny);
    }
    
    /**
     * Edge length for tree display, used with {@link LayoutStyle#PHYLOGRAM}.
     * (Default implementation uses {@link IndexedTree#getLength(int) }.)
     * 
     * @param node_idx
     * @return 
     */
    protected double getDisplayEdgeLength(int node_idx)
    {
        return tree.getLength(node_idx);
    }
    
    private IndexedPoint placeNodePhenogram(int node_idx)
    {
        int num_children=tree.getNumChildren(node_idx);
        // X coordinate at the median
        int cm1 = (num_children-1)/2;
        int cm2 = num_children/2;
        double nx = 0.5*(node_locations[tree.getChildIndex(node_idx, cm1)].getX()
                        + node_locations[tree.getChildIndex(node_idx, cm2)].getX());
        // Y coordinate is one larger than the children's max
        double ny = 0.0;
        for (int child_idx=0; child_idx<num_children; child_idx++)
        {
            int Cidx = tree.getChildIndex(node_idx,child_idx);
            double cy = node_locations[Cidx].getY();
            ny = Math.max(cy+1.0, ny);
        }
        return new IndexedPoint(node_idx,nx,ny);
    }

    /** 
     * Placement of parent node using edge lengths ({@link #getDisplayEdgeLength(int)}) to children. 
     * 
     * @param node_idx
     * @return 
     */
    private IndexedPoint placeNodePhylogram(int node_idx)
    {
        int num_children = tree.getNumChildren(node_idx);
        double ny = 0.0;
        for (int child_idx=0; child_idx<num_children; child_idx++)
        {
            int Cidx = tree.getChildIndex(node_idx,child_idx);
            double cy = node_locations[Cidx].getY();
            ny = Math.max(cy+getDisplayEdgeLength(Cidx), ny);
        }

        double nx;
        if (num_children%2==1)
        {
            // odd number of children: put x coordinate at the middle child, but shift it a little bit 
            // or else it looks too confusing with edge labels and fancy colorings
            nx = node_locations[tree.getChildIndex(node_idx,num_children/2)].getX();
            if (num_children!=1)
            { // i.e., at least 3
                int neighbor_idx = num_children/2+(((int)ny) % 2==0 ? 1 : -1); // somewhat randomly left or right neighbor 
                double nx_shifted = (0.6*nx+0.4*node_locations[tree.getChildIndex(node_idx,neighbor_idx)].getX());
                //System.out.println("#*TP.p "+i+"/"+node[i].newickName()+"\t"+nx_shifted+" ["+nx+"]\t"+ny);
                nx = nx_shifted;
            }
        } else
        {
            int cm1 = (num_children-1)/2;
            int cm2 = num_children/2;
            nx = 0.5*(node_locations[tree.getChildIndex(node_idx,cm1)].getX()
                        + node_locations[tree.getChildIndex(node_idx,cm2)].getX());
        }
        return new IndexedPoint(node_idx,nx,ny);
    }
    
    @Override
    protected String getTooltipText(int x, int y, IndexedPoint p) 
    {
        if (p==null)
            return getToolTipText();
        else 
        {
            int i=p.getIndex();
            return Mien.getLongNodeName(tree, i);
        }
    }
    
    /**
     * A Spinner attached to the magnification value: changing the spinner calls {@link #setMagnification(double) }.
     * 
     * @return 
     */
    public JSpinner createMagnificationSpinner()
    {
        SpinnerModel spinner_model = 
                new SpinnerMagnifierModel(1.0, Mien.TREE_MAGNIFICATION_MIN, Mien.TREE_MAGNIFICATION_MAX);

        JSpinner magnification_spin = new JSpinner(spinner_model);

        JSpinner.NumberEditor magnification_editor = new JSpinner.NumberEditor(magnification_spin, "#%");
        magnification_editor.getTextField().setFont(new Font("SansSerif", Font.PLAIN, 10));
        magnification_editor.getTextField().setBorder(null);
        magnification_editor.getTextField().setEditable(false);
        magnification_editor.getTextField().setBackground(this.getBackground()); // setEditable() may change the background color


        magnification_editor.getTextField().setToolTipText("Current magnification value");
        magnification_spin.setToolTipText("Change magnification");
        magnification_spin.setEditor(magnification_editor);
        magnification_spin.setMaximumSize(new Dimension(80,30));
        
                
        magnification_spin.addChangeListener(new javax.swing.event.ChangeListener() 
            {
                @Override
                public void stateChanged(javax.swing.event.ChangeEvent E) 
                {
                    double value = ((Number)magnification_spin.getValue()).doubleValue();
                    if (value!=getMagnification())
                    {
                        setMagnification(value);
                    }
                }
            });        
        
        this.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) 
            {
                if (MAGNIFICATION_PROPERTY.equals(evt.getPropertyName()))
                {
                    double current_mag = getMagnification();
                    double spin_value =((Number)magnification_spin.getValue()).doubleValue();
                    if (spin_value != current_mag)
                    {
                        magnification_spin.setValue(current_mag);
                    }
                }
            }
        });
        
        return magnification_spin;
    }
    
    /**
     * A little box for choosing the layout, with the attached listeners to update the tree layout.
     * 
     * @return 
     */
    public Box createLayoutChooser()
    {
        class LayoutSelector implements ActionListener
        {
            LayoutSelector(LayoutStyle layout)
            {
                this.layout = layout;
            }
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                TreePanel.this.layout_style = layout;
                TreePanel.this.initializeNodeLocations();
                TreePanel.this.valid_bounding_boxes = false;
                TreePanel.this.revalidate();
                TreePanel.this.repaint();
            }
            
            private final LayoutStyle layout;
        }
        
        String apple_button_size = "small";
        
        JRadioButton pitchfork = new JRadioButton("curved");
        pitchfork.putClientProperty("JComponent.sizeVariant", apple_button_size);        
        pitchfork.setSelected(TreePanel.this.layout_style == LayoutStyle.PITCHFORK);
        
        JRadioButton clado = new JRadioButton("cladogram");
        clado.setSelected(TreePanel.this.layout_style == LayoutStyle.CLADOGRAM);
        clado.putClientProperty("JComponent.sizeVariant", apple_button_size);        

        JRadioButton phylo = new JRadioButton("phylogram");
        phylo.setSelected(TreePanel.this.layout_style == LayoutStyle.PHYLOGRAM);
        phylo.setEnabled(TreePanel.this.tree.hasLength());
        phylo.putClientProperty("JComponent.sizeVariant", apple_button_size);        

        JRadioButton pheno = new JRadioButton("phenogram");
        pheno.setSelected(TreePanel.this.layout_style == LayoutStyle.PHENOGRAM);
        pheno.putClientProperty("JComponent.sizeVariant", apple_button_size);        
        
        ButtonGroup layout_choices = new ButtonGroup();
        layout_choices.add(pitchfork);
        layout_choices.add(clado);
        layout_choices.add(pheno);
        layout_choices.add(phylo);
        
        pitchfork.addActionListener(new LayoutSelector(LayoutStyle.PITCHFORK));
        clado.addActionListener(new LayoutSelector(LayoutStyle.CLADOGRAM));
        phylo.addActionListener(new LayoutSelector(LayoutStyle.PHYLOGRAM));
        pheno.addActionListener(new LayoutSelector(LayoutStyle.PHENOGRAM));
        
        Box B = Box.createHorizontalBox();
        B.add(pheno);
        B.add(clado);
        B.add(phylo);
        B.add(pitchfork);
        
        return B;
    }
    
    
    
    
    /**
     * Embeds the tree panel into a scrollable interface.
     * There is a controll bar at the bottom, 
     * which (by default) contains a magnification spinner.
     */
    public final static class Zoomed<PANEL extends TreePanel> extends JPanel
    {
        private final PANEL tree_panel;
        private final JScrollPane tree_scroll;

        private Box controll_bar=null;
        
        private double initial_magnification = Double.NaN;
        
        private final JSpinner zoom_spinner;
        
        public Zoomed(PANEL tree_panel)
        {
            this.tree_panel = tree_panel;
            this.zoom_spinner = tree_panel.createMagnificationSpinner();
            tree_scroll = new JScrollPane(tree_panel);
            tree_scroll.getViewport().setBackground(tree_panel.getBackground());
    //        tree_scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    //        tree_scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            initComponents();
        }
        
        public JScrollPane getScrollPane(){ return tree_scroll;}
        
        public Box getControllBar(){ return controll_bar;}
        public void setControllBar(Box controll_bar)
        {
            this.controll_bar = controll_bar; 
            this.add(controll_bar, BorderLayout.SOUTH);
            revalidate();
        }
        
        public PANEL getTreePanel(){ return tree_panel;}
        
        private void initComponents()
        {
            this.setLayout(new BorderLayout());
            this.add(tree_scroll, BorderLayout.CENTER);
            Box bar = Box.createHorizontalBox();
            bar.add(Box.createHorizontalGlue());
            bar.add(zoom_spinner);
            
            setControllBar(bar);
            
        }
        
        @Override
        protected void paintComponent(Graphics g)
        {
            if (Double.isNaN(this.initial_magnification))
            {
                tree_panel.computeNodeLabelBoundingBoxes(g);
                tree_panel.computeEdgeLabelBoundingBoxes(g);

                Dimension preferred_size = tree_panel.getPreferredSize();
                int w = tree_panel.getWidth();
                int h = tree_panel.getWidth();
                double preferred_area = preferred_size.getWidth()*preferred_size.getHeight();
                double actual_area = w*h;
                double mag = actual_area/preferred_area;
                
                double dw = w/preferred_size.getWidth();
                double dh = h/preferred_size.getHeight();
                mag = Math.min(mag, Math.sqrt(dw));
                mag = Math.min(mag, Math.sqrt(dh));
                mag = mag<Mien.TREE_MAGNIFICATION_MIN?Mien.TREE_MAGNIFICATION_MIN:(mag>Mien.TREE_MAGNIFICATION_MAX?Mien.TREE_MAGNIFICATION_MAX:mag);
                this.initial_magnification = mag;
//                System.out.println("#*TP.Z inimag "+initial_magnification+"\tpref "+preferred_size+" ("+preferred_area+")\tact "+w+"*"+h+"="+actual_area+"\tdw "+dw+" (prefw="+preferred_size.getWidth()+")\tdh "+dh+" prefh="+preferred_size.getHeight()+")");
                
                tree_panel.setMagnification(mag);
                
            }
            super.paintComponent(g);
        }
        
        
    }


    protected NodeLabel setNodeLabel( int node_idx, String label_text, int dx, int dy, double theta, float anchor_point)
    {
        NodeLabel label = new NodeLabel(label_text, getDisplayedNodeLocation(node_idx), dx, dy, theta, anchor_point);
        this.displayed_node_labels[node_idx] = label;
        return label;
    }
    
    protected NodeLabel setNodeLabel(int node_idx, String label_text, String trimmed_text, int dx, int dy, double theta, float anchor_point)
    {
        NodeLabelTrimmed label = new NodeLabelTrimmed(label_text, trimmed_text, getDisplayedNodeLocation(node_idx), dx, dy, theta, anchor_point);
        this.displayed_node_labels[node_idx]= label;
        return label;
    }
    
    protected class NodeLabel
    {
        private final IndexedPoint point;
        private int dx;
        private int dy;
        private String label_text;
        private double theta;
        private float anchor_point;
        
        private NodeLabel(String label_text, IndexedPoint point, int dx, int dy, double theta, float anchor_point)
        {
            this.point = point;
            this.dx = dx;
            this.dy = dy;
            this.label_text = label_text;
            this.theta = theta;
            this.anchor_point = anchor_point;
        }
        
        protected final int getOffsetX(){ return dx;}
        protected final int getOffsetY(){ return dy;}
        protected final String getLabelText(){ return label_text;}
        protected final void setLabelText(String txt){this.label_text=txt;} 
        protected final double getRotation(){ return theta;}
        protected final float getAnchorOffset(){ return anchor_point;}
        protected final IndexedPoint getPoint(){return point;}
        
        public final int getX(){ return (int)point.getX()+dx;}
        public final int getY(){ return (int)point.getY()+dy;}
        
        private Color label_background_fill;
        private Color label_stroke_color;
//        private Color label_background_stroke;
        
        public void setLabelBackgroundFill(Color c)
        {
            this.label_background_fill=c;
        }
        
        public void setColor(Color c)
        {
            this.label_stroke_color = c;
        }
        
//        public void setLabelBackgroundStroke(Color c)
//        {
//            this.label_background_stroke=c;
//        }
        protected final int getFontHeight(Graphics2D g2)
        {
            return g2.getFontMetrics().getHeight();
        }
        
        
        public void draw(Graphics2D g2)
        {
            int hgt = getFontHeight(g2);
            if (hgt<Mien.FONT_SIZE_MIN)
                return;
            
            
            int x = getX();
            int y = getY();
            if (label_background_fill != null)
            {
                Color label_color = g2.getColor();
                g2.setColor(label_background_fill);
                Rectangle2D labelR = DrawString.getBoundingBoxForRotatedString(g2, label_text, x, y, theta, anchor_point);
                
                g2.fillRoundRect((int)labelR.getX(), (int)labelR.getY(), (int)labelR.getWidth(), (int)labelR.getHeight(), hgt/2, hgt/2);
                g2.setColor(label_color);
            }

            Color old_color = g2.getColor();
            if (label_stroke_color != null)
                g2.setColor(label_stroke_color);
            DrawString.drawRotatedString(g2,label_text, x, y, theta, anchor_point); 
            g2.setColor(old_color);
        }
        
        /**
         * Boundiung box relative to the node position.
         * 
         * @param g2
         * @return 
         */
        public Rectangle2D getBoundingBox(Graphics2D g2)
        {
            int hgt = getFontHeight(g2);
            if (hgt<Mien.FONT_SIZE_MIN)
            {
                return new Rectangle2D.Double(-hgt/2.0,0,hgt,hgt);
            } else
            {
                int x = getOffsetX();
                int y = getOffsetY();
                return DrawString.getBoundingBoxForRotatedString(g2, label_text, x, y, theta, anchor_point);
            }
        }
    }
    
    
    
    private class NodeLabelTrimmed extends NodeLabel
    {
        private NodeLabelTrimmed(String label_text, String trimmable_text, IndexedPoint point, int dx, int dy, double theta, float anchor_point)
        {
            super(label_text, point, dx, dy, theta, anchor_point);
            this.trimmable_text = trimmable_text;
        }
        
        private final String trimmable_text;
        
        @Override
        public void draw(Graphics2D g2)
        {
            int hgt = getFontHeight(g2);
            if (hgt<Mien.FONT_SIZE_MIN)
                return;
            
            String full_name = trimmable_text;
            if (full_name!=null && !full_name.equals(""))
            {
                FontMetrics label_fm = g2.getFontMetrics();
                int x = getX();
                int y = getY();
                //int h = label_fm.getHeight()+3;
                String written_label = getLabelText();
                for (int j=full_name.length(); j>=0; j--)
                {
                    String dn = full_name.substring(0,j);
                    String cand_label = getLabelText()+" ["+dn;
                    if (j<full_name.length())
                        cand_label = cand_label+"...";
                    cand_label = cand_label+"]";
                    
                    Rectangle2D covered_by_label = DrawString.getBoundingBoxForRotatedString(g2, cand_label, x, y, getRotation(), getAnchorOffset());
                    int num_nodes_covered = getNumCoveredPoints(covered_by_label);
                    if (covered_by_label.contains(getPoint()))
                    {
                        num_nodes_covered --;
                    }
                    
                    if (j==0 || num_nodes_covered==0 || TreePanel.this.isSelected(getPoint().getIndex()))
                    {
                        written_label = cand_label;
                        break;
                    }
                }
                String old_label = getLabelText();
                setLabelText(written_label);
                super.draw(g2);
                setLabelText(old_label);
            }
        }
        
        @Override
        public Rectangle2D getBoundingBox(Graphics2D g2)
        {
            int hgt = getFontHeight(g2);
            if (hgt<Mien.FONT_SIZE_MIN)
            {
                return new Rectangle2D.Double(0,0,0, hgt);
            } else
            {
                String label = getMagnification()<1.0?" ":"mmmmmm";
                Rectangle2D label_area = DrawString.getBoundingBoxForRotatedString(g2, label, getOffsetX(), getOffsetY(), getRotation(), getAnchorOffset());
                return label_area;
            }
        }
    }

}