package sp.it.pl.layout.controller.io

import java.util.UUID
import sp.it.pl.layout.controller.io.Output.Id
import sp.it.util.type.VType
import sp.it.util.type.type

/** [XPut], that is a composition of [Input] and [Output]. */
open class InOutput<T>(id: UUID, name: String, type: VType<T>, initialValue: T): XPut<T> {
   @JvmField val o: Output<T> = Output(id, name, type, initialValue)
   @JvmField val i: Input<T> = object: Input<T>(name, type, initialValue, { o.value = it }) {
      override fun isAssignable(output: Output<*>) = output!==o && super.isAssignable(output)
   }

   companion object {
      inline operator fun <reified T> invoke(id: UUID, name: String, initialValue: T): InOutput<T> = InOutput(id, name, type(), initialValue)
   }

}

data class GeneratingOutputRef<T>(val ownerId: UUID, val name: String, val type: VType<T>, val block: (GeneratingOutputRef<T>) -> GeneratingOutput<T>) {
   val id = Id(ownerId, name)
}

open class GeneratingOutput<T>(ref: GeneratingOutputRef<T>, initialValue: T): InOutput<T>(ref.id.ownerId, ref.id.name, ref.type, initialValue) {
   open fun dispose() = Unit
}