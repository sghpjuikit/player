/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.parsing;

/**
 *
 * @author Plutonium_
 */
@FunctionalInterface
public interface FromStringConverter<T> {
    
    /** 
     * Converts String into object.
     * 
     * @return object parsed from String.
     */
    T fromS(String source);
    
    /** 
     * Converts String into object. Returns supplied value if converting fails.
     * <p>
     * Use this version if source isnt guaranteed to be parsable.
     * 
     * @return object parsed from String.
     */
    default T fromS(String source, T def) {
        try {
            T t = fromS(source);
            return t==null ? def : t;
        } catch (Exception e) {
            return def;
        }
    }
}