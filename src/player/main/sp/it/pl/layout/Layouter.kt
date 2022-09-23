package sp.it.pl.layout

import javafx.scene.Node
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import sp.it.pl.main.APP
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.Df
import sp.it.pl.main.IconFA
import sp.it.pl.main.contains
import sp.it.pl.main.get
import sp.it.pl.main.installDrag
import sp.it.pl.ui.objects.picker.ContainerPicker
import sp.it.pl.ui.objects.picker.Picker
import sp.it.pl.ui.objects.picker.WidgetPicker
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.launch
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.ui.containsMouse
import sp.it.util.ui.lay

/**
 * Container graphics for container null child.
 *
 * Shows user component picker to populate container with content (at this index) - it allows creation of layouts. Uses
 * two nested [Picker]s, one for [Container] and the other for widgets/templates.
 */
class Layouter: ComponentUi {

   private val container: Container<*>
   private val index: Int

   override val root = StackPane()
   var onCancel: () -> Unit = {}
   private var wasSelected = false
   private val cp: ContainerPicker
   private var isCpShown = false
   private val disposer = Disposer()

   @JvmOverloads
   constructor(container: Container<*>, index: Int, exitRoot: Pane? = null) {
      this.container = container
      this.index = index
      this.cp = ContainerPicker({ showContainer(it) }, { showWidgetArea(it) }).apply {
         onSelect = {
            wasSelected = true
            isCpShown = false
            AppAnimator.closeAndDo(root) { it.onSelect() }
         }
         onCancel = {
            if (!APP.ui.isLayoutMode)
               hide()
         }
         consumeCancelEvent = false
         buildContent()
      }
      APP.widgetManager.widgets.separateWidgets attach { cp.buildContent() } on disposer

      root.lay += cp.root

      AppAnimator.applyAt(cp.root, 0.0)

      root.installDrag(
         IconFA.EXCHANGE,
         "Switch components",
         { e -> Df.COMPONENT in e.dragboard },
         { e -> e.dragboard[Df.COMPONENT]===container },
         { e -> e.dragboard[Df.COMPONENT].swapWith(container, index) }
      )

      // show cp on mouse click
      root.onEventDown(MOUSE_CLICKED, PRIMARY, false) {
         if (!container.lockedUnder.value) {
            show()
            it.consume()
         }
      }

      // hide cp on mouse exit
      val er = exitRoot ?: cp.root
      er.onEventDown(MOUSE_EXITED) {
         if (!wasSelected && !isSuppressedHiding(er)) {
            cp.onCancel()
            it.consume()
         }
      } on disposer
   }

   override fun show() = showControls(true)

   override fun hide() = showControls(false)

   override fun dispose() = disposer()

   fun hideAnd(onHidden: () -> Unit) = showControls(false) { onHidden() }

   private fun showControls(value: Boolean, onClosed: () -> Unit = {}) {
      if (isCpShown==value) {
         if (!value) onClosed()
         return
      }
      val isWpShown = root.children.size!=1
      if (isWpShown) {
         if (!value) onClosed()
         return
      }

      isCpShown = value
      if (value) {
         AppAnimator.openAndDo(cp.root, null)
      } else {
         AppAnimator.closeAndDo(cp.root) {
            if (!wasSelected) onCancel()
            onClosed()
         }
      }
   }

   private fun showContainer(c: Container<*>) {
      container.addChild(index, c)
      if (c is ContainerBi) APP.actionStream("Divide layout")
   }

   private fun showWidgetArea(mode: WidgetPicker.Mode) {
      val wp = WidgetPicker(mode)
      wp.onSelect = { factory ->
         wasSelected = false
         AppAnimator.closeAndDo(wp.root) {
            root.children -= wp.root
            launch(FX) {
               container.addChild(index, factory.create())
               if (APP.ui.isLayoutMode) container.show()
               APP.actionStream("New widget")
            }
         }
      }
      wp.onCancel = {
         wasSelected = false
         AppAnimator.closeAndDo(wp.root) {
            root.children -= wp.root
            if (root.containsMouse())
               showControls(true)
         }
      }
      wp.buildContent()
      wp.consumeCancelEvent = true // we need right-click to not close container

      root.lay += wp.root
      AppAnimator.openAndDo(wp.root, null)
   }

   companion object {
      private const val SUPPRESS_HIDING_KEY = "suppress"

      fun suppressHidingFor(node: Node, suppress: Boolean) {
         if (suppress) node.properties[SUPPRESS_HIDING_KEY] = null
         else node.properties -= SUPPRESS_HIDING_KEY
      }

      private fun isSuppressedHiding(node: Node) = SUPPRESS_HIDING_KEY in node.properties
   }
}