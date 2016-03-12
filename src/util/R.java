/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import util.functional.Functors.Ƒ1;

/**
 * Reference - object property.
 *
 * @author Plutonium_
 */
public class R<V> {

    protected V v;

    public R() {}

    public R(V value) {
        set(value);
    }

    public V get() {
        return v;
    }

    public V get(V or) {
        return v;
    }

    public V get(Supplier<V> or) {
        return v;
    }

    public <M> V get(M m, Ƒ1<M, V> or) {
        if(v ==null) set(or.apply(m));
        return v;
    }

    public boolean isØ() {
        return v == null;
    }

    /** Sets value to null. */
    public void setØ() {
        set(null);
    }

    public void set(V val) {
        v = val;
    }

    public void setOf(UnaryOperator<V> op) {
        set(op.apply(get()));
    }

    public void setOf(V v2, BinaryOperator<V> op) {
        set(op.apply(get(), v2));
    }
}