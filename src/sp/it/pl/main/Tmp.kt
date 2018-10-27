package sp.it.pl.main

import org.reactfx.Subscription
import sp.it.pl.gui.pane.ActionPane
import sp.it.pl.layout.widget.controller.ClassController
import sp.it.pl.layout.widget.controller.FXMLController
import sp.it.pl.util.access.V
import sp.it.pl.util.access.v
import sp.it.pl.util.async.future.Fut
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun futureWrap(data: Any?): Fut<*> = data as? Fut<*> ?: Fut.fut(data)!!

inline fun <reified T> ActionPane.register(vararg actions: ActionPane.ActionData<T, *>) = register(T::class.java, *actions)

infix fun FXMLController.onDispose(s: () -> Unit) = d({ s() })

infix fun FXMLController.initClose(s: () -> Subscription) = d(s())

@Deprecated("experimental", level = DeprecationLevel.HIDDEN)
fun <T: Any> ClassController.v(initialValue: T, onChange: (T) -> Unit) = VLate(initialValue, onChange)

class VLate<T>(private val initialValue: T, private val onChange: (T) -> Unit) {
    operator fun provideDelegate(ref: ClassController, property: KProperty<*>): ReadOnlyProperty<ClassController, V<T>> {
        val v = v(initialValue) { if (ref.isInitialized) onChange(it) }
        return object: ReadOnlyProperty<ClassController, V<T>> {
            override fun getValue(thisRef: ClassController, property: KProperty<*>): V<T> {
                return v
            }
        }
    }
}

