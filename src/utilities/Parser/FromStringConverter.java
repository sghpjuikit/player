/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.Parser;

/**
 *
 * @author Plutonium_
 */
@FunctionalInterface
public interface FromStringConverter<T> {
    
    /** 
     * Converts String into object.
     * @return object parsed from String.
     */
    T fromS(String source);
}