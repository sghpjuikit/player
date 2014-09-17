/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Mutable lazy singleton object implementation.
 * <p>
 * Provides access to single (same) instance of an object. Additionally,
 * the object is mutated before it is returned, in a process where the 
 * instance is changed based on another object.
 * <p>
 * This provides additional flexibility. The object instance can play the role
 * of many objects (if at most one is needed simultaneously) each depending on
 * different object.
 * 
 * @param <T> type of instance
 * @param <R> type of object the instance relies on. use Void if none.
 *
 * @author Plutonium_
 */
public class SingleInstance<T,R> implements Closable {
    
    private T instance;
    private final Supplier<T> builder;
    private final BiConsumer<T,R> mutator;
    
    /**
     * 
     * @param builder produces the instance when it is requested.
     */
    public SingleInstance(Supplier<T> builder) {
        this(builder, null);
    }
    
    /**
     * 
     * @param builder produces the instance when it is requested.
     * @param mutator mutates instance's state for certain dependency object. use
     * null if no mutation is desired.
     */
    public SingleInstance(Supplier<T> builder, BiConsumer<T,R> mutator) {
        Objects.requireNonNull(builder);
        
        this.builder = builder;
        this.mutator = mutator;
    }
    
    /**
     * Use when the state of the instance does not matter
     * @return instance as is, but never null..
     */
    public T get() {
        // initialize instance
        if (instance == null) instance = builder.get();
        
        assert instance!=null;
        
        return instance;
    }
    
    /**
     * @param mutationSource, use null when type Void
     * @return the instance after applying mutation, ever null
     */
    public T get( R mutationSource) {
        // initialize instance
        if (instance == null) instance = builder.get();
        
        assert instance!=null;
        
        // prepare instance
        if (mutator != null) mutator.accept(instance, mutationSource);
        
        return instance;
    }
    
    public boolean isNull() {
        return instance == null;
    }
    
    /** {@inheritDoc} */
    @Override
    public void close() {
        instance = null;
    }
}
