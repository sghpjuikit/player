package util.parsing;

import util.functional.Functors.Ƒ1;

/**
 * Object to String converter.
 */
public interface ToStringConverter<T> extends Ƒ1<T,String> {

	/**
	 * Converts object into string.
	 *
	 * @return String the object has been converted.
	 */
	String toS(T object);

	@Override
	default String apply(T t) {
		return toS(t);
	}
}