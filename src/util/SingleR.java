package util;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Mutable lazy singleton object implementation.
 * <p/>
 * Provides access to single object instance, which can be created lazily -
 * when requested for the first time. Additionally, the the object can be
 * mutated (its state changed) before it is accessed. This allows a reuse of
 * the object across different objects that use it.
 *
 * @param <V> type of instance
 * @param <M> type of object the instance relies on
 *
 * @author Martin Polakovic
 */
public class SingleR<V,M> extends LazyR<V> {

    private final BiConsumer<V,M> mutator;

    /**
     *
     * @param builder produces the instance when it is requested.
     */
    public SingleR(Supplier<V> builder) {
        this(builder, null);
    }

    /**
     *
     * @param builder produces the instance when it is requested.
     * @param mutator mutates instance's state for certain dependency object. use
     * null if no mutation is desired.
     */
    public SingleR(Supplier<V> builder, BiConsumer<V,M> mutator) {
        super(builder);
        this.mutator = mutator;
    }

    /**
     * Same as {@link #get()}, but mutates the value.
     * @param mutation_source, use null when type Void
     * @return the instance after applying mutation, ever null
     */
    public V getM(M mutation_source) {
        V v = get();
        if (mutator != null) mutator.accept(v, mutation_source);
        return v;
    }

    @Override
    public void set(V val) {
        if (isSet) throw new IllegalArgumentException("Singleton instance already set.");
        super.set(val);
    }
}