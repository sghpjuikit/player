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
public class R<T> {

    protected T t;
    protected boolean isSet = false;

    public R() {}

    public R(T value) {
        set(value);
    }

    public T get() {
        return t;
    }

    public T get(T or) {
        if(isSet) set(or);
        return t;
    }

    public T get(Supplier<T> or) {
        if(isSet) set(or.get());
        return t;
    }

    public <M> T get(M m, Ƒ1<M,T> or) {
        if(t==null) set(or.apply(m));
        return t;
    }

    public boolean isSet() {
        return isSet;
    }

    public boolean isØ() {
        return t == null;
    }

    /** Sets value to null. */
    public void setØ() {
        set(null);
    }

    public void set(T val) {
        isSet = true;
        t = val;
    }

    public void setOf(UnaryOperator<T> op) {
        set(op.apply(get()));
    }

    public void setOf(T v2, BinaryOperator<T> op) {
        set(op.apply(get(), v2));
    }
}
