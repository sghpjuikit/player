/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.reactive;

import java.util.HashSet;

import org.reactfx.Subscription;

/**
 * Set of runnables. For use as a collection of handlers.
 * 
 * @author Plutonium_
 */
public class RunnableSet extends HashSet<Runnable> implements Runnable {


    public RunnableSet() {
        super(2);
    }

    @Override
    public void run() {
        forEach(Runnable::run);
    }

    public Subscription addS(Runnable r) {
        add(r);
        return () -> remove(r);
    }

}
