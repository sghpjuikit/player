package sp.it.pl.ui.objects.window.pane

import sp.it.pl.main.resizeIcon
import sp.it.util.ui.initMouseDrag
import sp.it.util.collections.setToOne
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.layout.HBox
import javafx.scene.layout.BorderPane
import javafx.geometry.Pos
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.input.MouseEvent
import sp.it.pl.ui.objects.window.Resize.*
import sp.it.util.math.P
import sp.it.util.reactive.attach
import sp.it.util.reactive.sync
import sp.it.util.ui.Util
import sp.it.util.ui.areaBy
import sp.it.util.ui.lay
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.sceneToLocal
import sp.it.util.ui.screenXy
import sp.it.util.ui.x

open class PaneWindowControls(owner: AnchorPane?): WindowPane(owner!!) {
   private val subroot: Pane = buildWindowLayout({ borderDragStart(it) }, { borderDragged(it) }, { borderDragEnd(it) })
   val borders = subroot.lookupId("borders", AnchorPane::class.java)
   val content = subroot.lookupId("content", AnchorPane::class.java)
   val header = subroot.lookupId("headerContainer", StackPane::class.java)
   val titleL = header.lookupId("titleL", Label::class.java)
   val contentRoot = subroot.lookupId("contentRoot", VBox::class.java)
   val leftHeaderBox = header.lookupId("leftHeaderBox", HBox::class.java)
   val rightHeaderBox = header.lookupId("rightHeaderBox", HBox::class.java)

   private var resizeStart: P = 0 x 0
   private var resizeWindow: Rectangle2D = (0 x 0) areaBy (0 x 0)
   private var headerAllowed = true

   /** Sets visibility of the window header, including its buttons for control of the window (close, etc.).  */
   var isHeaderVisible = true
      set(value) {
         if (headerAllowed) {
            field = value
            showHeader(value)
         }
      }

   fun setContent(n: Node) {
      content.children.setToOne(n)
      Util.setAnchors(n, 0.0)
   }

   private fun showHeader(v: Boolean) {
      if (v && !contentRoot.children.contains(header)) contentRoot.children.add(0, header)
      if (!v && contentRoot.children.contains(header)) contentRoot.children.remove(header)
   }

   /**
    * Set false to permanently hide header.
    */
   fun setHeaderAllowed(value: Boolean) {
      headerAllowed = value
      isHeaderVisible = value
   }

   /**
    * Set title for this window shown in the header.
    */
   fun setTitle(text: String?) {
      titleL.text = text
   }

   /**
    * Set title alignment.
    */
   fun setTitlePosition(align: Pos?) {
      BorderPane.setAlignment(titleL, align)
   }

   private fun buttonDragStart(it: MouseEvent) {
      if (resizable.value) {
         _resizing.value = SE
         resizeStart = it.screenXy
         it.consume()
      }
   }

   private fun borderDragStart(it: MouseEvent) {
      if (resizable.value) {
         resizeStart = it.screenXy
         resizeWindow = Rectangle2D(x.value, y.value, w.value, h.value)
         val b = root.parent.sceneToLocal(it)
         val bx = b.x - x.value
         val by = b.y - y.value
         val bw = w.value
         val bh = h.value
         val l = 20.0 // corner threshold
         val r = when {
            bx>bw - l && by>bh - l -> SE
            bx<l && by>bh - l -> SW
            bx<l && by<l -> NW
            bx>bw - l && by<l -> NE
            bx>bw - l -> E
            by>bh - l -> S
            bx<l -> W
            by<l -> N
            else -> NONE
         }
         
         if (r!==NONE) {
            _resizing.value = r
            it.consume()
         }
      }
   }

   private fun borderDragged(it: MouseEvent) {
      if (_resizing.value!==NONE) {
         val resizeDiff = it.screenXy - resizeStart
         when(_resizing.value) {
            SE -> {
               w.value = resizeWindow.width + resizeDiff.x
               h.value = resizeWindow.height + resizeDiff.y
            }
            S -> {
               h.value = resizeWindow.height + resizeDiff.y
            }
            E -> {
               w.value = resizeWindow.width + resizeDiff.x
            }
            SW -> {
               x.value = resizeWindow.minX + resizeDiff.x
               w.value = resizeWindow.width - resizeDiff.x
               h.value = resizeWindow.height + resizeDiff.y
            }
            W -> {
               x.value = resizeWindow.minX + resizeDiff.x
               w.value = resizeWindow.width - resizeDiff.x
            }
            NW -> {
               x.value = resizeWindow.minX + resizeDiff.x
               y.value = resizeWindow.minY + resizeDiff.y
               w.value = resizeWindow.width - resizeDiff.x
               h.value = resizeWindow.height - resizeDiff.y
            }
            N -> {
               y.value = resizeWindow.minY + resizeDiff.y
               h.value = resizeWindow.height - resizeDiff.y
            }
            NE -> {
               y.value = resizeWindow.minY + resizeDiff.y
               w.value = resizeWindow.width + resizeDiff.x
               h.value = resizeWindow.height - resizeDiff.y
            }
            ALL, NONE, null -> {}
         }
         it.consume()
      }
   }

   private fun borderDragEnd(it: MouseEvent) {
      if (_resizing.value!==NONE) {
         _resizing.value = NONE
         it.consume()
      }
   }

   init {
      root.lay += subroot

      // maintain custom pseudoclasses for .window styleclass
      resizing.attach { root.pseudoClassChanged(pcResized, it!==NONE) }
      moving.attach { root.pseudoClassChanged(pcMoved, it) }
      focused.attach { root.pseudoClassChanged(pcFocused, it) }

      // disable resizing behavior completely when not resizable
      resizable sync { r -> borders.children.forEach { if (it !is Pane) it.isMouseTransparent = !r } }

      val resizeB = resizeIcon()
      resizeB.initMouseDrag(
         P(),
         { drag -> drag.data = P(w.value, h.value) },
         { drag ->
            if (resizable.value) {
               w.value = drag.data.x + drag.diff.x
               h.value = drag.data.y + drag.diff.y
            }
         }
      )
//      resizeB.onMousePressed = EventHandler { buttonDragStart(it) }
//      resizeB.onMouseDragged = EventHandler { borderDragged(it) }
//      resizeB.onMouseReleased = EventHandler { borderDragEnd(it) }
      borders.children += resizeB
      Util.setAnchors(resizeB, null, 0.0, 0.0, null)
   }

   companion object {
      /** Pseudoclass active when this window is resized. Applied on root as '.window'.  */
      const val pcResized = "resized"
      /** Pseudoclass active when this window is moved. Applied on root as '.window'.  */
      const val pcMoved = "moved"
      /** Pseudoclass active when this window is focused. Applied on root as '.window'.  */
      const val pcFocused = "focused"
   }

}