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
interface ObjectStringParser {
    
    /**
     * Parser supports type only if it can parse both String into the type and the
     * inverse operation.
     * @return true if the class type is supported by this parser. False otherwise.
     */
    boolean supports(Class type);
    
    /** Converts String into object.
     * @return object parsed from String.
     */
    Object fromS(Class type, String source);
    
    /** Converts object into String.
     * @return String the object has been converted.
     */
    String toS(Object object);
}
