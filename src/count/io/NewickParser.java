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

package count.io;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;


/**
 * Newick-format phylogeny parsing.
 * 
 * Implementing classes fill exploit the hooks {@link #startChildren()}, {@link #nextChild() }, {@link #endChildren() } and {@link #endTree() },
 * {@link #setLength(double) } and {@link #setName(java.lang.String) }, which are called by {@link #parse() }.
 *  The grammar used here is the following.
 * [Corresponds to the Newick format used by Phylip, DRAWTREE etc., with the addition of the '#' style comments,
 * see Joe Felsenstein;s <a href="http://evolution.genetics.washington.edu/phylip/newick_doc.html">specification</a>].
 *
 * The original specification allowed internal taxon names only after the subtree,
 * here we accept the name also before.
 *
 * Terminals:
 * <ul>
 * <li>SEMICOLON <code>;</code></li>
 * <li>LPAREN <code>(</code></li>
 * <li>RPAREN <code>)</code></li>
 * <li>COMMA <code>,</code></li>
 * <li>COLON <code>:</code></li>
 * <li>SEMICOLON <code>;</code></li>
 * <li>LBRACKET <code>[</code></li>
 * <li>RBRACKET <code>]</code></li>
 * <li>QUOTE <code>'</code></li>
 * <li>DBLQUOTE <code>"</code></li>
 * <li>NUMBER  IEEE floating point value  (Inf, -Inf, NaN are ok)</li>
 * <li>ALPHANUM</li>
 * </ul>
 * Grammar:
 * <pre>
 *	&lt;Tree&gt;           ::= &lt;Node&gt; SEMICOLON
 *	&lt;Node&gt;           ::= &lt;Leaf&gt;|&lt;Internal&gt;
 *	&lt;Internal&gt;       ::= &lt;Name&gt; LPAREN &lt;Nodelist&gt; RPAREN &lt;Edge length&gt;
 *						| LPAREN &lt;NodeList&gt; RPAREN &lt;Edge Length&gt;
 *						| LPAREN &lt;NodeList&gt; RPAREN &lt;Name&gt; &lt;Edge Length&gt;
 *	&lt;Leaf&gt;           ::= &lt;Name&gt; &lt;Edge length&gt;|&lt;Edge length&gt;
 *	&lt;Nodelist&gt;       ::= &lt;Node&gt;|(&lt;Node&gt; COMMA &lt;Nodelist&gt;)
 *	&lt;Edge length&gt;    ::= |&lt;Nonzero edge&gt;
 *	&lt;Nonzero edge&gt;   ::= COLON NUMBER
 *	&lt;Name&gt;           ::= &lt;quoted or unquoted name&gt;
 * </pre>
 *
 * Whitespaces (newline, tab, space) are allowed anywhere
 * between tokens. Remarks are allowed where whitespace is allowed,
 * they either start with '#' and continue until the end of line or are
 * enclosed in brackets.
 * 
 * 
 *
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 * @since 2001
 */
public abstract class NewickParser
{   
    /**
     * Creates a new parser instance. 
     * 
     * @param input input source
     */
    protected NewickParser(PushbackReader input)
    {
        this(input, 
             false, // no nesting of Newick comments
             true,  // hashmark comments are recognized
             true); // unescaped single quotes are recognized in taxon name
    }
    
    /**
     * Creates a new parser instance.   
     * 
     * @param input input source used to instantiate our {@link PushbackReader} 
     */
    protected NewickParser(Reader input )
    {
        this(new PushbackReader(input));
    }
    
    /**
     * Instantiation of a parser. 
     * 
     * @param input input source used to instantiate our {@link PushbackReader} 
     * @param nested_comments_allowed whether NEwick comments can be nested
     * @param hashmark_comments_allowed whether <code>#</code> can start a comment
     * @param relaxed_name_parsing whether unescaped single quotes are ok in the name
     */
    protected NewickParser(Reader input, boolean nested_comments_allowed, boolean hashmark_comments_allowed, boolean relaxed_name_parsing)
    {
        this(new PushbackReader(input), nested_comments_allowed, hashmark_comments_allowed, relaxed_name_parsing);
    }
    
    /**
     * Instantiation of a parser with a given source.
     * 
     * @param input input source
     * @param nested_comments_allowed whether NEwick comments can be nested
     * @param hashmark_comments_allowed whether <code>#</code> can start a comment
     * @param relaxed_name_parsing whether unescaped single quotes are ok in the name
     */
    protected NewickParser(PushbackReader input, boolean nested_comments_allowed, boolean hashmark_comments_allowed, boolean relaxed_name_parsing)
    {    
        this.input = input;
        this.nested_comments_allowed = nested_comments_allowed;
        this.hashmark_comments_allowed = hashmark_comments_allowed;
        this.relaxed_name_parsing = relaxed_name_parsing;
    }

    protected final boolean nested_comments_allowed;
    protected final boolean hashmark_comments_allowed;
    /**
     * Whether finicky parser should complain about improper quote usage and 
     * messy node naming. 
     */
    protected final boolean relaxed_name_parsing;
    protected final PushbackReader input;
    
    /** Newick format terminal: quote
     */
    public static final char QUOTE='\'';
    /** Newick format terminal: left parenthesis (starts a new set of descendants)
     */
    public static final char LPAREN='('; 
    /** Newick format terminal: right parenthesis (after last child)
     */
    public static final char RPAREN=')';
    /** Newick format terminal: comma (separates child subtrees)
     */
    public static final char COMMA=','; 
    /** Newick format terminal: semicolon (terminates the tree)
     */
    public static final char SEMICOLON=';';
    /** Newick format terminal: left bracket (starts a comment)
     */
    public static final char LBRACKET='[';
    /** Newick format terminal: left bracket (ends a comment)
     */
    public static final char RBRACKET=']';
    /** Newick format terminal: double quote 
     */
    public static final char DBLQUOTE='"';
    /** Newick format terminal: hashmark
     */
    public static final char HASHMARK='#';
    /** Newick format terminal: backslash
     */
    public static final char BACKSLASH='\\';
    /** Newick format terminal: colon (starts length)
     */
    public static final char COLON=':';
    /** Newick format terminal: underscore (translates into whitespace)
     */
    public static final char UNDERSCORE='_';

    /**
     * Characters that need to be enclosed in double quotes according to the specification
     */
    private static final String NEED_QUOTE_FOR= ""+QUOTE+LPAREN+RPAREN+COMMA+SEMICOLON+COLON+LBRACKET+RBRACKET;


    private static enum ParsingState
    {
        BEFORE_NODE, 
        WITHIN_NODE,
        AFTER_NODE,
        PARSE_END;
    }
    
    /**
     * Called by {@link #parse() }  when starting to visit the current node's children (opening parenthesis).
     */
    protected abstract void startChildren();
    /**
     * Called by {@link #parse() } when starting to visit a sibling (comma).
     */
    protected abstract void nextChild();
    /**
     * Called by {@link #parse() } when done visiting the children (closing parenthesis).
     */
    protected abstract void endChildren();
    /**
     * Called by {@link #parse() } when done reading the tree (semicolon).
     */
    protected abstract void endTree();
    /**
     * Called by {@link #parse() } with the current lineage's length. 
     * 
     * @param d edge length, may also be NaN, positive or negative infinity
     */
    protected abstract void setLength(double d);
    /**
     * Called by {@link #parse() } with current node's name. 
     * 
     * @param s node name
     * @throws ParseException if current node's name is already set and not {@link #relaxed_name_parsing}
     * (technically, it is possible to list the node name both before and after the children at inner nodes). 
     * 
     */
    protected abstract void setName(String s) throws ParseException;
    
    
    /**
     * Lexical parsing for Newick format. 
     * 
     * Implementing classes fill exploit the hooks {@link #startChildren()}, {@link #nextChild() }, {@link #endChildren() } and {@link #endTree() },
     * {@link #setLength(double) } and {@link #setName(java.lang.String) }, which are called by this method.
     * 
     * @throws count.io.NewickParser.ParseException if input format does not conform
     * @throws IOException if I/O problem with file (reading or access) 
     */
    protected void parse(
        ) throws ParseException, IOException 
    {
        int current_level=0; // root level
        int c;
        ParsingState parsing_state=ParsingState.BEFORE_NODE;

        do
        {
            c=skipBlanksAndComments(input, true,nested_comments_allowed,hashmark_comments_allowed);
            //
            // --------------------------- LPAREN
            //
            if (c==LPAREN)
            {
                if (parsing_state == ParsingState.BEFORE_NODE)
                {
                    startChildren();
                    ++current_level;
                    // parsing_state=PARSE_BEFORE_NODE;
                } else
                    throw new ParseException(1, "Cannot have ``"+LPAREN+"'' here.");
            } else 
            //
            // --------------------------- COMMA
            //
            if (c==COMMA)
            {
                if (parsing_state == ParsingState.AFTER_NODE || parsing_state == ParsingState.WITHIN_NODE || parsing_state == ParsingState.BEFORE_NODE)
                {
                    if (current_level==0)
                        throw new ParseException(2, "Cannot have ``"+COMMA+"'' at root level.");
                    
                    nextChild();
                    parsing_state = ParsingState.BEFORE_NODE;
                } else
                    throw new ParseException(3, "Cannot have ``"+COMMA+"'' here.");
            } else
            //
            // --------------------------- RPAREN
            //
            if (c==RPAREN)
            {
                if (parsing_state == ParsingState.AFTER_NODE || parsing_state == ParsingState.WITHIN_NODE || parsing_state == ParsingState.BEFORE_NODE)
                {
                    if (current_level==0)
                        throw new ParseException(4, "Too many ``"+RPAREN+"''.");
                    --current_level;
                    endChildren();
                    parsing_state = ParsingState.WITHIN_NODE;
                } else
                    throw new ParseException(5, "Cannot have ``"+RPAREN+"'' here.");
            } else        
            //
            // --------------------------- COLON
            //
            if (c==COLON)
            {
                if (parsing_state == ParsingState.BEFORE_NODE || parsing_state == ParsingState.WITHIN_NODE)
                {
                    double d=parseEdgeLength(nested_comments_allowed,hashmark_comments_allowed);
                    setLength(d);
                    parsing_state=ParsingState.AFTER_NODE;
                } else
                    throw new ParseException(7,"Cannot have ``"+COLON+"'' here.");
            } else
            //
            // --------------------------- SEMICOLON
            //
            if (c==SEMICOLON)
            {
                if (parsing_state == ParsingState.AFTER_NODE || parsing_state == ParsingState.WITHIN_NODE || parsing_state == ParsingState.BEFORE_NODE)
                {
                    if (current_level != 0)
                        throw new ParseException(8,"Found ``"+SEMICOLON+"'' too early.");
                    endTree();
                    parsing_state=ParsingState.PARSE_END;
                }
            } else if (c!=-1)
            {
                //
                // --------------------------- taxon name
                //
                if (parsing_state == ParsingState.WITHIN_NODE || parsing_state == ParsingState.BEFORE_NODE)
                {
//                    if (!relaxed_name_parsing && current_node.getName() != null)
//                        throw new ParseException(9, "Cannot name a node twice.");
                    input.unread(c);
                    String s=parseName();
                    setName(s);
                } else
                    throw new ParseException(10, "Cannot have node name here.");
            }
        } while (c != -1 && parsing_state != ParsingState.PARSE_END);        

        if (parsing_state != ParsingState.PARSE_END)
            throw new ParseException(11, "Missing semicolon at the end");
    }
    
    /**
     * Skips white space and comments in the input. 
     * Reading position advances to the next informative character.
     * 
    `* @param input reader for the input
     * @param comments_allowed whether Newick-style comments (<code>[...]</code>) should be skipped
     * @param nested_comments_allowed whether Newick comments can be nested (e.g., should <code>[..[..]</code> be considered as a single comment, or advance to the second closing bracket) 
     * @param hashmark_comments_allowed whether </code>#</code> should be considered as a comment starter (until end of the line) 
     * @return first non-comment, non-whitespace character
     * @throws IOException if reading fails
     */
    private static int skipBlanksAndComments (
        PushbackReader input,
        boolean comments_allowed,
        boolean nested_comments_allowed,
        boolean hashmark_comments_allowed) throws IOException
    {
        int c;
        double d=1.0;
        boolean parsed_a_comment=false;
        do
        {
            do{c=input.read();} while(c!=-1 && Character.isWhitespace((char)c)); // skip leading blanks
            if (comments_allowed && c==LBRACKET)
            {
                parsed_a_comment=true;
                int nesting_level=1;
                do
                {
                    c=input.read();
                    if (c==RBRACKET) nesting_level--;
                    if (nested_comments_allowed && c==LBRACKET)
                        nesting_level++;
                } while (nesting_level != 0 && c != -1);
            } else if (hashmark_comments_allowed && c==HASHMARK)
            do {c=input.read();} while (c!=-1 && c!='\n' && c!='\r');
        } while(parsed_a_comment && c!=-1);
        return c;
    }

    /**
     * Buffer for parsing node names and edge lengths. Initialized for 256 chars and extended when necessary.
     */
    private char[] buffer=new char[256];
    /**
     * The number of filled positions in the buffer. 
     */
    private int buffer_length=0;

    private void resetBuffer(){ buffer_length=0;}
    
    /**
     * Test if buffer array needs to be extended.
     */
    private void checkBuffer()
    {
        if (buffer_length == buffer.length)
        {
            int new_capacity=2*buffer.length;
            char[] new_buffer=new char[new_capacity];
            System.arraycopy(buffer,0,new_buffer,0,buffer.length);
            buffer=new_buffer;
        }
    }

    /**
     * Adds one character in the buffer; extended if necessary.
     * @param c character to be added at the end
     */
    private void addToBuffer(char c)
    {
        checkBuffer();
        buffer[buffer_length++]=c;
    }    
    
    /**
     * Reads in a taxon name according to Newick rules. 
     * If relaxed name parsing is allowed ({@link #relaxed_name_parsing}), then unescaped single quotes are 
     * accepted (e.g., <q>Brewer's yeast</q>). 
     * 
     * @return a String containing the taxon name 
     * @throws IOException if there is a problem while reading
     */
    private String parseName() throws IOException
    {
        resetBuffer();
        char quote=0;
        int c=input.read();
        if (c==QUOTE || c==DBLQUOTE)
        {
            quote=(char)c;
            c=input.read();
        }

        if (quote==0) while(c != -1)
        {
            // Unquoted labels may not contain blanks, parentheses, square brackets,
            // single_quotes, colons, semicolons, or commas.
            if (Character.isWhitespace((char)c) || NEED_QUOTE_FOR.indexOf(c)>-1)
                break;
            if (c==UNDERSCORE) // replaced with space according to specification
              c=' ';
            addToBuffer((char)c);
            c=input.read();
        }
        else while(c != -1) // quoted
        {
            if (c==quote)
            {
                // check whether next is also a quote
                c=input.read();
                if (c!=quote && !relaxed_name_parsing)
                { // we're done
                    break;
                }
                if (c==quote)
                {
                    addToBuffer((char)c);
                    input.read();
                } else
                {
                    // relaxed parsing: not an escaped quote but maybe just a mistake
                    if (c==-1
                        || Character.isWhitespace((char)c)
                        || c==LPAREN
                        || c==RPAREN
                        || c==COLON
                        || c==SEMICOLON
                        || c==COMMA)
                    { // definitely end of name
                        break;
                    }
                    // otherwise it was a mistake
                    addToBuffer(quote);
                    // no need to read(): it's already done
                }
            } else
            { // not a quote
                addToBuffer((char)c);
                c=input.read();
            }
        }
        if (c!=-1) input.unread(c);
        return new String(buffer,0,buffer_length);
    }    
    
    /**
     * Parses edge length. 
     * In addition to usual numerical values, accept <code>NaN</code>, <code>+Inf</code>, <code>-Inf</code> and <code>Inf</code.  
     * 
     * @param input reader over the input
     * @param comments_allowed whether Newick-style comments are allowed at this position
     * @param whitespace_allowed whether starting whitespaces should be skipped here 
     * @return edge length; maybe NaN, positive or negative infinity
     * @throws IOException
     * @throws count.io.NewickParser.ParseException 
     */
    private double parseEdgeLength(
        boolean comments_allowed,
        boolean whitespace_allowed) throws IOException, ParseException
    {
        int c;
        if (whitespace_allowed)
            c=skipBlanksAndComments(input,comments_allowed,nested_comments_allowed,hashmark_comments_allowed);
        else
            c=input.read();

        resetBuffer();

        while (c!=-1 && !Character.isWhitespace((char)c) && c!=COMMA && c!=RPAREN && c!=SEMICOLON)
        {
            addToBuffer((char)c);
            c=input.read();
        }

        double retval=1.0;

        if (buffer_length == 0) retval=0.;
        if (buffer_length >= 3)
        {
            if (buffer[0]=='N' && buffer[1]=='a' && buffer[2]=='N' && buffer_length==3)
                retval=Double.NaN;
            if (buffer_length==4 && buffer[1]=='I' && buffer[2]=='n' && buffer[3]=='f')
            { 
                if (buffer[0]=='-')
                    retval=Double.NEGATIVE_INFINITY;
                else if (buffer[0]=='+')
                    retval=Double.POSITIVE_INFINITY;
            }
            if (buffer[0]=='I' && buffer[1]=='n' && buffer[2]=='f' && buffer_length==3)
                retval=Double.POSITIVE_INFINITY;
        }

        if (retval==1.0) // stayed the same --- no special values were seen
        {
            try
            {
                retval=Double.parseDouble(new String(buffer,0,buffer_length));
            } catch (NumberFormatException e)
            {
                throw new ParseException(99,"Cannot parse edge length: "+e.toString());
            }
        }
        if (c!=-1) input.unread(c);

        return retval;
    }    
    
    /**
     * Exception class raised by parsing syntax violations.
     */
    public static class ParseException extends IOException
    {
        public ParseException(int error_id, String s)
        {
          super("Parsing error "+error_id+":"+s);
        }
    }
    
    
    private static boolean ALWAYS_QUOTE_NAME=false;
    private static final String EMPTY_QUOTED_NAME = ""+QUOTE+QUOTE;

    public static String formatName(String name){return formatName(name,ALWAYS_QUOTE_NAME);}
    
    /**
     * Inserts quotes into/around a string following Newick specification.
     * 
     * Quotes are added if a taxon name contains a lexical token (parentheses, brackets, quotes) or white space.
     * Single quote in the name ("baker's yeast") are encoded by doubling the single quote: <code>'baker''s yeast'</code>.
     * 
     * @param name taxon name (null is OK, empty String "" will be returned)
     * @param always_quote whether forcing quotes in the returned name (or only if necessary)
     * @return properly formatted string to write in Newick file as node name. 
     */
    public static String formatName(String name, boolean always_quote)
    {
        if (name==null) return (always_quote?EMPTY_QUOTED_NAME:"");

        char[] cname=name.toCharArray();
        boolean need_quote=always_quote;
        for (int i=0; !need_quote && i<cname.length; i++)
        {
            char c = cname[i];
            need_quote = NEED_QUOTE_FOR.indexOf(c)!=-1 || Character.isWhitespace(c);
        }
        if (need_quote)
        {
            StringBuilder sb=new StringBuilder();
            sb.append(QUOTE);
            // replace single ' in the name with '' in the output
            int from_idx=0;
            int to_idx;
            do 
            {
                to_idx=name.indexOf(QUOTE,from_idx);
                if (to_idx==-1)
                    sb.append(cname,from_idx,cname.length-from_idx);
                else
                {
                    sb.append(cname,from_idx,to_idx-from_idx+1);
                    sb.append(QUOTE);
                    from_idx=to_idx+1;
                }
            } while(to_idx != -1 && from_idx<cname.length);
            sb.append(QUOTE);
            return sb.toString();
        } else
            return name;
    }
    
    /**
     * Default rounding precision in {@link #toIEEEFormat(double) }
     */
    private static final int DECIMALS=3;
    private static final double LOG10=Math.log(10.);
    /**
     * Small length under which a "negligible" value is displayed.
     * 
     * {@link #SHORT_BRANCH} should not be larger than 0.1^{@link #DECIMALS}.
     */
    private static final double SHORT_BRANCH = 1e-8;
    private static final double PRACTICALLY_ZERO_BRANCH_MULTIPLIER = 0.099; 
  
    /**
     * Powers of ten up to at least 1/{@link #SHORT_BRANCH}.
     */
    private static final double[] EXP10 = {1.,10.,100.,1000.,10000.,100000.,1e6,1e7,1e8,1e9,1e10,1e11,1e12,1e13,1e14,1e15,1e16,1e17,1e18,1e19,1e20,1e21,1e22,1e23,1e24,1e25,1e26,1e27,1e28,1e29,1e30,1e31};

    /**
     * A compact display format including extended numerical values.
     * 
     * @param d the value that needs to be displayed
     * @return a formatted string with default precision 
     */
    public static String toIEEEFormat(double d)
    {
        return toIEEEFormat(d, DECIMALS, SHORT_BRANCH);
    }
    
    /**
     * A compact display format for extended numerical values.
     * 
     * NaN, positive and negative infinity are recognized. The value
     * is shown with a precision defined by the number of decimal digits 
     * wanted after the decimal point. Very short edge lengths are replaced with a 
     * fixed, tiny positive.
     * 
     * @param d value to be displayed
     * @param decimals preferred precision for large values
     * @param too_short length cutoff for very short edges
     * @return short character string for edge length display and I/O
     */
    public static String toIEEEFormat(double d, int decimals, double too_short)
    {
        assert (decimals>=0 && decimals<EXP10.length);
        
        if (Double.isNaN(d)) return "NaN";
        else if (d==Double.POSITIVE_INFINITY) return "Inf";
        else if (d==Double.NEGATIVE_INFINITY) return "-Inf";
        else if (d==0.) return "0";
        else
        {
            int magnitude=(int) (Math.log(d)/LOG10+0.01);
            String retval;
            
            if (magnitude>=-(decimals+1))
            {
                // rounding by the precision of decimals
                double rounding_factor = EXP10[decimals];
                double r=((int)(d*rounding_factor+0.5))/rounding_factor;
                retval= Double.toString(r);
            } else if (d<too_short) {
                retval= Double.toString(PRACTICALLY_ZERO_BRANCH_MULTIPLIER*too_short);//(short_branch*0.5)+"";
            } else
            {
                // keep only one digit after decimal point: "0.0_0x" or "xe-yy" returned 
                double m = EXP10[-magnitude-1];
                double r=((int)(d*m)+0.5)/m;
          
                retval= Double.toString(r);
            }
            //System.out.println("#**TN.tIEEE "+d+"\tmag "+magnitude+"\tr "+retval);
            return retval;
            //return d+"";
        }
    }    

}
