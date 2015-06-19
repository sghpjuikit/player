/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access;

import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.collections.ObservableList;
import util.access.FieldValue.EnumerableValue;

/**
 * Accessor which can list all its possible values.
 * {@link Accessor} implementing {@link EnumerableValue}.
 * <p>
 *
 * @param <T> the value. The value should have properly implemented toString
 * method for popuating controls like ComboBox with human readable text.
 */
public class AccessorEnum<T> extends Accessor<T> implements EnumerableValue<T> {
    
    private final Supplier<Collection<T>> valueEnumerator;
    
    public AccessorEnum(T val, Supplier<Collection<T>> enumerator) {
        super(val);
        valueEnumerator = enumerator;
    }
    public AccessorEnum(T val, Consumer<T> applier, Supplier<Collection<T>> enumerator) {
        super(val, applier);
        valueEnumerator = enumerator;
    }
    public AccessorEnum(T val, ObservableList<T> enumerated) {
        super(val);
        valueEnumerator = () -> enumerated;
    }
    public AccessorEnum(Supplier<T[]> enumerator, T val) {
        super(val);
        valueEnumerator = () -> asList(enumerator.get());
    }

    /** {@inheritDoc} */
    @Override
    public Collection<T> enumerateValues() {
        return valueEnumerator.get();
    }
    
}
