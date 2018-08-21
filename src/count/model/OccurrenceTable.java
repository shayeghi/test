/*
 * Copyright 2016 Miklos Csuros (csurosm@gmail.com).
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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import count.util.Executable;

/**
 * Stores a table of family sizes across a number of organisms. 
 *
 * A table is always linked to a list of 
 * terminal taxon names. This list may be initialized 
 * at instantiation.
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 * @since April 17, 2008, 3:23 PM
 */
public class OccurrenceTable 
{
    private OccurrenceTable()
    {
        this.terminal_taxa = new HashMap<>();
    }
    /**
     * Instantiates a table with the names of these taxa.
     * @param terminal_taxon_names array of terminal taxon names
     */
    public OccurrenceTable(String[] terminal_taxon_names) 
    {
        this();
        initNames(terminal_taxon_names);
    }

    /**
     * Terminal taxon names are stored here
     */
    private final Map<String,Integer> terminal_taxa;
    /**
     * Family profiles are stored here (row=family, column=taxon)
     */
    private int[][] table;
    
    /**
     * Family names are stored here
     */
    private String[] family_names;

    /**
     * Family properties are stored here (one entry per family).
     */
    private Properties[] family_properties;

    /**
     * Property names are stored here.      
     * Property 0 is Family.
     */
    private String[] property_names;
    
    private boolean has_missing_entries; // set by setTable()

    /**
     * Whether this table has missing entries.
     *
     * @return
     */
    public boolean hasMissingEntries()
    {
        return has_missing_entries;
    }
    
    /**
     * Taxon names in the order of a profile.
     * 
     * @return 
     */
    public String[] getTaxonNames()
    {
        String[] names = new String[terminal_taxa.size()];
        terminal_taxa.keySet().stream().forEach((s) -> {
            names[terminal_taxa.get(s)]=s;
        });
        return names;
    }
    
    /**
     * Number of families here.
     * @return 
     */
    public int getFamilyCount(){ return this.family_names.length;}    

    /**
     * Size distribution of the given family across the terminal taxa.
     * 
     * @param family_idx index of the family
     * @return array of family sizes for the terminal taxa
     */
    public int[] getSizes(int family_idx)
    {
        return table[family_idx];
    }


    /**
     * Called by instantiation methods 
     *
     * @param taxa may be null
     */
    private void initNames(String[] taxon_names)
    {
        terminal_taxa.clear();
        for (int i=0; i<taxon_names.length; i++)
        {
            if (terminal_taxa.containsKey(taxon_names[i]))
                throw new IllegalArgumentException("Repeated taxon name "+taxon_names[i]);
            terminal_taxa.put(taxon_names[i], i);
        }
        
        //System.out.println("#**IT.i "+taxa.length);
        table = null;
        family_names = new String[0];
        family_properties = new Properties[0];
        property_names = new String[0];
    }
    
    /**
     * Name of the given family
     * @param family_idx index of the family
     * @return name of the family with the given index
     */
    public String getFamilyName(int family_idx)
    {
        return family_names[family_idx];
    }

    /**
     * Each family is associated with a set of properties.
     * Initially, the single known property is the family name.
     * Further properties can be added later. 
     * 
     * @return array of known property names
     */
    public String[] getKnownProperties()
    {
        return property_names.clone();
    }
    
    /**
     * Each family is associated with a set of properties.
     * Initially, the single known property is the family name.
     * Further properties can be added later. 
     * 
     * @param property_idx 0-based index of a known property
     * 
     * @return name of the given property
     */
    public String getPropertyName(int property_idx)
    {
        return property_names[property_idx];
    }
    
    /**
     * Each family is associated with a set of properties.
     * Initially, the single known property is the family name.
     * Further properties can be added later. 
     * 
     * @return number of known properties (at least 1)
     */
    public int getKnownPropertiesCount()
    {
        return property_names.length;
    }

    /**
     * Each family is associated with a set of properties.
     * Initially, the single known property is the family name.
     * Further properties can be added later. 
     * 
     * @param family_idx 0-based index of the family
     * @param property_name name of a known property
     * @return the value of the given property, or null if the property is unknown
     */
    public String getFamilyProperty(int family_idx, String property_name)
    {
        return family_properties[family_idx].getProperty(property_name);
    }

    /**
     * Each family is associated with a set of properties.
     * Initially, the single known property is the family name.
     * Further properties can be added later. 
     * 
     * @param family_idx 0-based index of the family
     * @param property_idx 0-based index of known properties
     * @return the value of the given property
     */
    public String getFamilyProperty(int family_idx, int property_idx)
    {
        return family_properties[family_idx].getProperty(property_names[property_idx]);
    }
    
    public void setFamilyProperty(int family_idx, String property_name, String property_value)
    {
        family_properties[family_idx].setProperty(property_name, property_value);
    }
    
    public void setFamilyProperty(int family_idx, int property_idx, String property_value)
    {
        setFamilyProperty(family_idx,property_names[property_idx],property_value);
    }
    
    /**
     * Registers a new property that applies to every family. 
     * 
     * @param property_name name of the new property
     * @return index of the new property
     */
    public int registerProperty(String property_name)
    {
        if (property_name==null)
            property_name = "P"+Integer.toString(property_names.length);
        String[] new_props = new String[property_names.length+1];
        for (int j=0; j<property_names.length; j++)
        {
            if (property_name.equals(property_names[j]))
                throw new IllegalArgumentException("Cannot add more than one property with the same name `"+property_name+"'");
            new_props[j]=property_names[j];
        }
        //System.out.println("#*OT.rP '"+property_name+"'\tidx "+property_names.length);
        new_props[property_names.length]=property_name;
        property_names = new_props;
        return property_names.length-1;
    }
    
    /**
     * Returns the number of families present at a terminal taxon
     * 
     * @param taxon_idx index of the terminal taxon (same order as getTerminalTaxonNames)
     * @return number of families with non-zero size
     */
    public int getNumFamiliesPresent(int taxon_idx)
    {
        int s = 0;
        for (int i=0; i<table.length; i++)
            if (table[i][taxon_idx]>0)
                s++;
        return s;
    }
    
    
    
//    /**
//     * Reads in a table.
//     * @param reader the input reader from which the lines are read
//     * @throws java.io.IOException if there is something wrong with I/O 
//     */
//    public void readTable(Reader reader) throws IOException
//    {
//        readTable(reader, false);
//    }
//    
    /**
     * Sets the table from externally constructed data. 
     * 
     * @param table family sizes: table[i][j] is the size of the i-th family in the j-th species (this latter indexed as in terminal_taxa)
     * @param family_names names for the families in the order of the indexes into the int[] array; if null, then they are set to are set to "F1,F2,..."
     */
    public void setTable(int[][] table, String[] family_names)
    {
        this.table=table;

        int nfam = table.length;
        this.family_names = family_names;
        if (family_names == null)
        {
            this.family_names = new String[nfam];
            for (int i=0; i<nfam; i++)
                this.family_names[i] = "F"+Integer.toString(i+1);
        }
        
        family_properties = new Properties[nfam];
        property_names = new String[1];
        property_names[0] = "Family";
        for (int family_idx = 0; family_idx<nfam; family_idx++)
        {
            Properties prop = new Properties();
            //if (family_names != null)
                prop.setProperty(property_names[0], this.family_names[family_idx]);
            family_properties[family_idx] = prop;
        }
        
        checkMissingEntries();
    }    
    
    private void checkMissingEntries()
    {
        this.has_missing_entries = false;
        for (int i=0; i<table.length && !has_missing_entries; i++)
            for (int j=0; j<table[i].length && !has_missing_entries; j++)
                has_missing_entries = (table[i][j]<0) ;
    }
    
    /*
     * Reads in an OccurrenceTable using a reader
     * 
     * @param reader the input reader from which the lines are read
     * @param includes_properties whether columns that do not correspond to terminal taxa should be considered as property columns
     * 
     * @throws java.io.IOException if there is something wrong with I/O 
     */
    public void readTable(Reader reader, boolean includes_properties) throws IOException
    {
        
        List<String> property_names_in_input = new ArrayList<>();
        List<int[]> parsed_sizes = new ArrayList<>();
        List<String> parsed_family_names = new ArrayList<>();
        List<List<String>> parsed_family_properties = new ArrayList<>();

        BufferedReader R=new BufferedReader(reader);
        String line;
        int[] taxon_order = null; // how to permute the columns to match terminal_taxa: -1 denotes unknown
        do 
        {
            line = R.readLine();
            if (line == null || line.trim().length()==0 || line.startsWith("#"))
                continue;
            
            String[] fields=line.split("\\t");
            if (taxon_order == null)
            {
                // header line
                taxon_order=new int[fields.length-1]; // column 0 is for family name 
                
                
                for (int j=1; j<fields.length; j++)
                {
                    if (terminal_taxa.containsKey(fields[j])) // we know this guy
                    {
                        int taxon_idx = terminal_taxa.get(fields[j]);
                        taxon_order[j-1]=taxon_idx;
                    } else 
                    {
                        taxon_order[j-1]=-1;
                        if (includes_properties)
                            property_names_in_input.add(fields[j]);
                    }
                }
            } else
            {
                // data lines
                List<String> properties_in_line = (includes_properties?new ArrayList<>():null);
                
                String family = fields[0]; 
                int[] copies = new int[terminal_taxa.size()];
                for (int j=1; j<fields.length; j++)
                {
                    int idx = taxon_order[j-1];
                    if (idx != -1)
                    {
                        if (PhyleticProfile.MISSING_ENTRY.equals(fields[j]))
                        {
                            copies[idx]=-1;
                        } else
                        {
                            int m=Integer.parseInt(fields[j]);
                            copies[idx]=m;
                        }
                    } else if (includes_properties)
                    {
                        properties_in_line.add(fields[j]);
                    }
                }
                parsed_family_names.add(family);
                parsed_sizes.add(copies);
                if (includes_properties)
                    parsed_family_properties.add(properties_in_line);
            }
        } while (line !=null);
        
        R.close();
        
        this.setTable(parsed_sizes.toArray(new int[0][]),parsed_family_names.toArray(new String[0]));
        
        if (includes_properties)
        {
            for (int j=0; j<property_names_in_input.size(); j++)
                registerProperty(property_names_in_input.get(j));
            int num_properties = property_names_in_input.size();
            int num_families = parsed_family_properties.size();
            for (int family_idx = 0; family_idx<num_families; family_idx++)
            {
                List<String> V = parsed_family_properties.get(family_idx);
                for (int j=0; j<num_properties; j++)
                    setFamilyProperty(family_idx, j+1, V.get(j)); // property 0 is family name that is already there
            }
        }
    }

    /**
     * Calculates a string representation of the table data.
     * 
     * @param include_properties
     * @return 
     */
    public String getFormattedTable(boolean include_properties)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Family");
        if (include_properties)
        {
            for (int prop_idx=1; prop_idx<getKnownPropertiesCount(); prop_idx++)
            {
                sb.append('\t');
                sb.append(getPropertyName(prop_idx));
            }
        }
        String[] taxon_names = getTaxonNames();
        
        for (int j=0; j<taxon_names.length; j++)
        {
            sb.append("\t");
            sb.append(taxon_names[j]);
        }
        sb.append("\n");
        for (int i=0; i<table.length; i++)
        {
            sb.append(getFamilyName(i));
            if (include_properties)
            {
                int num_properties = getKnownPropertiesCount();
                for (int prop_idx=1; prop_idx<num_properties; prop_idx++)
                {
                    sb.append('\t');
                    sb.append(getFamilyProperty(i,prop_idx));
                }
            }
            for (int j=0; j<taxon_names.length; j++)
            {
                sb.append("\t");
                if (table[i][j]<0)
                    sb.append(PhyleticProfile.MISSING_ENTRY);
                else
                    sb.append(Integer.toString(table[i][j]));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    private void mainmain(String[] args) throws Exception
    {
        int arg_idx = 0;
        if (2+arg_idx != args.length)
        {
            
            throw new IllegalArgumentException("Call as java "+getClass().getName()+" tree table");
        }
        
        String tree_file = args[arg_idx++];
        String table_file = args[arg_idx++];
        
        Phylogeny tree = Phylogeny.readNewick(new count.io.GeneralizedFileReader(tree_file));
        this.initNames(IndexedTreeTraversal.getLeafNames(tree));
        this.readTable(new count.io.GeneralizedFileReader(table_file), true);
        
        PrintStream out = System.out;
        
        out.println(Executable.getStandardHeader(this.getClass()));
        out.println(Executable.getStandardRuntimeInfo());
        out.println(Executable.getStandardHeader("Tree file: "+tree_file));
        out.println(Executable.getStandardHeader("Table file:"+table_file));
        
        out.println(this.getFormattedTable(true));
        
    }
    
    /**
     * Test code --- reads a phylogeny and a table, and then writes them to stdout.
     * @param args command line arguments
     */
    public static void main(String[] args) throws Exception
    {
        OccurrenceTable O = new OccurrenceTable();
        O.mainmain(args);
    }

}
