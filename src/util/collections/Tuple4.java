/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections;

/**
 *
 * @author Plutonium_
 */
public final class Tuple4<A,B,C,D> {

    public A a;
    public B b;
    public C c;
    public D d;
    
    public Tuple4(A a, B b, C c, D d) {
        set(a, b, c, d);
    }
    
    public void set(A a, B b, C c, D d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }
}
