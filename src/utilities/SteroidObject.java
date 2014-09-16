/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

/**
 *
 * @author Plutonium_
 */
public interface SteroidObject {
    
    /** 
     * Converts first letter of the string to upper case.
     */
    public default String toStringCapital() {
        String s = toString();
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1);
    }
    
    /** 
     * Converts first letter of the string to upper case and all others into
     * lower case.
     */
    public default String toStringCapitalCase() {
        String s = toString();
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
    
    /** 
     * Converts first letter of the string to upper case and all others into
     * lower case and replaces all '_' with ' '.
     * <p>
     * Use to make {@link Enum} constants more human readable, for gui for example.
     */
    public default String toStringEnum() {
        String s = toString().replaceAll("_", " ");
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
