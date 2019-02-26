package sp.it.pl.layout.widget.controller.io

import sp.it.pl.layout.area.IOLayer
import java.util.UUID
import java.util.function.Consumer

/** [XPut], that is a composition of [Input] and [Output]. */
class InOutput<T>: XPut<T> {
    @JvmField val o: Output<T>
    @JvmField val i: Input<T>

    // private due to use of reified generics
    private constructor(id: UUID, name: String, c: Class<T>) {
        this.o = Output<T>(id, name, c)
        this.i = object: Input<T>(name, c, Consumer<T> { o.value = it }) {
            override fun canBind(output: Output<*>) = output!==o && super.canBind(output)
        }
        IOLayer.all_inoutputs.add(this)
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        @JvmStatic operator fun <T> invoke(id: UUID, name: String, c: Class<T>): InOutput<T?> = InOutput(id, name, c) as InOutput<T?>

        @JvmStatic inline operator fun <reified T:Any> invoke(id: UUID, name: String): InOutput<T?> = invoke(id, name, T::class.javaObjectType)
    }

}