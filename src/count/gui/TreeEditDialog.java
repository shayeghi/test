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

import count.gui.kit.GrahamScan;
import count.gui.kit.IndexedPoint;
import count.io.DataFile;
import count.model.IndexedTree;
import count.model.IndexedTreeTraversal;
import count.model.Phylogeny;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.awt.geom.Point2D;
        
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;

/**
 *
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 */
public class TreeEditDialog extends JDialog
{
    private static final String EDIT_PRUNE = "Prune";
    private static final String EDIT_GRAFT_ABOVE = "Graft as sibling";
    private static final String EDIT_GRAFT_BELOW = "Graft as a new child";
    private static final String EDIT_REROOT = "Root here";
    private static final String EDIT_FUSE = "Fuse into parent";
    private static final String EDIT_CANCEL = "Cancel selection";
    
    private final App main_application;
    private final DataFile<IndexedTree> initial_data;
    private final List<TreePanel.Zoomed<TreeManipulator>> edited_trees;
    private int currently_edited_tree;
    private CardLayout layout;
    
    public TreeEditDialog(App daddy, String title)
    {
        super(daddy.getTopFrame(), title, true); // modal
        this.main_application = daddy;
        this.initial_data = main_application.getActiveWorkSpace().getSelectedTreeData();
        this.edited_trees = new ArrayList<>();
        initComponents();
    }
    
    /**
     * Final edited tree after user pressed cancel or ok.
     * 
     * @return null if cancelled, or tree was not edited 
     */
    public DataFile<IndexedTree> getEditedTree()
    {
        if (currently_edited_tree==0)            
            return null;
        else
            return edited_trees.get(currently_edited_tree).getTreePanel().getData();
    }
    
    
    private void initComponents()
    {
        layout = new CardLayout();
        this.getContentPane().setLayout(layout);
        addCopy(createCopy(initial_data));
    }
    
    private DataFile<IndexedTree> createCopy(DataFile<IndexedTree> template)
    {
        Phylogeny phylo = new Phylogeny();
        phylo.copyFromTree(template.getContent());
        String orig_name = (template.getFile()==null || template.getFile().getName()==null)?"":template.getFile().getName();
        String orig_parent = (template.getFile()==null)?null:template.getFile().getParent();
        String copy_name = edited_trees.isEmpty()?orig_name:Mien.createIdentifier(edited_trees.size()-1); //orig_name+"_"+Mien.anyIdentifier();
        
        File copy_file = new File(orig_parent, copy_name);
        DataFile<IndexedTree> copy = new DataFile<>(phylo,copy_file);
        
        return copy;
    }
    
    private TreeManipulator addCopy(DataFile<IndexedTree> copy)
    {
        while (edited_trees.size()>currently_edited_tree+1)
        {
            int idx = edited_trees.size()-1;
            TreePanel.Zoomed<TreeManipulator> deleted_tree_panel = edited_trees.get(idx);
            layout.removeLayoutComponent(deleted_tree_panel);
            this.getContentPane().remove(deleted_tree_panel);
            edited_trees.remove(idx);
            
            assert (edited_trees.size() == idx);
        }
        TreePanel.LayoutStyle tree_layout =edited_trees.isEmpty()
                        ?TreePanel.LayoutStyle.PHENOGRAM
                        :edited_trees.get(currently_edited_tree).getTreePanel().getTreeLayoutStyle();
        
        this.currently_edited_tree = edited_trees.size();

        TreeManipulator edit_this = new TreeManipulator(copy, tree_layout);
        
        main_application.getActiveWorkSpace().getTreesBrowser().calculateNodeColors(edit_this);
        
        TreePanel.Zoomed<TreeManipulator> zumm = new TreePanel.Zoomed<>(edit_this);
        zumm.setControllBar(edit_this.createControllBar());

        String zumm_name = Mien.createIdentifier(currently_edited_tree);
        this.getContentPane().add(zumm, zumm_name);
        layout.show(this.getContentPane(), zumm_name);
        edited_trees.add(zumm);

        return edit_this;
    }
    
    /**
     * Called by undo.
     */
    private void undoEditedTrees()
    {
        assert (this.currently_edited_tree>0);
        
        this.currently_edited_tree--;
        TreePanel.Zoomed<TreeManipulator> zumm = edited_trees.get(currently_edited_tree);
        String zumm_name = Mien.createIdentifier(currently_edited_tree);
        this.getContentPane().add(zumm, zumm_name);
        layout.show(this.getContentPane(), zumm_name);
    }
    
    
    private class TreeManipulator extends TreePanel implements MouseListener, MouseMotionListener
    {
        private final Phylogeny starting_phylo;

        TreeManipulator(DataFile<IndexedTree> starting_data, TreePanel.LayoutStyle layout)
        {
            super(starting_data, layout, ListSelectionModel.SINGLE_SELECTION);
            this.starting_phylo = (Phylogeny) starting_data.getContent();
            this.setAreaSelectionEnabled(false);
            this.setPointSelectionEnabled(false);
            
            initListeners();
            
        }
        
        private IndexedPoint prune_position;
        private int[] subtree_node_indices;
//        private IndexedPoint[] subtree_displayed_positions;
        private Polygon pruned_tree_hull;

        private Point regraft_position;
        
        
        private void initListeners()
        {
            this.addMouseListener(this);
            this.addMouseMotionListener(this);
        }
        
        
        private void clearPruningNode()
        {
            this.prune_position = null;
            this.pruned_tree_hull = null;
            this.subtree_node_indices = null;
//            getSelectionModel().clearSelection();
        }
        
        private void setPruningNode(int node_index)
        {
            this.prune_position = getDisplayedNodeLocation(node_index);
            this.regraft_position = new Point((int)prune_position.getX(), (int)prune_position.getY());
            
            final List<Point2D> point_list = new ArrayList<>();
            final List<IndexedPoint> subtree_nodes = new ArrayList<>();

//            IndexedTreeTraversal.NodeCollector subtree_nodes = new IndexedTreeTraversal.NodeCollector();
            IndexedTreeTraversal.prefix(starting_phylo.getNode(node_index), 
                    new IndexedTree.NodeVisitor() {
                @Override
                public void visit(IndexedTree.Node node) 
                {
                    IndexedPoint node_loc = getDisplayedNodeLocation(node.getIndex());
                    point_list.add(node_loc);
                    subtree_nodes.add(node_loc);
                    if (node.getIndex() != node_index) 
                    { // if not the subtree root, then add a point for the convex hull
                        IndexedTree.Node parent = node.getParent();
                        IndexedPoint parent_loc = getDisplayedNodeLocation(parent.getIndex());
                        Point2D edge_bend = new Point2D.Double(node_loc.getX(), parent_loc.getY());
                        point_list.add(edge_bend);
                    }
                }
            });
            subtree_node_indices = new int[subtree_nodes.size()];
            for (int i=0;i<subtree_nodes.size(); i++)
            {
                IndexedPoint P = subtree_nodes.get(i);
                subtree_node_indices[i] = P.getIndex();
            }
                
            
//            subtree_displayed_positions = new IndexedPoint[subtree_nodes.size()];
//            for (int i=0; i<subtree_displayed_positions.length; i++)
//            {
//                IndexedTree.Node node = subtree_nodes.get(i);
//                subtree_displayed_positions[i] = getDisplayedNodeLocation(node.getIndex());
//            }
            GrahamScan GS = new GrahamScan(point_list.toArray(new Point2D[]{}));
            java.awt.geom.Point2D[] subtree_convex_hull = GS.getHull();
            this.pruned_tree_hull = new Polygon();
            for (java.awt.geom.Point2D P: subtree_convex_hull)
            {
                this.pruned_tree_hull.addPoint((int)P.getX(), (int)P.getY());
            }
//            getSelectionModel().setSelectionInterval(node_index, node_index);
        }
        
        
        /**
         * Makes a small drawing of the selected subtree. 
         * 
         * @param g 
         */
        private void paintGraftingSubtree(Graphics g)
        {
            Graphics2D g2 = (Graphics2D)g.create();

            double w = pruned_tree_hull.getBounds2D().getWidth();
            double h = pruned_tree_hull.getBounds2D().getHeight();
            
            double FIXED_SIZE = 25.0*Mien.TREE_POINT_SIZE;
            double scala = Math.min(FIXED_SIZE/(w+1e-9), FIXED_SIZE/(h+1e-9));
            
            g2.scale(scala, scala);
            g2.translate(regraft_position.getX()/scala-prune_position.getX(), regraft_position.getY()/scala-prune_position.getY());
            g2.setColor(Mien.SMOKY_BACKGROUND);
            g2.fillPolygon(pruned_tree_hull);
            g2.setColor(Mien.TREE_EDGE_COLOR);
            for (int pidx: subtree_node_indices)
            {
                if (!starting_phylo.isLeaf(pidx))
                    this.plotEdgesToChildren(g2, pidx);
            }
            for (int pidx: subtree_node_indices)
            {
                this.plotNode(g2, pidx);
            }
        }
        
        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            if (prune_position != null )
            {
                Color col = g.getColor();
                g.setColor(Mien.AREA_SELECTION_COLOR);
                g.fillPolygon(pruned_tree_hull);
                g.setColor(Color.RED);
                g.drawPolygon(pruned_tree_hull);
                g.drawLine((int) prune_position.getX(), (int)prune_position.getY(), (int)regraft_position.getX(), (int)regraft_position.getY());
                paintGraftingSubtree(g);
                g.setColor(col);
            }
        }
        
        private JButton undo_button;
//        private JButton redo_button;
        private JButton ok_button;
        private JButton cancel_button;
        
        private Box createControllBar()
        {
            Box bar = Box.createHorizontalBox();
            bar.add(createLayoutChooser());
            bar.add(Box.createHorizontalGlue());
            
            undo_button = new JButton("Undo");
            
            ok_button = new JButton("Done editing");
//            ok_button.setOpaque(true);
//            ok_button.setForeground(Mien.OK_COLOR);
            
            
            cancel_button = new JButton("Cancel editing");
//            cancel_button.setOpaque(true);
//            cancel_button.setForeground(Mien.CANCEL_COLOR);

//            bar.add(redo_button);

            bar.add(cancel_button);            
            cancel_button.addActionListener(new ActionListener() 
            {
                @Override
                public void actionPerformed(ActionEvent e) 
                {
                    TreeEditDialog.this.currently_edited_tree = 0;
                    TreeEditDialog.this.dispose();
                }
            });

            if (TreeEditDialog.this.currently_edited_tree>0)
            {
                bar.add(undo_button);
                undo_button.addActionListener(new ActionListener() 
                {
                    @Override
                    public void actionPerformed(ActionEvent e) 
                    {
                        undoEditedTrees();
                    }
                });
            }

            bar.add(ok_button);
            ok_button.addActionListener(new ActionListener() 
            {
                @Override
                public void actionPerformed(ActionEvent e) 
                {
                    TreeEditDialog.this.dispose();
                }
            });
            
//            ok_button = new JButton("OK");
//            cancel_button = new JButton("Cancel");
//            Box button_box = new Box(BoxLayout.LINE_AXIS);
//            button_box.add(Box.createHorizontalGlue());
//            button_box.add(cancel_button);
//            button_box.add(Box.createRigidArea(new Dimension(10,0)));

            bar.add(Box.createHorizontalGlue());
            bar.add(createMagnificationSpinner());
            return bar;
        }
    
        @Override
        public void mouseMoved(MouseEvent evt)
        {
            if (this.prune_position != null)
            {
                this.regraft_position = evt.getPoint();
                repaint();
            }            
        }

        @Override
        public void mouseClicked(MouseEvent e) 
        {
            int x=e.getX();
            int y=e.getY();

            IndexedPoint p=getClosestPoint(x,y);
            if (p==null)
            {
                if (prune_position != null)
                {
                    clearPruningNode();
                    //repaint();
                } 
                this.getSelectionModel().clearSelection(); // triggers repaint
            } else
            {
                int pidx = p.getIndex();
                this.getSelectionModel().setSelectionInterval(pidx, pidx);
                
                NodeActions node_actions = new NodeActions(starting_phylo.getNode(pidx));
                node_actions.show();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) 
        {
            // ignore
        }

        @Override
        public void mouseReleased(MouseEvent e) 
        {
            // ignore
        }

        @Override
        public void mouseEntered(MouseEvent e) 
        {
            // ignore
        }

        @Override
        public void mouseExited(MouseEvent e) 
        {
            // ignore
        }

        @Override
        public void mouseDragged(MouseEvent e) 
        {
            // ignore
        }

        private class NodeActions implements ActionListener
        {
            private final Phylogeny.Taxon node;
            
            NodeActions(Phylogeny.Taxon node)
            {
                this.node = node;
            }
            
            @Override 
            public void actionPerformed(ActionEvent e)
            {
                JMenuItem src = (JMenuItem) e.getSource();
                String menu_text = src.getText();
                System.out.println("#*TED.TM.NA.aP "+node+"\t"+menu_text+"\te "+e);
                
                if (EDIT_PRUNE.equals(menu_text))
                {
                    setPruningNode(node.getIndex());
                } else if (EDIT_GRAFT_ABOVE.equals(menu_text))
                {

                    DataFile<IndexedTree> modified_tree = createCopy(TreeManipulator.this.getData());
                    Phylogeny copied_phylo = (Phylogeny)modified_tree.getContent();
                    copied_phylo.moveNode(copied_phylo.getNode(prune_position.getIndex()), copied_phylo.getNode(node.getIndex()), false);
                    addCopy(modified_tree);
                } else if (EDIT_GRAFT_BELOW.equals(menu_text))
                {
                    DataFile<IndexedTree> modified_tree = createCopy(TreeManipulator.this.getData());
                    Phylogeny copied_phylo = (Phylogeny)modified_tree.getContent();
                    copied_phylo.moveNode(copied_phylo.getNode(prune_position.getIndex()), copied_phylo.getNode(node.getIndex()), true);
                    addCopy(modified_tree);
                } else if (EDIT_REROOT.equals(menu_text))
                {
                    DataFile<IndexedTree> modified_tree = createCopy(TreeManipulator.this.getData());
                    Phylogeny copied_phylo = (Phylogeny)modified_tree.getContent();
                    copied_phylo.reroot(copied_phylo.getNode(node.getIndex()));
                    addCopy(modified_tree);
                } else if (EDIT_FUSE.equals(menu_text))
                {
                    DataFile<IndexedTree> modified_tree = createCopy(TreeManipulator.this.getData());
                    Phylogeny copied_phylo = (Phylogeny)modified_tree.getContent();
                    copied_phylo.fuseIntoParent(copied_phylo.getNode(node.getIndex()));
//                    
//                    System.out.println("#*TED.TM.NA.aP "+TreeManipulator.this.getData().getContent().hasLength()+" -> "+copied_phylo.hasLength());
                    addCopy(modified_tree);
                } else if (EDIT_CANCEL.equals(menu_text))
                {
                    // do nothing
                    clearPruningNode();
                    getSelectionModel().clearSelection();
                }
            }
            
            void show()
            {
                JPopupMenu popup_options = new JPopupMenu();
                popup_options.setBorder(BorderFactory.createTitledBorder(Mien.getLongNodeName(starting_phylo, node.getIndex())));
                
                if (prune_position == null)
                {
                    if (!node.isRoot())
                    {
                        JMenuItem prune = new JMenuItem(EDIT_PRUNE);
                        prune.addActionListener(this);
                        popup_options.add(prune);
                        
                        if (!node.isLeaf())
                        {
                            JMenuItem reroot = new JMenuItem(EDIT_REROOT);
                            reroot.addActionListener(this);
                            popup_options.add(reroot);

                            JMenuItem fuse = new JMenuItem(EDIT_FUSE);
                            fuse.addActionListener(this);
                            popup_options.add(fuse);
                        }
                    }                
                } else
                {
                    JMenuItem graft_above = new JMenuItem(EDIT_GRAFT_ABOVE);
                    graft_above.addActionListener(this);
                    popup_options.add(graft_above);
                    if (!node.isLeaf())
                    {
                        JMenuItem graft_below = new JMenuItem(EDIT_GRAFT_BELOW);
                        graft_below.addActionListener(this);
                        popup_options.add(graft_below);
                    }

                }
                JMenuItem cancel = new JMenuItem(EDIT_CANCEL);
                cancel.addActionListener(this);
                popup_options.add(cancel);
                
                IndexedPoint point = getDisplayedNodeLocation(node.getIndex());
                
                popup_options.show(TreeManipulator.this, (int)point.getX(), (int)point.getY());
            }
        }
    } // TreeManipulator
}
