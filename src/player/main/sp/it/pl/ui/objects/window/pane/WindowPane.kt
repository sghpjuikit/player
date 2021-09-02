package sp.it.pl.ui.objects.window.pane

import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ListChangeListener
import javafx.scene.Node
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
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
import sp.it.util.ui.sceneXy
import sp.it.util.ui.x

/** Window implemented as a [javafx.scene.layout.Pane] for in-application windows emulating window behavior. */
@Suppress("PropertyName")
open class WindowPane(val owner: AnchorPane) {
   protected val _resizing = ReadOnlyObjectWrapper(Resize.NONE)
   protected val _moving = ReadOnlyBooleanWrapper(false)
   protected val _focused = ReadOnlyBooleanWrapper(false)
   protected val _fullscreen = ReadOnlyBooleanWrapper(false)

   /** This window's scene-graph */
   val root = StackPane()

   /** This window's left top corner horizontal position in its parent's area */
   val x = object: SimpleObjectProperty<Double>(50.0) {
      override fun setValue(nv: Double) {
         var v = nv
         if (snappable.value) {
            v = mapSnap(v, v + w.value, w.value, owner.width)
            v = mapSnapX(v, owner.children)
         }
         if (offScreenFixOn.value)
            v = offScreenXMap(v)
         super.setValue(v)
         root.layoutX = v
      }
   }

   /** This window's left top corner vertical position in its parent's area */
   val y = object: SimpleObjectProperty<Double>(50.0) {
      override fun setValue(nv: Double) {
         var v = nv
         if (snappable.value) {
            v = mapSnap(v, v + h.value, h.value, owner.height)
            v = mapSnapY(v, owner.children)
         }
         if (offScreenFixOn.value)
            v = offScreenYMap(v)
         super.setValue(v)
         root.layoutY = v
      }
   }

   /** This window's [Node.prefWidth] */
   val w = root.prefWidthProperty()!!

   /** This window's [Node.prefHeight] */
   val h = root.prefHeightProperty()!!

   /** This window's [Node.visible] */
   val visible = root.visibleProperty()!!

   /** This window's [Node.opacity] */
   val opacity = root.opacityProperty()!!

   /** Indicates whether this window is maximized */
   val maximized = v(Maximized.NONE)

   /** Defines whether this window is resizable */
   val movable = v(true)

   /** Indicates whether the window is being moved */
   val moving: ReadOnlyBooleanProperty = _moving.readOnlyProperty

   /** Indicates whether and how the window is being resized */
   val resizing = _resizing.readOnlyProperty!!

   /** Whether this window is resizable */
   val resizable = v(true)

   /** Whether this window can be snapped on move/resize */
   val snappable = v(true)

   /** Window snap activation distance */
   val snapDistance = v(5.0)

   /** Window this window can be off its parent's area */
   val offScreenFixOn = v(true)

   /** Windows's minimal distance from parent's area's edge */
   val offScreenFixOffset = v(0.0)

   /** Whether this window is focused. */
   val focused: ReadOnlyBooleanProperty = _focused.readOnlyProperty

   private var moveStart = 0 x 0
   private val snapDist get() = snapDistance.value.emScaled
   private val focusListener = ListChangeListener { c: ListChangeListener.Change<out Node> ->
      val i = c.list.size
      _focused.value = i!=0 && c.list[i - 1]===root
   }

   init {
      // keep focused window on top
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

   /** Installs move by dragging on provided Node, usually root. */
   fun moveOnDragOf(n: Node) {
      n.addEventHandler(MOUSE_DRAGGED) {
         if (!moving.value && maximized.value!=Maximized.ALL && movable.value && it.button==PRIMARY) {
            _moving.value = true
            val r = snappable.value
            if (it.isShortcutDown) snappable.value = false
            moveStart = (x.value x y.value) - it.sceneXy
            snappable.value = r
         }
         if (moving.value) {
            val r = snappable.value
            if (it.isShortcutDown) snappable.value = false
            x.value = moveStart.x + it.sceneX
            y.value = moveStart.y + it.sceneY
            snappable.value = r
         }
         it.consume()
      }
      n.addEventHandler(MOUSE_RELEASED) {
         if (_moving.value) {
            _moving.value = false
         }
         it.consume()
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