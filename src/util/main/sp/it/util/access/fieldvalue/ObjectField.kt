package sp.it.util.access.fieldvalue

import sp.it.util.functional.Util.by
import sp.it.util.functional.asIs
import sp.it.util.functional.nullsLast
import sp.it.util.type.VType
import sp.it.util.type.isSubclassOf
import sp.it.util.type.rawJ
import java.util.Comparator

/**
 * @param <V> type of value this field extracts from
 * @param <T> type of this field and the type of the extracted value
 */
interface ObjectField<in V, out T>: StringGetter<V> {

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

   /** type of the value (not class of this object) */
   val type: VType<T>

   fun getOf(value: V): T

   override fun getOfS(value: V, substitute: String): String = toS(getOf(value), substitute)

   /** @return description of the field */
   fun description(): String

   /** @return name of the field */
   fun name(): String

   /**
    * Used as string converter for fielded values. For example in tables.
    * When the object signifies empty value, a substitute is returned.
    */
   fun toS(o: @UnsafeVariance T?, substitute: String): String

   /**
    * Returns a comparator comparing by the value extracted by this field or [sp.it.util.functional.Util.SAME] if
    * this field does not extract [java.lang.Comparable] type.
    *
    * Note, that because value returned by [getOf] can be null, comparator this method returns may
    * be unsafe - throw [java.lang.NullPointerException] when comparing null values - and must be guarded, by
    * providing transformer that makes it null safe.
    *
    * @param comparatorTransformer function that transforms the underlying null-unsafe comparator into null-safe one
    */
   @Suppress("UNCHECKED_CAST")
   fun <C: Comparable<C?>> comparator(comparatorTransformer: (Comparator<in C>) -> Comparator<in C?> = { it.nullsLast() }): Comparator<@UnsafeVariance V?> {
      return when {
         type.isSubclassOf<Comparable<*>>() -> by<V, C?>({ o -> getOf(o) as C? }, comparatorTransformer)
         else -> sp.it.util.functional.Util.SAME as Comparator<V?>
      }
   }

   fun <C: Comparable<C?>> comparator(): Comparator<@UnsafeVariance V?> = comparator<C> { it.nullsLast() }

   fun toS(v: V, o: @UnsafeVariance T?, substitute: String): String = toS(o, substitute)

   fun isTypeFilterable(): Boolean = true

   fun searchSupported(): Boolean = isTypeStringRepresentable()

   fun searchMatch(matcher: (String) -> Boolean): (V) -> Boolean = { matcher(getOfS(it, "")) }

   fun cWidth(): Double = 70.0

   fun cVisible(): Boolean = true

   fun cOrder(): Int = if (this is Enum<*>) ordinal else 1

}