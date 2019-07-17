package sp.it.pl.layout.widget

import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.layout.Pane
import sp.it.pl.layout.container.BiContainer
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.main.APP
import sp.it.pl.main.Widgets
import sp.it.util.conf.Config
import sp.it.util.file.div

/** Empty widget. Useful for certain layout operations and as a fill in for null. */
@Widget.Info(
   author = "Martin Polakovic",
   name = "Empty",
   description = "Empty widget with no content or functionality.",
   version = "1.0",
   year = "2014",
   group = Widget.Group.OTHER
)
@ExperimentalController("Has no use for user")
class EmptyWidget(widget: Widget): Controller(widget) {

   private val root = Pane()

   override fun loadFirstTime() = root
   override fun focus() {}
   override fun close() {}
   override fun getField(name: String) = null
   override fun getFields() = emptyList<Config<Any?>>()

}

val emptyWidgetFactory = WidgetFactory(EmptyWidget::class, APP.location.widgets/"Empty", null)

val initialTemplateFactory = TemplateFactory("Playback + Playlist") {
   BiContainer(VERTICAL).apply {
      children += 1 to APP.widgetManager.factories.getFactoryByGuiName(Widgets.PLAYBACK).orNone().create()
      children += 2 to APP.widgetManager.factories.getFactoryByGuiName(Widgets.PLAYLIST).orNone().create()
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