package logger

import javafx.scene.control.TextArea
import mu.KLogging
import sp.it.pl.layout.Widget
import sp.it.pl.main.WidgetTags.DEVELOPMENT
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.TextDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconUN
import sp.it.pl.main.Widgets.LOGGER_NAME
import sp.it.pl.main.emScaled
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

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

   companion object: WidgetCompanion, KLogging() {
      override val name = LOGGER_NAME
      override val description = "Displays process output (stdout), which contains application logging"
      override val descriptionLong = "$description.\nThe visualization is customizable."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2015)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(UTILITY, DEVELOPMENT)
      override val summaryActions = listOf<ShortcutPane.Entry>()
   }
}