package sp.it.pl.layout.controller.io

import sp.it.pl.layout.controller.io.EqualizeBy.EQ
import sp.it.util.reactive.Subscription
import sp.it.util.type.VType
import sp.it.util.type.nullify

abstract class Put<T>(val type: VType<*>, val name: String, val initialValue: T): XPut<T> {

   /** Value comparator that reduces [value] changes only to those with desired difference. Default [EQ]. */
   var equalBy: (T, T) -> Boolean = EQ
   /** Blocks invoked when [value] changes. */
   protected val monitors = mutableSetOf<(T) -> Unit>()
   /** Actual value. Only changes when set to value considered different by [equalBy]. Calls [monitors] on change. */
   var value: T = initialValue
      set(v) {
         if (!equalBy(field,v)) {
            field = v
            monitors.forEach { it(v) }
         }
      }

   fun sync(action: (T) -> Unit): Subscription {
      action(value)
      return attach(action)
   }

   fun attach(action: (T) -> Unit): Subscription {
      monitors += action
      return Subscription { monitors -= action }
   }

   fun dispose() {
      monitors.clear()
      nullify(::value)
   }

}