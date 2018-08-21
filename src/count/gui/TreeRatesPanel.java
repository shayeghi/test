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

import count.gui.kit.DiscreteDistributionPlot;
import count.gui.kit.DrawString;
import count.gui.kit.IndexedPoint;
import count.gui.kit.LayeredBorderLayout;
import count.io.DataFile;
import count.matek.DiscreteDistribution;
import count.model.BirthDeathProcess;
import count.model.IndexedTree;
import count.model.ProbabilisticEvolutionModel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
//import count.model.IndexedTree;

/**
 *
 * A {@link TreePanel} for showing loss-gain-duplication rates.
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 */
public class TreeRatesPanel extends TreePanel
{
    
    private final DataFile<ProbabilisticEvolutionModel.BirthDeath> rates_data;
    
    public TreeRatesPanel(DataFile<ProbabilisticEvolutionModel.BirthDeath> rates_data)
    {
        super(new DataFile<>(rates_data.getContent().getPhylogeny()));
        
        this.rates_data = rates_data;
        this.root_distribution = getModel().getRootDistribution();
        this.dash  = new float[2]; dash[0]=2.0f; dash[1]=4.0f;
        this.dashed_stroke = new BasicStroke(2.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0f, dash, 0);
        this.solid_stroke = new BasicStroke(2.f);
        
        initGUIElements();
            
    }

    private double max_gain_display;
    private int[] max_gain_display_exact = null;
    private double max_duplication_display;
    private int[] max_duplication_display_exact = null;
    private double max_loss_display;
    private int[] max_loss_display_exact = null;
    
    private boolean draw_loss;
    private boolean draw_gain;
    private boolean draw_duplication;    

    private DiscreteDistribution root_distribution=null; 

    private final float[] dash;
    private final Stroke dashed_stroke;
    private final Stroke solid_stroke ;
    
    
    private void initGUIElements()
    {
        IndexedTree main_tree = getTree();
        for (int leaf_idx=0; leaf_idx<main_tree.getNumLeaves(); leaf_idx++)
        {
            NodeLabel leaf_label = setNodeLabel(leaf_idx, Mien.getShortNodeName(main_tree, leaf_idx), 0, 0, -Math.PI*0.4, 0f);
            leaf_label.setColor(Color.DARK_GRAY);
        }
        
        setPaddingForLegend(true);
        setRootStem(0); // we will have a window instead
        setBoundingBoxSeparation(getNormalFontSize()/2);
        
        setDrawLoss(true);
        setDrawGain(getModel().hasLineageSpecificGain() || getModel().getGainRate(0)>0.0);
        setDrawDuplication(getModel().hasLineageSpecificDuplication() || getModel().getDuplicationRate(0)>0.0);
    }

    
    @Override
    protected void computeNodeLabelBoundingBoxes(Graphics g)
    {
        if (g==null)
            super.computeNodeLabelBoundingBoxes(g);
        else
        {
            Graphics2D g2 = (Graphics2D)g.create();
            double r = getNodeRadius();
            IndexedTree tree = getTree();
            int font_size = getNormalFontSize();
            for (int node_idx=0; node_idx<tree.getNumNodes(); node_idx++)
            {
                Rectangle2D R = new Rectangle2D.Double(-r/2,-r/2,r+1, r+1);
                R.add(getDisplayedNodeLabel(node_idx).getBoundingBox(g2));
//                
//                
//                if (tree.isLeaf(leaf_idx))
//                {
//                    String label_text = Mien.getShortNodeName(tree, leaf_idx);
//                    Rectangle2D Rlab = DrawString.getBoundingBoxForRotatedString(g2,label_text, 0, 0,-Math.PI*0.4,0.f);
//                    R.add(Rlab);
//                } 
                if (tree.isRoot(node_idx))
                {
                    {
                        Rectangle2D Rplot = new Rectangle2D.Double(-Mien.RATE_TREE_ROOT_DISTRIBUTION_WIDTH/2, font_size, Mien.RATE_TREE_ROOT_DISTRIBUTION_WIDTH, Mien.DISTRIBUTION_PLOT_HEIGHT);
                        R.add(Rplot);
                    }
                }
                this.setNodeLabelBoundingBox(node_idx, R);
            }         
        }
    }
    
    private void setDrawLoss(boolean b)
    {
        this.draw_loss = b;
        initMaximumDisplayLengths();
        this.setValidBoundingBoxes(false);
    }
    private void setDrawGain(boolean b)
    {
        this.draw_gain = b;
        initMaximumDisplayLengths();
        this.setValidBoundingBoxes(false);
    }
    private void setDrawDuplication(boolean b)
    {
        this.draw_duplication = b;
        initMaximumDisplayLengths();
        this.setValidBoundingBoxes(false);
    }
    
    
    

    /**
     * Given an array of sorted edge lengths x[0]<=x[1]<=...<=x[n-1], 
     * finds an L and the corresponding 
     * quantiles x[0..t-1] and x[n-t..n-1] such that 
     * when the maximum solid edge length is L, 
     * only short edges (i<t) and long edges (i>=n-t)
     * are displayed with a relative solid edge length below epsilon.
     * 
     * @param sorted_values edge lenth values in increasing order
     * @param epsilon (0&lt;epsilon&lt;1 is assumed) the small fractional edge length below which there is no clear visual distinction in the display
     * @return appropriate setting for L, the maximum edge length that is displayed normally (may be 0 in some cases!)
     */
    private static double getDisplayLengthThreshold(double[] sorted_values, double epsilon)
    {
        int n = sorted_values.length;
        double f = epsilon; // epsilon*epsilon / (1.-epsilon);
        double retval = 9e99; // such a weird return value would indicate a problem
        for (int t=1; t-1<=n-t; t++)
        {
            double low = sorted_values[t-1]; // constraint L<= low/epsilon
            double high  = sorted_values[n-t]; // constraint L>=high*epsilon/(1-epsilon)
            if (low >= high*f)
            { // found a good range 
                retval = high; // Math.sqrt(low*high)/Math.sqrt(1.-epsilon);
                break;
            }
        }

        if (retval == 0.0)
        {
            for (int t=0; t<n; t++)
                if (sorted_values[t]!=0.0)
                {
                    retval = sorted_values[(t+n-1)/2];
                    break;
                }
        }

        //retval = 2.0*sorted_values[n/2];

        return retval;
    }
    
    
    private void initMaximumDisplayLengths()
    {
        int num_edges = getModel().getPhylogeny().getNumEdges();
        double[] gain_rate        = new double[num_edges];
        double[] duplication_rate = new double[num_edges];
        double[] loss_rate       = new double[num_edges];
        for (int edge_idx = 0; edge_idx<num_edges; edge_idx++)
        {
            double len = getModel().getEdgeLength(edge_idx);
            double gn = len * getModel().getGainRate(edge_idx);
            double dp = len * getModel().getDuplicationRate(edge_idx);
            double ls = len * getModel().getLossRate(edge_idx);
            gain_rate[edge_idx]        = gn;
            duplication_rate[edge_idx] = dp;
            loss_rate[edge_idx]        = ls;
            //System.out.println("#*RMD.ERTP.sMDL "+edge_idx+"/"+tree_nodes[edge_idx].newickName()+"\tl "+ls+"\tg "+gn+"\td "+dp);
        }
        java.util.Arrays.sort(gain_rate);
        java.util.Arrays.sort(loss_rate);
        java.util.Arrays.sort(duplication_rate);

        double epsilon = Mien.RATE_TREE_SHORT_EDGE;

        if (draw_gain)
        {
            max_gain_display = getDisplayLengthThreshold(gain_rate, epsilon );
            max_gain_display_exact = new int[2];
            max_gain_display = 2.0*Mien.roundToMostSignificantDigit(max_gain_display/2.0, max_gain_display_exact);
            max_gain_display_exact[0]*=2;
            if (max_gain_display_exact[0]>=10)
            {
                max_gain_display_exact[0]*=0.1;
                max_gain_display_exact[1]++;
            }
            if (max_gain_display == 0.0)
                System.out.println("#*RMD.ERTP.sMDL gd "+getDisplayLengthThreshold(gain_rate, epsilon )+"\tmax "+max_gain_display);
        }
        if (draw_duplication)
        {
            max_duplication_display = getDisplayLengthThreshold(duplication_rate, epsilon );
            max_duplication_display_exact = new int[2];
            max_duplication_display = 2.0*Mien.roundToMostSignificantDigit(max_duplication_display/2.0, max_duplication_display_exact);
            max_duplication_display_exact[0] *= 2;
            if (max_duplication_display_exact[0]>=10)
            {
                max_duplication_display_exact[0]*=0.1;
                max_duplication_display_exact[1]++;
            }
        }

        max_loss_display = getDisplayLengthThreshold(loss_rate, epsilon );
        max_loss_display_exact = new int[2];
        max_loss_display = 2.*Mien.roundToMostSignificantDigit(max_loss_display/2, max_loss_display_exact);
        max_loss_display_exact[0] *= 2;
        if (max_loss_display_exact[0]>=10)
        {
            max_loss_display_exact[0]*=0.1;
            max_loss_display_exact[1]++;
        }

        //System.out.println("#*RMD.ERTP.sMDL rounded\tmax_l "+max_loss_display+"\tmax_g "+max_gain_display+"\tmax_d "+max_duplication_display);

    }
    
    
    private IndexedTree getTree()
    {
        return super.getData().getContent();
    }
    
    private ProbabilisticEvolutionModel.BirthDeath getModel()
    {
        return rates_data.getContent();
    }
    
    /**
     * Length of the plot for each edge is at most for edge length 1.0  
     * 
     * @param node_idx child node
     * @return maximum edge length or this edge length, whichever is smaller
     */
    @Override
    public double getDisplayEdgeLength(int node_idx)
    {
        double lN = getNormalizedEdgeLength(node_idx);
        double retval = Math.max(Mien.RATE_TREE_SHORT_EDGE, Math.min(lN, 1.0));
        return retval;
    }
        
    /**
     * Computes the normalized edge length: maximum of rate/mr, where rate=loss, gain, duplication and mr 
     * is the corresponding max_rate_display
     * 
     * @param N one of the tree nodes
     * @return normalized edge length
     */
    private double getNormalizedEdgeLength(int node_idx)
    {
        double m = Double.NEGATIVE_INFINITY;
        double len = (draw_loss || draw_gain || draw_duplication)?getModel().getEdgeLength(node_idx):1.0;
        
        if (draw_loss)
        {
            double loss = len * getModel().getLossRate(node_idx);
            if (max_loss_display!=0.)
                loss /= max_loss_display;
            m = loss;
        }
        if (draw_duplication)
        {
            double duplication = len*getModel().getDuplicationRate(node_idx);
            if (max_duplication_display!=0.)
                duplication /= max_duplication_display;
            m = Math.max(m, duplication);
        }
        if (draw_gain)
        {
            double gain = len * getModel().getGainRate(node_idx);
            if (max_gain_display!=0.)
                gain /= max_gain_display;
            m = Math.max(m, gain);
        }
        if (Double.isInfinite(m))
            m = super.getDisplayEdgeLength(node_idx);
            
        return m;
    }
    
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        this.plotSelectedNodes(g);
    }
    
    
    @Override
    public void plotEdges(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        IndexedTree main_tree = getTree();
        ProbabilisticEvolutionModel.BirthDeath model = getModel();
        for (int node_idx=0; node_idx<main_tree.getNumNodes(); node_idx++)
        {
            IndexedPoint Npt = getDisplayedNodeLocation(node_idx);
            if (main_tree.isRoot(node_idx)) // a little stem
            {
                int nx = (int)(Npt.getX()+0.5);
                int ny = (int)(Npt.getY()+0.5);
                g2.setStroke(solid_stroke);
                g2.setColor(Mien.RATE_TREE_EDGE_COLOR); 
                g2.drawLine(nx,ny,nx,ny+Mien.getTreeNormalFontSize(getTreePointSize()));
            }
            else
            {
                int pidx = main_tree.getParentIndex(node_idx);
                int nx = (int)(Npt.getX()+0.5);
                double ny = Npt.getY();
                IndexedPoint Ppt = getDisplayedNodeLocation(pidx);
                int px = (int)(Ppt.getX()+0.5);
                double py = Ppt.getY();
                int py_i = (int)(py+0.5);

                int num_bars=0;
                if (draw_loss) num_bars++;
                if (draw_gain) num_bars++;
                if (draw_duplication) num_bars++;

                int bar_x1, bar_x2, bar_x3;
                if (num_bars==3)
                {
                    bar_x1=nx-4;
                    bar_x2=nx;
                    bar_x3=nx+4;
                } else if (num_bars==2) 
                {
                    bar_x1 = nx-2;
                    bar_x2 = bar_x3 = nx+2;
                } else 
                {
                    bar_x1=bar_x2=bar_x3=nx;
                }

                //int loss_x = nx - 4;
                //int gain_x = nx + 4;
                //int dup_x = nx;

                g2.setStroke(solid_stroke);
                g2.setColor(Mien.RATE_TREE_EDGE_COLOR);

                if (px<nx)
                {
                    g2.drawLine(px, py_i, bar_x3, py_i);
                } else
                {
                    g2.drawLine(px, py_i, bar_x1, py_i);
                }
                double length = model.getEdgeLength(node_idx);
                double loss = model.getLossRate(node_idx)*length;
                double gain = model.getGainRate(node_idx)*length;
                double dup = model.getDuplicationRate(node_idx)*length;

                if (max_loss_display != 0.0)
                    loss /= max_loss_display;
                if (draw_gain && max_gain_display != 0.0)
                    gain /= max_gain_display;
                if (draw_duplication && max_duplication_display != 0.0)
                    dup /= max_duplication_display;

                double dlength = getDisplayEdgeLength(node_idx);

                if (num_bars == 0)
                {
                    g2.setColor(Mien.RATE_TREE_EDGE_COLOR);
                    this.drawEdge(g2, dlength, dlength, nx, ny, py);
                } else
                {
                    int bar_idx = 0;
                    if (draw_loss)
                    {
                        bar_idx++;
                        g2.setColor(Mien.RATES_LOSS_COLOR);
                        this.drawEdge(g2, loss, dlength, bar_x1, ny, py);
                        //System.out.println("#*RMD.ERTP.pE "+leaf_idx+"\tloss "+loss+"\tdl "+dlength+"\tny "+ny+"\tpy "+py+"\t// node "+node_location[Nidx]+"\tparent "+node_location[Pidx]);
                    }
                    if (draw_gain)
                    {
                        g2.setColor(Mien.RATES_GAIN_COLOR);
                        this.drawEdge(g2, gain, dlength, bar_x3, ny, py);
                        //System.out.println("#*RMD.ERTP.pE "+leaf_idx+"\tgain "+gain+"\tdl "+dlength+"\tny "+ny+"\tpy "+py+"\t// node "+node_location[Nidx]+"\tparent "+node_location[Pidx]);
                    }
                    if (draw_duplication)
                    {
                        int bar_x = (bar_idx==0?bar_x1:bar_x2);
                        bar_idx++;
                        g2.setColor(Mien.RATES_DUPLICATION_COLOR);
                        this.drawEdge(g2, dup, dlength, bar_x, ny, py);
                        //System.out.println("#*RMD.ERTP.pE "+leaf_idx+"\tdup  "+dup+"\tdl "+dlength+"\tny "+ny+"\tpy "+py+"\t// node "+node_location[Nidx]+"\tparent "+node_location[Pidx]);
                    }
                }

            }
        } // for   
    }
    
    @Override
    protected void plotNodes(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        Font small_font = new Font("Serif",Font.PLAIN,Mien.getTreeNormalFontSize(getTreePointSize())*4/5);
        if (root_distribution != null)
        {
            IndexedPoint Rpt = getDisplayedNodeLocation(getTree().getNumNodes()-1); // root index = n-1
            int nx = (int)(Rpt.getX()+0.5);
            int ny = (int)(Rpt.getY()+0.5);

            DiscreteDistributionPlot root_plot = new DiscreteDistributionPlot(root_distribution, Mien.RATE_TREE_ROOT_DISTRIBUTION_WIDTH, Mien.RATE_TREE_ROOT_DISTRIBUTION_HEIGHT);
            root_plot.setBackground(Mien.DISTRIBUTION_PLOT_BACKGROUND);
            root_plot.setLegendColor(Color.BLACK);
            root_plot.setPlottingColor(Mien.MULTI_PRESENCE_COLOR);
            //root_plot.setRange(1, 10);
            g2.setFont(small_font);

            root_plot.paintIcon(this, g2, nx-Mien.RATE_TREE_ROOT_DISTRIBUTION_WIDTH/2, ny+Mien.getTreeNormalFontSize(getTreePointSize()));

//                System.out.println("#*"+getClass().getName()+".plotNodes root "+java.util.Arrays.toString(root_distribution.getParameters())+"\t// "+root_distribution);
        }

//
//        if (false){
//            for (int i=0; i<tree_nodes.length; i++)
//            {
//                NodeWithRates N = tree_nodes[i];
//                int Nidx = getDisplayNodeIndex(N);
//                double nx = displayed_node_location[Nidx].getX();
//                double ny = displayed_node_location[Nidx].getY();
//                Rectangle2D bb = this.node_label_bounding_box[Nidx];
//                g2.setColor(Color.MAGENTA);
//                g2.translate(nx,ny);
//                g2.draw(bb);
//                g2.translate(-nx,-ny);
//                g2.setColor(Color.BLACK);
//            }
//        }
    }    
    
    
    @Override
    protected void plotNodeNames(Graphics g)
    {
        
        Font leaf_font = new Font("Serif", Font.PLAIN, getLabelFontSize());
        Font inner_font = new Font("Serif", Font.ITALIC, getLabelFontSize());
        
        Graphics2D g2 = (Graphics2D) g.create();
        IndexedTree tree = getTree();
        
        for (int nidx=0; nidx<tree.getNumNodes(); nidx++)
        {
            if (!this.isSelected(nidx) || tree.isRoot(nidx))
            {
                Font label_font=(tree.isLeaf(nidx)?leaf_font:inner_font);
                g2.setFont(label_font);
                getDisplayedNodeLabel(nidx).draw(g2);
            }
        }
    }

    protected void plotSelectedNodes(Graphics g)
    {
        ListSelectionModel selection_model = getSelectionModel();
        if (!selection_model.isSelectionEmpty())
        {
            Graphics2D g2 = (Graphics2D) g.create();
            int small_font_size = Math.max(getLabelFontSize(), getNormalFontSize()); //*4/5;
            Font small_font = new Font("Serif",Font.PLAIN,small_font_size);
            Font small_title_font = small_font.deriveFont(Font.BOLD);//.deriveFont(Font.ITALIC);

            IndexedTree main_tree = getTree();
            
            int i0 = selection_model.getMinSelectionIndex();
            if (i0 != -1)
            {
                int i1 = selection_model.getMaxSelectionIndex();
                for (int idx=i0; idx<=i1; idx++)
                    if (selection_model.isSelectedIndex(idx))
                    {
                        double nx = getDisplayedNodeLocation(idx).getX();
                        double ny = getDisplayedNodeLocation(idx).getY();

                        //super.plotNode(g2, N);
                        g2.setColor(Color.RED);
                        g2.setStroke(solid_stroke);
                        double r = (int)(getNodeRadius()+0.5);
                        int d = (int)(2.*r+0.5);
                        
                        

                        if (main_tree.isRoot(idx))
                        {
                            g2.drawOval((int)(nx-r),(int)(ny-r),d,d);
                        } else
                        {
                            d=Math.max(d, 4*small_font_size);
                            
                            int dh = (int)(1.5*d);
                            int dw = dh*5/2;
                            int dsep = 5;

                            int title_width = g2.getFontMetrics(small_title_font).stringWidth(Mien.getLongNodeName(main_tree,idx)+"  ");
                            if (title_width>dw)
                                dw = title_width;

                            int dx = (int)nx;
                            int dy = (int)ny;
                            if (main_tree.isLeaf(idx) || true) // always put it below the node
                                dy += r+3;
                            else
                                dy -= r+2*dh+3*dsep;

                            if (dy<0) dy=0;
                            if (dx<dw/2+dsep) dx=dw/2+dsep;
                            if (dy+2*dh+3*dsep>getHeight())
                                dy=getHeight()-2*dh-3*dsep;
                            if (dx+dw/2+dsep>getWidth())
                                dx = getWidth()-dw/2-dsep;

                            DiscreteDistribution xdistr = BirthDeathProcess.getTransient0(getModel(), idx);
                            DiscreteDistribution pdistr = BirthDeathProcess.getTransient1(getModel(), idx);
                            DiscreteDistributionPlot pplot = new DiscreteDistributionPlot(pdistr, dw, dh);
                            Color plotbg =  Mien.TREE_PANEL_SELECTED_NODE_INFO_BACKGROUND;
                            Color plotc = Color.RED;
                            Color legendc = Color.BLACK;
                            g2.setColor(plotbg);

                            int[] triangle_x = new int[3];
                            int[] triangle_y = new int[3];
                            triangle_x[0] = (int)nx;
                            triangle_y[0] = (int)ny;
                            triangle_x[1] = dx-dw/2+dw+dsep;
                            triangle_y[1] = dy-dsep;
                            triangle_x[2] = dx-dw/2;
                            triangle_y[2] = triangle_y[1];
                            g2.fillPolygon(triangle_x, triangle_y, 3);

                            g2.fillRect(dx-dw/2-dsep, dy-dsep, dw+2*dsep, 2*dh+3*dsep);
                            g2.setColor(Color.RED);
                            int pici_r = 4;
                            g2.fillOval((int)nx-pici_r, (int)ny-pici_r, 2*pici_r, 2*pici_r);

                            {   // draw node name
                                g2.setColor(legendc);
                                g2.setFont(small_title_font);
                                DrawString.drawCenteredString(g2, Mien.getLongNodeName(main_tree, idx), dx, dy);
                            }

                            pplot.setBackground(null);
                            pplot.setLegendColor(legendc);
                            pplot.setPlottingColor(plotc);
                            g2.setFont(small_font);
                            pplot.paintIcon(this, g2, dx-dw/2, dy);
                            g2.setColor(legendc);
                            g2.setFont(small_font.deriveFont(Font.BOLD));
                            g2.drawString("Inparalogs", dx-dw/2+6, dy+small_font_size);
                            DiscreteDistributionPlot xplot = new DiscreteDistributionPlot(xdistr, dw, dh);
                            xplot.setBackground(null);
                            xplot.setLegendColor(legendc);
                            xplot.setPlottingColor(plotc);
                            g2.setFont(small_font);
                            xplot.paintIcon(this, g2, dx-dw/2, dy+dh+dsep);
                            g2.setColor(legendc);
                            g2.setFont(small_font.deriveFont(Font.BOLD));
                            g2.drawString("Xenologs", dx-dw/2+6, dy+dh+dsep+small_font_size);
                        }
                    } // if selected
            }
        }
    }
    
//    @Override
//    protected void plotNodeNames(Graphics g)
//    {
//        Graphics2D g2 = (Graphics2D) g.create();
//        Font label_font = new Font("Serif",Font.PLAIN,getLabelFontSize());;
//
//        IndexedTree main_tree =  getTree(); 
//        
//        int leaf_idx=0; 
//        while (leaf_idx < main_tree.getNumLeaves())
//        {
//            IndexedPoint Npt = getDisplayedNodeLocation(leaf_idx);
//            this.drawNodeLabelRotatedLeft(g2, Mien.getShortNodeName(main_tree, leaf_idx), Npt, 0, 0);
////
////            double nx = Npt.getX();
////            double ny = Npt.getY();
////
////            int label_x = (int)(nx+0.5);
////            int label_y = (int) (ny+0.5);
////
////            g2.setFont(label_font);
////            g2.setColor(Color.DARK_GRAY);
////            String label_text = Mien.getShortNodeName(main_tree,leaf_idx);
////            DrawString.drawRotatedStringLeft(g2,label_text, label_x, label_y,-Math.PI*0.4);                    
//            
//            ++leaf_idx;
//        }
//        while(leaf_idx<main_tree.getNumNodes())
//        {
//            super.plotNodeLabel(g,leaf_idx);        
//            ++leaf_idx;
//        }
//    }
//    
    
    /**
     * Sets magnification level (and adjusts bounding box separation accordingly)
     * 
     * @param mag new magnification factor
     */
    @Override
    public void setMagnification(double mag)
    {
        int sep = (int)(mag*6.0+0.5);
        if (sep<1) sep=1;
        if (sep>20) sep=20;
        setBoundingBoxSeparation(sep); 
        super.setMagnification(mag);
    }    
    
    /*
     * Draws the vertical part for an edge: long edges have a proportional 
     * dashed region. The plotted length of the edge is determined by the difference 
     * between the end vertices' coordinates.
     * 
     * @param g2 graphics context: stroke may be altered on return 
     * @param value the edge length to be displayed
     * @param dlength maximum edge length displayed with solid stroke
     * @param x_i position of the edge
     * @param ny child's vertical coordinate
     * @param py parent's vertical coordinate
     */ 
    public final void drawEdge(Graphics2D g2, double value, double dlength, int x_i, double ny, double py)
    {
        TreeRatesPanel.drawEdge(g2, solid_stroke, dashed_stroke, value, dlength, x_i, ny, py);
    }
    
    /*
     * Draws the vertical part for an edge: long edges have a proportional 
     * dashed region. The plotted length of the edge is determined by the difference 
     * between the end vertices' coordinates.
     * 
     * @param g2 graphics context: stroke may be altered on return 
     * @param solid_stroke stroke used when edge length is below the maximum display edge length
     * @param dashed_stroke stroke used for part of long edges
     * @param value the edge length to be displayed
     * @param dlength maximum edge length displayed with solid stroke
     * @param x_i position of the edge
     * @param ny child's vertical coordinate
     * @param py parent's vertical coordinate
     */ 
    public static void drawEdge(Graphics2D g2, Stroke solid_stroke, Stroke dashed_stroke, double value, double dlength, int x_i, double ny, double py)
    {
        //int x_i = (int)(x+0.5);
        int py_i = (int)(py+0.5);

        if (value>dlength)
        {
            // long edge
            double solid_fraction = dlength/value;
            double my = (ny+(py-ny)*solid_fraction);
            int my_i = (int)(my+0.5);
            int ny_i = (int)(ny+0.5);
            g2.setStroke(solid_stroke);
            g2.drawLine(x_i, ny_i, x_i, my_i);
            g2.setStroke(dashed_stroke);
            g2.drawLine(x_i, my_i, x_i, py_i);
        } else
        {
            double ey = py + (ny-py) * value / dlength;
            int ey_i = (int)(ey+0.5);
            g2.setStroke(solid_stroke);
            g2.drawLine(x_i, py_i, x_i, ey_i);
        }
    }        
    

    private void setPaddingForLegend(boolean legend_shown)
    {
        int legend_padding = 10+(int)(3.*getNodeRadius());
        if (legend_shown)
            legend_padding += getPreferredRateLegendDimension().width;
        setPadding(new java.awt.Insets(10,legend_padding,10,10));        
    }
    
    private Dimension getPreferredRateLegendDimension()
    {
        int f = getNormalFontSize(); // line height for text
        int d = Mien.TREE_LEGEND_INSET; // separation between elements
        int lw = getNormalFontSize() * 5/3; // width of individual bars
        int bar_height = Mien.TREE_LEGEND_BAR_HEIGHT; // max. height of the bars
        int w = lw*4+d*5; // total width
        int num_rates = 1; // loss
        ProbabilisticEvolutionModel.BirthDeath model = getModel();
        if (model.hasLineageSpecificDuplication() || model.getDuplicationRate(0)>0.0)
            num_rates++;
        if (model.hasLineageSpecificGain() || model.getGainRate(0)>0.0)
            num_rates++;
        
        int h = num_rates*(2*d+2*f+bar_height)+d; // total height
        Dimension D = new Dimension (w,h);
        return D;
    }
    
    public static class WithLegend extends JLayeredPane
    {
        
//        private final TreeRatesPanel rates_panel; 
        private final TreePanel.Zoomed<TreeRatesPanel> tree_zoom;
        private final RateLegend rate_legend;
        
        public WithLegend(TreeRatesPanel panel)
        {
            super();
            this.tree_zoom = new TreePanel.Zoomed<>(panel);
            this.rate_legend = panel.new RateLegend();
            
            initGUIElements();
        }
        
        private void initGUIElements()
        {
            LayeredBorderLayout layers_layout = new LayeredBorderLayout();
            setLayout(layers_layout);
            layers_layout.addLayoutComponent(LayeredBorderLayout.CENTER, tree_zoom);
            this.add(tree_zoom, JLayeredPane.DEFAULT_LAYER);

            layers_layout.addLayoutComponent(LayeredBorderLayout.WEST, rate_legend);
            this.add(rate_legend, JLayeredPane.PALETTE_LAYER);
            
            Box controll_bar = tree_zoom.getControllBar();
            
            controll_bar.removeAll();
            controll_bar.add(createLegendShowCB());
            controll_bar.add(createLegendShowRates());
            
            controll_bar.add(Box.createHorizontalGlue());
            controll_bar.add(tree_zoom.getTreePanel().createMagnificationSpinner());
        }

        public JCheckBox createLegendShowCB()
        {
            Font bb_font = new Font("Serif", Font.PLAIN, tree_zoom.getTreePanel().getNormalFontSize());
            
            JCheckBox legendB = new JCheckBox("Legend");
            legendB.setFont(bb_font);
            legendB.setSelected(true);
            
            legendB.addItemListener(new ItemListener()
                {
                    @Override
                    public void itemStateChanged(ItemEvent E)
                    {
                        int ch = E.getStateChange();

                        if (ch == ItemEvent.DESELECTED)
                        {
                            tree_zoom.getTreePanel().setPaddingForLegend(false);
                            rate_legend.setVisible(false);
                        }
                        else if (ch==ItemEvent.SELECTED)
                        {
                            tree_zoom.getTreePanel().setPaddingForLegend(true);
                            rate_legend.setVisible(true);
                        }
                        tree_zoom.repaint();
                    }
                });
            
            return legendB;
        }
        
        public Box createLegendShowRates()
        {
            Box bb= Box.createHorizontalBox();
            
            Font bb_font = new Font("Serif", Font.PLAIN, tree_zoom.getTreePanel().getNormalFontSize());

            final TreeRatesPanel rate_panel = tree_zoom.getTreePanel();
            ProbabilisticEvolutionModel.BirthDeath model = rate_panel.getModel();
            
            {
                JCheckBox lossB = new JCheckBox("Loss");
                lossB.setBackground(Mien.RATES_LOSS_COLOR);
                lossB.setFont(bb_font);
                lossB.setSelected(true);
                lossB.addItemListener(new ItemListener()
                    {
                        @Override
                        public void itemStateChanged(ItemEvent E)
                        {
                            int ch = E.getStateChange();
                            if (ch == ItemEvent.DESELECTED)
                                rate_panel.setDrawLoss(false);
                            else if (ch==ItemEvent.SELECTED)
                                rate_panel.setDrawLoss(true);
                            rate_panel.repaint();
                        }
                    });

                bb.add(lossB);
            }
            
            if (model.hasLineageSpecificDuplication() || model.getDuplicationRate(0)>0.0) // has duplication
            {
                JCheckBox dupB = new JCheckBox("Duplication");
                dupB.setBackground(Mien.RATES_DUPLICATION_COLOR);
                dupB.setFont(bb_font);
                dupB.setSelected(true);
                dupB.addItemListener(new ItemListener()
                    {
                        @Override
                        public void itemStateChanged(ItemEvent E)
                        {
                            int ch = E.getStateChange();
                            if (ch == ItemEvent.DESELECTED)
                                rate_panel.setDrawDuplication(false);
                            else if (ch==ItemEvent.SELECTED)
                                rate_panel.setDrawDuplication(true);
                            rate_panel.repaint();
                        }
                    });                
                bb.add(dupB);
            }
            
            if (model.hasLineageSpecificGain() || model.getGainRate(0)>0.0) // has gain
            {
                JCheckBox gainB = new JCheckBox("Gain");
                gainB.setBackground(Mien.RATES_GAIN_COLOR);
                gainB.setFont(bb_font);
                gainB.setSelected(true);
                gainB.addItemListener(new ItemListener()
                    {
                        @Override
                        public void itemStateChanged(ItemEvent E)
                        {
                            int ch = E.getStateChange();
                            if (ch == ItemEvent.DESELECTED)
                                rate_panel.setDrawGain(false);
                            else if (ch==ItemEvent.SELECTED)
                                rate_panel.setDrawGain(true);
                            rate_panel.repaint();
                        }
                    });
                bb.add(gainB);
            }
            
            return bb;
        }
        
        
    }
    
    
    /**
     * A panel illustrating the scale for the tree edges
     */
    private class RateLegend extends JPanel implements PropertyChangeListener
    {
        RateLegend()
        {
            super();
            user_preferred = null;
            initGUIElements();
        }
        
        private Dimension user_preferred;

        private void initGUIElements()
        {
            setBackground(Mien.SMOKY_BACKGROUND);
            setBorder(javax.swing.BorderFactory.createRaisedBevelBorder());
        }
        
        @Override
        public void propertyChange(PropertyChangeEvent E)
        {
            if (TreePanel.MAGNIFICATION_PROPERTY.equals(E.getPropertyName())
                    && E.getSource() instanceof TreePanel)
            {
                // our kind of event!
                double old_mag = ((Double)E.getOldValue()).doubleValue();
                double new_mag = ((Double)E.getNewValue()).doubleValue();
                if (old_mag != new_mag)
                {
                    revalidate();
                    repaint();
                }
            }
        }
        
        @Override
        public void paintComponent(Graphics g)
        {
            //System.out.println("#*RMD.RL.pC start");
            super.paintComponent(g);
            
            int num_rates = 0; // loss
            if (TreeRatesPanel.this.draw_loss)
                num_rates++;
            if (TreeRatesPanel.this.draw_gain)
                num_rates++;
            if (TreeRatesPanel.this.draw_duplication)
                num_rates++;
            
            if (num_rates>0)
            {
                Graphics2D g2 = (Graphics2D) g.create();

                int w = getWidth();
                int h = getHeight();


                int font_size = Mien.getTreeNormalFontSize(getTreePointSize())*4/5;
                Font legend_font = new Font("Serif", Font.PLAIN, font_size);
                Font legend_title_font = legend_font.deriveFont(Font.BOLD);
                int skip = Mien.TREE_LEGEND_INSET; // this is how much space is left between elements
                int plot_height = (h - skip)/num_rates;            
                int bar_height = plot_height - 3*skip - 2*font_size;  
                int bar_width = (w-2*skip)/4;
                double scale_y = Math.abs(TreeRatesPanel.this.getDisplayTransform().getScaleY());

                int legend_y = skip;
                for (int rate_idx=0; rate_idx<3; rate_idx++)
                {
                    String legend_subtitle; // = "";
                    double max_display;
                    int[] max_exact;
                    Color rate_color;

                    boolean rate_shown; // = false;


                    if (rate_idx==0)
                    { // loss
                        legend_subtitle = "Loss rates";
                        max_display = TreeRatesPanel.this.max_loss_display;
                        max_exact = TreeRatesPanel.this.max_loss_display_exact;
                        rate_color = Mien.RATES_LOSS_COLOR;
                        rate_shown= TreeRatesPanel.this.draw_loss;
                    } else if (rate_idx==1)
                    {
                        legend_subtitle = "Duplication rates";
                        max_display = TreeRatesPanel.this.max_duplication_display;
                        max_exact = TreeRatesPanel.this.max_duplication_display_exact;
                        rate_color = Mien.RATES_DUPLICATION_COLOR;
                        rate_shown= TreeRatesPanel.this.draw_duplication;
                    } else 
                    {
                        legend_subtitle = "Gain rates";
                        max_display = TreeRatesPanel.this.max_gain_display;
                        max_exact = TreeRatesPanel.this.max_gain_display_exact;
                        rate_color = Mien.RATES_GAIN_COLOR;
                        rate_shown= TreeRatesPanel.this.draw_gain;
                    }

                    if (rate_shown)
                    {
                        g2.setFont(legend_title_font);
                        g2.drawString(legend_subtitle, skip, legend_y+font_size);
                        g2.setFont(legend_font);

                        double rate_ref = bar_height / scale_y;
                        if (rate_ref>=1.0)
                        {
                            double level0 = legend_y+font_size+skip+bar_height;
                            double level1 = level0 - scale_y;
                            g2.setColor(rate_color);
                            drawLegend(g2, font_size+skip, skip+bar_width/2, level0, level1, 0.5, max_exact);
                            g2.setColor(rate_color);
                            drawLegend(g2, font_size+skip, skip+3*bar_width/2, level0, level1, 1.0, max_exact);
                            g2.setColor(rate_color);
                            drawLegend(g2, font_size+skip, skip+5*bar_width/2, level0, level1, 2.0, max_exact);
                            g2.setColor(rate_color);
                            drawLegend(g2, font_size+skip, skip+7*bar_width/2, level0, level1, 4.0, max_exact);
                        } else
                        {
                            // we can accomodate a bar corresponding to a displayed length of rate_ref at most
                            double max_legend_length = rate_ref * max_display; // true length corresponding to rate_ref
                            int [] rounded_half_exact = new int[2];
                            double rounded_half = Mien.roundToMostSignificantDigit(max_legend_length/2.0, rounded_half_exact, true);
                            double rounded_half_rel = rounded_half/max_display;
                            double level0 = legend_y+font_size+skip+bar_height;
                            double level1 = level0 - 2.0*rounded_half_rel * scale_y;
                            rounded_half_exact[0] *= 2.0;

                            //System.out.println("#*RMD.RL.pC rate "+rate_idx+"\tbh "+bar_height+"\tref "+rate_ref
                            //        +"\tmax "+max_display+"\tscale "+scale_y+"\tmll "+max_legend_length
                            //        +"\trh "+rounded_half+"\tl0 "+level0+"\tl1 "+level1+"// "+legend_subtitle);

                            g2.setColor(rate_color);
                            drawLegend(g2, font_size+skip, skip+bar_width/2, level0, level1, 0.25, rounded_half_exact);
                            g2.setColor(rate_color);
                            drawLegend(g2, font_size+skip, skip+3*bar_width/2, level0, level1, 0.5, rounded_half_exact);
                            g2.setColor(rate_color);
                            drawLegend(g2, font_size+skip, skip+5*bar_width/2, level0, level1, 0.75, rounded_half_exact);
                            g2.setColor(rate_color);
                            drawLegend(g2, font_size+skip, skip+7*bar_width/2, level0, level1, 1.0, rounded_half_exact);
                        }
                        legend_y += plot_height;
                    } else
                    {
                        //g2.setFont(legend_title_font);
                        //g2.drawString("["+legend_subtitle+"]", skip, legend_y+font_size);
                    }
                }
            }
            
            //g2.drawString(Double.toString(tree_panel.getDisplayTransform().getScaleY()), 2, 20);
        }

        private void drawLegend(Graphics2D g2, double y_unit, int x, double level0, double level1, double factor, int[] scale)
        {
            TreeRatesPanel.this.drawEdge(g2, factor, 1.0, x, level1, level0);
            g2.setColor(Color.BLACK);
            DrawString.drawCenteredString(g2, getScaledValueDisplay(factor, scale), x, (int)(level0+y_unit+0.5));
        }
        
        private String getScaledValueDisplay(double scaling_factor, final int[] scale)
        {
            double mantissa = scaling_factor*scale[0];
            int exponent = scale[1];
            if (mantissa<0.1)
            {
                mantissa *= 10.0;
                exponent--;
            } else if (mantissa>=10.0)
            {
                mantissa /= 10.0;
                exponent++;
            }
            // check if we can get away without scientific notation
            if (exponent<=0)
            {
                if (exponent>=-2)
                    while (exponent<0){exponent++; mantissa*=0.1;}
            } else if (exponent<=2)
                while (exponent>0){exponent--; mantissa*=10.0;}
            
            // get rid of floating-point errors
            //mantissa = ((int)(mantissa*1000000.0+0.5))/1000000.0;
            
            String retval;// = "";
            if (mantissa == (int)mantissa)
            {
                retval = Integer.toString((int)mantissa);
            } else
            {
                retval = Float.toString((float)mantissa);
            }
            if (exponent!=0)
            {
                retval = retval + "e" + Integer.toString(exponent);
            }
            //System.out.println("#*RMD.RL.gSVD factor "+scaling_factor+"\tretval `"+retval+"'\tm "+mantissa+"\te "+exponent+"\tsc "+scale[0]+"\t"+scale[1]);
            
            return retval;
        }

       /**
         * Sets a user-preferred size, which overrules the locally computed values.
         * Reset to default by calling with null argument.
         */
        @Override
        public void setPreferredSize(Dimension size)
        {
            this.user_preferred = size;
            //System.out.println("#**ISD.sPS "+size);
        }
        

        @Override
        public Dimension getPreferredSize()
        {
            Dimension D = user_preferred;
            if (D ==null)
            {
                D = TreeRatesPanel.this.getPreferredRateLegendDimension();
            }
            //System.out.println("#*RMD.RL.gPS "+D+"\t"+tree_panel.getDisplayTransform().getScaleY());
            return D;
        }
        
        @Override
        public Dimension getMinimumSize()
        {
            return getPreferredSize();
        }        
    }
    
    
    
}
