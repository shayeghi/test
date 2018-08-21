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
package count.model;

/**
 *
 * Computations over profiles (size distribution across tree leaves)
 * 
 * @since April 19, 2008, 12:00 AM
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 */
public class PhyleticProfile 
{
    public static String MISSING_ENTRY = "?";
    

//    /**
//     * Instantiaton method for extending classes. setProfile() will have to be called 
//     * separately.
//     */
//    protected PhyleticProfile(){}

    /**
     * Sets the profile here.
     * 
     * @param profile the profile: size distribution at the leaves
     */
    private void setProfile(int[] profile){this.profile=profile;}
    
    /**
     * Instantiates the class
     *
     * @param profile array of sizes across the terminal taxa
     */
    public PhyleticProfile(int[] profile) 
    {
        setProfile(profile);
    }
    
    private int[] profile;

    /**
     * Profile with which this instance was created
     * 
     * @return the array with which this instance was created
     */
    public int[] getProfile()
    {
        return profile;
    }
    
    /**
     * Profile entry (family size) for a particular node.
     * 
     * @param leaf_idx index of a leaf (0..length of profile-1)
     * @return profile value at leaf_idx
     */
    public int get(int leaf_idx)
    {
        return profile[leaf_idx];
    }
    
    public Entier getValue(int leaf_idx)
    {
        return new Entier(get(leaf_idx));
    }
    
    /**
     * String representation of the pattern: numbers with at least two 
     * digits are enclosed in parentheses, one-digit numbers are simply listed.
     * Example: <tt>017(12)4</tt>.
     * 
     * @return a String representation of the extended phyletic pattern
     */
    public String getPatternString()
    {
        return getPatternString(profile);
    }
    
    /**
     * String representation of the pattern: numbers with at least two 
     * digits are enclosed in parentheses, one-digit numbers are simply listed.
     * Example: <tt>017(12)4</tt>.
     * 
     * @param profile array of sizes 
     * @return a String representation of the extended phyletic pattern
     */
    public static String getPatternString(int[] profile)
    {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<profile.length; i++)
        {
            if (profile[i]<0)
                sb.append('?');
            else if (profile[i]<10)
                sb.append(Integer.toString(profile[i]));
            else
            {
                sb.append('(');
                sb.append(Integer.toString(profile[i]));
                sb.append(')');
            }
        }
        return sb.toString();
    }
    
    public final static class Entier extends Number implements Comparable<Integer>
    {
        private final int value;
        public Entier(int n)
        {
            this.value = n;
        }

        @Override
        public int compareTo(Integer z)
        {
            return Integer.compare(value, z);
        }

        @Override
        public double doubleValue()
        {
            return (double) value;
        }

        @Override
        public int intValue()
        {
            return (int) value;
        }

        @Override
        public float floatValue()
        {
            return (float) value;
        }

        @Override
        public long longValue()
        {
            return (long) value;
        }

        /**
         * Produces a nicely rounded value for the argument. 
         * 
         * @return the String representation
         */
        @Override
        public String toString()    
        {
            if (value<0)
                return MISSING_ENTRY; // sad face
            else
                return Integer.toString(value);
        }    
        
        public boolean isAmbiguous()
        {
            return value<0;
        }
    }
}


