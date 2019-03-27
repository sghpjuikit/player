package sp.it.pl.util.ui.drag

import de.jensd.fx.glyphs.GlyphIcons
import javafx.event.EventHandler
import javafx.geometry.Bounds
import javafx.scene.Node
import javafx.scene.input.DragEvent
import javafx.scene.input.DragEvent.DRAG_DROPPED
import javafx.scene.input.DragEvent.DRAG_OVER
import javafx.scene.input.Dragboard
import javafx.scene.input.TransferMode.ANY
import sp.it.pl.util.dev.failIf
import sp.it.pl.util.functional.Functors
import java.util.function.Predicate
import java.util.function.Supplier
import javafx.scene.input.DataFormat as DataFormatFX

/** Equivalent to [Dragboard.hasContent]. */
operator fun Dragboard.contains(format: DataFormatFX) = hasContent(format)

/** Equivalent to [Dragboard.hasContent]. */
operator fun Dragboard.contains(format: DataFormat<*>) = hasContent(format.format)

/** Equivalent to [Dragboard.getContent]. */
operator fun Dragboard.get(format: DataFormatFX): Any? {
    failIf(format !in this) { "No data of $format in dragboard." }
    return getContent(format)
}

/** Equivalent to [Dragboard.getContent], but type safe. */
@Suppress("UNCHECKED_CAST")
operator fun <T> Dragboard.get(format: DataFormat<T>): T = this[format.format] as T

/** Equivalent to [Dragboard.setContent]. */
operator fun Dragboard.set(format: DataFormatFX, data: Any) = setContent(mapOf(format to data))

/** Equivalent to [Dragboard.setContent], but type safe. */
operator fun <T> Dragboard.set(format: DataFormat<out T>, data: T) = setContent(mapOf(format.format to data))

/** Type-safe data format for type-safe [Dragboard.get]. */
class DataFormat<T>(val format: DataFormatFX) {
    constructor(id: String): this(DataFormatFX(id))
}

/** Sets up drag support with specified characteristics for the specified node. See [DragPane.install]. */
fun installDrag(node: Node, icon: GlyphIcons, description: String, condition: (DragEvent) -> Boolean, action: (DragEvent) -> Unit) =
        installDrag(node, icon, description, condition, { false }, action)

/** Sets up drag support with specified characteristics for the specified node. See [DragPane.install]. */
fun installDrag(node: Node, icon: GlyphIcons, description: String, condition: (DragEvent) -> Boolean, exc: (DragEvent) -> Boolean, action: (DragEvent) -> Unit) =
        installDrag(node, icon, Supplier { description }, condition, exc, action)

/** Sets up drag support with specified characteristics for the specified node. See [DragPane.install]. */
fun installDrag(node: Node, icon: GlyphIcons, description: Supplier<out String>, condition: (DragEvent) -> Boolean, action: (DragEvent) -> Unit) =
        installDrag(node, icon, description, condition, { false }, action)

/** Sets up drag support with specified characteristics for the specified node. See [DragPane.install]. */
@JvmOverloads
fun installDrag(node: Node, icon: GlyphIcons, description: Supplier<out String>, condition: (DragEvent) -> Boolean, exc: (DragEvent) -> Boolean, action: (DragEvent) -> Unit, area: ((DragEvent) -> Bounds)? = null) {
    // accept drag if desired
    node.addEventHandler(DRAG_OVER, handlerAccepting(condition, exc))
    // handle drag & clear data
    node.addEventHandler(DRAG_DROPPED) { e ->
        if (condition(e)) {
            action(e)
            e.isDropCompleted = true
            e.consume()
        }
    }
    // show hint
    DragPane.install(node, icon, description, Predicate { condition(it) }, Predicate { exc(it) }, if (area==null) null else Functors.Ƒ1 { area(it) })
}

/** @return drag event handler that accepts drag with the specified conditions */
fun handlerAccepting(acceptWhen: (DragEvent) -> Boolean, unless: (DragEvent) -> Boolean) = EventHandler<DragEvent> {
    if (acceptWhen(it) && !unless(it)) {
        it.acceptTransferModes(*ANY)
        it.consume()
    }
}

/** @return drag event handler that accepts drag with the specified conditions */
@JvmOverloads
fun handlerAccepting(cond: (DragEvent) -> Boolean, orConsume: Boolean = false) = handlerAccepting(cond, { orConsume })