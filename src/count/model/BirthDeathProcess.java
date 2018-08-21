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
import count.matek.NegativeBinomial;
import count.matek.PointDistribution;
import count.matek.Poisson;
import count.matek.ShiftedGeometric;

/**
 *
 * Calculations for linear birth-and-death models.
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 */
public final class BirthDeathProcess 
{
    private BirthDeathProcess(){}
    
    public static DiscreteDistribution getTransient0(ProbabilisticEvolutionModel.BirthDeath model, int node_idx)
    {
        return getTransient0(model, node_idx, 0.0);
    }

    
    /**
     * Transient distribution for <var>X</var>(<var>t</var>) given that <var>X</var>(0)=0. (That is, xenolog block size on an edge of length <var>t</var>.)
     * @param model BD model
     * @param node_idx tree node index
     * @param extinction_probability
     * @return xenolog block size distribution
     */
    public static DiscreteDistribution getTransient0(ProbabilisticEvolutionModel.BirthDeath model, int node_idx, double extinction_probability)
    {
        DiscreteDistribution DD = null;
        
        double gain_rate = model.getGainRate(node_idx);
        double len = model.getEdgeLength(node_idx);

        if (gain_rate == 0.0 || len==0.0)
            DD = new PointDistribution(1.0);
        else
        {
            double duplication_rate = model.getDuplicationRate(node_idx);
            double loss_rate = model.getLossRate(node_idx);
            
            if (duplication_rate == 0.0)
            {
                // Math.expm1(x) = e^x-1
                // 1-e^(-y) = -(e^(-y)-1) = -Math.expm1(-y);
                double lm = (loss_rate>0.0
                        ?(gain_rate*(-Math.expm1(-loss_rate*len))/loss_rate)
                        :(gain_rate*len));
//                assert (lm>0.0);
                lm *= (1.0-extinction_probability);
                DD = (lm==0.0?new PointDistribution(1.0):new Poisson(lm));
            } else
            {
                double t = gain_rate / duplication_rate;
                double q=0.;
                if (duplication_rate == loss_rate)
                {
                    double dr = duplication_rate*len;
                    q = dr/(1.+dr);
                } else 
                {
                    double b = getBeta(loss_rate, duplication_rate, len);
                    q = duplication_rate * b;
                }
                // if (extinction_probability != 0.0) // formula OK with ext_prob=0.0
                q = q*(1.-extinction_probability)/(1.-q*extinction_probability);
                if (q<0.0) q = 0.0;
                if (q>1.0) q = 1.0;

                DD = (q==0.0)?(new PointDistribution(1.0)):(new NegativeBinomial(t,q));
            }
        }
        
        return DD;
        
    }

    public static DiscreteDistribution getTransient1(ProbabilisticEvolutionModel.BirthDeath model, int node_idx)
    {
        return getTransient1(model, node_idx, 0.0);
    }

    /**
     * Transient distribution for <var>X</var>(<var>t</var>) given that <var>X</var>(0)=1. (That is, inparalog block size on an edge of length <var>t</var>.)
     * @param model BD model
     * @param node_idx tree node index
     * @param extinction_probability
     * @return inparalog block size distribution
     */
    public static DiscreteDistribution getTransient1(ProbabilisticEvolutionModel.BirthDeath model, int node_idx, double extinction_probability)
    {
        DiscreteDistribution DD = null;
        double duplication_rate = model.getDuplicationRate(node_idx);
        if (duplication_rate == 0.0)
        {
            double ml =model.getLossRate(node_idx) * model.getEdgeLength(node_idx);
            double p1 = Math.exp(-ml);
            if (extinction_probability != 0.0)
                p1 *= 1.0-extinction_probability;
            // avoid numerical roundoff errors
            if (p1<0.0) p1=0.;
            if (p1>1.0) p1 = 1.0;
            DD = new PointDistribution(1.-p1);
        } else
        {
            double p=0.0, q=0.0;
            
            double loss_rate = model.getLossRate(node_idx);
            
            if (duplication_rate == loss_rate)
            {
                double dl = duplication_rate * model.getEdgeLength(node_idx);
                p = q = dl/(1.+dl);
            } else
            {
                double b= getBeta(loss_rate, duplication_rate, model.getEdgeLength(node_idx));
                p = loss_rate * b;
                q = duplication_rate * b;
            }
            double d = 1.-q*extinction_probability; // q*(1-ep)+(1-q)

            double pmod =  (p*(1.-extinction_probability)+(1.0-q)*extinction_probability)/d;
            double qmod = q*(1-extinction_probability)/d;
//            if (Double.isNaN(pmod) || Double.isNaN(qmod))
//            {
//                System.out.println("#*BDP.gT1 NUMERROR1 p "+p+"\tq "+q+"\text "+extinction_probability
//                        +"\tb "+getBeta(loss_rate, duplication_rate, model.getEdgeLength(node_idx))
//                        +"\td "+d+"\tpmod "+pmod+"\tqmod "+qmod);
//            }

            {
                p = pmod;
                q = qmod;
            }

            // avoid numerical roundoff errors
            if (p<0.0) p = 0.0;
            if (p>1.0) p = 1.0;
            if (q<0.0) q = 0.0;
            if (q>1.0) q = 1.0;

            //assert (!Double.isNaN(p));
            //assert (!Double.isNaN(q));

            DD = new ShiftedGeometric(p,q);
        }
        
        return DD;        
    }
    
    /**
     * Computes <var>beta</var>(\var>t</var>) for the
     * distribution formulas.
     *
     * @return beta(t), that is, (1-e^{-(\mu-\lambda)t})/(\mu-\lambda e^{-(\mu-\lambda)t}).
     */
    private static double getBeta(double loss_rate, double duplication_rate, double len)
    {
        double d=loss_rate-duplication_rate;
        double minus_dl = -d*len;
        double E = Math.exp(minus_dl);
        double y = loss_rate-duplication_rate*E;
        if (minus_dl<1.0) // this is the expected behavior: loss rate should be larger than duplication rate
        {
            double x = -Math.expm1(minus_dl); //1.0-E;
            return x/y;
        } else // duplication rate is larger than loss_rate + 1.0!
        {
            // need to be careful with E here:
            // minus_dl is a large positive number,
            // E is a large positive number, possibly infinity
            // y is a large negative number, possible negative inifnity
            // d is negative

            // x/y may become NaN (infinity / infinity)

            // (1-exp(-dl)) / (mu-lambda exp^(-dl))
            // = 1/lambda * ((mu-lambda*E) + lambda-mu )/ (mu-lambda * E)
            // = 1/lambda * (1+(lambda-mu)/(mu-lambda*E))
            return (1.0 -d / y)/duplication_rate;
        }
    }

    
}
