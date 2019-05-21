package sp.it.util.collections

import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.FXCollections.observableSet
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.collections.SetChangeListener
import sp.it.util.reactive.onChange
import sp.it.util.type.union
import kotlin.reflect.KClass

/** @return new list containing elements of this sequence, e.g. for safe iteration */
fun <T> Sequence<T>.materialize() = toList()

/** @return new list containing elements of this list, e.g. for safe iteration */
fun <T> List<T>.materialize() = toList()

/** @return new list containing elements of this set, e.g. for safe iteration */
fun <T> Set<T>.materialize() = toList()

/** @return the most specific common supertype of all elements */
fun <E: Any> Collection<E?>.getElementType(): Class<*> {
    return asSequence().filterNotNull()
            .map { it::class as KClass<*> }.distinct()
            .fold(null as KClass<*>?) { commonType, type -> commonType?.union(type) ?: type }
            ?.java
            ?: Void::class.java
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
infix fun <T> MutableCollection<T>.setTo(elements: Array<T>) = this setTo elements.toList()

/** Removes all elements and adds specified element to this collection. Atomic for [ObservableList]. */
infix fun <T> MutableCollection<T>.setToOne(element: T) {
    if (size!=1 || first()!=element)
        this setTo listOf(element)
}

/** @return read only observable list that maintains the elements from this list mapped using the specified mapper */
fun <T,R> ObservableList<T>.project(mapper: (T) -> R): ObservableListRO<R> {
    val outBacking = observableArrayList<R>(listOf())
    outBacking setTo map(mapper)
    onChange { outBacking setTo map(mapper) }
    return ObservableListRO(outBacking)
}

/** @return read only observable set that maintains the elements from this set mapped using the specified mapper */
fun <T,R> ObservableSet<T>.project(mapper: (T) -> R): ObservableSetRO<R> {
    val outBacking = observableSet<R>()
    outBacking setTo map(mapper)
    onChange { outBacking setTo map(mapper) }
    return ObservableSetRO(outBacking)
}

/** Type safe [ObservableList] implemented by delegation as [List] that is [Observable]. */
class ObservableListRO<T>(private val list: ObservableList<T>): List<T> by list, Observable {
    override fun removeListener(listener: InvalidationListener) = addListener(listener)
    override fun addListener(listener: InvalidationListener) = list.addListener(listener)
    fun addListener(listener: ListChangeListener<in T>) = list.addListener(listener)
    fun removeListener(listener: ListChangeListener<in T>) = list.removeListener(listener)
}

/** Type safe [ObservableSet] implemented by delegation as [Set] that is [Observable]. */
class ObservableSetRO<T>(private val set: ObservableSet<T>): Set<T> by set, Observable {
    override fun removeListener(listener: InvalidationListener) = addListener(listener)
    override fun addListener(listener: InvalidationListener) = set.addListener(listener)
    fun addListener(listener: SetChangeListener<in T>) = set.addListener(listener)
    fun removeListener(listener: SetChangeListener<in T>) = set.removeListener(listener)
}