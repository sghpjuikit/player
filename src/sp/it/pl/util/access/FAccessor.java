package sp.it.pl.util.access;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FAccessor<T> implements FAccessibleValue<T> {

	private final Consumer<T> setter;
	private final Supplier<T> getter;

	/**
	 * @param setter defines how the password will be set
	 * @param getter defines how the password will be accessed
	 */
	public FAccessor(Consumer<T> setter, Supplier<T> getter) {
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public final Consumer<T> getSetter() {
		return setter;
	}

	@Override
	public final Supplier<T> getGetter() {
		return getter;
	}

	@Override
	public final T getValue() {
		return getter.get();
	}

	@Override
	public final void setValue(T value) {
		setter.accept(value);
	}

}