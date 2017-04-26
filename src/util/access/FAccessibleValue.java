package util.access;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface FAccessibleValue<T> extends ApplicableValue<T> {

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