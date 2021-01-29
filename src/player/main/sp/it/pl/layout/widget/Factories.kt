package sp.it.pl.layout.widget

import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.layout.Pane
import javafx.scene.robot.Robot
import mu.KLogging
import sp.it.pl.layout.container.BiContainer
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.main.APP
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.IconUN
import sp.it.pl.main.Widgets.PLAYBACK
import sp.it.pl.main.Widgets.PLAYLIST
import sp.it.pl.ui.objects.picker.ContainerPicker
import sp.it.pl.ui.objects.placeholder.Placeholder
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.async.runFX
import sp.it.util.conf.Config
import sp.it.util.file.div
import sp.it.util.ui.centre
import sp.it.util.ui.stackPane
import sp.it.util.ui.toPoint2D
import sp.it.util.units.millis
import sp.it.util.units.version
import sp.it.util.units.year

/** Empty widget. Useful for certain layout operations and as a fill in for null. */
@ExperimentalController("Has no use for user")
class EmptyWidget(widget: Widget): Controller(widget) {

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
      override val summaryActions = listOf<ShortcutPane.Entry>()
      override val group = Widget.Group.OTHER
   }
}

/** Empty widget. Useful for certain layout operations and as a fill in for null. */
class IntroWidget(widget: Widget): Controller(widget) {

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
      override val summaryActions = listOf<ShortcutPane.Entry>()
      override val group = Widget.Group.OTHER
   }
}

val emptyWidgetFactory = WidgetFactory(EmptyWidget::class, APP.location.widgets/"emptyWidget")
val introWidgetFactory = WidgetFactory(IntroWidget::class, APP.location.widgets/"introWidget")

val initialTemplateFactory = TemplateFactory("Playback + Playlist") {
   BiContainer(VERTICAL).apply {
      children += 1 to APP.widgetManager.factories.getFactory(PLAYBACK.id).orNone().create()
      children += 2 to APP.widgetManager.factories.getFactory(PLAYLIST.id).orNone().create()
   }
}

fun testControlContainer() = BiContainer(HORIZONTAL).apply {
   children += 1 to BiContainer(VERTICAL).apply {
      children += 1 to emptyWidgetFactory.create()
      children += 2 to emptyWidgetFactory.create()
   }
   children += 2 to BiContainer(VERTICAL).apply {
      children += 1 to BiContainer(HORIZONTAL).apply {
         children += 1 to emptyWidgetFactory.create()
         children += 2 to emptyWidgetFactory.create()
      }
      children += 2 to BiContainer(HORIZONTAL).apply {
         children += 1 to emptyWidgetFactory.create()
         children += 2 to emptyWidgetFactory.create()
      }
   }

}