package sp.it.pl.util.access.fieldvalue

import sp.it.pl.util.access.TypedValue
import sp.it.pl.util.functional.Util.by
import sp.it.pl.util.functional.nullsLast
import java.util.*

/**
 * @param <V> type of value this field extracts from
 * @param <T> type of this field and the type of the extracted value
 */
interface ObjectField<V, T>: TypedValue<T>, StringGetter<V> {

    /**
     * Returns whether this value has human readable string representation. This
     * denotes, whether this type should be attempted to be displayed as text (not if it is String),
     * e.g., when generating generic table columns.
     *
     * The type does not have to be String for this field to be string representable. Any type
     * can be string representable as long as it provides a string converter producing human
     * readable string (compact enough to be used in gui such as tables). Example of string field
     * that is not string representable would be a fulltext field - field that is a concatenation
     * of all string fields, used for fulltext search.
     *
     * Default implementation returns true.
     *
     * @return whether the field can be displayed as a human readable text in a gui
     */
    fun isTypeStringRepresentable(): Boolean = true

    fun getOf(value: V): T?

    override fun getOfS(value: V, substitute: String): String = toS(getOf(value), substitute)

    /** Returns description of the field. */
    fun description(): String

    /** Returns name of the field. */
    fun name(): String

    /**
     * Used as string converter for fielded values. For example in tables.
     * When the object signifies empty value, a substitute is returned.
     */
    fun toS(o: T?, substitute: String): String

    /**
     * Returns a comparator comparing by the value extracted by this field or [util.functional.Util.SAME] if
     * this field does not extract [java.lang.Comparable] type.
     *
     * Note, that because value returned by [.getOf] can be null, comparator this method returns may
     * be unsafe - throw [java.lang.NullPointerException] when comparing null values - and must be guarded, by
     * providing transformer that makes it null safe.
     *
     * @param comparatorTransformer function that transforms the underlying null-unsafe comparator into null-safe one
     */
    @Suppress("UNCHECKED_CAST")
    fun <C: Comparable<C>> comparator(comparatorTransformer: (Comparator<in C>) -> Comparator<in C?> = { it.nullsLast() }): Comparator<V?> {
        return if (Comparable::class.java.isAssignableFrom(type))
            by<V, C>({ o -> getOf(o) as C? }, comparatorTransformer)
        else
            sp.it.pl.util.functional.Util.SAME as Comparator<V?>
    }

    fun <C: Comparable<C>> comparator(): Comparator<V?> = comparator<C> { it.nullsLast() }

    fun toS(v: V, o: T?, substitute: String): String = toS(o, substitute)

    /**
     * Variation of [.toString] method.
     * Converts first letter of the string to upper case.
     */
    fun toStringCapital(): String {
        val s = toString()
        return if (s.isEmpty()) "" else s.substring(0, 1).toUpperCase()+s.substring(1)
    }

    /**
     * Variation of [.toString] method.
     * Converts first letter of the string to upper case and all others into lower case.
     */
    fun toStringCapitalCase(): String {
        val s = toString()
        return if (s.isEmpty()) "" else s.substring(0, 1).toUpperCase()+s.substring(1).toLowerCase()
    }

    /**
     * Variation of [.toString] method.
     * Converts first letter of the string to upper case and all others into lower case and replaces all '_' with ' '.
     *
     * Use to make [Enum] constants more human readable, for gui for example.
     */
    fun toStringEnum(): String {
        val s = toString().replace("_".toRegex(), " ")
        return if (s.isEmpty()) "" else s.substring(0, 1).toUpperCase()+s.substring(1).toLowerCase()
    }

    fun isTypeFilterable(): Boolean = true

    fun searchSupported(): Boolean = isTypeStringRepresentable()

    fun searchMatch(matcher: (String) -> Boolean): (V) -> Boolean = { matcher(getOfS(it, "")) }

    fun cWidth(): Double = 70.0

    fun cVisible(): Boolean = true

    fun cOrder(): Int = if (this is Enum<*>) ordinal else 1

}