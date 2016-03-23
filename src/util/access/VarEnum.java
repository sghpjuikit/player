/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.collections.ObservableList;

import util.access.fieldvalue.EnumerableValue;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static util.functional.Util.forEach;

/**
 * Accessor which can enumerate all its possible values - implementing {@link EnumerableValue}.
 *
 * @param <T> the value. The value should have properly implemented toString
 * method for populating controls like ComboBox with human readable text.
 */
public class VarEnum<T> extends V<T> implements EnumerableValue<T> {

    public static <V> VarEnum<V> ofStream(V val, Supplier<Stream<V>> enumerator) {
        return new VarEnum<>(val, () -> enumerator.get().collect(toList()));
    }


    private final Supplier<Collection<T>> valueEnumerator;

    public VarEnum(T val, Supplier<Collection<T>> enumerator) {
        super(val);
        valueEnumerator = enumerator;
    }

    public VarEnum(T val, ObservableList<T> enumerated) {
        super(val);
        valueEnumerator = () -> enumerated;
    }

    /** Performance optimization of variadic {@link #VarEnum(Object, java.util.function.Supplier, java.util.function.Consumer[])}. */
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