package util.access.fieldvalue;

import java.util.Comparator;
import java.util.function.Function;
import util.access.TypedValue;
import static util.dev.Util.noØ;
import static util.functional.Util.by;

/**
 * @param <V> type of value this field extracts from
 * @param <T> type of this field and the type of the extracted value
 */
public interface ObjectField<V, T> extends TypedValue<T>, StringGetter<V> {

	T getOf(V value);

	@Override
	default String getOfS(V value, String substitute) {
		return toS(getOf(value), substitute);
	}

	/** Returns description of the field. */
	String description();

	/** Returns name of the field. */
	String name();

	/**
	 * Returns whether this value has human readable string representation. This
	 * denotes, whether this type should be attempted to be displayed as text (not if it is String),
	 * e.g., when generating generic table columns.
	 * <p/>
	 * The type does not have to be String for this field to be string representable. Any type
	 * can be string representable as long as it provides a string converter producing human
	 * readable string (compact enough to be used in gui such as tables). Example of string field
	 * that is not string representable would be a fulltext field - field that is a concatenation
	 * of all string fields, used for fulltext search.
	 * <p/>
	 * Default implementation returns true;
	 *
	 * @return whether the field can be displayed as a human readable text in a gui
	 */
	default boolean isTypeStringRepresentable() {
		return true;
	}

	/**
	 * Used as string converter for fielded values. For example in tables.
	 * When the object signifies empty value, a substitute is returned.
	 */
	String toS(T o, String substitute);

	/**
	 * Returns a comparator comparing by the value extracted by this field or {@link util.functional.Util#SAME} if
	 * this field does not extract {@link java.lang.Comparable} type.
	 * <p/>
	 * Comparator treats nulls as per {@link Comparator#nullsLast(java.util.Comparator)}.
	 */
	@SuppressWarnings("unchecked")
	default Comparator<V> comparator() {
		return comparator(Comparator::nullsLast);
	}

	/**
	 * Returns a comparator comparing by the value extracted by this field or {@link util.functional.Util#SAME} if
	 * this field does not extract {@link java.lang.Comparable} type.
	 * <p/>
	 * Note, that because value returned by {@link #getOf(Object)} can be null, comparator this method returns may
	 * be unsafe - throw {@link java.lang.NullPointerException} when comparing null values - and must be guarded, by
	 * providing transformer that makes it null safe.
	 *
	 * @param comparatorTransformer non null function that transforms the underlying comparator to be used. At minimum
	 * it must handle null logic such as it does not permit the underlying comparator to compare null.
	 */
	@SuppressWarnings("unchecked")
	default <C extends Comparable<? super C>> Comparator<V> comparator(Function<Comparator<? super C>,Comparator<? super C>> comparatorTransformer) {
		noØ(comparatorTransformer);
		return Comparable.class.isAssignableFrom(getType())
				? by(o -> (C) getOf(o), comparatorTransformer)
				: (Comparator) util.functional.Util.SAME;
	}

	default String toS(V v, T o, String substitute) {
		return ObjectField.this.toS(o, substitute);
	}

	/**
	 * Variation of {@link #toString()} method.
	 * Converts first letter of the string to upper case.
	 */
	default String toStringCapital() {
		String s = toString();
		return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	/**
	 * Variation of {@link #toString()} method.
	 * Converts first letter of the string to upper case and all others into
	 * lower case.
	 */
	default String toStringCapitalCase() {
		String s = toString();
		return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	/**
	 * Variation of {@link #toString()} method.
	 * Converts first letter of the string to upper case and all others into
	 * lower case and replaces all '_' with ' '.
	 * <p/>
	 * Use to make {@link Enum} constants more human readable, for gui for example.
	 */
	default String toStringEnum() {
		String s = toString().replaceAll("_", " ");
		return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	default double cWidth() {
		return 70;
	}

	default boolean cVisible() {
		return true;
	}

	default int cOrder() {
		return (this instanceof Enum) ? ((Enum) this).ordinal() : 1;
	}

}