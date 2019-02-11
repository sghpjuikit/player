package sp.it.pl.util.access;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface FAccessibleValue<T> extends AccessibleValue<T> {

	@Override
	default T getValue() {
		return getGetter().get();
	}

	@Override
	default void setValue(T val) {
		getSetter().accept(val);
	}

	Consumer<T> getSetter();

	Supplier<T> getGetter();

}