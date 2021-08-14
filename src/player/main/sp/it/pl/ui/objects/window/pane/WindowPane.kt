package sp.it.pl.ui.objects.window.pane

import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ListChangeListener
import javafx.scene.Node
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.ceil
import sp.it.pl.main.emScaled
import sp.it.pl.ui.objects.window.Resize
import sp.it.pl.ui.objects.window.stage.WindowBase.Maximized
import sp.it.util.access.v
import sp.it.util.reactive.onEventDown

/** Window implemented as a [javafx.scene.layout.Pane] for in-application windows emulating window behavior. */
@Suppress("PropertyName")
open class WindowPane(val owner: AnchorPane) {
   @JvmField val root = StackPane()
   private val _x = 100.0
   private val _y = 100.0
   private val _w = 0.0
   private val _h = 0.0
   @JvmField protected val _resizing = ReadOnlyObjectWrapper(Resize.NONE)
   @JvmField protected val _moving = ReadOnlyBooleanWrapper(false)
   @JvmField protected val _focused = ReadOnlyBooleanWrapper(false)
   @JvmField protected val _fullscreen = ReadOnlyBooleanWrapper(false)
   @JvmField val x = object: SimpleObjectProperty<Double>(50.0) {
      override fun setValue(nv: Double) {
         val tmp = ceil(nv).toInt()
         var v = (tmp + tmp%2).toDouble()
         if (snappable.value) {
            v = mapSnap(v, v + w.value, w.value, owner.width)
            v = mapSnapX(v, owner.children)
         }
         if (offScreenFixOn.value) v = offScreenXMap(v)
         super.setValue(v)
         root.layoutX = v
      }
   }
   @JvmField val y = object: SimpleObjectProperty<Double>(50.0) {
      override fun setValue(nv: Double) {
         val tmp = ceil(nv).toInt()
         var v = (tmp + tmp%2).toDouble()
         if (snappable.value) {
            v = mapSnap(v, v + h.value, h.value, owner.height)
            v = mapSnapY(v, owner.children)
         }
         if (offScreenFixOn.value) v = offScreenYMap(v)
         super.setValue(v)
         root.layoutY = v
      }
   }
   @JvmField val w = root.prefWidthProperty()!!
   @JvmField val h = root.prefHeightProperty()!!
   @JvmField val visible = root.visibleProperty()!!
   @JvmField val opacity = root.opacityProperty()!!

   /** Indicates whether this window is maximized */
   @JvmField val maximized = v(Maximized.NONE)

   /** Defines whether this window is resizable */
   @JvmField  val movable = v(true)

   /** Indicates whether the window is being moved */
   @JvmField val moving: ReadOnlyBooleanProperty = _moving.readOnlyProperty

   /** Indicates whether and how the window is being resized */
   @JvmField val resizing = _resizing.readOnlyProperty!!

   /** Defines whether this window is resizable */
   @JvmField val resizable = v(true)
   @JvmField val snappable = v(true)
   @JvmField val snapDistance = v(5.0)
   @JvmField val offScreenFixOn = v(true)
   @JvmField val offScreenFixOffset = v(0.0)
   @JvmField val focused: ReadOnlyBooleanProperty = _focused.readOnlyProperty

   private val snapDist get() = snapDistance.value.emScaled
   private val focusListener = ListChangeListener { c: ListChangeListener.Change<out Node> ->
      val i = c.list.size
      _focused.value = i!=0 && c.list[i - 1]===root
   }

   init {
      root.onEventDown(MOUSE_PRESSED) { root.toFront() }
   }

   /** Opens this window.  */
   fun open() {
      if (!isOpen) {
         owner.children.add(root)
         owner.children.addListener(focusListener)
      }
   }

   /** Whether this window is open. */
   val isOpen: Boolean
      get() = owner.children.contains(root)

   /** Closes this window. */
   open fun close() {
      if (isOpen) {
         owner.children.removeListener(focusListener)
         owner.children.remove(root)
      }
   }

   /** Position this window to center of its owner. */
   fun alignCenter() {
      x.value = (owner.width - w.value)/2
      y.value = (owner.height - h.value)/2
   }

   /** Resizes to half of its owner. */
   fun resizeHalf() {
      w.value = owner.width/2
      h.value = owner.height/2
   }

   @JvmField var startX = 0.0
   @JvmField var startY = 0.0

   /** Installs move by dragging on provided Node, usually root. */
   fun moveOnDragOf(n: Node) {
      n.addEventHandler(MOUSE_PRESSED) { e: MouseEvent ->
         e.consume()
         if (maximized.value==Maximized.ALL || !movable.value || e.button!=MouseButton.PRIMARY) return@addEventHandler
         _moving.value = true
         startX = x.value - e.sceneX
         startY = y.value - e.sceneY
      }
      n.addEventHandler(MouseEvent.MOUSE_DRAGGED) { e: MouseEvent ->
         if (moving.value) {
            x.value = startX + e.sceneX
            y.value = startY + e.sceneY
         }
         e.consume()
      }
      n.addEventHandler(MouseEvent.MOUSE_RELEASED) { e: MouseEvent ->
         if (_moving.value) {
            _moving.value = false
         }
         e.consume()
      }
   }

   private fun offScreenXMap(d: Double): Double = when {
      d<0 -> offScreenFixOffset.value
      d + w.value>owner.width -> owner.width - w.value - offScreenFixOffset.value
      else -> d
   }

   private fun offScreenYMap(d: Double): Double = when {
      d<0 -> offScreenFixOffset.value
      d + h.value>owner.height -> owner.height - h.value - offScreenFixOffset.value
      else -> d
   }

   private fun mapSnap(x: Double, right: Double, w: Double, owner_width: Double): Double = when {
      abs(x)<snapDist -> 0.0
      abs(right - owner_width)<snapDist -> owner_width - w
      else -> x
   }

   private fun mapSnapX(x: Double, windows: List<Node>): Double {
      return windows.filter { it!==root }
         .flatMap {
            sequence {
               val wr = it.layoutX + it.layoutBounds.width
               if (abs(x - wr)<snapDist) yield(wr)
               if (abs(x + w.value - it.layoutX)<snapDist) yield(it.layoutX - w.value)
               if (abs(x + w.value/2.0 - (it.layoutX + it.layoutBounds.width/2.0))<snapDist) yield(it.layoutX - w.value/2.0)
            }
         }
         .minByOrNull { (it - x).absoluteValue }
         ?.takeIf { (it - x).absoluteValue<snapDist }
         ?: x
   }

   private fun mapSnapY(y: Double, windows: List<Node>): Double {
      return windows.filter { it!==root }
         .flatMap {
            sequence {
               val wr = it.layoutY + it.layoutBounds.height
               if (abs(y - wr)<snapDist) yield(wr)
               if (abs(y + h.value - it.layoutY)<snapDist) yield(it.layoutY - h.value)
               if (abs(y + h.value/2.0 - (it.layoutY + it.layoutBounds.height/2.0))<snapDist) yield(it.layoutY - h.value/2.0)
            }
         }
         .minByOrNull { (it - y).absoluteValue }
         ?.takeIf { (it - y).absoluteValue<snapDist }
         ?: y
   }

}