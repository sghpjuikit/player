package sp.it.pl.layout.widget.controller.io

import java.util.UUID
import sp.it.util.type.VType
import sp.it.util.type.type

/** [XPut], that is a composition of [Input] and [Output]. */
class InOutput<T>: XPut<T> {
   @JvmField val o: Output<T>
   @JvmField val i: Input<T>

   constructor(id: UUID, name: String, type: VType<T>, initialValue: T) {
      this.o = Output(id, name, type, initialValue)
      this.i = object: Input<T>(name, type, initialValue, { o.value = it }) {
         override fun isAssignable(output: Output<*>) = output!==o && super.isAssignable(output)
      }
   }

   fun appWide() = apply {
      IOLayer.allInoutputs += this
   }

   companion object {
      inline operator fun <reified T> invoke(id: UUID, name: String, initialValue: T): InOutput<T> = InOutput(id, name, type(), initialValue)
   }

}