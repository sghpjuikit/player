package sp.it.pl.layout.widget.controller.io

import sp.it.util.type.VType
import sp.it.util.type.type
import java.util.UUID

/** [XPut], that is a composition of [Input] and [Output]. */
class InOutput<T>: XPut<T?> {
   @JvmField val o: Output<T>
   @JvmField val i: Input<T>

   // private due to use of reified generics
   @Suppress("UNCHECKED_CAST")
   private constructor(id: UUID, name: String, type: VType<T>) {
      this.o = Output(id, name, type)
      this.i = object: Input<T>(name, type, null, { o.value = it }) {
         override fun isAssignable(output: Output<*>) = output!==o && super.isAssignable(output)
      }
   }

   fun appWide() = apply {
      IOLayer.allInoutputs += this
   }

   companion object {

      @Suppress("UNCHECKED_CAST")
      @JvmStatic
      operator fun <T> invoke(id: UUID, name: String, type: VType<T>): InOutput<T?> = InOutput(id, name, type)

      inline operator fun <reified T: Any> invoke(id: UUID, name: String): InOutput<T?> = invoke(id, name, type())
   }

}