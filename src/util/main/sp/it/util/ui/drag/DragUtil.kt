package sp.it.util.ui.drag

import javafx.event.EventHandler
import javafx.scene.input.Clipboard
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode.ANY
import sp.it.util.dev.failIf
import javafx.scene.input.DataFormat as DataFormatFX
import sp.it.util.functional.ifNotNull

/** Equivalent to [Clipboard.hasContent]. */
operator fun Clipboard.contains(format: DataFormatFX) = hasContent(format)

/** Equivalent to [Clipboard.hasContent]. */
operator fun Clipboard.contains(format: DataFormat<*>) = hasContent(format.format)

/** Equivalent to [Clipboard.getContent]. */
operator fun Clipboard.get(format: DataFormatFX): Any? {
   failIf(format !in this) { "No data of $format in dragboard." }
   return getContent(format)
}

/** Equivalent to [Clipboard.getContent], but type safe. */
@Suppress("UNCHECKED_CAST")
operator fun <T: Any> Clipboard.get(format: DataFormat<T>): T = this[format.format] as T

/** Equivalent to [Clipboard.setContent], but null safe. */
operator fun Clipboard.set(format: DataFormatFX, data: Any?) = data.ifNotNull { setContent(mapOf(format to data)) }

/** Equivalent to [Clipboard.setContent], but null safe and type safe. */
operator fun <T: Any> Clipboard.set(format: DataFormat<T>, data: T?) = data.ifNotNull { setContent(mapOf(format.format to data)) }

/** Type-safe data format for type-safe [Clipboard.get]. */
class DataFormat<T: Any>(val format: DataFormatFX) {
   constructor(id: String): this(DataFormatFX(id))
}

/** @return drag event handler that accepts drag with the specified conditions */
fun handlerAccepting(acceptWhen: (DragEvent) -> Boolean, unless: (DragEvent) -> Boolean) = EventHandler<DragEvent> {
   if (acceptWhen(it) && !unless(it)) {
      it.acceptTransferModes(*ANY)
      it.consume()
   }
}

/** @return drag event handler that accepts drag with the specified conditions */
fun handlerAccepting(cond: (DragEvent) -> Boolean) = handlerAccepting(cond, { false })