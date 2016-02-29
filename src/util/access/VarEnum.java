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
import static util.functional.Util.forEach;

/**
 * Accessor which can enumerate all its possible values - implementing {@link EnumerableValue}.
 *
 * @param <T> the value. The value should have properly implemented toString
 * method for populating controls like ComboBox with human readable text.
 */
public class VarEnum<T> extends V<T> implements EnumerableValue<T> {

    private final Supplier<Collection<T>> valueEnumerator;

    public VarEnum(T val, Supplier<Collection<T>> enumerator) {
        super(val);
        valueEnumerator = enumerator;
    }

    public VarEnum(T val, ObservableList<T> enumerated) {
        super(val);
        valueEnumerator = () -> enumerated;
    }

    public VarEnum(T val, Supplier<Collection<T>> enumerator, Consumer<T> applier) {
        super(val, applier);
        valueEnumerator = enumerator;
    }

    @SafeVarargs
    public VarEnum(T val, Supplier<Collection<T>> enumerator, Consumer<T>... appliers) {
        super(val, t -> forEach(appliers, applier -> applier.accept(t)));
        valueEnumerator = enumerator;
    }

    public VarEnum(Supplier<T[]> enumerator, T val) {
        super(val);
        valueEnumerator = () -> asList(enumerator.get());
    }

    @Override
    public Collection<T> enumerateValues() {
        return valueEnumerator.get();
    }

}