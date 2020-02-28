package sp.it.pl.gui.objects.grid

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Pane
import sp.it.pl.gui.objects.hierarchy.Item
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.FileType.FILE
import sp.it.util.file.nameOrRoot
import sp.it.util.file.nameWithoutExtensionOrRoot
import sp.it.util.math.min
import sp.it.util.reactive.onEventDown
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.units.millis
import java.io.File

/**
 * GridCell implementation for file using [sp.it.pl.gui.objects.hierarchy.Item]
 * that shows a thumbnail image. Supports asynchronous loading of thumbnails and loading animation.
 */
open class GridFileIconCell: GridCell<Item, File>() {
   protected lateinit var root: Pane
   protected lateinit var name: Label
   protected lateinit var icon: Icon
   protected var imgLoadAnimation: Anim? = null
   private var imgLoadAnimationItem: Item? = null

   init {
      styleClass += "grid-file-icon-cell"
   }

   protected open fun computeName(item: Item): String = when (item.valType) {
      DIRECTORY -> item.value.nameOrRoot
      FILE -> item.value.nameWithoutExtensionOrRoot
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
      imgLoadAnimationItem = null
   }

   override fun updateItem(item: Item?, empty: Boolean) {
      if (item===getItem()) {
         if (!empty) updateIcon(item!!)
         return
      }
      super.updateItem(item, empty)

      if (imgLoadAnimation!=null) {
         imgLoadAnimation?.stop()
         imgLoadAnimationItem = item
         imgLoadAnimation?.applyAt(item?.loadProgress ?: 0.0)
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
      }
   }

   protected open fun computeGraphics() {
      name = Label()
      name.alignment = Pos.CENTER

      imgLoadAnimation = anim(200.millis) {
         if (imgLoadAnimationItem!=null) {
            imgLoadAnimationItem?.loadProgress = it
            icon.opacity = it*it*it*it
         }
      }

      icon = Icon().apply {
         isAnimated.value = false
         isFocusTraversable = false
      }
      root = object: Pane(icon, name) {
         override fun layoutChildren() {
            val w = layoutBounds.width
            val h = layoutBounds.height
            val th = computeCellTextHeight()
            icon.resizeRelocate(0.0, 0.0, w, h - th)
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
      }
   }

   fun updateIcon(i: Item) {
      icon.scale(1.125)
      icon.isFocusTraversable = false
      icon.opacity = if (FileField.IS_HIDDEN.getOf(i.value)) 0.4 else 1.0
      icon.size(gridView.value?.let { it.cellHeight.value min (it.cellHeight.value - computeCellTextHeight()) } ?: 50.0)

      val glyph: GlyphIcons = when (i.valType) {
         FILE -> when {
            i.value.path.endsWith("css") -> IconMD.LANGUAGE_CSS3 as GlyphIcons
            else -> IconFA.FILE
         }
         DIRECTORY -> when {
            i.value.isAbsolute && i.value.name.isEmpty() -> IconMD.HARDDISK
            else -> IconFA.FOLDER
         }
      }
      icon.icon(glyph)
      imgLoadAnimation?.playOpenFrom(item?.loadProgress ?: 0.0)
   }
}