
package utilities.functional.functor;

import java.util.function.BiConsumer;

/**
 * A functor that takes two arguments and returns no value - binary procedure.
 * Extends Consumer<T> interface to retain compatibiltity
 * 
 * @author Plutonium_
 */
@FunctionalInterface
public interface BiProcedure<T,E> extends BiConsumer<T,E> {
    
    /** Execute this procedure. */
    @Override
    void accept(T s1, E s2);
}
