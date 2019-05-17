package sp.it.pl.gui.objects.picker

import javafx.geometry.Insets
import javafx.geometry.VPos.CENTER
import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.layout.Region.USE_COMPUTED_SIZE
import javafx.scene.text.TextAlignment
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.collections.setTo
import sp.it.util.functional.supplyIf
import sp.it.util.math.max
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventDown
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.pane
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.scrollTextCenter
import sp.it.util.ui.setMinPrefMaxSize
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.stackPane
import sp.it.util.ui.text
import sp.it.util.units.millis
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Generic picker.
 *
 * The elements are obtained using [itemSupply] when [buildContent] is called, sorted lexicographically and displayed
 * in a 2D grid using [textConverter], [infoConverter].
 */
open class Picker<E> {

    private val tiles = CellPane()
    /** Scene graph root of this object. */
    val root = ScrollPane(tiles)
    /** Invoked when item is selected. Default implementation does nothing. */
    var onSelect: (E) -> Unit = {}
    /** Invoked when user user cancels the picking. Default implementation does nothing. */
    var onCancel: () -> Unit = {}
    /** It may be desirable to consume the mouse click event that caused the cancellation. Default false. */
    var consumeCancelEvent = false
    /** Cell text factory producing name/title of the item. Default implementation calls [Any.toString] */
    var textConverter: (E) -> String = Any?::toString
    /** Cell detail text text factory producing description of the item. Default implementation returns empty string. */
    var infoConverter: (E) -> String = { "" }
    /** Supplier that returns items to be displayed. Default implementation returns empty sequence. */
    var itemSupply: () -> Sequence<E> = { sequenceOf() }

    private val cellFactory: (E) -> Pane = { item ->
        stackPane {
            setMinSize(90.0, 30.0)
            styleClass += CELL_STYLE_CLASS
            padding = Insets(20.0)

            val contentText = textConverter(item)
            val contentInfoText = infoConverter(item)
            val content = label(contentText)

            lay += content
            lay += supplyIf(contentInfoText.isNotEmpty()) {
                val contentInfo = scrollTextCenter {
                    text(contentInfoText) {
                        isMouseTransparent = true
                        textOrigin = CENTER
                        textAlignment = TextAlignment.CENTER
                    }
                }

                val anim = anim(300.millis) {
                    content.opacity = 1-it*it
                    contentInfo.opacity = it
                    contentInfo.setScaleXY(0.7+0.3*it*it)
                }

                anim.applyNow()
                hoverProperty() attach { anim.playFromDir(it) }

                contentInfo
            }
        }
    }

    constructor() {
        root.apply {
            setMinPrefMaxSize(USE_COMPUTED_SIZE)
            isPannable = false  // forbid mouse panning (can cause unwanted horizontal scrolling)
            isFitToWidth = true // make content horizontally resize with scroll pane
            hbarPolicy = NEVER
            onEventDown(MOUSE_CLICKED, SECONDARY, false) {
                onCancel()
                if (consumeCancelEvent) it.consume()
            }
            styleClass += STYLE_CLASS
        }
    }

    fun buildContent() {
        tiles.children setTo itemSupply()
                .sortedBy(textConverter)
                .map { item ->
                    cellFactory(item).apply {
                        onEventDown(MOUSE_CLICKED, PRIMARY) { onSelect(item) }
                    }
                }
        tiles.children += pane {
            styleClass += CELL_STYLE_CLASS
            pseudoClassChanged("empty", true)
            properties[KEY_EMPTY_CELL] = null
            isManaged = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCells() = tiles.children.filter { !it.properties.containsKey(KEY_EMPTY_CELL) } as List<Region>

    private inner class CellPane: Pane() {

        override fun layoutChildren() {
            val cells = getCells()
            val padding = root.padding
            val gap = 1.0
            val width = root.width-padding.left-padding.right
            val height = root.height-padding.top-padding.bottom

            if (cells.isEmpty()) return

            val elements = cells.size
            val cellMinWidth = 1.0 max cells.first().minWidth
            val cellMinHeight = 1.0 max cells.first().minHeight

            var c = if (width>height) ceil(sqrt(elements.toDouble())).toInt() else floor(sqrt(elements.toDouble())).toInt()
            c = if (width<c*cellMinWidth) floor(width/cellMinWidth).toInt() else c
            val columns = max(1, c)

            val rows = ceil(elements.toDouble()/columns).toInt()

            val gapSumY = (rows-1)*gap
            val cellHeight = if (height<rows*cellMinHeight) cellMinHeight else (height-gapSumY)/rows-1.0/rows

            val w = if (rows*(cellHeight+gap)-gap>height) width-15 else width // TODO: take care of scrollbar better
            val gapSumX = (columns-1)*gap
            val cellWidth = (w-gapSumX)/columns

            cells.forEachIndexed { i, n ->
                val x = padding.left+i%columns*(cellWidth+gap)
                val y = padding.top+i/columns*(cellHeight+gap)
                n.resizeRelocate(
                        x.snapX,
                        y.snapY,
                        (x+cellWidth).snapX-x.snapX,
                        (y+cellHeight).snapY-y.snapY
                )
            }

            val needsEmptyCell = cells.isEmpty() || cells.size!=columns*rows
            val emptyCell = children.find { it.properties.containsKey(KEY_EMPTY_CELL) }!!
            if (needsEmptyCell) {
                val i = cells.size
                val x = padding.left+i%columns*(cellWidth+gap)
                val y = padding.top+i/columns*(cellHeight+gap)
                emptyCell.resizeRelocate(
                        x.snapX,
                        y.snapY,
                        (root.width-padding.right).snapX-x.snapX,
                        (y+cellHeight).snapY-y.snapY
                )
            } else {
                emptyCell.resizeRelocate(0.0, 0.0, 0.0, 0.0)
            }
        }

        private val Double.snapX: Double get() = this//snapPositionX(this)
        private val Double.snapY: Double get() = this//snapPositionY(this)
    }

    companion object {
        const val STYLE_CLASS = "item-picker"
        val CELL_STYLE_CLASS = listOf("block", "item-picker-element")
        private const val KEY_EMPTY_CELL = "empty_cell"
    }

}