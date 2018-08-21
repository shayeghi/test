/*
 * Copyright 2014 Miklos Csuros (csurosm@gmail.com).
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

import java.awt.geom.Point2D;

/**
 *
 * A point with an associated integer index, set at instantiation.
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 * @since November 14, 2007, 12:02 AM
 */
public class IndexedPoint extends Point2D.Double 
{
    private final int index;

    /**
    * Instantiation (0,0).
    * @param index the index associated with this point
    */
    public IndexedPoint(int index) 
    {
        this(index,0.0,0.0);
    }

    /**
     * instantiation with coordinates.  
     * @param index 
     * @param x
     * @param y 
     */
    public IndexedPoint(int index, double x, double y)
    {
        super(x,y);
        this.index = index;
    }

    public IndexedPoint(int index, Point2D p)
    {
        this(index, p.getX(), p.getY());
    }

//    private void init(int index)
//    {
//        this.index=index;
//    }
//
//    public void setIndex(int new_index){index=new_index;}

    public int getIndex(){return index;}

    protected String paramString()
    {
        StringBuilder sb=new StringBuilder();
        sb.append("idx ");
        sb.append(index);
        sb.append(" (");
        sb.append(x);
        sb.append(", ");
        sb.append(y);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String toString()
    {
        StringBuilder sb=new StringBuilder("iP");//getClass().getName());
        sb.append('[');
        sb.append(paramString());
        sb.append(']');
        return sb.toString();
    }
    
    @Override
    public int hashCode()
    {
        return super.hashCode()*3+getIndex();
    }
    
    @Override
    public boolean equals(Object O)
    {
        if (O instanceof IndexedPoint)
        {
           IndexedPoint P = (IndexedPoint) O;
           return P.getIndex() == getIndex() && super.equals(P);
        } else
        {
            return super.equals(O);
        }
    }
    
//    /**
//     * Set with nearest-point search operation.
//     * 
//     * @since November 14, 2007, 12:03 AM
//     */
//    public class Set extends AbstractSet<IndexedPoint> implements java.util.Set<IndexedPoint>
//    {
//
//        // point w/ index i is stored at data[i]
//        // some entries in data may be null
//        private IndexedPoint[] data;
//        private int number_of_elements; //every non-null entry in data[] is before this one
//        private int capacity;
//
//        private int number_of_points; // number of non-null entries in data[]
//
//        private int number_of_iterators=0;
//        /**
//         * Set when add or remove is called while there is a coupled {@link #iterator() {. 
//         */
//        private boolean modified_while_iterating;
//
//        public Set()
//        {
//          this(10);
//        }
//
//        public Set(int initial_capacity) 
//        {
//          this.data=new IndexedPoint[initial_capacity];
//          this.capacity=initial_capacity;
//          this.number_of_elements=0;
//          this.number_of_points=0;
//        }
//
//        /**
//         * @param p a point with <em>non-negative</em> index for a new point.
//         * If p is null or the index is
//         * negative, then p is not added.
//         *
//         * @return true iff the point was added to the set.
//         *
//         * <strong>The space occupied by this class is determined by
//         * the maximum index in the set.</strong>
//         *
//         */
//        @Override
//        public boolean add(IndexedPoint p)
//        {
//          int idx;
//          if (p==null || (idx=p.getIndex())<0) return false;
//          ensureNewElementCanBeAdded(idx);
//          if (data[idx] != null)
//            return false;
//          checkConcurrency();
//          data[idx]=p;
//          number_of_points++;
//          return true;
//        }
//
//        
//        private synchronized void checkConcurrency()
//        {
//          modified_while_iterating=(number_of_iterators>0);
//        }
//
//        public IndexedPoint get(int idx)
//        {
//          return data[idx];
//        }
//
//        /**
//         * Removes all points from the set. (Does not release memory.)
//         */
//        @Override
//        public void clear()
//        {
//          if (number_of_points>0)
//          {
//            checkConcurrency();
//            java.util.Arrays.fill(data,null);
//            number_of_points=0;
//          }
//          number_of_elements=0;
//        }
//
//        @Override
//        public int size(){return number_of_points;}
//
//        /**
//         * Finds the closest point.
//         *
//         * 
//         * Note: the search time of this method is proportional to
//         * the maximum index in the set. (Never got around to make it more effective;
//         * should be acceptable for 100s-1000s points)
//         * 
//         * @param x
//         * @param y
//         * @param within radius for search
//         * @return null if no point within prescribed radius, or the point that is closes to x,y
//         */
//        public IndexedPoint closestPoint(double x, double y, double within)
//        {
//            double d_min=within;
//            int i_min=-1;
//            for (int i=0; i<number_of_elements; i++)
//                if (data[i] != null)
//                {
//                    double d=data[i].distanceSq(x,y);
//                    if (d<d_min)
//                    {
//                      d_min=d;
//                      i_min=i;
//                    }
//                }
//            if (i_min>=0)
//              return data[i_min];
//            else
//              return null;
//        }
//
//        public List<IndexedPoint> withinRectangle(Rectangle r)
//        {
//            ArrayList<IndexedPoint> v=new ArrayList<>();
//            for (int i=0; i<number_of_elements; i++)
//                if (data[i] != null && r.contains(data[i]))
//                    v.add(data[i]);
//            return v;
//        }
//
//        @Override
//        public Iterator<IndexedPoint> iterator()
//        {
//          return new IndexedPointSetIterator();
//        }
//
//        /**
//         * Removes a point.
//         * @param p point to be removed (same index, same xy coordinates)
//         * @return true iff p was in the set.
//         */
//        public boolean remove(IndexedPoint p)
//        {
//            if (!contains(p))
//                return false;
//            checkConcurrency();
//            data[p.getIndex()]=null;
//            number_of_points--;
//            return true;
//        }
//
//        /**
//         * Removes a point.
//         * @return true iff o is an IndexedPoint in the set.
//         */
//        @Override
//        public boolean remove(Object o)
//        {
//            if (o instanceof IndexedPoint)
//                return remove((IndexedPoint)o);
//            else
//                return false;
//        }
//
//        /**
//         * Removes a point by index.
//         * @param point_index the point's index
//         * @return true iff there was a point with such index.
//         */
//        public boolean remove(int point_index)
//        {
//            if (!contains(point_index))
//                return false;
//            checkConcurrency();
//            data[point_index]=null;
//            number_of_points--;
//            return true;
//        }
//
//        /**
//         * Tests if a point is in the set.
//         * @param p a 
//         * @return true iff it's there (same index, same coordinates)
//         */
//        public boolean contains(IndexedPoint p)
//        {
//            if (p==null) return false;
//            int idx=p.getIndex();
//            return (contains(idx) && p.equals(data[idx]));
//        }
//
//        /**
//         * Tests if a point is in the set.
//         * @return true iff o is an IndexedPoint and it's in the set.
//         */
//        @Override
//        public boolean contains(Object o)
//        {
//            if (o instanceof IndexedPoint)
//                return contains((IndexedPoint)o);
//            else
//                return false;
//        }
//
//        /**
//         * Tests if there is a point int the set with the given index.
//         */
//        public boolean contains(int point_index)
//        {
//          return (number_of_elements>point_index && point_index>=0 && data[point_index]!=null);
//        }
//
//        private void ensureNewElementCanBeAdded(int i)
//        {
//          if (i>=capacity)
//          {
//            // expand array
//            expandData(2*i);
//          }
//          number_of_elements = Math.max(i+1,number_of_elements);
//        }
//
//        private void expandData(int newsize)
//        {
//          IndexedPoint[] newarray=new IndexedPoint[newsize];
//          System.arraycopy(data,0,newarray,0,capacity);
//          data=newarray;
//          capacity=newsize;
//        }
//
//        private synchronized void registerIterator()
//        {
//            this.number_of_iterators++;
//        }
//
//        /**
//         * An iterator with a synchronization feature (remove is okay but either within 
//         * iterator or when there is no iterator attached)
//         */
//        private class IndexedPointSetIterator implements Iterator 
//        {
//            private int at_index;
//            private int num_points_left;
//
//            public IndexedPointSetIterator()
//            {
//                registerIterator();
//                this.at_index = -1;
//                this.num_points_left = size();
//            }
//
//            @Override
//            public boolean hasNext()
//            {
//                checkModified();
//                return (this.num_points_left>0);
//            }
//
//            @Override
//            public Object next()
//            {
//                checkModified();
//                if (this.num_points_left==0)
//                  throw new java.util.NoSuchElementException("There are no more points in this set.");
//                // find next non-null
//                do {this.at_index++;} while(this.at_index<Set.this.number_of_elements && Set.this.get(this.at_index)==null);
//                this.num_points_left--;
//                return Set.this.get(this.at_index);
//            }
//
//            @Override
//            public void remove()
//            {
//              checkModified();
//              if (at_index<0)
//                throw new IllegalStateException("Cannot remove() before starting the iteration.");
//              if (get(at_index)==null)
//                throw new IllegalStateException("Point already removed.");
//
//              Set.this.data[this.at_index]=null;
//              Set.this.number_of_points--;
//            }
//
//            private void checkModified()
//            {
//              if (Set.this.modified_while_iterating)
//                throw new java.util.ConcurrentModificationException("Set was modified.");
//            }
//
//            @Override
//            public void finalize()
//            {
//              // this iterator is not used by anyone anymore
//              number_of_iterators--;
//            }
//
//        }
//
////        /**
////         * Adds a point to the set with the given index.
////         *
////         * @param point_index index
////         * @param x X coordinate
////         * @param y Y coordinate
////         * @return false iff there is an IndexedPoint in the set already with the same index.
////         */
////        public default boolean add(int point_index, double x, double y)
////        {
////            return add(new IndexedPoint(point_index, x, y));
////        }
////        
////        /**
////         * @param P new point
////         * @return false iff there is an IndexedPoint in the set already with the same index.
////         */
////        @Override
////        public boolean add(IndexedPoint P);
//
//        
//    }
}