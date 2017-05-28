package util.access.fieldvalue;

public interface StringGetter<V> {

	/**
	 * Converts the value to a string representation.
	 * When the object signifies empty value, a substitute is returned. The semantics of empty is left up for
	 * implementation.
	 *
	 * @param value original object
	 * @param substitute text used when the original object is 'empty'.
	 * @return string representation of the original object
	 */
	String getOfS(V value, String substitute);

}