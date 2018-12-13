package sp.it.pl.main

import sp.it.pl.gui.pane.ActionPane
import sp.it.pl.util.async.future.Fut
import sp.it.pl.util.async.future.Fut.Companion.fut

fun futureWrap(data: Any?): Fut<*> = data as? Fut<*> ?: fut(data)

inline fun <reified T> ActionPane.register(vararg actions: ActionPane.ActionData<T, *>) = register(T::class.java, *actions)