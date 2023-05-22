package sp.it.pl.ui.objects.picker

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Insets
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Pos
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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.collections.setTo
import sp.it.util.functional.supplyIf
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.ui.Util.getScrollBar
import sp.it.util.ui.height
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.minPrefMaxHeight
import sp.it.util.ui.pane
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.scrollTextCenter
import sp.it.util.ui.setMinPrefMaxSize
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.stackPane
import sp.it.util.ui.text
import sp.it.util.ui.vBox
import sp.it.util.ui.width
import sp.it.util.ui.x
import sp.it.util.units.em
import sp.it.util.units.millis

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
   /** Invoked when user cancels the picking. Default implementation does nothing. */
   var onCancel: () -> Unit = {}
   /** It may be desirable to consume the mouse click event that caused the cancellation. Default false. */
   var consumeCancelEvent = false
   /** Cell text factory producing name/title of the item. Default implementation calls [Any.toString] */
   var iconConverter: (E) -> GlyphIcons? = { null }
   /** Cell text factory producing name/title of the item. Default implementation calls [Any.toString] */
   var textConverter: (E) -> String = Any?::toString
   /** Cell detail text factory producing description of the item. Default implementation returns empty string. */
   var infoConverter: (E) -> String = { "" }
   /** Supplier that returns items to be displayed. Default implementation returns empty sequence. */
   var itemSupply: () -> Sequence<E> = { sequenceOf() }
   /** Minimum cell size. */
   val minCellSize = v(90 x 30)

   private val cellFactory: (E) -> Pane = { item ->
      stackPane {
         styleClass += CELL_STYLE_CLASS
         pseudoClassChanged("filled", true)
         padding = Insets(20.0)

         val contentIcon = iconConverter(item)
         val contentText = textConverter(item)
         val contentInfoText = infoConverter(item)
         val content = when (contentIcon) {
            null -> label(contentText)
            else -> vBox(5.0, Pos.CENTER) {
               lay += Icon(contentIcon).apply {
                  focusOwner.value = this@stackPane
                  this@stackPane.heightProperty() attach {
                     size((it.toDouble() - 3.0.em)/2.0 min 64.0)
                  }
               }
               lay += label(contentText)
            }
         }

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
               content.opacity = 1 - it*it
               contentInfo.isManaged = it==0.0
               contentInfo.opacity = it
               contentInfo.setScaleXY(0.7 + 0.3*it*it)
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
         isFitToWidth = true // auto-resize content horizontally
         heightProperty() sync { tiles.requestLayout() } // auto-resize content horizontally
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
         pseudoClassChanged("filled", false)
         properties[KEY_EMPTY_CELL] = null
         isManaged = false
      }
   }

   @Suppress("UNCHECKED_CAST")
   private fun getCells() = tiles.children.filter { KEY_EMPTY_CELL !in it.properties } as List<Region>

   private inner class CellPane: Pane() {

      override fun layoutChildren() {
         val cells = getCells()
         val padding = root.padding
         val gap = 1.0
         val width = root.width - padding.width
         val height = root.height - padding.height

         if (cells.isEmpty()) return

         val elements = cells.size
         val cellMinWidth = 1.0 max minCellSize.value.x
         val cellMinHeight = 1.0 max minCellSize.value.y

         var c = if (width>height) ceil(sqrt(elements.toDouble())).toInt() else floor(sqrt(elements.toDouble())).toInt()
         c = if (width<c*cellMinWidth) floor(width/cellMinWidth).toInt() else c
         val columns = max(1, c)

         val rows = ceil(elements.toDouble()/columns).toInt()

         val gapSumY = (rows - 1)*gap
         val cellHeight = if (height<rows*cellMinHeight) cellMinHeight else (height - gapSumY)/rows - 1.0/rows
         val cellSumY = rows*cellHeight
         val totalHeight = cellSumY + gapSumY
         minPrefMaxHeight = totalHeight

         val isScrollbarNecessary = totalHeight>height
         val scrollBarWidth = if (!isScrollbarNecessary) 0.0 else getScrollBar(root, VERTICAL)?.takeIf { it.isVisible }?.width ?: 0.0
         val gapSumX = (columns - 1)*gap
         val cellWidth = (width - scrollBarWidth - gapSumX)/columns

         cells.forEachIndexed { i, n ->
            val x = padding.left + i%columns*(cellWidth + gap)
            val y = padding.top + i/columns*(cellHeight + gap)
            n.resizeRelocate(
               x.snapX,
               y.snapY,
               (x + cellWidth).snapX - x.snapX,
               (y + cellHeight).snapY - y.snapY
            )
         }

         val needsEmptyCell = cells.isEmpty() || cells.size!=columns*rows
         val emptyCell = children.first { it.properties.containsKey(KEY_EMPTY_CELL) }
         if (needsEmptyCell) {
            val i = cells.size
            val x = padding.left + i%columns*(cellWidth + gap)
            val y = padding.top + i/columns*(cellHeight + gap)
            emptyCell.resizeRelocate(
               x.snapX,
               y.snapY,
               (root.width - padding.right).snapX - x.snapX,
               (y + cellHeight).snapY - y.snapY
            )
         } else {
            emptyCell.resizeRelocate(0.0, 0.0, 0.0, 0.0)
         }
      }

      private val Double.snapX: Double get() = snapPositionX(this)
      private val Double.snapY: Double get() = snapPositionY(this)
   }

   companion object {
      const val STYLE_CLASS = "item-picker"
      val CELL_STYLE_CLASS = listOf("block", "item-picker-element")
      private const val KEY_EMPTY_CELL = "empty_cell"
   }

}