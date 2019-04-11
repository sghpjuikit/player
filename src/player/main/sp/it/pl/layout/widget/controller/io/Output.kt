package sp.it.pl.layout.widget.controller.io

import sp.it.util.dev.failIf
import sp.it.util.reactive.Subscription
import java.util.Objects
import java.util.UUID

class Output<T>: Put<T?> {

    @JvmField val id: Id

    constructor(id: UUID, name: String, c: Class<T?>): super(c, name, null) {
        this.id = Id(id, name)
    }

    fun monitor(input: Input<T>): Subscription {
        failIf(!input.canBind(this)) { "Input<${input.type}> can not bind to put<$type>" }

        return sync { v ->
            if (v!=null && input.type.isInstance(v))
                input.value = v
        }
    }

    /** Calls [sp.it.pl.layout.widget.controller.io.Input.bind] on specified input with this output.  */
    fun bind(input: Input<in T>) = input.bind(this)

    /** Calls [sp.it.pl.layout.widget.controller.io.Input.unbind] on specified input with this output.  */
    fun unbind(input: Input<in T>) = input.unbind(this)

    override fun equals(other: Any?) = this===other || other is Output<*> && id==other.id

    override fun hashCode() = 5*89+Objects.hashCode(this.id)

    class Id(@JvmField val carrier_id: UUID, @JvmField val name: String) {

        override fun equals(o: Any?) = this===o || o is Id && o.name==name && o.carrier_id==carrier_id

        override fun hashCode(): Int {
            var hash = 3
            hash = 79*hash+Objects.hashCode(this.carrier_id)
            hash = 79*hash+Objects.hashCode(this.name)
            return hash
        }

        override fun toString() = "$name,$carrier_id"

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