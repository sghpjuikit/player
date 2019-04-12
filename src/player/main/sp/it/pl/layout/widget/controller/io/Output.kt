package sp.it.pl.layout.widget.controller.io

import java.util.Objects
import java.util.UUID

class Output<T>: Put<T?> {

    @JvmField val id: Id

    constructor(id: UUID, name: String, c: Class<T?>): super(c, name, null) {
        this.id = Id(id, name)
    }

    /** Calls [sp.it.pl.layout.widget.controller.io.Input.bind] on specified input with this output.  */
    fun bind(input: Input<in T>) = input.bind(this)

    /** Calls [sp.it.pl.layout.widget.controller.io.Input.unbind] on specified input with this output.  */
    fun unbind(input: Input<in T>) = input.unbind(this)

    override fun equals(other: Any?) = this===other || other is Output<*> && id==other.id

    override fun hashCode() = 5*89+Objects.hashCode(this.id)

    data class Id(@JvmField val ownerId: UUID, @JvmField val name: String) {

        override fun toString() = "$name,$ownerId"

        companion object {

            @JvmStatic fun fromString(s: String): Id {
                val i = s.lastIndexOf(",")
                val n = s.substring(0, i)
                val u = UUID.fromString(s.substring(i+1, s.length))
                return Id(u, n)
            }

        }

    }

}