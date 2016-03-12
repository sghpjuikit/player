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
    protected boolean isSet = false;

    /**
     * @param builder produces the instance when it is frst accessed
     */
    public LazyR(Supplier<V> builder) {
        Objects.requireNonNull(builder);
        this.builder = builder;
    }

    @Override
    public V get() {
        if (!isSet) {
            set(builder.get());
            builder = null;
        }
        return v;
    }

    @Override
    public void set(V val) {
        isSet = true;
        super.set(val);
    }

    public V get(V or) {
        if(!isSet) set(or);
        return v;
    }

    public V get(Supplier<V> or) {
        if(!isSet) set(or.get());
        return v;
    }

    public boolean isSet() {
        return isSet;
    }
}