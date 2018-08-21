/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package count.gui.kit;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 *
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 */
public class StringIcon implements Icon
{
    public StringIcon(int width, int height, String text_shown)
    {
        this.width = width;
        this.height = height;
        this.icon_string = text_shown;
    }

    private final int width;
    private final int height;
    private final String icon_string; 


    @Override
    public int getIconWidth(){return width;}
    @Override
    public int getIconHeight(){return height;}
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) // x y is top left 
    {
        Graphics myg = g.create();
        myg.setFont(myg.getFont().deriveFont(1.2f*height).deriveFont(Font.BOLD));
        myg.setColor(Color.BLACK);
        int w = myg.getFontMetrics().stringWidth(icon_string);
        myg.drawString(icon_string, x+width/2-w/2, y+height);
    }
    
    private static final int DEF_WIDTH=30;
    private static final int DEF_HEIGHT = 20;
    
    public static StringIcon createRightPointingFinger()
    { 
        return new StringIcon(DEF_WIDTH,DEF_HEIGHT,"\u261e");
    }

}