/*
 * Copyright 2014 Miklos Csuros (csuros@iro.umontreal.ca).
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

import count.gui.Mien;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * A generic panel for a set of indexed points.
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 * @since November 14, 2007, 12:10 AM
 */
public class PointSetPanel extends JPanel implements ListSelectionListener
{
    /**
     * A data structure for storing the points. 
     * 
     * (More sophisticated implementation would 
     * have a proper geometric data structure here.)
     */
    private final Set<IndexedPoint> point_set;

    /**
     * Rectangle describing the currently selected area.
     */
    private Rectangle current_selection;
    /**
     * Version of {@link #current_selection} for drawing.
     * Width and height are always positive and within the enclosing component.
     */
    private Rectangle drawn_selection;
    /**
     * Color used for displaying the selection.
     */
    private Color selection_area_color=Color.GRAY;

    /**
     * Default squared distance for finding points near a mouse click. 
     */
    private double close_point_radius_sqr=9.0;
    
    private final TooltipSaver tooltip_info = new TooltipSaver();

    private PointSelector point_selection_handler;
    private AreaSelection area_selection_handler;
    
    /**
     * Instantiation with full set of arguments.  
     * 
     * @param point_set set of points stored here.
     * @param selection_mode one of the constants from {@link javax.swing.ListSelectionModel}
     * @param point_selection_enabled whether points can be selected by mouse clicks
     * @param area_selection_enabled whether mouse dragging is tracked by a rectangular selection area
     */
    public PointSetPanel(Set<IndexedPoint> point_set, int selection_mode, boolean point_selection_enabled, boolean area_selection_enabled)
    {
        super(true); // double buffered JPanel
        this.point_set = point_set;
        {
            ListSelectionModel selection_model = new DefaultListSelectionModel();
            selection_model.setSelectionMode(selection_mode);
            setSelectionModel(selection_model);
        }
        
        setPointSelectionEnabled(point_selection_enabled);
        setAreaSelectionEnabled(area_selection_enabled);
        setToolTipText(""); // so that tool tip text will be queried 
        
    }
    
    
    /**
     * Instantiation with an empty point set.  
     * Add points later using {@link #getPointSet() } and {@link java.util.Set#add(java.lang.Object) }, then repaint.
     * 
     * @param selection_mode one of the constants from {@link javax.swing.ListSelectionModel}
     * @param point_selection_enabled whether points can be selected by mouse clicks
     * @param area_selection_enabled whether mouse dragging is tracked by a rectangular selection area
     */
    public PointSetPanel(int selection_mode, boolean point_selection_enabled, boolean area_selection_enabled)
    {
        this(new java.util.HashSet<>(),selection_mode, point_selection_enabled, area_selection_enabled);
    }    
    
    private ListSelectionModel point_selection;
    
    public final ListSelectionModel getSelectionModel()
    {
        return point_selection;
    }
    
    public final void setSelectionModel(ListSelectionModel model)
    {
        model.addListSelectionListener(this);
        this.point_selection = model;
    }
    
    public final void setPointSelectionEnabled(boolean enable)
    {
        if (enable)
        {
            if (point_selection_handler == null)
            {
                point_selection_handler = new PointSelector();
                this.addMouseListener(point_selection_handler);
            } else
            {
                // nothing to do if already enabled
            }
        } else
        {
            if (point_selection_handler!=null)
            {
                this.removeMouseListener(point_selection_handler);
                point_selection_handler=null;
            } else
            {
                // nothing to do if already disabled
            }
        }
    }
    
    public final void setAreaSelectionEnabled(boolean enable)
    {
        if (enable)
        {
            if (area_selection_handler==null)
            {
                area_selection_handler = new AreaSelection();
                this.addMouseListener(area_selection_handler);
                this.addMouseMotionListener(area_selection_handler);
            }
        } else
        {
            if (area_selection_handler != null)
            {
                this.removeMouseListener(area_selection_handler);
                this.removeMouseMotionListener(area_selection_handler);
                area_selection_handler = null;
            }
        }
    }
    
    
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
//        System.out.println("#*PSP.vC "+e);
        
        repaint(); // show selected nodes
    }
    
    /**
     * Selects or adds a point to current selection.
     * 
     * @param ipoint
     * @param e if {@link java.awt.event.MouseEvent#isControlDown() } , then extends the current selection; double-click sets the selection
     */
    protected void selectPoint(IndexedPoint ipoint, MouseEvent e)
    {
        int idx = ipoint.getIndex();
        if (e.getClickCount()>1)
        {
            point_selection.setSelectionInterval(idx, idx);
        } else if (e.isControlDown())
        {
            point_selection.addSelectionInterval(idx, idx);
        } else
        {
            point_selection.setSelectionInterval(idx, idx);
        }
    }
    
    /**
     * Selects a set of points.
     * 
     * @param selected_point_set
     * @param e 
     */
    protected void selectPoints(Set<IndexedPoint> selected_point_set, MouseEvent e)
    {
        point_selection.setValueIsAdjusting(true);
        if (!e.isControlDown())
            point_selection.clearSelection();
        for (IndexedPoint ipoint: selected_point_set)
        {
            int idx = ipoint.getIndex();
            point_selection.addSelectionInterval(idx, idx);
        }
        point_selection.setValueIsAdjusting(false);
    }
    
    /**
     * Whether the point with the given index is selected.
     * 
     * @param idx
     * @return 
     */
    public boolean isSelected(int idx)
    {
        return point_selection.isSelectedIndex(idx);
    }
    
    public boolean isSelectionEmpty()
    {
        return point_selection.isSelectionEmpty();
    }
    
    
    
//    
//    /**
//     * Hook for subclasses: called when a single point is selected by mouse clicks.
//     * @param P selected point, not <code>null</code>.
//     * @param num_mouse_clicks number of clicks 
//     * @param extend_selection whether the point should be used to extend the selection, or to replace the selection
//     */
//    protected abstract void selectPoint(IndexedPoint P, int num_mouse_clicks, boolean extend_selection);
//
//    /**
//     * Hook for subclasses: called when several points are selected in an area.
//     * @param L list of selected points, may be <code>null</code>
//     * @param extend_selection whether the points should be used to extend the selection, or to replace the selection
//     */
//    protected abstract void selectPoints(Set<IndexedPoint> L, boolean extend_selection);
//
//    /**
//     * Hook for subclasses: called when selection is removed.
//     */
//    protected abstract void removeSelection();
    
    /**
     * Hook for subclasses: called for displaying tool tip at a point. 
     * 
     * @param x mouse location
     * @param y mouse location
     * @param p closest point [may be <code>null</code>]
     * @return text to be displayed in tooltip
     */
    protected String getTooltipText(int x, int y, IndexedPoint p)
    {
        return Integer.toString(p.getIndex());
    }
            
//    public static void drawNodeLabelCentered(Graphics g, String label_text, IndexedPoint point, int dx, int dy)
//    {
//        int x = (int) point.getX()+dx;
//        int y = (int) point.getY()+dy;
//        
//        DrawString.drawCenteredString(g, label_text, x, y);
//    }
//    
//    public void drawNodeLabelTrimmed(Graphics g, String label_text, String trimmed_text, IndexedPoint point, int dx, int dy)
//    {
//        FontMetrics label_fm = g.getFontMetrics();
//
//        String full_name = trimmed_text;
//        if (full_name!=null && !full_name.equals(""))
//        {
//            int x = (int)point.getX()+dx;
//            int y = (int)point.getY()+dy;
//            int h = label_fm.getHeight()+3;
//            for (int j=full_name.length(); j>=0; j--)
//            {
//                String dn = full_name.substring(0,j);
//                String cand_label = label_text+" ["+dn;
//                if (j<full_name.length())
//                    cand_label = cand_label+"...";
//                cand_label = cand_label+"]";
//                int w = label_fm.stringWidth(cand_label)+8;
//                Rectangle covered_by_label = new Rectangle(x-1, y-h+3,w,h);
//                int num_nodes_covered = this.getNumCoveredPoints(covered_by_label);
//                covered_by_label.contains(point);
//                if (j==0 || num_nodes_covered==0)
//                {
//                    label_text = cand_label;
//                    break;
//                }
//            }
//            int w = label_fm.stringWidth(label_text)+2;
//            Rectangle covered_by_label = new Rectangle(x-1, y-h+3,w,h);
//            Color label_color = g.getColor();
//            g.setColor(Mien.TREE_NODE_LABEL_BACKGROUND);//new Color(174,249,63,50)); //new Color(24,24,200,80)); //
//            g.fillRoundRect(covered_by_label.x,covered_by_label.y,covered_by_label.width, covered_by_label.height, 10, 10);
//            g.setColor(label_color);
//            g.drawString(label_text, x, y);
//        }
//    }
//    
//    public static void drawNodeLabelRotatedLeft(Graphics2D g2, String label_text, IndexedPoint point, int dx, int dy)
//    {
//        int x = (int)point.getX()+dx;
//        int y = (int)point.getY()+dy;
//        DrawString.drawRotatedStringLeft(g2,label_text, x, y,-Math.PI*0.4); 
//    }
//    
   
    /**
     * Set of points in this panel.
     * 
     * @return set of points
     */
    protected final Set<IndexedPoint> getPointSet()
    {
        return point_set;
    }
    
    /**
     * Sets the color for the selection area.
     * 
     * @param color favorite color
     */
    public void setSelectionAreaColor(Color color)
    {
        this.selection_area_color=color;
    }
    
    public Rectangle getSelectedArea()
    {
        return current_selection;
    }
    
    /**
     * Sets the radius for finding closest points (used for
     * mouse clicks and tool tip text).
     * 
     * @param r (unsquared Euclidean) distance for being close
     */
    public void setCloseRadius(double r)
    {
        close_point_radius_sqr=r*r;
    }

    
    /**
     * Euclidean distance for "close" points. 
     * 
     * @return threshold used to find points close to a mouse click 
     */
    public double getCloseRadius()
    {
        return Math.sqrt(close_point_radius_sqr);
    }
    
    /**
     * Paints the area within this panel. 
     * Points are not plotted (that is for the extending class), only the selection. 
     * 
     * @param g graphics context
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (getSelectedArea()!=null)
            drawSelectedArea(g);
    }
    /**
     * Finds the closest element to some coordinate.
     * Points outside the default radius {@link #getCloseRadius() } are ignored.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return the closest point in the set that is close enough, or null if everybody is too far
     */
    public IndexedPoint getClosestPoint(double x, double y)
    {
        double       min_dist  = this.close_point_radius_sqr;
        IndexedPoint min_point = null;
        for (IndexedPoint ipoint:point_set)
        {
            double d = ipoint.distanceSq(x, y);
            if (d<=min_dist)
            {
                min_dist = d;
                min_point = ipoint;
            }
        }
        return min_point;
    }
    
    public Set<IndexedPoint> coveredPoints(Shape shape)
    {
        return
            point_set.stream().filter((ipoint) -> (shape.contains(ipoint))).collect(Collectors.toSet()); // playing with map-reduce
    }
    
    /**
     * Points covered by the urrently selected area.
     * 
     * @return list of points within the selected area, or null if no area selected.
     * 
     */
    public Set<IndexedPoint> getCoveredPoints()
    {
        if (this.current_selection==null || point_set == null)
            return null;
        else 
            return coveredPoints(current_selection);
    }
    
    public int getNumCoveredPoints(Shape shape)
    {
        int n = 0;
        for (IndexedPoint ipoint: point_set) // could do map-reduce but there are a few hundred (thousand) points only
            if (shape.contains(ipoint))
                n++;
        return n;
    }
    
    @Override
    public String getToolTipText(MouseEvent e)
    {
        return tooltip_info.getText(e);
    }
    
    private void drawSelectedArea(Graphics g)
    {
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setColor(this.selection_area_color);
        g2.fill(drawn_selection);
    }
    
    private void deselectArea()
    {
        if (current_selection!=null)
        {
            Rectangle repaint_area = current_selection;
            current_selection=null;
            repaint(repaint_area);
        }
    }
    
    /**
     * Handles point selection by calling hooks
     * such as {@link #selectPoint(count.gui.IndexedPoint, int, boolean) }, {@link #selectPoints(java.util.List, boolean) }
     * and {@link #removeSelection() }.
     */
    private class PointSelector extends java.awt.event.MouseAdapter 
    {
        /**
         * Reacts to left-button clicks.
         * 
         * @param e 
         */
        @Override
        public void mouseClicked(MouseEvent e)
        {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            System.out.println("#*PSP.PS.mC "+e);
            
            int x=e.getX();
            int y=e.getY();

            IndexedPoint p=getClosestPoint(x,y);
            if (p!=null)
            {
                selectPoint(p, e);
            }
            else if (!e.isControlDown())
            {
                point_selection.clearSelection();
            }
            System.out.println("#*PSP.PS.mC pt "+p+"\te "+e);
        }

//        @Override
//        public void mouseReleased(MouseEvent e)
//        {
//            if (!SwingUtilities.isLeftMouseButton(e)) return;
//        }
    }
    
    private class AreaSelection extends javax.swing.event.MouseInputAdapter 
    {
        private final Rectangle previous_drawn = new Rectangle();

        @Override
        public void mousePressed(MouseEvent e) 
        {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            System.out.println("#**PSP.AS.mP "+e+" w "+getWidth()+" h "+getHeight());
            int x = e.getX();
            int y = e.getY();
            current_selection = new Rectangle(x, y, 0, 0);
            updateDrawableRect(getWidth(), getHeight());
            repaint();
        }
        
        @Override
        public void mouseDragged(MouseEvent e) 
        {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            System.out.println("#*PSP.AS.mD "+current_selection+"\te "+e);
            updateSize(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) 
        {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            
            System.out.println("#*PSP.AS.mR "+current_selection+"\te "+e);
            
            updateSize(e);

            Rectangle r=getSelectedArea();

            if (r==null)
                return;

            if (r.getWidth()<0) // flip width
            {
                r.setLocation((int)(r.getX()+r.getWidth()), (int)r.getY());
                r.setSize(-(int)r.getWidth(),(int)r.getHeight());
            }
            if (r.getHeight()<0) // dlip height
            {
                r.setLocation((int)r.getX(), (int)(r.getY()+r.getHeight()));
                r.setSize((int)r.getWidth(),-(int)r.getHeight());
            }

            if (r.getWidth()!=0. && r.getHeight()!=0.) // for a regular mouse click, mousePressed sets a 0x0 rectangle mouseReleased brings us here
            {
                selectPoints(getCoveredPoints(),e);
                deselectArea();
            }

        }


        /**
         * Called from AreaSelector.
         */
        private void updateSize(MouseEvent e) 
        {
            /*
             * Update the size of the current rectangle
             * and call repaint.  Because {@link #current_selection}
             * always has the same origin, translate it
             * if the width or height is negative.
             *
             * For efficiency (though that isn't an issue here),
             * specify the painting region using arguments
             * to the repaint() call.
             *
             */
            int x = e.getX();
            int y = e.getY();
            
            System.out.println("#*PSP.uS x "+x+"\ty "+y+"\tcur "+current_selection+"\te "+e);
            
            current_selection.setSize(x - current_selection.x,y - current_selection.y);
            updateDrawableRect(getWidth(), getHeight());
            Rectangle area_repainted = drawn_selection.union(previous_drawn);


            // if just got focus beacuse of this selection, then repaint()
            // is not enough
            if ((e.getModifiers() & MouseEvent.MOUSE_RELEASED) != 0)
                paintImmediately(area_repainted.x, area_repainted.y,
                    area_repainted.width, area_repainted.height);
            else
                repaint(area_repainted.x, area_repainted.y,
                    area_repainted.width, area_repainted.height);
      //System.out.println("#**GP.GPAS.uS tR="+totalRepaint.toString()+"; e="+e.toString());
        }

        private void updateDrawableRect(int compWidth, int compHeight) 
        {
            int x = current_selection.x;
            int y = current_selection.y;
            int width = current_selection.width;
            int height = current_selection.height;

            //requestFocus(); // just in case

            //Make the width and height positive, if necessary.
            if (width < 0) 
            {
                width = 0 - width;
                x = x - width + 1;
                if (x < 0) 
                {
                    width += x;
                    x = 0;
                }
            }
            if (height < 0) 
            {
                height = 0 - height;
                y = y - height + 1;
                if (y < 0) 
                {
                    height += y;
                    y = 0;
                }
            }

            //The rectangle shouldn't extend past the drawing area.
            if ((x + width) > compWidth) 
            {
                width = compWidth - x;
            }
            if ((y + height) > compHeight) 
            {
                height = compHeight - y;
            }

            //Update rectToDraw after saving old value.
            if (drawn_selection == null) 
            {
                drawn_selection = new Rectangle(x, y, width, height);
            } else
            {
                previous_drawn.setBounds(
                            drawn_selection.x, drawn_selection.y,
                            drawn_selection.width, drawn_selection.height);
                drawn_selection.setBounds(x, y, width, height);
            } 
        }
        
        

  }

    private class TooltipSaver
    {
        private int x=-1;
        private int y=-1;
        private String text="";
        private IndexedPoint point = null;
        
        String getText(MouseEvent e)
        {
            int ex = e.getX();
            int ey = e.getY();
            
            if (ex==x && ey==y)
            {
                return text;
            } else
            {
                x=ex;
                y=ey;
                IndexedPoint pt = getClosestPoint(ex, ey);
                if (pt==point)
                    return text;
                else
                {
                    point = pt;
                    return text = getTooltipText(x, y, pt);
                }
            }
        }
    }    
}