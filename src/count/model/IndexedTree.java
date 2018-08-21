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

package count.model;

import java.util.Iterator;

/**
 * 
 * A rooted ordered tree in which every node is assigned a unique index defined by a 
 * depth-first traversal. 
 *  
 * Node indices are consecutive 0,1,...,<var>n</var>-1, where <var>n</var> is the total number of nodes. 
 * External nodes (leaves if you will) are placed at the lower indices: 0,1,...,<var>m</var>-1 are 
 * leaves, and indices <var>m</var>, <var>m</var>+1,...,<var>n</var>-1 are internal nodes. 
 * Parent's index is always larger than the child's index; the root has index <var>n</var>-1.  
 * Children are ordered by the node's indexing {@link Node#getChild(int) }.
 * 
 * @param <U> node types in this tree
 *
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 */
public interface IndexedTree<U extends IndexedTree.Node> extends Iterable<U>
{
    /**
     * Tree root.
     * 
     * @return null if empty tree; otherwise a node without parent
     */
    public abstract U getRoot();

    /**
     * Number of nodes in the tree (including internal nodes and external nodes / leaves). 
     * 
     * @return a nonnegative integer 
     */
    public abstract int getNumNodes();
    
    /**
     * Number of leaves / terminals / external nodes in the tree. 
     * 
     * @return a nonnegative integer
     */
    public abstract int getNumLeaves();

    /**
     * Iteration over the nodes follows the order defined by {@link #getNodes() }
     * 
     * @return an iterator over the tree nodes; children will be visited before parents and leaves are visited first
     */
    @Override
    public abstract Iterator<U> iterator();
    

    /**
     * Array of nodes in the index order. 
     * 
     * Node with index <var>i</var> occupies cell <var>i</var> in the returned array.
     * The indexes correspond to a depth-first traversal (parents 
     * after children) visiting leaves first.  
     * 
     * @return an array of nodes; leaves occupying lower indices and root in the array's last cell
     */
    public abstract U[] getNodes();
    
    public abstract U[] getLeaves();
    
    public default int getNumEdges()
    {
        return getNumNodes()-1;
    }
    
    /**
     * Node selection by index.
     * 
     * @param node_idx index of node in standard traversal {@link #getNodes}; assumed to be valid 
     * 
     * @return tree node with the given index 
     */
    public abstract U getNode(int node_idx);
    
    /**
     * Index of parent node. 
     * 
     * Default implementation uses {@link Node#getParent() }.
     * 
     * @param node_idx node index; node is assumed to be different from root
     * @return parent's index; negative value for root
     */
    public default int getParentIndex(int node_idx)
    {
        IndexedTree.Node P = getNode(node_idx).getParent();
        return (P==null?-1:P.getIndex());
    }
    
    /**
     * Index of a child node. 
     * 
     * @param node_idx node index; node is assumed to be internal 
     * @param cidx order of the child node in selected postfix traversal (0=left, 1=right for binary) 
     * @return index of that child node; unspecified for leaves ({@link Node#getNumChildren()}==0)
     */
    public default int getChildIndex(int node_idx, int cidx)
    {
        return getNode(node_idx).getChild(cidx).getIndex();
    }
    
    public abstract boolean hasLength();

    /**
     * Number of children. Default implementation calls {@link Node#getNumChildren() }.
     * 
     * @param node_idx node index
     * @return number of children; 0 for leaf/external node
     */
    public default int getNumChildren(int node_idx)
    {
        return getNode(node_idx).getNumChildren();
    }
    
    /**
     * Edge length. Default implementation calls {@link Node#getLength() }.
     * 
     * @param node_idx child node index
     * @return length of the edge leading to the given node. 
     */
    public default double getLength(int node_idx)
    {
        return getNode(node_idx).getLength();
    }
    
    /**
     * Test for being a terminal node / leaf. Default implementation calls {@link #getNumChildren(int) }.
     * 
     * @param node_idx node index
     * @return whether the indexed node is a leaf
     */
    public default boolean isLeaf(int node_idx)
    {
        return getNumChildren(node_idx)==0;
    }
    
    public default boolean isRoot(int node_idx)
    {
        return getParentIndex(node_idx)<0;
    }
    
    public default String getName(int node_idx)
    {
        return getNode(node_idx).getName();
    }
    
    /**
     * Interface for tree traversal. Used by {@link IndexedTreeTraversal}. 
     */
    public static interface NodeVisitor 
    {
        public void visit(Node node);
    }
    
    /**
     * Interface for common tree node methods. Non-root nodes carry length information 
     * about the lineage leading to them. Nodes have names (mandatory for terminals/external nodes, 
     * optional for internal nodes). 
     */ 
    public static interface Node
    {
        /**
         * Implementation-specific parent access
         * 
         * @return parent node; null for root
         */
        public abstract IndexedTree.Node getParent();
        /**
         * Number of children. 
         * 
         * @return number of children; 0 for external node
         */
        public abstract int getNumChildren();
        /**
         * Indexed child. 
         * (Behavior is unspecified for negative argument or too large of an index.)  
         * 
         * @param idx child index: 0,1,...,{@link #getParent() }-1
         * @return child node with given index
         */
        public abstract IndexedTree.Node getChild(int idx);

        /**
         * Whether this is a root node. 
         * 
         * @return true if parent is null; otherwise false
         */
        public default boolean isRoot()
        {
            return getParent()==null;
        }

        /**
         * Whether this is a terminal node with no children.
         * Default implementation calls {@link #getNumChildren() }. 
         * 
         * @return true if no children; otherwise false
         */
        public default boolean isLeaf()
        {
            return getNumChildren()==0;
        }
        
        public abstract double getLength();
        
        public abstract void setLength(double d);

        public abstract String getName();

//        public abstract IndexedTree getTree();

        public abstract int getIndex();        
        
        public abstract IndexedTree.Node newChild();
    }
}
