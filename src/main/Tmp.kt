package main

import layout.widget.WidgetFactory
import util.async.future.Fut

fun WidgetFactory<*>?.orEmpty(): WidgetFactory<*> = this ?: App.APP.widgetManager.widgetFactoryEmpty!!

fun futureWrap(data: Any?): Fut<*> = data as? Fut<*> ?: Fut.fut(data)!!