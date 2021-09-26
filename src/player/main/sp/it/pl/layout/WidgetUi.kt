package sp.it.pl.layout

import javafx.scene.layout.AnchorPane
import sp.it.pl.layout.Widget.LoadType.AUTOMATIC
import sp.it.pl.layout.Widget.LoadType.MANUAL
import sp.it.pl.layout.controller.io.IOLayer
import sp.it.pl.main.APP
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.DelayAnimator
import sp.it.pl.main.Df
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconOC
import sp.it.pl.main.contains
import sp.it.pl.main.get
import sp.it.pl.main.installDrag
import sp.it.pl.main.toS
import sp.it.pl.ui.objects.placeholder.Placeholder
import sp.it.util.functional.net
import sp.it.util.functional.traverse
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.type.nullify
import sp.it.util.ui.layFullArea
import sp.it.util.ui.pseudoclass
import sp.it.util.ui.removeFromParent
import sp.it.util.ui.styleclassToggle

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
      this.widget.ui = this

      root.id = "widget-ui"
      root.layFullArea += contentRoot.apply {
         id = "widget-ui-contentRoot"
         styleClass += STYLECLASS

         layFullArea += content.apply {
            id = "widget-ui-content"
            styleClass += CONTENT_STYLECLASS
         }
      }

      controls = WidgetUiControls(this)
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
            animation.openAndDo(contentRoot, null)
            content.children.clear()
            content.layFullArea += widget.load()

            // put controls to new widget
            widget.padding sync { content.style = it?.net { "-fx-padding:${it.toS()};" } } on disposer
            controls.title.textProperty() syncFrom widget.customName on disposer
            widget.locked sync { controls.lockB.icon(it, IconFA.LOCK, IconFA.UNLOCK) } on disposer
         }
         widget.loadType.value==MANUAL -> {
            AppAnimator.closeAndDo(contentRoot) {
               content.children.clear()
               animation.openAndDo(contentRoot, null)

               // put controls to new widget
               widget.padding sync { content.style = it?.net { "-fx-padding:${it.toS()};" } } on disposer
               controls.title.textProperty() syncFrom widget.customName on disposer
               widget.locked sync { controls.lockB.icon(it, IconFA.LOCK, IconFA.UNLOCK) } on disposer

               manualLoadPane = buildManualLoadPane()
               manualLoadPane?.showFor(content)
            }
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

   private fun buildManualLoadPane() = Placeholder(IconOC.UNFOLD, "") {
      widget.forceLoading = true
      loadWidget()
      widget.forceLoading = false
   }.apply {
      styleClass += "widget-ui-load-placeholder"
      info.text = "Unfold ${widget.customName.value} (LMB)"
   }

   override fun dispose() {
      disposer()
      controls.root.removeFromParent()
      nullify(::controls)
   }

   companion object {
      private val animation = DelayAnimator()
      const val STYLECLASS = "widget-ui"
      const val CONTENT_STYLECLASS = "widget-ui-content"
      @JvmField val PSEUDOCLASS_DRAGGED = pseudoclass("dragged")
   }
}