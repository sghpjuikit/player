package sp.it.pl.layout.widget

import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.layout.widget.controller.io.IOLayer
import sp.it.pl.layout.widget.controller.io.Input
import sp.it.pl.layout.widget.controller.io.Output
import sp.it.pl.main.APP
import sp.it.util.async.executor.EventReducer
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.functional.asIs
import sp.it.util.text.splitTrimmed
import java.util.ArrayList
import kotlin.reflect.jvm.jvmName

object WidgetIoManager {
   @JvmField val ios = ArrayList<WidgetIo>()
   private val reducer = EventReducer.toLast<Unit>(100.0) { updateWidgetIo() }

   fun requestWidgetIOUpdate() = reducer.push(Unit)

   fun updateWidgetIo() {
      failIfNotFxThread()

      val os = HashMap<Output.Id, Output<*>>()
      APP.widgetManager.widgets.findAll(OPEN).forEach { w ->
         if (w.controller!=null) {
            w.controller.io.o.getOutputs().forEach { os[it.id] = it }
         }
      }
      IOLayer.allInoutputs.forEach { os[it.o.id] = it.o }

      val iosToRem = mutableSetOf<WidgetIo>()
      ios.forEach { io ->
         io.widget.controller ?: return@forEach
         val i = io.widget.controller.io.i.getInputRaw(io.inputName)?.asIs<Input<Any?>>() ?: return@forEach
         io.outputsIds.forEach {
            val o = os[it]
            if (o!=null) {
               i.bind(o)
               iosToRem += io
            }
         }
      }
      ios -= iosToRem
   }
}

class WidgetIo {
   @JvmField val widget: Widget
   @JvmField val inputName: String
   @JvmField val outputsIds: List<Output.Id>

   constructor(widget: Widget, input_name: String, outputs: String) {
      this.widget = widget
      this.inputName = input_name
      this.outputsIds = outputs.splitTrimmed(":").map { Output.Id.fromString(it) }
   }

   override fun toString() = "${this::class.jvmName} ${widget.name}.$inputName -> $outputsIds"
}