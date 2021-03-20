package sp.it.util.conf

import java.util.function.Supplier

/** Collection supplier. */
typealias Enumerator<T> = Supplier<Collection<T>>

/** Enumerator for complete set of allowed values, returning all values. */
interface SealedEnumerator<out T> {
   fun enumerateSealed(): Collection<T>
}

/** Enumerator for unbounded set of allowed values, returning only known subset known at the time of invocation. */
interface UnsealedEnumerator<out T> {
   fun enumerateUnsealed(): Collection<T>
}