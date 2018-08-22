/*
 * Copyright 2016 Miklos Csuros (csurosm@gmail.com).
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

import count.gui.*;
import count.io.DataFile;
import count.io.NewickParser;
import count.model.IndexedTree;
import count.model.OccurrenceTable;
import count.model.Phylogeny;
import count.model.RateVariation;
import count.util.Executable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;


/**
 *
 * Count application's entry point.
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 */
public class Count extends App
{
    
    @Override
    protected String getExecutableTitle()
    {
        return Executable.TITLE;
    }
    
    @Override
    protected String getVersionNumber()
    {
        return Executable.VERSION;
    }
    
    /** Creates a new instance of DealerCount 
     * @param frame th enclosing JFrame
     */
    public Count(JFrame frame) 
    {
        super(frame);
        init();
    }
   
    private void init()
    {
//        guide = new UsersGuide(top_frame);
//
//        int w = top_frame.getWidth();
//        int h = top_frame.getHeight();
//        int x = top_frame.getX();
//        int y = top_frame.getY();
//        guide.setBounds(x+20,y+5,Math.min(w-40, 1000),600);

        doAbout(true);

    }
    
//    private void addWorkSpace (TreePanel first_tree) 
//    {
//        WorkSpace ws = new WorkSpace(this, first_tree);
//        addWorkSpace(ws);
//    }
    
    private JMenuItem start_session;
    private JMenuItem load_session;
    private JMenuItem save_session;
    private JMenuItem load_session_url;
    private JMenuItem add_tree;
    private JMenuItem edit_tree;
    
    @Override 
    protected final void setSessionMenu()
    {
        if (load_session == null)
        {
            load_session = new JMenuItem("Open previously saved session ...");
            load_session.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, 
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())); 
            load_session.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        doLoadSession();
                    }
                });
            load_session.setToolTipText("This tooltip has no information");
            
            save_session = new JMenuItem("Save everything ...");
            save_session.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())); 
            save_session.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        doSaveSession();
                    }
                }
            );
            load_session_url = new JMenuItem("Load session by path name or URL ...");
            load_session_url.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, 
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())); 
            
            start_session = new JMenuItem("Start new session ...");
            start_session.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        doStartSession();
                    }
                });
            start_session.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, 
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())); 
            
            add_tree = new JMenuItem("Load phylogeny into active session ...");
            add_tree.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    doLoadTree();
                }
            });
            
            edit_tree = new JMenuItem("Edit tree");
            edit_tree.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    doEditTree();
                }
            });
        }
        
        super.setSessionMenu();
    }    

    @Override
    protected void addSessionMenuItems()
    {
        session_menu.add(load_session);
        session_menu.add(load_session_url);
        session_menu.add(start_session);
        super.addSessionMenuItems();
        session_menu.addSeparator();
        session_menu.add(add_tree);
        session_menu.add(edit_tree);
        session_menu.add(save_session);
    }
    
    private JMenuItem data_load_table;
    private JMenuItem data_load_annotated;
    /**
     * Sets up the elements of the <em>Data</em> menu.
     */
    @Override
    protected void setDataMenu()
    {
        if (data_menu.getItemCount()==0) // initialization
        {


            data_load_table = new JMenuItem("Open table...");
            data_load_table.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        doLoadTable(false);
                    }
                });
            data_load_table.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            data_menu.add(data_load_table);       

            data_load_annotated = new JMenuItem("Open annotated table...");
            data_load_annotated.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        doLoadTable(true);
                    }
                });
            data_menu.add(data_load_annotated);       
        }
        
    }
    
    private JMenuItem rates_load;

    /**
     * Sets up the elements of the <em>Rate</em> menu.
     */
    @Override
    protected void setRateMenu()
    {
        if (rate_menu.getItemCount()==0) // initialization
        {
            rates_load = new JMenuItem("Load rates...");
            rates_load.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    doLoadRates(null);
                }
            });
            rates_load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            rate_menu.add(rates_load);
        }
    }
    
    
    /**
     * Sets up the elements of the <em>Analysis</em> menu.
     */
    @Override
    protected void setAnalysisMenu()
    {}
    
    
    @Override
    protected void doAbout()
    {
        doAbout(false);
    }

    protected void doAbout(boolean splash)
    {
        String title = (splash?"Welcome to Count":"About Count");
        if (splash) //splash)
        {
            String[] first_step = {"Just close the splash screen", load_session.getText(), start_session.getText()};
            int selected = JOptionPane.showOptionDialog(getTopFrame(), getAboutComponent(), title, JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, getCountIcon(), first_step, first_step[0]);
            if (selected == 1)
            {
                doLoadSession();
            } else if (selected==2)
            {
                doStartSession();
            }
        } else
        {
            JOptionPane.showMessageDialog(getTopFrame(), getAboutComponent(), title, JOptionPane.INFORMATION_MESSAGE,
                    getCountIcon());

        }
    }

    public static ImageIcon getCountIcon()
    {
        java.net.URL icon_url = ClassLoader.getSystemResource("img/count-icon.jpg");
        try 
        {
            if (icon_url == null)
            {
                icon_url = new java.net.URL("http://www.iro.umontreal.ca/~csuros/gene_content/images/count-icon.jpg");
            }
        
            return new ImageIcon(icon_url);
        } catch (Exception E)
        {
            return null;
        }
    }
    
    
    private static String ABOUT_TEXT 
            = "<h1>The "+Executable.TITLE+" v"+Executable.VERSION+"</h1>" +
            "<p>The Count is a software package " +
            "for the evolutionary analysis of phylogenetic profiles, and numerical characters in general, " +
            "written entirely in Java. " +
            "</p>" +
            "<p>Author: Mikl&oacute;s Cs&#369;r&ouml;s http://www.iro.umontreal.ca/~csuros/</p> " +
            "<p>Some numerical optimization routines were adapted from " +
            "<em>Numerical Recipes in C: The Art of Scientific Computing</em> " +
            "[W. H. Press, S. A. Teukolsky, W. V. Vetterling and B. P. Flannery; " +
            "Second edition, Cambridge University Press, 1997].</p>" +
            "<p>Some code for MacOS X integration is based on Eirik Bjorsnos' Java package <tt>org.simplericity.macify.eawt</tt>, " +
            "distributed under the terms of the Apache License http://www.apache.org/licenses/LICENSE-2.0</p>" +
//            "<p>On systems other than Mac OS and Windows, the JGoodies Look&amp;Feel package is used http://www.jgoodies.com/</p>" +
            "<p>The background image (Moon and Mars) of the Count logo (batty batty bat batty bat batty bat batty bat...)" +
            "is used with permission from the copyright owner, John Harms." +
            "</p>"+
            "<p>Please <b>cite</b> Count as "+UsersGuide.COUNT_REFERENCE+"</p>" +
            "<p>Algorithmic ideas uderlying the Count package were described in the following publications.</p>" +
            "<ul>" +
            UsersGuide.METHOD_REFERENCES+
            "</ul>" +
            "<p>Montr&eacute;al/Budapest/Ann Arbor/Hong Kong/Nha Trang/Bengaluru/Amsterdam/Szentendre/Szeged, 2010-2016</p>";

    private JComponent getAboutComponent()
    {
        JEditorPane EP = new JEditorPane("text/html",ABOUT_TEXT);
        EP.setEditable(false);
        EP.setBackground(getTopFrame().getBackground());
        EP.setPreferredSize(new Dimension(600,600));
        JScrollPane ep_scroll = new JScrollPane(EP);
        return ep_scroll;
    }

    private void doLoadSession()
    {
        throw new UnsupportedOperationException();
    }
    
    private void doSaveSession()
    {
        throw new UnsupportedOperationException();
    }

    private DataFile<IndexedTree> loadPhylo()
    {
        FileDialog dialog = null;
        dialog = new FileDialog(this.getTopFrame(),"Open Newick-format phylogeny file",FileDialog.LOAD);
        dialog.setVisible(true);
       
        String file_name = dialog.getFile();
        String directory = dialog.getDirectory();

        Phylogeny main_phylo=null;
        
        if (file_name != null)
            for (int i=0; i<this.getWorkSpaceCount(); i++)
                if (file_name.equals(this.getWorkSpace(i).getSessionName()))
                {
                    handleException(new RuntimeException("Duplicate session names"), "Duplicate names", "Cannot have two open sessions with the same phylogeny file names");
                    file_name = null;
                    break;
                } 
        
        DataFile<IndexedTree> data_read = null;
        
        
        if (file_name != null)
        {
            try 
            {
                java.io.Reader R 
                = new java.io.InputStreamReader(new java.io.FileInputStream(directory+file_name)); // we shun FileReader for no particular reason
                main_phylo = Phylogeny.readNewick(R);
            } catch (java.io.InterruptedIOException E)
            {
                // load canceled
                main_phylo = null;
            } catch (NewickParser.ParseException E)
            {
                handleException(E, "Newick file parsing error", "Parsing error while reading Newick file "+directory+file_name+".");
            } catch (java.io.IOException E)
            {
                handleException(E, "I/O error", "File error while attempting ro read Newick file "+directory+file_name+".");  
            }
            
            if (main_phylo != null)
            {
                boolean all_zero = true;
                for (int j=0; j<main_phylo.getNumEdges() && all_zero; j++)
                    all_zero = (main_phylo.getLength(j)==0.);
                
                if (all_zero)
                {
                    for (int j=0; j<main_phylo.getNumEdges(); j++) // don't change root
                        main_phylo.getNode(j).setLength(1.0);
                }
                
                data_read = new DataFile<>(main_phylo, new File(directory,file_name));
            }        
        }
        
        return data_read;
    }
    
    private void doStartSession()
    {
        DataFile<IndexedTree> did_load = loadPhylo();
        if (did_load != null)
            addWorkSpace(new WorkSpace(this, did_load));
    }
    
    private void doLoadTree()
    {
        DataFile<IndexedTree> did_load = loadPhylo();
        if (did_load != null)
            getActiveWorkSpace().addTree(did_load);
    }
    
    private void doEditTree()
    {
        TreeEditDialog edit_dialog = new TreeEditDialog(this, "Edit tree");
        
        Dimension frameD = getTopFrame().getSize();

        edit_dialog.pack();
        edit_dialog.setBounds((int)(0.05*frameD.width),(int)(0.05*frameD.height),(int)(0.9*frameD.width),(int)(0.9*frameD.height));

        edit_dialog.setVisible(true);
        DataFile<IndexedTree> did_edit = edit_dialog.getEditedTree();
        if (did_edit != null)
        {
            // rename it
            TreeCollection all_trees = getActiveWorkSpace().getTreesBrowser();
            int rename_idx=0;
            String renamed_ident = Mien.createIdentifier(rename_idx);
            while (all_trees.getTreePanel(renamed_ident)!=null)
            {
                rename_idx++;
                renamed_ident = Mien.createIdentifier(rename_idx);
            }

            File renamed = new File(did_edit.getFile().getParent(), renamed_ident);
            did_edit = new DataFile<>(did_edit.getContent(), renamed);
            
            getActiveWorkSpace().addTree(did_edit);
        }
    }
    
    private void doLoadTable(boolean with_annotation_columns)
    {
        FileDialog dialog = null;
        String dialog_title = (with_annotation_columns?"Open annotated family size table":"Open family size table");
        dialog = new FileDialog(getTopFrame(),dialog_title,FileDialog.LOAD);
        dialog.setVisible(true);
       
        String file_name = dialog.getFile();
        String directory = dialog.getDirectory();

        OccurrenceTable ot = null;
        if (file_name != null)
        {
            ot = new OccurrenceTable(getActiveWorkSpace().getTerminalNames());
            try
            {
                FileInputStream F = new FileInputStream(directory+file_name);
                ot.readTable(new InputStreamReader(F), with_annotation_columns); 
                F.close();
                checkTableEmpty(ot);
                DataFile<OccurrenceTable> table_data = new DataFile<>(ot, new File(directory, file_name));
                
                getActiveWorkSpace().addDataSet(table_data);
            } catch (java.io.InterruptedIOException E)
            {
                // Cancel: nothing to do
                ot = null;
            } catch (java.io.IOException E)
            {
                ot = null;
                handleException(E, "I/O error", "File error while reading occurrence table from a file.");
            } catch (IllegalArgumentException E)
            {
                ot = null;
                handleException(E, "Parsing error", "Cannot parse occurrence table in file.");
            } catch (Exception E)
            {
                ot = null;
                handleException(E, "A bug!", "Error while reading occurrence table from a file.");
            }
        }
    }
    
    
    private void doLoadRates(Reader R)
    {
        File rates_file = null;
        if (R==null)
        {
            FileDialog dialog = null;
            dialog = new FileDialog(getTopFrame(),"Load rates",FileDialog.LOAD);
            dialog.setVisible(true);

            String file_name = dialog.getFile();
            String directory = dialog.getDirectory();
            if (file_name != null)
            {
                try 
                {
                    rates_file = new File(directory,file_name);
                    FileInputStream file_input = new FileInputStream(rates_file);
                    R = new InputStreamReader(file_input);
                } catch (java.io.FileNotFoundException E)
                {
                    handleException(E, "I/O error", "Cannot open rates file.");
                }
            }
        }
        if (R != null)
        {
            RateVariation rate_model=null;
            try 
            {
                IndexedTree main_tree = getActiveWorkSpace().getSelectedTreeData().getContent();
                rate_model = RateVariation.readRates(R, main_tree);
                
                R.close();
            } catch (java.io.InterruptedIOException E)
            {
                // canceled
                rate_model = null;
            } catch (RateVariation.FileFormatException E)
            {
                rate_model = null;
                handleException(E, "I/O error", "Badly formatted rates file.");
            }
            catch (java.io.IOException E)
            {
                rate_model = null;
                handleException(E, "I/O error", "File error while reading rates from a file.");
            } catch (Exception E)
            {
                rate_model = null;
                handleException(E, "A bug maybe?", "Error while reading rates from a file.");
            }
            if (rate_model != null)
            {
                DataFile<RateVariation> rates_data = new DataFile<>(rate_model, rates_file);
                getActiveWorkSpace().addRates(rates_data, true);
            } 
        } // file_name 
        
    }
    
    
    private void checkTableEmpty(OccurrenceTable ot)
    {
        String[] leaf_names = ot.getTaxonNames();
        int has_zero = 0;
        StringBuilder zero_taxa = null;
        boolean has_spaces = false;
        for (int leaf_idx=0; leaf_idx<leaf_names.length; leaf_idx++)
        {
            int p = ot.getNumFamiliesPresent(leaf_idx);
            if (p==0)
            {
                has_zero ++;
                has_spaces = has_spaces || (leaf_names[leaf_idx].indexOf(' ')!=-1);
                if (zero_taxa == null)
                {
                    zero_taxa = new StringBuilder();
                    zero_taxa.append("\"<tt>");
                    zero_taxa.append(leaf_names[leaf_idx]);
                    zero_taxa.append("</tt>\"");
                }
                else
                {
                    if (has_zero<4)
                    {
                        zero_taxa.append(", ");
                        zero_taxa.append("\"<tt>");
                        zero_taxa.append(leaf_names[leaf_idx]);
                        zero_taxa.append("</tt>\"");
                    }
                    else if (has_zero==4)
                        zero_taxa.append(",...");
                }
            }
        }
        if (has_zero!=0)
        {
            String msg = "<p><b>"+(has_zero==1?"Taxon ":"Taxa ")+zero_taxa.toString()+(has_zero==1?" has":" have");
            msg += " no members in any of the families.</b> <br />Maybe the names are misspelled in the tree.";
            if (has_spaces)
                msg += " <br />(Attention: in the Newick format, underscore is replaced by space unless the name is enclosed by quotation marks.)";
            msg += "</p>";
            JEditorPane EP = new JEditorPane("text/html",msg);
            EP.setEditable(false);
            JOptionPane.showMessageDialog(getTopFrame(),EP, "Missing data?", JOptionPane.WARNING_MESSAGE);
        }
    }
//            } // if ot!=null
//        } catch (java.io.InterruptedIOException E)
//        {
//            // Cancel: nothing to do
//            ot = null;
//        } catch (java.io.IOException E)
//        {
//            ot = null;
//            handleException(E, "I/O error", "File error while reading occurrence table from a file.");
//        } catch (IllegalArgumentException E)
//        {
//            ot = null;
//            handleException(E, "Parsing error", "Cannot parse occurrence table in file.");
//        } catch (Exception E)
//        {
//            ot = null;
//            handleException(E, "A bug!", "Error while reading occurrence table from a file.");
//        }
//        return ot;
//    }
    
 //test2
    public static void main(String[] args)
    {
//        Verbose.setVerbose(true);
        try
        {
            String system = System.getProperty("os.name", "[unknown OS]");

            if (system.startsWith("Mac OS"))
            {
                // MacOS X 10.4+ --- Java 1.5 runtime properties; no effect on other systems 
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", Executable.TITLE);
                System.setProperty("apple.awt.showGrowBox","true");
                //System.setProperty("apple.awt.brushMetalLook","true");
            } 
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception E) // ClassNotFoundException, InstantiationException
        {
            System.err.println("Ooops - no L&F");
            E.printStackTrace();
        }

        
        javax.swing.SwingUtilities.invokeLater(new Runnable() 
            {
                @Override
                public void run() 
                {
                    createAndShowGUI();
                }
            });
    }
    
    
    private static void createAndShowGUI()
    {
        JFrame top = new JFrame();

        top.setIconImage(getCountIcon().getImage());        

        top.setBounds(25,25,
            (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()-50,
            (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight()-50);
        
        top.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Count C=new Count(top);

//        // try full screen mode --- menu needs to be handled by us then 
//        java.awt.GraphicsDevice screen =
//            java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
//        if (screen.isFullScreenSupported())
//        {
//            screen.setFullScreenWindow(top);
//        }


        C.getTopFrame().setVisible(true);
//        { // on MacOS, this is trickier
//            java.awt.GraphicsDevice monitor = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
//            if (monitor.isFullScreenSupported())
//            {
//                monitor.setFullScreenWindow(top);
//            }
//        }
    }        
    
    
}
