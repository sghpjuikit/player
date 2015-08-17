/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Mutable lazy singleton object implementation.
 * <p>
 * Provides access to single object instance, which can be created lazily - 
 * when requested for the first time. Additionally, the the object can be 
 * mutated (its state changed) before it is accessed. This allows a reuse of
 * the object across different objects that use it.  
 * 
 * @param <T> type of instance
 * @param <M> type of object the instance relies on
 *
 * @author Plutonium_
 */
public class SingleⱤ<T,M> extends Ɽ<T> {
    
    private final Supplier<T> builder;
    private final BiConsumer<T,M> mutator;
    
    /**
     * 
     * @param builder produces the instance when it is requested.
     */
    public SingleⱤ(Supplier<T> builder) {
        this(builder, null);
    }
    
    /**
     * 
     * @param builder produces the instance when it is requested.
     * @param mutator mutates instance's state for certain dependency object. use
     * null if no mutation is desired.
     */
    public SingleⱤ(Supplier<T> builder, BiConsumer<T,M> mutator) {
        Objects.requireNonNull(builder);
        
        this.builder = builder;
        this.mutator = mutator;
    }
    
    /**
     * Use when the state of the instance does not matter
     * @return instance as is, but never null..
     */
    @Override
    public T get() {
        // initialize instance
        if (t == null) t = builder.get();
        
        assert t!=null;
        
        return t;
    }
    
    /**
     * @param mutation_source, use null when type Void
     * @return the instance after applying mutation, ever null
     */
    public T getM(M mutation_source) {
        // initialize instance
        if (t == null) t = builder.get();
        
        assert t!=null;
        
        // prepare instance
        if (mutator != null) mutator.accept(t, mutation_source);
        
        return t;
    }
}
