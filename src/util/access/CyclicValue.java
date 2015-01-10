/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access;

/**
 *
 * @author Plutonium_
 */
public interface CyclicValue<E> {
    
    /**
     * Returns cycled value as defined by the implementation. The cycling might
     * not traverse all (even if finite amount of) values, it can skip or randomly
     * select value.
     */
    E cycle();
    
}
