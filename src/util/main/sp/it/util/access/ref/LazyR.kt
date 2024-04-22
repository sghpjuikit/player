package sp.it.util.access.ref

import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Nullable lazy value.
 * @param builder produces the instance when it is first accessed. Builder can produce null.
 * @param <V> type of instance
 */
@Suppress("UNCHECKED_CAST")
open class LazyR<V>(builder: () -> V) {
   protected var v: V? = null
   protected var builder: (() -> V)? = builder
   var isInitialized: Boolean = false
      protected set

   protected fun set(value: V) {
      isInitialized = true
      builder = null
      v = value
   }

   fun get(): V {
      if (!isInitialized) set(builder!!())
      return v as V
   }

   fun <M> get(m: M, or: (M) -> V): V {
      if (!isInitialized) set(or(m))
      return v as V
   }

   /** @return the current value if set, otherwise the specified value */
   fun getOr(or: V): V =
      if (isInitialized) v as V else or

   /** @return the current value if set, otherwise null */
   val orNull: V?
      get() = if (isInitialized) v else null

   /** @return the current value if set, otherwise the specified value */
   fun getOr(or: Supplier<V>): V =
      if (isInitialized) v as V else or.get()

   fun ifInitialized(block: Consumer<in V>) {
      if (isInitialized) block.accept(v as V)
   }

}