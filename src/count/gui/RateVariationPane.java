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

import count.gui.kit.DiscreteGammaPlot;
import count.io.DataFile;
import count.matek.DiscreteGamma;
import count.model.RateVariation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * A component with split panes to show {@link RateVariation} instance. 
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 */
public class RateVariationPane extends Browser.PrimaryItem
{
    private final DataFile<RateVariation> model_data;
    
    public RateVariationPane(DataFile<RateVariation> model_data)
    {
        super();
        
        this.model_data = model_data;
        initGUIElements();
    }
    
    private void initGUIElements()
    {
        JSplitPane main_pane = new JSplitPane();
        // horizontal split: tree at bottom
        this.setBorder(null);
        setBackground(Color.WHITE);
        
        main_pane.setBackground(getBackground());
        main_pane.setBorder(null);
        main_pane.setDividerLocation(300+getInsets().top);
        main_pane.setResizeWeight(0.5);
        main_pane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        main_pane.setOneTouchExpandable(true);
        
        JTable rates_table = new TreeRatesTable(model_data.getContent().getMainTree());
        JScrollPane table_scroll = new JScrollPane(rates_table);
        table_scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        table_scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        table_scroll.getViewport().setBackground(getBackground());
        
        JPanel rate_multiplier_panel = new RateMultiplierPanel();
        rate_multiplier_panel.setBackground(getBackground());
        JScrollPane rate_variation_scroll = new JScrollPane(rate_multiplier_panel);
        rate_variation_scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        rate_variation_scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        
        JSplitPane top_pane = new JSplitPane();
        top_pane.setBorder(null);
        top_pane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        top_pane.setLeftComponent(table_scroll);
        top_pane.setRightComponent(rate_variation_scroll);
        top_pane.setResizeWeight(0.1);
        top_pane.setOneTouchExpandable(true);
        
        main_pane.setTopComponent(top_pane);

        TreeRatesPanel tree_panel = new TreeRatesPanel(new DataFile<>(model_data.getContent().getMainTree()));
        
        // same selection model for tree and table
        ListSelectionModel selection_model = tree_panel.getSelectionModel();
        rates_table.setSelectionModel(selection_model);
        selection_model.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                if (!e.getValueIsAdjusting())
                {
                    int row_idx = selection_model.getLeadSelectionIndex();
                    if (row_idx != -1)
                        rates_table.scrollRectToVisible(rates_table.getCellRect(row_idx, 0, true));
                }
            }
        });
        
        TreeRatesPanel.WithLegend layers = new TreeRatesPanel.WithLegend(tree_panel);
        
        
        
        main_pane.setBottomComponent(layers);
        
        BorderLayout layout = new BorderLayout();
        this.setLayout(layout);
        
        this.add(main_pane, BorderLayout.CENTER);
    }
    
    private class RateMultiplierPanel extends JPanel
    {
        private final Font normal_font = new Font("Serif",Font.PLAIN,Mien.TABLE_FONT_SIZE);
        private final Font title_font  = new Font("Serif", Font.BOLD, Mien.TABLE_FONT_SIZE*6/5);
        private final Font subtitle_font = normal_font.deriveFont(Font.BOLD);
        private final Font it_font = normal_font.deriveFont(Font.ITALIC);
        private final Font plot_font = new Font("Serif", Font.PLAIN, Mien.TABLE_FONT_SIZE*4/5);

        private RateMultiplierPanel()
        {
            super();
            initComponents();
        }
        
        private void initComponents()
        {
            setBackground(Color.WHITE);
        }   
        
        @Override 
        public Dimension getPreferredSize()
        {
            int w = Mien.RATE_VARIATION_PLOT_WIDTH*3/2 + 2*Mien.RATE_VARIATION_PADDING;
            int h = Mien.TABLE_FONT_SIZE*6/5+2*Mien.RATE_VARIATION_PADDING+4*(Mien.RATE_VARIATION_PADDING+Mien.RATE_VARIATION_PLOT_HEIGHT);
            return new Dimension(w,h);
        }
        
        @Override 
        public Dimension getMinimumSize()
        {
            return getPreferredSize();
        }
        
        
        private void paintRateMultipliers(Graphics2D g2, double alpha, int n, double p0, int current_y, Color color, String subtitle, int subtitle_length, String forbidden_label)
        {
            g2.setFont(subtitle_font);
            g2.setColor(color);
            g2.drawString(subtitle, Mien.RATE_VARIATION_PADDING, current_y+subtitle_font.getSize());
            if (n==1)
            {
                int txt_x = Mien.RATE_VARIATION_PADDING+subtitle_length+Mien.RATE_VARIATION_PADDING;
                int txt_y = current_y+subtitle_font.getSize();
                if (p0==0.0)
                {
                    g2.setFont(it_font);
                    g2.drawString("no variation", txt_x, txt_y);
                } else
                {
                    g2.setFont(plot_font);
                    g2.drawString(forbidden_label+Double.toString(p0), txt_x, txt_y);
                }
            } else
            {
                DiscreteGammaPlot P_duplication = new DiscreteGammaPlot(new DiscreteGamma(alpha), n, color);
                g2.setFont(plot_font);
                P_duplication.paintIcon(this, g2, Mien.RATE_VARIATION_PADDING+subtitle_length+Mien.RATE_VARIATION_PADDING, current_y);
                if (p0!=0.0)
                {
                    int txt_x = Mien.RATE_VARIATION_PADDING+subtitle_length+Mien.RATE_VARIATION_PADDING+Mien.RATE_VARIATION_PLOT_WIDTH+Mien.RATE_VARIATION_PADDING;
                    int txt_y = current_y+subtitle_font.getSize();
                    g2.setFont(plot_font);
                    g2.drawString(forbidden_label+Double.toString(p0), txt_x, txt_y);
                }
            }            
        }

        @Override
        public void paintComponent(Graphics g)
        {
            RateVariation rates = model_data.getContent();

            Graphics2D g2 = (Graphics2D) g.create();
            
            int current_y = title_font.getSize()+Mien.RATE_VARIATION_PADDING;
            g2.setFont(title_font);
            g2.drawString("Rate variation across families", Mien.RATE_VARIATION_PADDING, current_y);
            
            int subtitle_length = g2.getFontMetrics(subtitle_font).stringWidth("Duplication rate:"); // longest subtitle 
            current_y+=Mien.RATE_VARIATION_PADDING;
            this.paintRateMultipliers(g2, rates.getAlphaEdgeLength(), rates.getNumGammaCategoriesEdgeLength(), 0.0, current_y, Mien.RATES_EDGELENGTH_COLOR, "Edge length:", subtitle_length, "");
            current_y += Mien.RATE_VARIATION_PLOT_HEIGHT;
            current_y += Mien.RATE_VARIATION_PADDING;
            this.paintRateMultipliers(g2, rates.getAlphaLoss(), rates.getNumGammaCategoriesLoss(), rates.getForbiddenLoss(), current_y, Mien.RATES_LOSS_COLOR, "Loss rates:", subtitle_length, "P(no-loss)=");
            current_y += Mien.RATE_VARIATION_PLOT_HEIGHT;
            current_y += Mien.RATE_VARIATION_PADDING;
            this.paintRateMultipliers(g2, rates.getAlphaDuplication(), rates.getNumGammaCategoriesDuplication(), rates.getForbiddenDuplication(), current_y, Mien.RATES_DUPLICATION_COLOR, "Duplication rates:", subtitle_length, "P(no-duplication)=");
            current_y += Mien.RATE_VARIATION_PLOT_HEIGHT;
            current_y += Mien.RATE_VARIATION_PADDING;
            this.paintRateMultipliers(g2, rates.getAlphaGain(), rates.getNumGammaCategoriesGain(), rates.getForbiddenGain(), current_y, Mien.RATES_GAIN_COLOR, "Gain rates:", subtitle_length, "P(no-gain)=");
        } // paintComponent

//            {
//                current_y+=Mien.RATE_VARIATION_PADDING;
//                g2.setFont(subtitle_font);
//                g2.setColor(Mien.RATES_EDGELENGTH_COLOR);
//                g2.drawString("Edge length:", Mien.RATE_VARIATION_PADDING, current_y+subtitle_font.getSize());
//                int n = rates.getNumGammaCategoriesEdgeLength();
//                if (n==1)
//                {
//                    g2.setFont(it_font);
//                    g2.drawString("no variation", Mien.RATE_VARIATION_PADDING+subtitle_length+Mien.RATE_VARIATION_PADDING, current_y+subtitle_font.getSize());
//                } else
//                {
//                    DiscreteGammaPlot P_edge_length = new DiscreteGammaPlot(new DiscreteGamma(rates.getAlphaEdgeLength()), n, Mien.RATES_EDGELENGTH_COLOR);
//                    g2.setFont(plot_font);
//                    P_edge_length.paintIcon(this, g2, Mien.RATE_VARIATION_PADDING+subtitle_length+Mien.RATE_VARIATION_PADDING, current_y);
//                }
//                current_y += Mien.RATE_VARIATION_PLOT_HEIGHT;
//            }
//            {
//                current_y+=Mien.RATE_VARIATION_PADDING;
//                g2.setFont(subtitle_font);
//                g2.setColor(Mien.RATES_LOSS_COLOR);
//                g2.drawString("Loss rates:", Mien.RATE_VARIATION_PADDING, current_y+subtitle_font.getSize());
//                int n = rates.getNumGammaCategoriesLoss();
//                if (n==1)
//                {
//                    g2.setFont(it_font);
//                    g2.drawString("no variation", Mien.RATE_VARIATION_PADDING+subtitle_length+Mien.RATE_VARIATION_PADDING, current_y+subtitle_font.getSize());
//                } else
//                {
//                    DiscreteGammaPlot P_loss = new DiscreteGammaPlot(new DiscreteGamma(rates.getAlphaLoss()), n, Mien.RATES_LOSS_COLOR);
//                    g2.setFont(plot_font);
//                    P_loss.paintIcon(this, g2, Mien.RATE_VARIATION_PADDING+subtitle_length+Mien.RATE_VARIATION_PADDING, current_y);
//                }
//                current_y += Mien.RATE_VARIATION_PLOT_HEIGHT;
//            }
//            {
//                current_y+=Mien.RATE_VARIATION_PADDING;
//                g2.setFont(subtitle_font);
//                g2.setColor(Mien.RATES_DUPLICATION_COLOR);
//                g2.drawString("Duplication rates:", Mien.RATE_VARIATION_PADDING, current_y+subtitle_font.getSize());
//                int n = rates.getNumGammaCategoriesDuplication();
//                if (n==1)
//                {
//                    double p0 = rates.getForbiddenDuplication();
//                    int txt_x = Mien.RATE_VARIATION_PADDING+subtitle_length+Mien.RATE_VARIATION_PADDING;
//                    int txt_y = current_y+subtitle_font.getSize();
//                    if (p0==0.0)
//                    {
//                        g2.setFont(it_font);
//                        g2.drawString("no variation", txt_x, txt_y);
//                    } else
//                    {
//                        g2.setFont(plot_font);
//                        g2.drawString("P(no-duplication)="+Double.toString(p0), txt_x, txt_y);
//                    }
//                } else
//                {
//                    DiscreteGammaPlot P_duplication = new DiscreteGammaPlot(new DiscreteGamma(rates.getAlphaDuplication()), n, Mien.RATES_DUPLICATION_COLOR);
//                    g2.setFont(plot_font);
//                    P_duplication.paintIcon(this, g2, Mien.RATE_VARIATION_PADDING+subtitle_length+Mien.RATE_VARIATION_PADDING, current_y);
//                    double p0 = rates.getForbiddenDuplication();
//                    if (p0!=0.0)
//                    {
//                        int txt_x = Mien.RATE_VARIATION_PADDING+subtitle_length+Mien.RATE_VARIATION_PADDING+Mien.RATE_VARIATION_PLOT_WIDTH+Mien.RATE_VARIATION_PADDING;
//                        int txt_y = current_y+subtitle_font.getSize();
//                        g2.setFont(plot_font);
//                        g2.drawString("P(no-duplication)="+Double.toString(p0), txt_x, txt_y);
//                    }
//                }
//                current_y += Mien.RATE_VARIATION_PLOT_HEIGHT;
//            }
//            {
//                current_y+= Mien.RATE_VARIATION_PADDING;
//                g2.setFont(subtitle_font);
//                g2.setColor(Mien.RATES_GAIN_COLOR);
//                g2.drawString("Gain rates:", Mien.RATE_VARIATION_PADDING, current_y+subtitle_font.getSize());
//                int n = rates.getNumGammaCategoriesGain();
//                if (n==1)
//                {
//                    double p0 = rates.getForbiddenGain();
//                    int txt_x = Mien.RATE_VARIATION_PADDING+subtitle_length+Mien.RATE_VARIATION_PADDING;
//                    int txt_y = current_y+subtitle_font.getSize();
//                    if (p0==0.0)
//                    {
//                        g2.setFont(it_font);
//                        g2.drawString("no variation", txt_x, txt_y);
//                    } else
//                    {
//                        g2.setFont(plot_font);
//                        g2.drawString("P(no-gain)="+Double.toString(p0), txt_x, txt_y);
//                    }
//                } else
//                {
//                    DiscreteGammaPlot P_gain = new DiscreteGammaPlot(new DiscreteGamma(rates.getAlphaGain()), n, Mien.RATES_GAIN_COLOR);
//                    g2.setFont(plot_font);
//                    P_gain.paintIcon(this, g2, Mien.RATE_VARIATION_PADDING+subtitle_length+Mien.RATE_VARIATION_PADDING, current_y);
//                    double p0 = rates.getForbiddenGain();
//                    if (p0!=0.0)
//                    {
//                        int txt_x = Mien.RATE_VARIATION_PADDING+subtitle_length+Mien.RATE_VARIATION_PADDING+Mien.RATE_VARIATION_PLOT_WIDTH+Mien.RATE_VARIATION_PADDING;
//                        int txt_y = current_y+subtitle_font.getSize();
//                        g2.setFont(plot_font);
//                        g2.drawString("P(no-gain)="+Double.toString(p0), txt_x, txt_y);
//                    }
//                }
//            }
//        } // paintComponent
    }    
    
}
