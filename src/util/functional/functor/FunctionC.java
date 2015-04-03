/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.functional.functor;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.value.WritableValue;
import javafx.util.Callback;

/**
 * Unification of {@link Callback) and {@link Function} with additional methods 
 * for function composition.
 * 
 *
 * @author Plutonium_
 */
@FunctionalInterface
public interface FunctionC<IN,OUT> extends Function<IN,OUT>, Callback<IN,OUT> {
    
    /** 
    Functional method of {@link Callback}.
    Equivalent to {@code apply(in);}.
    Do not override. */
    @Override
    public default OUT call(IN in) {
        return apply(in);
    }
    
    /**
    @param mutator consumer that takes the input of this function and applies it
    on output of this function after this function finishes
    @return composed function that applies this function to its input and then
    mutates the output before returning it.
    */
    public default FunctionC<IN,OUT> andApply(Consumer<OUT> mutator) {
        return in -> {
            OUT o = apply(in);
            mutator.accept(o);
            return o;
        };
    }
    
    /**
    Similar to {@link #andApply(java.util.function.Consumer)} but the mutator takes
    additional parameter - initial input of this function.
    
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
            return mutator.apply(in,o);
        };
    }
    
    /** Converts callback into composable one. */
    public static<IN,OUT> FunctionC<IN,OUT> composable(Callback<IN,OUT> c) {
        return c::call;
    }
    
    /** Converts callback into composable one. */
    public static<IN,OUT> FunctionC<IN,OUT> composable(Function<IN,OUT> c) {
        return c::apply;
    }
    
    /** Equivalent to {@code composable(f).andApply(m);}. */
    public static<IN,OUT> FunctionC<IN,OUT> composeF(Function<IN,OUT> f, Consumer<OUT> m) {
        return composable(f).andApply(m);
    }
    
    /** Equivalent to {@code return composable(f).andApply(m);}. */
    public static<IN,OUT> FunctionC<IN,OUT> composeC(Callback<IN,OUT> f, Consumer<OUT> m) {
        return composable(f).andApply(m);
    }
    
    /** Equivalent to {@code f.setValue(composable(f.getValue()).andApply(m));}. */
    public static<IN,OUT> void composeF(WritableValue<Function<IN,OUT>> f, Consumer<OUT> m) {
        f.setValue(composable(f.getValue()).andApply(m));
    }
    
    /** Equivalent to {@code cf.setValue(composable(f.getValue()).andApply(m));}. */
    public static<IN,OUT> void composeC(WritableValue<Callback<IN,OUT>> f, Consumer<OUT> m) {
        f.setValue(composable(f.getValue()).andApply(m));
    }    
    
}
