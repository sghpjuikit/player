package sp.it.util.functional

import sp.it.util.functional.Option.None
import sp.it.util.functional.Option.Some

/**
 * Option monad for functional value handling.
 *
 * @param <R> value
 */
sealed interface Option<out R> {

   /** @return true iff this is [None] */
   val isNone: Boolean

   /** @return true iff this is [Some] */
   val isSome: Boolean

   object None: Option<Nothing> {
      override val isNone get() = true
      override val isSome get() = false
   }

   data class Some<E>(val value: E): Option<E> {
      override val isNone get() = false
      override val isSome get() = true
   }

   companion object {
      operator fun <R: Any, RN: R?> invoke(value: RN): Option<R> = if (value==null) None else Some(value)
   }

}

/** @return this if this is [Some] and [test] is true, else [None] */
inline fun <R> Option<R>.filter(test: (R) -> Boolean): Option<R> = when (this) {
   is Some<R> -> if (test(value)) this else None
   is None -> None
}

/** @return [Some] mapped by [mapper] if this is [Some], else [None] */
inline fun <R, T> Option<R>.map(mapper: (R) -> T): Option<T> = when (this) {
   is Some<R> -> Some(mapper(value))
   is None -> None
}

/** @return [Option] mapped by [mapper] if this is [Some], else [None] */
inline fun <R, T> Option<R>.flatMap(mapper: (R) -> Option<T>): Option<T> = when (this) {
   is Some<R> -> mapper(value)
   is None -> None
}

/** @return the value if ok or throw an exception if error */
fun <R> Option<R>.getOrThrow(or: () -> Throwable = { NoSuchElementException("Can not get value of Option.None") }): R = when (this) {
   is Some<R> -> value
   is None -> throw or()
}

/** @return the value if ok or the specified value if error */
fun <R, R1: R, R2: R> Option<R1>.getOr(or: R2): R = when (this) {
   is Some<R1> -> value
   is None -> or
}

/** @return the value if ok or the value computed with specified supplier if error */
inline fun <R, R1: R, R2: R> Option<R1>.getOrSupply(or: () -> R2): R = when (this) {
   is Some<R1> -> value
   is None -> or()
}

/** Invoke the specified action if [Option.isSome]. Returns this. */
inline fun <R> Option<R>.ifSome(action: (R) -> Unit) = apply { if (this is Some<R>) action(value) }

/** Invoke the specified action if [Option.isNone]. Returns this. */
inline fun <R> Option<R>.ifNone(action: () -> Unit) = apply { if (this is None) action() }

/** Invoke the specified action. Returns this. */
inline fun <R> Option<R>.ifAny(action: () -> Unit) = apply { action() }