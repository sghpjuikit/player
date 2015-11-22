/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.collections.ObservableList;

import util.access.FieldValue.EnumerableValue;

import static java.util.Arrays.asList;

/**
 * Accessor which can list all its possible values.
 * {@link V} implementing {@link EnumerableValue}.
 * <p>
 *
 * @param <T> the value. The value should have properly implemented toString
 * method for popuating controls like ComboBox with human readable text.
 */
public class VarEnum<T> extends V<T> implements EnumerableValue<T> {
    
    private final Supplier<Collection<T>> valueEnumerator;
    
    public VarEnum(T val, Supplier<Collection<T>> enumerator) {
        super(val);
        valueEnumerator = enumerator;
    }
    
    public VarEnum(T val, Consumer<T> applier, Supplier<Collection<T>> enumerator) {
        super(val, applier);
        valueEnumerator = enumerator;
    }
    
    public VarEnum(T val, ObservableList<T> enumerated) {
        super(val);
        valueEnumerator = () -> enumerated;
    }
    
    public VarEnum(Supplier<T[]> enumerator, T val) {
        super(val);
        valueEnumerator = () -> asList(enumerator.get());
    }

    /** {@inheritDoc} */
    @Override
    public Collection<T> enumerateValues() {
        return valueEnumerator.get();
    }
    
}
