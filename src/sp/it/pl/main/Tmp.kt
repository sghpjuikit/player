package sp.it.pl.main

import sp.it.pl.gui.pane.ActionPane
import sp.it.pl.util.async.future.Fut

fun futureWrap(data: Any?): Fut<*> = data as? Fut<*> ?: Fut.fut(data)!!

inline fun <reified T> ActionPane.register(vararg actions: ActionPane.ActionData<T,*>) = register(T::class.java, *actions)