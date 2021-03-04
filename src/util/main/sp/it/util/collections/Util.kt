package sp.it.util.collections

import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.collections.FXCollections
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.FXCollections.observableSet
import javafx.collections.ListChangeListener
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet
import javafx.collections.SetChangeListener
import sp.it.util.functional.Try
import sp.it.util.functional.getOr
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.onChange
import sp.it.util.type.raw
import sp.it.util.type.union
import java.util.Optional
import java.util.Stack
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection.Companion.STAR
import kotlin.reflect.full.createType
import kotlin.reflect.full.withNullability
import sp.it.util.type.kTypeNothingNonNull
import sp.it.util.type.kTypeNothingNullable

/** @return new list containing elements of this sequence, e.g. for safe iteration */
fun <T> Sequence<T>.materialize() = toList()

/** @return new list containing elements of this list, e.g. for safe iteration */
fun <T> List<T>.materialize() = toList()

/** @return new set containing elements of this set, e.g. for safe iteration */
fun <T> Set<T>.materialize() = toSet()

/** @return new map containing elements of this map, e.g. for safe iteration */
fun <K, V> Map<K, V>.materialize() = toMap()

fun <K,V> Map<K,V>.toStringPretty(): String = asSequence().joinToString("\n", "{\n", "\n}") { "$it" }

fun <T> Iterable<T>.toStringPretty(): String = asSequence().joinToString("\n", "(\n", "\n)") { "$it" }

/** @return the most specific common supertype of all elements */
fun <E: Any> Collection<E?>.getElementType(): KType = when {
   isEmpty() -> kTypeNothingNonNull()
   all { it===null } -> kTypeNothingNullable()
   else -> {
      null
         ?: run {
            // Try obtaining exact type by inspecting the class
            runTry {
               this::class.supertypes.find { it.classifier==Collection::class }
                  ?.arguments?.getOrNull(0)?.type
                  ?.withNullability(null in this)
            }.orNull()
         }
         ?: run {
            // Find lowest common element type
            fun KClass<*>.estimateType() = createType(typeParameters.map { STAR })
            asSequence().filterNotNull()
               .map { it::class }.distinct()
               .fold(null as KClass<*>?) { commonType, type -> commonType?.union(type) ?: type }!!
               .estimateType()
               .withNullability(null in this)
         }
   }
}

/** @return the most specific common supertype of all elements */
fun <E: Any> Collection<E?>.getElementClass(): Class<*> = getElementType().raw.javaObjectType

/** Wraps the specified object into a collection */
fun collectionWrap(o: Any?): Collection<Any?> = o as? Collection<Any?> ?: listOf(o)

/** Unwraps the specified object into ordinary object */
fun collectionUnwrap(o: Any?): Any? = when (o) {
   is Collection<*> -> {
      when (o.size) {
         0 -> null
         1 -> o.first()
         else -> o
      }
   }
   is Optional<*> -> o.orNull()
   is Try<*, *> -> o.getOr(o)
   is Result<*> -> o.getOrDefault(o)
   else -> o
}

/** @return stack of the specified elements stacked from first on bottom */
fun <T> stackOf(vararg elements: T): Stack<T> = elements.toCollection(Stack())

/** @return sequence containing specified number of elements generated by their index starting from 0 */
fun <T> tabulate0(size: Int, element: (Int) -> T) = (0 until size).asSequence().map(element)

/** @return sequence containing specified number of elements generated by their index starting from 1 */
fun <T> tabulate1(size: Int, element: (Int) -> T) = (1 until size + 1).asSequence().map(element)

/** @return sequence containing the computed element after every nth element (specified in original sequence order) */
fun <T, T1: T, T2: T> Sequence<T1>.insertEvery(nth: Int, prefix: Boolean = false, suffix: Boolean = false, element: () -> T2): Sequence<T> = sequence {
   if (prefix) yield(element())
   this@insertEvery.forEachIndexed { i, it ->
      if (i%nth==0 && i>0) yield(element())
      yield(it)
   }
   if (suffix) yield(element())
}

/** Removes all elements and adds all specified elements to this collection. Atomic for [ObservableList]. */
@Suppress("DEPRECATION")
infix fun <T> MutableCollection<T>.setTo(elements: Collection<T>) {
   if (this is ObservableList<T>) {
      this.setAll(if (elements is MutableCollection<T>) elements else ArrayList(elements))
   } else {
      this.clear()
      this += elements
   }
}

/** Removes all elements and adds all specified elements to this collection. Atomic for [ObservableList]. */
infix fun <T> MutableCollection<T>.setTo(elements: Sequence<T>) = this setTo elements.toList()

/** Removes all elements and adds all specified elements to this collection. Atomic for [ObservableList]. */
infix fun <T> MutableCollection<T>.setTo(elements: Array<out T>) = this setTo elements.toList()

/** Removes all elements and adds specified element to this collection. Atomic for [ObservableList]. */
infix fun <T> MutableCollection<T>.setToOne(element: T) {
   if (size!=1 || first()!=element)
      this setTo listOf(element)
}

/** @return read-only observable list that maintains the elements from this list mapped using the specified mapper */
fun <T, R, LIST> LIST.project(mapper: (T) -> R): ObservableListRO<R> where LIST: List<T>, LIST: Observable {
   val outBacking = observableList<R>()
   outBacking setTo map(mapper)
   onChange { outBacking setTo map(mapper) }
   return ObservableListRO(outBacking)
}

/** @return read-only observable set that maintains the elements from this set mapped using the specified mapper */
fun <T, R, SET> SET.project(mapper: (T) -> R): ObservableSetRO<R> where SET: Set<T>, SET: Observable {
   val outBacking = observableSet<R>()
   outBacking setTo map(mapper)
   onChange { outBacking setTo map(mapper) }
   return ObservableSetRO(outBacking)
}

/** Type safe read-only [ObservableList] implemented by delegation as [List] that is [Observable]. */
class ObservableListRO<T>(private val list: ObservableList<T>): List<T> by list, Observable by list {
   fun addListener(listener: ListChangeListener<in T>) = list.addListener(listener)
   fun removeListener(listener: ListChangeListener<in T>) = list.removeListener(listener)
   fun toJavaFx(): ObservableList<T> = observableArrayList(this).also {
      onChange { it setTo this }
   }
}

/** Type safe read-only [ObservableSet] implemented by delegation as [Set] that is [Observable]. */
class ObservableSetRO<T>(private val set: ObservableSet<T>): Set<T> by set, Observable by set{
   override fun removeListener(listener: InvalidationListener) = addListener(listener)
   override fun addListener(listener: InvalidationListener) = set.addListener(listener)
   fun addListener(listener: SetChangeListener<in T>) = set.addListener(listener)
   fun removeListener(listener: SetChangeListener<in T>) = set.removeListener(listener)
   fun toJavaFx(): ObservableSet<T> = observableSet(this).also {
      onChange { it setTo this }
   }
}

/** Type safe read-only [ObservableMap] implemented by delegation as [Map] that is [Observable]. */
class ObservableMapRO<K,V>(private val map: ObservableMap<K,V>): Map<K,V> by map, Observable by map {
   override fun removeListener(listener: InvalidationListener) = addListener(listener)
   override fun addListener(listener: InvalidationListener) = map.addListener(listener)
   fun addListener(listener: MapChangeListener<in K, in V>) = map.addListener(listener)
   fun removeListener(listener: MapChangeListener<in K, in V>) = map.removeListener(listener)
}

/** @return mutable observable list */
fun <T> observableList(): ObservableList<T> = observableArrayList()

/** @return mutable observable set */
fun <T> observableSet(): ObservableSet<T> = observableSet()

/** @return mutable observable map */
fun <K,V> observableMap(): ObservableMap<K,V> = FXCollections.observableHashMap()

fun <T> ObservableList<T>.readOnly() = ObservableListRO(this)

fun <T> ObservableSet<T>.readOnly() = ObservableSetRO(this)

/** Returns a map containing all key-value pairs with not null keys. */
@Suppress("UNCHECKED_CAST")
fun <K: Any, V> Map<K?, V>.filterNotNullKeys(): Map<K, V> = filterKeys { it!=null } as Map<K, V>

/** Returns a map containing all key-value pairs with not null values. */
@Suppress("UNCHECKED_CAST")
fun <K: Any, V> Map<K, V?>.filterNotNullValues(): Map<K, V> = filterValues { it!=null } as Map<K, V>