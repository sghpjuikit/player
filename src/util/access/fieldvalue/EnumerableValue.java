package util.access.fieldvalue;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Value that is included in an enumeration can list its possible values, which it is restricted to.
 */
public interface EnumerableValue<T> {

	/**
	 * Provides list of all currently available values. The list can differ if constructed at different time.
	 */
	Collection<T> enumerateValues();

	default Stream<T> streamValues() {
		return enumerateValues().stream();
	}
}
