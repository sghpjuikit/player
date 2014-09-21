/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.access;

import util.access.CyclicValue;

/**
 * Extends behavior for {@link Enum} types to include {@link CyclicValue].
 * 
 * @param <E> {@link Enum} type. It must be the same type as the extended enum. 
 * For example: {@code enum MyEnum implements CyclicEnum<MyEnum>}
 * 
 * @author Plutonium_
 */
public interface CyclicEnum<E extends Enum> extends CyclicValue<E> {
    
    /**
     * Returns cyclically next enum constant value from list of all values.
     * @return next cyclical enum constant according to its ordinal number.
     */
    @Override
    public default E next() {
        return CyclicValue.next((E)this);
    }
    
    /**
     * Returns cyclically previous enum constant value from list of all values.
     * @return previous cyclical enum constant according to its ordinal number.
     */
    @Override
    public default E previous() {
        return CyclicValue.previous((E)this);
    }
    
}
