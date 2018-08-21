/*
 * Copyright 2016 Miklos Csuros (csuros@iro.umontreal.ca).
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

import count.gui.kit.BoxIcon;
import count.gui.kit.DiamondIcon;
import count.model.IndexedTree;
import count.model.IndexedTreeTraversal;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

/**
 *
 * Collection of tree for the Trees tab: multiple phylogenies with the same terminal node set.
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 */
public class TreeCollection extends JPanel
{
    
    private final JTabbedPane tree_panels;
    /**
     * Common set of terminal names, and their indexes in the trees.
     */
    private final List<Map<String, Integer>> map_names;
    
    public TreeCollection(TreePanel first_panel)
    {
        super(new GridLayout(1,1));
        this.tree_panels= new JTabbedPane(JTabbedPane.LEFT);
        super.add(tree_panels);

//        IndexedTree first_tree = first_panel.getTree();

        // init name mapping 
//        terminal_names = new HashMap<>();
//        for (int leaf_idx=0; leaf_idx<first_tree.getNumLeaves(); leaf_idx++)
//            terminal_names.put(first_tree.getName(leaf_idx),leaf_idx);
        map_names = new ArrayList<>();
        initTerminalHues(first_panel.getData().getContent().getNumLeaves());
        addTree(first_panel);
        
    }
    
    public TreePanel getActiveTree()
    {
        int active_idx = tree_panels.getSelectedIndex();
        if (active_idx==-1)
            return null;
        else
        {
            TreePanel.Zoomed zum = (TreePanel.Zoomed)tree_panels.getComponentAt(active_idx);
            return zum.getTreePanel();
        }
    }
    
    
    public String[] getTaxonNames()
    {
        Map<String, Integer> taxon_indices = map_names.get(0);
        String[] names = new String[taxon_indices.size()];
        
        for(String s: taxon_indices.keySet())
            names[taxon_indices.get(s)]=s;
        return names;
    }
    
    /**'
     * Adds a listener to changes of tree selection
     * 
     * @param listener 
     */
    public void addChangeListener(ChangeListener listener)
    {
        tree_panels.addChangeListener(listener);
    }
    
    public final TreePanel addTree(TreePanel panel)
    {
        Map<String, Integer> leaf_name_mapping = mapTerminals(panel);
        if (leaf_name_mapping==null)
            return null;

        map_names.add(leaf_name_mapping);
        calculateNodeColors(panel, leaf_name_mapping);
                
        
        TreePanel.Zoomed<TreePanel> zoom = new TreePanel.Zoomed<>(panel);
        Box control_bar =  zoom.getControllBar();
        control_bar.removeAll();
        control_bar.add(panel.createLayoutChooser());
        control_bar.add(Box.createHorizontalGlue());
        control_bar.add(panel.createMagnificationSpinner());

        // add zoom and set short name
        tree_panels.add(Mien.chopFileExtension(panel.getData().getFile().getName()), zoom);
        
        
        tree_panels.setSelectedIndex(tree_panels.getTabCount()-1);
        
        return panel;
    }
    
    public final TreePanel getTreePanel(int idx)
    {
        return ((TreePanel.Zoomed) tree_panels.getComponentAt(idx)).getTreePanel();
    }
    
    public final TreePanel getTreePanel(String tree_name)
    {
        int tree_idx =  tree_panels.indexOfTab(tree_name);
        if (tree_idx==-1)
            return null;
        else
            return getTreePanel(tree_idx);
    }
    
    public int getTreePanelCount()
    {
        return tree_panels.getTabCount();
    }
    
    /**
     * Checks if a new tree has the same set of terminal nodes as the first one,
     * and computes the mapping.
     * @param other_panel new tree panel to be added
     * @return null if not the same terminal nodes
     */
    public Map<String, Integer> mapTerminals(TreePanel other_panel)
    {
        
        IndexedTree other_tree = other_panel.getData().getContent();
        
        if (tree_panels.getTabCount()==0)
        { // first tree to be added
            Map<String, Integer> terminal_names =new HashMap<>();
            for (int leaf_idx=0; leaf_idx<other_tree.getNumLeaves(); leaf_idx++)
                terminal_names.put(other_tree.getName(leaf_idx),leaf_idx);
            
            return terminal_names;
        } else
        {
            Map<String, Integer> terminal_names = map_names.get(0); // index mapping in first tree
            Map<String, Integer> mapped_leaves = new HashMap<>(); // index mapping in this tree
            StringBuilder sb_problem = new StringBuilder();
            IndexedTree first_tree = getTreePanel(0).getData().getContent();
            
            for (int leaf_idx=0; leaf_idx<other_tree.getNumLeaves(); leaf_idx++)
            {
                String leaf_name = other_tree.getName(leaf_idx);
                
                if (terminal_names.containsKey(leaf_name))
                {
                    if (mapped_leaves.containsKey(leaf_name))
                    {
                        sb_problem.append("<li>Leaf ").append(leaf_name).append(" appears more than once in the tree.</li>\n");
                    } else
                    {
                        int orig_idx = terminal_names.get(leaf_name);
                        String name_str  = first_tree.getName(orig_idx); // same String reused
                        mapped_leaves.put(name_str, leaf_idx);
                    }
                } else
                {
                    sb_problem.append("<li>Leaf ").append(leaf_name).append(" does not appear in first tree.</li>\n");
                }
            }
            for (String leaf_name: terminal_names.keySet())
            {
                if (!mapped_leaves.containsKey(leaf_name))
                {
                    sb_problem.append("<li>Leaf ").append(leaf_name).append(" from the first tree is missing from this tree.</li>\n");
                }
            }
            
            boolean tree_is_correct = (sb_problem.length()==0);
            
            if (tree_is_correct)
            {
                return mapped_leaves; 
            } else
            {
                StringBuilder page_text = new StringBuilder("<h1>Cannot add this tree</h1>");
                page_text.append("<p><em>(But you can start a new session with it...)</em></p>");
                page_text.append("<ul>").append(sb_problem).append("</ul>");
                JEditorPane problems_pane = new JEditorPane("text/html", page_text.toString());
                problems_pane.setEditable(false);
                problems_pane.setBackground(Mien.WARNING_COLOR);
                JScrollPane problems_scroll = new JScrollPane(problems_pane);
                problems_scroll.setMaximumSize(new java.awt.Dimension(500,400));
                problems_scroll.setPreferredSize(problems_scroll.getMaximumSize());
                JOptionPane.showMessageDialog(tree_panels,
                        problems_scroll,
                        "Is your tree file correct?",
                        JOptionPane.WARNING_MESSAGE
                        );
                return null;
            }
        }
    }
    
    private void initTerminalHues(int num_leaves)
    {
        terminal_hues = new float[num_leaves];
        float z = (num_leaves-1.f)*6f/5f; // 0..5/6 is accepted hue : keep gap between read and purple

        for (int leaf_idx=0; leaf_idx<num_leaves; leaf_idx++)
        {
            float hue = (leaf_idx)/z;
            terminal_hues[leaf_idx] = hue;
        }
    } 
    
    private float[] terminal_hues;
    private static final float BRIGHTNESS = 0.8f;
    
    public void calculateNodeColors(TreePanel panel)
    {
        Map<String, Integer> leaf_index = mapTerminals(panel);
        if (leaf_index != null)
        {
            this.calculateNodeColors(panel, leaf_index);
        }
    }
    
    /**
     * Colors the tree nodes by using the first tree as reference
     * 
     * @param panel
     * @param leaf_index mapping from leaf names 
     */
    private void calculateNodeColors(TreePanel panel, Map<String, Integer> leaf_index)
    {
//        Map<String, Integer> leaf_index = map_names.get(panel_idx);
//        TreePanel panel = getTreePanel(panel_idx);
        
        
        IndexedTree tree = panel.getData().getContent();
        int num_leaves = leaf_index.size();
        int num_nodes = tree.getNumNodes();
        
        float[] node_hues = new float[num_nodes];
        
        final int[] height = IndexedTreeTraversal.getHeights(tree);
        float root_height = height[height.length-1]; 

        int pt_size = panel.getTreePointSize();
        Map<String, Integer> original_index = map_names.get(0);
        int node_idx = 0;
        while (node_idx<num_leaves)
        {
            int j = original_index.get(tree.getName(node_idx));
            float hue = node_hues[node_idx] = terminal_hues[j];
            
            Color col = Color.getHSBColor(hue, 1.0f, BRIGHTNESS);
            BoxIcon leaf_icon = new BoxIcon(pt_size, true); // filled
            leaf_icon.setDrawColor(Mien.TREE_UNSELECTED_LEAF_COLOR);
            leaf_icon.setFillColor(col);
            
            BoxIcon selected_leaf_icon = new BoxIcon(pt_size, true);
            selected_leaf_icon.setDrawColor(Mien.TREE_SELECTED_LEAF_COLOR);
            selected_leaf_icon.setFillColor(col);
            selected_leaf_icon.setCrossing(Color.RED);

            panel.setNodeDisplayIcon(node_idx, selected_leaf_icon, leaf_icon);
            
//            System.out.println("#*TC.cNC "+node_idx+"\t"+hue+"\t"+tree.getNode(node_idx));
            
            node_idx++;
        }
        while (node_idx<tree.getNumNodes())
        {
            float hue = 0.0f;
            for (int ci=0; ci<tree.getNumChildren(node_idx); ci++)
            {
                int child_idx = tree.getChildIndex(node_idx, ci);
                float child_hue = node_hues[child_idx];
                hue += child_hue;
            }
            hue /= tree.getNumChildren(node_idx);
            node_hues[node_idx] = hue;
            
//            System.out.println("#*TC.cNC "+node_idx+"\t"+hue+"\t"+tree.getNode(node_idx));
            
            float sat = 1.0f-height[node_idx]/root_height;
            Color col = Color.getHSBColor(hue, sat, BRIGHTNESS);
//            DiamondIcon node_empty = new DiamondIcon(pt_size, false);
            DiamondIcon node_full = new DiamondIcon(pt_size, true);
            
//            node_empty.setDrawColor(col);
//            node_empty.setFillColor(Mien.TREE_UNSELECTED_NODE_COLOR);
            node_full.setDrawColor(Mien.TREE_UNSELECTED_NODE_COLOR);
            node_full.setFillColor(col);
            
            DiamondIcon selected_node_icon = new DiamondIcon(pt_size, true);
            selected_node_icon.setDrawColor(Mien.TREE_SELECTED_NODE_COLOR);
            selected_node_icon.setFillColor(col);
            
            panel.setNodeDisplayIcon(node_idx, selected_node_icon, node_full);
            
            node_idx++;
        }
    }
}
