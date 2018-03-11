package sp.it.pl.main

import org.reactfx.Subscription
import sp.it.pl.gui.pane.ActionPane
import sp.it.pl.layout.widget.controller.FXMLController
import sp.it.pl.util.async.future.Fut

fun futureWrap(data: Any?): Fut<*> = data as? Fut<*> ?: Fut.fut(data)!!

inline fun <reified T> ActionPane.register(vararg actions: ActionPane.ActionData<T, *>) = register(T::class.java, *actions)

infix fun FXMLController.onDispose(s: () -> Unit) = d({ s() })

infix fun FXMLController.initClose(s: () -> Subscription) = d(s())