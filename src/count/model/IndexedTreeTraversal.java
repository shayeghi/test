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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

/**
 *
 * Proper postfix traversal of an indexed tree. 
 * 
 * Iterator corresponds to a recursive implementation, but uses a local variable for stack. 
 * 
 * <code>for (int i=0; i&lt;node.getNumChildren(); i++) visit(node.getChild(i))); visit(node);
 * </code>
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 */
public class IndexedTreeTraversal implements Iterator<IndexedTree.Node>
{
    public IndexedTreeTraversal(IndexedTree T)
    {
        this(T.getRoot());
    }
    
    public IndexedTreeTraversal(IndexedTree.Node root)
    {
        this.pending_nodes = new Stack<>();
        initTraversal(root);
    }
    
    private void initTraversal(IndexedTree.Node root)
    {
        if (root != null)
        {
            ProcessedNode init_root = new ProcessedNode(root, -1);
            pending_nodes.push(init_root);
        }
        next();
    }
    
    @Override
    public IndexedTree.Node next()    
    {
        IndexedTree.Node retval = next_node;
        if (pending_nodes.empty())
            next_node = null;
        else
        {
            ProcessedNode last_visit = pending_nodes.pop();
            last_visit.current_child++;
            while (last_visit.current_child < last_visit.node.getNumChildren())
            {
                pending_nodes.push(last_visit);
                IndexedTree.Node child_node = last_visit.node.getChild(last_visit.current_child);
                last_visit = new ProcessedNode(child_node, 0);
            }
            next_node = last_visit.node;
        }
        return retval;
    }
    
    @Override
    public boolean hasNext()
    {
        return (next_node != null);
    }

    private final Stack<ProcessedNode> pending_nodes;
    private IndexedTree.Node next_node;
    
    private static class ProcessedNode 
    {
        IndexedTree.Node node;
        int current_child;
        
        ProcessedNode(IndexedTree.Node visited_node, int current_child)
        {
            this.node = visited_node;
            this.current_child = current_child;
        }
    }
    
    
    
    /**
     * Postfix traversal by recursion: parent is visited after children. 
     * 
     * @param node subtree root (may be null)
     * @param visitor what to do with a node (called with null if node is null)
     */
    public static void postfix(IndexedTree.Node node, IndexedTree.NodeVisitor visitor)
    {
        if (node!=null) 
            for (int cidx=0; cidx<node.getNumChildren(); cidx++)
                postfix(node.getChild(cidx), visitor);
        visitor.visit(node);
    }
    
    /**
     * Prefix traversal by recursion: parent is visited before children. 
     * 
     * @param node subtree root (may be null)
     * @param visitor what to do with a node (called with null if node is null)
     */
    public static void prefix(IndexedTree.Node node, IndexedTree.NodeVisitor visitor)
    {
        visitor.visit(node);
        if (node==null) return;
        for (int cidx=0; cidx<node.getNumChildren(); cidx++)
            prefix(node.getChild(cidx), visitor);
    }
    
    /**
     * Generic traversal method. 
     * 
     * @param node current subtree root
     * @param prefix_visit called before visiting children
     * @param infix_visit called between visiting children (<var>r</var>-1 times for <var>r</var> children, using <var>node</var> as argument)
     * @param postfix_visit called after visiting children
     */
    public static void traverse(IndexedTree.Node node, IndexedTree.NodeVisitor prefix_visit, IndexedTree.NodeVisitor infix_visit, IndexedTree.NodeVisitor postfix_visit)
    {
        prefix_visit.visit(node);
        if (node!=null)
        {
            int cidx=0;
            while (cidx<node.getNumChildren()-1)
            {
                traverse(node.getChild(cidx), prefix_visit, infix_visit, postfix_visit);
                infix_visit.visit(node);
                cidx++;
            }
            if (cidx<node.getNumChildren())
            {
                traverse(node.getChild(cidx), prefix_visit, infix_visit, postfix_visit);
                // and no infix visit here
            }
        }
        postfix_visit.visit(node);
    }
    
    /**
     * Empty visitor -- does not do anything. 
     */
    public static final IndexedTree.NodeVisitor EMPTY_VISIT = new IndexedTree.NodeVisitor() {
        @Override
        public void visit(IndexedTree.Node node) {}
    };
    
    
    public static int[] getSubtreeSizes(IndexedTree tree)
    {
        final int num_nodes = tree.getNumNodes();
        final int[] size = new int[num_nodes];
        for (int node_idx=0; node_idx<num_nodes; node_idx++)
        {
            if (tree.isLeaf(node_idx))
                size[node_idx]=1;
            else
            {
                int s = 0;
                final int nc = tree.getNumChildren(node_idx);
                for (int ci=0; ci<nc; ci++)
                {
                    int child_idx = tree.getChildIndex(node_idx, ci);
                    s += size[child_idx];
                }
                size[node_idx]=s+1;
            }
        }
        return size;
    }
    
    /**
     * Array of depths (number of edges from root).
     * 
     * @param tree
     * @return 
     */
    public static int[] getDepths(IndexedTree tree)
    {
        final int num_nodes = tree.getNumNodes();
        int[] depth = new int[num_nodes];
        
        int node_idx = num_nodes-1;
        assert (tree.isRoot(node_idx));

        depth[node_idx] = 0;
        while (node_idx>0)
        {
            --node_idx;
            int parent_idx = tree.getParentIndex(node_idx);
            depth[node_idx] = depth[parent_idx]+1;
        }
        return depth;
    }
    
    /**
     * Array of heights (max number of edges to a terminal node in subtree).
     * 
     * @param tree
     * @return 
     */
    public static int[] getHeights(IndexedTree tree)
    {
        final int num_nodes = tree.getNumNodes();
        final int[] height = new int[num_nodes];
        // for leaves it stays at 0
        for (int node_idx=tree.getNumLeaves(); node_idx<num_nodes; node_idx++)
        {
            int num_ch = tree.getNumChildren(node_idx);
            assert (num_ch!=0);
            int ci =0;
            int maxh = height[tree.getChildIndex(node_idx, ci)];
            ci++;
            while (ci<num_ch)
            {
                int child_idx = tree.getChildIndex(node_idx, ci);
                int h = height[child_idx];
                maxh = maxh<h?h:maxh;
                ci++;
            }
            height[node_idx] = maxh+1;
        }
        return height;
    }
    
    public static String[] getLeafNames(IndexedTree tree)
    {
        int num_leaves = tree.getNumLeaves();
        String[] names = new String[num_leaves];
        for (int leaf_idx=0; leaf_idx<num_leaves; leaf_idx++)
        {
            names[leaf_idx] = tree.getName(leaf_idx);
        }
        return names;
    }
    
    /**
     * Distance as sum of edge lengths from the root.
     * 
     * @param tree
     * @return 
     */
    public static double[] getScaledDepth(IndexedTree tree)
    {
        int num_nodes = tree.getNumNodes();
        double[] d = new double[num_nodes];
        int node_idx = num_nodes-1;

        assert (tree.isRoot(node_idx));
        d[node_idx] = 0.0; // root
        
        while(node_idx>0)
        {
            --node_idx;
            double len = tree.getLength(node_idx);
            double pd = d[tree.getParentIndex(node_idx)];
            d[node_idx] = pd+len;
        }
        return d;
    }
    
    
    public static class NodeCollector extends ArrayList<IndexedTree.Node> implements IndexedTree.NodeVisitor
    {
        @Override
        public void visit(IndexedTree.Node node) 
        {
            if (node != null)
                this.add(node);
        }        
    }
    
    public static class LeafCollector extends ArrayList<IndexedTree.Node> implements IndexedTree.NodeVisitor
    {
        @Override
        public void visit(IndexedTree.Node node) 
        {
            if (node != null && node.isLeaf())
                this.add(node);
        }        
        
    }
            
    
}
