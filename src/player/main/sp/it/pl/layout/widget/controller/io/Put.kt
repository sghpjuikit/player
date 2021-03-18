package sp.it.pl.layout.widget.controller.io

import sp.it.util.reactive.Subscription
import sp.it.util.type.VType
import sp.it.util.type.nullify

open class Put<T>(type: VType<T>, name: String, initialValue: T): XPut<T> {
   val name: String = name
   val type: VType<T> = type
   protected val monitors = mutableSetOf<(T) -> Unit>()
   val initialValue: T = initialValue
   var value: T = initialValue
      set(v) {
         if (field!==v) {
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