/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities.functional.functor;

/**
 * A functor that takes no arguments and returns no value.
 * 
 * @author uranium
 */
@FunctionalInterface
public interface Procedure extends Runnable {
    
    /** Execute this procedure. */
    @Override
    public void run();
    
    /** @return procedure that does nothing. */
    public static Procedure DO_NOTHING() {
        return () -> {};
    }
}
