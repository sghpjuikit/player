package util.parsing;

/**
 * Object to String converter.
 *
 * @author Martin Polakovic
 */
public interface ToStringConverter<T> {

	/**
	 * Converts object into string.
	 *
	 * @return String the object has been converted.
	 */
	String toS(T object);
}