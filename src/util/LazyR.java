package util;

import java.util.function.Supplier;
import util.functional.Functors;
import static util.dev.Util.noØ;

/**
 * Lazy reference.
 *
 * @param <V> type of instance
 * <p>
 * Created by Plutonium_ on 3/12/2016.
 */
public class LazyR<V> extends R<V> {

	protected Supplier<V> builder;
	protected boolean isSet = false;

	/**
	 * @param builder produces the instance when it is first accessed. Builder can produce null. It will never execute
	 * more than once.
	 */
	public LazyR(Supplier<V> builder) {
		noØ(builder);
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
		isSet = true; super.set(val);
	}

	@Override
	public V get(V or) {
		if (!isSet) set(or);
		return v;
	}

	@Override
	public V get(Supplier<V> or) {
		if (!isSet) set(or.get());
		return v;
	}

	public <M> V get(M m, Functors.Ƒ1<M,V> or) {
		if (!isSet) set(or.apply(m));
		return v;
	}

	public boolean isSet() {
		return isSet;
	}

}