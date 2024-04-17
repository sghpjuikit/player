package sp.it.util.functional

import java.util.Comparator
import java.util.Comparator.comparing
import java.util.IdentityHashMap

fun <T, R> ((T) -> R).memoized(): (T) -> R {
   val cache = IdentityHashMap<T, R>()
   return { cache.computeIfAbsent(it, this) }
}

fun <T, R: Comparable<R>> Sequence<T>.sortedByMemoized(by: (T) -> R) = sortedWith(comparing(by.memoized()))

fun <T, R: Comparable<R>> Iterable<T>.sortedByMemoized(by: (T) -> R) = sortedWith(comparing(by.memoized()))