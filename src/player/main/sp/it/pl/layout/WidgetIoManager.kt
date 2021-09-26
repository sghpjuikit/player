package sp.it.pl.layout

import kotlin.reflect.jvm.jvmName
import sp.it.pl.layout.controller.io.IOLayer
import sp.it.pl.layout.controller.io.Input
import sp.it.pl.layout.controller.io.Output
import sp.it.util.async.executor.EventReducer
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.text.splitNoEmpty

object WidgetIoManager {
   val ios = ArrayList<WidgetIo>()
   private val reducer = EventReducer.toLast<Unit>(100.0) { updateWidgetIo() }

   fun requestWidgetIOUpdate() = reducer.push(Unit)

   fun updateWidgetIo() {
      failIfNotFxThread()

      val os = IOLayer.allOutputs().associateBy { it.id }
      val iosToRem = mutableSetOf<WidgetIo>()
      ios.forEach { io ->
         val c = io.widget.controller ?: return@forEach
         val i = c.io.i.getInputRaw(io.inputName)?.asIs<Input<Any?>>() ?: return@forEach
         io.outputsIds.forEach { oId ->
            // bind to outputs
            os[oId].ifNotNull { o ->
               i.bind(o)
               iosToRem += io
            }
            // bind to generators
            IOLayer.generatingOutputRefs.find { it.id == oId }.ifNotNull { o ->
               i.bind(o)
               iosToRem += io
            }
         }
      }
      ios -= iosToRem
   }
}

class WidgetIo(widget: Widget, input_name: String, outputs: String) {
   val widget: Widget = widget
   val inputName: String = input_name
   val outputsIds: List<Output.Id> = outputs.splitNoEmpty(":").map { Output.Id.fromString(it) }.toList()

   override fun toString() = "${this::class.jvmName} ${widget.name}.$inputName -> $outputsIds"
}