package sp.it.pl.layout.widget.controller.io

import sp.it.util.dev.failIf
import sp.it.util.functional.asIf
import sp.it.util.reactive.Subscription
import sp.it.util.type.Util.getRawType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.Objects
import java.util.UUID

class Output<T>: Put<T?> {

    @JvmField val id: Id

    constructor(id: UUID, name: String, c: Class<T?>): super(c, name, null) {
        this.id = Id(id, name)
    }

    @Suppress("UNCHECKED_CAST")
    fun monitor(input: Input<T>): Subscription {
        failIf(!input.canBind(this)) { "Input<${input.type}> can not bind to put<$type>" }
        fun Type?.listType() = getRawType(this.asIf<ParameterizedType>()!!.actualTypeArguments[0])

        return sync { v ->
            if (v!=null) {
                when {
                    input.type==List::class.java && type==List::class.java -> {
                        input.value = v
                    }
                    input.type==List::class.java -> {
                        if (input.typeRaw.listType().isInstance(v))
                            input.value = listOf(v) as T
                    }
                    type==List::class.java -> {}
                    else -> {
                        if (input.type.isInstance(v))
                            input.value = v
                    }
                }
            }
        }
    }

    /** Calls [sp.it.pl.layout.widget.controller.io.Input.bind] on specified input with this output.  */
    fun bind(input: Input<in T>) = input.bind(this)

    /** Calls [sp.it.pl.layout.widget.controller.io.Input.unbind] on specified input with this output.  */
    fun unbind(input: Input<in T>) = input.unbind(this)

    override fun equals(other: Any?) = this===other || other is Output<*> && id==other.id

    override fun hashCode() = 5*89+Objects.hashCode(this.id)

    class Id(@JvmField val carrier_id: UUID, @JvmField val name: String) {

        override fun equals(other: Any?) = this===other || other is Id && other.name==name && other.carrier_id==carrier_id

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