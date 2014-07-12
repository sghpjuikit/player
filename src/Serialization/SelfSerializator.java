/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Serialization;

import java.io.File;
import java.io.IOException;
import utilities.Log;

/**
 *
 * @author Plutonium_
 */
public interface SelfSerializator<I> extends Serializator<I>{
    
    public default void serialize(File f) throws IOException {
        serialize((I)this, f);
    }
    public default boolean serializeSupressed(File f) {
        try {
            serialize((I)this, f);
            return true;
        } catch(IOException e) {
            Log.err("Unable to serialize " + getClass().getSimpleName() + 
                    " into the file: " + f.getPath());
            return false;
        }
    }
    
}
