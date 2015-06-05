/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access.FieldValue;

import util.SteroidObject;
import util.access.TypedValue;

/**
 *
 * @author Plutonium_
 */
public interface FieldEnum<T> extends TypedValue, SteroidObject {
    
    /** Returns description of the field. */
    public String description();
    
    /** Returns name of the field. */
    public String name();
    
    /** 
     * Used as string converter for fielded values. For example in tables. 
     * When the object signifies empty value, empty string is returned.
     */
    public String toS(Object o, String empty_val);
}