package sp.it.util.access.ref

import java.util.function.BiConsumer
import java.util.function.Supplier

/**
 * Mutable lazy singleton object implementation.
 *
 * Provides access to single object instance, which can be created lazily - when requested for the first time.
 * Additionally, the object can be mutated (its state changed) before it is accessed. This allows a reuse of the
 * object across different objects that use it.
 *
 * @param builder produces the instance when it is requested.
 * @param mutator mutates instance's state for certain dependency object. use null if no mutation is desired.
 * @param <V> type of instance
 * @param <M> type of object the instance relies on
 */
class SingleR<V, M> constructor(builder: () -> V, mutator: BiConsumer<V, M>): LazyR<V>(builder) {
   private val mutator: BiConsumer<V, M> = mutator

   /**
    * Calls [get] and mutates the value using [mutator].
    *
    * @param mutation_source
    * @return the instance after calling [get] and mutating
    */
   fun getM(mutation_source: M): V {
      var v = get()
      mutator.accept(v, mutation_source)
      return v
   }
}