package util;

import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Reference - object property.
 *
 * @param <V> type of value
 *
 * @author Martin Polakovic
 */
public class R<V> {

    protected V v;

    public R() {}

    public R(V value) {
        set(value);
    }

    public V get() {
        return v;
    }

    public V get(V or) {
        return v;
    }

    public V get(Supplier<V> or) {
        return v;
    }

    public boolean isØ() {
        return v == null;
    }

    /** Sets value to null. */
    public void setØ() {
        set(null);
    }

    public void set(V val) {
        v = val;
    }

    public void setOf(UnaryOperator<V> op) {
        set(op.apply(get()));
    }

    public void setOf(V v2, BinaryOperator<V> op) {
        set(op.apply(get(), v2));
    }
}