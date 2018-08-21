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
package count.model;

import count.matek.DiscreteDistribution;
import java.io.BufferedReader;
import java.util.Arrays;

/**
 *
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 */
public class TreeWithRates implements ProbabilisticEvolutionModel.BirthDeath
{
    public static final double DEFAULT_GAIN_RATE = 0.2;
    public static final double DEFAULT_LOSS_RATE = 1.0;
    public static final double DEFAULT_DUPLICATION_RATE = 0.5;

    private double[] gain_rates=null;
    private double[] loss_rates=null;
    private double[] duplication_rates=null;
    private double[] edge_lengths=null;
    
    private double common_gain_rate;
    private double common_loss_rate;
    private double common_duplication_rate;
    private double common_edge_length;
    
    private final IndexedTree tree;
    
    private DiscreteDistribution root_distribution;
    
    public TreeWithRates(IndexedTree phylo, DiscreteDistribution root_distribution)
    {
        this.tree = phylo;
        setRootDistribution(root_distribution);
        initRates();
    }
    
    private void initRates()
    {
        double f= edgeNormalizingFactor();
        for (int edge_idx = 0; edge_idx<tree.getNumEdges(); edge_idx++)
        {
            this.setEdgeLength(tree.getLength(edge_idx)*f);
        }
        this.setLossRate(DEFAULT_LOSS_RATE);
        this.setGainRate(DEFAULT_GAIN_RATE);
        this.setDuplicationRate(DEFAULT_DUPLICATION_RATE);
    }
    
    private double edgeNormalizingFactor()
    {
        double[] depths = IndexedTreeTraversal.getScaledDepth(tree);
        int node_idx = 0;
        double dmax = depths[node_idx];
        node_idx++;
        while (node_idx<tree.getNumLeaves())
        {
            dmax = Math.max(dmax, depths[node_idx]);
            node_idx++;
        }
        
        int dlog = (int)Math.log10(dmax);
        // dmax dlog
        // 0.1..0.9 -1
        // 1..9 0
        // 10..90 1
        // 
        double factor = 1.0;
        if (dlog<0)
        {
            while (dlog<0)
            {
                dlog++;
                factor *= 10.0;
            }
        } else
        {
            while (dlog>0)
            {
                dlog--;
                factor *= 0.1;
            }
        }
        return factor;
    }
    
    public final boolean hasLineageSpecificLength()
    {
        return edge_lengths!=null;
    }
    public final boolean hasLineageSpecificLoss()
    {
        return loss_rates != null;
    }
    public final boolean hasLineageSpecificDuplication()
    {
        return duplication_rates != null;
    }
    public final boolean hasLineageSpecificGain()
    {
        return gain_rates != null;
    }
    
    public final void setRootDistribution(DiscreteDistribution distr)
    {
        this.root_distribution = distr;
    }
    
    @Override
    public final DiscreteDistribution getRootDistribution()
    {
        return this.root_distribution;
    }

    @Override
    public final double getEdgeLength(int node_idx)
    {
        if (hasLineageSpecificLength())
            return edge_lengths[node_idx];
        else
            return common_edge_length;
    }
    
    @Override
    public final double getLossRate(int node_idx)
    {
        if (hasLineageSpecificLoss())
        {
            return loss_rates[node_idx];
        } else
        {
            return common_loss_rate;
        }
    }
    
    @Override
    public final double getGainRate(int node_idx)
    {
        if (hasLineageSpecificGain())
        {
            return gain_rates[node_idx];
        } else
        {
            return common_gain_rate;
        }
    }
    
    @Override
    public final double getDuplicationRate(int node_idx)
    {
        if (hasLineageSpecificDuplication())
        {
            return duplication_rates[node_idx];
        } else
        {
            return common_duplication_rate;
        }    
    }
    
    @Override 
    public final IndexedTree getPhylogeny()
    {
        return this.tree;
    }
    
    /**
     * Sets common edge length (and erases lineage-specific values).
     * @param len
     */
    public void setEdgeLength(double len)
    {
        this.edge_lengths = null;
        this.common_edge_length = len;
    }
    
    /**
     * Sets lineage-specific length.
     * 
     * @param edge_idx
     * @param len 
     */
    public void setEdgeLength(int edge_idx, double len)
    {
        if (edge_lengths == null)
        {
            edge_lengths = new double[tree.getNumEdges()];
            Arrays.fill(edge_lengths, common_edge_length);
        }
        edge_lengths[edge_idx] = len;
    }
    
    /**
     * Sets common gain rate (and erases lineage-specific values).
     * 
     * @param r 
     */
    public void setGainRate(double r)
    {
        this.gain_rates = null;
        this.common_gain_rate = r;
    }
    
    /**
     * Sets lineage-specific gain.
     * @param edge_idx
     * @param r
     */
    public void setGainRate(int edge_idx, double r)
    {
        if (gain_rates == null)
        {
            gain_rates = new double[tree.getNumEdges()];
            Arrays.fill(gain_rates, common_gain_rate);
        }
        gain_rates[edge_idx] = r;
    }
    
    /**
     * Sets common duplication rate (and erases lineage-specific ones).
     * 
     * @param r 
     */
    public void setDuplicationRate(double r)
    {
        this.duplication_rates = null;
        this.common_duplication_rate = r;
    }
    

    /**
     * Sets lineage-specific duplication rate.
     * 
     * @param edge_idx
     * @param r 
     */
    public void setDuplicationRate(int edge_idx, double r)
    {
        if (duplication_rates == null)
        {
            duplication_rates = new double[tree.getNumEdges()];
            Arrays.fill(duplication_rates, common_duplication_rate);
        }
        duplication_rates[edge_idx] = r;
    }
    
    /**
     * Sets common loss rate (and erases lineage-specific ones).
     * 
     * @param r 
     */
    public void setLossRate(double r)
    {
        this.loss_rates = null;
        this.common_loss_rate = r;
    }
    
    /**
     * Sets lineage-specific loss rate.
     * 
     * @param edge_idx
     * @param r 
     */
    public void setLossRate(int edge_idx, double r)
    {
        if (loss_rates ==  null)
        {
            loss_rates = new double[tree.getNumEdges()];
            Arrays.fill(loss_rates, common_loss_rate);
        }
        loss_rates[edge_idx] = r;
    }
    
    public void readEdgeRates(BufferedReader R)
    {
        EdgeRatesReader reader = new EdgeRatesReader(R);
        
        IndexedTreeTraversal.postfix(getPhylogeny().getRoot(), reader);
    }
    
    private class EdgeRatesReader implements IndexedTree.NodeVisitor
    {
        private final BufferedReader input;
        EdgeRatesReader(BufferedReader R)
        {
            this.input = R;
        } 
        
        @Override
        public void visit(IndexedTree.Node node)
        {
            
            if (node!=null && !node.isRoot())
            {
                try
                {
                    String line = null;
                    do
                    {
                        line = input.readLine();
                    } while (line != null && line.startsWith("#")); // skip comments

                    int node_idx = node.getIndex();
                    String[] fields=line.split("\\s+");
                    setEdgeLength(node_idx, Double.parseDouble(fields[0]));
                    setDuplicationRate(node_idx, Double.parseDouble(fields[1]));
                    setLossRate(node_idx, Double.parseDouble(fields[2]));
                    setGainRate(node_idx, Double.parseDouble(fields[3]));
//                    System.out.println("#*visit "+node+"\t// "+line);
                    
                } catch (java.io.IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
