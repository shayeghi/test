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
package count.io;

import java.io.File;

/**
 * A class for storing different types of Objects (data) with an associated file.
 *
 * @param <Datatype> whatever data is associated with the file
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 * @since December 10, 2007, 9:10 PM
 */
public final class DataFile<Datatype> 
{
    
    /** Creates a new instance of DataFile */
    public DataFile(Datatype data, File associated_file) 
    {
        this.data = data;
        this.associated_file = associated_file;
        setDirty(associated_file == null || associated_file.getParent()==null);
    }

    /**
     * Initialization with null file. 
     *
     * @param data the object stored here
     */
    public DataFile(Datatype data) 
    {
        this(data, null);
    }
    
    
    /**
     * Returns the file associated with this data set
     * @return the associated file (may be null)
     */
    public File getFile()
    {
        return associated_file;
    }

    /**
     * Returns the data set
     */
    public Datatype getContent()
    {
        return data;
    }
    

    /**
     * Sets the associated file
     */
    public void setFile(File F)
    {
        this.associated_file = F;
        setDirty(associated_file == null || associated_file.getParent()==null);        
    }
    

    /**
     * Sets the data
     */
    public void setData(Datatype D)
    {
        this.data = D;
    }
    
    /**
     * Whether the data set was modified since the last save
     */
    public boolean isDirty()
    {
        return dirty;
    }
    
    /**
     * Sets the dirty bit (data set is not saved)
     */ 
    public void setDirty(boolean dirty)
    {
        this.dirty=dirty;
    }
    
    private Datatype data;
    private File associated_file;
    
    private boolean dirty;
}
