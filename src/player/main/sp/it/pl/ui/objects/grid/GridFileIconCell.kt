package sp.it.pl.ui.objects.grid

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconUN
import sp.it.pl.ui.objects.contextmenu.ValueContextMenu
import sp.it.pl.ui.objects.hierarchy.Item
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.FileType.FILE
import sp.it.util.file.nameOrRoot
import sp.it.util.math.min
import sp.it.util.reactive.onEventDown
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.x
import sp.it.util.units.millis
import java.io.File

/**
 * GridCell implementation for file using [sp.it.pl.ui.objects.hierarchy.Item]
 * that shows a thumbnail image. Supports asynchronous loading of thumbnails and loading animation.
 */
open class GridFileIconCell: GridCell<Item, File>() {
   protected lateinit var root: Pane
   protected lateinit var name: Label
   protected lateinit var icon: Icon
   protected var imgLoadAnimation: Anim? = null
   private var loadProgress = 0.0 // 0-1

   init {
      styleClass += "icon-file-grid-cell"
   }

   protected open fun computeName(item: Item): String = when (item.valType) {
      DIRECTORY -> item.value.nameOrRoot
      FILE -> item.value.nameWithoutExtension
   }

   protected open fun computeCellTextHeight(): Double = 40.0

   protected open fun onAction(i: Item, edit: Boolean) = Unit

   override fun updateSelected(selected: Boolean) {
      super.updateSelected(selected)
      icon.select(selected)
   }

   override fun dispose() {
      failIfNotFxThread()

      imgLoadAnimation?.stop()
      imgLoadAnimation = null
   }

   override fun updateItem(item: Item?, empty: Boolean) {
      if (item===getItem()) {
         if (!empty) updateIcon(item!!)
         return
      }
      super.updateItem(item, empty)

      if (imgLoadAnimation!=null) {
         imgLoadAnimation?.stop()
         imgLoadAnimation?.applyAt(loadProgress)
      }

      if (empty) {
         graphic = null   // do not discard contents of the graphics
      } else {
         if (!::root.isInitialized) computeGraphics()  // create graphics lazily and only once
         if (graphic!==root) graphic = root           // set graphics only when necessary
      }

      if (graphic!=null) {
         name.text = if (item==null) null else computeName(item)
         updateIcon(item!!)
         pseudoClassChanged("file-hidden", FileField.IS_HIDDEN.getOf(item.value))
      }
   }

   protected open fun computeGraphics() {
      val iconAnimationParent = StackPane() // prevents opacity clash with css
      name = label {
         alignment = Pos.CENTER
      }

      icon = Icon().apply {
         isAnimated.value = false
         isFocusTraversable = false
         iconAnimationParent.lay += this
      }
      root = object: Pane(iconAnimationParent, name) {
         override fun layoutChildren() {
            val w = layoutBounds.width
            val h = layoutBounds.height
            val th = computeCellTextHeight()
            iconAnimationParent.resizeRelocate(0.0, 0.0, w, h - th)
            name.resizeRelocate(0.0, h - th, w, th)
         }
      }.apply {
         isSnapToPixel = true
         minSize = -1.0 x -1.0
         prefSize = -1.0 x -1.0
         maxSize = -1.0 x -1.0
         onEventDown(MOUSE_CLICKED) {
            if (it.button==PRIMARY && it.clickCount==2) {
               onAction(item, it.isShiftDown)
               it.consume()
            }
         }
         onEventDown(MOUSE_CLICKED, SECONDARY) {
            globalContextMenu.setItemsFor(item?.value)
            globalContextMenu.show(root, it)
         }
      }
      imgLoadAnimation = anim(200.millis) {
         loadProgress = it
         iconAnimationParent.opacity = it*it*it*it
      }
   }

   fun updateIcon(i: Item) {
      icon.scale(1.125)
      icon.isFocusTraversable = false
      icon.size(gridView.value?.let { it.cellHeight.value min (it.cellHeight.value - computeCellTextHeight()) } ?: 50.0)

      val glyph: GlyphIcons = when (i.valType) {
         FILE -> when {
            i.value.path.endsWith("css") -> IconMD.LANGUAGE_CSS3
            else -> IconUN(0x1f4c4)
         }
         DIRECTORY -> when {
            i.value.isAbsolute && i.value.name.isEmpty() -> IconMD.HARDDISK
            else -> IconUN(0x1f4c1)
         }
      }
      icon.icon(glyph)
      imgLoadAnimation?.playOpenFrom(loadProgress)
   }

   companion object {
      private val globalContextMenu by lazy { ValueContextMenu<Any?>() }
   }
}