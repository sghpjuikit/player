package sp.it.pl.main

import sp.it.pl.layout.widget.WidgetFactory
import sp.it.pl.util.async.future.Fut

fun WidgetFactory<*>?.orEmpty(): WidgetFactory<*> = this ?: App.APP.widgetManager.widgetFactoryEmpty!!

fun futureWrap(data: Any?): Fut<*> = data as? Fut<*> ?: Fut.fut(data)!!