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
 * Common interface to evolutionary models: there is a tree and some 
 * mutation probabilities on every edge.
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 */
public interface ProbabilisticEvolutionModel 
{
    public IndexedTree getPhylogeny();
    
//    public double getTransitionProbability(int node_idx, int parent_value, int child_value);   
    public DiscreteDistribution getRootDistribution();

    /**
     * Interface to birth-and-death models, defined by edge-specific  
     * loss rate <var>Lr</var>, duplication rate <var>Dr</var> and gain rate <var>Gr</var>.  
     * Instantaneous rate 
     * for change <var>n</var>&rarr;<var>n</var>-1 is <var>n</var><var>Lr</var>, 
     * for <var>n</var>&rarr;<var>n</var>+1 is <var>Gr</var>+<var>n</var><var>Dr</var>.
     */
    public interface BirthDeath extends ProbabilisticEvolutionModel
    {
        public double getLossRate(int child_node_idx);
        public double getGainRate(int child_node_idx);
        public double getDuplicationRate(int child_node_idx);
        public double getEdgeLength(int child_node_idx);
        public boolean hasLineageSpecificLength();
        public boolean hasLineageSpecificDuplication();
        public boolean hasLineageSpecificGain();
        public boolean hasLineageSpecificLoss();
    }
}

