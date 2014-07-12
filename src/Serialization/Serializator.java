/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Serialization;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Plutonium_
 */
public interface Serializator<I> {
    
    public void serialize(I item, File f) throws IOException ;
}
