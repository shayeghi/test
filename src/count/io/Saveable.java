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

package count.io;

import java.io.IOException;

/**
 *
 * A saveable (savable) item has a default file for saving the data. 
 * 
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 * @since December 28, 2007, 3:39 PM
 */
public interface Saveable extends Exportable
{
    /**
     * Whether an associated file is set already for this Object (i.e., <em>Save</em> can be used)
     * @return true if there is an associated file
     */
    public abstract boolean hasAssociatedFile();

    /**
     * Saves the data with the current associated file.
     * 
     * @throws IOException if an I/O error occurs
     */
    public abstract void saveData() throws IOException;
    
    /**
     * Whether a save is necessary.
     * @return true if data was modified from what is stored in the associated file
     */
    public abstract boolean isDirty();
    
    /**
     * Sets whether save is necessary.
     * @param dirty the new value for the dirty bit
     */
    public abstract void setDirty(boolean dirty);
        
    
}
