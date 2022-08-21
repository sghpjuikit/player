package sp.it.util.access.fieldvalue

import java.util.Comparator.comparing
import java.util.Comparator.naturalOrder
import java.util.function.Function
import sp.it.util.functional.Util.SAME
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.nullsFirst
import sp.it.util.functional.nullsLast
import sp.it.util.type.VType
import sp.it.util.type.isSubclassOf

/**
 * @param <V> type of value this field extracts from
 * @param <T> type of this field and the type of the extracted value
 */
interface ObjectField<in V, out T>: StringGetter<V> {

   /**
    * Returns whether this value has human-readable string representation. This
    * denotes, whether this type should be attempted to be displayed as text (not if it is String),
    * e.g., when generating generic table columns.
    *
    * The type does not have to be String for this field to be string representable. Any type
    * can be string representable as long as it provides a string converter producing human-readable
    * string (compact enough to be used in gui such as tables). Example of string field
    * that is not string representable would be a fulltext field - field that is a concatenation
    * of all string fields, used for fulltext search.
    *
    * Default implementation returns true.
    *
    * @return whether the field can be displayed as a human-readable text in a gui
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
    * Note, that because value returned by [getOf] can be null, the returned comparator must be guarded, by
    * providing transformer that makes it null safe.
    *
    * @param comparatorTransformer function that transforms the underlying null-unsafe comparator into null-safe one
    */
   fun comparator(comparatorTransformer: (Comparator<Comparable<*>>) -> Comparator<Comparable<*>?>): Comparator<@UnsafeVariance V?> = when {
      type.isSubclassOf<Comparable<*>>() -> comparing(Function { v: V? -> if (v==null) null else getOf(v) }.asIs(), comparatorTransformer(naturalOrder<Comparable<Comparable<*>>>().asIs()))
      else -> comparatorTransformer(SAME.asIs()).asIs()
   }

   fun comparatorNonNull(comparatorTransformer: (Comparator<Comparable<*>>) -> Comparator<Comparable<*>?>): Comparator<@UnsafeVariance V> = when {
      type.isSubclassOf<Comparable<*>>() && type.isNullable -> comparing(Function { v: V -> getOf(v) }.asIs(), comparatorTransformer(naturalOrder<Comparable<Comparable<*>>>().asIs()))
      type.isSubclassOf<Comparable<*>>() -> comparing(asIs<ObjectField<V,Comparable<Any>>>()::getOf)
      else -> SAME.asIs()
   }

   /** @return non-null comparator by [getOf] with [nullsFirst] order if [type] is nullable */
   fun comparatorNF(): Comparator<@UnsafeVariance V> = comparatorNonNull { it.nullsFirst() }

   /** @return non-null comparator by [getOf] with [nullsLast] order if [type] is nullable */
   fun comparatorNL(): Comparator<@UnsafeVariance V> = comparatorNonNull { it.nullsLast() }

   /** @return nullable comparator by [getOf] with [nullsFirst] order */
   fun comparatorNNF(): Comparator<@UnsafeVariance V?> = comparator { it.nullsFirst() }

   /** @return nullable comparator by [getOf] with [nullsLast] order */
   fun comparatorNNL(): Comparator<@UnsafeVariance V?> = comparator { it.nullsLast() }

   fun toS(v: V, o: @UnsafeVariance T?, substitute: String): String = toS(o, substitute)

   fun isTypeFilterable(): Boolean = true

   fun searchSupported(): Boolean = isTypeStringRepresentable()

   fun searchMatch(matcher: (String) -> Boolean): (V) -> Boolean = { matcher(getOfS(it, "")) }

   fun cWidth(): Double = 100.0

   fun cVisible(): Boolean = true

   fun cOrder(): Int = if (this is Enum<*>) ordinal else 1

   fun cName(): String = name()

   fun <R> flatMap(by: ObjectField<T, R>): ObjectFieldFlatMapped<@UnsafeVariance V, @UnsafeVariance T, R> = ObjectFieldFlatMapped(this, by)

   fun memoized(): ObjectField<V, T> = ObjectFieldMemoized(this)

}

fun <T, U> comparingMemoized(
   keyExtractor: Function<in T, out U>,
   keyComparator: java.util.Comparator<in U>
): Comparator<T> {
   return Comparator { c1: T, c2: T ->
      keyComparator.compare(
         keyExtractor.apply(c1),
         keyExtractor.apply(c2)
      )
   }
}