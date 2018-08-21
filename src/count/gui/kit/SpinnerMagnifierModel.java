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
package count.gui.kit;

import javax.swing.SpinnerNumberModel;


/**
 * A spinner number model with variable increments:
 * possible values are 1.25,1.5,...,10,12.5,..,100,125,... and 0.9,0.8,...0.1,0.09,0.08,...,0.01,...
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 */
public class SpinnerMagnifierModel extends SpinnerNumberModel
{
    public SpinnerMagnifierModel(double value, double minimum, double maximum)
    {
        super(value, minimum, maximum, 1.0);
    }
        
    @Override
    public Object getPreviousValue()
    {
        Number V = (Number)getValue();
        double v = V.doubleValue();
        Number M = (Number)getMinimum();
        double m = M.doubleValue();
        if (m>=v)
            return null;
        double d = 0.1;
        if (v<1.0)
        {
            double z=v;
            while (z<=0.1)
            {
                z*=10.0;
                d*=0.1;
            }
        } else
        {
            double z=v;
            while(z>1.0)
            {
                z*=0.1;
                d*=10.0;
            }
        }
        if (d>=1.0)
            d/=4.0;

        double w= d*((int)(v/d))-d;
//        System.out.println("#*SMM.gNV "+v+"\td "+d+"\tw "+w);
        return new Double(w);
    }

    @Override
    public Object getNextValue()
    {
        Number V = (Number)getValue();
        double v = V.doubleValue();
        Number M = (Number)getMaximum();
        double m = M.doubleValue();
        if (m<=v)
            return null;
        double d = 0.1;
        if (v<1.0)
        {
            double z = v;
            while (z<0.1)
            {
                z*=10.0;
                d*=0.1;
            }
        } else
        {
            double z=v;
            while(z>=1.0)
            {
                z*=0.1;
                d*=10.0;
            }
        }
        
        
        if (d>=1.0)
            d/=4.0;
        double w= d*((int)(v/d))+d;
//        System.out.println("#*SMM.gNV "+v+"\td "+d+"\tw "+w);
        return new Double(w);
    }

}