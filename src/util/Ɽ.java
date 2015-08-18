/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.util.function.Supplier;

import util.functional.Functors.Ƒ1;

/**
 *
 * @author Plutonium_
 */
public class Ɽ<T> {
    
    protected T t;
    
    public T get() {
        return t;
    }
    
    public T get(T or) {
        if(t==null) t = or;
        return t;
    }
    
    public T get(Supplier<T> or) {
        if(t==null) t = or.get();
        return t;
    }
    
    public <M> T get(M m, Ƒ1<M,T> or) {
        if(t==null) t = or.apply(m);
        return t;
    }
    
    public boolean isØ() {
        return t == null;
    }
    
    /** Sets instance to null*/
    public void setØ() {
        t = null;
    }
}
