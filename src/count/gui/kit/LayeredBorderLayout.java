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
package count.gui.kit;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 *
 * A border layout adapted for using with JLayeredPane. 
 * This layout manager handles five components, in the same 
 * vein as java.awt.BorderLayout, with the only exception that the
 * components may overlap. Overlaps are supposed to be handled by a 
 * JLayeredPane (center component is in a different
 * layer from the side components).
 * The center component's dimension are set to fit the parental container.
 * while other components (NORTH, WEST, SOUTH, EAST) are set to their preferredSize(). 
 * If the preferredSize() for some component is too large, then the component
 * size is set to use the maximal available space while keeping the preferred 
 * proportions of width vs. height. 
 *
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 * @since December 3, 2007, 12:45 AM
 */

public class LayeredBorderLayout implements LayoutManager
{
    private int minWidth = 0, minHeight = 0;
    private int preferredWidth = 0, preferredHeight = 0;
    private boolean sizeUnknown = true;
    
    public static final String NORTH = "NORTH";
    public static final String SOUTH = "SOUTH";
    public static final String EAST = "EAST";
    public static final String WEST = "WEST";
    public static final String CENTER = "CENTER";
    
    private static final int iNORTH = 0;
    private static final int iWEST = 1;
    private static final int iCENTER = 2;
    private static final int iEAST = 3;
    private static final int iSOUTH = 4;

    private static final int intValue(String position)
    {
        if (NORTH.equals(position))
            return iNORTH;
        else if (WEST.equals(position))
            return iWEST;
        else if (CENTER.equals(position))
            return iCENTER;
        else if (EAST.equals(position))
            return iEAST;
        else if (SOUTH.equals(position))
            return iSOUTH;
        else
            return -1;
    }
    
    private Component[] components;

    /**
     * Instantiaton of the layout manager, with no associated components.
     */
    public LayeredBorderLayout() 
    {
        components = new Component[5];
        sizeUnknown = true;
    }
    
    /**
     * Adds a new component to the layout. Notice that the component 
     * needs to be added to the parent container separately. 
     * (This parent container is presumably a JLayeredPane, to which 
     * components added by specifying the layers ---
     *
     * @param name placement for the component ({@link #CENTER}, {@link #SOUTH}, {@link #WEST}, {@link #NORTH} or {@link #EAST})
     * @param comp the component to be added
     */
    @Override
    public void addLayoutComponent(String name, Component comp) 
    {
        //System.out.println("#**LBL.aLC "+name+"\t"+comp);
        components[intValue(name)]=comp;
        sizeUnknown = true;
    }

    /**
     * Removes a component and invalidates the layout.
     * (Required by the interface.)
     * 
     * @param comp a component to be removed. No error for null or unknown component.
     */
    @Override
    public void removeLayoutComponent(Component comp) 
    {
        for (int j=0; j<5; j++)
            if (components[j]==comp)
                components[j]=null;
        sizeUnknown = true;
    }

    /**
     * Calculates preferred and minimum sizes for the container.
     */
    private void setSizes() 
    {

        //Reset preferred/minimum width and height.
        preferredWidth = 0;
        preferredHeight = 0;
        minWidth = 0;
        minHeight = 0;

        Dimension[] prefDim = new Dimension[5];
        Dimension[] minDim = new Dimension[5];
        Dimension Z = new Dimension(0,0);
        for (int i=0; i<5; i++)
        {
            if (components[i]!=null && components[i].isVisible())
            {
                prefDim[i] = components[i].getPreferredSize();
                minDim[i] = components[i].getMinimumSize();
            } else
            {
                prefDim[i]=minDim[i]=Z;
            }
            minWidth = Math.max(minWidth, minDim[i].width);
            minHeight = Math.max(minHeight, minDim[i].height);
        }
        preferredWidth = prefDim[iCENTER].width;
        preferredHeight = prefDim[iCENTER].height;
        
        sizeUnknown = false;
    }


    /**
     * Preferred size for this layout.
     *
     * @param parent parent container (for insets)
     * @return preferred dimension
     */
    @Override
    public Dimension preferredLayoutSize(Container parent) 
    {
        Dimension dim = new Dimension(0, 0);
        setSizes();
        //Always add the container's insets!
        Insets insets = parent.getInsets();
        dim.width = preferredWidth
                    + insets.left + insets.right;
        dim.height = preferredHeight
                     + insets.top + insets.bottom;

        return dim;
    }

    /**
     * Minimum layout size. 
     * 
     * @param parent parent container (for insets)
     * @return minimum size
     */
    @Override
    public Dimension minimumLayoutSize(Container parent) 
    {
        Dimension dim = new Dimension(0, 0);
        setSizes();

        //Always add the container's insets!
        Insets insets = parent.getInsets();
        dim.width = minWidth
                    + insets.left + insets.right;
        dim.height = minHeight
                     + insets.top + insets.bottom;


        return dim;
    }

    /**
     * Scales a component to fit an allotted space.
     * The component's preferred size is
     * scaled so that its width and height do not surpass
     * those of the available space.
     *
     * @param C the component for which the size is needed
     * @param available_space available space
     * @return the component's fitted size
     */
    private static Dimension fitComponentDimension(Component C, Dimension available_space)
    {
        Dimension pref = C.getPreferredSize();
        int w = pref.width;
        int h = pref.height;
        
        int pw = available_space.width;
        int ph = available_space.height;
            
        if (w>pw || h>ph)
        {
            // reduce proportionally
            if (w>pw)
            {
                h = h * pw/w;
                w=pw;
            }
            if (h>ph)
            {
                w = w* ph/h;
                h = ph;
            }
        }
        
        return new Dimension(w,h);
    }

    /*
     * This is called when the panel is first displayed,
     * and every time its size changes. 
     *
     * @param parent in the intended use, this is a JLayeredPane.
     */
    @Override
    public void layoutContainer(Container parent) 
    {
     // Note: You CAN'T assume preferredLayoutSize or
     // minimumLayoutSize will be called -- in the case
     // of applets, at least, they probably won't be.
        Insets insets = parent.getInsets();
        int pw = parent.getWidth()
                       - (insets.left + insets.right);
        int ph = parent.getHeight()
                        - (insets.top + insets.bottom);
        Dimension pD = new Dimension(pw,ph);
       
        // Go through the components' sizes, if neither
        // preferredLayoutSize nor minimumLayoutSize has
        // been called.
        if (sizeUnknown) 
            setSizes();


        // layout center component
        if (components[iCENTER]!=null && components[iCENTER].isVisible())
        {
            components[iCENTER].setBounds(insets.left,insets.top,pw,ph);
        }
        
        // layout north component
        if (components[iNORTH]!=null && components[iNORTH].isVisible())
        {
            Dimension D = fitComponentDimension(components[iNORTH],pD);
            int x = insets.left+(pw-D.width)/2;
            int y = insets.top;
            components[iNORTH].setBounds(x,y,D.width,D.height);
        }
        // layout west component
        if (components[iWEST]!=null && components[iWEST].isVisible())
        {
            Dimension D = fitComponentDimension(components[iWEST],pD);
            int x = insets.left;
            int y = insets.top+(ph-D.height)/2;
            components[iWEST].setBounds(x,y,D.width,D.height);
        }
        // layout east component
        if (components[iEAST]!=null && components[iEAST].isVisible())
        {
            Dimension D = fitComponentDimension(components[iEAST],pD);
            int x = insets.left+pw-D.width;
            int y = insets.top+(ph-D.height)/2;
            components[iWEST].setBounds(x,y,D.width,D.height);
        }
        // layout south component
        if (components[iSOUTH]!=null && components[iSOUTH].isVisible())
        {
            Dimension D = fitComponentDimension(components[iSOUTH],pD);
            int x = insets.left+(pw-D.width)/2;
            int y = insets.top+ph-D.height;
            components[iSOUTH].setBounds(x,y,D.width,D.height);
        }
        
        
        
    }

    @Override
    public String toString() 
    {
        return getClass().getName();
    }    
}
