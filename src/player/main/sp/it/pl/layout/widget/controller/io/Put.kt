package sp.it.pl.layout.widget.controller.io

import sp.it.util.reactive.Subscription
import sp.it.util.type.nullify
import java.lang.reflect.Type

open class Put<T>: XPut<T> {
   val name: String
   val type: Class<T>
   var typeRaw: Type? = null
   protected val monitors = mutableSetOf<(T) -> Unit>()
   var value: T
      set(v) {
         if (field!==v) {
            field = v
            monitors.forEach { it(v) }
         }
      }

   constructor(type: Class<T>, name: String, initialValue: T) {
      this.name = name
      this.type = type
      this.value = initialValue
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