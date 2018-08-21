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

import count.matek.DiscreteGamma;
import count.matek.NegativeBinomial;
import count.matek.PointDistribution;
import count.matek.Poisson;
import count.matek.ShiftedGeometric;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Rate variation model: gamma + invariant rate factors
 * for edge length, loss, duplication and transfer.
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 */
public final class RateVariation 
{
    /**
     * Last entry is for multiplier 0, previous ones are for gamma
     */
    private double[] mul_duplication; 
    /**
     * Last entry is for multiplier 0, previous ones are for gamma
     */
    private double[] mul_loss; 
    /**
     * Last entry is for multiplier 0, previous ones are for gamma
     */
    private double[] mul_gain; 
    /**
     * Last entry is for multiplier 0, previous ones are for gamma
     */
    private double[] mul_length; 
    
    private double alpha_duplication=1.0;
    private double alpha_loss=1.0;
    private double alpha_gain=1.0;
    private double alpha_length=1.0;
    
    private double forbidden_duplication = 0.0;
    private double forbidden_loss = 0.0;
    private double forbidden_gain = 0.0;

    private TreeWithRates main_tree;
    private ScaledTree[] class_trees;
    private double[] class_probabilities;
    
    
    private RateVariation(){}
    
    public RateVariation(TreeWithRates main_tree)
    {
        initTree(main_tree);
    }
    
    /**
     * Sets the main tree, and initializes the multipliers (representing no variation).
     * 
     * @param main_tree 
     */
    private void initTree(TreeWithRates main_tree)
    {
        this.main_tree = main_tree;
        this.mul_duplication = setArrayValues(new double[2], alpha_duplication, true);
        this.mul_gain = setArrayValues(new double[2], alpha_gain, true);
        this.mul_loss = setArrayValues(new double[2], alpha_loss, true);
        this.mul_length = setArrayValues(new double[1], alpha_length, false);
        
        initClassTrees();
        initClassProbabilities();
    }

    private void initClassTrees()
    {
        int nc = getNumClasses();
        class_trees = new ScaledTree[nc];
        for (int cidx=0; cidx<nc; cidx++)
        {
            class_trees[cidx] = new ScaledTree(main_tree);
        }
        updateClassTreeMultipliers();
    }
    
    private void initClassProbabilities()
    {
        int nc = getNumClasses();
        class_probabilities = new double[nc];
        
        for (int class_idx=0; class_idx<nc; class_idx++)
        {
            int dup_idx = getIndexDuplication(class_idx);
            int dup_n= mul_duplication.length-1;
            double dup_p = (dup_idx == dup_n)
                           ?forbidden_duplication
                           :(1.-forbidden_duplication)/dup_n;
            int loss_idx = getIndexLoss(class_idx);
            int loss_n = mul_loss.length-1;
            double loss_p = (loss_idx == loss_n)
                           ?forbidden_loss
                           :(1.-forbidden_loss)/loss_n;
            int gain_idx = getIndexGain(class_idx);
            int gain_n = mul_gain.length-1;
            double transfer_p = (gain_idx == gain_n)
                                ?forbidden_gain
                                :(1.-forbidden_gain)/gain_n;
            int edge_n = mul_loss.length;
            double edge_p = 1.0/edge_n;
            
            class_probabilities[class_idx] =  dup_p * loss_p * transfer_p * edge_p;
        }
    }
    
    private void updateClassTreeMultipliers()
    {
        int nc = getNumClasses();
        for (int cidx=0; cidx<nc; cidx++)
        {
            ScaledTree rate_tree = class_trees[cidx];
            double xd = mul_duplication[getIndexDuplication(cidx)];
            double xl = mul_loss[getIndexLoss(cidx)];
            double xg = mul_gain[getIndexGain(cidx)];
            double xe = mul_length[getIndexEdgeLength(cidx)];
            
            rate_tree.setDuplicationRateMultiplier(xd);
            rate_tree.setEdgeLengthMultiplier(xe);
            rate_tree.setGainRateMultiplier(xg);
            rate_tree.setLossRateMultiplier(xl);
        }
    }
    
    public void setDiscretizationLoss(int num_gamma_categories, double alpha)
    {
        if (num_gamma_categories == getNumGammaCategoriesLoss())
        {
            setArrayValues(mul_loss, alpha, true);
            updateClassTreeMultipliers();
        } else
        {
            this.mul_loss = setArrayValues(new double[num_gamma_categories+1], alpha, true);
            initClassProbabilities();
            initClassTrees();
        }
    }
    

    public void setDiscretizationGain(int num_gamma_categories, double alpha)
    {
        if (num_gamma_categories == getNumGammaCategoriesGain())
        {
            setArrayValues(mul_gain, alpha, true);
            updateClassTreeMultipliers();
        } else
        {
            this.mul_gain = setArrayValues(new double[num_gamma_categories+1], alpha, true);
            initClassProbabilities();
            initClassTrees();
        }
    }

    public void setDiscretizationDuplication(int num_gamma_categories, double alpha)
    {
        if (num_gamma_categories == getNumGammaCategoriesDuplication())
        {
            setArrayValues(mul_duplication, alpha, true);
            updateClassTreeMultipliers();
        } else
        {
            this.mul_duplication = setArrayValues(new double[num_gamma_categories+1], alpha, true);
            initClassProbabilities();
            initClassTrees();
        }
    }

    public void setDiscretizationEdgeLength(int num_gamma_categories, double alpha)
    {
        if (num_gamma_categories == getNumGammaCategoriesEdgeLength())
        {
            setArrayValues(mul_length, alpha, false);
            updateClassTreeMultipliers();
        } else
        {
            this.mul_length = setArrayValues(new double[num_gamma_categories], alpha, false);
            initClassProbabilities();
            initClassTrees();
        }
    }

    public void setForbiddenDuplication(double forbidden_dup_prob)
    {
        this.forbidden_duplication = forbidden_dup_prob;
        initClassProbabilities();
        updateClassTreeMultipliers();
    }
    
    public void setForbiddenGain(double forbidden_gain_prob)
    {
        this.forbidden_gain = forbidden_gain_prob;
        initClassProbabilities();
        updateClassTreeMultipliers();
    }
    
    public void setForbiddenLoss(double forbidden_loss_prob)
    {
        this.forbidden_loss = forbidden_loss_prob;
        initClassProbabilities();
        updateClassTreeMultipliers();
    }
   
    public int getNumGammaCategoriesLoss(){ return mul_loss.length-1;}
    public int getNumGammaCategoriesDuplication(){ return mul_duplication.length-1;}
    public int getNumGammaCategoriesGain(){ return mul_gain.length-1;}
    public int getNumGammaCategoriesEdgeLength(){ return mul_length.length;}
    
    public double getAlphaLoss(){ return alpha_loss;}
    public double getAlphaDuplication(){ return alpha_duplication;}
    public double getAlphaGain(){ return alpha_gain;}
    public double getAlphaEdgeLength(){ return alpha_length;}
    
    public double getForbiddenDuplication(){ return forbidden_duplication;}
    public double getForbiddenGain(){ return forbidden_gain;}
    public double getForbiddenLoss(){ return forbidden_loss;}


    /**
     * Returns the number of discrete classes
     *
     * @return number of classes
     */
    public final int getNumClasses()
    {
        return mul_duplication.length
                *mul_loss.length
                *mul_gain.length
                *mul_length.length;
    }
    
    /**
     * Returns the duplication rate category for this class
     *
     * @param class_idx combined rate class index
     * @return index for duplication rate category
     */
    public final int getIndexDuplication(int class_idx)
    {
        int nd = mul_duplication.length;

        return class_idx % nd;
    }
    
    /**
     * Returns the loss rate category for this class
     *
     * @param class_idx combined rate class index
     * @return loss rate category in this class
     */
    public final int getIndexLoss(int class_idx)
    {
        int nd = mul_duplication.length;
        int nl = mul_loss.length;
        
        return (class_idx / nd ) % nl;
    }
    
    /**
     * Returns the gain rate category for this class
     *
     * @param class_idx combined rate class index
     * @return gain rate category for this class
     */
    public final int getIndexGain(int class_idx)
    {
        int nd = mul_duplication.length;
        int nl = mul_loss.length;
        int ng = mul_gain.length;
        
        return (class_idx / (nd *nl) ) % ng;
    }
    
    /**
     * Returns the edge length category for this class
     *
     * @param class_idx combined class index
     * @return edge length category for this class
     */
    public final int getIndexEdgeLength(int class_idx)
    {
        int nd = mul_duplication.length;
        int nl = mul_loss.length;
        int ng = mul_gain.length;
        
        return (class_idx / (nd*nl*ng));
    }
    
    /**
     * Prior probability for a rate class
     *
     * @param class_idx combined class index
     * @return prior probability for the class
     */
    public double getClassProbability(int class_idx)
    {
        return class_probabilities[class_idx];
    }
    
    
    /**
     * The underlying main tree from which the class-specific models are derived
     * by applying the appropriate modifiers.
     *
     * @return main rate tree
     */
    public TreeWithRates getMainTree()
    {
        return main_tree;
    }
    
    /**
     * Sets the invariant+gamma multipliers for an array (which may be {@link #duplication_multipliers}, {@link #gain_multipliers}, {@link #length_multipliers}, {@link #loss_multipliers}).
     * Last entry will be set to 0.0.
     * 
     * @param A array in which the multipliers are set (<code>A.length</code>-1 discrete Gamma categories) 
     * @param alpha Gamma shape parameter
     */
    private static double[] setArrayValues(double[] A, double alpha, boolean has_forbidden)
    {
        int num_gamma_classes = A.length-(has_forbidden?1:0);
        if (num_gamma_classes == 1)
        {
            A[0]=1.0;
        } else
        {
            DiscreteGamma G = new DiscreteGamma(alpha);

            double[] val = G.getPartitionMeans(num_gamma_classes);
            for (int i=0; i<val.length; i++)
            {
                A[i] = val[i];
                //Verbose.message("RV.sAV "+i+"/"+A.length+"\t"+A[i]+"\t// alpha="+alpha);
            }
        }
        if (has_forbidden)
        {
            A[A.length-1] = 0.0;
        }
        return A;
    }
        
    /**
     * Whether a class has positive probability. Return value is precomputed.
     *
     * @param cidx class index
     * @return true if {@link #getClassProbability(int) } would give a positive value
     */
    public final boolean isPertinentClass(int cidx)
    {
        return getClassProbability(cidx)>0.0;
    }
    
    
    /*  ---------------------------------------------
    
    
        --------  I/O
    
    
        ---------------------------------------------
    */
    

    /**
     * The string used in the rate file to mark the rate variation parameters
     */
    static String RATE_VARIATION_PREFIX = "|variation";
    static String ROOT_PRIOR_PREFIX = "|root";
    static String MODEL_END = "|End";

    /**
     * Our own exception type.
     */
    public static class FileFormatException extends IOException
    {
        private FileFormatException(String msg)
        {
            super(msg);
        }
    }

    public static RateVariation readRates(Reader reader, IndexedTree main_tree) throws FileFormatException, IOException
    {
        BufferedReader R = new BufferedReader(reader);
        TreeWithRates model = new TreeWithRates(main_tree, null);
        
        model.readEdgeRates(R);
        
        RateVariation var = new RateVariation(model);
        
        String line=null;
        do
        {
            line=R.readLine();
            if (line != null)
            {
                if (line.startsWith(MODEL_END))
                    break;
                line = line.trim();
                if (line.length()==0 || line.startsWith("#"))
                    continue;
                if (line.startsWith(RATE_VARIATION_PREFIX))
                {
                    String variation_data = line.substring(RATE_VARIATION_PREFIX.length()+1);
                    String[] fields = variation_data.split("\\s+");
                    if (fields.length<3)
                        throw new FileFormatException("Rate variation line has bad syntax: "+line);
                            
                    int num_categories = Integer.parseInt(fields[1]);
                    double alpha = Double.parseDouble(fields[2]);
                    double zero = 0.0;
                    if (fields.length>3)
                    {
                        zero = Double.parseDouble(fields[3]);
                    }
                    if ("loss".equals(fields[0]))
                    {
                        var.setDiscretizationLoss(num_categories, alpha);
                        var.setForbiddenLoss(zero);
                    } else if ("duplication".equals(fields[0]))
                    {
                        var.setDiscretizationDuplication(num_categories, alpha);
                        var.setForbiddenDuplication(zero);
                    } else if ("transfer".equals(fields[0]) || "gain".equals(fields[0]))
                    {
                        var.setDiscretizationGain(num_categories, alpha);
                        var.setForbiddenGain(zero);
                    } else if ("length".equals(fields[0]))
                    {
                        var.setDiscretizationEdgeLength(num_categories, alpha);
                    } else 
                    {
                        throw new FileFormatException("Variation type '"+fields[0]+"' is not recognized in the line '"+line+"'");
                    }
                } else if (line.startsWith(ROOT_PRIOR_PREFIX))
                {
                    String prior_data = line.substring(ROOT_PRIOR_PREFIX.length()+1);
                    String[] fields = prior_data.split("\\s+");
                    double[] params = new double[fields.length-1];
                    for (int i=0; i<params.length; i++)
                        params[i] = Double.parseDouble(fields[i+1]);
                    if (fields[0].equals(Poisson.class.getSimpleName()))
                        model.setRootDistribution(new Poisson(params[0]));
                    else if (fields[0].equals(NegativeBinomial.class.getSimpleName()))
                        model.setRootDistribution(new NegativeBinomial(params[0], params[1]));
                    else if (fields[0].equals(PointDistribution.class.getSimpleName()))
                        model.setRootDistribution(new PointDistribution(params[0]));
                    else if (fields[0].equals(ShiftedGeometric.class.getSimpleName()))
                        model.setRootDistribution(new ShiftedGeometric(params[0], params[1]));
                    else
                        throw new FileFormatException("Root prior distribution '"+fields[0]+"' is unknown in line '"+line+"'");
                }
            }
     
        } while (line != null);

        return var;
    }
    
    private void mainmain(String[] args) throws Exception
    {
        
        int arg_idx = 0;
        String tree_file = args[arg_idx++];
        String rate_file = args[arg_idx++];
        
        Phylogeny tree = Phylogeny.readNewick(new count.io.GeneralizedFileReader(tree_file));
        
        RateVariation zeb = readRates(new count.io.GeneralizedFileReader(rate_file), tree);
        
        
    }
    
    public static void main(String[] args) throws Exception
    {
        RateVariation O = new RateVariation();
        O.mainmain(args);
        
    }
    
    
}
