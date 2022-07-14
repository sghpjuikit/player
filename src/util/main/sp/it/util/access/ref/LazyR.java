package sp.it.util.access.ref;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import sp.it.util.functional.Functors.F1;
import static sp.it.util.dev.FailKt.noNull;

/**
 * Nullable lazy value.
 *
 * @param <V> type of instance
 */
public class LazyR<V> {

	protected V v;
	protected Supplier<V> builder;
	protected boolean isSet = false;

	/**
	 * @param builder produces the instance when it is first accessed. Builder can produce null. It will never execute
	 * more than once.
	 */
	public LazyR(Supplier<V> builder) {
		this.builder = noNull(builder);
	}

	public V get() {
		if (!isSet) set(builder.get());
		return v;
	}

	protected void set(V val) {
		isSet = true;
		builder = null;
		v = val;
	}

	public <M> V get(M m, F1<M,V> or) {
		if (!isSet) set(or.apply(m));
		return v;
	}

	/** @return the current value if set, otherwise the specified value */
	public V getOr(V or) {
		return isSet ? v : or;
	}

	/** @return the current value if set, otherwise null */
	public @Nullable V getOrNull() {
		return getOr((V) null);
	}

	/** @return the current value if set, otherwise the specified value */
	public V getOr(Supplier<V> or) {
		return isSet ? v : or.get();
	}

	public boolean isSet() {
		return isSet;
	}

	public void ifSet(Consumer<? super V> block) {
		if (isSet)
			block.accept(v);
	}

}