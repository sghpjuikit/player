package util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Lazy reference.
 *
 * @param <V> type of instance
 *
 * Created by Plutonium_ on 3/12/2016.
 */
public class LazyR<V> extends R<V> {

    protected Supplier<V> builder;

    /**
     * @param builder produces the instance when it is frst accessed
     */
    public LazyR(Supplier<V> builder) {
        Objects.requireNonNull(builder);
        this.builder = builder;
    }

    @Override
    public V get() {
        if (isSet) {
            set(builder.get());
            builder = null;
        }
        return t;
    }
}