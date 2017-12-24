package sp.it.pl.main

import sp.it.pl.gui.pane.ActionPane
import sp.it.pl.layout.widget.WidgetFactory
import sp.it.pl.layout.widget.WidgetManager
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.async.future.Fut

inline fun <reified T> WidgetManager.use(source: WidgetManager.WidgetSource, noinline action: (T) -> Unit) = use(T::class.java, source, action)

fun WidgetFactory<*>?.orEmpty(): WidgetFactory<*> = this ?: APP.widgetManager.widgetFactoryEmpty!!

fun futureWrap(data: Any?): Fut<*> = data as? Fut<*> ?: Fut.fut(data)!!

inline fun <reified T> ActionPane.register(vararg actions: ActionPane.ActionData<T,*>) = register(T::class.java, *actions)