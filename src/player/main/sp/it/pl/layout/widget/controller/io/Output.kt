package sp.it.pl.layout.widget.controller.io

import sp.it.util.collections.materialize
import sp.it.util.type.VType
import java.util.Objects
import java.util.UUID

class Output<T>: Put<T> {
   val id: Id

   constructor(ownerId: UUID, name: String, type: VType<T>, initialValue: T): super(type, name, initialValue) {
      this.id = Id(ownerId, name)
   }

   /** Calls [sp.it.pl.layout.widget.controller.io.Input.bind] on specified input with this output. */
   fun bind(input: Input<in T>) = input.bind(this)

   /** Calls [sp.it.pl.layout.widget.controller.io.Input.unbind] on specified input with this output. */
   fun unbind(input: Input<in T>) = input.unbind(this)

   /** @return true iff this output is bound to any [Input]. */
   fun isBound(): Boolean {
      return IOLayer.allLinks.keys.asSequence()
         .filter { it.key1()==this || it.key2()==this }
         .flatMap {
            sequenceOf(it.key1(), it.key2()).mapNotNull {
               when (it) {
                  is Input<*> -> it
                  is InOutput<*> -> it.i
                  else -> null
               }
            }
         }
         .count()>0
   }

   /** Calls [sp.it.pl.layout.widget.controller.io.Input.unbind] on all inputs bound to this output. */
   fun unbindAll() {
      IOLayer.allLinks.keys.asSequence()
         .filter { it.key1()==this || it.key2()==this }
         .flatMap {
            sequenceOf(it.key1(), it.key2()).mapNotNull {
               when (it) {
                  is Input<*> -> it
                  is InOutput<*> -> it.i
                  else -> null
               }
            }
         }
         .materialize()
         .forEach { it.unbind(this) }
   }

   override fun equals(other: Any?) = this===other || other is Output<*> && id==other.id

   override fun hashCode() = 5*89 + Objects.hashCode(this.id)

   data class Id(val ownerId: UUID, val name: String) {

      override fun toString() = "$name,$ownerId"

      companion object {
         fun fromString(s: String): Id {
            val i = s.lastIndexOf(",")
            val n = s.substring(0, i)
            val u = UUID.fromString(s.substring(i + 1, s.length))
            return Id(u, n)
         }
      }

   }

}