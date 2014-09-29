/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import util.access.FieldValue.EnumerableValue;

/**
 * {@link Accessor} implementing {@link EnumerableValue}
 *
 * @author Plutonium_
 */
public class AccessorEnum<T> extends Accessor<T> implements EnumerableValue<T> {
    
    private final Supplier<List<T>> valueEnumerator;
    
    public AccessorEnum(T val, Consumer<T> applier, Supplier<List<T>> enumerator) {
        super(val, applier);
        valueEnumerator = enumerator;
    }

    @Override
    public List<T> enumerateValues() {
        return valueEnumerator.get();
    }
    
}
