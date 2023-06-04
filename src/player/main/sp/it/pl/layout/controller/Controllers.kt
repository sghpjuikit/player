package sp.it.pl.layout.controller

import javafx.geometry.Pos.CENTER
import javafx.scene.Node
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Pane
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.initialTemplateFactory
import sp.it.pl.layout.isCompiling
import sp.it.pl.main.APP
import sp.it.pl.main.appProgressIndicator
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Anim.Interpolators.Companion.geomElastic
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.ui.hBox
import sp.it.util.ui.hyperlink
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.vBox
import sp.it.util.units.millis
import javafx.scene.robot.Robot
import mu.KLogging
import sp.it.pl.layout.WidgetTag
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.IconUN
import sp.it.pl.ui.objects.picker.ContainerPicker
import sp.it.pl.ui.objects.placeholder.Placeholder
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.async.runFX
import sp.it.util.conf.Config
import sp.it.util.ui.centre
import sp.it.util.ui.stackPane
import sp.it.util.ui.toPoint2D
import sp.it.util.units.version
import sp.it.util.units.year

/** Controller for [Widget] with no [sp.it.pl.layout.WidgetFactory]. */
class ControllerNoFactory(widget: Widget): SimpleController(widget) {
   init {
      root.lay += vBox(5, CENTER) {
         lay += label("Widget ${widget.name} is not recognized")
         lay += compileInfoUi()
      }
   }
}

/** Controller for [Widget] that fails to instantiate its controller. */
class ControllerLoadError(widget: Widget): SimpleController(widget) {
   init {
      root.lay += vBox(5, CENTER) {
         lay += label("Widget ${widget.name} failed to load properly")
         lay += compileInfoUi()
         lay += hyperlink("Reload") {
            onEventDown(MOUSE_CLICKED, PRIMARY) { APP.widgetManager.factories.recompile(widget.factory) }
         }
      }
   }
}

private fun SimpleController.compileInfoUi(): Node {
   val isCompiling = widget.factory.isCompiling(onClose)
   return hBox(10, CENTER) {
      lay += label("Compiling...").apply {
         val a = anim { setScaleXY(it*it) }.delay(500.millis).dur(500.millis).intpl(geomElastic()).applyNow()
         isCompiling sync { if (it) a.playOpen() else a.playClose() } on onClose
      }
      lay += appProgressIndicator().apply {
         isCompiling sync { progress = if (it) -1.0 else 1.0 } on onClose
      }
   }
}

/** Controller for empty [Widget]. Useful for certain layout operations and as a fill in for null. */
class ControllerEmpty(widget: Widget): Controller(widget) {

   private val root = Pane()

   override fun uiRoot() = root
   override fun focus() {}
   override fun close() {}
   override fun getConfigs() = emptyList<Config<Any?>>()
   override fun getConfig(name: String) = null

   companion object: WidgetCompanion, KLogging() {
      override val name = "Empty"
      override val description = "Empty widget with no content or functionality"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2014)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf<WidgetTag>()
      override val summaryActions = listOf<ShortcutPane.Entry>()
   }
}

/** Controller for intro [Widget]. Useful as initial content for new user. */
class ControllerIntro(widget: Widget): Controller(widget) {

   private val root = stackPane()

   override fun uiRoot() = root.apply {
      val p = Placeholder(IconUN(0x1f4c1), "Start with a simple click\n\nIf you are 1st timer, choose ${ContainerPicker.choiceForTemplate} > ${initialTemplateFactory.name}") {
         runFX(300.millis) {
            val c = widget.parent
            val i = c?.indexOf(widget)
            if (c!=null && i!=null) {
               val clickAt = root.localToScreen(root.layoutBounds).centre.toPoint2D()
               AppAnimator.closeAndDo(root) {
                  runFX(500.millis) {
                     c.removeChild(widget)
                     with(Robot()) {
                        mouseMove(clickAt)
                        mouseClick(PRIMARY)
                     }
                  }
               }
            }
         }
      }
      AppAnimator.applyAt(p, 0.0)
      p.showFor(root)
      AppAnimator.openAndDo(p) {}
   }

   override fun focus() {}
   override fun close() {}
   override fun getConfigs() = emptyList<Config<Any?>>()
   override fun getConfig(name: String) = null

   companion object: WidgetCompanion, KLogging() {
      override val name = "Intro"
      override val description = "Introductory help widget guiding user through initial steps"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2020)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf<WidgetTag>()
      override val summaryActions = listOf<ShortcutPane.Entry>()
   }
}