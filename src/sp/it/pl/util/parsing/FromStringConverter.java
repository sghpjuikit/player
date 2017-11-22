package sp.it.pl.util.parsing;

import sp.it.pl.util.functional.Try;

/**
 * String to Object converter.
 *
 * @param <T> type of object
 */
public interface FromStringConverter<T> {

	/** Converts String into object. */
	Try<T,String> ofS(String s);

	/** Returns whether string can be parsed. Equivalent to {@code ofS(s).isOk()}. */
	default boolean isParsable(String s) {
		return ofS(s).isOk();
	}
}