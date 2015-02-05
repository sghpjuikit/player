/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections;

import java.util.Objects;

/**
 * Mutable tuple.
 */
public final class TupleM4<A,B,C,D> {

    public A a;
    public B b;
    public C c;
    public D d;
    
    public TupleM4(A a, B b, C c, D d) {
        set(a, b, c, d);
    }
    
    public void set(A a, B b, C c, D d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }
    
    @Override
    public boolean equals(Object other) {
        if(other instanceof Tuple4) {
            Tuple4<?, ?, ?, ?> that = (Tuple4<?, ?, ?, ?>) other;
            return Objects.equals(this.a, that._1) &&
                   Objects.equals(this.b, that._2) &&
                   Objects.equals(this.c, that._3) &&
                   Objects.equals(this.d, that._4);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(a,b,c,d);
    }

    @Override
    public String toString() {
        return "(" + Objects.toString(a) + ", "
                   + Objects.toString(b) + ", "
                   + Objects.toString(c) + ", "
                   + Objects.toString(d)
                   + ")";
    }
}
