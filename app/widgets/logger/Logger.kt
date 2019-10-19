package logger

import javafx.scene.control.TextArea
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.TextDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.main.Widgets
import sp.it.pl.main.emScaled
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x

@Widget.Info(
   author = "Martin Polakovic",
   name = Widgets.LOGGER,
   description = "Displays console output by listening to System.out, which contains application logging.",
   version = "1.0.0",
   year = "2015",
   group = Widget.Group.DEVELOPMENT
)
class Logger(widget: Widget): SimpleController(widget), TextDisplayFeature {

   private val wrapText by cv(false).def(name = "Wrap text", info = "Wrap text at the end of the text area to the next line.")
   private val area = TextArea()

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.consumeScrolling()

      root.lay += area.apply {
         isEditable = false
         isWrapText = false
         wrapTextProperty() syncFrom wrapText on onClose

         text = "# This is redirected output (System.out) stream of this application.\n"
      }

      APP.systemout.addListener { area.appendText(it) } on onClose
   }

   override fun showText(text: String) = println(text)

}