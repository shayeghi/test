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

import java.awt.Component;
import java.awt.GridLayout;
import java.io.File;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import count.model.IndexedTree;
import count.io.DataFile;
import count.model.OccurrenceTable;
import count.model.RateVariation;

/**
 * Class for the workspace associated with a session. Each session has a unique 
 * phylogeny, but may have multiple data and rate sets. 
 *
 * @author  csuros
 */

public class WorkSpace extends JPanel
{
    
    /**
     * Initializes a work space with tree components in a tabbed pane: trees, data and rates.
     * @param A
     * @param main_tree
     */
    public WorkSpace(App A, DataFile<IndexedTree> main_tree)
    {
        super(new GridLayout(1,1));
        this.app = A;
        this.work_area = new JTabbedPane();
        
        File path_to_tree_file = main_tree.getFile();
        
        this.session_name = Mien.chopFileExtension(path_to_tree_file.getName());
        
        trees_panel = new TreeCollection(createTreePanel(main_tree));
        
        data_browser = new Browser<>(Browser.PrimaryItem.class);
        
        rates_browser = new Browser<>(Browser.PrimaryItem.class);
        
        initComponents();
        
        checkPhylogeny(main_tree.getContent());
    }
    
    private final App app;
    private final JTabbedPane work_area;
    
    
    /**
     * Title for the Rates tab
     */
    public static final String RATES_PANEL = "Rates";
    
    /**
     * Title for the Data tab
     */
    public static final String DATA_PANEL = "Data";

    /**
     * Title for the Tree tab
     */
    public static final String TREE_PANEL = "Tree"; 
    
    
    /**
     * Browser in the Data tab
     */
    private final Browser<Browser.PrimaryItem> data_browser;
    
    /**
     * Browser in the Rate tab
     */
    private final Browser<Browser.PrimaryItem> rates_browser;    
    
    /**
     * Trees
     */
    private final TreeCollection trees_panel;

    
    private String session_name;
    
    private void initComponents()
    {
        work_area.removeAll();
        work_area.add(TREE_PANEL, trees_panel);
        work_area.add(DATA_PANEL, data_browser);
        work_area.add(RATES_PANEL, rates_browser);
        
        work_area.setToolTipTextAt(work_area.indexOfTab(TREE_PANEL), "Phylogeny ");
        work_area.setToolTipTextAt(work_area.indexOfTab(DATA_PANEL), "Data set(s)");
        work_area.setToolTipTextAt(work_area.indexOfTab(RATES_PANEL), "Rate model(s)");
        if (this.getApp().isMac())
        {
            work_area.setForegroundAt(work_area.indexOfTab(TREE_PANEL), Mien.WORKTAB_TREES_COLOR);
            work_area.setForegroundAt(work_area.indexOfTab(DATA_PANEL), Mien.WORKTAB_DATA_COLOR);
            work_area.setForegroundAt(work_area.indexOfTab(RATES_PANEL), Mien.WORKTAB_RATES_COLOR);
        }
        
        super.add(work_area);
    }
    
    
    public String getSessionName(){ return this.session_name;}
    
    public App getApp()
    {
        return app;
    }
    
    public static App getApp(Component C)
    {
        while (C != null)
        {
            if (C instanceof WorkSpace)
            {
                WorkSpace ws = (WorkSpace) C;
                return ws.getApp();
            } 
            C = C.getParent();
        }
        // now if we got here, we got a problem.
        return null;
    }
    
 
//    /**
//     * Returns the browser associated with the Rates tab.
//     * @return rates browser
//     */
//    public Browser getRatesBrowser()
//    {
//        return rates_browser;
//    }
//    
//    /**
//     * Returns the browser associated with the Data tab.
//     * @return data browser
//     */
//    public Browser getDatasetsBrowser()
//    {
//        return data_browser;
//    }    
    
    public String[] getTerminalNames()
    {
        return trees_panel.getTaxonNames();
    } 
    
    
    private TreePanel createTreePanel(DataFile<IndexedTree> tree_data)
    {
        TreePanel TP = new TreePanel(tree_data);
        TP.setNormalFontSize((TP.getNormalFontSize()*7)/6); // 12->14, 11->12.8, 10->11.7
        return TP;
    }            
    
    /**
     * Adds a new (possibly first) tree to the tree panel
     * @param tree_data 
     */

    public void addTree(DataFile<IndexedTree> tree_data)
    {
        TreePanel TP = createTreePanel(tree_data);
        trees_panel.addTree(TP);
        work_area.setSelectedIndex(work_area.indexOfTab(TREE_PANEL));
    }
    
    public DataFile<IndexedTree> getSelectedTreeData()
    {
        return trees_panel.getActiveTree().getData();
    }
    
    /**
     * Adds a new data set to the Data tab's browser and switches to it.
     * @param table_data
     */ 
    public void addDataSet(DataFile<OccurrenceTable> table_data)
    {
        OccurrenceTablePanel FP = new OccurrenceTablePanel(table_data);
        data_browser.addTopItem(FP);
        work_area.setSelectedIndex(work_area.indexOfTab(DATA_PANEL));
        
    }
    
    public void addRates(DataFile<RateVariation> rates_data, boolean add_at_top)
    {
        RateVariationPane RP = new RateVariationPane(rates_data);
        if (add_at_top)
            rates_browser.addTopItem(RP);
        else
            rates_browser.addItem(RP);
        
        work_area.setSelectedIndex(work_area.indexOfTab(RATES_PANEL));
    }
    
    public TreeCollection getTreesBrowser(){ return this.trees_panel;}
        
    private void checkPhylogeny(IndexedTree tree)
    {
        // check if there are any problems
        StringBuffer problems = null;
        for (int node_idx=0; node_idx<tree.getNumNodes(); node_idx++)
        {
            if (tree.isLeaf(node_idx))
            {
                String leaf_name =tree.getName(node_idx);
                if (leaf_name.indexOf(' ')>=0)
                {
                    if (problems==null) problems = new StringBuffer("<ul>");
                    problems.append("<li>Leaf name `").append(leaf_name).append("' has a space: in Newick tree files, underscore " +
                            "within a node name is interpreted as a space --- verify if that is consistent " +
                            "with the table column names, where underscore is left as is.</li>\n");
                }
            } else if (tree.getNumChildren(node_idx)==1)
            {
                if (problems==null) problems = new StringBuffer("<ul>");
                problems.append("<li>Inner node `").append(Mien.getLongNodeName(tree, node_idx)).append("' has only 1 child. " +
                        "This may cause problems " +
                        "in certain analyses, or even a program error. " +
                        "The input tree is supposed to have bi- or multifurcations everywhere, " +
                        "except at the leaves.</li>\n");
            }
            if (tree.isRoot(node_idx) && tree.getNumChildren(node_idx)==2)
            {
                if (problems==null) problems = new StringBuffer("<ul>");
                problems.append("<li>The root has only two children. While it is not necessarily a problem, " +
                        "it may cause ambiguities in certain inference methods, " +
                        "particularly with reversible probabilistic models " +
                        "(Felsenstein's \"pulley principle\").</li>\n");
            }
        }
        if (problems != null)
        {
            problems.append("</ul>");

            JEditorPane problems_pane = new JEditorPane("text/html", "<h1>Possible problems with your tree</h1>"+problems.toString());
            problems_pane.setEditable(false);
            problems_pane.setBackground(new java.awt.Color(255, 255, 192));
            JScrollPane problems_scroll = new JScrollPane(problems_pane);
            problems_scroll.setMaximumSize(new java.awt.Dimension(500,400));
            problems_scroll.setPreferredSize(problems_scroll.getMaximumSize());
            JOptionPane.showMessageDialog(trees_panel,
                    problems_scroll,
                    "Is your tree file correct?",
                    JOptionPane.WARNING_MESSAGE
                    );
        }
    }    
}