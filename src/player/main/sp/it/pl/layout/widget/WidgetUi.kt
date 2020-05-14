package sp.it.pl.layout.widget

import javafx.event.EventHandler
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.AnchorPane
import sp.it.pl.layout.container.ComponentUiBase
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.widget.Widget.LoadType.AUTOMATIC
import sp.it.pl.layout.widget.Widget.LoadType.MANUAL
import sp.it.pl.layout.widget.controller.io.IOLayer
import sp.it.pl.main.APP
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.DelayAnimator
import sp.it.pl.main.Df
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconOC
import sp.it.pl.main.contains
import sp.it.pl.main.get
import sp.it.pl.main.installDrag
import sp.it.pl.ui.objects.placeholder.Placeholder
import sp.it.util.action.ActionManager
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncTo
import sp.it.util.type.nullify
import sp.it.util.ui.layFullArea
import sp.it.util.ui.pseudoclass
import sp.it.util.ui.removeFromParent

/**
 * UI allowing user to manage [Widget] instances. Manages widget's lifecycle and user's interaction with the widget.
 *
 * Maintains final 1:1 relationship with the widget, always contains exactly 1 final widget.
 */
class WidgetUi: ComponentUiBase<Widget> {
   /** Container this ui is associated with. */
   val container: Container<*>
   /** Index of the child in the [container] */
   val index: Int
   /** Widget this ui is associated with. Equivalent to [component]. */
   val widget: Widget
   override val root = AnchorPane()
   val contentRoot = AnchorPane()
   val controls: WidgetUiControls
   val content = AnchorPane()
   private val disposer = Disposer()
   private var manualLoadPane: Placeholder? = null

   /**
    * Creates area for the container and its child widget at specified child position.
    *
    * @param container widget's parent container
    * @param index index of the widget within the container
    * @param widget widget that will be managed and displayed
    */
   constructor(container: Container<*>, index: Int, widget: Widget): super(widget) {
      this.container = container
      this.index = index
      this.widget = widget
      this.widget.parent = container
      this.widget.uiTemp = this

      root.id = "widget-ui"
      root.layFullArea += contentRoot.apply {
         id = "widget-ui-contentRoot"
         styleClass += STYLECLASS

         layFullArea += content.apply {
            id = "widget-ui-content"
            styleClass += "widget-ui-content"
         }
      }

      controls = WidgetUiControls(this)
      contentRoot.layFullArea += controls.root

      installDrag(
         root, IconFA.EXCHANGE, "Switch components",
         { e -> Df.COMPONENT in e.dragboard },
         { e -> e.dragboard[Df.COMPONENT].let { it==this.container || it==widget } },
         { e -> e.dragboard[Df.COMPONENT].swapWith(this.container, this.index) }
      )

      // show help
      root.onEventDown(KEY_PRESSED) {
         if (!it.isAltDown && !it.isControlDown && !it.isShortcutDown && !it.isMetaDown) {
            if (it.code==ActionManager.keyShortcutsComponent) {
               APP.actions.showShortcutsFor(widget)
               it.consume()
            }
         }
      }

      // report component graphics changes
      root.parentProperty() sync { IOLayer.allLayers.forEach { it.requestLayout() } }
      root.layoutBoundsProperty() sync { IOLayer.allLayers.forEach { it.requestLayout() } }

      loadWidget()
      if (APP.ui.isLayoutMode) show() else hide()
   }

   private fun loadWidget() {
      disposer()

      when {
         widget.isLoaded || widget.forceLoading || widget.loadType.value==AUTOMATIC -> {
            manualLoadPane?.hide()
            manualLoadPane = null

            // load widget
            animation.openAndDo(contentRoot, null)
            content.children.clear()
            content.layFullArea += widget.load()

            // put controls to new widget
            widget.custom_name syncTo controls.title.textProperty() on disposer
            controls.propB.isDisable = widget.getConfigs().isEmpty()
            widget.locked sync { controls.lockB.icon(if (it) IconFA.LOCK else IconFA.UNLOCK) } on disposer
         }
         widget.loadType.value==MANUAL -> {
            AppAnimator.closeAndDo(contentRoot) {
               content.children.clear()
               animation.openAndDo(contentRoot, null)

               // put controls to new widget
               widget.custom_name syncTo controls.title.textProperty() on disposer
               controls.propB.isDisable = widget.getConfigs().isEmpty()
               widget.locked sync { controls.lockB.icon(if (it) IconFA.LOCK else IconFA.UNLOCK) } on disposer

               manualLoadPane = buildManualLoadPane()
               manualLoadPane?.showFor(content)
            }
         }
      }
   }

   override fun show() = controls.show()

   override fun hide() = controls.hide()

   fun setStandaloneStyle() {
      contentRoot.styleClass.clear()
      content.styleClass.clear()
   }

   private fun buildManualLoadPane() = Placeholder(IconOC.UNFOLD, "") {
      widget.forceLoading = true
      loadWidget()
      widget.forceLoading = false
   }.apply {
      styleClass += "widget-ui-load-placeholder"
      info.text = "Unfold ${widget.custom_name.value} (LMB)"
   }

   override fun dispose() {
      disposer()
      controls.root.removeFromParent()
      nullify(::controls)
   }

   companion object {
      private val animation = DelayAnimator()
      const val STYLECLASS = "widget-ui"
      @JvmField val PSEUDOCLASS_DRAGGED = pseudoclass("dragged")
   }
}