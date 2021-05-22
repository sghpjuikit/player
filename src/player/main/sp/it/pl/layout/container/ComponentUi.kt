package sp.it.pl.layout.container

import javafx.event.EventHandler
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import sp.it.pl.layout.AltState
import sp.it.pl.layout.Component
import sp.it.pl.layout.widget.ComponentLoader
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.io.IOLayer
import sp.it.pl.main.AppAnimator
import sp.it.util.access.ref.LazyR
import sp.it.util.async.runLater
import sp.it.util.dev.fail
import sp.it.util.dev.failCase
import sp.it.util.dev.printStacktrace
import sp.it.util.functional.asIf
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1If
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.size

/** Ui allowing user to manage [sp.it.pl.layout.Component] instances. */
interface ComponentUi: AltState {

   /** Scene graph root of this object. */
   val root: Pane

   /** Dispose of this ui with the intention of never being used again. */
   fun dispose() = Unit

   fun focusTraverse(child: Component, source: Widget): Unit = Unit

}

abstract class ComponentUiBase<C: Component>(val component: C): ComponentUi {

   /** Detaches the widget into standalone content in new window. */
   fun detach() {
      fun Component.size() = when (this) {
         is Widget -> load().asIf<Region>()?.size ?: root.size
         is Container<*> -> root.size
         else -> failCase(this)
      }

      val c = component
      val sizeOld = c.size()

      c.parent!!.addChild(c.indexInParent(), null)
      ComponentLoader.WINDOW(c)

      val w = c.window ?: fail { "Can not detach invisible component" }
      w.showingProperty().sync1If({ it }) {
         runLater {
            val sizeNew = c.size()
            val sizeDiff = sizeOld - sizeNew
            w.size = w.size + sizeDiff
         }
      }
   }

}

/** Ui allowing user to manage [Container] instances. */
abstract class ContainerUi<C: Container<*>>: ComponentUiBase<C> {

   final override val root = AnchorPane()
   val container: C
   var controls = LazyR { buildControls() }
   var isLayoutMode = false
   var isContainerMode = false

   constructor(container: C): super(container) {
      this.container = container
      root.styleClass += "container-ui"

      // report component graphics changes
      root.parentProperty() sync { IOLayer.allLayers.forEach { it.requestLayout() } }
      root.layoutBoundsProperty() sync { IOLayer.allLayers.forEach { it.requestLayout() } }

      // switch to container/normal layout mode using right/left click
      root.onEventDown(MOUSE_CLICKED, SECONDARY, false) {
         if (isLayoutMode && (!isContainerMode)) {
            if (container.children.isEmpty()) {
               AppAnimator.closeAndDo(root) { container.close() }
            } else {
               setContainerMode(true)
            }
            it.consume()
         }
      }
      root.onEventDown(MOUSE_CLICKED, SECONDARY, false) {
         // always consume setContainerMode event when it is not possible to go any higher
         if (isLayoutMode && (container.parent is Layout || container.parent is SwitchContainer))
            it.consume()
      }
   }

   protected open fun buildControls() = ContainerUiControls(this)

   override fun show() {
      isLayoutMode = true
      root.pseudoClassChanged("layout-mode", true)

      container.children.values.forEach {
         if (it is Container<*>) it.show()
         if (it is Widget) it.ui?.show()
      }
   }

   override fun hide() {
      if (isContainerMode) setContainerMode(false)

      isLayoutMode = false
      root.pseudoClassChanged("layout-mode", false)

      container.children.values.forEach {
         if (it is Container<*>) it.hide()
         if (it is Widget) it.ui?.hide()
      }
   }

   internal fun setContainerMode(b: Boolean) {
      if (isContainerMode==b) return

      isContainerMode = b
      controls.get().root.toFront()
      controls.get().anim.playFromDir(b)
      if (!b) {
          controls.get().anim.onFinished = EventHandler {
              controls.get().disposer.invoke()
              controls = LazyR { buildControls() }
          }
      }
   }

}