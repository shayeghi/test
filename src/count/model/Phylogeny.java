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

import count.io.NewickParser;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;

/**
 *
 * Representation of a rooted tree with [optional] edge lengths. 
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 */
public class Phylogeny implements IndexedTree<Phylogeny.Taxon>
{
    /**
     * Creates an empty phylogeny (with null root).
     */
    public Phylogeny()
    {
        this(null);
    }
    /**
     * Creates a phylogeny with the given root.
     * 
     * @param root phylogeny root
     */
    public Phylogeny (Taxon  root)
    {
        this.root = root;
        if (root != null)
            root.setLength(Double.NaN); // no lengths for now
    }
    
    /**
     * 
     */
    protected void initRoot()
    {
        this.root = new Taxon();
        root.setLength(Double.NaN);
    }
    
    private Taxon root;
    
    @Override
    public Taxon getRoot() 
    {
        return root;
    }

    @Override
    public int getNumNodes() 
    {
        return (root == null?0:root.num_nodes_in_subtree); 
    }

    @Override
    public int getNumLeaves() 
    {
        return (root == null?0:root.num_leaves_in_subtree); 
    }

    @Override
    public Iterator<Taxon> iterator() 
    {
        return Arrays.asList(getNodes()).iterator();
    }

    private Taxon[] depth_first_traversal;
    
    /**
     * Array of nodes, leaves first, in depth-first-traversal (children before parents).
     * 
     * @return copy of the local array of nodes
     */
    @Override
    public Taxon[] getNodes() 
    {
        if (root == null) return new Taxon[0];
        if (root.getIndex()<0 || depth_first_traversal==null) // root index set to negative when manipulating tree
        {
            computeIndexes();
        }
        return Arrays.copyOf(depth_first_traversal, getNumNodes());
    }

    @Override
    public Taxon[] getLeaves() 
    {
        if (root == null) return new Taxon[0];
        if (root.getIndex()<0 || depth_first_traversal==null)
        {
            computeIndexes();
        }
        return Arrays.copyOf(depth_first_traversal, getNumLeaves());
    }

    @Override
    public Taxon getNode(int node_idx) 
    {
        return depth_first_traversal[node_idx];
    }

    @Override
    public boolean hasLength() 
    {
        return (!Double.isNaN(root.getLength()));
    }
    
    /**
     * Computes the node indices assuming that {@link Taxon#num_leaves_in_subtree} and {@link Taxon#num_nodes_in_subtree} are set correctly.
     */
    protected final void computeIndexes()
    {
        int num_leaves = getNumLeaves();
        int num_nodes = getNumNodes();
        depth_first_traversal = new Taxon[num_nodes];
        getRoot().computeIndex(depth_first_traversal, 0,num_leaves);
    }
    
    public void copyFromTree(IndexedTree tree)
    {
        int num_nodes = tree.getNumNodes();
        
        Taxon[] copied_nodes = new Taxon[num_nodes];
        int nidx=0;
        while (nidx<num_nodes-1) // skip root
        {
            Taxon node = copied_nodes[nidx]= new Taxon(tree.getNumChildren(nidx));
            node.setName(tree.getName(nidx));
            node.node_idx = nidx;
            nidx++;
        }
        initRoot();
        root.setName(tree.getName(nidx));
        root.node_idx = nidx;
        copied_nodes[nidx]= root;
        
        nidx=0;
        if (tree.hasLength())
        {
            while (nidx<num_nodes)
            {
                copied_nodes[nidx].setLength(tree.getLength(nidx));
                nidx++;
            }
            nidx=0;
        }
        while (nidx<num_nodes)
        {
            Taxon node = copied_nodes[nidx];
            node.num_nodes_in_subtree=1;
            int nc = tree.getNumChildren(nidx);
            if (nc==0)
            {
                node.num_leaves_in_subtree=1;
            } else
            {
                node.num_leaves_in_subtree=0;
                for (int ci=0; ci<nc; ci++)
                {
                    int cidx = tree.getChildIndex(nidx, ci);
                    Taxon child = copied_nodes[cidx];
                    node.addChild(child);
                    node.num_leaves_in_subtree += child.num_leaves_in_subtree;
                    node.num_nodes_in_subtree += child.num_nodes_in_subtree;
                }
            }
            nidx++;
        }
        this.depth_first_traversal = copied_nodes;
    }
    
    /**
     * Fuses a node into its parent (all its children become children of the original parent).
     * 
     * @param node 
     */
    public void fuseIntoParent(Taxon node)
    {
        Taxon p = node.getParent();
        int idx = node.getIndexAtParent();
        System.out.println("#*P.fuse start "+node+"\tp "+p+"\t@ "+idx);
        int nc = node.getNumChildren();
        if (nc==0)
            throw new IllegalArgumentException("Thou shalt not fuse a leaf into its parent.");
        int pnc = p.getNumChildren();
        //  abcidef
        //  abcjjjdef
        for (int j=1; j<nc; j++) // ensure capacity
        {
            p.newChild(); // these will be thrown away
        }
        
        { // shift old children to make place for the new
            int old_idx=pnc-1;
            int new_idx=p.getNumChildren()-1;
            
            while (old_idx>idx)
            {
                p.setChild(p.getChild(old_idx), new_idx);
                old_idx--;
                new_idx--;
            }
        }
        double len = (hasLength()?node.getLength():0.0);
        
        for (int ci=0; ci<nc; ci++)
        {
            Taxon child = node.getChild(ci);
            child.setLength(child.getLength()+len);
            p.setChild(child, idx+ci);
        }
        
//        System.out.println("#*P.fuse "+node+"\tp "+p);
        
        
        // update live statistics
        do
        {
            p.num_nodes_in_subtree--;
            p=p.getParent();
        } while (p!=null);
        
        this.computeIndexes();
    }
    
    /**
     * Subtree prune and regraft.
     * 
     * @param node this is node cut with its subtree
     * @param graft_position where it gets attached
     * @param graft_into whether node should become a new child, or rather a sibling (new node inserted on edge to graft position) 
     */
    public void moveNode(Taxon node, Taxon graft_position, boolean graft_into)
    {
        // cut node from current parent
        Taxon pp = node.getParent();
        int idx = node.getIndexAtParent();
        
        for (int ci=idx; ci<pp.getNumChildren()-1; ci++)
        {
            pp.setChild(pp.getChild(idx+1), idx);
        }
        pp.num_children--;
        
        Taxon p = graft_position;
        if (!graft_into)
        {
            p =  new Taxon();
            // need to create new parent
            if (graft_position.isRoot())
            {
                p.setLength(graft_position.getLength());
                if (!hasLength())
                    graft_position.setLength(1.0);
                p.addChild(graft_position);
                this.root = p;
            } else
            {
                if (hasLength())
                {
                    double len = graft_position.getLength()/2.0;
                    graft_position.setLength(len);
                    p.setLength(len);
                }
                int gidx = graft_position.getIndexAtParent();
                graft_position.getParent().setChild(p, gidx);
                p.addChild(graft_position);
            }
        }
        p.addChild(node);
        
        this.cleanSingularNodes();
        root.countNodes();
        computeIndexes();
    }
    
    /**
     * Roots the tree at a given node.
     * 
     * @param node 
     */
    public void reroot(Taxon node)
    {
//        if (getRoot()==null)
//            return;
        
        double rlen = getRoot().getLength();
        node.reroot();
        node.setLength(rlen);
        this.cleanSingularNodes();

        this.root = node;
        this.root.countNodes();
        
//        System.out.println("#*P.reroot "+node+"\t// "+depth_first_traversal.length);
//        for (int nidx=depth_first_traversal.length-1; nidx>=0; nidx--)
//            System.out.println("#*P.reroot "+depth_first_traversal[nidx]);
//        
        this.computeIndexes();
    }
    
    private void cleanSingularNodes()
    {
        getRoot().cleanSingularNodes();
        while (root.getNumChildren()==1)
        {
            Taxon child = root.getChild(0);
            child.setLength(root.getLength());
            root = child;
        }
    }
    
    
    public String newickTree(            
            boolean always_quote_name,
            boolean show_edge_lengths,
            boolean line_breaks_after_each_node,
            boolean show_node_id_info)
    {
        return getRoot().newickSubtree(always_quote_name, show_edge_lengths, line_breaks_after_each_node, show_node_id_info)+NewickParser.SEMICOLON;
    }
    
    public static Phylogeny readNewick(Reader R) throws IOException
    {
        Phylogeny F = new Phylogeny();
        Parser P = F.new Parser(R);
        P.parse();
        return F;
    }
    
    /**
     * Standard implementation for a node in a phylogeny. 
     * 
     */
    public static class Taxon implements IndexedTree.Node
    {
        /**
         * Parent node.
         */
        private Taxon momma;
        /**
         * Number of children
         */
        private int num_children;
        /**
         * Dynamically allocated array of children.
         */
        private Taxon[] children;
        /**
         * Length of lineage leading to this node. 
         */
        private double length;
        /**
         * Name of this taxon; may be null
         */
        private String name;
        /**
         * Number of nodes in subtree.
         */
        private int num_nodes_in_subtree;
        /**
         * Number of external nodes / leaves / terminals in subtree. 
         */
        private int num_leaves_in_subtree;

        /**
         * 
         */
        private int node_idx;
        
//        /**
//         * order of this node at its parent among the siblings
//         */
//        private int cidx_at_parent;  
        
        /**
         * Instantiation of a new node. 
         * 
         * Initial attributes are appropriate for root; length is set to 1.0.
         */
        Taxon()
        {
            //this.momma = null;
            //this.children = null;
            //this.num_children = 0;
//            this.cidx_at_parent = -1;
            this.num_leaves_in_subtree=1;
            this.num_nodes_in_subtree=1;
            this.length = 1.0;
        }
        
        Taxon(int children_capacity)
        {
            this();
            if (children_capacity != 0)
                children = new Taxon[children_capacity];
        }
        
        @Override
        public Taxon getParent() 
        {
            return momma;
        }
        
        @Override
        public int getNumChildren() 
        {
            return num_children;
        }

        @Override
        public Taxon getChild(int cidx) 
        {
            // assert (idx>=0 && idx<num_children);
            return children[cidx];
        }

        @Override
        public double getLength() 
        {
            return length;
        }

        @Override
        public void setLength(double d) 
        {
            this.length = d;
        }

        @Override
        public String getName() 
        {
            return name;
        }
        
        public void setName(String s)
        {
            this.name = s;
        }

        @Override
        public int getIndex() 
        {
            return node_idx;
        }
        
        /**
         * Calculates {@link #num_leaves_in_subtree} and {@link #num_nodes_in_subtree} by recursion in the subtree
         */
        public final void countNodes()
        {
            int nc = getNumChildren();
            this.num_nodes_in_subtree = 1;
            if (nc==0)
            {
                this.num_leaves_in_subtree = 1;
            } else
            {
                this.num_leaves_in_subtree = 0;
                for (int ci=0; ci<nc; ci++)
                {
                    Taxon child = getChild(ci);
                    child.countNodes();
                    this.num_leaves_in_subtree += child.num_leaves_in_subtree;
                    this.num_nodes_in_subtree += child.num_nodes_in_subtree;
                }
            }
        }
        
        /**
         * Fuses nodes with a single child into their parents using postfix traversal
         */
        private void cleanSingularNodes()
        {
            int nc = getNumChildren();
            
            for (int j=0; j<nc; j++) // skipped when nc==0
                getChild(j).cleanSingularNodes();

            if (nc==1 && !isRoot())
            {
                Taxon child = getChild(0);
                double ln = this.getLength();
                double lc = this.getLength();
                double len = (ln==1.0 && lc==1.0)?1.0:(ln+lc);
                Taxon parent = getParent();
                int idx = getIndexAtParent();
                parent.setChild(child, idx);
                child.setLength(len);
            } 
        }
        
        private void computeIndex(Taxon[] depth_first_traversal, int next_leaf_idx, int next_intl_idx)
        {            
            if (isLeaf())
            {
                depth_first_traversal[next_leaf_idx]=this;
                this.node_idx = next_leaf_idx;
            } else
            {
                for (int cidx=0; cidx<getNumChildren();cidx++)
                {
                    Taxon C = this.getChild(cidx);
                    C.computeIndex(depth_first_traversal, next_leaf_idx, next_intl_idx);
                    next_leaf_idx += C.num_leaves_in_subtree;
                    next_intl_idx += C.num_nodes_in_subtree-C.num_leaves_in_subtree;
                }
                depth_first_traversal[next_intl_idx]=this;
                this.node_idx=next_intl_idx;
            }
        }

        @Override
        public Taxon newChild() 
        {
            Taxon N = new Taxon();
            this.addChild(N);
            return N;
        }
        
        private static final int DEFAULT_NUM_CHILDREN=2;
        /**
         * Adds a new child; the child's parent is set to this node
         * @param N new child to be added
         */
        public void addChild(Taxon N)
        {
            // ensure initialization and capacity
            if (children == null)
            {
                children = new Taxon[DEFAULT_NUM_CHILDREN];
            } else if (num_children==children.length)
            {
                int new_capacity=children.length+(children.length%2==0?children.length/2:children.length);
                // 0,2,3,6,9,
                Taxon[] new_child = new Taxon[new_capacity];
                System.arraycopy(children,0,new_child,0,children.length);
                children = new_child;
            }
            setChild(N, num_children);

            num_children++;
        }
        
//        public void updateCounts(int delta_leaves, int delta_nodes)
//        {
//            num_leaves_in_subtree += delta_leaves;
//            num_nodes_in_subtree += delta_nodes;
//            if (isRoot())
//            {
//                this.node_idx=-1; // dirty flag
//                // done
//            } else
//                getParent().updateCounts(delta_leaves, delta_nodes);
//        }
        
        /**
         * Connects a child to this node.
         * 
         * Sets the pointers between child and parent; 
         * sets child edge length to 1. Parent's number of children is unaffected. 
         * 
         * @param Nchild child  node
         * @param cidx at which index should this child be set
         * @return previous child node at this position (possibly null)
         */
        private Taxon setChild(Taxon Nchild, int cidx)
        {
            Taxon old_node = this.children[cidx];
            this.children[cidx]=Nchild;
            Nchild.momma=this;
//            Nchild.cidx_at_parent=cidx;
            Nchild.length = 1.0;
            return old_node;
        }
        
        private String getNodeIdInfo()
        {
            return (isLeaf()?"L":"N")+Integer.toString(this.node_idx);
        }

        public String shortDesc()
        {
            String nnn = getName();
            return getNodeIdInfo()+(nnn==null?"":"/"+nnn);
        }
        
        private int getIndexAtParent()
        {
            Taxon p = getParent();
            int idx=0; 
            while (idx<p.getNumChildren() && p.getChild(idx)!=this) idx++;
            return idx;
        }
        
        private void reroot()
        {
            if (isRoot())
                return;
            Taxon p = getParent();
            int idx=getIndexAtParent();
            p.reroot();
            for (int i=idx; i+1<p.getNumChildren(); i++)
                p.setChild(p.getChild(i+1), i);
            p.num_children--;
            addChild(p);
            
            double len = getLength();
            p.setLength(len);
            setLength(0.);
            this.momma = null;
        }
        
        protected String paramString()
        {
            StringBuilder sb=new StringBuilder();
            sb.append(shortDesc());
            sb.append(" len ");
            sb.append(length);
            sb.append(" prnt ");
            if (getParent() != null)
                sb.append(getParent().shortDesc());
            else
                sb.append('-');
            sb.append(" chld {");
            for (int i=0; i<num_children; i++)
            {
                if (i>0)
                    sb.append(", ");
                sb.append(getChild(i).shortDesc());
            }
            sb.append("}");
            sb.append(", nl ").append(num_leaves_in_subtree);
            sb.append(", nn ").append(num_nodes_in_subtree);
            return sb.toString();
        }
        
        @Override
        public String toString()
        {
            String simple_name = getClass().getSimpleName();
            String class_id_name = "";
            for (int i=0; i<simple_name.length(); i++)
            {
                char c = simple_name.charAt(i);
                if (Character.isUpperCase(c))
                    class_id_name = class_id_name + c;
            }
            if ("".equals(class_id_name))
                class_id_name = simple_name.substring(0,Math.min(simple_name.length(),5));
            StringBuilder sb=new StringBuilder(class_id_name);
            sb.append("[");
            sb.append(paramString());
            sb.append("]");
            return sb.toString();
        }
        
        public String newickSubtree(
            boolean always_quote_name,
            boolean show_edge_lengths,
            boolean line_breaks_after_each_node,
            boolean show_node_id_info)
        {
            StringBuilder sb=new StringBuilder();
            if (show_node_id_info)
            {
                sb.append(NewickParser.LBRACKET);
                sb.append("<<< ");
                sb.append(getNodeIdInfo());
                sb.append(NewickParser.RBRACKET);
            }
            if (num_children>0)
            {
                sb.append(NewickParser.LPAREN);
                for (int i=0; i<num_children; i++)
                {
                    if (i>0)
                    {
                        sb.append(NewickParser.COMMA);
                        if (line_breaks_after_each_node)
                            sb.append("\n");
                        else
                            sb.append(' ');
                        if (show_node_id_info)
                        {
                            sb.append(NewickParser.LBRACKET);
                            sb.append("=== ");
                            sb.append(getNodeIdInfo());
                            sb.append(NewickParser.RBRACKET);
                        }
                    }
                    sb.append(getChild(i).newickSubtree(always_quote_name,show_edge_lengths,line_breaks_after_each_node,show_node_id_info));
                }
                sb.append(Parser.RPAREN);
            }

            if (isLeaf() || getName() != null)
            {
                if (!isLeaf())
                    sb.append(' ');
                sb.append(NewickParser.formatName(getName(),always_quote_name));
            }

            if (show_edge_lengths && !isRoot())
            {
                sb.append(NewickParser.COLON);
                sb.append(NewickParser.toIEEEFormat(getLength()));
            }
            if (show_node_id_info)
            {
                sb.append(Parser.LBRACKET);
                sb.append(" >>> ");
                sb.append(getNodeIdInfo());
                sb.append(Parser.RBRACKET);
            }
            return sb.toString();
        }
    }
    
    private class Parser extends NewickParser
    {
        private Parser(Reader input)
        {
            super(input);
            
            
        }
//        private Parser(PushbackReader input)
//        {
//            super (input);
//        }        
//        private Parser(PushbackReader input, boolean nested_comments_allowed, boolean hashmark_comments_allowed, boolean relaxed_name_parsing)
//        {
//            super(input, nested_comments_allowed, hashmark_comments_allowed, relaxed_name_parsing);
//        }
//        private Parser(Reader input, boolean nested_comments_allowed, boolean hashmark_comments_allowed, boolean relaxed_name_parsing)
//        {
//            super(input, nested_comments_allowed, hashmark_comments_allowed, relaxed_name_parsing);
//        }
        
        private Taxon current_node;
        private int current_leaf_idx;
        private int current_intl_idx;
        
        @Override 
        protected void parse() throws NewickParser.ParseException, IOException
        {
            initRoot();
            current_node = getRoot();
            current_leaf_idx = 0;
            current_intl_idx = 0;
            super.parse();
            // check if all leaves have names
            for (String s: IndexedTreeTraversal.getLeafNames(Phylogeny.this))
                if (s==null)
                    throw new ParseException(11, "Tree has unnamed terminal nodes (too many commas somewhere maybe?)");
            
            
            
            
        }
        
        /**
         * Makes sure that current node's index and subtree size are correct. 
         * 
         * @return parent of current node
         */
        private Taxon completeCurrentNode()
        {
            if (current_node.isLeaf())
                current_node.node_idx = current_leaf_idx++;
            else
                current_node.node_idx = current_intl_idx++;
            Taxon parent = current_node.getParent();
            if (parent != null)
            {
                parent.num_leaves_in_subtree += current_node.num_leaves_in_subtree;
                parent.num_nodes_in_subtree += current_node.num_nodes_in_subtree;
            }
            return parent;
        }

        @Override
        protected void startChildren() 
        {
            if (current_node.isLeaf())
                current_node.num_leaves_in_subtree--; 
            
            current_node = current_node.newChild();
        }

        @Override
        protected void nextChild() 
        {
            current_node = completeCurrentNode().newChild();
        }

        @Override
        protected void endChildren() 
        {
            current_node = completeCurrentNode();
        }
        
        @Override
        protected void endTree()
        {
            // reset root to null if nothing there: a Newick file could describe an empty tree (";"). 
            if (getRoot().isLeaf() && getRoot().getName() == null)
            {
                root = null;
            } else
                computeIndexes();
        }

        @Override
        protected void setLength(double d) 
        {
            current_node.setLength(d);
            if (!hasLength()) getRoot().setLength(0.0);
        }
        
        @Override
        protected void setName(String s) throws NewickParser.ParseException
        {
            if (current_node.getName()==null)
            {
                current_node.setName(s);
            } else if (relaxed_name_parsing)
            {
                current_node.setName(current_node.getName().concat(" && "+s));
            } else // 
            {
                assert (!relaxed_name_parsing && current_node.getName()!=null);
                throw new NewickParser.ParseException(9, "Cannot name a node twice.");
            } 
        }
        
    }
    
    
    /**
     * Main entry for testing. 
     * 
     * @param args 2 arguments: file and comma-separated list of terminal taxa
     * @throws Exception whenever it feels like it
     */
    public static void main(String[] args) throws Exception
    {
        if (args.length != 2)
        {
            System.err.println("Call as $0 file list\n\twhere list is a comma-separated list of terminal taxa\n\t;Output is the tree spanned by those taxa.");
            System.exit(0);
        }
        String tree_file = args[0];
        Phylogeny P = Phylogeny.readNewick(new java.io.FileReader(tree_file));
        
        System.out.println(P.newickTree(false, false, true, true));
        
        Iterator<IndexedTree.Node> iter = new IndexedTreeTraversal(P);
        while(iter.hasNext())
        {
            System.out.println("#** node "+iter.next());
        }
        
        IndexedTreeTraversal.postfix(P.getRoot(), new IndexedTree.NodeVisitor() {
            @Override
            public void visit(IndexedTree.Node node) {
                System.out.println("#*| node "+node.toString());
            }
        });
        
    }
}
