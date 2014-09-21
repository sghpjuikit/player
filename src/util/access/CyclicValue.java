/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.access;

/**
 *
 * @author Plutonium_
 */
public interface CyclicValue<T> {
    
    /**
     * Returns next value in line. Subsequent calling of this method should
     * always produce the same ordered stream of all possible values,unless the
     * granularity does not allow this (for example real numbers).
     * 
     * @return 
     */
    T next();
    
    /**
     * Returns previous value in line. Subsequent calling of this method should
     * always produce the same ordered stream of all possible values,unless the
     * granularity does not allow this (for example real numbers).
     * 
     * @return 
     */
    T previous();
    
    /**
     * Returns cycled value as defined by the implementation. The (even infinite)
     * cycling might not traverse all (even if finite amount of) values ad it
     * could jump, skip or randomly select value.
     * <p>
     * Default implementation is equivalent to {@link #setNextValue()}.
     */
    default T cycle() {
        return next();
    }
    
/******************************************************************************/
    
    public static boolean next(boolean v) { return !v; }
    public static boolean previous(boolean v) { return !v; }
    public static Boolean next(Boolean v) { return !v; }
    public static Boolean previous(Boolean v) { return !v; }
    
    /**
     * Returns cyclically next enum constant value from list of all values for
     * specified enum constant.
     * @return next cyclical enum constant according to its ordinal number.
     */
    public static <E extends Enum> E next(E val) {
        Class c = val.getClass();
        E vals[] = (E[])( c.isEnum() ?  c.getEnumConstants()
                                     :  c.getEnclosingClass().getEnumConstants());
        int index = (val.ordinal()+1) % vals.length;
        return vals[index];
    }
    
    /**
     * Returns cyclically previous enum constant value from list of all values for
     * specified enum constant.
     * @return previous cyclical enum constant according to its ordinal number.
     */
    public static <E extends Enum> E previous(E val) {
        Class c = val.getClass();
        E vals[] = (E[])( c.isEnum() ?  c.getEnumConstants()
                                     :  c.getEnclosingClass().getEnumConstants());
        int ord = val.ordinal();
        int index = ord==0 ? vals.length-1 : ord-1;
        return vals[index];
    }
}
