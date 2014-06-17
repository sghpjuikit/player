
package utilities.functional.functor;

import java.util.function.Consumer;

/**
 * A functor that takes one argument and returns no value - unary procedure.
 * Extends Consumer<T> interface to retain compatibiltity
 * 
 * @author Plutonium_
 */
@FunctionalInterface
public interface UnProcedure<T> extends Consumer<T>{
    
    /** Execute this procedure. */
    @Override
    void accept(T s);
}
