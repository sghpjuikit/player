package sp.it.util.functional

import sp.it.util.functional.Option.None
import sp.it.util.functional.Option.Some

/**
 * Option monad for functional value handling.
 *
 * @param <R> value
 */
sealed class Option<out R> {

   /** @return true iff this is [None] */
   abstract val isNone: Boolean

   /** @return true iff this is [Some] */
   abstract val isSome: Boolean

   /** @return this if this is [Some] and [test] is true, else [None] */
   inline fun filter(test: (R) -> Boolean): Option<R> = when (this) {
      is Some<R> -> if (test(value)) this else None
      is None -> None
   }

   /** @return [Some] mapped by [mapper] if this is [Some], else [None] */
   inline fun <T> map(mapper: (R) -> T): Option<T> = when (this) {
      is Some<R> -> Some(mapper(value))
      is None -> None
   }

   /** @return [Option] mapped by [mapper] if this is [Some], else [None] */
   inline fun <T> flatMap(mapper: (R) -> Option<T>): Option<T> = when (this) {
      is Some<R> -> mapper(value)
      is None -> None
   }

   /** @return the value if ok or throw an exception if error */
   fun getOrThrow(or: () -> Throwable = { NoSuchElementException("Can not get value of Option.None") }): R = when (this) {
      is Some<R> -> value
      is None -> throw or()
   }

   /** Invoke the specified action if [isSome]. Returns this. */
   inline fun ifSome(action: (R) -> Unit) = apply { if (this is Some<R>) action(value) }

   /** Invoke the specified action if [isNone]. Returns this. */
   inline fun ifNone(action: () -> Unit) = apply { if (this is None) action() }

   /** Invoke the specified action. Returns this. */
   inline fun ifAny(action: () -> Unit) = apply { action() }

   object None: Option<Nothing>() {
      override val isNone = true
      override val isSome = false
   }

   data class Some<E>(val value: E): Option<E>() {
      override val isNone = false
      override val isSome = true
   }

   companion object {
      operator fun <R: Any, RN: R?> invoke(value: RN): Option<R> = if (value==null) None else Some(value)
   }

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