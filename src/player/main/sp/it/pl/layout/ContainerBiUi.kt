package sp.it.pl.layout

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ELLIPSIS_H
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ELLIPSIS_V
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MAGIC
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.Node
import javafx.scene.control.SplitPane
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.AnchorPane
import kotlin.reflect.KProperty0
import sp.it.pl.main.APP
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.access.toggle
import sp.it.util.access.toggleNext
import sp.it.util.access.value
import sp.it.util.collections.setToOne
import sp.it.util.dev.fail
import sp.it.util.dev.failCase
import sp.it.util.dev.failIf
import sp.it.util.functional.asIf
import sp.it.util.math.clip
import sp.it.util.math.distance
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.reactive.syncTo
import sp.it.util.ui.hasFocus
import sp.it.util.ui.layFullArea
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.setAnchors
import sp.it.util.ui.size
import sp.it.util.ui.x2

class ContainerBiUi(c: ContainerBi): ContainerUi<ContainerBi>(c) {
   private val root1 = AnchorPane()
   private val root2 = AnchorPane()
   private val splitPane = SplitPane(root1, root2)
   private var ui1: ComponentUi? = null
   private var ui2: ComponentUi? = null
   private val disposer = Disposer()
   private var initialized = false

   var absoluteSize: Int
      get() = container.absoluteSize.value
      set(i) {
         failIf(i<0 || i>2) { "Only values 0,1,2 allowed" }

         if (initialized) convertPositionForAbsoluteSizeChange(i)

         ui1.asIf<WidgetUi?>()?.controls?.updateAbsB()
         ui2.asIf<WidgetUi?>()?.controls?.updateAbsB()
         container.children[1].asIf<Container<*>?>()?.ui?.asIf<ContainerUi<*>>()?.let { it.controls.ifSet { it.updateIcons() } }
         container.children[2].asIf<Container<*>?>()?.ui?.asIf<ContainerUi<*>>()?.let { it.controls.ifSet { it.updateIcons() } }
      }
   var joined: Boolean
      get() = container.joined.value
      set(i) {
         splitPane.pseudoClassChanged("joined", i)
      }
   var collapsed: Int
      get() = container.collapsed.value
      set(i) {
         container.collapsed.value = i
         splitPane.orientationProperty().removeListener(collapsedL1)
         if (i!=0) splitPane.orientationProperty().addListener(collapsedL1)
         splitPane.removeEventFilter(MOUSE_RELEASED, collapsedL2)
         if (i!=0) splitPane.addEventFilter(MOUSE_RELEASED, collapsedL2)
         splitPane.removeEventFilter(MOUSE_DRAGGED, collapsedL3)
         if (i!=0) splitPane.addEventFilter(MOUSE_DRAGGED, collapsedL3)
         if (initialized) updatePosition()
         splitPane.pseudoClassChanged("collapsed", i!=0)
      }
   private val collapsedActivatorDist: Double
      get() = when (splitPane.orientation!!) {
         HORIZONTAL -> APP.ui.snapDistance.value/splitPane.layoutBounds.width
         VERTICAL -> APP.ui.snapDistance.value/splitPane.layoutBounds.height
      }
   private val collapsedL1 = ChangeListener<Orientation> { _, _, _ -> updatePosition() }
   private val collapsedL2 = EventHandler<MouseEvent> {
      if (it.clickCount==2 && it.button==PRIMARY && collapsed!=0) {
         val so = splitPane.orientation
         val isGrabber = so==VERTICAL && collapsed==-1 && it.y<grabberSize ||
            so==VERTICAL && collapsed==1 && it.y>splitPane.height - grabberSize ||
            so==HORIZONTAL && collapsed==-1 && it.x<grabberSize ||
            so==HORIZONTAL && collapsed==1 && it.x>splitPane.width - grabberSize

         if (isGrabber)
            collapsed = 0
      }
   }
   private val collapsedL3 = EventHandler<MouseEvent> {
      if (collapsed!=0) {
         val so = splitPane.orientation
         val isGrabber = so==VERTICAL && collapsed==-1 && it.y<grabberSize ||
            so==VERTICAL && collapsed==1 && it.y>splitPane.height - grabberSize ||
            so==HORIZONTAL && collapsed==-1 && it.x<grabberSize ||
            so==HORIZONTAL && collapsed==1 && it.x>splitPane.width - grabberSize

         if (isGrabber)
            collapsed = 0
      }
   }

   init {
      root.layFullArea += splitPane
      root1.minSize = 0.x2
      root2.minSize = 0.x2

      splitPane.onMouseClicked = root.onMouseClicked

      container.orientation syncTo splitPane.orientationProperty()
      container.collapsed sync { collapsed = it }
      container.absoluteSize sync { absoluteSize = it }
      container.joined sync { joined = it }

      // initialize position
      splitPane.sync1IfInScene {
         initialized = true
         updatePosition()
         root.parentProperty() sync {
            updatePosition()
         }
      }

      // fixes initialization
      splitPane.layoutBoundsProperty().attach { updatePosition() }

      splitPane.skinProperty() syncNonNullWhile { skin ->
         val divider = skin.node.lookup(".split-pane-divider")!!
         divider.onMousePressed = null
         divider.onMouseDragged = EventHandler {
            val vRaw = when (splitPane.orientation!!) {
               HORIZONTAL -> splitPane.sceneToLocal(it.sceneX, 0.0).x/splitPane.width
               VERTICAL -> splitPane.sceneToLocal(0.0, it.sceneY).y/splitPane.height
            }
            val vSnap = !it.isShiftDown && !it.isShortcutDown
            val vSnaps = when {
               vSnap -> sequenceOf(0.0, 1.0, 0.5, if (splitPane.orientation==HORIZONTAL) splitPane.height/splitPane.width else splitPane.width/splitPane.height)
               else -> sequenceOf(0.0, 1.0)
            }
            val v = (vSnaps.minByOrNull { it distance vRaw }?.takeIf { it distance vRaw <= collapsedActivatorDist } ?: vRaw).clip(0.0, 1.0)

            val shouldBeCollapsed = v==0.0 || v==1.0
            if (shouldBeCollapsed) {
               val collapsedNew = if (v<0.5) -1 else 1
               if (collapsed!=collapsedNew) collapsed = collapsedNew
            } else {
               val collapsedNew = 0
               if (collapsed!=collapsedNew) collapsed = collapsedNew
               updatePosition(v)
            }

            it.consume()
         }
         Subscription()
      }

      // apply/persist position on position change end (allows recovering collapsed to container.position)
      splitPane.onEventUp(MOUSE_RELEASED) {
         if (collapsed==0)
            container.position.value = when (absoluteSize) {
               0 -> splitPane.dividers[0].position
               1 -> if (container.orientation.value==HORIZONTAL) root1.width else root1.height
               2 -> if (container.orientation.value==HORIZONTAL) root2.width else root2.height
               else -> fail()
            }
      }

      // toggle collapsed mode
      splitPane.onEventUp(MOUSE_CLICKED, PRIMARY, false) {
         if (it.clickCount == 2 && isOverDivider(it)) {
            collapsed = when {
               collapsed != 0 -> 0
               absoluteSize != 0 -> (absoluteSize-1)*2-1
               else -> null
                  ?: sequenceOf(root1 to -1, root2 to 1).find { it.first.hasFocus() }?.second
                  ?: sequenceOf(root1 to -1, root2 to 1).minByOrNull { it.first.layoutBounds.size.let { it.x * it.y } }?.second
                  ?: -1
            }
            it.consume()
         }
      }
      // toggle joined mode
      splitPane.onEventUp(MOUSE_CLICKED, SECONDARY, false) {
         if (it.clickCount == 2 && isOverDivider(it)) {
            c.joined.toggle()
            it.consume()
         }
      }
   }

   override fun buildControls() = super.buildControls().apply {
      val orientB = Icon(MAGIC, -1.0, "Change orientation").addExtraIcon(0).onClickDo { container.orientation.toggleNext() }.styleclass("header-icon")
      container.orientation sync { orientB.icon(it==VERTICAL, ELLIPSIS_V, ELLIPSIS_H) } on disposer
   }

   private fun isOverDivider(e: MouseEvent): Boolean {
      val vRaw = when (splitPane.orientation!!) {
         HORIZONTAL -> splitPane.sceneToLocal(e.sceneX, 0.0).x/splitPane.width
         VERTICAL -> splitPane.sceneToLocal(0.0, e.sceneY).y/splitPane.height
      }
      return splitPane.dividers[0].position distance vRaw <= collapsedActivatorDist
   }

   private fun updatePosition(position: Double = computePosition()) {
      root.scene ?: return

      splitPane.setDividerPosition(0, position)
      root1.maxSize = if (collapsed==-1) 0.x2 else (-1).x2
      root2.maxSize = if (collapsed==+1) 0.x2 else (-1).x2
   }

   private fun computePosition() = when (collapsed) {
      -1 -> 0.0
      0 -> when (absoluteSize) {
         0 -> container.position.value
         1 -> container.position.value/(if (container.orientation.value==HORIZONTAL) splitPane.width else splitPane.height)
         2 -> 1-container.position.value/(if (container.orientation.value==HORIZONTAL) splitPane.width else splitPane.height)
         else -> fail()
      }
      1 -> 1.0
      else -> failCase(collapsed)
   }

   private fun convertPositionForAbsoluteSizeChange(abs: Int) {
      container.position.value = when (collapsed) {
         -1, 1 -> when (abs) {
            0 -> 0.5
            1 -> container.position.value/(if (container.orientation.value==HORIZONTAL) splitPane.width else splitPane.height)
            2 -> 1-container.position.value/(if (container.orientation.value==HORIZONTAL) splitPane.width else splitPane.height)
            else -> fail()
         }
         0 -> when (abs) {
            0 -> splitPane.dividers[0].position
            1 -> if (container.orientation.value==HORIZONTAL) root1.width else root1.height
            2 -> if (container.orientation.value==HORIZONTAL) root2.width else root2.height
            else -> fail()
         }
         else -> failCase(collapsed)
      }
   }

   fun setComponent(i: Int, c: Component?) {
      failIf(i!=1 && i!=2) { "Index must be 1 or 2" }

      fun <T> T.closeUi(ui: KProperty0<ComponentUi?>) = apply { if (ui.value is Layouter || ui.value is WidgetUi) ui.value?.dispose() }
      fun <T: AltState> T.showIfLM() = apply { if (APP.ui.isLayoutMode) show() }

      val r = if (i==1) root1 else root2
      val ui = if (i==1) ::ui1 else ::ui2
      val n: Node = when (c) {
         is Container<*> -> {
            ui.value = null.closeUi(ui)
            c.load(r).apply {
               c.showIfLM()
            }
         }
         is Widget -> {
            ui.value = ui.value.takeIf { it is WidgetUi && it.widget==c }
               ?: WidgetUi(container, i, c).closeUi(ui).showIfLM()
            ui.value!!.root
         }
         null -> {
            ui.value = ui.value.takeIf { it is Layouter } ?: Layouter(container, i).closeUi(ui).showIfLM()
            ui.value!!.root
         }
      }

      r.children.setToOne(n)
      n.setAnchors(0.0)
   }

   /** Toggle fixed size between child1/child2/off. */
   fun toggleAbsoluteSize() {
      container.absoluteSize.value = absoluteSize.let { if (it==2) 0 else it + 1 }
   }

   /** Toggle fixed size for specified children between true/false. */
   fun toggleAbsoluteSizeFor(i: Int) {
      container.absoluteSize.value = if (absoluteSize==i) 0 else i
   }

   /** Collapse on/off to the left or top depending on the orientation. */
   fun toggleCollapsed1() {
      collapsed = if (collapsed==-1) 0 else -1
   }

   /** Collapse on/off to the right or bottom depending on the orientation. */
   fun toggleCollapsed2() {
      collapsed = if (collapsed==1) 0 else 1
   }

   override fun show() {
      super.show()
      ui1.asIf<Layouter>()?.show()
      ui2.asIf<Layouter>()?.show()
   }

   override fun hide() {
      super.hide()
      ui1.asIf<Layouter>()?.hide()
      ui2.asIf<Layouter>()?.hide()
   }

   override fun dispose() {
      disposer()
      super.dispose()
   }

   companion object {
      private const val grabberSize = 20.0
   }

}