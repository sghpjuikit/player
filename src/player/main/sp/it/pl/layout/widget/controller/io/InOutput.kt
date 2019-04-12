package sp.it.pl.layout.widget.controller.io

import sp.it.pl.layout.area.IOLayer
import sp.it.util.type.Util.getRawType
import sp.it.util.type.type
import java.lang.reflect.Type
import java.util.UUID

/** [XPut], that is a composition of [Input] and [Output]. */
class InOutput<T>: XPut<T?> {
    @JvmField val o: Output<T>
    @JvmField val i: Input<T>

    // private due to use of reified generics
    @Suppress("UNCHECKED_CAST")
    private constructor(id: UUID, name: String, type: Type) {
        this.o = Output(id, name, getRawType(type) as Class<T?>)
        this.o.typeRaw = type
        this.i = object: Input<T>(name, getRawType(type) as Class<T?>, null, { o.value = it }) {
            override fun isAssignable(output: Output<*>) = output!==o && super.isAssignable(output)
        }
        this.i.typeRaw = type
    }

    fun appWide() = apply {
        IOLayer.all_inoutputs += this
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        operator fun <T> invoke(id: UUID, name: String, type: Type): InOutput<T?> = InOutput(id, name, type)

        inline operator fun <reified T:Any> invoke(id: UUID, name: String): InOutput<T?> = invoke(id, name, type<T>())
    }

}