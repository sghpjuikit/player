package sp.it.util.collections

import javafx.collections.ObservableList
import sp.it.util.type.union
import kotlin.reflect.KClass

/** @return new list containing elements of this sequence, e.g. for safe iteration */
fun <T> Sequence<T>.materialize() = toList()

/** @return new list containing elements of this list, e.g. for safe iteration */
fun <T> List<T>.materialize() = toList()

/** @return new set containing elements of this set, e.g. for safe iteration */
fun <T> Set<T>.materialize() = toSet()

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
    if (this is ObservableList<T>)
        this.setAll(if (elements is MutableCollection<T>) elements else ArrayList(elements))
    else {
        this.clear()
        this += elements
    }
}

/** Removes all elements and adds all specified elements to this collection. Atomic for [ObservableList]. */
infix fun <T> MutableCollection<T>.setTo(elements: Sequence<T>) = this setTo elements.toList()

/** Removes all elements and adds all specified elements to this collection. Atomic for [ObservableList]. */
infix fun <T> MutableCollection<T>.setTo(elements: Array<T>) = this setTo elements.toList()

/** Removes all elements and adds specified element to this collection. Atomic for [ObservableList]. */
infix fun <T> MutableCollection<T>.setToOne(element: T) = this setTo listOf(element)