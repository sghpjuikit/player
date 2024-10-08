package sp.it.util.reactive

import java.util.concurrent.CopyOnWriteArraySet
import kotlin.reflect.KClass
import sp.it.util.collections.materialize

private typealias C<T> = (T) -> Unit

/** Set of functions taking 1 parameter. Use as a collection of handlers/listeners. */
class Handler1<I>(backingSet: MutableSet<C<I>> = CopyOnWriteArraySet()): MutableSet<C<I>> by backingSet, C<I> {

   /** Invokes all contained functions, in order they were put in. */
   override fun invoke(input: I) {
      forEach { it(input) }
   }

   /** Adds the specified function to this. Returns subscription to remove it. */
   infix fun attach(block: (I) -> Unit): Subscription {
      add(block)
      return Subscription { remove(block) }
   }

   /** Adds the specified function to this called if an event is the specified event object. Returns subscription to remove it. */
   fun <E: Any> onEventObject(event: E, block: (E) -> Unit) = attach {
      if (it===event) block(it)
   }

   /** Adds the specified function to this called if an event is instance of the specified type. Returns subscription to remove it. */
   @Suppress("UNCHECKED_CAST")
   fun <E: Any> onEvent(type: KClass<E>, block: (E) -> Unit) = attach {
      if (type.isInstance(it)) block(it as E)
   }

   /** Adds the specified function to this called if an event is instance of the specified type and filter returns true. Returns subscription to remove it. */
   inline fun <reified E: I> onEvent(crossinline filter: (E) -> Boolean = { true }, crossinline block: (E) -> Unit) = attach {
      if (it is E && filter(it)) block(it)
   }

   fun asChan(): ChanValue<I> = object: ChanValue<I> {
      override infix fun subscribe(listener: (I) -> Unit): Subscription = addRem(listener)
   }

}