package sp.it.pl.main

import org.reactfx.Subscription
import sp.it.pl.gui.pane.ActionPane
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
fun <T: Any> FXMLController.v(initialValue: T, onChange: (T) -> Unit) = VLate(initialValue, onChange)

class VLate<T>(private val initialValue: T, private val onChange: (T) -> Unit) {
    operator fun provideDelegate(ref: FXMLController, property: KProperty<*>): ReadOnlyProperty<FXMLController, V<T>> {
        val v = v(initialValue) { if (ref.isInitialized) onChange(it) }
        return object: ReadOnlyProperty<FXMLController, V<T>> {
            override fun getValue(thisRef: FXMLController, property: KProperty<*>): V<T> {
                return v
            }
        }
    }
}

