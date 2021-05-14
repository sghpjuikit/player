package sp.it.pl.layout.widget.controller.io

import sp.it.util.reactive.Subscription
import sp.it.util.type.VType
import sp.it.util.type.nullify

abstract class Put<T>(val type: VType<*>, val name: String, val initialValue: T): XPut<T> {

   protected val monitors = mutableSetOf<(T) -> Unit>()

   var value: T = initialValue
      set(v) {
         if (field!=v) {
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