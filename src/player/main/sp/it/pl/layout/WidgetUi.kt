package sp.it.pl.layout

import javafx.geometry.HPos
import javafx.geometry.VPos
import javafx.scene.layout.AnchorPane
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import sp.it.pl.layout.Widget.LoadType.AUTOMATIC
import sp.it.pl.layout.Widget.LoadType.MANUAL
import sp.it.pl.layout.controller.io.IOLayer
import sp.it.pl.main.APP
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.DelayAnimator
import sp.it.pl.main.Df
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconOC
import sp.it.pl.main.Ui.FPS
import sp.it.pl.main.contains
import sp.it.pl.main.get
import sp.it.pl.main.installDrag
import sp.it.pl.main.toS
import sp.it.pl.ui.objects.placeholder.Placeholder
import sp.it.util.async.coroutine.runSuspendingFx
import sp.it.util.functional.net
import sp.it.util.functional.traverse
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.type.nullify
import sp.it.util.ui.anchorPane
import sp.it.util.ui.layFullArea
import sp.it.util.ui.pseudoclass
import sp.it.util.ui.removeFromParent
import sp.it.util.ui.styleclassToggle
import sp.it.util.units.kt

/**
 * UI allowing user to manage [Widget] instances. Manages widget's lifecycle and user's interaction with the widget.
 *
 * Maintains final 1:1 relationship with the widget, always contains exactly 1 final widget.
 */
class WidgetUi(container: Container<*>, index: Int, widget: Widget): ComponentUiBase<Widget>(widget) {
   /** Container this ui is associated with. */
   val container = container

   /** Index of the child in the [container] */
   val index = index

   /** Widget this ui is associated with. Equivalent to [component]. */
   val widget = widget.apply {
      parent = container
      ui = this@WidgetUi
   }

   override val root = anchorPane {}
   val content = anchorPane {
      id = "widget-ui-content"
      styleClass += CONTENT_STYLECLASS
   }
   val contentRoot = object: AnchorPane() {
      init {
         id = "widget-ui-contentRoot"
         styleClass += STYLECLASS
         layFullArea += content
      }
      override fun layoutChildren() {
         super.layoutChildren()
         controls.root.prefWidth = width
         controls.root.prefHeight = height
         layoutInArea(controls.root, 0.0, 0.0, width, height, 0.0, padding, true, true, HPos.CENTER, VPos.CENTER, true)
      }
   }
   val controls = WidgetUiControls(this).apply {
      root.isManaged = false
   }
   private var disposed = false
   private val disposer = Disposer()
   private var manualLoadPane: Placeholder? = null

   init {
      root.id = "widget-ui"
      root.layFullArea += contentRoot
      contentRoot.layFullArea += controls.root

      root.installDrag(
         IconFA.EXCHANGE,
         "Switch components",
         { e -> Df.COMPONENT in e.dragboard },
         { e -> e.dragboard[Df.COMPONENT].let { it==this.container || it==widget } },
         { e -> e.dragboard[Df.COMPONENT].swapWith(this.container, this.index) }
      )

      // report component graphics changes
      root.parentProperty() sync { IOLayer.requestLayoutFor(widget) }
      root.layoutBoundsProperty() sync { IOLayer.requestLayoutFor(widget) }

      root.sceneProperty() attach { updateStandalone() }

      loadWidget()
      if (APP.ui.isLayoutMode) show() else hide()
   }

   private fun loadWidget() {
      disposer()
      updateStandalone()

      when {
         widget.isLoaded || widget.forceLoading || widget.loadType.value==AUTOMATIC -> {
            manualLoadPane?.hide()
            manualLoadPane = null

            // load widget
            val anim = animation.openAndDo(contentRoot, null) on disposer
            val delay = anim.delay
            val delayOffset = (1000/FPS*2).milliseconds // 2 frames
            runSuspendingFx {
               delay(delay.kt - delayOffset) // delay with animation, but invoke before animation starts loading
               content.children.clear()
               content.layFullArea += widget.load()
            } on disposer


            // put controls to new widget
            widget.padding sync { content.style = it?.net { "-fx-padding:${it.toS()};" } } on disposer
            controls.title.textProperty() syncFrom widget.customName on disposer
            widget.locked sync { controls.lockB.icon(it, IconFA.LOCK, IconFA.UNLOCK) } on disposer
         }
         widget.loadType.value==MANUAL -> {
            AppAnimator.closeAndDo(contentRoot) {
               content.children.clear()
               animation.openAndDo(contentRoot, null) on disposer

               // put controls to new widget
               widget.padding sync { content.style = it?.net { "-fx-padding:${it.toS()};" } } on disposer
               controls.title.textProperty() syncFrom widget.customName on disposer
               widget.locked sync { controls.lockB.icon(it, IconFA.LOCK, IconFA.UNLOCK) } on disposer

               manualLoadPane = buildManualLoadPane()
               manualLoadPane?.showFor(content)
            } on disposer
         }
      }
   }

   private fun updateStandalone() {
      val isStandalone = widget.traverse<Component> { it.parent }.any { it is Layout && it.isStandalone.value }
      contentRoot.styleclassToggle(STYLECLASS, !isStandalone)
      content.styleclassToggle(CONTENT_STYLECLASS, !isStandalone)
   }

   override fun show() = controls.show()

   override fun hide() = controls.hide()

   private fun buildManualLoadPane() = Placeholder(widget.factory.icon ?: IconOC.UNFOLD, "") {
      widget.forceLoading = true
      loadWidget()
      widget.forceLoading = false
   }.apply {
      styleClass += "widget-ui-load-placeholder"
      info.text = "Unfold ${widget.customName.value} (LMB)"
   }

   override fun dispose() {
      if (disposed) return
      disposed = true
      disposer()
      controls.root.removeFromParent()
      controls.showSettingsClose()
      nullify(::controls)
   }

   companion object {
      private val animation = DelayAnimator()
      const val STYLECLASS = "widget-ui"
      const val CONTENT_STYLECLASS = "widget-ui-content"
      @JvmField val PSEUDOCLASS_DRAGGED = pseudoclass("dragged")
   }
}