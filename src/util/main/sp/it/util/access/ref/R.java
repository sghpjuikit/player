package sp.it.util.access.ref;

import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * Reference - object property.
 *
 * @param <V> type of value
 */
public class R<V> {

	protected V v;

	public R(V value) {
		set(value);
	}

	public V get() {
		return v;
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

	public Optional<V> toOptional() {
		return Optional.ofNullable(v);
	}

}