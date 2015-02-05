/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Serialization;

import java.io.File;
import java.io.IOException;
import util.dev.Log;

/**
 *
 * @author Plutonium_
 */
public interface SelfDeserializator<I> {
    
    public void deserialize(File f) throws IOException;
    public default boolean deserializeSupressed(File f) {
        try {
            deserialize(f);
            return true;
        } catch(IOException e) {
            Log.err("Unable to deserialize " + getClass().getSimpleName() + 
                    " from the file: " + f.getPath());
            return false;
        }
    }
    
}