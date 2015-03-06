/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.functional.functor;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * {@link Function} with additional methods for function composition.
 *
 * @author Plutonium_
 */
public interface FunctionC<IN,OUT> extends Function<IN,OUT> {
    
    /**
    @param mutator consumer that takes the input of this function and applies it
    on output of this function after this function finishes
    @return composed function that applies this function to its input and then
    mutates the output before returning it.
    */
    public default FunctionC<IN,OUT> andApply(BiConsumer<IN,OUT> mutator) {
        return in -> {
            OUT o = apply(in);
            mutator.accept(in,o);
            return o;
        };
    }
    
    /**
    Similar to {@link #andThen(java.util.function.Function)} but the mutator
    takes additional parameter - the original input to this function.
    
    @param mutator consumer that takes the input of this function and applies it
    on output of this function after this function finishes
    @return composed function that applies this function to its input and then
    applies the mutator before returning it.
    */
    public  default <OUT2> FunctionC<IN,OUT2> andThen(BiFunction<IN,OUT,OUT2> mutator) {
        return in -> {
            OUT o = apply(in);
            OUT2 o2 = mutator.apply(in,o);
            return o2;
        };
    }
}
