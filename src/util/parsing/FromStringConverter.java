/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.parsing;

/**
 * String to Object converter.
 * 
 * @author Plutonium_
 */
@FunctionalInterface
public interface FromStringConverter<T> {
    
    /** Converts String into object. */
    T fromS(String source);
    
    /** 
     * Converts String into object or supplied value if not parsable.
     * Use this version if source isnt guaranteed to be parsable.
     */
    default T fromS(String source, T def) {
        T t = fromS(source);
        return t==null ? def : t;
    }
    
    /** Returns whether string can be parsed. Equivalent to {@code fromS(s)!=null}. */
    default boolean isParsable(String s) {
        return fromS(s)!=null;
    }
}