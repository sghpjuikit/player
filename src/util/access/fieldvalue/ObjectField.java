package util.access.fieldvalue;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import util.SwitchException;
import util.access.TypedValue;
import static util.dev.Util.noØ;
import static util.functional.Util.by;

/**
 * @param <V> type of value this field extracts from
 * @param <T> type of this field and the type of the extracted value
 * @author Martin Polakovic
 */
public interface ObjectField<V, T> extends TypedValue<T> {

	T getOf(V value);

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
	 * When the object signifies empty value, empty string is returned.
	 */
	String toS(T o, String empty_val);

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

	default String toS(V v, T o, String empty_val) {
		return ObjectField.this.toS(o, empty_val);
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

	default double c_width() {
		return 70;
	}

	default boolean c_visible() {
		return true;
	}

	default int c_order() {
		return (this instanceof Enum) ? ((Enum) this).ordinal() : 1;
	}

	class ColumnField implements ObjectField<Object,Integer> {

		public static final Set<ColumnField> FIELDS = new HashSet<>();
		public static final ColumnField INDEX = new ColumnField("#", "Index of the item in the list");

		private final String name;
		private final String description;

		ColumnField(String name, String description) {
			this.name = name;
			this.description = description;
			FIELDS.add(this);
		}

		public static ColumnField valueOf(String s) {
			if (INDEX.name().equals(s)) return INDEX;
			throw new SwitchException(s);
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public Integer getOf(Object value) {
			return null;
		}

		@Override
		public String description() {
			return description;
		}

		@Override
		public String toS(Integer o, String empty_val) {
			return "";
		}

		@Override
		public Class<Integer> getType() {
			return Integer.class;
		}
	}

}