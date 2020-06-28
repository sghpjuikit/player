package sp.it.util.reactive

import sp.it.util.functional.asIs
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.safeCast

/** Set of functions taking 1 parameter. Use as a collection of handlers/listeners. */
class Handler1<I>: MutableSet<(I) -> Unit> by LinkedHashSet(2), (I) -> Unit {

   /** Invokes all contained functions, in order they were put in. */
   override fun invoke(input: I) {
      forEach { it(input) }
   }

   /** Adds specified function to this. Calling the return value will remove it. */
   fun addS(block: (I) -> Unit): Subscription {
      add(block)
      return Subscription { remove(block) }
   }

   @Suppress("UNCHECKED_CAST")
   fun <E: Any> onEvent(type: KClass<E>, block: (E) -> Unit) = addS {
      if (type.isInstance(it)) block(it as E)
   }

   inline fun <reified E: I> onEvent(crossinline block: (E) -> Unit) = addS {
      if (it is E) block(it)
   }

}