/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.reactive;

/**
 <p>
 @author Plutonium_
 */
public class ValueEventSourceN<T> extends ValueEventSource<T> {
    private T v;
    private T empty_val;

    public ValueEventSourceN(T initialValue) {
        super(initialValue);
        empty_val = initialValue;
    }

    @Override
    public void push(T event) {
        super.push(event==null ? empty_val : event);
    }
}