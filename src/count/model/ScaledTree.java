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

/**
 *
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 */
public final class ScaledTree implements ProbabilisticEvolutionModel.BirthDeath
{
    private double multiplier_edge_length;
    private double multiplier_gain_rate;
    private double multiplier_duplication_rate;
    private double multiplier_loss_rate;
    private final ProbabilisticEvolutionModel.BirthDeath rate_tree;
    private DiscreteDistribution root_distribution;

    
    public ScaledTree(ProbabilisticEvolutionModel.BirthDeath  model)
    {
        this.rate_tree = model;
        this.root_distribution = model.getRootDistribution();
        setEdgeLengthMultiplier(1.0);
        setGainRateMultiplier(1.0);
        setLossRateMultiplier(1.0);
        setDuplicationRateMultiplier(1.0);
    }
    
    public void setEdgeLengthMultiplier(double x)
    {
        this.multiplier_edge_length = x;
    }
    
    public void setGainRateMultiplier(double x)
    {
        this.multiplier_gain_rate = x;
    }
    
    public void setLossRateMultiplier(double x)
    {
        this.multiplier_loss_rate=x;
    }
    
    public void setDuplicationRateMultiplier(double x)
    {
        this.multiplier_duplication_rate = x;
    }
    
    @Override
    public double getEdgeLength(int node_idx)
    {
        return rate_tree.getEdgeLength(node_idx)*multiplier_edge_length;
    }
    
    @Override 
    public DiscreteDistribution getRootDistribution()
    {
        return this.root_distribution;
    }
    
    @Override
    public double getGainRate(int node_idx)
    {
        return rate_tree.getGainRate(node_idx)*multiplier_gain_rate;
    }
    
    @Override
    public double getDuplicationRate(int node_idx)
    {
        return rate_tree.getDuplicationRate(node_idx)*multiplier_duplication_rate;
    }
    
    @Override
    public double getLossRate(int node_idx)
    {
        return rate_tree.getLossRate(node_idx)*multiplier_loss_rate;
    }
    
    @Override
    public IndexedTree getPhylogeny()
    {
        return rate_tree.getPhylogeny();
    }
    
    @Override
    public boolean hasLineageSpecificLength()
    {
        return rate_tree.hasLineageSpecificLength();
    }
    
    @Override
    public boolean hasLineageSpecificDuplication()
    {
        return rate_tree.hasLineageSpecificDuplication();
    }
    
    @Override
    public boolean hasLineageSpecificGain()
    {
        return rate_tree.hasLineageSpecificGain();
    }
    
    @Override 
    public boolean hasLineageSpecificLoss()
    {
        return rate_tree.hasLineageSpecificLoss();
    }
    
    
    
}
