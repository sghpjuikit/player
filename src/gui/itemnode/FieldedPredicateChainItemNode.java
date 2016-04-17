/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.itemnode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;
import java.util.stream.Stream;

import util.access.fieldvalue.ObjectField;
import util.collections.Tuple3;

import static util.functional.Util.*;

/**
 * Chained filter node producing {@link util.access.fieldvalue.ObjectField} predicate.
 *
 * @author Martin Polakovic
 */
public class FieldedPredicateChainItemNode<T,F extends ObjectField<T>> extends ChainValueNode<Predicate<T>,FieldedPredicateItemNode<T,F>> {

    private final List<Tuple3<String,Class,F>> data = new ArrayList<>();

    public FieldedPredicateChainItemNode(Supplier<FieldedPredicateItemNode<T,F>> chainedFactory) {
        this(1,chainedFactory);
    }

    public FieldedPredicateChainItemNode(int i, Supplier<FieldedPredicateItemNode<T,F>> chainedFactory) {
        super(i, chainedFactory);

        inconsistent_state = false;
        generateValue();
    }

    protected List<Tuple3<String,Class,F>> getData() {
        return data;
    }

    public void setData(List<Tuple3<String,Class,F>> classes) {
        inconsistent_state = true;
        data.clear(); // causes serious problems, unknown
        data.addAll(classes);
        chain.forEach(g -> g.chained.setData(classes));
        clear(); // bug fix, not sure if it does not cause problems
    }

    public void setPrefTypeSupplier(Supplier<Tuple3<String,Class,F>> supplier) {
        chain.forEach(g -> g.chained.setPrefTypeSupplier(supplier));
    }

    public boolean isEmpty() {
        return chain.stream().allMatch(c -> c.chained.isEmpty());
    }

    public void clear() {
        inconsistent_state = true;
        chain.setAll(chain.get(0));
        chain.forEach(c -> c.chained.clear());
        inconsistent_state = false;
        generateValue();
    }

    @Override
    protected Predicate<T> reduce(Stream<Predicate<T>> values) {
        return values.reduce(Predicate::and).orElse((Predicate)IS);
    }

}