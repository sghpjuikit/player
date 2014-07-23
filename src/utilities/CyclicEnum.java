/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities;

/**
 * Extends behavior for {@link Enum} types.
 * <p>
 * Doesn't provide any abstract methods, instead provides default implementations
 * for two enum traversing methods - next and previous and their static
 * counterparts.
 * 
 * @param <E> {@link Enum} type. It must be the same type as the extended enum. 
 * For example: {@code enum MyEnum implements CyclicEnum<MyEnum>}
 * <p>
 * @author Plutonium_
 */
public interface CyclicEnum<E extends Enum> {
    
    /**
     * Returns cyclically next enum constant value from list of all values.
     * @return next cyclical enum constant according to its ordinal number.
     */
    public default E next() {
        return next((E)this);
    }
    
    /**
     * Returns cyclically previous enum constant value from list of all values.
     * @return previous cyclical enum constant according to its ordinal number.
     */
    public default E previous() {
        return previous((E)this);
    }

    /**
     * Returns cyclically next enum constant value from list of all values for
     * specified enum constant.
     * @return next cyclical enum constant according to its ordinal number.
     */
    public static <E> E next(E val) {
        E vals[] = (E[]) val.getClass().getEnclosingClass().getEnumConstants();
        int index = (((Enum)val).ordinal()+1) % vals.length;
        return vals[index];
    }
    
    /**
     * Returns cyclically previous enum constant value from list of all values for
     * specified enum constant.
     * @return previous cyclical enum constant according to its ordinal number.
     */
    public static <E> E previous(E val) {
        E vals[] = (E[]) val.getClass().getEnclosingClass().getEnumConstants();
        int ord = ((Enum)val).ordinal();
        int index = ord==0 ? vals.length-1 : ord-1;
        return vals[index];
    }
}
